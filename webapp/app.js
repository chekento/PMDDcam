const $ = (q) => document.querySelector(q);
const clamp = (v, a = 0, b = 1) => Math.min(b, Math.max(a, Number.isFinite(v) ? v : a));
const lerp = (a, b, t) => a + (b - a) * t;
const smooth = (a, b, x) => { const t = clamp((x - a) / Math.max(1e-6, b - a)); return t * t * (3 - 2 * t); };

const I18N = {
  de:{open:'Bild öffnen',camera:'Kamera',capture:'Aufnehmen',aiDepth:'KI-Tiefe',save:'Projekt sichern',looks:'80 PMDD Looks',mix:'Style-Mix',depthGeometry:'Tiefe & Raum',layers:'Ebenen',depthStrength:'Tiefenstärke',separation:'Layer-Trennung',focus:'Fokusebene',relief:'Relief',staticMotion:'Bewegungsillusion im Einzelbild',illusionStrength:'Illusionsstärke',peripheral:'Peripherie',motionScale:'Cue-Skalierung',lr:'Links / rechts',ud:'Oben / unten',nf:'Nah / fern',staticHint:'Die Bewegung bleibt im statischen Bild kodiert: asymmetrische Tiefenkanten, periphere Drift-Reize und Annäherungs-/Entfernungs-Cues reagieren auf die Blickperspektive des Betrachters.',viewer:'Interaktiver 2.5D/3D Viewer',parallax:'Parallaxe',liveMotion:'Objekt-/Layerbewegung',speed:'Animationsgeschwindigkeit',trackingHint:'Pointer/Touch funktioniert sofort. Head-Tracking lädt MediaPipe lokal in den Browser und nutzt die Frontkamera.',finish:'Bildcharakter',haze:'Atmosphäre',sharpness:'Struktur',bokeh:'Bokeh',saturation:'Sättigung',project:'Nicht-destruktives Projekt',privacy:'Original, Tiefenkarte und Rezept bleiben lokal im Browser. KI-Modelle werden bei Bedarf geladen; das Bild wird nicht zu einem PMDD-Server hochgeladen.'},
  en:{open:'Open image',camera:'Camera',capture:'Capture',aiDepth:'AI depth',save:'Save project',looks:'80 PMDD Looks',mix:'Style mix',depthGeometry:'Depth & space',layers:'Layers',depthStrength:'Depth strength',separation:'Layer separation',focus:'Focus plane',relief:'Relief',staticMotion:'Static motion illusion',illusionStrength:'Illusion strength',peripheral:'Peripheral cues',motionScale:'Cue scale',lr:'Left / right',ud:'Up / down',nf:'Near / far',staticHint:'Motion is encoded into the still image with asymmetric depth edges, peripheral drift cues and approach/retreat cues that react perceptually when the viewer changes position.',viewer:'Interactive 2.5D/3D viewer',parallax:'Parallax',liveMotion:'Object/layer motion',speed:'Animation speed',trackingHint:'Pointer/touch works immediately. Head tracking loads MediaPipe into the browser and uses the front camera.',finish:'Image character',haze:'Atmosphere',sharpness:'Structure',bokeh:'Bokeh',saturation:'Saturation',project:'Non-destructive project',privacy:'Original, depth map and recipe remain local in the browser. AI models load only when requested; the image is not uploaded to a PMDD server.'},
  fr:{open:'Ouvrir image',camera:'Caméra',capture:'Capturer',aiDepth:'Profondeur IA',save:'Sauver projet',looks:'80 looks PMDD',mix:'Mix style',depthGeometry:'Profondeur & espace',layers:'Calques',depthStrength:'Force profondeur',separation:'Séparation des plans',focus:'Plan focal',relief:'Relief',staticMotion:'Illusion de mouvement statique',illusionStrength:'Intensité',peripheral:'Périphérie',motionScale:'Échelle des indices',lr:'Gauche / droite',ud:'Haut / bas',nf:'Près / loin',viewer:'Viewer 2.5D/3D',parallax:'Parallaxe',liveMotion:'Mouvement des plans',speed:'Vitesse',finish:'Caractère image',haze:'Atmosphère',sharpness:'Structure',bokeh:'Bokeh',saturation:'Saturation',project:'Projet non destructif'},
  es:{open:'Abrir imagen',camera:'Cámara',capture:'Capturar',aiDepth:'Profundidad IA',save:'Guardar proyecto',looks:'80 estilos PMDD',mix:'Mezcla',depthGeometry:'Profundidad y espacio',layers:'Capas',depthStrength:'Fuerza de profundidad',separation:'Separación',focus:'Plano focal',relief:'Relieve',staticMotion:'Ilusión de movimiento estática',illusionStrength:'Intensidad',peripheral:'Periferia',motionScale:'Escala de pistas',lr:'Izq. / der.',ud:'Arriba / abajo',nf:'Cerca / lejos',viewer:'Visor 2.5D/3D',parallax:'Paralaje',liveMotion:'Movimiento de capas',speed:'Velocidad',finish:'Carácter',haze:'Atmósfera',sharpness:'Estructura',bokeh:'Bokeh',saturation:'Saturación',project:'Proyecto no destructivo'},
  zh:{open:'打开图片',camera:'相机',capture:'拍摄',aiDepth:'AI 深度',save:'保存项目',looks:'80 种 PMDD 风格',mix:'风格混合',depthGeometry:'深度与空间',layers:'图层',depthStrength:'深度强度',separation:'图层分离',focus:'焦点层',relief:'浮雕',staticMotion:'静态运动错觉',illusionStrength:'错觉强度',peripheral:'周边线索',motionScale:'线索尺度',lr:'左右',ud:'上下',nf:'远近',viewer:'交互式 2.5D/3D',parallax:'视差',liveMotion:'图层运动',speed:'动画速度',finish:'图像特征',haze:'氛围',sharpness:'结构',bokeh:'散景',saturation:'饱和度',project:'非破坏性项目'},
  ja:{open:'画像を開く',camera:'カメラ',capture:'撮影',aiDepth:'AI 深度',save:'プロジェクト保存',looks:'80 PMDD ルック',mix:'スタイル比率',depthGeometry:'深度と空間',layers:'レイヤー',depthStrength:'深度強度',separation:'レイヤー分離',focus:'フォーカス面',relief:'レリーフ',staticMotion:'静止画の動き錯視',illusionStrength:'錯視強度',peripheral:'周辺キュー',motionScale:'キュー尺度',lr:'左右',ud:'上下',nf:'近い / 遠い',viewer:'インタラクティブ 2.5D/3D',parallax:'視差',liveMotion:'レイヤー移動',speed:'速度',finish:'画像特性',haze:'雰囲気',sharpness:'構造',bokeh:'ボケ',saturation:'彩度',project:'非破壊プロジェクト'}
};

