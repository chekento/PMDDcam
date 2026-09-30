package cloud.kosch.pmddcam

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

/** All edits are a recipe. Neither this nor the renderer ever writes to the original. */
data class Recipe(
    var layers: Int = 96,
    var depth: Float = 1.25f,
    var separation: Float = .8f,
    var focus: Float = .6f,
    var relief: Float = .35f,
    var haze: Float = .06f,
    var bokeh: Float = .12f,
    var sharpness: Float = .25f,
    var occlusion: Float = .12f,
    var texture: Float = .1f,
    var vignette: Float = .06f,
    var exposure: Float = 0f,
    var contrast: Float = .05f,
    var saturation: Float = 1.03f,
    var style: String = "vivid",
    var styleMix: Float = 1f,
    var motion: Boolean = true,
    var motionAmount: Float = .7f,
    var motionScale: Float = .6f,
    var peripheral: Float = .7f,
    var depthCoupling: Float = .8f,
    var protectFaces: Boolean = true,
    var lockAnchors: Boolean = true,
    var detectObjects: Boolean = true,
    var parallax: Float = 1f,
    var horizontal: Boolean = true,
    var vertical: Boolean = true,
    var distance: Boolean = true,
    var viewDistance: Float = 45f,
    var screenSize: Float = 16f,
    var invertDepth: Boolean = false,
    var outputSize: Int = 2560
) {
    fun normalized(): Recipe = copy(
        layers=layers.coerceIn(8,128), depth=depth.safe(1.25f,0f,2.5f),
        separation=separation.safe(.8f), focus=focus.safe(.6f), relief=relief.safe(.35f),
        haze=haze.safe(.06f), bokeh=bokeh.safe(.12f), sharpness=sharpness.safe(.25f),
        occlusion=occlusion.safe(.12f), texture=texture.safe(.1f), vignette=vignette.safe(.06f),
        exposure=exposure.safe(0f,-1f,1f), contrast=contrast.safe(.05f,-.5f,.8f),
        saturation=saturation.safe(1.03f,0f,2f), style=Styles.get(style).id,
        styleMix=styleMix.safe(1f),motionAmount=motionAmount.safe(.7f,0f,2f),
        motionScale=motionScale.safe(.6f),peripheral=peripheral.safe(.7f),
        depthCoupling=depthCoupling.safe(.8f),parallax=parallax.safe(1f,0f,2f),
        viewDistance=viewDistance.safe(45f,20f,150f),screenSize=screenSize.safe(16f,8f,100f),
        outputSize=outputSize.coerceIn(1024,4096)
    )
    fun json(): JSONObject = JSONObject().apply {
        put("version",1)
        Recipe::class.java.declaredFields.filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }.forEach {
            it.isAccessible=true; put(it.name,it.get(this@Recipe))
        }
    }
    companion object {
        fun from(j: JSONObject): Recipe {
            val r=Recipe()
            Recipe::class.java.declaredFields.filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }.forEach {
                it.isAccessible=true
                if(j.has(it.name)) runCatching {
                    when(it.type) {
                        java.lang.Float.TYPE -> it.setFloat(r,j.getDouble(it.name).toFloat())
                        java.lang.Integer.TYPE -> it.setInt(r,j.getInt(it.name))
                        java.lang.Boolean.TYPE -> it.setBoolean(r,j.getBoolean(it.name))
                        String::class.java -> it.set(r,j.getString(it.name))
                    }
                }
            }
            return r.normalized()
        }
    }
}

fun Float.safe(fallback:Float, min:Float=0f,max:Float=1f) = if(isFinite()) coerceIn(min,max) else fallback
enum class Role(val title:String) { ANCHOR("Anker · stabil"), DYNAMIC("Dynamisch"), ATMOSPHERE("Atmosphäre") }
enum class Motion(val title:String) { APPROACH("Annäherung"), RETREAT("Entfernung"), DRIFT("Seitliche Drift"), ROTATE("Rotation"), FLOW("Fließen"), PULSE("Pulsieren") }

