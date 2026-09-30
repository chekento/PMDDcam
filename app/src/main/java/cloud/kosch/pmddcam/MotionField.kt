package cloud.kosch.pmddcam

import android.graphics.Bitmap
import kotlin.math.*

/** Object-bound direction/strength texture; anchors receive no independent displacement. */
object MotionField {
    data class Textures(val vectors:Bitmap,val timing:Bitmap)
    fun texture(depth:DepthMap,objects:List<SceneObject>,recipe:Recipe):Bitmap = create(depth,objects,recipe).let{it.timing.recycle();it.vectors}
    fun create(depth:DepthMap,objects:List<SceneObject>,recipe:Recipe):Textures{
        val w=depth.width;val h=depth.height
        val timing=IntArray(w*h){0xff000000.toInt()}
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
            selected?.let{o->
                val frequency=if(o.role==Role.ATMOSPHERE).055f+.07f*o.speed else .13f+.21f*o.speed
                val phase=((o.id*.618034f)%1f+1f)%1f
                val extent=if(o.mask!=null).028f else (min(o.right-o.left,o.bottom-o.top)*.14f).coerceIn(.001f,.035f)
                timing[i]=0xff000000.toInt()or((frequency*255).roundToInt()shl 16)or((phase*255).roundToInt()shl 8)or((extent/.04f*255).roundToInt())
            }
            0xff000000.toInt() or (((direction.x*.5f+.5f)*255).roundToInt().coerceIn(0,255)shl 16) or
                (((direction.y*.5f+.5f)*255).roundToInt().coerceIn(0,255)shl 8) or (amount*255).roundToInt()
        }
        return Textures(Bitmap.createBitmap(colors,w,h,Bitmap.Config.ARGB_8888),Bitmap.createBitmap(timing,w,h,Bitmap.Config.ARGB_8888))
    }
}