const STYLE_ROWS = `vivid|PMDD Vivid|Foto;natural|PMDD Natural|Foto;cinema|Cinematic|Foto;portrait|Soft Portrait|Foto;landscape|Alpine Clarity|Foto;gold|Golden Hour|Foto;blue|Blue Hour|Foto;noir|Film Noir|Foto;silver|Silver Gelatin|Foto;matte|Matte Editorial|Foto;chrome|Chrome Color|Foto;comic|Comic Classic|Illustration;manga|Manga Ink|Illustration;anime|Anime Cel|Illustration;graphic|Graphic Novel|Illustration;pop|Pop Art|Illustration;ligne|Ligne Claire|Illustration;pastelcel|Pastel Cel|Illustration;superhero|Superhero|Illustration;storybook|Storybook|Illustration;risocomic|Riso Comic|Illustration;watercolor|Wasserfarben|Atelier;aquarelle|Aquarell Warm|Atelier;gouache|Gouache|Atelier;oil|Ölgemälde|Atelier;impression|Impression|Atelier;ink|Tusche|Atelier;pencil|Bleistift|Atelier;coloredpencil|Buntstift|Atelier;charcoal|Kohle|Atelier;etching|Kupferstich|Atelier;retro70|Retro 70s|Retro;retro80|Retro 80s|Retro;instant|Instant Film|Retro;sepia|Sepia|Retro;cyanotype|Cyanotypie|Retro;vhs|VHS Print|Retro;arcade|8-Bit Arcade|Retro;pixel16|16-Bit Adventure|Retro;gameboy|Pocket Green|Retro;newspaper|Newspaper|Retro;futuretech|Futuretech|Zukunft;cyberpunk|Cyberpunk|Zukunft;neontokyo|Neon Tokyo|Zukunft;hologram|Holographic|Zukunft;synthwave|Synthwave|Zukunft;blueprint|Blueprint|Zukunft;infrared|Infrared Dream|Zukunft;matrix|Matrix Green|Zukunft;space|Deep Space|Zukunft;liquidmetal|Liquid Metal|Zukunft;dream|Dream Pastel|Atmosphäre;nordic|Nordic Mist|Atmosphäre;desert|Desert Sand|Atmosphäre;emerald|Emerald Forest|Atmosphäre;sakura|Sakura Bloom|Atmosphäre;underwater|Underwater|Atmosphäre;lava|Lava Light|Atmosphäre;arctic|Arctic Ice|Atmosphäre;moon|Moonlight|Atmosphäre;velvet|Velvet Dusk|Atmosphäre;slide|Slide Film|Foto;bleach|Bleach Bypass|Foto;dramatic|Dramatic B&W|Foto;softbloom|Soft Bloom|Foto;comicnoir|Comic Noir|Illustration;flatcel|Flat Cel|Illustration;posterpop|Poster Pop|Illustration;linocut|Linolschnitt|Atelier;sumie|Sumi-e|Atelier;chalk|Kreidepastell|Atelier;drybrush|Dry Brush|Atelier;pointillism|Pointillismus|Atelier;cga|CGA 4|Retro;c64|C64 16|Retro;crt|CRT Arcade|Retro;thermal|Thermal Vision|Zukunft;neonwire|Neon Wire|Zukunft;sunsetrose|Sunset Rose|Atmosphäre;dreambloom|Dream Bloom|Atmosphäre`;
const STYLES = STYLE_ROWS.split(';').map(row => { const [id,name,group] = row.split('|'); return {id,name,group}; });
const GROUPS = [...new Set(STYLES.map(s => s.group))];

const state = {
  lang:'de', source:null, sourceType:'demo', cameraStream:null, headStream:null,
  style:'vivid', styleMix:1, layers:96, depthStrength:1.25, separation:.8, focus:.6, relief:.35,
  illusion:.9, peripheral:.78, motionScale:.7, cueX:true, cueY:true, cueZ:true,
  parallax:1.15, liveMotion:.7, liveSpeed:.65, axisX:true, axisY:true, axisZ:true,
  haze:.06, sharpness:.3, bokeh:.12, saturation:1.03, invertDepth:false,
  depthMode:'heuristic', dirtySource:true, dirtyDepth:true, dirtyStyle:true, depthBusy:false,
  display:'pmdd', showDepth:false, live3d:true, compareX:.5,
  controlMode:'pointer', eye:[0,0,0], target:[0,0,0], headBaseWidth:null,
  headLandmarker:null, headBusy:false, lastHeadAt:0, tiltHandler:null,
  gl:null, glProgram:null, photoTex:null, depthTex:null, lastFrame:0, fpsFrames:0, fpsAt:performance.now(),
  db:null
};

const glCanvas = $('#glView');
const compareCanvas = $('#compareCanvas');
const sourceCanvas = $('#sourceCanvas');
const styledCanvas = $('#styledCanvas');
const depthCanvas = $('#depthCanvas');
const depthWorkCanvas = $('#depthWorkCanvas');
const cameraVideo = $('#cameraVideo');
const srcCtx = sourceCanvas.getContext('2d', {willReadFrequently:true});
const styleCtx = styledCanvas.getContext('2d', {willReadFrequently:true});
const depthCtx = depthCanvas.getContext('2d', {willReadFrequently:true});
const compareCtx = compareCanvas.getContext('2d');

function message(text, ms=2600){ const el=$('#stageMessage'); el.textContent=text; el.classList.add('show'); clearTimeout(message.timer); message.timer=setTimeout(()=>el.classList.remove('show'),ms); }
function pct(v){ return `${Math.round(v*100)}%`; }
function setOut(id, value){ const o=$(`#${id}Out`); if(o)o.value=value; }
function currentLang(){ const q=new URLSearchParams(location.search).get('lang') || navigator.language || 'de'; state.lang=['de','en','fr','es','zh','ja'].find(x=>q.toLowerCase().startsWith(x))||'en'; applyLang(); }
function applyLang(){ document.documentElement.lang=state.lang; const d=I18N[state.lang]||I18N.en; document.querySelectorAll('[data-i18n]').forEach(el=>{ const v=d[el.dataset.i18n]||I18N.en[el.dataset.i18n]; if(v)el.textContent=v; }); $('#langToggle').textContent=state.lang.toUpperCase(); }

function setupStyles(){
  const select=$('#styleSelect'); select.textContent='';
  for(const group of GROUPS){ const og=document.createElement('optgroup'); og.label=group; for(const s of STYLES.filter(x=>x.group===group)){ const op=document.createElement('option'); op.value=s.id; op.textContent=s.name; og.append(op); } select.append(og); }
  select.value=state.style;
  const chips=$('#lookChips'); chips.textContent='';
  for(const group of GROUPS){ const b=document.createElement('button'); b.type='button'; b.className='look-chip'; b.textContent=group; b.dataset.group=group; b.addEventListener('click',()=>{ const s=STYLES.find(x=>x.group===group); if(!s)return; state.style=s.id; select.value=s.id; markActiveGroup(); state.dirtyStyle=true; }); chips.append(b); }
  markActiveGroup();
}
function markActiveGroup(){ const g=STYLES.find(s=>s.id===state.style)?.group; document.querySelectorAll('.look-chip').forEach(b=>b.classList.toggle('active',b.dataset.group===g)); }

