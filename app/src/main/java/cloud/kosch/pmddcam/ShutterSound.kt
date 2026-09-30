package cloud.kosch.pmddcam

import android.media.MediaActionSound
import android.util.Log
import androidx.camera.core.CameraInfo
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** System shutter audio, serialized off the UI thread and tied to actual capture start. */
internal class ShutterSound(
    private val enabled:()->Boolean,
    private val required:()->Boolean={CameraInfo.mustPlayShutterSound()},
    private val createPlayer:()->Player={SystemPlayer()},
    private val worker:ExecutorService=Executors.newSingleThreadExecutor()
):AutoCloseable {
    internal interface Player { fun load();fun play();fun release() }
    private class SystemPlayer:Player {
        private val sound=MediaActionSound()
        override fun load()=sound.load(MediaActionSound.SHUTTER_CLICK)
        override fun play()=sound.play(MediaActionSound.SHUTTER_CLICK)
        override fun release()=sound.release()
    }
    // Until the policy query finishes, do not offer a way to suppress required audio.
    @Volatile var requiredByDevice=true
        private set
    private var player:Player?=null
    private var closed=false
    init{refresh()}
    fun refresh()=enqueue{queryPolicy();if(requiredByDevice||enabled())player()}
    fun onCaptureStarted()=enqueue{
        // Recheck at the exposure, including after a timer or regional policy change.
        queryPolicy();if(requiredByDevice||enabled())player().play()
    }
    private fun queryPolicy(){
        requiredByDevice=try{required()}catch(e:RuntimeException){
            Log.w("PMDDcam","Shutter policy unavailable; keeping sound enabled",e);true
        }
    }
    private fun player():Player=player?:createPlayer().also{next->
        try{next.load();player=next}catch(e:RuntimeException){next.release();throw e}
    }
    @Synchronized private fun enqueue(action:()->Unit){
        if(!closed)worker.execute{try{action()}catch(e:RuntimeException){Log.e("PMDDcam","System shutter audio unavailable",e)}}
    }
    @Synchronized override fun close(){
        if(closed)return
        closed=true
        worker.execute{try{player?.release()}finally{player=null}}
        worker.shutdown()
    }
}
