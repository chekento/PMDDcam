package cloud.kosch.pmddcam

import android.Manifest
import android.content.Intent
import android.graphics.*
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTest {
    @Test fun bundledModelAndProjectPipelineWorkOnDevice()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=ProjectStore(context);val p=store.create(Recipe())
        val photo=Bitmap.createBitmap(384,512,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(photo);val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader=LinearGradient(0f,0f,384f,512f,Color.rgb(40,90,130),Color.rgb(200,170,100),Shader.TileMode.CLAMP);canvas.drawPaint(paint);paint.shader=null
        paint.color=Color.rgb(230,85,50);canvas.drawCircle(230f,300f,95f,paint);paint.color=Color.rgb(30,80,50);canvas.drawRect(25f,120f,90f,390f,paint)
        store.original(p.id).outputStream().use{assertTrue(photo.compress(Bitmap.CompressFormat.JPEG,95,it))}
        val bytes=store.original(p.id).readBytes()
        val analysis=SceneAnalyzer(context).analyze(photo,true){}
        assertEquals(256,analysis.depth.width);assertTrue(analysis.depth.values.all{it.isFinite()&&it in 0f..1f})
        assertTrue(analysis.depth.values.max()-analysis.depth.values.min()>.5f)
        p.objects=analysis.objects;p.ready=true;store.saveDepth(p.id,analysis.depth,true);store.saveDepth(p.id,analysis.depth);store.save(p)
        val rendered=PmddRenderer.render(photo,analysis.depth,p);assertEquals(photo.width,rendered.image.width)
        store.thumbnail(p.id,rendered.image);assertArrayEquals(bytes,store.original(p.id).readBytes());assertTrue(store.load(p.id).ready)
        rendered.image.recycle();photo.recycle()
    }
    @Test fun nativeActivityLaunchesEditsAndRecreates() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("pm grant ${instrumentation.targetContext.packageName} ${Manifest.permission.CAMERA}").close()
        val context=instrumentation.targetContext
        val store=ProjectStore(context);val p=store.create(Recipe())
        val b=Bitmap.createBitmap(320,480,Bitmap.Config.ARGB_8888)
        val c=Canvas(b);val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader=LinearGradient(0f,0f,320f,480f,Color.rgb(17,48,84),Color.rgb(85,190,155),Shader.TileMode.CLAMP);c.drawPaint(paint);paint.shader=null
        paint.color=Color.rgb(188,168,243);c.drawCircle(200f,285f,75f,paint);paint.color=Color.rgb(246,213,130);c.drawCircle(95f,180f,45f,paint)
        store.original(p.id).outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}
        val d=DepthMap(64,64,FloatArray(4096){i->(i/64)/63f});store.saveDepth(p.id,d);store.saveDepth(p.id,d,true);store.thumbnail(p.id,b);p.ready=true;store.save(p);b.recycle()
        val device=UiDevice.getInstance(instrumentation)
        fun click(text:String){val obj=device.wait(Until.findObject(By.text(text)),30_000);assertNotNull("Missing UI: $text",obj);obj.click();device.waitForIdle()}
        fun dialogButton(id:String){val obj=device.wait(Until.findObject(By.res("android",id)),20_000);assertNotNull("Missing dialog button: $id",obj);obj.click();device.waitForIdle()}
        // Shell-owned evidence survives Gradle uninstalling the test application.
        fun shot(name:String){
            device.executeShellCommand("mkdir -p /sdcard/Download/pmddcam-tests")
            device.executeShellCommand("screencap -p /sdcard/Download/pmddcam-tests/$name")
            assertTrue("Missing screenshot: $name",device.executeShellCommand("ls -s /sdcard/Download/pmddcam-tests/$name").trim().substringBefore(' ').toLongOrNull()?.let{it>0}==true)
        }
        ActivityScenario.launch<MainActivity>(Intent(instrumentation.targetContext,MainActivity::class.java)).use{scenario->
            scenario.onActivity{assertFalse(it.isFinishing)}
            assertNotNull(device.wait(Until.findObject(By.text("Sammlung")),20_000));shot("camera.png")
            click("Sammlung")
            val card=device.wait(Until.findObject(By.text("64 Layer · weiter bearbeiten")),20_000);assertNotNull(card);card.click()
            assertNotNull(device.wait(Until.findObject(By.text("Original / PMDD")),30_000))
            device.wait(Until.gone(By.text("Projekt wird geöffnet …")),30_000);device.waitForIdle();shot("editor.png")
            click("Betrachtermodus");click("Mit Finger steuern · ziehen / aufziehen")
            device.swipe(350,500,650,650,20);device.waitForIdle();shot("viewer.png")
            click("Stile · 60");assertNotNull(device.wait(Until.findObject(By.text("PMDD Natural")),20_000));shot("styles.png");dialogButton("button2")
            click("PMDD");click("Tiefe & Ebenen");assertNotNull(device.wait(Until.findObject(By.textStartsWith("Tiefenebenen")),20_000));shot("settings.png");dialogButton("button1")
            device.waitForIdle();scenario.recreate();scenario.onActivity{assertFalse(it.isFinishing)}
            assertNotNull(device.wait(Until.findObject(By.text("Original / PMDD")),30_000))
        }
    }
}