function styleRecipe(id){
  const s=STYLES.find(x=>x.id===id)||STYLES[0], idx=STYLES.indexOf(s), seed=(idx*43%101)/101;
  const base={Foto:{sat:1.03,con:1.04,warm:0,tech:'photo'},Illustration:{sat:1.18,con:1.13,warm:.02,tech:'comic'},Atelier:{sat:.95,con:.96,warm:.08,tech:'water'},Retro:{sat:.86,con:1.06,warm:.08,tech:'retro'},Zukunft:{sat:1.24,con:1.17,warm:-.07,tech:'neon'},'Atmosphäre':{sat:.9,con:.94,warm:.04,tech:'atmo'}}[s.group];
  const r={...base,sat:base.sat+(seed-.5)*.16,con:base.con+(seed-.5)*.13,warm:base.warm+(seed-.5)*.08,levels:256,grain:0,bloom:0,ink:0,halftone:0,pixel:0,duo:null,thermal:false,solar:false,vhs:false,wood:false,stipple:false};
  if(/noir|silver|manga|ink|pencil|charcoal|newspaper|dramatic|linocut|sumie/.test(id))r.sat=0;
  if(/comic|manga|anime|graphic|ligne|superhero|comicnoir|flatcel/.test(id)){r.tech='comic';r.levels=/comicnoir|manga/.test(id)?4:7;r.ink=/anime|flatcel/.test(id)?.42:.8;}
  if(/pop|risocomic|newspaper/.test(id)){r.tech='halftone';r.levels=6;r.halftone=.85;}
  if(/posterpop/.test(id)){r.tech='poster';r.levels=5;}
  if(/watercolor|aquarelle|storybook|dream|sakura|drybrush/.test(id)){r.tech='water';r.levels=18;r.grain=.08;}
  if(/gouache|oil|impression/.test(id)){r.tech='oil';r.levels=20;r.grain=.15;}
  if(/pencil|coloredpencil|charcoal|etching|linocut|sumie/.test(id)){r.tech='draw';r.grain=.17;r.ink=/linocut|charcoal/.test(id)?.9:.62;}
  if(/arcade|pixel16|gameboy|cga|c64|crt/.test(id)){r.tech='pixel';r.pixel=/arcade|gameboy|cga/.test(id)?9:6;r.levels=/gameboy|cga/.test(id)?4:/c64/.test(id)?16:8;}
  if(/futuretech|cyberpunk|neontokyo|hologram|matrix|space|lava|neonwire/.test(id)){r.tech='neon';r.bloom=.55;}
  if(/softbloom|dreambloom/.test(id)){r.tech='bloom';r.bloom=.8;}
  if(/thermal/.test(id)){r.tech='thermal';r.thermal=true;}
  if(/infrared|liquidmetal/.test(id)){r.tech='solar';r.solar=true;}
  if(/vhs|crt/.test(id))r.vhs=true;
  if(/pointillism/.test(id)){r.tech='stipple';r.stipple=true;}
  if(/sepia/.test(id))r.duo=['#271709','#f9e2b1'];
  if(/cyanotype|blueprint/.test(id))r.duo=['#061f54','#d8eef3'];
  if(/matrix/.test(id))r.duo=['#041108','#75ff91'];
  if(/synthwave/.test(id))r.duo=['#361349','#78e8f2'];
  if(/desert/.test(id))r.duo=['#55352c','#ffe8b1'];
  if(/underwater/.test(id))r.duo=['#072a45','#a1eee1'];
  if(/moon/.test(id))r.duo=['#0d142c','#c9ddfa'];
  if(/velvet/.test(id))r.duo=['#391c43','#f7ccd6'];
  if(/sunsetrose/.test(id))r.duo=['#341e62','#ffcb95'];
  if(/gold/.test(id)){r.warm=.23;r.sat=1.12;}
  if(/blue/.test(id)){r.warm=-.23;r.sat=.94;}
  if(/retro70/.test(id)){r.warm=.19;r.sat=.72;r.grain=.18;}
  if(/retro80/.test(id)){r.sat=1.24;r.grain=.13;}
  if(/instant/.test(id)){r.warm=.12;r.sat=.76;r.con=.82;r.grain=.12;}
  if(/bleach/.test(id)){r.sat=.34;r.con=1.48;r.grain=.1;}
  if(/slide|chrome/.test(id)){r.sat=1.36;r.con=1.18;}
  return r;
}

function hexRgb(h){ const n=parseInt(h.slice(1),16); return [(n>>16)&255,(n>>8)&255,n&255]; }
function luma(data,i){ return (data[i]*.2126+data[i+1]*.7152+data[i+2]*.0722)/255; }
function quant(v,n){ return Math.round(clamp(v)*(n-1))/(n-1); }
function noise(x,y){ let n=(x*374761393+y*668265263)|0; n=((n^(n>>>13))*1274126177)|0; return ((n&65535)/32767.5)-1; }
function thermalColor(t){ t=clamp(t); const stops=[[0,.02,.18],[0,.12,.55],[.08,.75,.85],[.96,.85,.08],[1,.14,.02]]; const p=t*(stops.length-1),i=Math.min(stops.length-2,Math.floor(p)),f=p-i,a=stops[i],b=stops[i+1]; return [lerp(a[0],b[0],f),lerp(a[1],b[1],f),lerp(a[2],b[2],f)]; }

async function imageToBitmap(src){ if(src instanceof ImageBitmap)return src; if(src instanceof HTMLImageElement||src instanceof HTMLVideoElement||src instanceof HTMLCanvasElement)return src; return await createImageBitmap(src); }
function fitCanvas(ctx,img,w,h){ ctx.fillStyle='#030a0e';ctx.fillRect(0,0,w,h); const iw=img.videoWidth||img.naturalWidth||img.width,ih=img.videoHeight||img.naturalHeight||img.height,scale=Math.min(w/iw,h/ih),dw=iw*scale,dh=ih*scale;ctx.drawImage(img,(w-dw)/2,(h-dh)/2,dw,dh); }

async function setSource(input,type='photo'){
  const img=await imageToBitmap(input); const iw=img.videoWidth||img.naturalWidth||img.width,ih=img.videoHeight||img.naturalHeight||img.height;
  const maxEdge=1280, scale=Math.min(1,maxEdge/Math.max(iw,ih)), w=Math.max(2,Math.round(iw*scale)),h=Math.max(2,Math.round(ih*scale));
  [sourceCanvas,styledCanvas,depthCanvas,depthWorkCanvas].forEach(c=>{c.width=w;c.height=h});
  fitCanvas(srcCtx,img,w,h); state.source=img; state.sourceType=type; state.depthMode='heuristic'; state.dirtySource=true;state.dirtyDepth=true;state.dirtyStyle=true;
  $('#sourceBadge').textContent=type.toUpperCase();$('#resolutionStat').textContent=`${w}×${h}`;$('#depthStat').textContent='HEURISTIC';$('#depthBadge').textContent='HEURISTIC DEPTH';
  computeHeuristicDepth(); renderStatic(); uploadTextures(); renderCompareOverlay();
}

