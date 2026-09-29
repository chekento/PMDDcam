package cloud.kosch.pmddcam

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.*
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class MainActivity:ComponentActivity(),SensorEventListener {
    private val backgroundColor=Color.rgb(11,16,24)
    private val surfaceColor=Color.rgb(23,32,46)
    private val accent=Color.rgb(134,241,209)
    private val muted=Color.rgb(154,172,191)
    private lateinit var root:LinearLayout
    private lateinit var content:FrameLayout
    private lateinit var dock:LinearLayout
    private lateinit var status:TextView
    private lateinit var busy:LinearLayout
    private lateinit var busyLabel:TextView
    private lateinit var store:ProjectStore
    private val prefs by lazy{getSharedPreferences("pmdd",MODE_PRIVATE)}
    private var page="camera"
    private var project:Project?=null
    private var source:Bitmap?=null
    private var depth:DepthMap?=null
    private var result:RenderResult?=null
    private var viewer:DepthViewer?=null
    private var provider:ProcessCameraProvider?=null
    private var capture:ImageCapture?=null
    private var camera:Camera?=null
    private var preview:PreviewView?=null
    private var front=false
    private var flash=ImageCapture.FLASH_MODE_OFF
    private var timer=0
    private var grid=true
    private var task:Job?=null
    private var rendering:Job?=null
    private var tracking:HeadTracker?=null
    private var sensorMode=false
    private var sensorOrigin:FloatArray?=null
    private val undo=ArrayDeque<Pair<Project,DepthMap>>()
    private val redo=ArrayDeque<Pair<Project,DepthMap>>()
    private var exportKind="png"
    private var originalShown=false
    private var captureEpoch=0
    private var analysisNote=""
    private val pickPhoto=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)importPhoto(uri)}
    private val pickProject=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)runTask("Projekt wird wiederhergestellt …"){
        val p=withContext(Dispatchers.IO){store.restore(uri)};openProject(p)
    }}
    private val saveFile=registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->if(uri!=null)exportTo(uri)}
    private var afterPermission:(()->Unit)?=null
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if(granted)afterPermission?.invoke() else{setStatus("Kamerazugriff fehlt. Fotoimport ist weiterhin möglich.");message("Du kannst ein vorhandenes Foto importieren oder die Kamera später in den Android-Einstellungen freigeben.")};afterPermission=null
    }

    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        store=ProjectStore(applicationContext)
        exportKind=state?.getString("exportKind")?:"png"
        root=column().apply{setBackgroundColor(backgroundColor)}
        ViewCompat.setOnApplyWindowInsetsListener(root){v,insets->val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);insets}
        val header=row().apply{setPadding(dp(20),dp(14),dp(14),dp(6));gravity=Gravity.CENTER_VERTICAL}
        header.addView(ImageView(this).apply{setImageResource(cloud.kosch.pmddcam.R.drawable.ic_launcher)},LinearLayout.LayoutParams(dp(42),dp(42)))
        val branding=column().apply{setPadding(dp(12),0,0,0);addView(label("PMDDcam",24f,Color.WHITE,true));addView(label("PHOTOGRAPH A DEEPER WORLD",9f,accent))}
        header.addView(branding,LinearLayout.LayoutParams(0,-2,1f));header.addView(button("?",false){about()},LinearLayout.LayoutParams(dp(48),dp(48)));root.addView(header)
        status=label("PMDD 4.0 · 64 Tiefenebenen · lokal",12f,muted).apply{setPadding(dp(22),dp(2),dp(16),dp(12))};root.addView(status)
        val shell=FrameLayout(this);content=FrameLayout(this);shell.addView(content,FrameLayout.LayoutParams(-1,-1))
        busy=column().apply{gravity=Gravity.CENTER;setPadding(dp(28),dp(24),dp(28),dp(24));setBackgroundColor(0xed0b1018.toInt());isClickable=true;isFocusable=true;visibility=View.GONE}
        busy.addView(ProgressBar(this),LinearLayout.LayoutParams(dp(48),dp(48)))
        busyLabel=label("Bild wird verarbeitet …",17f,Color.WHITE,true).apply{gravity=Gravity.CENTER;setPadding(0,dp(20),0,dp(16))};busy.addView(busyLabel)
        busy.addView(button("Abbrechen · Original behalten"){}.apply{setOnClickListener{task?.cancel();rendering?.cancel();busy.visibility=View.GONE;showLibrary()}})
        shell.addView(busy,FrameLayout.LayoutParams(-1,-1));root.addView(shell,LinearLayout.LayoutParams(-1,0,1f))
        val navigation=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;setPadding(dp(10),dp(8),dp(10),dp(10))};dock=row();navigation.addView(dock);root.addView(navigation)
        setContentView(root)
        onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true){override fun handleOnBackPressed(){
            if(busy.visibility==View.VISIBLE){message("Die Verarbeitung läuft. Mit „Abbrechen“ bleibt das Original erhalten.");return}
            if(page!="camera")showCamera() else{isEnabled=false;onBackPressedDispatcher.onBackPressed()}
        }})
        val last=state?.getString("project")
        if(last!=null)runTask("Projekt wird geöffnet …"){openProject(withContext(Dispatchers.IO){store.load(last)})} else showCamera()
    }
    override fun onSaveInstanceState(out:Bundle){out.putString("project",if(page=="editor")project?.id else null);out.putString("exportKind",exportKind);super.onSaveInstanceState(out)}
    private fun defaults()=runCatching{Recipe.from(JSONObject(prefs.getString("defaults","{}")!!))}.getOrDefault(Recipe())
    private fun runTask(text:String,block:suspend CoroutineScope.()->Unit){
        if(task?.isActive==true)return
        task=lifecycleScope.launch {
            busyLabel.text=text;busy.visibility=View.VISIBLE
            try{block()}catch(e:CancellationException){throw e}catch(e:OutOfMemoryError){message("Zu wenig Arbeitsspeicher. Das Original bleibt erhalten. Bitte eine kleinere Ausgabe wählen.")}
            catch(e:Exception){message(e.message?:"Dieser Schritt konnte nicht abgeschlossen werden. Das Original bleibt erhalten.");if(page=="editor"&&viewer==null)showLibrary()}
            finally{busy.visibility=View.GONE}
        }
    }
    private fun setStatus(text:String){runOnUiThread{status.text=text}}
    private fun requireCamera(action:()->Unit){if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)action()else{afterPermission=action;permission.launch(Manifest.permission.CAMERA)}}
    private fun clearPage(){
        captureEpoch++;stopTracking();provider?.unbindAll();capture=null;camera=null
        rendering?.cancel();viewer?.release();viewer?.onPause();viewer=null;preview=null
        content.removeAllViews();dock.removeAllViews()
    }
    private fun nav(text:String,action:()->Unit){dock.addView(button(text,false,action),LinearLayout.LayoutParams(-2,dp(48)).apply{marginEnd=dp(7)})}

    private fun showCamera(){
        clearPage();page="camera";setStatus("Aufnehmen. Tiefe entdecken. Später neu gestalten.")
        val layout=column().apply{setPadding(dp(14),0,dp(14),0)}
        val quick=row().apply{gravity=Gravity.CENTER}
        quick.addView(button("Blitz: ${flashName()}"){flash=(flash+1)%3;capture?.flashMode=flash;(itLabel(quick,0)).text="Blitz: ${flashName()}"},LinearLayout.LayoutParams(0,dp(48),1f))
        quick.addView(button("Timer: ${timer}s"){timer=when(timer){0->3;3->10;else->0};itLabel(quick,1).text="Timer: ${timer}s"},LinearLayout.LayoutParams(0,dp(48),1f))
        quick.addView(button("Raster ${if(grid)"an" else "aus"}"){grid=!grid;showCamera()},LinearLayout.LayoutParams(0,dp(48),1f));layout.addView(quick)
        val stage=FrameLayout(this).apply{background=shape(surfaceColor,24f);clipToOutline=true}
        val cameraPreview=PreviewView(this).apply{scaleType=PreviewView.ScaleType.FIT_CENTER;implementationMode=PreviewView.ImplementationMode.COMPATIBLE;contentDescription="Kameravorschau"};preview=cameraPreview
        stage.addView(cameraPreview,FrameLayout.LayoutParams(-1,-1))
        if(grid)stage.addView(object:View(this){private val p=Paint().apply{color=0x45ffffff;strokeWidth=1f};override fun onDraw(c:Canvas){for(i in 1..2){c.drawLine(width*i/3f,0f,width*i/3f,height.toFloat(),p);c.drawLine(0f,height*i/3f,width.toFloat(),height*i/3f,p)}}},FrameLayout.LayoutParams(-1,-1))
        val badge=label("L5  •  ${defaults().layers} LAYER  •  ORIGINAL SICHER",10f,accent,true).apply{setPadding(dp(12),dp(8),dp(12),dp(8));background=shape(0xc9111e2c.toInt(),16f)}
        stage.addView(badge,FrameLayout.LayoutParams(-2,-2,Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply{topMargin=dp(16)})
        layout.addView(stage,LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(8);bottomMargin=dp(8)})
        val zoomLabel=label("Zoom · 1.0×",12f,muted);layout.addView(zoomLabel)
        layout.addView(SeekBar(this).apply{max=100;contentDescription="Kamerazoom";setOnSeekBarChangeListener(seek{value->val state=camera?.cameraInfo?.zoomState?.value;val maxZoom=minOf(8f,state?.maxZoomRatio?:1f);val minZoom=state?.minZoomRatio?:1f;val ratio=minZoom+(maxZoom-minZoom)*value;camera?.cameraControl?.setZoomRatio(ratio);zoomLabel.text="Zoom · %.1f×".format(ratio)})})
        val shutterRow=row().apply{gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(6),0,dp(10))}
        shutterRow.addView(button("Import"){pickPhoto.launch(arrayOf("image/*"))},LinearLayout.LayoutParams(0,dp(64),1f))
        shutterRow.addView(button("●",true){takePhoto()},LinearLayout.LayoutParams(dp(84),dp(76)).apply{marginStart=dp(14);marginEnd=dp(14)})
        shutterRow.addView(button("Wechsel"){front=!front;bindCamera(cameraPreview)},LinearLayout.LayoutParams(0,dp(64),1f));layout.addView(shutterRow)
        content.addView(layout,FrameLayout.LayoutParams(-1,-1))
        nav("Sammlung"){showLibrary()};nav("Vorgaben"){settingsDialog(defaults(),true)};nav("Projekt öffnen"){pickProject.launch(arrayOf("application/zip","application/octet-stream"))}
        requireCamera{bindCamera(cameraPreview)}
    }
    private fun itLabel(row:LinearLayout,i:Int)=row.getChildAt(i) as Button
    private fun flashName()=when(flash){ImageCapture.FLASH_MODE_AUTO->"Auto";ImageCapture.FLASH_MODE_ON->"An";else->"Aus"}
    private fun bindCamera(view:PreviewView){
        if(page!="camera")return
        val epoch=captureEpoch
        val future=ProcessCameraProvider.getInstance(this)
        future.addListener({
            if(page!="camera" || epoch!=captureEpoch || preview!==view)return@addListener
            try{
                val p=future.get();provider=p;p.unbindAll()
                val selector=if(front)CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                require(p.hasCamera(selector)){"Diese Kamera ist nicht verfügbar. Mit „Wechsel“ die andere Kamera wählen."}
                val live=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)}
                val cap=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).setFlashMode(flash).build();capture=cap
                camera=p.bindToLifecycle(this,selector,live,cap)
                view.setOnTouchListener{_,event->if(event.action==MotionEvent.ACTION_UP){val point=view.meteringPointFactory.createPoint(event.x,event.y);camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).build());view.performClick()};true}
            }catch(e:Exception){setStatus(e.message?:"Kamera konnte nicht gestartet werden.")}
        },ContextCompat.getMainExecutor(this))
    }
    private fun takePhoto(){
        val cap=capture?:run{requireCamera{preview?.let{bindCamera(it)}};return}
        if(task?.isActive==true)return
        runTask("Originalaufnahme wird gespeichert …"){
            for(n in timer downTo 1){busyLabel.text="Aufnahme in $n …";delay(1000)}
            val p=withContext(Dispatchers.IO){store.create(defaults())}
            cap.targetRotation=preview?.display?.rotation?:Surface.ROTATION_0
            val output=ImageCapture.OutputFileOptions.Builder(store.original(p.id)).build()
            suspendCancellableCoroutine<Unit>{continuation->
                cap.takePicture(output,ContextCompat.getMainExecutor(this@MainActivity),object:ImageCapture.OnImageSavedCallback{
                    override fun onImageSaved(out:ImageCapture.OutputFileResults){if(continuation.isActive)continuation.resume(Unit){}}
                    override fun onError(e:ImageCaptureException){if(continuation.isActive)continuation.resumeWith(Result.failure(e))}
                })
            }
            openProject(p)
        }
    }
    private fun importPhoto(uri:Uri)=runTask("Originalfoto wird kopiert …") {openProject(withContext(Dispatchers.IO){store.import(uri,defaults())})}
    private suspend fun openProject(p:Project){
        rendering?.cancelAndJoin()
        clearPage();page="editor";project=p;prefs.edit().putString("last",p.id).apply();undo.clear();redo.clear()
        source?.recycle();source=withContext(Dispatchers.IO){store.decode(p.id,1100)}
        if(!p.ready || !File(store.dir(p.id),"depth.bin").exists()){
            val analysis=SceneAnalyzer(applicationContext).analyze(requireNotNull(source),p.recipe.detectObjects){s->runOnUiThread{busyLabel.text=s}}
            depth=analysis.depth;p.objects=analysis.objects;p.note=analysis.note;p.ready=true
            withContext(Dispatchers.IO){store.saveDepth(p.id,analysis.depth,true);store.saveDepth(p.id,analysis.depth);store.save(p)}
        }else depth=withContext(Dispatchers.IO){store.depth(p.id)}
        analysisNote=p.note
        buildEditor();renderNow()
    }
    private fun buildEditor(){
        val p=project?:return
        content.removeAllViews();dock.removeAllViews();originalShown=false
        val layout=column().apply{setPadding(dp(14),0,dp(14),0)}
        val info=row().apply{gravity=Gravity.CENTER_VERTICAL}
        info.addView(label("DEIN TIEFENSTUDIO",11f,accent,true),LinearLayout.LayoutParams(0,-2,1f))
        info.addView(button("↶"){undoEdit()});info.addView(button("↷"){redoEdit()});layout.addView(info)
        val stage=FrameLayout(this).apply{background=shape(surfaceColor,20f);clipToOutline=true}
        viewer=DepthViewer(this);stage.addView(viewer,FrameLayout.LayoutParams(-1,-1))
        layout.addView(stage,LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(8);bottomMargin=dp(12)})
        val compare=row()
        compare.addView(button("Original / PMDD"){originalShown=!originalShown;showCurrentImage()},LinearLayout.LayoutParams(0,dp(48),1f))
        compare.addView(button("Betrachtermodus"){viewerDialog()},LinearLayout.LayoutParams(0,dp(48),1f));layout.addView(compare)
        layout.addView(label("${Styles.get(p.recipe.style).name}  ·  Original dauerhaft im Projekt",12f,muted).apply{setPadding(dp(4),dp(10),0,dp(8))})
        content.addView(layout,FrameLayout.LayoutParams(-1,-1))
        nav("Kamera"){showCamera()};nav("Stile · 60"){stylesDialog()};nav("PMDD"){settingsDialog(p.recipe.copy())};nav("Objekte"){objectsDialog()};nav("Tiefe malen"){paintDepth()};nav("Export"){exportDialog()};nav("Sammlung"){showLibrary()}
        setStatus("${p.recipe.layers} Ebenen · Tiefe ${(p.recipe.depth*100).roundToInt()} % · ${p.objects.count{it.id!=999}} Bereiche")
    }
    private fun snapshot(){val p=project?:return;val d=depth?:return;undo.addLast(p.snapshot() to d.copy());if(undo.size>20)undo.removeFirst();redo.clear()}
    private fun undoEdit(){if(undo.isEmpty())return;redo.addLast(project!!.snapshot() to depth!!.copy());restoreEdit(undo.removeLast())}
    private fun redoEdit(){if(redo.isEmpty())return;undo.addLast(project!!.snapshot() to depth!!.copy());restoreEdit(redo.removeLast())}
    private fun restoreEdit(state:Pair<Project,DepthMap>){project=state.first;depth=state.second;persist();scheduleRender()}
    private fun persist(){project?.let{store.save(it);depth?.let{d->store.saveDepth(it.id,d)}}}
    private fun changed(){persist();originalShown=false;scheduleRender()}
    private fun scheduleRender(){
        rendering?.cancel();rendering=lifecycleScope.launch {delay(120);try{renderNow()}catch(e:CancellationException){throw e}catch(e:Exception){message(e.message?:"Vorschau fehlgeschlagen.")}}
    }
    private suspend fun renderNow(){
        val p=project?.snapshot()?:return;val src=source?:return;val d=depth?.copy()?:return
        setStatus("PMDD-Vorschau wird berechnet …")
        val next=withContext(Dispatchers.Default){PmddRenderer.render(src,d,p)}
        currentCoroutineContext().ensureActive()
        result?.image?.recycle();result=next
        showCurrentImage()
        withContext(Dispatchers.IO){store.thumbnail(p.id,next.image)}
        setStatus(if(analysisNote.isNotBlank())analysisNote else "${p.recipe.layers} Ebenen · Tiefe ${(p.recipe.depth*100).roundToInt()} % · ${Styles.get(p.recipe.style).name}")
    }
    private fun showCurrentImage(){
        val r=result?:return;val p=project?:return
        viewer?.setImage(if(originalShown)source?:r.image else r.image,r.depth,p.recipe)
        if(originalShown){viewer?.active=false;stopTracking();setStatus("Unverändertes Original · erneut tippen für PMDD")}
    }
    private fun showLibrary(){
        clearPage();page="library";setStatus("Originale, Tiefenkarten und Einstellungen bleiben bearbeitbar.")
        val list=column().apply{setPadding(dp(16),0,dp(16),dp(20))};content.addView(scroll(list));nav("Kamera"){showCamera()};nav("Foto importieren"){pickPhoto.launch(arrayOf("image/*"))};nav("Projekt öffnen"){pickProject.launch(arrayOf("application/zip","application/octet-stream"))}
        lifecycleScope.launch {
            val projects=withContext(Dispatchers.IO){store.list()}
            if(page!="library")return@launch
            if(projects.isEmpty()){list.addView(label("Dein erster Blick in die Tiefe",25f,Color.WHITE,true));list.addView(label("Nimm ein Foto auf oder importiere eine Aufnahme. PMDDcam erstellt automatisch eine Tiefenansicht und bewahrt das Original auf.",16f,muted).apply{setPadding(0,dp(20),0,dp(24))});list.addView(button("Foto aufnehmen",true){showCamera()})}
            for(p in projects){
                val card=row().apply{gravity=Gravity.CENTER_VERTICAL;background=shape(surfaceColor,18f);setPadding(dp(12),dp(12),dp(12),dp(12))}
                val thumb=ImageView(this@MainActivity).apply{scaleType=ImageView.ScaleType.CENTER_CROP}
                card.addView(thumb,LinearLayout.LayoutParams(dp(84),dp(84)))
                val text=column().apply{setPadding(dp(14),0,0,0);addView(label(Styles.get(p.recipe.style).name,17f,Color.WHITE,true));addView(label(SimpleDateFormat("dd.MM.yyyy · HH:mm",Locale.GERMAN).format(Date(p.created)),12f,muted));addView(label(if(p.ready)"${p.recipe.layers} Layer · weiter bearbeiten" else "Original gesichert · Verarbeitung fortsetzen",12f,accent))}
                card.addView(text,LinearLayout.LayoutParams(0,-2,1f));list.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
                card.setOnClickListener{runTask("Projekt wird geöffnet …"){openProject(p)}}
                val image=withContext(Dispatchers.IO){val f=File(store.dir(p.id),"thumb.jpg");if(f.exists())ProjectStore.decodeFile(f,240)else runCatching{store.decode(p.id,240)}.getOrNull()};thumb.setImageBitmap(image)
            }
        }
    }

    private fun settingsDialog(initial:Recipe,defaultsOnly:Boolean=false){
        val choice=arrayOf("Tiefe & Ebenen","Bewegungsillusion","Bild & Stilintensität","Betrachtung & Parallaxe","Erkennung & Ausgabe","Intensiv-Preset","Natürlich-Preset","Als Standard für neue Fotos")
        AlertDialog.Builder(this).setTitle(if(defaultsOnly)"Aufnahme-Vorgaben" else "PMDD-Einstellungen").setItems(choice){_,which->
            val r=initial.copy()
            when(which){
                5->{r.layers=96;r.depth=1.1f;r.separation=.9f;r.motionAmount=.5f;applyRecipe(r,defaultsOnly)}
                6->{r.layers=48;r.depth=.55f;r.separation=.45f;r.motionAmount=.18f;r.haze=.14f;applyRecipe(r,defaultsOnly)}
                7->{prefs.edit().putString("defaults",r.json().toString()).apply();message("Vorgaben für neue Aufnahmen gespeichert.")}
                else->settingsSection(r,which,defaultsOnly)
            }
        }.setNegativeButton("Schließen",null).show()
    }
    private fun settingsSection(r:Recipe,group:Int,defaultsOnly:Boolean){
        val body=column().apply{setPadding(dp(16),0,dp(16),dp(12))}
        when(group){
            0->{slider(body,"Tiefenebenen",r.layers.toFloat(),8f,128f){r.layers=it.roundToInt()};slider(body,"3D-Tiefe",r.depth,0f,1.5f){r.depth=it};slider(body,"Ebenentrennung",r.separation){r.separation=it};slider(body,"Fokusebene · fern → nah",r.focus){r.focus=it};slider(body,"Plastisches Licht",r.relief){r.relief=it};slider(body,"Atmosphärische Ferne",r.haze){r.haze=it};slider(body,"Tiefenunschärfe",r.bokeh){r.bokeh=it};slider(body,"Kontaktschatten",r.occlusion){r.occlusion=it};check(body,"Tiefenrichtung umkehren",r.invertDepth){r.invertDepth=it}}
            1->{body.addView(label("Statische Mikro-Kontraste erzeugen Wahrnehmungshinweise. Die Stärke der Illusion hängt auch vom Motiv, Display und Blick ab.",13f,muted));check(body,"Bewegungsillusion aktiv",r.motion){r.motion=it};slider(body,"Illusionsstärke",r.motionAmount){r.motionAmount=it};slider(body,"Texturfrequenz",r.motionScale){r.motionScale=it};slider(body,"Peripherie betonen",r.peripheral){r.peripheral=it};slider(body,"Bewegung an Tiefe koppeln",r.depthCoupling){r.depthCoupling=it};check(body,"Stabile Anker schützen",r.lockAnchors){r.lockAnchors=it};check(body,"Erkannte Gesichter schützen",r.protectFaces){r.protectFaces=it}}
            2->{slider(body,"Stilmischung",r.styleMix){r.styleMix=it};slider(body,"Lokale Schärfe",r.sharpness){r.sharpness=it};slider(body,"Mikrotextur",r.texture){r.texture=it};slider(body,"Belichtung",r.exposure,-1f,1f){r.exposure=it};slider(body,"Kontrast",r.contrast,-.5f,.8f){r.contrast=it};slider(body,"Sättigung",r.saturation,0f,2f){r.saturation=it};slider(body,"Vignette",r.vignette){r.vignette=it}}
            3->{check(body,"Links / rechts",r.horizontal){r.horizontal=it};check(body,"Oben / unten",r.vertical){r.vertical=it};check(body,"Näher / weiter",r.distance){r.distance=it};slider(body,"Interaktive Parallaxe",r.parallax){r.parallax=it};slider(body,"Betrachtungsabstand · cm",r.viewDistance,20f,150f){r.viewDistance=it};slider(body,"Bildbreite · cm",r.screenSize,8f,100f){r.screenSize=it};body.addView(label("Für die reale Bewegungserkennung den Betrachtermodus mit Kopfsteuerung einschalten. Ein PNG bleibt statisch.",13f,muted))}
            4->{check(body,"Objekte und Gesichter automatisch erkennen",r.detectObjects){r.detectObjects=it};body.addView(label("Wirkt bei neuen Aufnahmen bzw. „Neu analysieren“. Vorhandene Objektrollen bleiben beim Bearbeiten erhalten.",12f,muted));slider(body,"Export · längste Kante",r.outputSize.toFloat(),1024f,4096f){r.outputSize=it.roundToInt()};body.addView(label("Originale behalten ihre volle Auflösung. Die Ausgabe wird zum Schutz vor Speicherabbruch an den Gerätespeicher angepasst.",13f,muted))}
        }
        val dialog=AlertDialog.Builder(this).setTitle(arrayOf("Tiefe & Ebenen","Bewegungsillusion","Bildgestaltung","Betrachtung","Erkennung & Ausgabe")[group]).setView(scroll(body)).setPositiveButton("Übernehmen"){_,_->applyRecipe(r,defaultsOnly)}.setNegativeButton("Abbrechen",null).create();showDialog(dialog)
    }
    private fun applyRecipe(r:Recipe,defaultsOnly:Boolean){
        if(defaultsOnly){prefs.edit().putString("defaults",r.normalized().json().toString()).apply();message("Aufnahme-Vorgaben gespeichert.")}
        else{snapshot();project?.recipe=r.normalized();changed()}
    }
    private fun stylesDialog(){
        val p=project?:return;val d=depth?:return;val src=source?:return
        val layout=column().apply{setPadding(dp(14),0,dp(14),0)}
        val tabs=Spinner(this);val groups=Styles.all.map{it.group}.distinct();tabs.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,groups);layout.addView(tabs)
        val gridView=GridLayout(this).apply{columnCount=2};layout.addView(scroll(gridView),LinearLayout.LayoutParams(-1,0,1f))
        val dialog=AlertDialog.Builder(this).setTitle("60 Stile · alle mit PMDD").setView(layout).setNegativeButton("Schließen",null).create()
        var previews:Job?=null
        fun populate(category:String){
            previews?.cancel();gridView.removeAllViews()
            val options=Styles.all.filter{it.group==category}
            val images=mutableListOf<ImageView>()
            for(style in options){
                val card=column().apply{setPadding(dp(6),dp(6),dp(6),dp(6));background=shape(surfaceColor,14f)}
                val iv=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP;contentDescription=style.name};images+=iv;card.addView(iv,LinearLayout.LayoutParams(-1,dp(110)))
                card.addView(label(style.name,13f,if(style.id==p.recipe.style)accent else Color.WHITE,true).apply{setPadding(dp(4),dp(8),dp(4),dp(6))})
                gridView.addView(card,GridLayout.LayoutParams().apply{width=0;height=-2;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(dp(3),dp(3),dp(3),dp(3))})
                card.setOnClickListener{snapshot();p.recipe.style=style.id;dialog.dismiss();changed()}
            }
            previews=lifecycleScope.launch {
                val small=Bitmap.createScaledBitmap(src,160,(160f*src.height/src.width).roundToInt().coerceAtLeast(1),true)
                try{for((i,style) in options.withIndex()){
                    val recipe=p.snapshot().apply{this.recipe.style=style.id}
                    val thumb=withContext(Dispatchers.Default){PmddRenderer.render(small,d,recipe)}
                    ensureActive();images[i].setImageBitmap(thumb.image)
                }}finally{if(small!==src)small.recycle()}
            }
        }
        tabs.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(parent:AdapterView<*>?){};override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){populate(groups[position])}}
        dialog.setOnDismissListener{previews?.cancel()};showDialog(dialog,.86f)
    }

    private fun objectsDialog(){
        val p=project?:return;val body=column().apply{setPadding(dp(16),0,dp(16),dp(16))}
        body.addView(label("Jedes Objekt erhält eine eigene Bewegungsidentität. Automatisch erkannte feste Objekte starten als ruhige Anker.",13f,muted))
        val dialog=AlertDialog.Builder(this).setTitle("Objekte & Bewegungsfelder").setView(scroll(body)).setNegativeButton("Schließen",null).create()
        p.objects.forEach{o->body.addView(button("${o.name}\n${o.role.title} · ${o.motion.title}"){dialog.dismiss();objectEditor(o.id)})}
        body.addView(button("+ Objektbereich markieren",true){dialog.dismiss();addRegion()})
        body.addView(button("Objekte und Tiefe neu analysieren"){dialog.dismiss();AlertDialog.Builder(this).setMessage("Automatische Tiefenkarte und Objektrollen neu ermitteln? Manuelle Tiefen- und Objektänderungen werden ersetzt. Das Original bleibt erhalten.").setNegativeButton("Abbrechen",null).setPositiveButton("Neu analysieren"){_,_->runTask("Szene wird neu analysiert …"){
            snapshot();val a=SceneAnalyzer(applicationContext).analyze(source!!,p.recipe.detectObjects){setStatus(it)};depth=a.depth;p.objects=a.objects;p.note=a.note;analysisNote=a.note
            withContext(Dispatchers.IO){store.saveDepth(p.id,a.depth,true)};persist();renderNow()
        }}.show()})
        showDialog(dialog)
    }
    private fun objectEditor(id:Int){
        val p=project?:return;val current=p.objects.firstOrNull{it.id==id}?:return;val o=current.copy()
        val body=column().apply{setPadding(dp(16),0,dp(16),dp(12))}
        val name=EditText(this).apply{setText(o.name);setTextColor(Color.WHITE);hint="Objektname";isSingleLine=true};body.addView(name)
        check(body,"Bereich aktiv",o.enabled){o.enabled=it}
        choose(body,"Rolle",Role.entries.map{it.title},o.role.ordinal){o.role=Role.entries[it]}
        choose(body,"Hauptbewegung",Motion.entries.map{it.title},o.motion.ordinal){o.motion=Motion.entries[it]}
        slider(body,"Richtung · Grad",o.angle,0f,360f){o.angle=it};slider(body,"Bewegungshinweis · Tempo",o.speed){o.speed=it};slider(body,"Lokale Intensität",o.intensity){o.intensity=it}
        check(body,"Eigene Tiefenposition",o.overrideDepth){o.overrideDepth=it};slider(body,"Tiefe · fern → nah",o.depth){o.depth=it}
        body.addView(label("Tempo steuert statische Texturdichte und Richtungshinweise. Das exportierte Foto besitzt keine animierten Frames.",12f,muted))
        val dialog=AlertDialog.Builder(this).setTitle("Objekt gestalten").setView(scroll(body)).setPositiveButton("Übernehmen"){_,_->snapshot();o.name=name.text.toString().take(80).ifBlank{current.name};p.objects[p.objects.indexOf(current)]=o;changed()}.setNegativeButton("Abbrechen",null).setNeutralButton("Entfernen"){_,_->snapshot();p.objects.remove(current);changed()}.create();showDialog(dialog)
    }
    private fun paintDepth(){
        val src=source?:return;val d=depth?.copy()?:return
        val body=column().apply{setPadding(dp(12),0,dp(12),0)}
        val editor=MaskEditor(this,src,d);body.addView(editor,LinearLayout.LayoutParams(-1,0,1f))
        body.addView(label("Hell = nah · Dunkel = fern. Mit dem Finger malen.",12f,muted))
        slider(body,"Zieltiefe",editor.target){editor.target=it};slider(body,"Pinselradius",editor.radius,.015f,.2f){editor.radius=it};body.addView(button("Pinselstrich zurück"){editor.undo()})
        val dialog=AlertDialog.Builder(this).setTitle("Tiefenkarte korrigieren").setView(body).setPositiveButton("Übernehmen"){_,_->snapshot();depth=d;changed()}.setNegativeButton("Abbrechen",null).setNeutralButton("Modelltiefe"){_,_->snapshot();depth=store.depth(project!!.id,true);changed()}.create();showDialog(dialog,.9f)
    }
    private fun addRegion(){
        val p=project?:return;val src=source?:return;val d=depth?:return
        val body=column().apply{setPadding(dp(12),0,dp(12),0)}
        body.addView(label("Ziehe einen Rahmen um das gewünschte Objekt. Die Tiefenkarte begrenzt das weiche Bewegungsfeld innerhalb des Rahmens.",13f,muted))
        val editor=MaskEditor(this,src,d.copy(),true);body.addView(editor,LinearLayout.LayoutParams(-1,0,1f))
        val dialog=AlertDialog.Builder(this).setTitle("Objektbereich markieren").setView(body).setPositiveButton("Hinzufügen"){_,_->val b=editor.region
            if(b.width()<.025f || b.height()<.025f){message("Bitte einen größeren Objektbereich markieren.");return@setPositiveButton}
            snapshot();val id=(p.objects.maxOfOrNull{it.id}?:1000)+1;p.objects+=SceneObject(id,"Eigener Bereich",b.left,b.top,b.right,b.bottom,depth=d.sample(b.centerX(),b.centerY()));changed();objectEditor(id)
        }.setNegativeButton("Abbrechen",null).create();showDialog(dialog,.8f)
    }

    private fun viewerDialog(){
        originalShown=false;showCurrentImage()
        AlertDialog.Builder(this).setTitle("Betrachtermodus").setItems(arrayOf("Mit Finger steuern · ziehen / aufziehen","Kopfbewegung · Frontkamera","Gerät neigen · Sensor","Zentrieren / neu kalibrieren","Statisches PMDD-Bild")){_,which->
            when(which){
                0->{stopTracking();viewer?.active=true;viewer?.requestRender();setStatus("Ziehen: links/rechts & oben/unten · Aufziehen: näher/weiter")}
                1->requireCamera{stopTracking();viewer?.active=true;tracking=HeadTracker(this,this,{x,y,z->viewer?.setPosition(x,y,z)},{setStatus(it)}).also{it.start()}}
                2->{stopTracking();val manager=getSystemService(SENSOR_SERVICE)as SensorManager;val sensor=manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)?:manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                    if(sensor==null)message("Kein geeigneter Sensor vorhanden. Bitte Finger- oder Kopfsteuerung wählen.")else{sensorMode=true;sensorOrigin=null;viewer?.active=true;manager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_GAME);setStatus("Neigungssteuerung · näher/weiter per Aufziehen")}}
                3->{viewer?.center();tracking?.calibrate();sensorOrigin=null}
                4->{stopTracking();viewer?.active=false;viewer?.center();setStatus("Statisches PMDD-Foto · für PNG/JPEG-Export")}
            }
        }.setNegativeButton("Schließen",null).show()
    }
    private fun stopTracking(){tracking?.stop();tracking=null;(getSystemService(SENSOR_SERVICE)as SensorManager).unregisterListener(this);sensorMode=false;sensorOrigin=null}
    override fun onSensorChanged(event:SensorEvent){
        if(!sensorMode)return
        val matrix=FloatArray(9);SensorManager.getRotationMatrixFromVector(matrix,event.values)
        val remapped=FloatArray(9)
        @Suppress("DEPRECATION") val rotation=windowManager.defaultDisplay.rotation
        val x=when(rotation){Surface.ROTATION_90->SensorManager.AXIS_Y;Surface.ROTATION_180->SensorManager.AXIS_MINUS_X;Surface.ROTATION_270->SensorManager.AXIS_MINUS_Y;else->SensorManager.AXIS_X}
        val y=when(rotation){Surface.ROTATION_90->SensorManager.AXIS_MINUS_X;Surface.ROTATION_180->SensorManager.AXIS_MINUS_Y;Surface.ROTATION_270->SensorManager.AXIS_X;else->SensorManager.AXIS_Y}
        SensorManager.remapCoordinateSystem(matrix,x,y,remapped)
        val angles=FloatArray(3);SensorManager.getOrientation(remapped,angles)
        if(sensorOrigin==null)sensorOrigin=angles.clone()
        fun wrapped(value:Float)=atan2(sin(value),cos(value))
        val origin=sensorOrigin!!;viewer?.setPosition(wrapped(angles[2]-origin[2])*1.8f,wrapped(angles[1]-origin[1])*1.8f,viewer?.viewZ?:0f)
    }
    override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}

    private fun exportDialog(){
        AlertDialog.Builder(this).setTitle("Exportieren & sichern").setItems(arrayOf("PMDD-Foto · PNG","PMDD-Foto · JPEG","Unverändertes Original","Bearbeitbares PMDD-Projekt · ZIP","PMDD-Bild teilen","Tiefenkarte · PNG")){_,which->
            exportKind=arrayOf("png","jpg","original","project","share","depth")[which]
            if(exportKind=="share"){shareImage();return@setItems}
            val ext=when(exportKind){"project"->"pmdd.zip";"original"->originalExtension();"depth"->"depth.png";else->exportKind}
            saveFile.launch("PMDDcam-${project!!.id.take(8)}.$ext")
        }.setNegativeButton("Schließen",null).show()
    }
    private fun originalExtension():String {val options=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(store.original(project!!.id).path,options);return when(options.outMimeType){"image/png"->"png";"image/webp"->"webp";"image/heif","image/heic"->"heic";else->"jpg"}}
    private suspend fun exportBitmap(p:Project):Bitmap = withContext(Dispatchers.Default){
        val maxEdge=if(Runtime.getRuntime().maxMemory()<384L*1024*1024)min(p.recipe.outputSize,2048)else p.recipe.outputSize
        val full=store.decode(p.id,maxEdge)
        try{PmddRenderer.render(full,store.depth(p.id),p).image}finally{full.recycle()}
    }
    private fun exportTo(uri:Uri){val p=project?.snapshot()?:return;val kind=exportKind
        runTask("Ausgabe wird erstellt …"){
            withContext(Dispatchers.IO){
                requireNotNull(contentResolver.openOutputStream(uri)).use{out->when(kind){
                    "original"->store.original(p.id).inputStream().use{it.copyTo(out)}
                    "project"->store.archive(p,out)
                    "depth"->{val bitmap=PmddRenderer.depthBitmap(PmddRenderer.effectiveDepth(store.depth(p.id),p));try{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,out))}finally{bitmap.recycle()}}
                    else->{val bitmap=exportBitmap(p);try{check(bitmap.compress(if(kind=="jpg")Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG,96,out))}finally{bitmap.recycle()}}
                }}
            };message("Gespeichert.")
        }
    }
    private fun shareImage(){val p=project?.snapshot()?:return
        runTask("Bild zum Teilen wird erstellt …"){
            val file=withContext(Dispatchers.IO){val dir=File(cacheDir,"exports").apply{mkdirs()};dir.listFiles()?.filter{System.currentTimeMillis()-it.lastModified()>24*3600_000L}?.forEach{it.delete()};val f=File(dir,"PMDDcam-${System.currentTimeMillis()}.jpg");val bitmap=exportBitmap(p);try{f.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.JPEG,96,it))}}finally{bitmap.recycle()};f}
            val uri=FileProvider.getUriForFile(this@MainActivity,"$packageName.files",file)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="image/jpeg";putExtra(Intent.EXTRA_STREAM,uri);clipData=ClipData.newRawUri("PMDD-Foto",uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"PMDD-Foto teilen"))
        }
    }
    private fun about(){
        AlertDialog.Builder(this).setTitle("PMDDcam · 0.1.0").setMessage("Fotografiere einen tieferen Raum.\n\nPMDD 4.0 — Perceptual Motion & Depth Design\nKonzept: Kolja Werner Schumann · kosch.cloud\nHuman-AI-Co-Design mit ChatGPT.\n\nDie Fotoverarbeitung und Erkennung laufen auf deinem Gerät. Originale werden getrennt von Effekten gespeichert. Zum Sichern außerhalb der App ein PMDD-Projekt exportieren.\n\nStatische PMDD-Illusionen und interaktive 2.5D-Parallaxe sind getrennte Modi. Tiefen werden aus einem Foto geschätzt; verdeckte Rückseiten kann das Foto nicht zeigen. Die 60 Stile sind lokale Bildverfahren.\n\nMiDaS v2.1 (MIT), ONNX Runtime (MIT), AndroidX (Apache 2.0), Google ML Kit.\n\n${assets.open("THIRD_PARTY.txt").bufferedReader().use{it.readText()}}")
            .setPositiveButton("Schließen",null).setNeutralButton("PMDD-Geschichte"){_,_->startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://kosch.cloud/blog/pmdd---die-magie-hinter-der-illusion--wie-wahrnehmung-und-ki-zu-lebendigen-bildern-verschmelzen")))}.setNegativeButton("GitHub"){_,_->startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/chekento/PMDDcam")))}.show()
    }

    private fun column()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
    private fun row()=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
    private fun label(text:String,size:Float=14f,color:Int=Color.WHITE,bold:Boolean=false)=TextView(this).apply{this.text=text;textSize=size;setTextColor(color);if(bold)setTypeface(typeface,Typeface.BOLD);setLineSpacing(dp(3).toFloat(),1f)}
    private fun button(text:String,primary:Boolean=false,action:()->Unit)=Button(this).apply{
        this.text=text;isAllCaps=false;textSize=13f;minHeight=dp(48);minimumHeight=dp(48);setPadding(dp(14),dp(8),dp(14),dp(8));setTextColor(if(primary)backgroundColor else Color.WHITE)
        background=shape(if(primary)accent else surfaceColor,16f);stateListAnimator=null;setOnClickListener{if(busy.visibility!=View.VISIBLE)action()}
    }
    private fun shape(color:Int,radius:Float)=GradientDrawable().apply{setColor(color);cornerRadius=dp(radius).toFloat()}
    private fun dp(value:Int)=(value*resources.displayMetrics.density).roundToInt()
    private fun dp(value:Float)=(value*resources.displayMetrics.density).roundToInt()
    private fun scroll(child:View)=ScrollView(this).apply{isFillViewport=true;addView(child)}
    private fun slider(body:LinearLayout,name:String,value:Float,min:Float=0f,max:Float=1f,onChange:(Float)->Unit){
        val title=label("$name · ${format(value,max)}",13f,muted).apply{setPadding(0,dp(15),0,0)};body.addView(title)
        body.addView(SeekBar(this).apply{this.max=1000;progress=((value-min)/(max-min)*1000).roundToInt().coerceIn(0,1000);progressTintList=ColorStateList.valueOf(accent);thumbTintList=ColorStateList.valueOf(accent);contentDescription=name;minimumHeight=dp(48)
            setOnSeekBarChangeListener(seek{fraction->val current=min+(max-min)*fraction;title.text="$name · ${format(current,max)}";onChange(current)})})
    }
    private fun format(value:Float,max:Float)=if(max>2)"${value.roundToInt()}" else "${(value*100).roundToInt()} %"
    private fun seek(callback:(Float)->Unit)=object:SeekBar.OnSeekBarChangeListener{override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){};override fun onProgressChanged(s:SeekBar?,progress:Int,user:Boolean){if(user)callback(progress.toFloat()/(s?.max?:100))}}
    private fun check(body:LinearLayout,text:String,value:Boolean,change:(Boolean)->Unit){body.addView(CheckBox(this).apply{this.text=text;isChecked=value;setTextColor(Color.WHITE);minHeight=dp(48);buttonTintList=ColorStateList.valueOf(accent);setOnCheckedChangeListener{_,checked->change(checked)}})}
    private fun choose(body:LinearLayout,title:String,values:List<String>,selected:Int,change:(Int)->Unit){body.addView(label(title,13f,muted).apply{setPadding(0,dp(12),0,0)});body.addView(Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,values);setSelection(selected);minimumHeight=dp(48);onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,position:Int,id:Long){change(position)}}})}
    private fun showDialog(dialog:AlertDialog,height:Float=.78f){dialog.show();dialog.window?.setLayout((resources.displayMetrics.widthPixels*.96f).toInt(),(resources.displayMetrics.heightPixels*height).toInt());dialog.window?.setBackgroundDrawable(shape(backgroundColor,22f))}
    private fun message(text:String){if(!isFinishing)AlertDialog.Builder(this).setMessage(text).setPositiveButton("OK",null).show()}
    override fun onPause(){stopTracking();viewer?.onPause();super.onPause()}
    override fun onResume(){super.onResume();viewer?.onResume()}
    override fun onDestroy(){rendering?.cancel();task?.cancel();stopTracking();viewer?.release();provider?.unbindAll();super.onDestroy()}
}
