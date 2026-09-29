package cloud.kosch.pmddcam

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.AtomicFile
import androidx.exifinterface.media.ExifInterface
import org.json.JSONObject
import java.io.*
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class ProjectStore(private val context:Context) {
    val root=File(context.filesDir,"projects").apply{mkdirs()}
    fun dir(id:String):File {
        require(id.matches(Regex("[a-zA-Z0-9-]{1,64}")))
        return File(root,id).apply{mkdirs()}
    }
    fun original(id:String)=File(dir(id),"original.image")
    fun create(recipe:Recipe):Project = Project(UUID.randomUUID().toString(),System.currentTimeMillis(),recipe.copy(),mutableListOf()).also{save(it)}
    fun save(p:Project)=atomic(File(dir(p.id),"project.json")){it.write(p.json().toString(2).toByteArray())}
    fun load(id:String)=Project.from(JSONObject(AtomicFile(File(dir(id),"project.json")).openRead().bufferedReader().use{it.readText()}))
    fun list():List<Project> = root.listFiles().orEmpty().filter{it.isDirectory}.mapNotNull{
        runCatching {load(it.name)}.getOrNull()?.takeIf{p->original(p.id).length()>0}
    }.sortedByDescending{it.created}

    fun import(uri:Uri,recipe:Recipe):Project {
        val p=create(recipe)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input){"Das Bild konnte nicht geöffnet werden."}
            atomic(original(p.id)){copyBounded(input,it,100L*1024*1024)}
        }
        requireImage(original(p.id))
        return p
    }
    fun requireImage(file:File) {
        val info=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        BitmapFactory.decodeFile(file.path,info)
        require(info.outWidth>0 && info.outHeight>0){"Dieses Bildformat wird nicht unterstützt."}
        require(info.outWidth.toLong()*info.outHeight<=300_000_000L){"Die Aufnahme ist zu groß (max. 300 MP)."}
    }
    fun decode(id:String,longEdge:Int):Bitmap=decodeFile(original(id),longEdge)
    fun saveDepth(id:String,depth:DepthMap,base:Boolean=false) {
        atomic(File(dir(id),if(base) "depth-base.bin" else "depth.bin")){s->
            val out=DataOutputStream(s);out.writeInt(0x504D4431);out.writeInt(depth.width);out.writeInt(depth.height)
            depth.values.forEach{out.writeFloat(it.safe(.5f))};out.flush()
        }
    }
    fun depth(id:String,base:Boolean=false):DepthMap = DataInputStream(File(dir(id),if(base) "depth-base.bin" else "depth.bin").inputStream().buffered()).use {
        require(it.readInt()==0x504D4431){"Ungültige Tiefenkarte"}
        val w=it.readInt();val h=it.readInt();require(w in 1..1024 && h in 1..1024)
        DepthMap(w,h,FloatArray(w*h){_->it.readFloat().safe(.5f)})
    }
    fun thumbnail(id:String,bitmap:Bitmap) {
        val scale=360f/max(bitmap.width,bitmap.height)
        val thumb=Bitmap.createScaledBitmap(bitmap,(bitmap.width*scale).roundToInt().coerceAtLeast(1),(bitmap.height*scale).roundToInt().coerceAtLeast(1),true)
        atomic(File(dir(id),"thumb.jpg")){check(thumb.compress(Bitmap.CompressFormat.JPEG,85,it))}
        if(thumb!==bitmap)thumb.recycle()
    }
    fun archive(p:Project,target:OutputStream) {
        save(p)
        ZipOutputStream(target.buffered()).use { zip ->
            listOf("project.json","original.image","depth.bin","depth-base.bin","thumb.jpg").forEach{name->
                val file=File(dir(p.id),name)
                if(file.exists()){zip.putNextEntry(ZipEntry(name));file.inputStream().use{it.copyTo(zip)};zip.closeEntry()}
            }
        }
    }
    fun restore(uri:Uri):Project {
        val temporary=File(root,"restore-${UUID.randomUUID()}").apply{mkdirs()}
        try {
            var size=0L;val seen=mutableSetOf<String>()
            context.contentResolver.openInputStream(uri).use { src ->
                ZipInputStream(requireNotNull(src).buffered()).use{zip->
                    while(true){
                        val e=zip.nextEntry?:break
                        require(e.name in setOf("project.json","original.image","depth.bin","depth-base.bin","thumb.jpg") && seen.add(e.name)){"Ungültiges PMDD-Projekt"}
                        File(temporary,e.name).outputStream().use{size+=copyBounded(zip,it,150L*1024*1024-size)}
                        zip.closeEntry()
                    }
                }
            }
            val metadata=File(temporary,"project.json")
            require(metadata.length() in 1..1_000_000)
            val old=Project.from(JSONObject(metadata.readText()))
            requireImage(File(temporary,"original.image"))
            val id=UUID.randomUUID().toString()
            val p=old.copy(id=id,created=System.currentTimeMillis())
            require(temporary.renameTo(File(root,id))){"Projekt konnte nicht wiederhergestellt werden."}
            if(p.ready)runCatching{depth(id);depth(id,true)}.onFailure{p.ready=false;p.note="Tiefenkarte wird neu analysiert."}
            save(p);return p
        } finally {temporary.deleteRecursively()}
    }
    companion object {
        fun atomic(file:File,write:(FileOutputStream)->Unit) {
            val a=AtomicFile(file);val stream=a.startWrite()
            try {write(stream);a.finishWrite(stream)} catch(e:Throwable){a.failWrite(stream);throw e}
        }
        private fun copyBounded(src:InputStream,out:OutputStream,limit:Long):Long {
            var total=0L;val buffer=ByteArray(32768)
            while(true){val n=src.read(buffer);if(n<0)break;total+=n;require(total<=limit){"Datei ist zu groß."};out.write(buffer,0,n)}
            return total
        }
        fun decodeFile(file:File,longEdge:Int):Bitmap {
            val info=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(file.path,info)
            require(info.outWidth>0 && info.outHeight>0){"Die Aufnahme konnte nicht gelesen werden."}
            var sample=1
            while(max(info.outWidth,info.outHeight)/sample>longEdge*2)sample*=2
            val raw=requireNotNull(BitmapFactory.decodeFile(file.path,BitmapFactory.Options().apply{inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888}))
            val orientation=runCatching{ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION,1)}.getOrDefault(1)
            val m=Matrix().apply{when(orientation){
                2->setScale(-1f,1f);3->setRotate(180f);4->setScale(1f,-1f)
                5->{setRotate(90f);postScale(-1f,1f)};6->setRotate(90f)
                7->{setRotate(-90f);postScale(-1f,1f)};8->setRotate(-90f)
            }}
            val rotated=Bitmap.createBitmap(raw,0,0,raw.width,raw.height,m,true)
            if(rotated!==raw)raw.recycle()
            val scale=minOf(1f,longEdge.toFloat()/max(rotated.width,rotated.height))
            val result=Bitmap.createScaledBitmap(rotated,(rotated.width*scale).roundToInt().coerceAtLeast(1),(rotated.height*scale).roundToInt().coerceAtLeast(1),true)
            if(result!==rotated)rotated.recycle()
            return result
        }
    }
}
