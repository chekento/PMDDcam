package cloud.kosch.pmddcam

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoreTest {
    @Test fun `high defaults and invalid recipe inputs are bounded`() {
        assertEquals(64,Recipe().layers);assertTrue(Recipe().depth>=.8f)
        val r=Recipe.from(JSONObject("{\"layers\":900,\"depth\":-7,\"outputSize\":90000,\"style\":\"missing\"}"))
        assertEquals(128,r.layers);assertEquals(0f,r.depth,0f);assertEquals(4096,r.outputSize);assertEquals("natural",r.style)
        val roundTrip=Recipe.from(Recipe().copy(layers=96,vertical=false,style="comic",motion=false).json())
        assertEquals(96,roundTrip.layers);assertFalse(roundTrip.vertical);assertFalse(roundTrip.motion);assertEquals("comic",roundTrip.style)
    }
    @Test fun `depth normalization handles constant maps and non-finite pixels`() {
        val flat=DepthMap.normalize(FloatArray(16){8f},4,4);assertTrue(flat.values.all{it==.5f})
        val d=DepthMap.normalize(floatArrayOf(Float.NaN,0f,1f,2f),2,2)
        assertTrue(d.values.all{it.isFinite()&&it in 0f..1f})
        val map=DepthMap(2,2,floatArrayOf(0f,1f,0f,1f));assertEquals(.5f,map.sample(.5f,.3f),.0001f)
    }
    @Test fun `all sixty styles render distinct deterministic images without editing original`()=runBlocking {
        assertEquals(60,Styles.all.size);assertEquals(60,Styles.all.map{it.id}.toSet().size)
        val image=fixture();val before=pixels(image);val depth=DepthMap(8,8,FloatArray(64){it/63f})
        val fingerprints=mutableSetOf<Int>()
        for(s in Styles.all){
            val p=Project("test",0,Recipe(style=s.id,styleMix=1f),mutableListOf(SceneObject(999,"Atmosphäre",0f,0f,1f,1f,Role.ATMOSPHERE)))
            val result=PmddRenderer.render(image,depth,p);fingerprints+=pixels(result.image).contentHashCode();assertArrayEquals(before,pixels(image));result.image.recycle()
        }
        assertEquals("Every style must produce a distinct recipe output",60,fingerprints.size)
        val p=Project("test",0,Recipe(),mutableListOf())
        val a=PmddRenderer.render(image,depth,p);val b=PmddRenderer.render(image,depth,p)
        assertArrayEquals(pixels(a.image),pixels(b.image))
        image.recycle();a.image.recycle();b.image.recycle()
    }
    @Test fun `motion masks keep anchors and out of region pixels isolated`() {
        val o=SceneObject(1,"Test",.2f,.2f,.8f,.8f,depth=.7f)
        assertEquals(0f,MotionMath.mask(o,.1f,.4f,.7f),0f)
        assertTrue(MotionMath.mask(o,.5f,.5f,.7f)>.99f)
        o.enabled=false;assertEquals(0f,MotionMath.mask(o,.5f,.5f,.7f),0f)
        assertTrue(MotionMath.wave(.1f)<MotionMath.wave(.6f))
    }
    @Test fun `original survives reediting and project archive round trip`() {
        val store=ProjectStore(RuntimeEnvironment.getApplication());val p=store.create(Recipe())
        val image=fixture();store.original(p.id).outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)}
        val original=store.original(p.id).readBytes();val d=DepthMap(4,4,FloatArray(16){it/15f})
        store.saveDepth(p.id,d);store.saveDepth(p.id,d,true);p.ready=true;p.recipe.style="watercolor";store.save(p)
        assertArrayEquals(original,store.original(p.id).readBytes())
        val loaded=store.load(p.id);assertEquals("watercolor",loaded.recipe.style);assertArrayEquals(d.values,store.depth(p.id).values,0f)
        val out=ByteArrayOutputStream();store.archive(p,out)
        var archivedOriginal:ByteArray?=null
        ZipInputStream(out.toByteArray().inputStream()).use{zip->while(true){val e=zip.nextEntry?:break;if(e.name=="original.image")archivedOriginal=zip.readBytes();zip.closeEntry()}}
        assertArrayEquals(original,archivedOriginal);image.recycle()
    }
    @Test fun `project ids cannot leave private project root`() {
        val store=ProjectStore(RuntimeEnvironment.getApplication())
        assertThrows(IllegalArgumentException::class.java){store.dir("../../escape")}
    }
    private fun fixture():Bitmap = Bitmap.createBitmap(IntArray(64*48){i->val x=i%64;val y=i/64;0xff000000.toInt() or ((x*4) shl 16) or ((y*5) shl 8) or ((x*7+y*9)%256)},64,48,Bitmap.Config.ARGB_8888)
    private fun pixels(b:Bitmap)=IntArray(b.width*b.height).also{b.getPixels(it,0,b.width,0,0,b.width,b.height)}
}
