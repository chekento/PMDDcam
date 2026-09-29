package cloud.kosch.pmddcam

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors
import kotlin.math.sqrt

/** Front-camera analysis is opt-in, stays on-device and never writes a frame. */
class HeadTracker(private val context:Context,private val owner:LifecycleOwner,
    private val position:(Float,Float,Float)->Unit,private val status:(String)->Unit) {
    private val executor=Executors.newSingleThreadExecutor()
    private val detector=FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).setMinFaceSize(.15f).build())
    private var analysis:ImageAnalysis?=null
    private var provider:ProcessCameraProvider?=null
    @Volatile private var running=false
    private var baseline:FloatArray?=null
    private val smoothed=FloatArray(3)
    private var lastTime=0L
    fun calibrate(){baseline=null;smoothed.fill(0f);position(0f,0f,0f)}
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    fun start(){
        running=true
        val future=ProcessCameraProvider.getInstance(context)
        future.addListener({
            if(!running)return@addListener
            try{
                val p=future.get();provider=p
                require(p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)){"Keine Frontkamera vorhanden."}
                val useCase=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();analysis=useCase
                useCase.setAnalyzer(executor){proxy->
                    val image=proxy.image
                    val now=System.currentTimeMillis()
                    if(!running || image==null || now-lastTime<100){proxy.close();return@setAnalyzer}
                    lastTime=now
                    val rotation=proxy.imageInfo.rotationDegrees
                    val w=if(rotation%180==0)proxy.width else proxy.height
                    val h=if(rotation%180==0)proxy.height else proxy.width
                    detector.process(InputImage.fromMediaImage(image,rotation)).addOnSuccessListener {faces->
                        if(running){
                            val face=faces.maxByOrNull{it.boundingBox.width()*it.boundingBox.height()}
                            if(face!=null){
                                val b=face.boundingBox;val x=b.exactCenterX()/w;val y=b.exactCenterY()/h;val size=sqrt(b.width().toFloat()*b.height())/w
                                if(baseline==null)baseline=floatArrayOf(x,y,size)
                                val base=baseline!!
                                val values=floatArrayOf(-(x-base[0])*4,(y-base[1])*4,(size/base[2]-1)*2)
                                for(i in 0..2)smoothed[i]=smoothed[i]*.72f+values[i].coerceIn(-1f,1f)*.28f
                                position(smoothed[0],smoothed[1],smoothed[2]);status("Kopfsteuerung aktiv · lokal")
                            }else{status("Gesicht zur Frontkamera richten");for(i in 0..2)smoothed[i]*=.85f;position(smoothed[0],smoothed[1],smoothed[2])}
                        }
                    }.addOnFailureListener{if(running)status("Gesichtserkennung pausiert")}.addOnCompleteListener{proxy.close()}
                }
                p.bindToLifecycle(owner,CameraSelector.DEFAULT_FRONT_CAMERA,useCase)
                status("Gerade auf das Display schauen · kalibriert automatisch")
            }catch(e:Exception){status(e.message?:"Frontkamera nicht verfügbar.");stop()}
        },ContextCompat.getMainExecutor(context))
    }
    fun stop(){running=false;analysis?.let{it.clearAnalyzer();provider?.unbind(it)};analysis=null;detector.close();executor.shutdown()}
}
