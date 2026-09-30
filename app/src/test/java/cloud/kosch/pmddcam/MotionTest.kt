package cloud.kosch.pmddcam

import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MotionTest {
    @Test fun `people animals and vehicles are dynamic while furniture is stable`() {
        for(category in listOf(1,3,5,9,16,18))assertEquals(Role.DYNAMIC,ObjectMotion.profile(category).role)
        for(category in listOf(10,11,13,15,62,63,67,85))assertEquals(Role.ANCHOR,ObjectMotion.profile(category).role)
        assertNotEquals(ObjectMotion.profile(5).motion,ObjectMotion.profile(1).motion)
    }
    @Test fun `motion changes textured objects but leaves outside regions and anchors exact`()=runBlocking {
        val w=128;val h=96
        val source=Bitmap.createBitmap(IntArray(w*h){i->val n=80+((i%w*17+i/w*11)%120);0xff000000.toInt()or(n shl 16)or(n shl 8)or n},w,h,Bitmap.Config.ARGB_8888)
        val d=DepthMap(16,12,FloatArray(192){.5f})
        val r=Recipe(depth=0f,styleMix=0f,sharpness=0f,bokeh=0f,contrast=0f,saturation=1f,vignette=0f,texture=0f,motionAmount=2f,peripheral=0f,depthCoupling=0f)
        val mover=SceneObject(1,"Auto",.1f,.1f,.6f,.9f,Role.DYNAMIC,Motion.DRIFT,15f,.8f,1f,.5f)
        suspend fun render(o:SceneObject)=PmddRenderer.render(source,d,Project("motion",0,r,mutableListOf(o))).image
        val active=render(mover);val locked=render(mover.copy(role=Role.ANCHOR))
        var differences=0
        for(y in 0 until h)for(x in 0 until w){
            assertEquals("Anchors must not acquire motion cues",source.getPixel(x,y),locked.getPixel(x,y))
            if(x>w*.65)assertEquals("No field outside the object",source.getPixel(x,y),active.getPixel(x,y))
            if(active.getPixel(x,y)!=source.getPixel(x,y))differences++
        }
        assertTrue("Static cues must have visible pixel effect in a textured region",differences>200)
        listOf(source,active,locked).forEach{it.recycle()}
    }
    @Test fun `weather masks follow the sky and survive saved project recipes`()=runBlocking {
        val w=128;val h=128
        val photo=Bitmap.createBitmap(IntArray(w*h){i->val x=i%w;val y=i/w
            when{y>=80->0xffdddddd.toInt();x in 18..66&&y in 8..30->0xffeeeeee.toInt();x in 18..66&&y in 40..64->0xff939393.toInt();else->0xff5eaff7.toInt()}},w,h,Bitmap.Config.ARGB_8888)
        val depth=DepthMap(w,h,FloatArray(w*h){if(it/w>=80).8f else .1f})
        val objects=AtmosphereAnalyzer.detect(photo,depth)
        assertTrue(objects.any{it.id==2000});assertTrue(objects.any{it.id==2001})
        val cloud=SceneObject.from(objects.first{it.id==2000}.json())
        assertTrue(cloud.mask!!.sample(.3f,.15f)>.5f)
        for(o in objects)assertEquals("Bright foreground architecture stays outside atmosphere",0f,o.mask!!.sample(.3f,.8f),0f)
        photo.recycle()
    }
    @Test fun `viewer object texture protects anchors and face regions`() {
        val map=DepthMap(32,32,FloatArray(1024){.5f})
        val moving=SceneObject(1,"Person",.1f,.1f,.9f,.9f,depth=.5f)
        val face=SceneObject(2,"Gesicht",.3f,.2f,.7f,.5f,Role.ANCHOR,depth=.5f,face=true)
        val field=MotionField.texture(map,listOf(moving,face),Recipe())
        assertEquals(0,field.getPixel(16,11)and 255)
        assertTrue((field.getPixel(16,23)and 255)>50)
        assertEquals(0,field.getPixel(1,1)and 255);field.recycle()
    }
    @Test fun `head translation rotation and distance produce distinct bounded viewpoints`() {
        fun sample(x:Float=.5f,y:Float=.5f,size:Float=.25f,yaw:Float=0f,pitch:Float=0f):FloatArray{
            val pose=HeadPose();repeat(5){pose.update(.5f,.5f,.25f,0f,0f)};assertTrue(pose.calibrated)
            var p=FloatArray(3);repeat(6){p=pose.update(x,y,size,yaw,pitch)};return p
        }
        assertTrue(sample(x=.6f)[0]>.4f);assertTrue(sample(x=.4f)[0]<-.4f)
        assertTrue(sample(y=.4f)[1]>.4f);assertTrue(sample(y=.6f)[1]<-.4f)
        assertTrue(sample(size=.34f)[2]>.5f);assertTrue(sample(size=.18f)[2]<-.5f)
        assertTrue(sample(yaw=20f)[0]<-.15f);assertTrue(sample(pitch=20f)[1]>.15f)
        assertTrue(sample(x=9f,y=-9f,size=5f).all{it.isFinite()&&it in -1f..1f})
        val pose=HeadPose();repeat(5){pose.update(.5f,.5f,.25f,0f,0f)};pose.reset()
        assertFalse(pose.calibrated);assertArrayEquals(FloatArray(3),pose.lost(),0f)
    }
}
