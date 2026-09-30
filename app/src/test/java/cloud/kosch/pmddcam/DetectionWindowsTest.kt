package cloud.kosch.pmddcam

import org.junit.Assert.*
import org.junit.Test

class DetectionWindowsTest {
    @Test fun `detail passes stay bounded and map portrait landscape and square inputs`() {
        for((w,h) in listOf(709 to 1536,1536 to 709,800 to 800,300 to 900)) {
            val windows=DetectionWindows.forImage(w,h)
            assertTrue(windows.size<=7)
            assertEquals(DetectionWindows.Window(0,0,w,h),windows.first())
            for(window in windows) {
                assertTrue(window.left>=0&&window.top>=0&&window.left+window.width<=w&&window.top+window.height<=h)
                val hit=DetectionWindows.map(window,w,h,3,.8f,floatArrayOf(0f,0f,1f,1f))!!
                assertEquals(window.left.toFloat()/w,hit.left,.00001f)
                assertEquals((window.top+window.height).toFloat()/h,hit.bottom,.00001f)
            }
        }
        assertEquals(1,DetectionWindows.forImage(300,900).size)
    }
    @Test fun `crop duplicate fragments collapse while adjacent subjects and different categories survive`() {
        val a=DetectionWindows.Hit(3,.85f,.4f,.4f,.6f,.6f)
        val fragment=a.copy(score=.6f,left=.45f,right=.55f)
        val adjacent=a.copy(score=.7f,left=.58f,right=.78f)
        val person=a.copy(category=1,score=.75f)
        val found=DetectionWindows.distinct(listOf(fragment,person,adjacent,a))
        assertEquals(listOf(a,person,adjacent),found)
        assertEquals(2,DetectionWindows.distinct(found,2).size)
        val window=DetectionWindows.Window(0,0,100,100)
        assertNull(DetectionWindows.map(window,100,100,3,.8f,floatArrayOf(0f,0f,Float.NaN,1f)))
        assertNull(DetectionWindows.map(window,100,100,3,.8f,floatArrayOf(.7f,0f,.2f,1f)))
    }
}
