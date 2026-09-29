package cloud.kosch.pmddcam

import android.Manifest
import android.content.Intent
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTest {
    @Test fun bundledModelAndProjectPipelineWorkOnDevice()=runBlocking {
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
        rendered.image.recycle();photo.recycle()
    }
    @Test fun cameraCaptureEditsAndRecreates() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val device=UiDevice.getInstance(instrumentation)
        device.executeShellCommand("pm grant ${instrumentation.targetContext.packageName} ${Manifest.permission.CAMERA}")
        val context=instrumentation.targetContext
        val store=ProjectStore(context);val existing=store.list().map{it.id}.toSet()
        fun click(text:String){val obj=device.wait(Until.findObject(By.text(text)),30_000);assertNotNull("Missing UI: $text",obj);obj.click();device.waitForIdle()}
        fun dialogButton(id:String){val obj=device.wait(Until.findObject(By.res("android",id)),20_000);assertNotNull("Missing dialog button: $id",obj);obj.click();device.waitForIdle()}
        // Shell-owned evidence survives Gradle uninstalling the test application.
        fun shot(name:String){
            device.executeShellCommand("mkdir -p /sdcard/Download/pmddcam-tests")
            device.executeShellCommand("screencap -p /sdcard/Download/pmddcam-tests/$name")
            assertTrue("Missing screenshot: $name",device.executeShellCommand("ls -s /sdcard/Download/pmddcam-tests/$name").trim().substringBefore(' ').toLongOrNull()?.let{it>0}==true)
        }
        ActivityScenario.launch<MainActivity>(Intent(instrumentation.targetContext,MainActivity::class.java)).use{scenario->
            val streaming=CountDownLatch(1)
            fun cameraView(view:View):PreviewView? {
                if(view is PreviewView)return view
                if(view is ViewGroup)for(i in 0 until view.childCount)cameraView(view.getChildAt(i))?.let{return it}
                return null
            }
            scenario.onActivity{activity->
                val preview=cameraView(activity.window.decorView);assertNotNull("Camera preview exists",preview)
                preview!!.previewStreamState.observe(activity){if(it==PreviewView.StreamState.STREAMING)streaming.countDown()}
            }
            assertTrue("Camera produces preview frames",streaming.await(30,TimeUnit.SECONDS));shot("camera.png")
            val shutter=device.wait(Until.findObject(By.desc("Foto aufnehmen")),20_000);assertNotNull(shutter);shutter.click()
            assertNotNull(device.wait(Until.findObject(By.text("Original / PMDD")),90_000))
            device.wait(Until.gone(By.text("Abbrechen · Original behalten")),30_000);device.waitForIdle();shot("editor.png")
            val captured=store.list().single{it.id !in existing};assertTrue(captured.ready)
            val original=store.original(captured.id).readBytes();assertTrue("Full captured original exists",original.size>1000)
            click("Betrachtermodus");click("Mit Finger steuern · ziehen / aufziehen")
            device.swipe(350,500,650,650,20);device.waitForIdle();shot("viewer.png")
            click("Stile · 60");assertNotNull(device.wait(Until.findObject(By.text("PMDD Natural")),20_000));shot("styles.png");dialogButton("button2")
            click("Stile · 60");click("Cinematic")
            click("PMDD");click("Tiefe & Ebenen");assertNotNull(device.wait(Until.findObject(By.textStartsWith("Tiefenebenen")),20_000));shot("settings.png");dialogButton("button1")
            device.waitForIdle();scenario.recreate();scenario.onActivity{assertFalse(it.isFinishing)}
            assertNotNull(device.wait(Until.findObject(By.text("Original / PMDD")),30_000))
            assertNotNull(device.wait(Until.findObject(By.textStartsWith("Cinematic  ·")),30_000))
            assertArrayEquals("Editing preserves the camera original",original,store.original(captured.id).readBytes())
        }
    }
}
