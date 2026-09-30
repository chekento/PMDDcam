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
    private val textures=IntArray(4)
    private val vertices=ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(floatArrayOf(-1f,-1f,1f,-1f,-1f,1f,1f,1f));position(0)}
    private var photo:Bitmap?=null;private var depth:Bitmap?=null;private var field:Bitmap?=null;private var timing:Bitmap?=null
    @Volatile internal var completedFrames=0
        private set
    @Volatile private var needsUpload=false
    @Volatile var recipe=Recipe()
    @Volatile var viewX=0f
    @Volatile var viewY=0f
    @Volatile var viewZ=0f
    @Volatile var active=false
        set(value){field=value;refreshAnimation()}
    @Volatile private var paused=false
    @Volatile private var released=false
    @Volatile private var hasAnimatedObjects=false
    @Volatile internal var animationTimeOverride:Float?=null
    private var elapsed=0f;private var lastTick=0L
    private val animationFrame=object:Runnable{
        override fun run(){if(shouldAnimate()){requestRender();postDelayed(this,33)}}
    }
    private fun shouldAnimate()=active&&!paused&&!released&&hasAnimatedObjects&&recipe.animateObjects&&recipe.animationAmount>0f
    private fun refreshAnimation(){removeCallbacks(animationFrame);if(shouldAnimate())post(animationFrame);requestRender()}
    override fun onPause(){paused=true;removeCallbacks(animationFrame);super.onPause()}
    override fun onResume(){super.onResume();paused=false;refreshAnimation()}
    override fun onDetachedFromWindow(){paused=true;removeCallbacks(animationFrame);super.onDetachedFromWindow()}
    private var canvasWidth=1;private var canvasHeight=1
    private var touchX=0f;private var touchY=0f
    private val pinch=ScaleGestureDetector(context,object:ScaleGestureDetector.SimpleOnScaleGestureListener(){
        override fun onScale(d:ScaleGestureDetector):Boolean {viewZ=(viewZ+(d.scaleFactor-1)*2).coerceIn(-1f,1f);requestRender();return true}
    })
    init {setEGLContextClientVersion(2);setRenderer(this);renderMode=RENDERMODE_WHEN_DIRTY;preserveEGLContextOnPause=true;contentDescription="PMDD-Bild. Im Betrachtermodus ziehen und mit zwei Fingern näher oder weiter bewegen."}
    fun setImage(image:Bitmap,map:DepthMap,r:Recipe,objects:List<SceneObject> = emptyList()) {
        // Owned copies are replaced on the GL thread only; uploads cannot race recycling.
        val copy=image.copy(Bitmap.Config.ARGB_8888,false);val d=PmddRenderer.depthBitmap(map)
        val scene=objects.map{it.copy()};val snapshot=map.copy();val settings=r.copy()
        queueEvent{
            photo?.recycle();depth?.recycle();field?.recycle();timing?.recycle()
            val fields=MotionField.create(snapshot,scene,settings)
            photo=copy;depth=d;field=fields.vectors;timing=fields.timing;recipe=settings;needsUpload=true
            hasAnimatedObjects=scene.any{it.enabled&&it.role!=Role.ANCHOR&&it.intensity>0f}
            post{refreshAnimation()}
        }
        requestRender()
    }
    fun setPosition(x:Float,y:Float,z:Float){viewX=x.safe(0f,-1f,1f);viewY=y.safe(0f,-1f,1f);viewZ=z.safe(0f,-1f,1f);requestRender()}
    fun center(){setPosition(0f,0f,0f)}
    fun release(){released=true;removeCallbacks(animationFrame);queueEvent{photo?.recycle();depth?.recycle();field?.recycle();timing?.recycle();photo=null;depth=null;field=null;timing=null;glDeleteTextures(4,textures,0);if(program!=0)glDeleteProgram(program)}}
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){
        program=glCreateProgram();glAttachShader(program,shader(GL_VERTEX_SHADER,VERTEX));glAttachShader(program,shader(GL_FRAGMENT_SHADER,FRAGMENT));glLinkProgram(program)
        val linked=IntArray(1);glGetProgramiv(program,GL_LINK_STATUS,linked,0);check(linked[0]!=0){glGetProgramInfoLog(program)}
        glGenTextures(4,textures,0);needsUpload=true
        glClearColor(.035f,.05f,.075f,1f)
    }
    override fun onSurfaceChanged(gl:GL10?,width:Int,height:Int){canvasWidth=width;canvasHeight=height;glViewport(0,0,width,height)}
    override fun onDrawFrame(gl:GL10?){
        glClear(GL_COLOR_BUFFER_BIT);val bitmap=photo?:return;val map=depth?:return;val motion=field?:return;val clock=timing?:return
        if(needsUpload){listOf(bitmap,map,motion,clock).forEachIndexed{i,b->
            glBindTexture(GL_TEXTURE_2D,textures[i]);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);GLUtils.texImage2D(GL_TEXTURE_2D,0,b,0)
        };needsUpload=false}
        glUseProgram(program)
        val p=glGetAttribLocation(program,"position");glEnableVertexAttribArray(p);glVertexAttribPointer(p,2,GL_FLOAT,false,0,vertices)
        for(i in 0..3){glActiveTexture(GL_TEXTURE0+i);glBindTexture(GL_TEXTURE_2D,textures[i]);glUniform1i(glGetUniformLocation(program,arrayOf("photo","depthMap","motionField","motionTiming")[i]),i)}
        val r=recipe
        val animating=shouldAnimate()
        val tick=System.nanoTime()
        if(animating){if(lastTick>0L)elapsed+=((tick-lastTick)/1e9f).coerceIn(0f,.1f);lastTick=tick}else lastTick=0L
        glUniform1f(glGetUniformLocation(program,"animationTime"),animationTimeOverride?:elapsed)
        glUniform1f(glGetUniformLocation(program,"animationSpeed"),r.animationSpeed)
        glUniform1f(glGetUniformLocation(program,"animationAmount"),if(animating)r.animationAmount else 0f)
        val geometry=(r.screenSize/16f*45f/r.viewDistance).coerceIn(.4f,2f)
        val amount=if(active)(1-exp(-r.parallax*r.depth*.9f*geometry))*.085f else 0f
        glUniform3f(glGetUniformLocation(program,"eye"),if(r.horizontal)viewX*amount else 0f,if(r.vertical)viewY*amount else 0f,if(r.distance)viewZ*amount*1.8f else 0f)
        glUniform1f(glGetUniformLocation(program,"objectMotion"),if(r.motion)r.motionAmount else 0f)
        glUniform1f(glGetUniformLocation(program,"focusDepth"),r.focus)
        glUniform1f(glGetUniformLocation(program,"layerCount"),r.layers.toFloat())
        val imageAspect=bitmap.width.toFloat()/bitmap.height;val screenAspect=canvasWidth.toFloat()/canvasHeight
        val fitX=if(imageAspect>screenAspect)1f else imageAspect/screenAspect
        val fitY=if(imageAspect>screenAspect)screenAspect/imageAspect else 1f
        glUniform2f(glGetUniformLocation(program,"fit"),fitX,fitY)
        glDrawArrays(GL_TRIANGLE_STRIP,0,4);glDisableVertexAttribArray(p);completedFrames++
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
            varying vec2 uv; uniform sampler2D photo; uniform sampler2D depthMap; uniform sampler2D motionField; uniform sampler2D motionTiming; uniform float animationTime; uniform float animationSpeed; uniform float animationAmount; uniform float objectMotion; uniform vec3 eye; uniform float focusDepth; uniform float layerCount;
            void main(){
                vec2 direction=eye.xy+(uv-.5)*eye.z;
                if(dot(direction,direction)<.00000001 && animationAmount<.00001){gl_FragColor=texture2D(photo,uv);return;}
                vec2 best=uv;
                float hit=0.; float previousGap=0.; float previousPlane=1.;
                // Continuous intersections; layer count changes sampling, never photo shading.
                if(dot(direction,direction)>=.00000001){for(int i=0;i<128;i++){
                    if(float(i)>=layerCount)break;
                    float plane=1.-float(i)/(layerCount-1.);
                    vec2 p=uv-direction*(plane-focusDepth);
                    float d=texture2D(depthMap,clamp(p,vec2(.001),vec2(.999))).r;
                    float gap=plane-d;
                    if(hit<.5 && gap<=0.){
                        float fraction=previousGap/max(.00001,previousGap-gap);
                        float surface=mix(previousPlane,plane,clamp(fraction,0.,1.));
                        best=uv-direction*(surface-focusDepth);hit=1.;
                    }
                    previousGap=max(0.,gap);previousPlane=plane;
                }
                }
                vec3 field=texture2D(motionField,clamp(best,vec2(.001),vec2(.999))).rgb;
                float drive=eye.x+eye.y*.75+eye.z*.4;
                vec3 timing=texture2D(motionTiming,clamp(best,vec2(.001),vec2(.999))).rgb;
                float phase=timing.g*6.2831853;
                float wave=(sin(animationTime*animationSpeed*timing.r*6.2831853+phase)-sin(phase))*.5;
                float extent=timing.b*.04;
                vec2 local=(field.rg*2.-1.)*field.b*(drive*objectMotion*.8+extent*animationAmount*wave);
                float limit=min(.035,extent*1.6);
                best-=clamp(local,vec2(-limit),vec2(limit));
                // Clamp edge sampling; limited excursion prevents broad unseen regions.
                gl_FragColor=texture2D(photo,clamp(best,vec2(.001),vec2(.999)));
            }"""
    }
}
