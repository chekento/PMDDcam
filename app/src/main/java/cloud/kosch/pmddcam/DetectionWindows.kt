package cloud.kosch.pmddcam

import kotlin.math.roundToInt

/** Bounded detail passes recover small subjects lost by a single 300px full-frame input. */
internal object DetectionWindows {
    data class Window(val left:Int,val top:Int,val width:Int,val height:Int)
    data class Hit(val category:Int,val score:Float,val left:Float,val top:Float,val right:Float,val bottom:Float)

    fun forImage(width:Int,height:Int):List<Window> {
        require(width>0&&height>0)
        val full=Window(0,0,width,height)
        if(minOf(width,height)<=300)return listOf(full)
        val side=(minOf(width,height)*.64f).roundToInt().coerceAtLeast(1)
        fun starts(length:Int,count:Int)=List(count){i->((length-side)*i.toFloat()/(count-1)).roundToInt()}.distinct()
        val xs=starts(width,if(width>height)3 else 2)
        val ys=starts(height,if(height>width)3 else 2)
        return listOf(full)+ys.flatMap{y->xs.map{x->Window(x,y,side,side)}}
    }

    fun map(window:Window,imageWidth:Int,imageHeight:Int,category:Int,score:Float,box:FloatArray):Hit? {
        if(!score.isFinite()||score<.4f||box.size!=4||box.any{!it.isFinite()})return null
        val left=(window.left+box[1].coerceIn(0f,1f)*window.width)/imageWidth
        val top=(window.top+box[0].coerceIn(0f,1f)*window.height)/imageHeight
        val right=(window.left+box[3].coerceIn(0f,1f)*window.width)/imageWidth
        val bottom=(window.top+box[2].coerceIn(0f,1f)*window.height)/imageHeight
        if(right-left<.01f||bottom-top<.01f)return null
        return Hit(category,score,left,top,right,bottom)
    }

    /** Suppress duplicate views, including a fragment contained by another crop's detection. */
    fun distinct(hits:List<Hit>,limit:Int=20):List<Hit> {
        val kept=mutableListOf<Hit>()
        for(hit in hits.sortedByDescending{it.score}) {
            if(kept.size>=limit)break
            if(kept.none{other->
                if(other.category!=hit.category)false else {
                    val intersection=(minOf(hit.right,other.right)-maxOf(hit.left,other.left)).coerceAtLeast(0f)*
                        (minOf(hit.bottom,other.bottom)-maxOf(hit.top,other.top)).coerceAtLeast(0f)
                    val a=(hit.right-hit.left)*(hit.bottom-hit.top)
                    val b=(other.right-other.left)*(other.bottom-other.top)
                    intersection/(a+b-intersection).coerceAtLeast(.000001f)>.45f ||
                        intersection/minOf(a,b).coerceAtLeast(.000001f)>.8f
                }
            })kept+=hit
        }
        return kept
    }
}
