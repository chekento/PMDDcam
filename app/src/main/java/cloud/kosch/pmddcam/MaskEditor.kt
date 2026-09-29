package cloud.kosch.pmddcam

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class MaskEditor(context:Context,private val photo:Bitmap,val depth:DepthMap,val regionMode:Boolean=false):View(context){
    var target=.9f
    var radius=.06f
    var region=RectF(.25f,.25f,.75f,.75f)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val fit=RectF()
    private var heat:Bitmap?=null
    private var startX=0f;private var startY=0f
    private var lastX=0f;private var lastY=0f
    private val history=ArrayDeque<FloatArray>()
    init{contentDescription=if(regionMode)"Rechteck um das Objekt ziehen" else "Tiefenkarte mit dem Finger bemalen";refresh()}
    fun undo(){if(history.isNotEmpty()){history.removeLast().copyInto(depth.values);refresh()}}
    fun refresh(){heat?.recycle();heat=PmddRenderer.depthBitmap(depth);invalidate()}
    override fun onDraw(c:Canvas){
        val s=min(width.toFloat()/photo.width,height.toFloat()/photo.height)
        fit.set((width-photo.width*s)/2,(height-photo.height*s)/2,(width+photo.width*s)/2,(height+photo.height*s)/2)
        paint.alpha=255;c.drawBitmap(photo,null,fit,paint)
        if(!regionMode){paint.alpha=115;heat?.let{c.drawBitmap(it,null,fit,paint)};paint.alpha=255}
        else{paint.style=Paint.Style.STROKE;paint.strokeWidth=3f;paint.color=Color.rgb(134,241,209)
            c.drawRect(fit.left+region.left*fit.width(),fit.top+region.top*fit.height(),fit.left+region.right*fit.width(),fit.top+region.bottom*fit.height(),paint);paint.style=Paint.Style.FILL}
    }
    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(fit.width()<=0)return false
        val u=((e.x-fit.left)/fit.width()).coerceIn(0f,1f);val v=((e.y-fit.top)/fit.height()).coerceIn(0f,1f)
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN->{parent?.requestDisallowInterceptTouchEvent(true);startX=u;startY=v;lastX=u;lastY=v
                if(!regionMode){history.addLast(depth.values.clone());if(history.size>12)history.removeFirst();dab(u,v)} }
            MotionEvent.ACTION_MOVE->{
                if(regionMode){region=RectF(min(startX,u),min(startY,v),max(startX,u),max(startY,v));invalidate()}
                else{val steps=(hypot(u-lastX,v-lastY)/(radius*.25f)).toInt().coerceIn(1,60);for(i in 1..steps){val f=i.toFloat()/steps;dab(lastX+(u-lastX)*f,lastY+(v-lastY)*f)};lastX=u;lastY=v;refresh()}
            }
            MotionEvent.ACTION_UP->{if(!regionMode)refresh();parent?.requestDisallowInterceptTouchEvent(false);performClick()}
        };return true
    }
    private fun dab(u:Float,v:Float){
        val ratio=fit.height()/fit.width()
        val rx=radius;val ry=radius/ratio
        val x0=((u-rx)*depth.width).toInt().coerceIn(0,depth.width-1);val x1=((u+rx)*depth.width).toInt().coerceIn(0,depth.width-1)
        val y0=((v-ry)*depth.height).toInt().coerceIn(0,depth.height-1);val y1=((v+ry)*depth.height).toInt().coerceIn(0,depth.height-1)
        for(y in y0..y1)for(x in x0..x1){val d=hypot((x.toFloat()/depth.width-u)/rx,(y.toFloat()/depth.height-v)/ry)
            if(d<1){val weight=(1-d)*.65f;val i=y*depth.width+x;depth.values[i]=depth.values[i]*(1-weight)+target*weight}}
    }
    override fun performClick():Boolean{super.performClick();return true}
    override fun onDetachedFromWindow(){super.onDetachedFromWindow();heat?.recycle();heat=null}
}
