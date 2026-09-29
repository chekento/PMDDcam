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
        val edge=smooth(0f,.12f,min(min(x,1-x),min(y,1-y)))
        if(o.face)return edge
        // Depth-compatible membership, feathered boundaries. This is a soft region,
        // not a claimed semantic segmentation mask for every possible object.
        return edge*(1-smooth(.18f,.5f,abs(d-o.depth)))
    }
    fun wave(phase:Float):Float {
        val f=phase-floor(phase)
        // Static asymmetric luminance sequence; no time uniform or frame changes.
        return when {f<.22f->-1f;f<.5f->-.28f;f<.72f->1f;else->.35f}
    }
    fun signal(o:SceneObject,u:Float,v:Float,scale:Float):Float {
        val x=(u-(o.left+o.right)/2);val y=(v-(o.top+o.bottom)/2)
        val angle=o.angle*PI.toFloat()/180
        val frequency=18f+scale*72f+o.speed*30f
        val along=x*cos(angle)+y*sin(angle)
        val radius=sqrt(x*x+y*y)
        val primary=when(o.motion) {
            Motion.APPROACH -> -radius*frequency
            Motion.RETREAT -> radius*frequency
            Motion.ROTATE -> atan2(y,x)*frequency*.13f+radius*frequency*.15f
            Motion.PULSE -> radius*frequency+sin(radius*25)*.35f
            Motion.FLOW -> along*frequency+sin((x*sin(angle)-y*cos(angle))*25)*.7f
            Motion.DRIFT -> along*frequency
        }
        // Secondary detail and trailing energy share the primary direction.
        val secondary=wave(primary*.51f+.18f)*.17f
        return wave(primary)*.83f+secondary
    }
}

data class RenderResult(val image:Bitmap,val depth:DepthMap)