data class SceneObject(
    val id: Int,
    var name: String,
    var left:Float, var top:Float, var right:Float, var bottom:Float,
    var role:Role = Role.DYNAMIC,
    var motion:Motion = Motion.APPROACH,
    var angle:Float = 25f,
    var speed:Float = .5f,
    var intensity:Float = .5f,
    var depth:Float = .65f,
    var overrideDepth:Boolean = false,
    val face:Boolean = false,
    var enabled:Boolean = true
) {
    fun json()=JSONObject().apply {
        put("id",id);put("name",name);put("bounds",JSONArray(listOf(left,top,right,bottom)))
        put("role",role.name);put("motion",motion.name);put("angle",angle);put("speed",speed)
        put("intensity",intensity);put("depth",depth);put("overrideDepth",overrideDepth);put("face",face);put("enabled",enabled)
    }
    companion object {
        fun from(j:JSONObject): SceneObject {
            val b=j.getJSONArray("bounds")
            return SceneObject(j.getInt("id"),j.getString("name"),b.getDouble(0).toFloat().safe(0f),b.getDouble(1).toFloat().safe(0f),
                b.getDouble(2).toFloat().safe(1f),b.getDouble(3).toFloat().safe(1f),
                runCatching { Role.valueOf(j.getString("role")) }.getOrDefault(Role.ANCHOR),
                runCatching { Motion.valueOf(j.getString("motion")) }.getOrDefault(Motion.DRIFT),
                j.optDouble("angle",25.0).toFloat().safe(25f,0f,360f),j.optDouble("speed",.5).toFloat().safe(.5f),
                j.optDouble("intensity",.5).toFloat().safe(.5f),j.optDouble("depth",.65).toFloat().safe(.65f),
                j.optBoolean("overrideDepth"),j.optBoolean("face"),j.optBoolean("enabled",true))
        }
    }
}

class DepthMap(val width:Int,val height:Int,val values:FloatArray) {
    init { require(width>0 && height>0 && values.size==width*height) }
    fun sample(u:Float,v:Float):Float {
        val x=u.coerceIn(0f,1f)*(width-1);val y=v.coerceIn(0f,1f)*(height-1)
        val ix=x.toInt();val iy=y.toInt();val ax=x-ix;val ay=y-iy
        val jx=min(ix+1,width-1);val jy=min(iy+1,height-1)
        return (values[iy*width+ix]*(1-ax)+values[iy*width+jx]*ax)*(1-ay)+
            (values[jy*width+ix]*(1-ax)+values[jy*width+jx]*ax)*ay
    }
    fun copy()=DepthMap(width,height,values.clone())
    companion object {
        fun normalize(raw:FloatArray,w:Int,h:Int):DepthMap {
            val sorted=raw.filter { it.isFinite() }.sorted()
            require(sorted.isNotEmpty()) { "Das Tiefenmodell lieferte keine gültigen Werte." }
            val lo=sorted[(sorted.size*.02).toInt()];val hi=sorted[(sorted.size*.98).toInt().coerceAtMost(sorted.lastIndex)]
            val span=hi-lo
            return DepthMap(w,h,FloatArray(raw.size) { if(span<1e-6f) .5f else ((raw[it]-lo)/span).safe(.5f) })
        }
    }
}

data class Project(val id:String,val created:Long,var recipe:Recipe,var objects:MutableList<SceneObject>, var note:String="",var ready:Boolean=false) {
    fun json()=JSONObject().apply {
        put("schema",1);put("id",id);put("created",created);put("recipe",recipe.json());put("ready",ready);put("note",note)
        put("objects",JSONArray(objects.map{it.json()}))
    }
    fun snapshot()=copy(recipe=recipe.copy(),objects=objects.map{it.copy()}.toMutableList())
    companion object {
        fun from(j:JSONObject):Project {
            require(j.optInt("schema")==1) { "Unbekanntes Projektformat" }
            val id=j.getString("id");require(id.matches(Regex("[a-zA-Z0-9-]{1,64}")))
            val a=j.getJSONArray("objects")
            require(a.length()<=100)
            return Project(id,j.getLong("created"),Recipe.from(j.getJSONObject("recipe")),MutableList(a.length()){SceneObject.from(a.getJSONObject(it))},j.optString("note"),j.optBoolean("ready"))
        }
    }
}
