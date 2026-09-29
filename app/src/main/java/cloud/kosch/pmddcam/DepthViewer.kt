package cloud.kosch.pmddcam

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/** Interactive 2.5D viewer. Static PMDD exports do not contain this movement. */
class DepthViewer(context:Context):GLSurfaceView(context),GLSurfaceView.Renderer {
    private var program=0
    private val textures=IntArray(2)
    private val vertices=ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(floatArrayOf(-1f,-1f,1f,-1f,-1f,1f,1f,1f));position(0)}
    private var photo:Bitmap?=null;private var depth:Bitmap?=null
    @Volatile private var needsUpload=false
    @Volatile var recipe=Recipe()
    @Volatile var viewX=0f
    @Volatile var viewY=0f
    @Volatile var viewZ=0f
    @Volatile var active=false
    private var canvasWidth=1;private var canvasHeight=1
    private var touchX=0f;private var touchY=0f
    private val pinch=ScaleGestureDetector(context,object:ScaleGestureDetector.SimpleOnScaleGestureListener(){
        override fun onScale(d:ScaleGestureDetector):Boolean {viewZ=(viewZ+(d.scaleFactor-1)*2).coerceIn(-1f,1f);requestRender();return true}
    })
    init {setEGLContextClientVersion(2);setRenderer(this);renderMode=RENDERMODE_WHEN_DIRTY;preserveEGLContextOnPause=true;contentDescription="PMDD-Bild. Im Betrachtermodus ziehen und mit zwei Fingern näher oder weiter bewegen."}
    fun setImage(image:Bitmap,map:DepthMap,r:Recipe) {
        // Owned copies are replaced on the GL thread only; uploads cannot race recycling.
        val copy=image.copy(Bitmap.Config.ARGB_8888,false);val d=PmddRenderer.depthBitmap(map)
        queueEvent{photo?.recycle();depth?.recycle();photo=copy;depth=d;recipe=r.copy();needsUpload=true}
        requestRender()
    }
    fun setPosition(x:Float,y:Float,z:Float){viewX=x.safe(0f,-1f,1f);viewY=y.safe(0f,-1f,1f);viewZ=z.safe(0f,-1f,1f);requestRender()}
    fun center(){setPosition(0f,0f,0f)}
    fun release(){queueEvent{photo?.recycle();depth?.recycle();photo=null;depth=null;glDeleteTextures(2,textures,0);if(program!=0)glDeleteProgram(program)}}
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){
        program=glCreateProgram();glAttachShader(program,shader(GL_VERTEX_SHADER,VERTEX));glAttachShader(program,shader(GL_FRAGMENT_SHADER,FRAGMENT));glLinkProgram(program)
        val linked=IntArray(1);glGetProgramiv(program,GL_LINK_STATUS,linked,0);check(linked[0]!=0){glGetProgramInfoLog(program)}
        glGenTextures(2,textures,0);needsUpload=true
        glClearColor(.035f,.05f,.075f,1f)
    }
    override fun onSurfaceChanged(gl:GL10?,width:Int,height:Int){canvasWidth=width;canvasHeight=height;glViewport(0,0,width,height)}
    override fun onDrawFrame(gl:GL10?){
        glClear(GL_COLOR_BUFFER_BIT);val bitmap=photo?:return;val map=depth?:return
        if(needsUpload){listOf(bitmap,map).forEachIndexed{i,b->
            glBindTexture(GL_TEXTURE_2D,textures[i]);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);GLUtils.texImage2D(GL_TEXTURE_2D,0,b,0)
        };needsUpload=false}
        glUseProgram(program)
        val p=glGetAttribLocation(program,"position");glEnableVertexAttribArray(p);glVertexAttribPointer(p,2,GL_FLOAT,false,0,vertices)
        for(i in 0..1){glActiveTexture(GL_TEXTURE0+i);glBindTexture(GL_TEXTURE_2D,textures[i]);glUniform1i(glGetUniformLocation(program,if(i==0)"photo" else "depthMap"),i)}
        val r=recipe
        val geometry=(r.screenSize/16f*45f/r.viewDistance).coerceIn(.4f,2f)
        val amount=if(active)r.parallax*r.depth*.035f*geometry else 0f
        glUniform3f(glGetUniformLocation(program,"eye"),if(r.horizontal)viewX*amount else 0f,if(r.vertical)viewY*amount else 0f,if(r.distance)viewZ*amount*1.8f else 0f)
        glUniform1f(glGetUniformLocation(program,"focusDepth"),r.focus)
        val imageAspect=bitmap.width.toFloat()/bitmap.height;val screenAspect=canvasWidth.toFloat()/canvasHeight
        val fitX=if(imageAspect>screenAspect)1f else imageAspect/screenAspect
        val fitY=if(imageAspect>screenAspect)screenAspect/imageAspect else 1f
        glUniform2f(glGetUniformLocation(program,"fit"),fitX,fitY)
        glDrawArrays(GL_TRIANGLE_STRIP,0,4);glDisableVertexAttribArray(p)
    }
    override fun onTouchEvent(event:MotionEvent):Boolean {
        if(!active)return super.onTouchEvent(event)
        pinch.onTouchEvent(event)
        when(event.actionMasked){
            MotionEvent.ACTION_DOWN->{touchX=event.x;touchY=event.y;parent?.requestDisallowInterceptTouchEvent(true)}
            MotionEvent.ACTION_MOVE->if(!pinch.isInProgress && event.pointerCount==1){viewX=(viewX+(event.x-touchX)/width*3).coerceIn(-1f,1f);viewY=(viewY+(event.y-touchY)/height*3).coerceIn(-1f,1f);touchX=event.x;touchY=event.y;requestRender()}
            MotionEvent.ACTION_UP->{performClick();parent?.requestDisallowInterceptTouchEvent(false)}
        };return true
    }
    override fun performClick():Boolean {super.performClick();return true}
    private fun shader(type:Int,source:String):Int {val s=glCreateShader(type);glShaderSource(s,source);glCompileShader(s);val ok=IntArray(1);glGetShaderiv(s,GL_COMPILE_STATUS,ok,0);check(ok[0]!=0){glGetShaderInfoLog(s)};return s}
    companion object {
        private const val VERTEX="""attribute vec2 position; varying vec2 uv; uniform vec2 fit;
            void main(){gl_Position=vec4(position*fit,0.,1.);uv=vec2(position.x*.5+.5,.5-position.y*.5);}"""
        private const val FRAGMENT="""precision highp float;
            varying vec2 uv; uniform sampler2D photo; uniform sampler2D depthMap; uniform vec3 eye; uniform float focusDepth;
            void main(){
                vec2 direction=eye.xy+(uv-.5)*eye.z;
                vec2 best=uv;
                float hit=0.;
                // Front-to-back depth intersections preserve foreground occlusion.
                for(int i=0;i<64;i++){
                    float plane=1.-float(i)/63.;
                    vec2 p=uv-direction*(plane-focusDepth);
                    float d=texture2D(depthMap,clamp(p,vec2(.001),vec2(.999))).r;
                    if(hit<.5 && d>=plane){best=p;hit=1.;}
                }
                // Clamp edge sampling; limited excursion prevents broad unseen regions.
                gl_FragColor=texture2D(photo,clamp(best,vec2(.001),vec2(.999)));
            }"""
    }
}
