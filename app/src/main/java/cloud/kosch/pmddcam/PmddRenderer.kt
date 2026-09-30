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
        if(o.id==999)return 1-smooth(.18f,.52f,d)
        val w=(o.right-o.left).coerceAtLeast(.001f);val h=(o.bottom-o.top).coerceAtLeast(.001f)
        val x=(u-o.left)/w;val y=(v-o.top)/h
        if(x !in 0f..1f || y !in 0f..1f)return 0f
        val edge=smooth(0f,.16f,min(min(x,1-x),min(y,1-y)))
        if(o.face || o.overrideDepth)return edge
        return edge*(1-smooth(.18f,.5f,abs(d-o.depth)))
    }
    fun wave(phase:Float):Float {
        // Continuous, zero-mean asymmetry: no hard luminance steps or stripe edges.
        val a=(phase-floor(phase))*2f*PI.toFloat()
        return -sin(a)*.78f-sin(2*a+.65f)*.22f
    }
    fun signal(o:SceneObject,u:Float,v:Float,scale:Float,samplingEdge:Float=1024f):Float {
        val x=u-(o.left+o.right)/2;val y=v-(o.top+o.bottom)/2
        val angle=o.angle*PI.toFloat()/180
        val frequency=min(18f+scale*50f+o.speed*22f,max(2f,samplingEdge/8f))
        val along=x*cos(angle)+y*sin(angle);val radius=sqrt(x*x+y*y)
        val phase=when(o.motion) {
            Motion.APPROACH -> -radius*frequency
            Motion.RETREAT -> radius*frequency
            Motion.ROTATE -> atan2(y,x)*frequency*.13f+radius*frequency*.15f
            Motion.PULSE -> radius*frequency+sin(radius*25)*.35f
            Motion.FLOW -> along*frequency+sin((x*sin(angle)-y*cos(angle))*25)*.7f
            Motion.DRIFT -> along*frequency
        }
        return wave(phase)*.83f+wave(phase*.51f+.18f)*.17f
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
        val scene=if(style.id=="vivid" && r.styleMix>0f)SceneTone.create(source) else null
        val objects=project.objects.filter{it.enabled}
        val context=currentCoroutineContext()
        val invW=1f/width;val invH=1f/height
        val detailStep=max(1,shortEdge/420)
        val gain=r.depth*(.8f+.65f*r.depth)
        val geometry=(r.screenSize/16f*45f/r.viewDistance).coerceIn(.4f,2f)
        val pixelSize=max(1,shortEdge/if(style.id=="pixel16")180 else 100)
        val printCell=max(2.5f,shortEdge/75f)
        val styleSaturation=if(style.id=="matrix"||style.id=="gameboy"||style.id=="blueprint")1f else style.saturation
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
                val pr=red+(br-red)*blend;val pg=green+(bg-green)*blend;val pb=blue+(bb-blue)*blend
                var sr=red;var sg=green;var sb=blue
                when(style.technique){
                    Technique.PHOTO -> Unit
                    Technique.COMIC -> {
                        val ink=1-MotionMath.smooth(.025f,.19f,edge)*style.ink
                        sr=softQuant(pr,style.levels)*ink;sg=softQuant(pg,style.levels)*ink;sb=softQuant(pb,style.levels)*ink
                    }
                    Technique.WATERCOLOR -> {
                        val pigment=1-MotionMath.smooth(.018f,.2f,edge)*.16f
                        val grain=paper*.018f*(1-gray)
                        sr=softQuant(pr,style.levels)*pigment*.89f+.11f+grain
                        sg=softQuant(pg,style.levels)*pigment*.89f+.11f+grain
                        sb=softQuant(pb,style.levels)*pigment*.89f+.11f+grain
                    }
                    Technique.OIL -> {
                        // Stroke variation follows existing detail, never a global sine overlay.
                        val stroke=paper*abs(detail)*.18f
                        sr=softQuant(pr,style.levels)+detail*.2f+stroke
                        sg=softQuant(pg,style.levels)+detail*.2f+stroke
                        sb=softQuant(pb,style.levels)+detail*.2f+stroke
                    }
                    Technique.INK -> {
                        val line=(MotionMath.smooth(.015f,.17f,edge)*.9f+(1-gray)*.08f).coerceIn(0f,1f)
                        if(style.id=="blueprint"){sr=.025f+line*.83f;sg=.12f+line*.8f;sb=.28f+line*.7f}
                        else {sr=1-line;sg=sr;sb=sr}
                    }
                    Technique.PENCIL -> {
                        val line=(1-MotionMath.smooth(.009f,.18f,edge)*.85f-max(0f,-detail)*1.4f-(1-gray)*.1f+noise*.024f*(1-gray)).coerceIn(0f,1f)
                        val color=if(style.id=="coloredpencil").6f else .06f
                        sr=line*(1-color)+red*color;sg=line*(1-color)+green*color;sb=line*(1-color)+blue*color
                    }
                    Technique.HATCH -> {
                        val line=1-MotionMath.smooth(.03f,.25f,abs(sin((x+y)/printCell*PI.toFloat())))
                        val cross=1-MotionMath.smooth(.03f,.25f,abs(sin((x-y)/printCell*PI.toFloat())))
                        val mark=line*MotionMath.smooth(.2f,.8f,1-gray)+cross*MotionMath.smooth(.65f,.95f,1-gray)
                        val value=(.96f-mark*.6f-edge*.6f+noise*.015f).coerceIn(0f,1f)
                        sr=value;sg=value;sb=value
                    }
                    Technique.HALFTONE -> {
                        val cx=(x/printCell-floor(x/printCell))-.5f;val cy=(y/printCell-floor(y/printCell))-.5f
                        val dot=1-MotionMath.smooth((1-gray)*.32f-.025f,(1-gray)*.32f+.025f,cx*cx+cy*cy)
                        val ink=1-dot*.58f
                        sr=softQuant(pr,style.levels)*.72f+.28f;sg=softQuant(pg,style.levels)*.72f+.28f;sb=softQuant(pb,style.levels)*.72f+.28f
                        sr*=ink;sg*=ink;sb*=ink
                    }
                    Technique.NEON -> {
                        val glow=(MotionMath.smooth(.012f,.22f,edge)*.62f+max(0f,detail)*1.3f).coerceIn(0f,.85f)
                        val base=if(style.id=="hologram").75f else .56f
                        if(style.id=="matrix"){sr=gray*.035f;sg=gray*.48f;sb=gray*.09f}
                        else{sr=red*base;sg=green*base;sb=blue*base}
                        sr+=channel(style.tint,16)*glow;sg+=channel(style.tint,8)*glow;sb+=channel(style.tint,0)*glow
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
                        if(style.id=="gameboy"){
                            val color=pocketPalette[(luma(sample)*3).roundToInt().coerceIn(0,3)]
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
                if(style.id=="vhs"){
                    sr=channel(pixels[y*width+max(0,x-detailStep)],16)
                    sb=channel(pixels[y*width+min(width-1,x+detailStep)],0)
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
                var faceWeight=0f;var anchor=0f;var motion=0f
                for(o in objects){
                    val weight=MotionMath.mask(o,u,v,d);if(weight<.001f)continue
                    if(o.face && r.protectFaces)faceWeight=max(faceWeight,weight)
                    if(o.role==Role.ANCHOR)anchor=max(anchor,weight)
                    else if(r.motion){
                        // Atmosphere uses photographed directional detail, not a screen-wide pattern.
                        val signal=if(o.id==999){
                            val a=o.angle*PI.toFloat()/180
                            ((gx*cos(a)+gy*sin(a))*6f).coerceIn(-1f,1f)
                        } else MotionMath.signal(o,u,v,r.motionScale,shortEdge.toFloat())
                        motion+=signal*weight*o.intensity
                    }
                }
                val stability=max(faceWeight,if(r.lockAnchors)anchor else 0f)
                val far=(1-d).pow(1.5f)
                val detailGain=r.sharpness*(.35f+d*.85f)+r.relief*gain*(.16f+d*.52f)+r.separation*gain*(d-.25f)*.35f
                // Suppress large step edges rather than outlining every branch with a bright/dark rim.
                val edgeGuard=1-MotionMath.smooth(.055f,.22f,abs(detail))*.9f
                val local=(detail*detailGain*edgeGuard).coerceIn(-.09f,.09f)*(1-faceWeight*.8f)
                // Only deepen shadow detail already in the photograph. No depth-normal emboss.
                val contact=(min(0f,detail)*r.occlusion*gain*.08f*(.35f+.65f*d)*edgeGuard).coerceAtLeast(-.035f)*(1-faceWeight)
                val contrast=(r.contrast+r.separation*gain*(d-.5f)*.13f).coerceIn(-.5f,.7f)
                // Depth strength no longer turns atmosphere into an opaque milky veil.
                val haze=(r.haze*far*(.045f+.02f*r.separation)*min(gain,2.2f)).coerceAtMost(.16f)*(if(scene!=null)1-r.styleMix*.7f else 1f)
                val focalBlur=(MotionMath.smooth(0f,.7f,r.focus-d)*r.bokeh*(.18f+.16f*gain)*(1-edge*2).coerceIn(.2f,1f)).coerceAtMost(.8f)
                sr=sr*(1-focalBlur)+br*focalBlur;sg=sg*(1-focalBlur)+bg*focalBlur;sb=sb*(1-focalBlur)+bb*focalBlur
                val radial=sqrt((u-.5f).pow(2)+(v-.5f).pow(2))*1.414f
                val periphery=(1-r.peripheral)+r.peripheral*MotionMath.smooth(.15f,.75f,radial)
                val structure=MotionMath.smooth(.015f,.1f,abs(detail)+edge*.35f)
                val illusion=(motion.coerceIn(-1.2f,1.2f)*detail*r.motionAmount*1.7f*structure*periphery*(1-stability)*
                    ((1-r.depthCoupling)+r.depthCoupling*(.3f+d*.7f))*geometry).coerceIn(-.16f,.16f)
                val micro=detail*noise*r.texture*.12f*d*(1-faceWeight)
                val tone=local+contact+illusion+micro+r.exposure*.3f-r.vignette*radial.pow(2)*.22f
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
    private fun channel(c:Int,shift:Int)=((c shr shift)and 255)/255f
    private fun luma(c:Int)=( ((c shr 16)and 255)*.2126f+((c shr 8)and 255)*.7152f+(c and 255)*.0722f)/255f
    private fun quant(x:Float,n:Int)=round(x*(n-1))/(n-1)
    private fun softQuant(x:Float,n:Int):Float {val a=x.coerceIn(0f,1f)*(n-1);val low=floor(a);val f=a-low;return (low+MotionMath.smooth(.18f,.82f,f))/(n-1)}
    private fun noise(x:Int,y:Int):Float {var n=x*374761393+y*668265263;n=(n xor(n ushr 13))*1274126177;return (n and 65535)/32767.5f-1f}
    private fun rgb(r:Float,g:Float,b:Float)=0xff000000.toInt()or((r.coerceIn(0f,1f)*255).roundToInt() shl 16)or((g.coerceIn(0f,1f)*255).roundToInt() shl 8)or(b.coerceIn(0f,1f)*255).roundToInt()
}
