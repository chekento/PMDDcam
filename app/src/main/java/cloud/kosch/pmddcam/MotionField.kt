package cloud.kosch.pmddcam

import android.graphics.Bitmap
import kotlin.math.*

/** Object-bound direction/strength texture; anchors receive no independent displacement. */
object MotionField {
    fun texture(depth:DepthMap,objects:List<SceneObject>,recipe:Recipe):Bitmap{
        val w=depth.width;val h=depth.height
        val colors=IntArray(w*h){i->
            val u=(i%w+.5f)/w;val v=(i/w+.5f)/h;val d=depth.values[i]
            var strength=0f;var protected=0f;var selected:SceneObject?=null
            for(o in objects){
                val weight=MotionMath.mask(o,u,v,d)
                if((o.face&&recipe.protectFaces)||(o.role==Role.ANCHOR&&recipe.lockAnchors))protected=max(protected,weight)
                if(o.role!=Role.ANCHOR && weight*o.intensity>strength){strength=weight*o.intensity;selected=o}
            }
            val direction=selected?.let{MotionMath.direction(it,u,v)}?:MotionMath.Direction(0f,0f)
            val amount=(strength*(1-protected)*(.6f+.4f*(selected?.speed?:0f))).coerceIn(0f,1f)
            0xff000000.toInt() or (((direction.x*.5f+.5f)*255).roundToInt().coerceIn(0,255)shl 16) or
                (((direction.y*.5f+.5f)*255).roundToInt().coerceIn(0,255)shl 8) or (amount*255).roundToInt()
        }
        return Bitmap.createBitmap(colors,w,h,Bitmap.Config.ARGB_8888)
    }
}
