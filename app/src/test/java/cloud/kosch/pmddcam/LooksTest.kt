package cloud.kosch.pmddcam

import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LooksTest {
    private val neutral=Recipe(depth=0f,sharpness=0f,bokeh=0f,texture=0f,contrast=0f,vignette=0f,saturation=1f,motion=false)
    private suspend fun render(photo:Bitmap,style:String,recipe:Recipe=neutral):Bitmap=PmddRenderer.render(photo,DepthMap(8,8,FloatArray(64){.5f}),Project("style",0,recipe.copy(style=style),mutableListOf())).image
    @Test fun `pocket and CGA styles retain four colors and solid pixel blocks`()=runBlocking {
        val photo=Bitmap.createBitmap(IntArray(160*120){i->0xff000000.toInt()or((i%160*255/159)shl 16)or((i/160*255/119)shl 8)or((i*73)%256)},160,120,Bitmap.Config.ARGB_8888)
        for(style in listOf("gameboy","cga"))for(recipe in listOf(neutral,Recipe())){
            val result=render(photo,style,recipe);val values=IntArray(160*120);result.getPixels(values,0,160,0,0,160,120)
            assertTrue("A four-color console must not retain photographic shading",values.toSet().size<=4)
            for(y in 0 until 120 step 2)for(x in 0 until 160 step 2){assertEquals(result.getPixel(x,y),result.getPixel(x+1,y));assertEquals(result.getPixel(x,y),result.getPixel(x,y+1))}
            result.recycle()
        };photo.recycle()
    }
    @Test fun `watercolor simplifies fine photographic texture while neon draws emissive boundaries`()=runBlocking {
        val w=160;val h=120
        val photo=Bitmap.createBitmap(IntArray(w*h){i->val grain=if((i%w+i/w)%2==0)18 else -18;val n=(if(i%w<80)100 else 185)+grain;0xff000000.toInt()or(n shl 16)or(n shl 8)or n},w,h,Bitmap.Config.ARGB_8888)
        val water=render(photo,"watercolor")
        fun energy(image:Bitmap):Double{var sum=0.0;for(y in 20..99)for(x in 20..59)sum+=abs((image.getPixel(x,y)and 255)-(image.getPixel(x+1,y)and 255));return sum/(80*40)}
        assertTrue("A watercolor wash must remove most high-frequency photo texture",energy(water)<energy(photo)*.5)
        val flat=Bitmap.createBitmap(IntArray(w*h){i->val n=if(i%w<80)30 else 160;0xff000000.toInt()or(n shl 16)or(n shl 8)or n},w,h,Bitmap.Config.ARGB_8888)
        val neon=render(flat,"futuretech")
        assertTrue("Contours must emit visibly more cyan light than flat regions",(neon.getPixel(79,60)and 255)>(neon.getPixel(30,60)and 255)+35)
        listOf(photo,water,flat,neon).forEach{it.recycle()}
    }
    @Test fun `animation settings round trip independently of static illusion`() {
        val r=Recipe.from(Recipe(motion=false,animateObjects=true,animationAmount=1.7f,animationSpeed=1.3f).json())
        assertFalse(r.motion);assertTrue(r.animateObjects);assertEquals(1.7f,r.animationAmount,0f);assertEquals(1.3f,r.animationSpeed,0f)
        val bounded=Recipe(animationAmount=Float.NaN,animationSpeed=99f).normalized()
        assertTrue(bounded.animationAmount.isFinite());assertEquals(2f,bounded.animationSpeed,0f)
        val map=DepthMap(16,16,FloatArray(256){.5f})
        val moving=SceneObject(1,"Auto",.1f,.1f,.9f,.9f,Role.DYNAMIC,depth=.5f)
        val a=MotionField.create(map,listOf(moving),r)
        val fixed=MotionField.create(map,listOf(moving.copy(role=Role.ANCHOR)),r)
        assertTrue((a.vectors.getPixel(8,8)and 255)>0)
        assertEquals(0,fixed.vectors.getPixel(8,8)and 255)
        assertTrue((a.timing.getPixel(8,8)and 255)>0)
        listOf(a.vectors,a.timing,fixed.vectors,fixed.timing).forEach{it.recycle()}
    }
}
