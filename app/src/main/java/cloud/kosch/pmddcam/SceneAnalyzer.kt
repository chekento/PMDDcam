package cloud.kosch.pmddcam

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.*

data class Analysis(val depth:DepthMap,val objects:MutableList<SceneObject>,val note:String)

class SceneAnalyzer(private val context:Context) {
    suspend fun analyze(bitmap:Bitmap,detect:Boolean,status:(String)->Unit):Analysis = analysisMutex.withLock { withContext(Dispatchers.Default) {
        status("Räumliche Tiefe wird erkannt …")
        val depth=infer(bitmap)
        ensureActive()
        val objects=mutableListOf<SceneObject>();val notes=mutableListOf<String>()
        if(detect) {
            status("Objekte und stabile Gesichter werden erkannt …")
            val input=InputImage.fromBitmap(bitmap,0)
            try {
                objects+=ObjectAnalyzer(context).detect(bitmap,depth)
            } catch(e:CancellationException){throw e} catch(e:Exception){notes+="Objekterkennung nicht verfügbar; Bereiche lassen sich manuell ergänzen."}
            val faces=FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build())
            try {
                faces.process(input).await().forEachIndexed{i,f->
                    val b=f.boundingBox
                    objects+=SceneObject(100+i,"Gesicht ${i+1}",b.left.toFloat()/bitmap.width,b.top.toFloat()/bitmap.height,b.right.toFloat()/bitmap.width,b.bottom.toFloat()/bitmap.height,Role.ANCHOR,
                        depth=depth.sample(b.exactCenterX()/bitmap.width,b.exactCenterY()/bitmap.height),face=true)
                }
            } catch(e:CancellationException){throw e} catch(e:Exception){notes+="Gesichtsschutz konnte nicht automatisch ermittelt werden."} finally{faces.close()}
            // A person mask refines object boundaries without turning all objects into flat cards.
            val segmenter=Segmentation.getClient(SelfieSegmenterOptions.Builder().setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE).enableRawSizeMask().build())
            try {
                val mask=segmenter.process(input).await();val buffer=mask.buffer;buffer.rewind()
                val confidence=FloatArray(mask.width*mask.height){buffer.float}
                if(confidence.count{it>.85f}>confidence.size*.015 && objects.any{it.face}) {
                    for(y in 0 until depth.height)for(x in 0 until depth.width){
                        val c=confidence[(y*mask.height/depth.height)*mask.width+x*mask.width/depth.width]
                        if(c>.65f){val k=y*depth.width+x;depth.values[k]=(depth.values[k]+.06f*c).coerceIn(0f,1f)}
                    }
                }
            } catch(e:CancellationException){throw e} catch(_:Exception){notes+="Personenmaske nicht verfügbar; die Modell-Tiefenkarte bleibt aktiv."} finally{segmenter.close()}
        }
        objects.add(0,SceneObject(999,"Atmosphärische Ferne",0f,0f,1f,1f,Role.ATMOSPHERE,Motion.DRIFT,25f,.3f,.45f,.18f))
        Analysis(depth,objects,notes.joinToString(" "))
    } }

    companion object { private val analysisMutex=Mutex() }

    private fun infer(bitmap:Bitmap):DepthMap {
        val model=File(context.noBackupFilesDir,"midas-small-v21.onnx")
        if(!model.exists() || model.length()!=66_764_249L){
            val tmp=File(model.parentFile,"depth-model.tmp")
            context.assets.open("midas-small.onnx").use{src->tmp.outputStream().use{src.copyTo(it)}}
            check(tmp.length()==66_764_249L && tmp.renameTo(model)){"Das Tiefenmodell konnte nicht geladen werden."}
        }
        val env=OrtEnvironment.getEnvironment()
        OrtSession.SessionOptions().use{options->
            options.setIntraOpNumThreads(minOf(4,Runtime.getRuntime().availableProcessors()).coerceAtLeast(1))
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            env.createSession(model.path,options).use{session->
                val small=Bitmap.createScaledBitmap(bitmap,256,256,true)
                val colors=IntArray(256*256);small.getPixels(colors,0,256,0,0,256,256)
                if(small!==bitmap)small.recycle()
                val data=FloatArray(colors.size*3)
                // Official v2.1 ONNX export contains ImageNet mean/std normalization.
                // Input is RGB [0,1], NCHW, resized to 256x256 as in MiDaS tf/run_onnx.py.
                colors.forEachIndexed{i,c->data[i]=((c shr 16)and 255)/255f;data[colors.size+i]=((c shr 8)and 255)/255f;data[2*colors.size+i]=(c and 255)/255f}
                OnnxTensor.createTensor(env,FloatBuffer.wrap(data),longArrayOf(1,3,256,256)).use{tensor->
                    session.run(mapOf(session.inputNames.first() to tensor)).use{result->
                        val out=result[0] as OnnxTensor
                        val buffer=out.floatBuffer
                        val raw=FloatArray(buffer.remaining());buffer.get(raw)
                        check(raw.size==256*256){"Unerwartete Ausgabe des Tiefenmodells."}
                        return DepthMap.normalize(raw,256,256)
                    }
                }
            }
        }
    }
}