function computeHeuristicDepth(){
  const w=sourceCanvas.width,h=sourceCanvas.height;if(!w||!h)return;
  const src=srcCtx.getImageData(0,0,w,h),a=src.data,n=w*h,raw=new Float32Array(n),lum=new Float32Array(n),edge=new Float32Array(n);
  for(let i=0,p=0;i<n;i++,p+=4)lum[i]=(a[p]*.2126+a[p+1]*.7152+a[p+2]*.0722)/255;
  const at=(x,y)=>lum[Math.max(0,Math.min(h-1,y))*w+Math.max(0,Math.min(w-1,x))];
  for(let y=0;y<h;y++)for(let x=0;x<w;x++){const i=y*w+x,L=lum[i],gx=(at(x+2,y)-at(x-2,y))*.5,gy=(at(x,y+2)-at(x,y-2))*.5,e=Math.min(1,Math.hypot(gx,gy)*3.2);edge[i]=e;const rx=(x/(w-1)-.5)*2,ry=(y/(h-1)-.5)*2,center=1-Math.min(1,Math.hypot(rx,ry)),bottom=y/(h-1),local=(Math.abs(L-at(x+5,y))+Math.abs(L-at(x-5,y))+Math.abs(L-at(x,y+5))+Math.abs(L-at(x,y-5)))*.25;raw[i]=.28*bottom+.26*center+.23*e+.15*local+.08*(1-L);}
  // Guided smoothing: preserve strong luminance edges while removing depth speckle.
  let cur=raw,tmp=new Float32Array(n);
  for(let pass=0;pass<3;pass++){for(let y=0;y<h;y++)for(let x=0;x<w;x++){const i=y*w+x;let sum=cur[i]*2,ws=2;for(const [dxo,dyo] of [[1,0],[-1,0],[0,1],[0,-1],[2,0],[-2,0],[0,2],[0,-2]]){const xx=Math.max(0,Math.min(w-1,x+dxo)),yy=Math.max(0,Math.min(h-1,y+dyo)),j=yy*w+xx,d=Math.abs(lum[i]-lum[j]),ww=1/(1+40*d*d);sum+=cur[j]*ww;ws+=ww;}tmp[i]=sum/ws;}[cur,tmp]=[tmp,cur];}
  let lo=1,hi=0;for(let i=0;i<n;i++){lo=Math.min(lo,cur[i]);hi=Math.max(hi,cur[i]);}const span=Math.max(1e-6,hi-lo),out=depthCtx.createImageData(w,h),d=out.data;
  for(let i=0,p=0;i<n;i++,p+=4){let z=clamp((cur[i]-lo)/span);z=smooth(.02,.98,z);if(state.invertDepth)z=1-z;const v=Math.round(z*255);d[p]=d[p+1]=d[p+2]=v;d[p+3]=255;}depthCtx.putImageData(out,0,0);state.depthMode='heuristic';state.dirtyDepth=false;uploadTextures();}

let depthEstimator=null;
async function computeAIDepth(){
  if(state.depthBusy)return;state.depthBusy=true;$('#aiDepthButton').disabled=true;message('Depth Anything V2 wird geladen …',5000);
  try{
    if(!depthEstimator){ const mod=await import('https://cdn.jsdelivr.net/npm/@huggingface/transformers@4.3.0'); const opts={progress_callback:p=>{if(p?.status==='progress'&&Number.isFinite(p.progress))message(`KI-Tiefenmodell ${Math.round(p.progress)}%`,1200);}}; if(navigator.gpu)opts.device='webgpu'; try{depthEstimator=await mod.pipeline('depth-estimation','onnx-community/depth-anything-v2-small',opts);}catch(e){delete opts.device;depthEstimator=await mod.pipeline('depth-estimation','onnx-community/depth-anything-v2-small',opts);} }
    message('KI-Tiefe wird berechnet …',8000); const result=await depthEstimator(sourceCanvas); const raw=result.depth; if(!raw||!raw.data)throw new Error('No depth image returned');
    const work=depthWorkCanvas;work.width=raw.width;work.height=raw.height;const wc=work.getContext('2d'),im=wc.createImageData(raw.width,raw.height);for(let i=0,p=0;i<raw.data.length;i++,p+=4){let v=raw.data[i];if(state.invertDepth)v=255-v;im.data[p]=im.data[p+1]=im.data[p+2]=v;im.data[p+3]=255;}wc.putImageData(im,0,0);depthCtx.clearRect(0,0,depthCanvas.width,depthCanvas.height);depthCtx.imageSmoothingEnabled=true;depthCtx.drawImage(work,0,0,depthCanvas.width,depthCanvas.height);state.depthMode='ai';state.dirtyDepth=false;state.dirtyStyle=true;$('#depthStat').textContent='AI · DAv2';$('#depthBadge').textContent='AI DEPTH · DAv2';message('Depth Anything V2 aktiv');renderStatic();uploadTextures();
  }catch(e){console.error(e);message('KI-Tiefe konnte nicht geladen werden — heuristische Tiefe bleibt aktiv.',4500);}
  finally{state.depthBusy=false;$('#aiDepthButton').disabled=false;}
}

