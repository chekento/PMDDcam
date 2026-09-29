package cloud.kosch.pmddcam

import android.Manifest
import android.content.Intent
import android.graphics.*
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.io.File
import kotlin.math.abs
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTest {
    @Test fun bundledModelAndProjectPipelineWorkOnDevice()=runBlocking {
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertEquals("Inference is tested in airplane mode", "1",device.executeShellCommand("settings get global airplane_mode_on").trim())
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=ProjectStore(context);val p=store.create(Recipe())
        val photo=InstrumentationRegistry.getInstrumentation().context.assets.open("dogs.jpg").use{BitmapFactory.decodeStream(it)}!!
        store.original(p.id).outputStream().use{assertTrue(photo.compress(Bitmap.CompressFormat.JPEG,95,it))}
        val bytes=store.original(p.id).readBytes()
        val analysis=SceneAnalyzer(context).analyze(photo,true){}
        assertEquals("Every bundled detector initializes offline", "", analysis.note)
        assertTrue("The detector recognizes the dogs in the reference photo",analysis.objects.count{it.name.startsWith("Hund ·")}>=2)
        assertEquals(256,analysis.depth.width);assertTrue(analysis.depth.values.all{it.isFinite()&&it in 0f..1f})
        assertTrue(analysis.depth.values.max()-analysis.depth.values.min()>.5f)
        p.objects=analysis.objects;p.ready=true;store.saveDepth(p.id,analysis.depth,true);store.saveDepth(p.id,analysis.depth);store.save(p)
        val rendered=PmddRenderer.render(photo,analysis.depth,p);assertEquals(photo.width,rendered.image.width)
        store.thumbnail(p.id,rendered.image);assertArrayEquals(bytes,store.original(p.id).readBytes());assertTrue(store.load(p.id).ready)
        fun evidence(name:String,image:Bitmap){
            val file=File(context.cacheDir,"qa-$name.png")
            file.outputStream().use{assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,it))}
            device.executeShellCommand("mkdir -p /sdcard/Download/pmddcam-tests")
            device.executeShellCommand("run-as ${context.packageName} cat ${file.absolutePath} > /sdcard/Download/pmddcam-tests/qa-$name.png")
            file.delete()
        }
        evidence("original",photo);evidence("natural",rendered.image)
        rendered.image.recycle()
        for(style in listOf("natural","comic","watercolor","cinema","futuretech","gameboy")){
            val sample=p.snapshot().apply{recipe.style=style;recipe.styleMix=1f;if(style=="natural"){recipe.depth=2.5f;recipe.motionAmount=2f}}
            val image=PmddRenderer.render(photo,analysis.depth,sample).image;evidence(if(style=="natural")"maximum" else style,image);image.recycle()
        }
        photo.recycle()
    }
    @Test fun cameraCaptureEditsAndRecreates() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val device=UiDevice.getInstance(instrumentation)
        device.executeShellCommand("pm grant ${instrumentation.targetContext.packageName} ${Manifest.permission.CAMERA}")
        val context=instrumentation.targetContext
        val store=ProjectStore(context);val existing=store.list().map{it.id}.toSet()
        fun click(text:String){awaitUi(device,By.text(text),30_000,"button: $text").click();device.waitForIdle()}
        fun icon(description:String){awaitUi(device,By.desc(description),30_000,"icon: $description").click();device.waitForIdle()}
        fun dialogButton(id:String){awaitUi(device,By.res("android",id),20_000,"dialog button: $id").click();device.waitForIdle()}
        // Shell-owned evidence survives Gradle uninstalling the test application.
        fun shot(name:String){
            device.executeShellCommand("mkdir -p /sdcard/Download/pmddcam-tests")
            device.executeShellCommand("screencap -p /sdcard/Download/pmddcam-tests/$name")
            assertTrue("Missing screenshot: $name",device.executeShellCommand("ls -s /sdcard/Download/pmddcam-tests/$name").trim().substringBefore(' ').toLongOrNull()?.let{it>0}==true)
        }
        ActivityScenario.launch<MainActivity>(Intent(instrumentation.targetContext,MainActivity::class.java)).use{scenario->
            val streaming=CountDownLatch(1)
            var previewAspect=0f
            fun cameraView(view:View):PreviewView? {
                if(view is PreviewView)return view
                if(view is ViewGroup)for(i in 0 until view.childCount)cameraView(view.getChildAt(i))?.let{return it}
                return null
            }
            scenario.onActivity{activity->
                val preview=cameraView(activity.window.decorView);assertNotNull("Camera preview exists",preview)
                preview!!.previewStreamState.observe(activity){if(it==PreviewView.StreamState.STREAMING)streaming.countDown()}
            }
            assertTrue("Camera produces preview frames",streaming.await(30,TimeUnit.SECONDS))
            scenario.onActivity{activity->
                val preview=cameraView(activity.window.decorView)!!
                previewAspect=preview.width.toFloat()/preview.height
                assertTrue("The camera uses the full screen height",preview.height>=activity.window.decorView.height*.94f)
            }
            val shutter=awaitUi(device,By.desc("Foto aufnehmen"),20_000,"camera shutter");shot("camera.png");shutter.click()
            awaitUi(device,By.text("Original ↔ PMDD"),90_000,"captured photo editor")
            assertTrue("Initial PMDD render finishes",device.wait(Until.gone(By.text("Abbrechen · Original behalten")),30_000));device.waitForIdle();shot("editor.png")
            val captured=store.list().single{it.id !in existing};assertTrue(captured.ready)
            val original=store.original(captured.id).readBytes();assertTrue("Full captured original exists",original.size>1000)
            val capturedImage=store.decode(captured.id,600)
            assertTrue("Saved framing matches the fullscreen preview",abs(capturedImage.width.toFloat()/capturedImage.height-previewAspect)<.08f);capturedImage.recycle()
            click("Original ↔ PMDD");awaitUi(device,By.text("Original"),20_000,"original is selected");shot("original-toggle.png")
            click("Original ↔ PMDD");awaitUi(device,By.text("PMDD Natural"),20_000,"PMDD is selected again")
            icon("Betrachtermodus");click("Mit Finger steuern · ziehen / aufziehen")
            device.swipe(350,500,650,650,20);device.waitForIdle();shot("viewer.png")
            click("Looks");awaitUi(device,By.text("60 Stile · alle mit PMDD"),20_000,"style previews");shot("styles.png");dialogButton("button2")
            click("Looks");click("Cinematic")
            click("Werkzeuge");awaitUi(device,By.text("Tiefe malen"),20_000,"readable tool popup");shot("tools.png");device.pressBack()
            click("PMDD");awaitUi(device,By.text("Tiefe & Ebenen"),20_000,"readable PMDD popup");shot("pmdd-menu.png");click("Tiefe & Ebenen");awaitUi(device,By.textStartsWith("Tiefenebenen"),20_000,"depth settings");shot("settings.png");dialogButton("button1")
            device.waitForIdle();scenario.recreate();scenario.onActivity{assertFalse(it.isFinishing)}
            awaitUi(device,By.text("Original ↔ PMDD"),30_000,"restored editor")
            awaitUi(device,By.text("Cinematic"),30_000,"persisted Cinematic style")
            assertArrayEquals("Editing preserves the camera original",original,store.original(captured.id).readBytes())
        }
    }

    private fun awaitUi(device:UiDevice,selector:BySelector,timeout:Long,description:String):UiObject2 {
        val deadline=SystemClock.uptimeMillis()+timeout
        do {
            // API 35 emulator cold boots can leave a Quickstep ANR over our activity.
            // Recover only this exact external launcher dialog, never a PMDDcam ANR.
            if(device.hasObject(By.pkg("android").text("Quickstep isn't responding"))){
                val close=device.findObject(By.res("android","aerr_close"))
                if(close!=null){
                    device.executeShellCommand("mkdir -p /sdcard/Download/pmddcam-tests")
                    device.executeShellCommand("screencap -p /sdcard/Download/pmddcam-tests/launcher-anr.png")
                    android.util.Log.w("PMDDcamDeviceTest","Closing external Quickstep ANR; app assertions remain active")
                    close.click();device.waitForIdle()
                }
            }
            device.findObject(selector)?.let{return it}
            SystemClock.sleep(100)
        } while(SystemClock.uptimeMillis()<deadline)
        throw AssertionError("Missing UI: $description (foreground: ${device.currentPackageName})")
    }
}
