package cloud.kosch.pmddcam

import android.graphics.Bitmap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.*

object MotionMath {
    fun smooth(edge0:Float,edge1:Float,value:Float):Float {
        val t=((value-edge0)/(edge1-edge0)).coerceIn(0f,1f);return t*t*(3-2*t)
    }
    fun mask(o:SceneObject,u:Float,v:Float,d:Float):Float {
        if(!o.enabled)return 0f
        if(o.id==999 && o.mask==null)return 0f
        o.mask?.let{return it.sample(u,v)}
        val w=(o.right-o.left).coerceAtLeast(.001f);val h=(o.bottom-o.top).coerceAtLeast(.001f)
        val x=(u-o.left)/w;val y=(v-o.top)/h
        if(x !in 0f..1f || y !in 0f..1f)return 0f
        val edge=smooth(0f,.16f,min(min(x,1-x),min(y,1-y)))
        if(o.face || o.overrideDepth)return edge
        return edge*(1-smooth(.18f,.5f,abs(d-o.depth)))
    }
    data class Direction(val x:Float,val y:Float)
    fun direction(o:SceneObject,u:Float,v:Float):Direction{
        val angle=o.angle*PI.toFloat()/180;val dx=cos(angle);val dy=sin(angle)
        val x=(u-(o.left+o.right)/2)/(o.right-o.left).coerceAtLeast(.01f)
        val y=(v-(o.top+o.bottom)/2)/(o.bottom-o.top).coerceAtLeast(.01f)
        val radius=max(.12f,hypot(x,y));val rx=x/radius;val ry=y/radius
        val vx:Float;val vy:Float
        when(o.motion){
            Motion.APPROACH,Motion.PULSE->{vx=rx*.8f+dx*.2f;vy=ry*.8f+dy*.2f}
            Motion.RETREAT->{vx=-rx*.8f+dx*.2f;vy=-ry*.8f+dy*.2f}
            Motion.ROTATE->{vx=-ry;vy=rx}
            Motion.FLOW->{vx=dx;vy=dy+y*.3f}
            Motion.DRIFT->{vx=dx;vy=dy}
        }
        val length=max(.1f,hypot(vx,vy));return Direction(vx/length,vy/length)
    }

}

data class RenderResult(val image:Bitmap,val depth:DepthMap)