function renderStatic(){
  const w=sourceCanvas.width,h=sourceCanvas.height;if(!w||!h)return;
  const src=srcCtx.getImageData(0,0,w,h),dep=depthCtx.getImageData(0,0,w,h),out=styleCtx.createImageData(w,h),a=src.data,zdat=dep.data,o=out.data,r=styleRecipe(state.style),du=r.duo?.map(hexRgb),step=Math.max(1,Math.round(Math.min(w,h)/(230-state.motionScale*70)));
  const L=(x,y)=>{x=Math.max(0,Math.min(w-1,x));y=Math.max(0,Math.min(h-1,y));return luma(a,(y*w+x)*4)};
  const Z=(x,y)=>{x=Math.max(0,Math.min(w-1,x));y=Math.max(0,Math.min(h-1,y));return zdat[(y*w+x)*4]/255};
  for(let y=0;y<h;y++)for(let x=0;x<w;x++){
    const i=(y*w+x)*4,baseR=a[i]/255,baseG=a[i+1]/255,baseB=a[i+2]/255,l=L(x,y),z=Z(x,y),gx=(L(x+step,y)-L(x-step,y))*.5,gy=(L(x,y+step)-L(x,y-step))*.5,dzx=(Z(x+step,y)-Z(x-step,y))*.5,dzy=(Z(x,y+step)-Z(x,y-step))*.5,edge=clamp((Math.abs(gx)+Math.abs(gy))*2.5),rx=x/(w-1)-.5,ry=y/(h-1)-.5,rad=clamp(Math.hypot(rx,ry)*1.414),per=.4+.6*lerp(1,state.peripheral,rad),blur=(L(x-step,y)+L(x+step,y)+L(x,y-step)+L(x,y+step))*.25;
    let R=baseR,G=baseG,B=baseB,Y=l;
    // Style families have visibly different rendering behavior, not just hue shifts.
    if(r.tech==='water'||r.tech==='oil'){const soften=r.tech==='oil'?.55:.72;R=lerp(R,blur,soften*.18);G=lerp(G,blur,soften*.18);B=lerp(B,blur,soften*.18);}
    if(r.tech==='draw'){const ink=clamp(1-edge*r.ink);R=G=B=lerp(l,1-l*.18,0.08)*ink;}
    if(r.tech==='comic'||r.tech==='poster'||r.tech==='halftone'){R=quant(R,r.levels);G=quant(G,r.levels);B=quant(B,r.levels);const ink=1-edge*r.ink*.8;R*=ink;G*=ink;B*=ink;}
    if(r.tech==='pixel'){const bx=Math.floor(x/r.pixel)*r.pixel,by=Math.floor(y/r.pixel)*r.pixel,bi=(Math.min(h-1,by)*w+Math.min(w-1,bx))*4;R=quant(a[bi]/255,r.levels);G=quant(a[bi+1]/255,r.levels);B=quant(a[bi+2]/255,r.levels);}
    if(r.tech==='neon'){const e=edge*(.75+.7*z);R=R*.34+e*.25;G=G*.34+e*.95;B=B*.39+e*.82;}
    if(r.tech==='thermal'){[R,G,B]=thermalColor(l);}
    if(r.tech==='solar'){R=Math.abs(.5-R)*1.9;G=Math.abs(.5-G)*1.8;B=Math.abs(.5-B)*1.95;}
    if(r.tech==='stipple'){const dot=((x*13+y*7)%11)<Math.round(l*10)?1:.22;R*=dot;G*=dot;B*=dot;}
    if(r.halftone){const cell=5+(step%3),dx=(x%cell)/cell-.5,dy=(y%cell)/cell-.5,dot=Math.hypot(dx,dy)<Math.sqrt(1-l)*.45?.72:1;R*=dot;G*=dot;B*=dot;}
    Y=R*.2126+G*.7152+B*.0722;const sat=r.sat*state.saturation;R=Y+(R-Y)*sat;G=Y+(G-Y)*sat;B=Y+(B-Y)*sat;R=(R-.5)*r.con+.5+r.warm*.12;G=(G-.5)*r.con+.5;B=(B-.5)*r.con+.5-r.warm*.14;
    if(du){Y=clamp(R*.2126+G*.7152+B*.0722);R=lerp(du[0][0]/255,du[1][0]/255,Y);G=lerp(du[0][1]/255,du[1][1]/255,Y);B=lerp(du[0][2]/255,du[1][2]/255,Y);}
    if(r.vhs){R=lerp(R,L(x-step*2,y),.22);B=lerp(B,L(x+step*2,y),.22);if(y%Math.max(2,step*2)===0){R*=.91;G*=.91;B*=.91;}}
    const detail=(l-blur)*state.sharpness*(.25+.9*z)*(1-edge*.5);
    const depthEdge=(state.cueX?dzx:0)+(state.cueY?dzy:0);
    const imageFlow=(state.cueX?gx:0)+(state.cueY?gy:0);
    const radialFlow=state.cueZ?(gx*rx+gy*ry)*2:0;
    // The still-image illusion uses asymmetric edge phase, depth boundaries and peripheral weighting.
    const cue=(imageFlow*.52+depthEdge*.78*state.depthStrength+radialFlow*.46)*state.illusion*(.22+.78*z)*per*state.motionScale;
    const phaseCue=((L(x-step*2,y-step)-L(x+step,y+step))*.58+(L(x-step,y+step)-L(x+step*2,y-step))*.34)*state.illusion*.09*per*(.2+.8*z);
    const relief=(z-state.focus)*state.relief*state.depthStrength*.055 + depthEdge*state.separation*state.depthStrength*.08;
    const grain=noise(x,y)*r.grain*.035;
    let tone=detail+cue*.12+phaseCue+relief+grain;
    if(r.bloom){const bloom=Math.max(0,l-.62)*r.bloom;R+=bloom*.28;G+=bloom*.34;B+=bloom*.4;}
    const far=1-z,haze=state.haze*Math.pow(far,1.5)*.28,bokeh=state.bokeh*smooth(0,.7,state.focus-z)*(1-edge*.85)*.18;
    R=lerp(R,blur,bokeh);G=lerp(G,blur,bokeh);B=lerp(B,blur,bokeh);R=clamp(R+tone);G=clamp(G+tone);B=clamp(B+tone);R=lerp(R,.8,haze);G=lerp(G,.87,haze);B=lerp(B,.94,haze);
    R=lerp(baseR,R,state.styleMix);G=lerp(baseG,G,state.styleMix);B=lerp(baseB,B,state.styleMix);
    o[i]=Math.round(clamp(R)*255);o[i+1]=Math.round(clamp(G)*255);o[i+2]=Math.round(clamp(B)*255);o[i+3]=255;
  }
  styleCtx.putImageData(out,0,0);state.dirtyStyle=false;uploadTextures();renderCompareOverlay();
}

