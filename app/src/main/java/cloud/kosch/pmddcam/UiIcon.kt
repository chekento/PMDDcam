package cloud.kosch.pmddcam

import android.graphics.*
import android.graphics.drawable.Drawable

/** Small consistent line icons, drawn at any display density. */
class UiIcon(private val name:String, private val tint:Int=Color.WHITE):Drawable(){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=tint;style=Paint.Style.STROKE;strokeWidth=1.7f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
    override fun draw(canvas:Canvas){
        val save=canvas.save();canvas.translate(bounds.left.toFloat(),bounds.top.toFloat());canvas.scale(bounds.width()/24f,bounds.height()/24f)
        fun line(vararg xy:Float){val path=Path();path.moveTo(xy[0],xy[1]);for(i in 2 until xy.size step 2)path.lineTo(xy[i],xy[i+1]);canvas.drawPath(path,paint)}
        fun circle(x:Float,y:Float,r:Float)=canvas.drawCircle(x,y,r,paint)
        when(name){
            "camera"->{canvas.drawRoundRect(3f,7f,21f,20f,3f,3f,paint);line(7f,7f,9f,4f,15f,4f,17f,7f);circle(12f,13.5f,3.5f)}
            "photos"->{canvas.drawRoundRect(6f,3f,21f,18f,2f,2f,paint);line(3f,7f,3f,20f,17f,20f);line(7f,15f,11f,11f,14f,14f,17f,10f,20f,14f);circle(16f,7f,1f)}
            "looks"->{line(12f,2f,14.5f,9.5f,22f,12f,14.5f,14.5f,12f,22f,9.5f,14.5f,2f,12f,9.5f,9.5f,12f,2f)}
            "depth"->{line(3f,8f,12f,3f,21f,8f,12f,13f,3f,8f);line(3f,12f,12f,17f,21f,12f);line(3f,16f,12f,21f,21f,16f)}
            "settings"->{for(x in listOf(5f,12f,19f)){line(x,3f,x,21f)};for((x,y) in listOf(5f to 8f,12f to 16f,19f to 9f)){paint.style=Paint.Style.FILL;paint.color=0xff17202e.toInt();canvas.drawCircle(x,y,2.8f,paint);paint.color=tint;paint.style=Paint.Style.STROKE;circle(x,y,2.8f)}}
            "tools"->{line(4f,20f,16f,8f);circle(17f,7f,4f);line(4f,4f,9f,9f,7f,11f,2f,6f,4f,4f);line(15f,15f,20f,20f)}
            "compare"->{canvas.drawRoundRect(3f,4f,21f,20f,3f,3f,paint);line(12f,2f,12f,22f);line(5f,16f,9f,11f);line(15f,13f,18f,9f,21f,13f)}
            "undo","redo"->{if(name=="redo"){canvas.translate(24f,0f);canvas.scale(-1f,1f)};line(8f,5f,3f,10f,8f,15f);line(3f,10f,14f,10f);canvas.drawArc(9f,10f,21f,22f,-90f,130f,false,paint)}
            "switch"->{canvas.drawArc(4f,4f,20f,20f,205f,135f,false,paint);canvas.drawArc(4f,4f,20f,20f,25f,135f,false,paint);line(16f,3f,20f,8f,21f,3f);line(3f,21f,4f,16f,8f,21f)}
            "eye"->{val path=Path();path.moveTo(2f,12f);path.quadTo(12f,-2f,22f,12f);path.quadTo(12f,26f,2f,12f);canvas.drawPath(path,paint);circle(12f,12f,3f)}
            "export"->{line(4f,15f,4f,21f,20f,21f,20f,15f);line(12f,3f,12f,16f);line(7f,8f,12f,3f,17f,8f)}
            "brush"->{line(9f,16f,18f,3f,21f,6f,12f,18f,9f,16f);val path=Path();path.moveTo(10f,17f);path.cubicTo(4f,13f,7f,20f,2f,21f);path.cubicTo(9f,23f,13f,20f,10f,17f);canvas.drawPath(path,paint)}
            "objects"->{line(3f,9f,3f,3f,9f,3f);line(15f,3f,21f,3f,21f,9f);line(21f,15f,21f,21f,15f,21f);line(9f,21f,3f,21f,3f,15f);circle(12f,12f,4f)}
            "flash"->line(14f,2f,5f,14f,11f,14f,10f,22f,20f,9f,13f,9f,14f,2f)
            "sound"->{line(3f,9f,7f,9f,12f,5f,12f,19f,7f,15f,3f,15f,3f,9f);canvas.drawArc(10f,7f,20f,17f,-55f,110f,false,paint);canvas.drawArc(7f,3f,25f,21f,-50f,100f,false,paint)}
            "timer"->{circle(12f,14f,8f);line(9f,2f,15f,2f);line(12f,6f,12f,2f);line(12f,14f,16f,11f)}
            "grid"->{for(v in listOf(8f,16f)){line(v,3f,v,21f);line(3f,v,21f,v)}}
            "back"->line(15f,4f,7f,12f,15f,20f)
            "info"->{circle(12f,12f,9f);line(12f,11f,12f,17f);circle(12f,7f,.4f)}
            else->line(7f,5f,17f,12f,7f,19f)
        }
        canvas.restoreToCount(save)
    }
    override fun setAlpha(alpha:Int){paint.alpha=alpha;invalidateSelf()}
    override fun setColorFilter(filter:ColorFilter?){paint.colorFilter=filter;invalidateSelf()}
    @Deprecated("Drawable opacity is unused") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
