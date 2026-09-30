package cloud.kosch.pmddcam

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ShutterSoundTest {
    @Test fun `capture rechecks policy and serializes preload play and release`() {
        val events=mutableListOf<String>();val worker=Executors.newSingleThreadExecutor()
        var enabled=false;var required=false
        val player=object:ShutterSound.Player{
            override fun load(){events+="load"}
            override fun play(){events+="play"}
            override fun release(){events+="release"}
        }
        val sound=ShutterSound({enabled},{required},{player},worker)
        fun drain(){worker.submit{}.get(5,TimeUnit.SECONDS)}
        drain();assertFalse(sound.requiredByDevice)
        sound.onCaptureStarted();drain();assertTrue("Optional sound stays off",events.isEmpty())
        enabled=true;sound.refresh();drain();assertEquals(listOf("load"),events)
        sound.onCaptureStarted();drain();assertEquals(listOf("load","play"),events)
        enabled=false;required=true
        sound.onCaptureStarted();drain()
        assertTrue("Capture observes a changed device policy",sound.requiredByDevice)
        assertEquals(listOf("load","play","play"),events)
        sound.close();sound.close();sound.onCaptureStarted()
        assertTrue(worker.awaitTermination(5,TimeUnit.SECONDS))
        assertEquals("Release happens once, after queued captures",listOf("load","play","play","release"),events)
    }
    @Test fun `failed policy query cannot silently disable a required shutter`() {
        val worker=Executors.newSingleThreadExecutor();var plays=0
        val player=object:ShutterSound.Player{
            override fun load(){}
            override fun play(){plays++}
            override fun release(){}
        }
        val sound=ShutterSound({false},{throw IllegalStateException("Policy service unavailable")},{player},worker)
        sound.onCaptureStarted();sound.close()
        assertTrue(worker.awaitTermination(5,TimeUnit.SECONDS))
        assertTrue(sound.requiredByDevice);assertEquals(1,plays)
    }
}
