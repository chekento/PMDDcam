package cloud.kosch.pmddcam

import android.Manifest
import android.content.Intent
import android.content.ContentValues
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.provider.MediaStore
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
        assertTrue("Recognized dogs receive dynamic motion roles",analysis.objects.filter{it.name.startsWith("Hund ·")}.all{it.role==Role.DYNAMIC&&it.intensity>.5f})
        assertEquals(256,analysis.depth.width);assertTrue(analysis.depth.values.all{it.isFinite()&&it in 0f..1f})
        assertTrue(analysis.depth.values.max()-analysis.depth.values.min()>.5f)
        p.objects=analysis.objects;p.ready=true;store.saveDepth(p.id,analysis.depth,true);store.saveDepth(p.id,analysis.depth);store.save(p)
        val rendered=PmddRenderer.render(photo,analysis.depth,p);assertEquals(photo.width,rendered.image.width)
        store.thumbnail(p.id,rendered.image);assertArrayEquals(bytes,store.original(p.id).readBytes());assertTrue(store.load(p.id).ready)
        fun evidence(name:String,image:Bitmap){
            if(Build.VERSION.SDK_INT>=29){
                val values=ContentValues().apply{
                    put(MediaStore.MediaColumns.DISPLAY_NAME,"qa-$name.png")
                    put(MediaStore.MediaColumns.MIME_TYPE,"image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/pmddcam-tests")
                    put(MediaStore.MediaColumns.IS_PENDING,1)
                }
                val resolver=context.contentResolver
                val uri=requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values))
                requireNotNull(resolver.openOutputStream(uri)).use{assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,it))}
                resolver.update(uri,ContentValues().apply{put(MediaStore.MediaColumns.IS_PENDING,0)},null,null)
                assertTrue("Missing rendered photo evidence",device.executeShellCommand("ls -s /sdcard/Download/pmddcam-tests/qa-$name.png").trim().substringBefore(' ').toLongOrNull()?.let{it>0}==true)
            }
        }
        evidence("original",photo);evidence("vivid",rendered.image)
        rendered.image.recycle()
        for(style in listOf("natural","comic","watercolor","oil","cinema","futuretech","gameboy","risocomic","pointillism","thermal")){
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
            assertFalse("Shutter sound defaults off",context.getSharedPreferences("pmdd",0).getBoolean("shutterSound",false))
            icon("Kamera-Einstellungen")
            val sound=awaitUi(device,By.textStartsWith("Auslöseton:"),20_000,"shutter sound setting")
            if(!sound.text.contains("Gerät")){
                assertEquals("Auslöseton: Aus",sound.text);shot("camera-settings.png");sound.click();awaitUi(device,By.text("Auslöseton: An"),20_000,"sound enabled in camera status");device.waitForIdle()
                assertTrue("Sound toggle persists",context.getSharedPreferences("pmdd",0).getBoolean("shutterSound",false))
                scenario.recreate()
                val restarted=CountDownLatch(1)
                scenario.onActivity{activity->cameraView(activity.window.decorView)!!.previewStreamState.observe(activity){if(it==PreviewView.StreamState.STREAMING)restarted.countDown()}}
                assertTrue("Camera resumes after sound preference restore",restarted.await(30,TimeUnit.SECONDS))
                icon("Kamera-Einstellungen");awaitUi(device,By.text("Auslöseton: An"),20_000,"restored sound setting").click();awaitUi(device,By.text("Auslöseton: Aus"),20_000,"sound disabled in camera status");device.waitForIdle()
                assertFalse(context.getSharedPreferences("pmdd",0).getBoolean("shutterSound",false))
            }else device.pressBack()
            val shutter=awaitUi(device,By.desc("Foto aufnehmen"),20_000,"camera shutter");shot("camera.png");shutter.click()
            awaitUi(device,By.text("Original ↔ PMDD"),90_000,"captured photo editor")
            assertTrue("Initial PMDD render finishes",device.wait(Until.gone(By.text("Abbrechen · Original behalten")),30_000));device.waitForIdle();shot("editor.png")
            val captured=store.list().single{it.id !in existing};assertTrue(captured.ready)
            val original=store.original(captured.id).readBytes();assertTrue("Full captured original exists",original.size>1000)
            val capturedImage=store.decode(captured.id,600)
            assertTrue("Saved framing matches the fullscreen preview",abs(capturedImage.width.toFloat()/capturedImage.height-previewAspect)<.08f);capturedImage.recycle()
            click("Original ↔ PMDD");awaitUi(device,By.text("Original"),20_000,"original is selected");shot("original-toggle.png")
            click("Original ↔ PMDD");awaitUi(device,By.text("PMDD Vivid"),20_000,"PMDD is selected again")
            icon("Betrachtermodus");click("Mit Finger steuern · ziehen / aufziehen")
            device.swipe(350,500,650,650,20);device.waitForIdle();shot("viewer.png")
            click("Looks");awaitUi(device,By.text("80 Stile · alle mit PMDD"),20_000,"style previews");shot("styles.png");dialogButton("button2")
            click("Looks");click("Cinematic")
            click("Werkzeuge");awaitUi(device,By.text("Tiefe malen"),20_000,"readable tool popup");shot("tools.png");device.pressBack()
            click("PMDD");awaitUi(device,By.text("Tiefe & Ebenen"),20_000,"readable PMDD popup");shot("pmdd-menu.png");click("Tiefe & Ebenen");awaitUi(device,By.textStartsWith("Tiefenebenen"),20_000,"depth settings");shot("settings.png");dialogButton("button1")
            device.waitForIdle();scenario.recreate();scenario.onActivity{assertFalse(it.isFinishing)}
            awaitUi(device,By.text("Original ↔ PMDD"),30_000,"restored editor")
            awaitUi(device,By.text("Cinematic"),30_000,"persisted Cinematic style")
            assertArrayEquals("Editing preserves the camera original",original,store.original(captured.id).readBytes())
        }
    }

    @Test fun viewerRendersSixDirectionsAndKeepsStaticModeStill() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val device=UiDevice.getInstance(instrumentation)
        device.executeShellCommand("pm grant ${instrumentation.targetContext.packageName} ${Manifest.permission.CAMERA}")
        val image=Bitmap.createBitmap(IntArray(128*128){i->val x=i%128;val y=i/128
            if(x in 30..86&&y in 25..92){if((x/5+y/7)%2==0)0xffffaa20.toInt() else 0xff3d9250.toInt()}
            else if((x/12+y/12)%2==0)0xff293c7e.toInt() else 0xffa4c7e2.toInt()},128,128,Bitmap.Config.ARGB_8888)
        val depth=DepthMap(64,64,FloatArray(4096){i->if(i%64 in 15..43&&i/64 in 12..46).85f else .18f})
        val objects=listOf(SceneObject(1,"Moving foreground",.23f,.18f,.69f,.74f,Role.DYNAMIC,Motion.DRIFT,15f,.7f,.9f,.85f),SceneObject(2,"Stable anchor",.28f,.3f,.4f,.65f,Role.ANCHOR,depth=.85f))
        ActivityScenario.launch<MainActivity>(Intent(instrumentation.targetContext,MainActivity::class.java)).use{scenario->
            lateinit var view:DepthViewer
            scenario.onActivity{activity->view=DepthViewer(activity);activity.setContentView(view);view.setImage(image,depth,Recipe(),objects);view.active=true}
            fun awaitFrame(count:Int){val deadline=SystemClock.uptimeMillis()+10_000;while(view.completedFrames<count&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(20);assertTrue("GL frame completes",view.completedFrames>=count)}
            awaitFrame(1)
            fun frame(x:Float,y:Float,z:Float,active:Boolean=true,time:Float=0f):Bitmap{
                val previous=view.completedFrames
                scenario.onActivity{view.animationTimeOverride=time;view.active=active;view.setPosition(x,y,z)};awaitFrame(previous+1)
                // A second draw ensures PixelCopy sees the requested position after buffer swap.
                val next=view.completedFrames;view.requestRender();awaitFrame(next+1)
                val result=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);val done=CountDownLatch(1);var code=-1
                scenario.onActivity{PixelCopy.request(view,result,{value->code=value;done.countDown()},Handler(Looper.getMainLooper()))}
                assertTrue(done.await(10,TimeUnit.SECONDS));assertEquals(PixelCopy.SUCCESS,code);return result
            }
            fun difference(a:Bitmap,b:Bitmap):Double{val aa=IntArray(192*192);val bb=IntArray(aa.size);a.getPixels(aa,0,192,0,0,192,192);b.getPixels(bb,0,192,0,0,192,192);return aa.indices.sumOf{i->listOf(0,8,16).sumOf{shift->abs(((aa[i]shr shift)and 255)-((bb[i]shr shift)and 255))}}.toDouble()/aa.size/3}
            val center=frame(0f,0f,0f)
            for((name,position) in listOf("left" to floatArrayOf(.85f,0f,0f),"right" to floatArrayOf(-.85f,0f,0f),"up" to floatArrayOf(0f,.85f,0f),"down" to floatArrayOf(0f,-.85f,0f),"near" to floatArrayOf(0f,0f,.85f),"far" to floatArrayOf(0f,0f,-.85f))){
                val moved=frame(position[0],position[1],position[2]);assertTrue("$name changes rendered pixels",difference(center,moved)>.2)
                device.executeShellCommand("mkdir -p /sdcard/Download/pmddcam-tests");device.executeShellCommand("screencap -p /sdcard/Download/pmddcam-tests/axis-$name.png");moved.recycle()
            }
            val animated=frame(0f,0f,0f,time=3f)
            assertTrue("Object animation changes pixels even with a stationary viewpoint",difference(center,animated)>.2)
            val screenRatio=view.width.toFloat()/view.height
            val anchorX=(192*.34f).toInt();val anchorY=(192*(.5f+(.475f-.5f)*screenRatio)).toInt()
            assertEquals("A protected anchor must stay still during autonomous animation",center.getPixel(anchorX,anchorY),animated.getPixel(anchorX,anchorY))
            assertEquals("Background outside object masks must stay still",center.getPixel(15,96),animated.getPixel(15,96))
            device.executeShellCommand("screencap -p /sdcard/Download/pmddcam-tests/animation-active.png")
            animated.recycle()
            scenario.onActivity{view.recipe=view.recipe.copy(animateObjects=false)}
            val disabled=frame(0f,0f,0f,time=3f);assertTrue("Animation can be disabled independently",difference(center,disabled)<.02);disabled.recycle()
            scenario.onActivity{view.recipe=view.recipe.copy(animateObjects=true)}
            val fixed=frame(.85f,-.85f,.85f,false,time=3f)
            assertTrue("Static output does not follow simulated head positions",difference(center,fixed)<.02)
            fixed.recycle();center.recycle()
            scenario.onActivity{view.active=true;view.animationTimeOverride=null;view.onPause()}
            val paused=view.completedFrames;SystemClock.sleep(150);assertEquals("Paused viewer stops rendering animation",paused,view.completedFrames)
            scenario.onActivity{view.onResume()};awaitFrame(paused+1)
            scenario.onActivity{view.release();view.onPause()}
        };image.recycle()
    }

    private fun awaitUi(device:UiDevice,selector:BySelector,timeout:Long,description:String):UiObject2 {
        val deadline=SystemClock.uptimeMillis()+timeout
        do {
            // Android shows this one-time education overlay on the first immersive screen.
            if(device.hasObject(By.pkg("android").text("Viewing full screen"))){
                val confirm=device.findObject(By.res("android","ok"))
                if(confirm!=null){confirm.click();device.waitForIdle();continue}
            }
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
