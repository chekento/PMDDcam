package cloud.kosch.pmddcam

import android.graphics.Bitmap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.*

/** Low-resolution edge-preserving illumination statistics; no invented geometry or light sources. */
internal class SceneTone private constructor(
    private val width:Int,private val height:Int,private val a:FloatArray,private val b:FloatArray,
    val black:Float,val white:Float
) {
    fun base(u:Float,v:Float,luma:Float):Float = (sample(a,u,v)*luma+sample(b,u,v)).coerceIn(0f,1f)
    private fun sample(values:FloatArray,u:Float,v:Float):Float {
        val x=(u*(width-1)).coerceIn(0f,(width-1).toFloat());val y=(v*(height-1)).coerceIn(0f,(height-1).toFloat())
        val ix=x.toInt();val iy=y.toInt();val fx=x-ix;val fy=y-iy
        val nx=min(ix+1,width-1);val ny=min(iy+1,height-1)
        return (values[iy*width+ix]*(1-fx)+values[iy*width+nx]*fx)*(1-fy)+(values[ny*width+ix]*(1-fx)+values[ny*width+nx]*fx)*fy
    }
    companion object {
        suspend fun create(source:Bitmap):SceneTone {
            val scale=min(1f,512f/max(source.width,source.height))
            val w=max(1,(source.width*scale).roundToInt());val h=max(1,(source.height*scale).roundToInt())
            val small=Bitmap.createScaledBitmap(source,w,h,true);val colors=IntArray(w*h)
            small.getPixels(colors,0,w,0,0,w,h);if(small!==source)small.recycle()
            val luma=FloatArray(colors.size){i->val c=colors[i];(((c shr 16)and 255)*.2126f+((c shr 8)and 255)*.7152f+(c and 255)*.0722f)/255f}
            val histogram=IntArray(256);luma.forEach{histogram[(it*255).roundToInt().coerceIn(0,255)]++}
            fun percentile(fraction:Float):Float{val target=(luma.size*fraction).roundToInt().coerceAtLeast(1);var count=0;for(i in histogram.indices){count+=histogram[i];if(count>=target)return i/255f};return 1f}
            val radius=max(3,min(w,h)/22)
            val mean=mean(luma,w,h,radius);val squared=mean(FloatArray(luma.size){luma[it]*luma[it]},w,h,radius)
            val aa=FloatArray(luma.size);val bb=FloatArray(luma.size)
            for(i in luma.indices){val variance=max(0f,squared[i]-mean[i]*mean[i]);aa[i]=variance/(variance+.0016f);bb[i]=mean[i]*(1-aa[i])}
            return SceneTone(w,h,mean(aa,w,h,radius),mean(bb,w,h,radius),min(.055f,percentile(.01f))*.8f,max(.92f,percentile(.995f)))
        }
        private suspend fun mean(values:FloatArray,w:Int,h:Int,radius:Int):FloatArray{
            val tmp=FloatArray(values.size);val result=FloatArray(values.size);val span=radius*2+1;val ctx=currentCoroutineContext()
            for(pass in 0..1){
                val src=if(pass==0)values else tmp;val dst=if(pass==0)tmp else result
                val rows=if(pass==0)h else w;val length=if(pass==0)w else h;val step=if(pass==0)1 else w
                for(row in 0 until rows){
                    if(row%32==0)ctx.ensureActive()
                    val start=if(pass==0)row*w else row;var sum=0f
                    for(k in -radius..radius)sum+=src[start+k.coerceIn(0,length-1)*step]
                    for(i in 0 until length){dst[start+i*step]=sum/span;sum+=src[start+(i+radius+1).coerceIn(0,length-1)*step]-src[start+(i-radius).coerceIn(0,length-1)*step]}
                }
            };return result
        }
    }
}