function initGL(){
  const gl=glCanvas.getContext('webgl',{alpha:false,antialias:true,preserveDrawingBuffer:true});state.gl=gl;if(!gl){$('#gpuStat').textContent='NO WEBGL';message('WebGL ist nicht verfügbar.');return;}
  const compile=(type,src)=>{const s=gl.createShader(type);gl.shaderSource(s,src);gl.compileShader(s);if(!gl.getShaderParameter(s,gl.COMPILE_STATUS))throw new Error(gl.getShaderInfoLog(s));return s;};
  const vs=`attribute vec2 p;varying vec2 uv;uniform vec2 fit;void main(){gl_Position=vec4(p*fit,0.,1.);uv=vec2(p.x*.5+.5,.5-p.y*.5);}`;
  const fs=`precision highp float;varying vec2 uv;uniform sampler2D photo,depthMap;uniform vec3 eye;uniform float focus,layers,time,liveMotion,liveSpeed,depthStrength,separation;uniform int showDepth;
  float hash(float n){return fract(sin(n*91.3458)*47453.5453);} 
  void main(){
    if(showDepth==1){float d=texture2D(depthMap,uv).r;gl_FragColor=vec4(vec3(d),1.);return;}
    vec2 direction=eye.xy+(uv-.5)*eye.z; direction*=depthStrength;
    vec2 best=uv;float hit=0.;float prevGap=0.;float prevPlane=1.;
    if(dot(direction,direction)>.000000001){
      for(int i=0;i<128;i++){if(float(i)>=layers)break;float plane=1.-float(i)/(layers-1.);vec2 q=uv-direction*(plane-focus)*(.72+separation*.55);q=clamp(q,vec2(.001),vec2(.999));float d=texture2D(depthMap,q).r;float gap=plane-d;if(hit<.5&&gap<=0.){float f=prevGap/max(.00001,prevGap-gap);float surf=mix(prevPlane,plane,clamp(f,0.,1.));best=uv-direction*(surf-focus)*(.72+separation*.55);hit=1.;}prevGap=max(0.,gap);prevPlane=plane;}
    }
    best=clamp(best,vec2(.002),vec2(.998));float d=texture2D(depthMap,best).r;float q=floor(d*max(8.,layers))/max(8.,layers);float ang=hash(q*47.1)*6.2831853;float phase=hash(q*103.7)*6.2831853;float wave=sin(time*liveSpeed*(.65+hash(q*71.)*.9)+phase);vec2 layerDir=vec2(cos(ang),sin(ang));best+=layerDir*wave*liveMotion*.004*(.25+d*.75);
    // Depth-consistent disocclusion fill: reject obviously different foreground/background samples and blend nearby valid texels.
    vec4 c0=texture2D(photo,clamp(best,vec2(.001),vec2(.999)));float bd=texture2D(depthMap,clamp(best,vec2(.001),vec2(.999))).r;vec4 acc=c0;float ws=1.;
    for(int j=1;j<=3;j++){float s=float(j)*.0025;vec2 off=normalize(direction+vec2(.0001))*s;vec2 a=clamp(best+off,vec2(.001),vec2(.999)),b=clamp(best-off,vec2(.001),vec2(.999));float da=texture2D(depthMap,a).r,db=texture2D(depthMap,b).r;float wa=1.-smoothstep(.035,.18,abs(da-bd)),wb=1.-smoothstep(.035,.18,abs(db-bd));acc+=texture2D(photo,a)*wa+texture2D(photo,b)*wb;ws+=wa+wb;}
    gl_FragColor=acc/ws;
  }`;
  const pr=gl.createProgram();gl.attachShader(pr,compile(gl.VERTEX_SHADER,vs));gl.attachShader(pr,compile(gl.FRAGMENT_SHADER,fs));gl.linkProgram(pr);if(!gl.getProgramParameter(pr,gl.LINK_STATUS))throw new Error(gl.getProgramInfoLog(pr));state.glProgram=pr;gl.useProgram(pr);
  const buf=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,buf);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array([-1,-1,1,-1,-1,1,1,1]),gl.STATIC_DRAW);const p=gl.getAttribLocation(pr,'p');gl.enableVertexAttribArray(p);gl.vertexAttribPointer(p,2,gl.FLOAT,false,0,0);
  const tex=(unit)=>{const t=gl.createTexture();gl.activeTexture(gl.TEXTURE0+unit);gl.bindTexture(gl.TEXTURE_2D,t);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.LINEAR);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.LINEAR);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_S,gl.CLAMP_TO_EDGE);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_T,gl.CLAMP_TO_EDGE);return t;};state.photoTex=tex(0);state.depthTex=tex(1);gl.uniform1i(gl.getUniformLocation(pr,'photo'),0);gl.uniform1i(gl.getUniformLocation(pr,'depthMap'),1);resizeGL();$('#gpuStat').textContent=navigator.gpu?'WEBGPU+GL':'WEBGL';
}
function uploadTextures(){const gl=state.gl;if(!gl||!styledCanvas.width)return;gl.activeTexture(gl.TEXTURE0);gl.bindTexture(gl.TEXTURE_2D,state.photoTex);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,state.display==='original'?sourceCanvas:styledCanvas);gl.activeTexture(gl.TEXTURE1);gl.bindTexture(gl.TEXTURE_2D,state.depthTex);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,depthCanvas);}
function resizeGL(){const r=$('#stage').getBoundingClientRect(),d=Math.min(devicePixelRatio||1,2),w=Math.max(2,Math.round(r.width*d)),h=Math.max(2,Math.round(r.height*d));if(glCanvas.width!==w||glCanvas.height!==h){glCanvas.width=w;glCanvas.height=h;compareCanvas.width=w;compareCanvas.height=h;}state.gl?.viewport(0,0,w,h);renderCompareOverlay();}
function renderCompareOverlay(){if(!sourceCanvas.width||!compareCanvas.width)return;compareCtx.clearRect(0,0,compareCanvas.width,compareCanvas.height);fitCanvas(compareCtx,sourceCanvas,compareCanvas.width,compareCanvas.height);const pctClip=100-state.compareX*100;compareCanvas.style.clipPath=`inset(0 ${pctClip}% 0 0)`;$('#compareHandle').style.left=`${state.compareX*100}%`;}
function renderFrame(now){
  const gl=state.gl,pr=state.glProgram;for(let i=0;i<3;i++)state.eye[i]=lerp(state.eye[i],state.target[i],.16);
  if(gl&&pr&&styledCanvas.width){const ia=styledCanvas.width/styledCanvas.height,sa=glCanvas.width/glCanvas.height,fitX=ia>sa?1:ia/sa,fitY=ia>sa?sa/ia:1;gl.useProgram(pr);const u=n=>gl.getUniformLocation(pr,n),g=state.live3d?(.092*(1-Math.exp(-state.parallax*state.depthStrength*.92))):0;gl.uniform2f(u('fit'),fitX,fitY);gl.uniform3f(u('eye'),state.axisX?state.eye[0]*g:0,state.axisY?state.eye[1]*g:0,state.axisZ?state.eye[2]*g*1.55:0);gl.uniform1f(u('focus'),state.focus);gl.uniform1f(u('layers'),state.layers);gl.uniform1f(u('time'),now/1000);gl.uniform1f(u('liveMotion'),state.live3d?state.liveMotion:0);gl.uniform1f(u('liveSpeed'),state.liveSpeed);gl.uniform1f(u('depthStrength'),state.depthStrength);gl.uniform1f(u('separation'),state.separation);gl.uniform1i(u('showDepth'),state.showDepth?1:0);gl.drawArrays(gl.TRIANGLE_STRIP,0,4);}
  state.fpsFrames++;if(now-state.fpsAt>1000){const fps=Math.round(state.fpsFrames*1000/(now-state.fpsAt));$('#fpsBadge').textContent=`${fps} FPS`;state.fpsFrames=0;state.fpsAt=now;}
  if(state.controlMode==='head')trackHead(now);requestAnimationFrame(renderFrame);
}

async function openFile(file){const bmp=await createImageBitmap(file);await setSource(bmp,'photo');}
async function startCamera(facing='environment',forHead=false){
  try{const stream=await navigator.mediaDevices.getUserMedia({video:{facingMode:facing,width:{ideal:1280},height:{ideal:720}},audio:false});cameraVideo.srcObject=stream;await cameraVideo.play();if(forHead)state.headStream=stream;else state.cameraStream=stream;return stream;}catch(e){console.error(e);message('Kamera nicht verfügbar');return null;}
}
function stopStream(stream){stream?.getTracks?.().forEach(t=>t.stop());}
async function captureCamera(){if(cameraVideo.readyState<2)return;const c=document.createElement('canvas');c.width=cameraVideo.videoWidth;c.height=cameraVideo.videoHeight;c.getContext('2d').drawImage(cameraVideo,0,0);const bmp=await createImageBitmap(c);await setSource(bmp,'camera');stopStream(state.cameraStream);state.cameraStream=null;$('#captureButton').disabled=true;}