object PmddRenderer {
    suspend fun render(source:Bitmap,raw:DepthMap,project:Project):RenderResult {
        val r=project.recipe.normalized();val style=Styles.get(r.style)
        val styleSaturation=if(style.id=="matrix"||style.id=="gameboy"||style.id=="blueprint")1f else style.saturation
        val width=source.width;val height=source.height
        val pixels=IntArray(width*height);source.getPixels(pixels,0,width,0,0,width,height)
        val output=IntArray(pixels.size)
        val blurSmall=Bitmap.createScaledBitmap(source,max(1,width/24),max(1,height/24),true)
        val blur=Bitmap.createScaledBitmap(blurSmall,width,height,true)
        if(blurSmall!==blur)blurSmall.recycle()
        val soft=IntArray(pixels.size);blur.getPixels(soft,0,width,0,0,width,height);if(blur!==source)blur.recycle()
        val depth=effectiveDepth(raw,project)
        val objects=project.objects.filter{it.enabled}
        val context=currentCoroutineContext()
        val invW=1f/width;val invH=1f/height
        for(y in 0 until height){
            if(y%16==0)context.ensureActive()
            val v=(y+.5f)*invH
            for(x in 0 until width){
                val u=(x+.5f)*invW;val index=y*width+x
                var src=pixels[index]
                if(style.technique==Technique.PIXEL){
                    val size=max(2,width/140);src=pixels[(y/size*size)*width+(x/size*size)]
                }
                val red=((src shr 16)and 255)/255f;val green=((src shr 8)and 255)/255f;val blue=(src and 255)/255f
                val c=soft[index];val br=((c shr 16)and 255)/255f;val bg=((c shr 8)and 255)/255f;val bb=(c and 255)/255f
                val gray=red*.2126f+green*.7152f+blue*.0722f
                val smoothGray=br*.2126f+bg*.7152f+bb*.0722f
                val left=pixels[y*width+max(0,x-1)];val right=pixels[y*width+min(width-1,x+1)]
                val up=pixels[max(0,y-1)*width+x];val down=pixels[min(height-1,y+1)*width+x]
                val edge=(abs(luma(left)-luma(right))+abs(luma(up)-luma(down))).coerceIn(0f,1f)
                var sr=red;var sg=green;var sb=blue
                val noise=noise(x,y)
                when(style.technique){
                    Technique.PHOTO -> Unit
                    Technique.COMIC -> {sr=quant(red,style.levels);sg=quant(green,style.levels);sb=quant(blue,style.levels)
                        val ink=(1-edge*3.4f).coerceIn(.12f,1f);sr*=ink;sg*=ink;sb*=ink}
                    Technique.WATERCOLOR -> {sr=quant(red*.35f+br*.65f,style.levels)*.86f+.14f;sg=quant(green*.35f+bg*.65f,style.levels)*.86f+.14f;sb=quant(blue*.35f+bb*.65f,style.levels)*.86f+.14f
                        val pigment=1-edge*.3f+noise*.025f;sr*=pigment;sg*=pigment;sb*=pigment}
                    Technique.OIL -> {val stroke=sin(x*.7f+y*.25f)*.012f;sr=quant(red*.6f+br*.4f,style.levels)+stroke;sg=quant(green*.6f+bg*.4f,style.levels)+stroke;sb=quant(blue*.6f+bb*.4f,style.levels)+stroke}
                    Technique.INK -> {val value=(1-edge*5-(1-gray)*.12f).coerceIn(0f,1f)
                        if(style.id=="blueprint"){sr=.05f+(1-value)*.55f;sg=.14f+(1-value)*.65f;sb=.3f+(1-value)*.68f}else{sr=value;sg=value;sb=value}}
                    Technique.PENCIL -> {val line=(gray/(smoothGray+.04f)).coerceIn(0f,1f)*(1-edge*.9f)
                        sr=line*.83f+red*.17f;sg=line*.83f+green*.17f;sb=line*.83f+blue*.17f}
                    Technique.HATCH -> {val hatch=if((x+y)%7<2 || (gray<.3f&&(x-y+width)%9<2)) .5f else 1f
                        val value=(gray*.4f+.6f)*if(gray<.75f)hatch else 1f;sr=value;sg=value;sb=value}
                    Technique.HALFTONE -> {val sx=(x%7-3)/3f;val sy=(y%7-3)/3f;val dot=if(sx*sx+sy*sy<(1-gray)*1.8f).55f else 1.1f
                        sr=quant(red,style.levels)*dot;sg=quant(green,style.levels)*dot;sb=quant(blue,style.levels)*dot}
                    Technique.NEON -> {val glow=(edge*2.1f+max(0f,gray-smoothGray)*1.8f).coerceIn(0f,.75f)
                        sr=red*.72f+((style.tint shr 16)and 255)/255f*glow;sg=green*.72f+((style.tint shr 8)and 255)/255f*glow;sb=blue*.72f+(style.tint and 255)/255f*glow}
                    Technique.DUOTONE -> {sr=gray*(.25f+((style.tint shr 16)and 255)/255f*.85f);sg=gray*(.25f+((style.tint shr 8)and 255)/255f*.85f);sb=gray*(.25f+(style.tint and 255)/255f*.85f)}
                    Technique.SOLAR -> {sr=if(red>.55f)1-red else red;sg=if(green>.65f)1-green else green;sb=if(blue>.5f)1-blue else blue;sr*=1.6f;sg*=1.6f;sb*=1.6f}
                    Technique.PIXEL -> {sr=quant(red,style.levels);sg=quant(green,style.levels);sb=quant(blue,style.levels)
                        if(style.id=="gameboy"){val q=quant(gray,4);sr=q*.6f;sg=q*.75f;sb=q*.27f}}
                }
                val sl=sr*.2126f+sg*.7152f+sb*.0722f
                val saturation=styleSaturation
                sr=((sl+(sr-sl)*saturation-.5f)*style.contrast+.5f)+style.warmth*.12f+noise*style.grain*.08f
                sg=(sl+(sg-sl)*saturation-.5f)*style.contrast+.5f+noise*style.grain*.08f
                sb=((sl+(sb-sl)*saturation-.5f)*style.contrast+.5f)-style.warmth*.15f+noise*style.grain*.08f
                sr=red+(sr-red)*r.styleMix;sg=green+(sg-green)*r.styleMix;sb=blue+(sb-blue)*r.styleMix
                val d=depth.sample(u,v)
                var faceWeight=0f;var anchor=0f;var motion=0f
                for(o in objects){
                    val weight=MotionMath.mask(o,u,v,d)
                    if(weight<.001f)continue
                    if(o.face && r.protectFaces)faceWeight=max(faceWeight,weight)
                    if(o.role==Role.ANCHOR)anchor=max(anchor,weight)
                    else if(r.motion)motion+=MotionMath.signal(o,u,v,r.motionScale)*weight*o.intensity
                }
                val stability=max(faceWeight,if(r.lockAnchors)anchor else 0f)
                val far=(1-d).pow(1.5f)
                val dx=depth.sample(u+.004f,v)-depth.sample(u-.004f,v)
                val dy=depth.sample(u,v+.004f)-depth.sample(u,v-.004f)
                val relief=((-dx-dy)*r.relief*1.1f).coerceIn(-.16f,.16f)*(1-faceWeight)
                val local=(gray-smoothGray)*r.sharpness*(.3f+d)*.9f
                val contact=-abs(dx+dy)*r.occlusion*.7f
                val contrast=1+r.contrast+r.separation*(d-.35f)*r.depth*.42f
                val haze=r.haze*far*.36f*r.depth
                val focalBlur=((r.focus-d).coerceAtLeast(0f))*r.bokeh*.6f
                sr=sr*(1-focalBlur)+br*focalBlur;sg=sg*(1-focalBlur)+bg*focalBlur;sb=sb*(1-focalBlur)+bb*focalBlur
                val radial=sqrt((u-.5f).pow(2)+(v-.5f).pow(2))*1.414f
                val periphery=(1-r.peripheral)+r.peripheral*MotionMath.smooth(.15f,.75f,radial)
                val geometry=(r.screenSize/16f*45f/r.viewDistance).coerceIn(.4f,2f)
                val illusion=motion.coerceIn(-1.2f,1.2f)*r.motionAmount*.075f*periphery*(1-stability)*
                    ((1-r.depthCoupling)+r.depthCoupling*(.3f+d*.7f))*geometry
                val micro=noise*r.texture*.012f*d*(1-faceWeight)
                val tone=relief+local+contact+illusion+micro+r.exposure*.3f-r.vignette*radial.pow(2)*.22f
                sr=(sr-.5f)*contrast+.5f+tone;sg=(sg-.5f)*contrast+.5f+tone;sb=(sb-.5f)*contrast+.5f+tone
                sr=sr*(1-haze)+.78f*haze;sg=sg*(1-haze)+.86f*haze;sb=sb*(1-haze)+.94f*haze
                val lum=sr*.2126f+sg*.7152f+sb*.0722f
                output[index]=rgb(lum+(sr-lum)*r.saturation,lum+(sg-lum)*r.saturation,lum+(sb-lum)*r.saturation)
            }
        }
        return RenderResult(Bitmap.createBitmap(output,width,height,Bitmap.Config.ARGB_8888),depth)
    }

