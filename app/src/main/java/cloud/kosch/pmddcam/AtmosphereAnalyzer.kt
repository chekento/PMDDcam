package cloud.kosch.pmddcam

import android.graphics.Bitmap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.*

/** Conservative photographic candidates, explicitly labelled as suggestions rather than semantic certainty. */
object AtmosphereAnalyzer {
    suspend fun detect(photo:Bitmap,depth:DepthMap):List<SceneObject>{
        val scale=128f/max(photo.width,photo.height);val w=max(8,(photo.width*scale).roundToInt());val h=max(8,(photo.height*scale).roundToInt())
        val small=Bitmap.createScaledBitmap(photo,w,h,true);val colors=IntArray(w*h)
        small.getPixels(colors,0,w,0,0,w,h);if(small!==photo)small.recycle()
        val lum=FloatArray(w*h);val neutral=FloatArray(w*h);val candidate=BooleanArray(w*h);val blue=BooleanArray(w*h)
        for(y in 0 until h)for(x in 0 until w){
            val i=y*w+x;val c=colors[i];val r=((c shr 16)and 255)/255f;val g=((c shr 8)and 255)/255f;val b=(c and 255)/255f
            lum[i]=r*.2126f+g*.7152f+b*.0722f;neutral[i]=max(r,max(g,b))-min(r,min(g,b))
            blue[i]=b-r>.07f && b-g>.025f && b>.35f
            candidate[i]=y<h*.8f && depth.sample(x.toFloat()/(w-1),y.toFloat()/(h-1))<.3f &&
                (blue[i] || (neutral[i]<.16f && lum[i]>.43f))
        }
        // Flood only top-connected sky. Bright buildings/roads near the viewer cannot seed atmosphere.
        val sky=BooleanArray(w*h);val queue=IntArray(w*h);var first=0;var last=0
        fun add(i:Int){if(candidate[i]&&!sky[i]){sky[i]=true;queue[last++]=i}}
        for(x in 0 until w)add(x)
        for(y in 0 until h/3){add(y*w);add(y*w+w-1)}
        while(first<last){val i=queue[first++];val x=i%w;val y=i/w;if(x>0)add(i-1);if(x+1<w)add(i+1);if(y>0)add(i-w);if(y+1<h)add(i+w)}
        val clouds=FloatArray(w*h);val mist=FloatArray(w*h);val context=currentCoroutineContext()
        for(y in 1 until h-1){
            context.ensureActive()
            for(x in 1 until w-1){
                val i=y*w+x;if(!sky[i]||blue[i])continue
                val edge=max(abs(lum[i+1]-lum[i-1]),abs(lum[i+w]-lum[i-w]))
                val confidence=(1-MotionMath.smooth(.08f,.17f,neutral[i]))*(1-MotionMath.smooth(.07f,.20f,edge))
                val cloud=MotionMath.smooth(.5f,.78f,lum[i])
                clouds[i]=confidence*cloud
                if(y>h*.18f)mist[i]=confidence*(1-cloud)*MotionMath.smooth(.43f,.57f,lum[i])*.75f
            }
        }
        val result=mutableListOf<SceneObject>()
        fun region(values:FloatArray,id:Int,name:String,motion:Motion,intensity:Float){
            if(values.count{it>.2f}<max(12,(w*h*.003f).toInt()))return
            // Feather inward; never dilate a weather effect onto architecture.
            val bytes=ByteArray(values.size)
            for(y in 1 until h-1)for(x in 1 until w-1){val i=y*w+x;var sum=0f;for(dy in -1..1)for(dx in -1..1)sum+=values[i+dy*w+dx];bytes[i]=(min(values[i],sum/9)*255).roundToInt().toByte()}
            result+=SceneObject(id,name,0f,0f,1f,1f,Role.ATMOSPHERE,motion,8f,.36f,intensity,.12f,mask=RegionMask(w,h,bytes))
        }
        region(clouds,2000,"Wolken · Vorschlag",Motion.DRIFT,.65f)
        region(mist,2001,"Dunst / Nebel · Vorschlag",Motion.FLOW,.4f)
        return result
    }
}