async function initHeadTracking(){
  if(state.headLandmarker)return true;message('MediaPipe Face Landmarker wird geladen …',5000);
  try{const mp=await import('https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@1.0.1/vision_bundle.mjs');const vision=await mp.FilesetResolver.forVisionTasks('https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@1.0.1/wasm');state.headLandmarker=await mp.FaceLandmarker.createFromOptions(vision,{baseOptions:{modelAssetPath:'https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task'},runningMode:'VIDEO',numFaces:1,minFaceDetectionConfidence:.45,minFacePresenceConfidence:.45,minTrackingConfidence:.45,outputFaceBlendshapes:false,outputFacialTransformationMatrixes:false});return true;}catch(e){console.error(e);message('MediaPipe Head-Tracking konnte nicht geladen werden.',4500);return false;}
}
async function enableHeadMode(){
  if(!(await initHeadTracking())){setControlMode('pointer');return;}stopStream(state.cameraStream);state.cameraStream=null;$('#captureButton').disabled=true;stopStream(state.headStream);if(!(await startCamera('user',true))){setControlMode('pointer');return;}state.headBaseWidth=null;message('HEAD aktiv · bewege Kopf links/rechts, oben/unten und näher/weiter');
}
function disableHeadMode(){stopStream(state.headStream);state.headStream=null;state.headBaseWidth=null;state.target=[0,0,0];}
async function trackHead(now){if(!state.headLandmarker||!state.headStream||state.headBusy||now-state.lastHeadAt<45||cameraVideo.readyState<2)return;state.headBusy=true;state.lastHeadAt=now;try{const result=state.headLandmarker.detectForVideo(cameraVideo,now);const l=result.faceLandmarks?.[0];if(l?.length>454){const left=l[234],right=l[454],nose=l[1],top=l[10],chin=l[152],eyeL=l[33],eyeR=l[263];const cx=(left.x+right.x)/2,cy=(top.y+chin.y)/2,fw=Math.abs(right.x-left.x),fh=Math.abs(chin.y-top.y),eyeY=(eyeL.y+eyeR.y)/2;state.headBaseWidth??=fw;const yaw=clamp((nose.x-cx)/Math.max(.03,fw)*4,-1,1),pitch=clamp(((nose.y-eyeY)/Math.max(.03,fh)-.20)*3,-1,1),tx=clamp((.5-cx)*2.8-yaw*.62,-1,1),ty=clamp((cy-.5)*2.5+pitch*.45,-1,1),tz=clamp((fw/state.headBaseWidth-1)*2.4,-1,1);state.target=[tx,ty,tz];}}catch(e){console.warn(e);}finally{state.headBusy=false;}}

async function setControlMode(mode){
  if(mode===state.controlMode)return; if(state.controlMode==='head')disableHeadMode(); if(state.controlMode==='tilt'&&state.tiltHandler){window.removeEventListener('deviceorientation',state.tiltHandler);state.tiltHandler=null;}
  state.controlMode=mode;document.querySelectorAll('.mode-button').forEach(b=>b.classList.remove('active'));const map={pointer:'#pointerMode',tilt:'#tiltMode',head:'#headMode'};if(map[mode])$(map[mode]).classList.add('active');$('#trackingStatus').textContent=mode.toUpperCase();
  if(mode==='tilt'){try{if(typeof DeviceOrientationEvent==='undefined')throw new Error('unsupported');if(DeviceOrientationEvent.requestPermission&&await DeviceOrientationEvent.requestPermission()!=='granted')throw new Error('denied');state.tiltHandler=e=>{const g=e.gamma||0,b=e.beta||0;state.target=[clamp(g/28,-1,1),clamp((b-45)/35,-1,1),state.target[2]];};window.addEventListener('deviceorientation',state.tiltHandler);message('TILT aktiv');}catch(e){message('Geräteneigung nicht verfügbar');state.controlMode='pointer';$('#pointerMode').classList.add('active');$('#trackingStatus').textContent='POINTER';}}
  if(mode==='head')await enableHeadMode();
}

function bindRange(id,key,formatter=pct,rerender=true){const el=$(`#${id}`);el.addEventListener('input',()=>{state[key]=+el.value;setOut(id,formatter(state[key]));if(rerender){state.dirtyStyle=true;renderStatic();}});setOut(id,formatter(state[key]));}
function bindCheck(id,key,rerender=false){const el=$(`#${id}`);el.addEventListener('change',()=>{state[key]=el.checked;if(rerender)renderStatic();});}
function cycleDisplay(){state.display=state.display==='pmdd'?'compare':state.display==='compare'?'original':'pmdd';const b=$('#compareToggle');b.textContent=state.display==='pmdd'?'PMDD':state.display==='compare'?'COMPARE':'ORIGINAL';b.classList.toggle('active',state.display!=='original');compareCanvas.hidden=state.display!=='compare';$('#compareHandle').hidden=state.display!=='compare';uploadTextures();}

async function openDb(){return new Promise((resolve,reject)=>{const req=indexedDB.open('pmddcam-web',1);req.onupgradeneeded=()=>req.result.createObjectStore('projects',{keyPath:'id'});req.onsuccess=()=>resolve(req.result);req.onerror=()=>reject(req.error);});}
function canvasBlob(canvas,type='image/webp',quality=.92){return new Promise(resolve=>canvas.toBlob(resolve,type,quality));}
function recipeSnapshot(){return {schema:2,version:'0.2.0',created:new Date().toISOString(),recipe:{style:state.style,styleMix:state.styleMix,layers:state.layers,depthStrength:state.depthStrength,separation:state.separation,focus:state.focus,relief:state.relief,illusion:state.illusion,peripheral:state.peripheral,motionScale:state.motionScale,cueX:state.cueX,cueY:state.cueY,cueZ:state.cueZ,parallax:state.parallax,liveMotion:state.liveMotion,liveSpeed:state.liveSpeed,axisX:state.axisX,axisY:state.axisY,axisZ:state.axisZ,haze:state.haze,sharpness:state.sharpness,bokeh:state.bokeh,saturation:state.saturation,invertDepth:state.invertDepth},depthMode:state.depthMode};}
async function saveProject(){try{state.db??=await openDb();const source=await canvasBlob(sourceCanvas),depth=await canvasBlob(depthCanvas,'image/png'),project={id:'latest',saved:Date.now(),source,depth,meta:recipeSnapshot()};await new Promise((resolve,reject)=>{const tx=state.db.transaction('projects','readwrite'),req=tx.objectStore('projects').put(project);req.onsuccess=resolve;req.onerror=()=>reject(req.error);});$('#projectStat').textContent='SAVED';message('Projekt lokal in IndexedDB gesichert');}catch(e){console.error(e);message('Projekt konnte nicht gesichert werden');}}
async function loadLatest(){try{state.db??=await openDb();const project=await new Promise((resolve,reject)=>{const tx=state.db.transaction('projects','readonly'),req=tx.objectStore('projects').get('latest');req.onsuccess=()=>resolve(req.result);req.onerror=()=>reject(req.error);});if(!project)return false;const bmp=await createImageBitmap(project.source);await setSource(bmp,'saved');applyRecipe(project.meta?.recipe);const dbmp=await createImageBitmap(project.depth);depthCtx.clearRect(0,0,depthCanvas.width,depthCanvas.height);depthCtx.drawImage(dbmp,0,0,depthCanvas.width,depthCanvas.height);state.depthMode=project.meta?.depthMode||'saved';$('#depthStat').textContent=state.depthMode.toUpperCase();$('#projectStat').textContent='SAVED';renderStatic();return true;}catch(e){console.warn(e);return false;}}
function applyRecipe(r={}){for(const key of Object.keys(r)){if(key in state)state[key]=r[key];}const ids={styleMix:'styleMix',layers:'layers',depthStrength:'depthStrength',separation:'separation',focus:'focusDepth',relief:'relief',illusion:'illusionStrength',peripheral:'peripheral',motionScale:'motionScale',parallax:'parallax',liveMotion:'liveMotion',liveSpeed:'liveSpeed',haze:'haze',sharpness:'sharpness',bokeh:'bokeh',saturation:'saturation'};for(const [k,id] of Object.entries(ids)){const el=$(`#${id}`);if(el&&Number.isFinite(+state[k]))el.value=state[k];}$('#styleSelect').value=state.style;for(const [k,id] of [['cueX','cueX'],['cueY','cueY'],['cueZ','cueZ'],['axisX','axisX'],['axisY','axisY'],['axisZ','axisZ']])$(`#${id}`).checked=!!state[k];refreshOutputs();markActiveGroup();}
function refreshOutputs(){setOut('styleMix',pct(state.styleMix));setOut('layers',state.layers);setOut('depthStrength',pct(state.depthStrength));setOut('separation',pct(state.separation));setOut('focusDepth',pct(state.focus));setOut('relief',pct(state.relief));setOut('illusionStrength',pct(state.illusion));setOut('peripheral',pct(state.peripheral));setOut('motionScale',pct(state.motionScale));setOut('parallax',pct(state.parallax));setOut('liveMotion',pct(state.liveMotion));setOut('liveSpeed',pct(state.liveSpeed));setOut('haze',pct(state.haze));setOut('sharpness',pct(state.sharpness));setOut('bokeh',pct(state.bokeh));setOut('saturation',pct(state.saturation));$('#layerBadge').textContent=`${state.layers} LAYERS`;}
function downloadBlob(blob,name){const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(a.href),5000);}