    fun effectiveDepth(raw:DepthMap,p:Project):DepthMap {
        val r=p.recipe.normalized();val result=raw.copy()
        for(y in 0 until raw.height)for(x in 0 until raw.width){
            val i=y*raw.width+x;var d=raw.values[i]
            for(o in p.objects)if(o.overrideDepth&&o.enabled){val mask=MotionMath.mask(o,x.toFloat()/raw.width,y.toFloat()/raw.height,d);d=d*(1-mask)+o.depth*mask}
            if(r.invertDepth)d=1-d
            // Smooth high-layer stack: one quantized depth field shared by effects and viewer.
            d=(round(d*(r.layers-1))/(r.layers-1)).coerceIn(0f,1f)
            result.values[i]=d
        }
        return result
    }
    fun depthBitmap(depth:DepthMap):Bitmap = Bitmap.createBitmap(IntArray(depth.values.size){val d=(depth.values[it].safe(.5f)*255).roundToInt();0xff000000.toInt() or(d shl 16)or(d shl 8)or d},depth.width,depth.height,Bitmap.Config.ARGB_8888)
    private fun luma(c:Int)=(((c shr 16)and 255)*.2126f+((c shr 8)and 255)*.7152f+(c and 255)*.0722f)/255f
    private fun quant(x:Float,n:Int)=round(x*(n-1))/(n-1)
    private fun noise(x:Int,y:Int):Float {var n=x*374761393+y*668265263;n=(n xor(n ushr 13))*1274126177;return (n and 65535)/32767.5f-1f}
    private fun rgb(r:Float,g:Float,b:Float)=0xff000000.toInt()or((r.coerceIn(0f,1f)*255).roundToInt() shl 16)or((g.coerceIn(0f,1f)*255).roundToInt() shl 8)or(b.coerceIn(0f,1f)*255).roundToInt()
}
