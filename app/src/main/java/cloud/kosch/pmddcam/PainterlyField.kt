package cloud.kosch.pmddcam

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.*

/** Edge-preserving paint regions, computed at a bounded resolution independent of export size. */
internal class PainterlyField private constructor(val width:Int,val height:Int,private val colors:IntArray) {
    fun color(u:Float,v:Float):Int = colors[(v*height).toInt().coerceIn(0,height-1)*width+(u*width).toInt().coerceIn(0,width-1)]
    fun edge(u:Float,v:Float):Float {
        val x=(u*width).toInt().coerceIn(0,width-1);val y=(v*height).toInt().coerceIn(0,height-1)
        fun lum(x:Int,y:Int):Float{val c=colors[y.coerceIn(0,height-1)*width+x.coerceIn(0,width-1)];return (((c shr 16)and 255)*.2126f+((c shr 8)and 255)*.7152f+(c and 255)*.0722f)/255f}
        return hypot(lum(x+1,y)-lum(x-1,y),lum(x,y+1)-lum(x,y-1))
    }
    companion object {
        suspend fun create(source:IntArray,w:Int,h:Int,technique:Technique):PainterlyField? {
            if(technique !in setOf(Technique.COMIC,Technique.WATERCOLOR,Technique.OIL,Technique.PASTEL,Technique.POSTER))return null
            val scale=min(1f,720f/max(w,h));val fw=max(1,(w*scale).roundToInt());val fh=max(1,(h*scale).roundToInt())
            val stride=fw+1;val size=stride*(fh+1)
            val rr=FloatArray(size);val gg=FloatArray(size);val bb=FloatArray(size);val ll=FloatArray(size)
            val ctx=currentCoroutineContext()
            for(y in 0 until fh){
                ctx.ensureActive();var r=0f;var g=0f;var b=0f;var l2=0f
                for(x in 0 until fw){
                    val c=source[min(h-1,((y+.5f)/fh*h).toInt())*w+min(w-1,((x+.5f)/fw*w).toInt())]
                    val cr=((c shr 16)and 255)/255f;val cg=((c shr 8)and 255)/255f;val cb=(c and 255)/255f;val l=cr*.2126f+cg*.7152f+cb*.0722f
                    r+=cr;g+=cg;b+=cb;l2+=l*l;val i=(y+1)*stride+x+1
                    rr[i]=rr[i-stride]+r;gg[i]=gg[i-stride]+g;bb[i]=bb[i-stride]+b;ll[i]=ll[i-stride]+l2
                }
            }
            val radius=max(2,(min(fw,fh)/when(technique){Technique.OIL->55f;Technique.WATERCOLOR->75f;Technique.PASTEL->90f;else->125f}).roundToInt())
            fun sum(a:FloatArray,x0:Int,y0:Int,x1:Int,y1:Int)=a[y1*stride+x1]-a[y0*stride+x1]-a[y1*stride+x0]+a[y0*stride+x0]
            val output=IntArray(fw*fh)
            for(y in 0 until fh){
                ctx.ensureActive()
                for(x in 0 until fw){
                    var best=Float.MAX_VALUE;var r=0f;var g=0f;var b=0f
                    for(q in 0..3){
                        val x0=max(0,if(q%2==0)x-radius else x);val x1=min(fw,if(q%2==0)x+1 else x+radius+1)
                        val y0=max(0,if(q<2)y-radius else y);val y1=min(fh,if(q<2)y+1 else y+radius+1)
                        val count=((x1-x0)*(y1-y0)).toFloat()
                        val cr=sum(rr,x0,y0,x1,y1)/count;val cg=sum(gg,x0,y0,x1,y1)/count;val cb=sum(bb,x0,y0,x1,y1)/count
                        val l=cr*.2126f+cg*.7152f+cb*.0722f;val variance=max(0f,sum(ll,x0,y0,x1,y1)/count-l*l)
                        if(variance<best){best=variance;r=cr;g=cg;b=cb}
                    }
                    output[y*fw+x]=0xff000000.toInt()or((r.coerceIn(0f,1f)*255).roundToInt()shl 16)or((g.coerceIn(0f,1f)*255).roundToInt()shl 8)or(b.coerceIn(0f,1f)*255).roundToInt()
                }
            }
            return PainterlyField(fw,fh,output)
        }
    }
}

/** Diffused photographed edges/highlights, rather than a uniform neon tint. */
internal class LightField(private val w:Int,private val h:Int,private val values:FloatArray) {
    fun sample(u:Float,v:Float):Float {
        val x=u.coerceIn(0f,1f)*(w-1);val y=v.coerceIn(0f,1f)*(h-1);val ix=x.toInt();val iy=y.toInt();val fx=x-ix;val fy=y-iy
        return (values[iy*w+ix]*(1-fx)+values[iy*w+min(w-1,ix+1)]*fx)*(1-fy)+
            (values[min(h-1,iy+1)*w+ix]*(1-fx)+values[min(h-1,iy+1)*w+min(w-1,ix+1)]*fx)*fy
    }
    companion object {
        suspend fun create(source:IntArray,width:Int,height:Int,edges:Boolean):LightField {
            val scale=min(1f,512f/max(width,height));val w=max(2,(width*scale).roundToInt());val h=max(2,(height*scale).roundToInt())
            val luma=FloatArray(w*h){i->val c=source[min(height-1,((i/w+.5f)/h*height).toInt())*width+min(width-1,((i%w+.5f)/w*width).toInt())];(((c shr 16)and 255)*.2126f+((c shr 8)and 255)*.7152f+(c and 255)*.0722f)/255f}
            var a=FloatArray(w*h){i->val x=i%w;val y=i/w
                if(edges)(abs(luma[y*w+min(w-1,x+1)]-luma[y*w+max(0,x-1)])+abs(luma[min(h-1,y+1)*w+x]-luma[max(0,y-1)*w+x])).coerceIn(0f,1f)
                else MotionMath.smooth(.52f,.95f,luma[i])}
            val radius=max(2,min(w,h)/45);val ctx=currentCoroutineContext()
            for(pass in 0..1){
                val b=FloatArray(a.size);val rows=if(pass==0)h else w;val length=if(pass==0)w else h;val step=if(pass==0)1 else w
                for(row in 0 until rows){ctx.ensureActive();val start=if(pass==0)row*w else row;var sum=0f
                    for(k in -radius..radius)sum+=a[start+k.coerceIn(0,length-1)*step]
                    for(i in 0 until length){b[start+i*step]=sum/(radius*2+1);sum+=a[start+(i+radius+1).coerceIn(0,length-1)*step]-a[start+(i-radius).coerceIn(0,length-1)*step]}
                };a=b
            };return LightField(w,h,a)
        }
    }
}
