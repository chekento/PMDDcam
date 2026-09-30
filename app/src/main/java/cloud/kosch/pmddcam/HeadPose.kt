package cloud.kosch.pmddcam

import kotlin.math.*

/** Calibrated relative viewpoint from face position, size and rotation; no camera frames are retained. */
class HeadPose {
    private val samples=mutableListOf<FloatArray>()
    private var baseline:FloatArray?=null
    private val smoothed=FloatArray(3)
    val calibrated get()=baseline!=null
    fun reset(){samples.clear();baseline=null;smoothed.fill(0f)}
    fun update(x:Float,y:Float,size:Float,yaw:Float,pitch:Float):FloatArray{
        if(listOf(x,y,size,yaw,pitch).any{!it.isFinite()} || size<.02f)return smoothed.clone()
        if(baseline==null){
            samples+=floatArrayOf(x,y,size,yaw,pitch)
            if(samples.size>=5)baseline=FloatArray(5){axis->samples.map{it[axis]}.sorted()[2]}
            return smoothed.clone()
        }
        val b=baseline!!;val scale=b[2].coerceAtLeast(.08f)
        // Front camera input is unmirrored. Viewpoint changes oppose near-surface motion.
        val values=floatArrayOf((x-b[0])/scale*1.5f-(yaw-b[3])/80f,
            -(y-b[1])/scale*1.5f+(pitch-b[4])/80f,
            ln(size/b[2])*2.5f)
        for(i in 0..2){val t=values[i].coerceIn(-1f,1f);val alpha=if(abs(t-smoothed[i])>.18f).48f else .28f;smoothed[i]+=(t-smoothed[i])*alpha}
        return smoothed.clone()
    }
    fun lost():FloatArray{for(i in 0..2)smoothed[i]*=.82f;return smoothed.clone()}
}
