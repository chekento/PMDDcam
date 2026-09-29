package cloud.kosch.pmddcam

import android.graphics.Bitmap
import kotlin.math.abs
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
    @Test fun `flat surfaces cannot acquire bands or phantom shadows from depth and motion`()=runBlocking {
        val photo=Bitmap.createBitmap(128,96,Bitmap.Config.ARGB_8888).apply{eraseColor(0xff9cacbd.toInt())}
        val d=DepthMap(32,24,FloatArray(32*24){i->if(i%32<16).1f else .9f})
        val recipe=Recipe(depth=2.5f,relief=1f,occlusion=1f,motionAmount=2f,texture=1f,
            sharpness=0f,separation=0f,haze=0f,bokeh=0f,vignette=0f,contrast=0f,saturation=1f,styleMix=0f)
        val objects=mutableListOf(SceneObject(999,"Atmosphäre",0f,0f,1f,1f,Role.ATMOSPHERE),
            SceneObject(1,"Dynamisch",.1f,.1f,.9f,.9f,intensity=1f,depth=.5f))
        val result=PmddRenderer.render(photo,d,Project("flat",0,recipe,objects))
        assertArrayEquals("Depth transitions must not paint shadows or waves onto a flat photo",pixels(photo),pixels(result.image))
        photo.recycle();result.image.recycle()
    }
    @Test fun `maximum depth produces substantially stronger image cues than low depth`()=runBlocking {
        val photo=fixture();val d=DepthMap(8,8,FloatArray(64){it/63f})
        val r=Recipe(styleMix=0f,sharpness=.15f,texture=0f,vignette=0f,contrast=0f,motion=false,relief=.8f,separation=.8f)
        suspend fun render(amount:Float)=PmddRenderer.render(photo,d,Project("strength",0,r.copy(depth=amount),mutableListOf())).image
        val base=render(0f);val low=render(.35f);val high=render(2.5f)
        fun difference(a:Bitmap,b:Bitmap):Double{val pa=pixels(a);val pb=pixels(b);return pa.indices.sumOf{i->listOf(0,8,16).sumOf{shift->abs(((pa[i] shr shift)and 255)-((pb[i] shr shift)and 255))}}.toDouble()/pa.size/3}
        assertTrue("The extended range must change pixels, not only the slider label",difference(high,base)>difference(low,base)*1.8)
        listOf(photo,base,low,high).forEach{it.recycle()}
    }
    @Test fun `zero style mix fully disables every style including pixel palettes`()=runBlocking {
        val photo=fixture();val d=DepthMap(8,8,FloatArray(64){it/63f});var baseline:IntArray?=null
        for(style in Styles.all){
            val result=PmddRenderer.render(photo,d,Project("mix",0,Recipe(style=style.id,styleMix=0f),mutableListOf()))
            val values=pixels(result.image);if(baseline==null)baseline=values else assertArrayEquals(style.name,baseline,values)
            result.image.recycle()
        };photo.recycle()
    }
    @Test fun `continuous depth and new intensity ranges survive recipe round trip`() {
        val recipe=Recipe.from(Recipe(layers=8,depth=2.5f,motionAmount=2f,parallax=2f).json())
        assertEquals(2.5f,recipe.depth,0f);assertEquals(2f,recipe.motionAmount,0f);assertEquals(2f,recipe.parallax,0f)
        val d=DepthMap(32,8,FloatArray(256){it/255f})
        val p=Project("continuous",0,recipe,mutableListOf())
        assertArrayEquals("Eight viewer planes must not posterize the photo depth",d.values,PmddRenderer.effectiveDepth(d,p).values,0f)
        p.objects+=SceneObject(1,"Foreground",.1f,.1f,.9f,.9f,depth=.95f,overrideDepth=true)
        assertEquals(.95f,PmddRenderer.effectiveDepth(d,p).sample(.5f,.5f),.001f)
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
