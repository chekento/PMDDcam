package cloud.kosch.pmddcam

import org.json.JSONObject
import java.util.Base64
import kotlin.math.min

/** Immutable, image-aligned soft region. The same mask follows edits, exports and the viewer. */
class RegionMask(val width:Int,val height:Int,values:ByteArray) {
    private val values=values.clone()
    init{require(width in 1..256 && height in 1..256 && values.size==width*height)}
    fun sample(u:Float,v:Float):Float{
        val x=u.coerceIn(0f,1f)*(width-1);val y=v.coerceIn(0f,1f)*(height-1)
        val ix=x.toInt();val iy=y.toInt();val ax=x-ix;val ay=y-iy
        fun at(a:Int,b:Int)=(values[b*width+a].toInt()and 255)/255f
        val nx=min(ix+1,width-1);val ny=min(iy+1,height-1)
        return (at(ix,iy)*(1-ax)+at(nx,iy)*ax)*(1-ay)+(at(ix,ny)*(1-ax)+at(nx,ny)*ax)*ay
    }
    fun json()=JSONObject().put("width",width).put("height",height).put("values",Base64.getEncoder().encodeToString(values))
    companion object {
        fun from(j:JSONObject):RegionMask{
            val w=j.getInt("width");val h=j.getInt("height");val encoded=j.getString("values")
            require(w in 1..256 && h in 1..256 && encoded.length<=87384)
            return RegionMask(w,h,Base64.getDecoder().decode(encoded))
        }
    }
}