object PmddRenderer {
    suspend fun render(source:Bitmap,raw:DepthMap,project:Project):RenderResult {
        val r=project.recipe.normalized();val style=Styles.get(r.style)
        val width=source.width;val height=source.height;val shortEdge=min(width,height)
        val pixels=IntArray(width*height);source.getPixels(pixels,0,width,0,0,width,height)
        // A real low-pass filter replaces the coarse downsample/upsample halo source.
        val soft=boxBlur(pixels,width,height,max(1,shortEdge/140))
        val output=IntArray(pixels.size)
        val depth=effectiveDepth(raw,project)
        val guide=GuidedDepth(source,depth)
        val paint=if(r.styleMix>0f)PainterlyField.create(pixels,width,height,style.technique)else null
        val light=if(r.styleMix>0f&&style.technique in setOf(Technique.NEON,Technique.BLOOM))LightField.create(pixels,width,height,style.technique==Technique.NEON)else null
        val scene=if(style.id=="vivid" && r.styleMix>0f)SceneTone.create(source) else null
        val objects=project.objects.filter{it.enabled}
        val context=currentCoroutineContext()
        val invW=1f/width;val invH=1f/height
        val detailStep=max(1,shortEdge/420)
        val gain=r.depth*(.8f+.65f*r.depth)
        val geometry=(r.screenSize/16f*45f/r.viewDistance).coerceIn(.4f,2f)
        val pixelSize=max(2,shortEdge/if(style.id=="pixel16")145 else 82)
        val artistic=style.technique !in setOf(Technique.PHOTO,Technique.BLOOM)
        val detailRetention=1-r.styleMix*(if(style.technique==Technique.PIXEL)1f else if(artistic).86f else 0f)
        val printCell=max(2.5f,shortEdge/75f)
        val styleSaturation=if(style.id=="matrix"||style.id=="gameboy"||style.id=="blueprint")1f else style.saturation
        val cgaPalette=intArrayOf(0xff101018.toInt(),0xff40e8e0.toInt(),0xffee46b6.toInt(),0xfff7f5ee.toInt())
        val c64Palette=intArrayOf(0xff000000.toInt(),0xffffffff.toInt(),0xff813338.toInt(),0xff75cec8.toInt(),0xff8e3c97.toInt(),0xff56ac4d.toInt(),0xff2e2c9b.toInt(),0xffedf171.toInt(),0xff8e5029.toInt(),0xff553800.toInt(),0xffc46c71.toInt(),0xff4a4a4a.toInt(),0xff7b7b7b.toInt(),0xffa9ff9f.toInt(),0xff706deb.toInt(),0xffb2b2b2.toInt())
        val posterPalette=intArrayOf(0xff132746.toInt(),0xffe44955.toInt(),0xfff4c84e.toInt(),0xff369da7.toInt(),0xfff7edd7.toInt())
        val pocketPalette=intArrayOf(0xff162b18.toInt(),0xff385c31.toInt(),0xff85a34b.toInt(),0xffd9e995.toInt())
        for(y in 0 until height){
            if(y%16==0)context.ensureActive()
            val v=(y+.5f)*invH
            for(x in 0 until width){
                val u=(x+.5f)*invW;val index=y*width+x;val src=pixels[index]
                val red=channel(src,16);val green=channel(src,8);val blue=channel(src,0)
                val c=soft[index];val br=channel(c,16);val bg=channel(c,8);val bb=channel(c,0)
                val gray=luma(src);val smoothGray=luma(c);val detail=gray-smoothGray
                val gx=luma(soft[y*width+min(width-1,x+detailStep)])-luma(soft[y*width+max(0,x-detailStep)])
                val gy=luma(soft[min(height-1,y+detailStep)*width+x])-luma(soft[max(0,y-detailStep)*width+x])
                val edge=(abs(gx)+abs(gy)).coerceIn(0f,1f)
                val noise=noise(x,y)
                val paper=noise(x/3,y/3)*.6f+noise(x/11,y/11)*.4f
                // Range weighting smooths pigment inside surfaces while retaining their outlines.
                val blend=.76f/(1+abs(detail)*18f)
                val painted=paint?.color(u,v)
                val pr=painted?.let{channel(it,16)}?: (red+(br-red)*blend)
                val pg=painted?.let{channel(it,8)}?: (green+(bg-green)*blend)
                val pb=painted?.let{channel(it,0)}?: (blue+(bb-blue)*blend)
                val paintEdge=paint?.edge(u,v)?:edge
                val pigmentGray=pr*.2126f+pg*.7152f+pb*.0722f
                var sr=red;var sg=green;var sb=blue
                when(style.technique){
                    Technique.PHOTO -> Unit
                    Technique.COMIC -> {
                        val levels=style.levels.coerceIn(3,8)
                        val ink=1-MotionMath.smooth(.045f,.22f,paintEdge)*style.ink
                        sr=quant(pr,levels)*ink;sg=quant(pg,levels)*ink;sb=quant(pb,levels)*ink
                        if(style.id=="manga"){
                            val cell=max(2f,printCell*.65f);val dot=printDot(x.toFloat(),y.toFloat(),cell,0f,.35f)
                            val mid=MotionMath.smooth(.18f,.38f,pigmentGray)*(1-MotionMath.smooth(.7f,.85f,pigmentGray))
                            sr=(quant(pigmentGray,4)*(.98f-dot*mid*.3f))*ink;sg=sr;sb=sr
                        }
                    }
                    Technique.WATERCOLOR -> {
                        val pooling=MotionMath.smooth(.025f,.26f,paintEdge)*.2f
                        val wash=paper*.035f*(.25f+pigmentGray)
                        val dry=if(style.id=="drybrush")max(0f,noise-.1f)*.20f else 0f
                        val opacity=.83f-pigmentGray*.08f-dry
                        sr=softQuant(pr,style.levels)*opacity*(1-pooling)+(1-opacity)*.99f+wash
                        sg=softQuant(pg,style.levels)*opacity*(1-pooling)+(1-opacity)*.965f+wash
                        sb=softQuant(pb,style.levels)*opacity*(1-pooling)+(1-opacity)*.91f+wash
                    }
                    Technique.OIL -> {
                        val stroke=(noise(x/3,y/8)*.65f+noise(x/11,y/3)*.35f)*.034f
                        val relief=(pigmentGray-smoothGray).coerceIn(-.15f,.15f)*.35f
                        sr=quant(pr,style.levels)+stroke+relief;sg=quant(pg,style.levels)+stroke+relief;sb=quant(pb,style.levels)+stroke+relief
                    }
                    Technique.INK -> {
                        val line=(MotionMath.smooth(.018f,.12f,edge)*.94f+(1-MotionMath.smooth(.12f,.32f,gray))*.65f).coerceIn(0f,1f)
                        if(style.id=="blueprint"){sr=.025f+line*.91f;sg=.09f+line*.87f;sb=.25f+line*.73f}
                        else {val brush=if(style.id=="sumie")max(0f,-detail)*.8f else 0f;sr=(1-line-brush).coerceAtLeast(0f);sg=sr;sb=sr}
                    }
                    Technique.PENCIL -> {
                        val hatch=1-MotionMath.smooth(.10f,.30f,abs(sin((x+y)/max(2f,printCell*.38f)*PI.toFloat())))
                        val graphite=(max(0f,smoothGray-gray)*4.5f+edge*1.65f+(1-gray)*hatch*.36f).coerceIn(0f,.97f)
                        val line=(.98f-graphite+noise*.035f*(1-gray)).coerceIn(0f,1f)
                        val color=if(style.id=="coloredpencil").68f else 0f
                        sr=line*(1-color*(1-red));sg=line*(1-color*(1-green));sb=line*(1-color*(1-blue))
                    }
                    Technique.HATCH,Technique.WOODCUT -> {
                        val cell=if(style.technique==Technique.WOODCUT)printCell*.8f else printCell*.4f
                        val line=1-MotionMath.smooth(.03f,.28f,abs(sin((x+y)/cell*PI.toFloat())))
                        val cross=1-MotionMath.smooth(.03f,.25f,abs(sin((x-y)/cell*PI.toFloat())))
                        val mark=line*MotionMath.smooth(.12f,.68f,1-gray)+cross*MotionMath.smooth(.48f,.86f,1-gray)
                        val value=if(style.technique==Technique.WOODCUT)(1-MotionMath.smooth(.28f,.48f,mark+edge*2.2f+(1-gray)*.35f))
                            else (.97f-mark*.72f-edge*.8f+noise*.028f).coerceIn(0f,1f)
                        sr=value;sg=value;sb=value
                    }
                    Technique.HALFTONE -> {
                        if(style.id=="newspaper"){
                            val value=1-printDot(x.toFloat(),y.toFloat(),printCell,.78f,1-gray)*.94f;sr=value;sg=value*.97f;sb=value*.9f
                        }else if(style.id=="risocomic"){
                            val redInk=printDot(x.toFloat(),y.toFloat(),printCell,.26f,(1-green)*.86f)
                            val blueInk=printDot(x.toFloat(),y.toFloat(),printCell,-.52f,(1-red)*.84f)
                            sr=.98f-blueInk*.76f;sg=.94f-redInk*.66f-blueInk*.27f;sb=.84f-redInk*.36f
                        }else{
                            val cyan=printDot(x.toFloat(),y.toFloat(),printCell,.26f,1-pr)
                            val magenta=printDot(x.toFloat(),y.toFloat(),printCell,1.31f,1-pg)
                            val yellow=printDot(x.toFloat(),y.toFloat(),printCell,0f,1-pb)
                            sr=1-cyan*.9f;sg=.98f-magenta*.9f;sb=.92f-yellow*.87f
                        }
                    }
                    Technique.NEON -> {
                        val contour=MotionMath.smooth(.022f,.16f,edge)
                        val glow=((light?.sample(u,v)?:0f)*2.2f+contour*.7f).coerceIn(0f,1.25f)
                        val lit=MotionMath.smooth(.35f,.95f,gray)
                        val base=when(style.id){"neonwire"->.015f;"hologram"->.1f;else->.26f}
                        sr=red*base;sg=green*base;sb=blue*base
                        if(style.id=="futuretech"||style.id=="hologram"){sr=gray*.035f;sg=gray*.10f;sb=gray*.19f}
                        if(style.id=="matrix"){sr=gray*.015f;sg=gray*.18f;sb=gray*.025f}
                        sr+=channel(style.tint,16)*glow;sg+=channel(style.tint,8)*glow;sb+=channel(style.tint,0)*glow
                        if(style.id=="cyberpunk"||style.id=="neontokyo"){sg+=lit*.13f;sb+=lit*.24f}
                        if(style.id=="hologram"){val scan=.8f+.2f*cos(y/max(1f,shortEdge/350f)*PI.toFloat());sr*=scan;sg*=scan;sb*=scan}
                    }
                    Technique.PASTEL -> {
                        val tooth=(noise*.55f+paper*.45f)*.07f
                        sr=softQuant(pr,style.levels)*.83f+.16f+tooth;sg=softQuant(pg,style.levels)*.83f+.15f+tooth;sb=softQuant(pb,style.levels)*.83f+.13f+tooth
                    }
                    Technique.STIPPLE -> {
                        val cell=max(3f,shortEdge/100f);val ix=floor(x/cell).toInt();val iy=floor(y/cell).toInt()
                        val cx=(ix+.5f+noise(ix,iy)*.16f)*cell;val cy=(iy+.5f+noise(iy,ix)*.16f)*cell
                        val sample=pixels[cy.toInt().coerceIn(0,height-1)*width+cx.toInt().coerceIn(0,width-1)]
                        val dot=1-MotionMath.smooth(.30f,.44f,hypot(x-cx,y-cy)/cell)
                        sr=.97f*(1-dot)+channel(sample,16)*dot;sg=.95f*(1-dot)+channel(sample,8)*dot;sb=.87f*(1-dot)+channel(sample,0)*dot
                    }
                    Technique.POSTER -> {
                        val color=nearestPalette(posterPalette,pr,pg,pb);sr=channel(color,16);sg=channel(color,8);sb=channel(color,0)
                    }
                    Technique.THERMAL -> {
                        val t=gray*4f
                        sr=(1.5f-abs(t-3f)).coerceIn(0f,1f);sg=(1.5f-abs(t-2f)).coerceIn(0f,1f);sb=(1.5f-abs(t-1f)).coerceIn(0f,1f)
                    }
                    Technique.BLOOM -> {
                        val glow=(light?.sample(u,v)?:0f)*.22f
                        sr=red+(1-red)*glow;sg=green+(1-green)*glow*.92f;sb=blue+(1-blue)*glow*.85f
                    }
                    Technique.DUOTONE -> {
                        val t=gray.pow(.86f)
                        sr=channel(style.shadow,16)*(1-t)+channel(style.highlight,16)*t
                        sg=channel(style.shadow,8)*(1-t)+channel(style.highlight,8)*t
                        sb=channel(style.shadow,0)*(1-t)+channel(style.highlight,0)*t
                    }
                    Technique.SOLAR -> {
                        if(style.id=="infrared"){sr=green*.95f+red*.15f;sg=blue*.72f+gray*.16f;sb=red*.65f+blue*.4f}
                        else{val metal=(.5f+.38f*sin((gray-.5f)*8f)+edge*.45f).coerceIn(0f,1f);sr=metal*.88f;sg=metal*.96f;sb=metal}
                    }
                    Technique.PIXEL -> {
                        val sample=pixels[(y/pixelSize*pixelSize)*width+x/pixelSize*pixelSize]
                        sr=quant(channel(sample,16),style.levels);sg=quant(channel(sample,8),style.levels);sb=quant(channel(sample,0),style.levels)
                        if(style.id=="gameboy"||style.id=="cga"||style.id=="c64"){
                            val color=if(style.id=="gameboy")pocketPalette[(luma(sample)*3).roundToInt().coerceIn(0,3)] else nearestPalette(if(style.id=="cga")cgaPalette else c64Palette,channel(sample,16),channel(sample,8),channel(sample,0))
                            sr=channel(color,16);sg=channel(color,8);sb=channel(color,0)
                        }
                    }
                }
                if(scene!=null){
                    // Bright, dimensional photographic grade inspired by the reference aesthetic.
                    // Lift photographed shadows while preserving a black point and highlight detail.
                    val base=scene.base(u,v,gray)
                    val sky=MotionMath.smooth(.025f,.22f,blue-max(red,green))
                    val foliage=MotionMath.smooth(.005f,.12f,green-max(red,blue))
                    val normalized=((gray-scene.black)/(scene.white-scene.black)).coerceIn(0f,1f)
                    val shadowLift=.12f*(1-MotionMath.smooth(.18f,.55f,base))*MotionMath.smooth(0f,.08f,normalized)
                    val highlightRoll=.06f*MotionMath.smooth(.55f,.95f,normalized)
                    var target=normalized+shadowLift-highlightRoll
                    target+=.16f*(target-.5f)*4f*target*(1-target)
                    val clarity=(gray-base).coerceIn(-.10f,.10f)*.45f
                    target=(target+clarity-sky*.075f).coerceIn(0f,1f)
                    val scale=if(gray>.001f)target/gray else 1f
                    sr=red*scale;sg=green*scale;sb=blue*scale
                    // Colour follows the photographed scene: no replacement sky or decorative leaves.
                    sr-=sky*.018f;sg-=sky*.026f
                    sr+=foliage*.01f;sb-=foliage*.022f
                    val saturation=(max(red,max(green,blue))-min(red,min(green,blue)))/max(.05f,max(red,max(green,blue)))
                    val vibrance=1f+.22f*(1-saturation)
                    sr=target+(sr-target)*vibrance;sg=target+(sg-target)*vibrance;sb=target+(sb-target)*vibrance
                }
                if(style.id=="crt"){val scan=if(y%pixelSize==0).72f else 1f;sr*=scan;sg*=scan;sb*=scan}
                if(style.id=="vhs"){
                    sr=channel(pixels[y*width+max(0,x-detailStep)],16)
                    sb=channel(pixels[y*width+min(width-1,x+detailStep)],0)
                    val scan=if(y%max(2,shortEdge/220)==0).92f else 1f;sr*=scan;sg*=scan;sb*=scan
                }
                val sl=sr*.2126f+sg*.7152f+sb*.0722f
                sr=(sl+(sr-sl)*styleSaturation-.5f)*style.contrast+.5f+style.warmth*.12f
                sg=(sl+(sg-sl)*styleSaturation-.5f)*style.contrast+.5f
                sb=(sl+(sb-sl)*styleSaturation-.5f)*style.contrast+.5f-style.warmth*.15f
                val shadows=(1-gray)*(1-gray)*style.toning;val highlights=gray*gray*style.toning
                sr+=((channel(style.shadow,16)-.5f)*shadows+(channel(style.highlight,16)-.5f)*highlights)*.3f
                sg+=((channel(style.shadow,8)-.5f)*shadows+(channel(style.highlight,8)-.5f)*highlights)*.3f
                sb+=((channel(style.shadow,0)-.5f)*shadows+(channel(style.highlight,0)-.5f)*highlights)*.3f
                val grain=noise*style.grain*.045f*(.35f+gray*(1-gray))
                sr=sr*(1-style.lift)+style.lift+grain;sg=sg*(1-style.lift)+style.lift+grain;sb=sb*(1-style.lift)+style.lift+grain
                sr=red+(sr-red)*r.styleMix;sg=green+(sg-green)*r.styleMix;sb=blue+(sb-blue)*r.styleMix
                val d=guide.sample(u,v,src)
                var faceWeight=0f;var anchor=0f;var motionWeight=0f;var mover:SceneObject?=null
                for(o in objects){
                    val weight=MotionMath.mask(o,u,v,d);if(weight<.001f)continue
                    if(o.face && r.protectFaces)faceWeight=max(faceWeight,weight)
                    if(o.role==Role.ANCHOR)anchor=max(anchor,weight)
                    else if(r.motion && weight*o.intensity>motionWeight){
                        motionWeight=weight*o.intensity;mover=o
                    }
                }
                val stability=max(faceWeight,if(r.lockAnchors)anchor else 0f)
                val far=(1-d).pow(1.5f)
                val detailGain=r.sharpness*(.35f+d*.85f)+r.relief*gain*(.16f+d*.52f)+r.separation*gain*(d-.25f)*.35f
                // Suppress large step edges rather than outlining every branch with a bright/dark rim.
                val edgeGuard=1-MotionMath.smooth(.055f,.22f,abs(detail))*.9f
                val local=(detail*detailGain*edgeGuard*detailRetention).coerceIn(-.09f,.09f)*(1-faceWeight*.8f)
                // Only deepen shadow detail already in the photograph. No depth-normal emboss.
                val contact=(min(0f,detail)*detailRetention*r.occlusion*gain*.08f*(.35f+.65f*d)*edgeGuard).coerceAtLeast(-.035f)*(1-faceWeight)
                val contrast=(r.contrast+r.separation*gain*(d-.5f)*.13f).coerceIn(-.5f,.7f)
                // Depth strength no longer turns atmosphere into an opaque milky veil.
                val haze=(r.haze*far*(.045f+.02f*r.separation)*min(gain,2.2f)).coerceAtMost(.16f)*(if(scene!=null)1-r.styleMix*.7f else 1f)
                val focalBlur=(detailRetention*MotionMath.smooth(0f,.7f,r.focus-d)*r.bokeh*(.18f+.16f*gain)*(1-edge*2).coerceIn(.2f,1f)).coerceAtMost(.8f)
                sr=sr*(1-focalBlur)+br*focalBlur;sg=sg*(1-focalBlur)+bg*focalBlur;sb=sb*(1-focalBlur)+bb*focalBlur
                val radial=sqrt((u-.5f).pow(2)+(v-.5f).pow(2))*1.414f
                val periphery=.55f+.45f*((1-r.peripheral)+r.peripheral*MotionMath.smooth(.15f,.75f,radial))
                val structure=MotionMath.smooth(.015f,.1f,abs(detail)+edge*.35f)
                // An asymmetric local edge sequence follows the object's direction. No periodic overlay.
                var directionalCue=0f
                mover?.let{o->
                    val direction=MotionMath.direction(o,u,v)
                    val step=max(1,(shortEdge/(260f+r.motionScale*260f)).roundToInt())
                    val dx=(direction.x*step).roundToInt();val dy=(direction.y*step).roundToInt()
                    fun at(offset:Int)=luma(pixels[(y+dy*offset).coerceIn(0,height-1)*width+(x+dx*offset).coerceIn(0,width-1)])
                    directionalCue=(.10f*at(-2)-.80f*at(-1)+.46f*gray+.56f*at(1)-.32f*at(2))*(.6f+.8f*o.speed)
                }
                val motionGain=r.motionAmount*(.65f+.45f*r.motionAmount)
                val illusion=(directionalCue*motionWeight*motionGain*structure*periphery*(1-stability)*
                    ((1-r.depthCoupling)+r.depthCoupling*(.45f+d*.55f))*geometry).coerceIn(-.13f,.13f)
                val micro=detail*detailRetention*noise*r.texture*.12f*d*(1-faceWeight)
                val tone=local+contact+illusion*detailRetention+micro+r.exposure*.3f-r.vignette*radial.pow(2)*.22f
                sr=depthTone(sr,contrast,tone);sg=depthTone(sg,contrast,tone);sb=depthTone(sb,contrast,tone)
                sr=sr*(1-haze)+.8f*haze;sg=sg*(1-haze)+.87f*haze;sb=sb*(1-haze)+.94f*haze
                val lum=sr*.2126f+sg*.7152f+sb*.0722f
                sr=lum+(sr-lum)*r.saturation;sg=lum+(sg-lum)*r.saturation;sb=lum+(sb-lum)*r.saturation
                if(scene!=null){
                    // Compress chroma into the available headroom instead of clipping bright petals/skin.
                    val l=(sr*.2126f+sg*.7152f+sb*.0722f).coerceIn(0f,1f)
                    val high=max(sr,max(sg,sb));val low=min(sr,min(sg,sb));var chroma=1f
                    if(high>1f)chroma=min(chroma,(1-l)/(high-l).coerceAtLeast(.0001f))
                    if(low<0f)chroma=min(chroma,l/(l-low).coerceAtLeast(.0001f))
                    sr=l+(sr-l)*chroma;sg=l+(sg-l)*chroma;sb=l+(sb-l)*chroma
                }
                output[index]=rgb(sr,sg,sb)
            }
        }
        return RenderResult(Bitmap.createBitmap(output,width,height,Bitmap.Config.ARGB_8888),depth)
    }