function wireUI(){
  $('#fileInput').addEventListener('change',e=>{const f=e.target.files?.[0];if(f)openFile(f);});
  $('#cameraButton').addEventListener('click',async()=>{if(state.controlMode==='head')await setControlMode('pointer');stopStream(state.cameraStream);if(await startCamera('environment',false)){state.sourceType='live';$('#sourceBadge').textContent='LIVE CAMERA';$('#captureButton').disabled=false;message('Kamera aktiv — Aufnahme drücken');}});
  $('#captureButton').addEventListener('click',captureCamera);$('#aiDepthButton').addEventListener('click',computeAIDepth);$('#saveButton').addEventListener('click',saveProject);
  $('#exportButton').addEventListener('click',()=>styledCanvas.toBlob(b=>b&&downloadBlob(b,`PMDDcam-${Date.now()}.png`),'image/png'));
  $('#exportRecipe').addEventListener('click',()=>downloadBlob(new Blob([JSON.stringify(recipeSnapshot(),null,2)],{type:'application/json'}),`PMDDcam-${Date.now()}.pmdd.json`));
  $('#recipeInput').addEventListener('change',async e=>{const f=e.target.files?.[0];if(!f)return;try{const j=JSON.parse(await f.text());applyRecipe(j.recipe||j);renderStatic();message('PMDD-Rezept importiert');}catch(err){message('Ungültiges PMDD-Rezept');}});
  $('#compareToggle').addEventListener('click',cycleDisplay);$('#depthToggle').addEventListener('click',()=>{state.showDepth=!state.showDepth;$('#depthToggle').classList.toggle('active',state.showDepth);});$('#motionToggle').addEventListener('click',()=>{state.live3d=!state.live3d;$('#motionToggle').classList.toggle('active',state.live3d);if(!state.live3d)state.target=[0,0,0];});
  $('#styleSelect').addEventListener('change',e=>{state.style=e.target.value;markActiveGroup();renderStatic();});$('#randomLook').addEventListener('click',()=>{const s=STYLES[Math.floor(Math.random()*STYLES.length)];state.style=s.id;$('#styleSelect').value=s.id;markActiveGroup();renderStatic();});
  bindRange('styleMix','styleMix');bindRange('layers','layers',v=>Math.round(v),false);$('#layers').addEventListener('input',()=>{$('#layerBadge').textContent=`${state.layers} LAYERS`;});bindRange('depthStrength','depthStrength',pct,false);bindRange('separation','separation',pct);bindRange('focusDepth','focus',pct,false);bindRange('relief','relief');bindRange('illusionStrength','illusion');bindRange('peripheral','peripheral');bindRange('motionScale','motionScale');bindRange('parallax','parallax',pct,false);bindRange('liveMotion','liveMotion',pct,false);bindRange('liveSpeed','liveSpeed',pct,false);bindRange('haze','haze');bindRange('sharpness','sharpness');bindRange('bokeh','bokeh');bindRange('saturation','saturation');
  bindCheck('cueX','cueX',true);bindCheck('cueY','cueY',true);bindCheck('cueZ','cueZ',true);bindCheck('axisX','axisX');bindCheck('axisY','axisY');bindCheck('axisZ','axisZ');
  $('#pointerMode').addEventListener('click',()=>setControlMode('pointer'));$('#tiltMode').addEventListener('click',()=>setControlMode('tilt'));$('#headMode').addEventListener('click',()=>setControlMode('head'));$('#centerView').addEventListener('click',()=>{state.target=[0,0,0];state.eye=[0,0,0];});
  $('#langToggle').addEventListener('click',()=>{const a=['de','en','fr','es','zh','ja'],i=a.indexOf(state.lang);state.lang=a[(i+1)%a.length];applyLang();});
  const stage=$('#stage');let draggingCompare=false;stage.addEventListener('pointerdown',e=>{if(state.display==='compare'&&Math.abs(e.clientX-(stage.getBoundingClientRect().left+stage.clientWidth*state.compareX))<45){draggingCompare=true;stage.setPointerCapture(e.pointerId);return;}if(state.controlMode==='pointer')stage.setPointerCapture(e.pointerId);});stage.addEventListener('pointermove',e=>{const r=stage.getBoundingClientRect();if(draggingCompare){state.compareX=clamp((e.clientX-r.left)/r.width);renderCompareOverlay();return;}if(state.controlMode==='pointer'&&state.live3d){state.target[0]=clamp(((e.clientX-r.left)/r.width-.5)*2,-1,1);state.target[1]=clamp(((e.clientY-r.top)/r.height-.5)*2,-1,1);}});stage.addEventListener('pointerup',()=>draggingCompare=false);stage.addEventListener('pointercancel',()=>draggingCompare=false);stage.addEventListener('wheel',e=>{if(state.live3d){e.preventDefault();state.target[2]=clamp(state.target[2]-e.deltaY*.002,-1,1);}},{passive:false});
  window.addEventListener('resize',resizeGL);
}

async function boot(){currentLang();setupStyles();wireUI();initGL();refreshOutputs();let restored=await loadLatest();if(!restored){const img=new Image();img.crossOrigin='anonymous';img.onload=()=>setSource(img,'demo');img.src='../docs/assets/pmddcam-before.webp';}if('serviceWorker'in navigator)navigator.serviceWorker.register('./sw.js').catch(()=>{});requestAnimationFrame(renderFrame);}

boot();