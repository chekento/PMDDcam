package cloud.kosch.pmddcam

import android.Manifest
import android.content.Intent
import android.graphics.*
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    @Test fun nativeActivityLaunchesAndRecreates() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("pm grant ${instrumentation.targetContext.packageName} ${Manifest.permission.CAMERA}").close()
        ActivityScenario.launch<MainActivity>(Intent(instrumentation.targetContext,MainActivity::class.java)).use{scenario->
            scenario.onActivity{assertFalse(it.isFinishing)};scenario.recreate();scenario.onActivity{assertFalse(it.isFinishing)}
        }
    }
}