    fun effectiveDepth(raw:DepthMap,p:Project):DepthMap {
        val r=p.recipe.normalized();val result=raw.copy()
        for(y in 0 until raw.height)for(x in 0 until raw.width){
            val i=y*raw.width+x;var d=raw.values[i].safe(.5f)
            for(o in p.objects)if(o.overrideDepth&&o.enabled){
                val mask=MotionMath.mask(o,(x+.5f)/raw.width,(y+.5f)/raw.height,d);d=d*(1-mask)+o.depth*mask
            }
            // Keep shading continuous. Layer count controls intersections in the interactive viewer.
            result.values[i]=(if(r.invertDepth)1-d else d).coerceIn(0f,1f)
        }
        return result
    }
    fun depthBitmap(depth:DepthMap):Bitmap = Bitmap.createBitmap(IntArray(depth.values.size){val d=(depth.values[it].safe(.5f)*255).roundToInt();0xff000000.toInt()or(d shl 16)or(d shl 8)or d},depth.width,depth.height,Bitmap.Config.ARGB_8888)

    private class GuidedDepth(source:Bitmap,val map:DepthMap) {
        private val colors=IntArray(map.values.size)
        init{val guide=Bitmap.createScaledBitmap(source,map.width,map.height,true);guide.getPixels(colors,0,map.width,0,0,map.width,map.height);if(guide!==source)guide.recycle()}
        private fun weight(index:Int,color:Int):Float {
            val delta=(abs(((color shr 16)and 255)-((colors[index] shr 16)and 255))+abs(((color shr 8)and 255)-((colors[index] shr 8)and 255))+abs((color and 255)-(colors[index]and 255)))/765f
            return 1f/(1f+80f*delta*delta)
        }
        fun sample(u:Float,v:Float,color:Int):Float {
            val x=u*(map.width-1);val y=v*(map.height-1);val ix=x.toInt();val iy=y.toInt();val fx=x-ix;val fy=y-iy
            val a=iy*map.width+ix;val b=iy*map.width+min(ix+1,map.width-1)
            val c=min(iy+1,map.height-1)*map.width+ix;val d=min(iy+1,map.height-1)*map.width+min(ix+1,map.width-1)
            val wa=(1-fx)*(1-fy)*weight(a,color);val wb=fx*(1-fy)*weight(b,color)
            val wc=(1-fx)*fy*weight(c,color);val wd=fx*fy*weight(d,color)
            return (map.values[a]*wa+map.values[b]*wb+map.values[c]*wc+map.values[d]*wd)/(wa+wb+wc+wd).coerceAtLeast(.00001f)
        }
    }
    private suspend fun boxBlur(pixels:IntArray,w:Int,h:Int,radius:Int):IntArray {
        val tmp=IntArray(pixels.size);val result=IntArray(pixels.size);val span=radius*2+1;val ctx=currentCoroutineContext()
        for(pass in 0..1){
            val source=if(pass==0)pixels else tmp;val target=if(pass==0)tmp else result
            val rows=if(pass==0)h else w;val length=if(pass==0)w else h;val step=if(pass==0)1 else w
            for(row in 0 until rows){
                if(row%32==0)ctx.ensureActive()
                val start=if(pass==0)row*w else row
                var rr=0;var gg=0;var bb=0
                for(k in -radius..radius){val c=source[start+k.coerceIn(0,length-1)*step];rr+=(c shr 16)and 255;gg+=(c shr 8)and 255;bb+=c and 255}
                for(i in 0 until length){
                    target[start+i*step]=0xff000000.toInt()or((rr/span)shl 16)or((gg/span)shl 8)or(bb/span)
                    val a=source[start+(i-radius).coerceIn(0,length-1)*step];val b=source[start+(i+radius+1).coerceIn(0,length-1)*step]
                    rr+=((b shr 16)and 255)-((a shr 16)and 255);gg+=((b shr 8)and 255)-((a shr 8)and 255);bb+=(b and 255)-(a and 255)
                }
            }
        }
        return result
    }
    private fun depthTone(value:Float,contrast:Float,tone:Float):Float {
        val t=value.coerceIn(0f,1f)
        val curved=t+contrast*(t-.5f)*4f*t*(1-t)
        val headroom=if(tone>0f)min(1f,(1-curved)*3f) else min(1f,curved*3f)
        return curved+tone*headroom
    }
    private fun printDot(x:Float,y:Float,cell:Float,angle:Float,density:Float):Float {
        val u=(x*cos(angle)-y*sin(angle))/cell;val v=(x*sin(angle)+y*cos(angle))/cell
        val a=u-floor(u)-.5f;val b=v-floor(v)-.5f;val radius=sqrt(density.coerceIn(0f,1f))*.71f
        return 1-MotionMath.smooth(radius-.035f,radius+.035f,hypot(a,b))
    }
    private fun nearestPalette(palette:IntArray,r:Float,g:Float,b:Float):Int {
        var best=palette[0];var distance=Float.MAX_VALUE
        for(c in palette){val d=(r-channel(c,16)).pow(2)*.3f+(g-channel(c,8)).pow(2)*.59f+(b-channel(c,0)).pow(2)*.11f;if(d<distance){distance=d;best=c}}
        return best
    }
    private fun channel(c:Int,shift:Int)=((c shr shift)and 255)/255f
    private fun luma(c:Int)=( ((c shr 16)and 255)*.2126f+((c shr 8)and 255)*.7152f+(c and 255)*.0722f)/255f
    private fun quant(x:Float,n:Int)=round(x*(n-1))/(n-1)
    private fun softQuant(x:Float,n:Int):Float {val a=x.coerceIn(0f,1f)*(n-1);val low=floor(a);val f=a-low;return (low+MotionMath.smooth(.18f,.82f,f))/(n-1)}
    private fun noise(x:Int,y:Int):Float {var n=x*374761393+y*668265263;n=(n xor(n ushr 13))*1274126177;return (n and 65535)/32767.5f-1f}
    private fun rgb(r:Float,g:Float,b:Float)=0xff000000.toInt()or((r.coerceIn(0f,1f)*255).roundToInt() shl 16)or((g.coerceIn(0f,1f)*255).roundToInt() shl 8)or(b.coerceIn(0f,1f)*255).roundToInt()
}
