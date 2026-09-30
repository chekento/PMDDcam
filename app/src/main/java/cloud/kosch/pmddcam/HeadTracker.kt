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
import android.os.SystemClock

/** Front-camera analysis is opt-in, stays on-device and never writes a frame. */
class HeadTracker(private val context:Context,private val owner:LifecycleOwner,
    private val position:(Float,Float,Float)->Unit,private val status:(String)->Unit) {
    private val executor=Executors.newSingleThreadExecutor()
    private val detector=FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).setMinFaceSize(.1f).enableTracking().build())
    private var analysis:ImageAnalysis?=null
    private var provider:ProcessCameraProvider?=null
    @Volatile private var running=false
    private val pose=HeadPose()
    private var trackedId:Int?=null
    private var lastSeen=0L
    private var lastTime=0L
    fun calibrate(){pose.reset();trackedId=null;position(0f,0f,0f)}
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
                    val now=SystemClock.elapsedRealtime()
                    if(!running || image==null || now-lastTime<66){proxy.close();return@setAnalyzer}
                    lastTime=now
                    val rotation=proxy.imageInfo.rotationDegrees
                    val w=if(rotation%180==0)proxy.width else proxy.height
                    val h=if(rotation%180==0)proxy.height else proxy.width
                    detector.process(InputImage.fromMediaImage(image,rotation)).addOnSuccessListener {faces->
                        if(running){
                            val face=faces.firstOrNull{trackedId!=null&&it.trackingId==trackedId}?:faces.maxByOrNull{it.boundingBox.width()*it.boundingBox.height()}
                            if(face!=null){
                                val b=face.boundingBox;val x=b.exactCenterX()/w;val y=b.exactCenterY()/h;val size=sqrt(b.width().toFloat()*b.height())/w
                                if((trackedId!=null&&face.trackingId!=trackedId)||(lastSeen>0&&now-lastSeen>1500))pose.reset()
                                trackedId=face.trackingId;lastSeen=now
                                val values=pose.update(x,y,size,face.headEulerAngleY,face.headEulerAngleX)
                                position(values[0],values[1],values[2])
                                status(if(pose.calibrated)"Kopfsteuerung · bewegen, neigen, näher / weiter" else "Kurz gerade schauen · Kalibrierung …")
                            }else{val values=pose.lost();position(values[0],values[1],values[2]);status("Gesicht zur Frontkamera richten")}

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
