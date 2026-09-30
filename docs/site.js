(() => {
  const supported = ["de","en","fr","es","zh","ja"];
  const normalize = (value) => {
    const v=(value||"").toLowerCase().replace("_","-");
    if(v.startsWith("de")) return "de";
    if(v.startsWith("fr")) return "fr";
    if(v.startsWith("es")) return "es";
    if(v.startsWith("zh")) return "zh";
    if(v.startsWith("ja")) return "ja";
    if(v.startsWith("en")) return "en";
    return null;
  };
  const t = {
    de:{
      brandTag:"PMDD Kamera für Android",home:"Übersicht",features:"Funktionen",technology:"Technik",download:"APK herunterladen",language:"Sprache",
      heroTitle:"Fotografiere einen tieferen Raum.",heroSub:"Verwandle Fotos lokal auf Android in räumlich geschichtete PMDD-Bilder – mit Originalarchiv, 61 Stilen und interaktiver Tiefe.",
      downloadNow:"PMDDcam 0.3.0 herunterladen",explore:"Funktionen ansehen",compareTitle:"Original ↔ PMDD",compareText:"Dasselbe Foto. Das Original bleibt unverändert; PMDD-Tiefe, Stil und Wirkung bleiben separat bearbeitbar.",original:"Original",enhanced:"PMDD",
      coreTitle:"Was PMDDcam ausmacht",c1t:"Nicht-destruktiv",c1d:"Originalbytes, Tiefenkarte und Rezept bleiben getrennt. Umschalten ist jederzeit möglich.",c2t:"Lokal auf dem Gerät",c2d:"Tiefenschätzung, Objekterkennung und Rendering laufen ohne Foto-Upload oder API-Schlüssel.",c3t:"Tiefe + Bewegung",c3d:"PMDD-Staffelung plus interaktiver 2.5D-Viewer für Touch, Neigung und optionale Kopfsteuerung.",
      galleryTitle:"PMDDcam in Bildern",privacyTitle:"Privat by design",privacyText:"Keine App-Internetberechtigung für die Fotoverarbeitung, kein Cloudkonto, keine Foto-Uploads.",
      fTitle:"Funktionen",fIntro:"Von der Aufnahme bis zum exportierbaren PMDD-Projekt: die wichtigsten Module der Preview.",fCamT:"Vollbild-Kamera",fCamD:"CameraX, Front/Rückkamera, Tap-Fokus, Pinch-Zoom, Blitz, Raster und Timer; optionaler Auslöseton (Standard aus, Gerätevorgabe hat Vorrang).",fDepthT:"PMDD-Tiefe",fDepthD:"8–128 Viewer-Ebenen, Standard 96, Tiefenstärke bis 250 %, Fokus, Atmosphäre und Bokeh.",fStyleT:"61 Stilrezepte",fStyleD:"Neu: PMDD Vivid als kräftiger Standardlook. Foto, Illustration, Atelier, Retro, Zukunft und Atmosphäre – lokal und nachträglich mischbar.",fObjectT:"Objektlogik",fObjectD:"Anker, dynamische Bereiche und Atmosphäre mit eigener Intensität, Bewegung und Tiefenposition.",fViewerT:"Interaktiver Viewer",fViewerD:"Touch, Geräteneigung, Pinch oder optionale Kopfsteuerung für links/rechts, oben/unten und näher/weiter.",fExportT:"Export & Archiv",fExportD:"PNG, JPEG, Original, Tiefenkarte und bearbeitbares ZIP-Projekt; das Original wird nie überschrieben.",
      techTitle:"Technologie",techIntro:"Lokale Modelle, Rendering-Pipeline und technische Grenzen der Android-Preview.",techDepthT:"Relative Tiefe",techDepthD:"MiDaS v2.1 Small schätzt monokular die relative Tiefe. Sie ist keine metrisch vermessene 3D-Geometrie.",techObjT:"Objekterkennung",techObjD:"SSD-MobileNet ergänzt bis zu 20 Bereiche aus 80 COCO-Klassen; ML Kit hilft bei Gesichtern und Personenmasken.",techRunT:"On-device Runtime",techRunD:"ONNX Runtime führt die gebündelten Modelle lokal aus. Kein API-Schlüssel ist für die Fotoanalyse nötig.",techBuildT:"Android & Build",techBuildD:"Android 8.0+, JDK 17, Android SDK 35 und Build Tools 35.0.0. CI prüft Unit-Tests, Lint und Emulator-Workflows.",techLimitT:"Grenzen",techLimitD:"Große virtuelle Blickwinkel können Kanten dehnen; die PMDD-Wirkung hängt von Motiv, Display und Betrachtung ab.",
      foot:"PMDD — Perceptual Motion & Depth Design · Kolja Werner Schumann (KoSch) · Human-AI-Co-Design mit ChatGPT"
    },
    en:{
      brandTag:"PMDD camera for Android",home:"Overview",features:"Features",technology:"Technology",download:"Download APK",language:"Language",
      heroTitle:"Photograph a deeper space.",heroSub:"Turn photos into layered PMDD imagery locally on Android — with preserved originals, 61 styles and interactive depth.",
      downloadNow:"Download PMDDcam 0.3.0",explore:"Explore features",compareTitle:"Original ↔ PMDD",compareText:"The same photo. The original stays untouched while PMDD depth, style and appearance remain independently editable.",original:"Original",enhanced:"PMDD",
      coreTitle:"What makes PMDDcam different",c1t:"Non-destructive",c1d:"Original bytes, depth map and recipe stay separate. Switch between them at any time.",c2t:"On-device",c2d:"Depth estimation, object detection and rendering run without photo uploads or an API key.",c3t:"Depth + motion",c3d:"PMDD depth design plus an interactive 2.5D viewer for touch, tilt and optional head tracking.",
      galleryTitle:"PMDDcam in pictures",privacyTitle:"Private by design",privacyText:"No app internet permission for photo processing, no cloud account and no photo uploads.",
      fTitle:"Features",fIntro:"From capture to an exportable PMDD project: the core modules in the preview.",fCamT:"Fullscreen camera",fCamD:"CameraX, front/rear camera, tap focus, pinch zoom, flash, grid and timer; optional shutter sound (off by default, device policy takes priority).",fDepthT:"PMDD depth",fDepthD:"8–128 viewer samples, 96 by default, depth strength up to 250%, focus, atmosphere and bokeh.",fStyleT:"61 style recipes",fStyleD:"New: PMDD Vivid as the rich default look. Photo, illustration, studio, retro, future and atmosphere — local and blendable later.",fObjectT:"Object logic",fObjectD:"Anchors, dynamic regions and atmosphere with per-region intensity, motion and depth position.",fViewerT:"Interactive viewer",fViewerD:"Touch, device tilt, pinch or optional head control for left/right, up/down and near/far.",fExportT:"Export & archive",fExportD:"PNG, JPEG, original, depth map and editable ZIP project; the source image is never overwritten.",
      techTitle:"Technology",techIntro:"Local models, rendering pipeline and technical limits of the Android preview.",techDepthT:"Relative depth",techDepthD:"MiDaS v2.1 Small estimates monocular relative depth. It is not metrically measured 3D geometry.",techObjT:"Object detection",techObjD:"SSD-MobileNet adds up to 20 regions from 80 COCO classes; ML Kit assists with faces and person masks.",techRunT:"On-device runtime",techRunD:"ONNX Runtime executes bundled models locally. No API key is required for photo analysis.",techBuildT:"Android & build",techBuildD:"Android 8.0+, JDK 17, Android SDK 35 and Build Tools 35.0.0. CI checks unit tests, lint and emulator workflows.",techLimitT:"Limits",techLimitD:"Large virtual viewpoints can stretch edges; perceived PMDD impact depends on subject, display and viewing conditions.",
      foot:"PMDD — Perceptual Motion & Depth Design · Kolja Werner Schumann (KoSch) · Human-AI co-design with ChatGPT"
    },
    fr:{
      brandTag:"Caméra PMDD pour Android",home:"Aperçu",features:"Fonctions",technology:"Technologie",download:"Télécharger l’APK",language:"Langue",
      heroTitle:"Photographiez un espace plus profond.",heroSub:"Transformez localement vos photos Android en images PMDD stratifiées, avec original conservé, 61 styles et profondeur interactive.",
      downloadNow:"Télécharger PMDDcam 0.3.0",explore:"Voir les fonctions",compareTitle:"Original ↔ PMDD",compareText:"La même photo. L’original reste intact, tandis que la profondeur, le style et le rendu PMDD restent modifiables séparément.",original:"Original",enhanced:"PMDD",
      coreTitle:"Ce qui distingue PMDDcam",c1t:"Non destructif",c1d:"Octets d’origine, carte de profondeur et recette restent séparés. Le basculement reste toujours possible.",c2t:"Sur l’appareil",c2d:"Estimation de profondeur, détection d’objets et rendu sans téléversement de photo ni clé API.",c3t:"Profondeur + mouvement",c3d:"Conception PMDD et visualiseur 2.5D interactif pour toucher, inclinaison et suivi optionnel de la tête.",
      galleryTitle:"PMDDcam en images",privacyTitle:"Confidentialité intégrée",privacyText:"Aucune autorisation Internet pour le traitement photo, aucun compte cloud et aucun téléversement.",
      fTitle:"Fonctions",fIntro:"De la prise de vue au projet PMDD exportable : les principaux modules de la preview.",fCamT:"Caméra plein écran",fCamD:"CameraX, caméras avant/arrière, mise au point tactile, zoom pincé, flash, grille et minuterie ; son optionnel (désactivé par défaut, priorité aux règles de l’appareil).",fDepthT:"Profondeur PMDD",fDepthD:"8–128 niveaux d’échantillonnage, 96 par défaut, force jusqu’à 250 %, mise au point, atmosphère et bokeh.",fStyleT:"61 recettes de style",fStyleD:"Nouveau : PMDD Vivid, le style par défaut aux couleurs vives. Photo, illustration, atelier, rétro, futur et atmosphère — localement et mixables après coup.",fObjectT:"Logique d’objets",fObjectD:"Ancres, zones dynamiques et atmosphère avec intensité, mouvement et profondeur par zone.",fViewerT:"Visualiseur interactif",fViewerD:"Toucher, inclinaison, pincement ou contrôle optionnel par la tête pour gauche/droite, haut/bas et proche/loin.",fExportT:"Export & archive",fExportD:"PNG, JPEG, original, carte de profondeur et projet ZIP modifiable ; la source n’est jamais écrasée.",
      techTitle:"Technologie",techIntro:"Modèles locaux, pipeline de rendu et limites techniques de la preview Android.",techDepthT:"Profondeur relative",techDepthD:"MiDaS v2.1 Small estime une profondeur monoculaire relative ; ce n’est pas une géométrie 3D mesurée.",techObjT:"Détection d’objets",techObjD:"SSD-MobileNet ajoute jusqu’à 20 zones parmi 80 classes COCO ; ML Kit aide pour les visages et masques de personnes.",techRunT:"Exécution locale",techRunD:"ONNX Runtime exécute les modèles intégrés localement. Aucune clé API n’est requise pour l’analyse.",techBuildT:"Android & build",techBuildD:"Android 8.0+, JDK 17, Android SDK 35 et Build Tools 35.0.0. La CI vérifie tests unitaires, lint et émulateur.",techLimitT:"Limites",techLimitD:"Les grands angles virtuels peuvent étirer les bords ; l’effet PMDD dépend du sujet, de l’écran et des conditions de visionnage.",
      foot:"PMDD — Perceptual Motion & Depth Design · Kolja Werner Schumann (KoSch) · Co-design humain-IA avec ChatGPT"
    },
    es:{
      brandTag:"Cámara PMDD para Android",home:"Resumen",features:"Funciones",technology:"Tecnología",download:"Descargar APK",language:"Idioma",
      heroTitle:"Fotografía un espacio más profundo.",heroSub:"Convierte fotos localmente en Android en imágenes PMDD por capas, conservando el original, con 61 estilos y profundidad interactiva.",
      downloadNow:"Descargar PMDDcam 0.3.0",explore:"Ver funciones",compareTitle:"Original ↔ PMDD",compareText:"La misma foto. El original permanece intacto y la profundidad, el estilo y el aspecto PMDD siguen siendo editables por separado.",original:"Original",enhanced:"PMDD",
      coreTitle:"Qué hace diferente a PMDDcam",c1t:"No destructivo",c1d:"Bytes originales, mapa de profundidad y receta se mantienen separados. Puedes alternar cuando quieras.",c2t:"En el dispositivo",c2d:"Estimación de profundidad, detección de objetos y render sin subir fotos ni usar una clave API.",c3t:"Profundidad + movimiento",c3d:"Diseño de profundidad PMDD más un visor 2.5D interactivo para toque, inclinación y control opcional de cabeza.",
      galleryTitle:"PMDDcam en imágenes",privacyTitle:"Privacidad desde el diseño",privacyText:"Sin permiso de Internet para procesar fotos, sin cuenta en la nube y sin subir imágenes.",
      fTitle:"Funciones",fIntro:"Desde la captura hasta un proyecto PMDD exportable: los módulos principales de la preview.",fCamT:"Cámara a pantalla completa",fCamD:"CameraX, cámara frontal/trasera, enfoque táctil, zoom por pellizco, flash, cuadrícula y temporizador; sonido opcional (desactivado por defecto, prevalece la política del dispositivo).",fDepthT:"Profundidad PMDD",fDepthD:"8–128 muestras del visor, 96 por defecto, fuerza hasta 250 %, enfoque, atmósfera y bokeh.",fStyleT:"61 recetas de estilo",fStyleD:"Nuevo: PMDD Vivid como estilo predeterminado de colores vivos. Foto, ilustración, estudio, retro, futuro y atmósfera — locales y mezclables posteriormente.",fObjectT:"Lógica de objetos",fObjectD:"Anclas, zonas dinámicas y atmósfera con intensidad, movimiento y posición de profundidad por zona.",fViewerT:"Visor interactivo",fViewerD:"Toque, inclinación, pellizco o control opcional de cabeza para izquierda/derecha, arriba/abajo y cerca/lejos.",fExportT:"Exportación y archivo",fExportD:"PNG, JPEG, original, mapa de profundidad y proyecto ZIP editable; la imagen fuente nunca se sobrescribe.",
      techTitle:"Tecnología",techIntro:"Modelos locales, pipeline de render y límites técnicos de la preview Android.",techDepthT:"Profundidad relativa",techDepthD:"MiDaS v2.1 Small estima profundidad monocular relativa; no es geometría 3D medida métricamente.",techObjT:"Detección de objetos",techObjD:"SSD-MobileNet añade hasta 20 zonas de 80 clases COCO; ML Kit ayuda con rostros y máscaras de personas.",techRunT:"Runtime local",techRunD:"ONNX Runtime ejecuta los modelos incluidos localmente. No hace falta clave API para analizar fotos.",techBuildT:"Android y build",techBuildD:"Android 8.0+, JDK 17, Android SDK 35 y Build Tools 35.0.0. CI comprueba tests, lint y flujos de emulador.",techLimitT:"Límites",techLimitD:"Los puntos de vista virtuales amplios pueden estirar bordes; el efecto PMDD depende del motivo, pantalla y condiciones de observación.",
      foot:"PMDD — Perceptual Motion & Depth Design · Kolja Werner Schumann (KoSch) · Co-diseño humano-IA con ChatGPT"
    },
    zh:{
      brandTag:"Android PMDD 相机",home:"概览",features:"功能",technology:"技术",download:"下载 APK",language:"语言",
      heroTitle:"拍摄更有深度的空间。",heroSub:"在 Android 本地把照片转换为分层 PMDD 图像，同时保留原图，并提供 61 种风格与交互式深度。",
      downloadNow:"下载 PMDDcam 0.3.0",explore:"查看功能",compareTitle:"原图 ↔ PMDD",compareText:"同一张照片。原图保持不变；PMDD 深度、风格和视觉效果可独立继续编辑。",original:"原图",enhanced:"PMDD",
      coreTitle:"PMDDcam 的核心特点",c1t:"非破坏式",c1d:"原始字节、深度图和配方彼此分离，随时可以切换比较。",c2t:"设备本地处理",c2d:"深度估计、物体检测和渲染无需上传照片，也无需 API 密钥。",c3t:"深度 + 运动",c3d:"PMDD 深度设计结合交互式 2.5D 查看器，支持触控、设备倾斜及可选头部控制。",
      galleryTitle:"PMDDcam 图集",privacyTitle:"隐私优先设计",privacyText:"照片处理无需应用联网权限、无需云账户，也不会上传照片。",
      fTitle:"功能",fIntro:"从拍摄到可导出的 PMDD 项目：Preview 的核心模块。",fCamT:"全屏相机",fCamD:"CameraX、前后摄、点击对焦、双指缩放、闪光灯、网格和定时器；快门音默认关闭，设备强制要求优先。",fDepthT:"PMDD 深度",fDepthD:"查看器 8–128 层采样，默认 96；深度强度最高 250%，并提供焦点、氛围和散景。",fStyleT:"61 种风格配方",fStyleD:"新增默认风格 PMDD Vivid，色彩更鲜明。照片、插画、画室、复古、未来与氛围六大类，全部本地处理并可后期混合。",fObjectT:"物体逻辑",fObjectD:"锚点、动态区域与氛围层可分别设置强度、运动和深度位置。",fViewerT:"交互式查看器",fViewerD:"支持触控、设备倾斜、双指操作或可选头部控制，实现左右、上下和远近变化。",fExportT:"导出与归档",fExportD:"PNG、JPEG、原图、深度图和可编辑 ZIP 项目；源图永不被覆盖。",
      techTitle:"技术",techIntro:"Android Preview 的本地模型、渲染管线与技术边界。",techDepthT:"相对深度",techDepthD:"MiDaS v2.1 Small 从单张图像估计相对深度，并非经过度量测量的 3D 几何。",techObjT:"物体检测",techObjD:"SSD-MobileNet 可从 80 个 COCO 类别中补充最多 20 个区域；ML Kit 辅助人脸与人物蒙版。",techRunT:"设备本地运行",techRunD:"ONNX Runtime 在本地执行随 APK 提供的模型，照片分析不需要 API 密钥。",techBuildT:"Android 与构建",techBuildD:"Android 8.0+、JDK 17、Android SDK 35、Build Tools 35.0.0；CI 检查单元测试、Lint 和模拟器流程。",techLimitT:"限制",techLimitD:"较大的虚拟视角可能拉伸边缘；PMDD 的感知效果取决于主体、显示设备与观看条件。",
      foot:"PMDD — Perceptual Motion & Depth Design · Kolja Werner Schumann (KoSch) · 与 ChatGPT 的人机协同设计"
    },
    ja:{
      brandTag:"Android 向け PMDD カメラ",home:"概要",features:"機能",technology:"技術",download:"APKをダウンロード",language:"言語",
      heroTitle:"より深い空間を撮影。",heroSub:"Android 上でローカルに、写真をレイヤー化された PMDD イメージへ。原画を保持し、61 スタイルとインタラクティブな奥行きを提供します。",
      downloadNow:"PMDDcam 0.3.0 をダウンロード",explore:"機能を見る",compareTitle:"Original ↔ PMDD",compareText:"同じ写真を比較。オリジナルは変更せず、PMDD の奥行き・スタイル・見え方は個別に編集できます。",original:"オリジナル",enhanced:"PMDD",
      coreTitle:"PMDDcam の特徴",c1t:"非破壊編集",c1d:"元のバイト列、深度マップ、レシピを分離して保持し、いつでも切り替えられます。",c2t:"オンデバイス",c2d:"深度推定・物体検出・レンダリングは写真アップロードや API キーなしで端末上で動作します。",c3t:"奥行き + モーション",c3d:"PMDD の奥行き設計と、タッチ・傾き・任意の頭部操作に対応する 2.5D ビューアー。",
      galleryTitle:"PMDDcam ギャラリー",privacyTitle:"プライバシーを前提に設計",privacyText:"写真処理にアプリのインターネット権限、クラウドアカウント、写真アップロードは不要です。",
      fTitle:"機能",fIntro:"撮影からエクスポート可能な PMDD プロジェクトまで、Preview の主要モジュール。",fCamT:"フルスクリーンカメラ",fCamD:"CameraX、前後カメラ、タップフォーカス、ピンチズーム、フラッシュ、グリッド、タイマー。シャッター音は標準でオフ、端末の必須設定を優先。",fDepthT:"PMDD 奥行き",fDepthD:"ビューアーのサンプル数 8–128、標準 96。深度強度最大 250%、フォーカス、空気感、ボケ。",fStyleT:"61 スタイルレシピ",fStyleD:"鮮やかな PMDD Vivid が新しい標準スタイル。写真、イラスト、アトリエ、レトロ、未来、雰囲気の6カテゴリ。ローカル処理で後からブレンド可能。",fObjectT:"オブジェクトロジック",fObjectD:"アンカー、動的領域、雰囲気領域ごとに強度・動き・深度位置を設定できます。",fViewerT:"インタラクティブビューアー",fViewerD:"タッチ、端末の傾き、ピンチ、任意の頭部操作で左右・上下・近い/遠いを制御。",fExportT:"エクスポート & アーカイブ",fExportD:"PNG、JPEG、オリジナル、深度マップ、編集可能 ZIP プロジェクト。元画像は上書きしません。",
      techTitle:"技術",techIntro:"Android Preview のローカルモデル、レンダリングパイプライン、技術的な限界。",techDepthT:"相対深度",techDepthD:"MiDaS v2.1 Small が単眼画像から相対深度を推定します。実測された 3D ジオメトリではありません。",techObjT:"物体検出",techObjD:"SSD-MobileNet は 80 COCO クラスから最大 20 領域を補助し、ML Kit は顔と人物マスクを支援します。",techRunT:"オンデバイス Runtime",techRunD:"ONNX Runtime が同梱モデルをローカル実行します。写真解析に API キーは不要です。",techBuildT:"Android & Build",techBuildD:"Android 8.0+、JDK 17、Android SDK 35、Build Tools 35.0.0。CI で単体テスト、Lint、エミュレータを検証します。",techLimitT:"制限",techLimitD:"大きな仮想視点ではエッジが伸びる場合があります。PMDD の知覚効果は被写体、画面、閲覧条件に依存します。",
      foot:"PMDD — Perceptual Motion & Depth Design · Kolja Werner Schumann (KoSch) · ChatGPT との Human-AI Co-Design"
    }
  };

  const qs=new URLSearchParams(location.search);
  const explicit=normalize(qs.get("lang"));
  let saved=null;
  try{saved=normalize(localStorage.getItem("pmddcam-lang"));}catch(e){}
  const detected=(()=>{
    const langs=(navigator.languages&&navigator.languages.length?navigator.languages:[navigator.language]).map(normalize).filter(Boolean);
    return langs[0]||"en";
  })();
  let lang=explicit||saved||detected;

  function apply(next){
    lang=normalize(next)||"en";
    const dict=t[lang]||t.en;
    document.documentElement.lang=lang;
    document.querySelectorAll("[data-i18n]").forEach(el=>{
      const key=el.dataset.i18n;
      if(dict[key]!==undefined) el.textContent=dict[key];
    });
    const select=document.querySelector("#language");
    if(select) select.value=lang;
    document.querySelectorAll("[data-lang-link]").forEach(a=>{
      const raw=a.getAttribute("href");
      const u=new URL(raw,location.href);
      u.searchParams.set("lang",lang);
      a.href=u.toString();
    });
    const page=document.body.dataset.page||"home";
    const titles={home:"PMDDcam",features:dict.fTitle+" · PMDDcam",technology:dict.techTitle+" · PMDDcam"};
    document.title=titles[page]||"PMDDcam";
  }

  document.addEventListener("DOMContentLoaded",()=>{
    apply(lang);
    const select=document.querySelector("#language");
    if(select) select.addEventListener("change",e=>{
      const next=normalize(e.target.value)||"en";
      try{localStorage.setItem("pmddcam-lang",next);}catch(err){}
      const u=new URL(location.href);u.searchParams.set("lang",next);
      history.replaceState(null,"",u);
      apply(next);
    });
  });
})();