package cloud.kosch.pmddcam

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** Bundled SSD MobileNet; no Play Services module or network download at runtime. */
class ObjectAnalyzer(private val context:Context) {
    fun detect(photo:Bitmap,depth:DepthMap):List<SceneObject> {
        val model=File(context.noBackupFilesDir,"ssd-mobilenet-v1-12.onnx")
        if(!model.exists() || model.length()!=29_461_455L){
            val tmp=File(model.parentFile,"object-model.tmp")
            context.assets.open("ssd-mobilenet.onnx").use{input->tmp.outputStream().use{input.copyTo(it)}}
            check(tmp.length()==29_461_455L && tmp.renameTo(model)){"Das Objektmodell konnte nicht geladen werden."}
        }
        val env=OrtEnvironment.getEnvironment()
        OrtSession.SessionOptions().use{options->
            options.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(1,4))
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            env.createSession(model.path,options).use{session->
                val small=Bitmap.createScaledBitmap(photo,300,300,true)
                val pixels=IntArray(300*300);small.getPixels(pixels,0,300,0,0,300,300)
                if(small!==photo)small.recycle()
                // The exported graph accepts uint8 RGB NHWC and owns normalization.
                val input=ByteBuffer.allocateDirect(pixels.size*3).order(ByteOrder.nativeOrder())
                pixels.forEach{c->input.put(((c shr 16)and 255).toByte());input.put(((c shr 8)and 255).toByte());input.put((c and 255).toByte())};input.rewind()
                OnnxTensor.createTensor(env,input,longArrayOf(1,300,300,3),OnnxJavaType.UINT8).use{tensor->
                    session.run(mapOf(session.inputNames.single() to tensor)).use{outputs->
                        fun floats(name:String):FloatArray {
                            val output=outputs.get(name).orElse(null) as? OnnxTensor ?: error("Fehlende Objektausgabe: $name")
                            val buffer=output.floatBuffer;return FloatArray(buffer.remaining()).also{buffer.get(it)}
                        }
                        val boxes=floats("detection_boxes");val scores=floats("detection_scores");val classes=floats("detection_classes")
                        val count=(floats("num_detections").firstOrNull()?.toInt()?:0).coerceIn(0,minOf(boxes.size/4,scores.size,classes.size))
                        val found=mutableListOf<SceneObject>()
                        for(i in 0 until count){
                            val score=scores[i];if(!score.isFinite() || score<.4f || found.size>=20)continue
                            val category=classes[i].roundToInt();val label=labels.getOrNull(category)?.takeIf{it.isNotEmpty()}?:continue
                            val top=boxes[4*i].safe(0f,0f,1f);val left=boxes[4*i+1].safe(0f,0f,1f)
                            val bottom=boxes[4*i+2].safe(1f,0f,1f);val right=boxes[4*i+3].safe(1f,0f,1f)
                            if(right-left<.01f || bottom-top<.01f)continue
                            val profile=ObjectMotion.profile(category)
                            found+=SceneObject(found.size+1,"$label · ${(score*100).roundToInt()} %",left,top,right,bottom,
                                profile.role,profile.motion,profile.angle,profile.speed,profile.intensity,depth.sample((left+right)/2,(top+bottom)/2))
                        }
                        return found
                    }
                }
            }
        }
    }

    companion object {
        // COCO category IDs contain intentional gaps; keep the original 1..90 mapping.
        private val labels=("|Person|Fahrrad|Auto|Motorrad|Flugzeug|Bus|Zug|Lastwagen|Boot|Ampel|Hydrant||Stoppschild|Parkuhr|Bank|Vogel|Katze|Hund|Pferd|Schaf|Kuh|Elefant|Bär|Zebra|Giraffe||Rucksack|Regenschirm|||Handtasche|Krawatte|Koffer|Frisbee|Ski|Snowboard|Ball|Drachen|Baseballschläger|Baseballhandschuh|Skateboard|Surfbrett|Tennisschläger|Flasche||Weinglas|Tasse|Gabel|Messer|Löffel|Schüssel|Banane|Apfel|Sandwich|Orange|Brokkoli|Karotte|Hotdog|Pizza|Donut|Kuchen|Stuhl|Sofa|Topfpflanze|Bett||Esstisch|||Toilette||Fernseher|Laptop|Maus|Fernbedienung|Tastatur|Mobiltelefon|Mikrowelle|Backofen|Toaster|Spüle|Kühlschrank||Buch|Uhr|Vase|Schere|Teddybär|Haartrockner|Zahnbürste").split('|')
    }
}
