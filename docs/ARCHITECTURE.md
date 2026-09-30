# PMDDcam 0.3.0 – Umsetzung

## Datenfluss

CameraX / System-Dateiauswahl → unveränderte Originaldatei → korrekt ausgerichtete Arbeitskopie → MiDaS → Objekt-/Gesichtserkennung → Tiefenfeld → Stil → statisches PMDD → Vorschau / Export.

Die interaktive Ansicht liest das fertige Bild und dasselbe effektive Tiefenfeld. Es gibt keinen Zeitparameter im Shader. Nur die Betrachterposition verändert die Ansicht. Damit sind statische Bewegungsillusion und echte interaktive Verschiebung klar getrennt.

## Zuordnung des PMDD-Frameworks

| PMDD-Prinzip | Implementierung | Grenze |
|---|---|---|
| L1/L2 Depth Foundation | Kontinuierliche relative KI-Tiefe, 8–128 Viewer-Abtastebenen, Kontrasthierarchie, Atmosphäre, Lichtrelief, Schärfe, Hintergrundweichzeichnung | Die Aufnahme liefert Perspektive und reale Überdeckungen; die App erfindet keine neue Kamera-Geometrie |
| L3 Depth Dynamics | Radiale statische Mikro-Kontraste für Approach/Retreat, tiefengekoppelte Stärke | Wahrnehmungshinweis, keine physische Bewegung |
| L4 Selective Motion | SceneObject mit Rolle, Richtung, Motion Identity, Tempo, Intensität; weiches lokales Feld | Allgemeine Objektgrenzen sind tiefengeführte Regionen; Personenmaske ist separat |
| L5 Viewing Geometry | Peripheriegewichtung, Distanz/Bildbreite, interaktiver Viewer mit drei Steuerachsen | Ein einzelnes Foto ist kein Stereo-Paar |
| OMM | Objekt → Tiefe → Rolle → Bewegungsidentität → Richtung → Tempo → Intensität | Automatische Erkennung ist grob; Rollen lassen sich korrigieren |
| PMG / Hierarchie | Hauptsignal plus gerichtetes sekundäres Mikrosignal | Keine automatisch rekonstruierte komplette semantische Kausalkette, z. B. neue Partikel hinter einem Fahrzeug |
| Depth Locking | Gesichts- und Ankerbereiche unterdrücken Bewegungsmuster; Tiefen und Originalüberdeckungen bleiben gemeinsam | Das 2.5D-Feld kennt keine verdeckten Oberflächen |
| Object-bound Fields | Gefiederte Bereichsgrenzen, Tiefe als zusätzliche Zugehörigkeit, keine globale Wellenverformung | Manuelle Bereiche haben weiche statt exakter semantischer Grenzen |

## Rendering und Oberfläche ab 0.2.0

Die Tiefenkarte bleibt kontinuierlich. Farbgewichtete bilineare Interpolation führt ihre Vergrößerung am Foto. Das Lichtrelief nutzt vorhandene Bilddetails statt Ableitungen des niedrig aufgelösten Tiefenmodells; dadurch entstehen keine erfundenen Schlagschatten an dessen Grenzen. Ein separabler lokaler Tiefpass ersetzt die grobe Verkleinerungs-/Vergrößerungsunschärfe. Die globale Atmosphäre folgt fotografierten Richtungsdetails und trägt keine periodische Welle mehr über Himmel und Gebäude. Objektbewegungen bleiben weich maskiert und auf vorhandene Struktur begrenzt.

Die Tiefenstärke reicht bis 2,5, Bewegungs- und Parallaxenstärke bis 2,0. Der statische Kontrastgewinn wächst überproportional; die interaktive Verschiebung wächst kontinuierlich bis zu einer begrenzten Auslenkung. Der Viewer interpoliert den Schnittpunkt im Tiefenfeld zwischen 8–128 Abtastebenen. Stiloperationen werden vor der Stilmischung ausgeführt, sodass 0 % jeden Stil vollständig deaktiviert.

CameraX-Preview und Aufnahme teilen einen ViewPort; die FILL_CENTER-Vorschau füllt den Bildschirm und die Aufnahme übernimmt denselben Ausschnitt. Bedienelemente liegen mit System-/Cutout-Abstand darüber. In der Bearbeitung bleibt das vollständige Foto seitenverhältnistreu. Original/PMDD ist direkt erreichbar, weitere Einstellungen liegen in den drei Menüs Looks, PMDD und Werkzeuge.

## PMDD Vivid und Auslöseton ab 0.3.0

`SceneTone` berechnet eine kantenbewusste Beleuchtungsbasis mit geführter Filterung auf maximal 512 Pixeln längster Kante. Die Koeffizienten werden mit der hochaufgelösten Bildluminanz rekonstruiert; ein robuster Schwarz-/Weißpunkt, selektive Schattenanhebung und eine begrenzte S-Kurve erhalten Tonwertabstufungen. Fotografierte Farben steuern die Blau-/Grünabstimmung. Vivid erzeugt keine Sonnenstrahlen, Blätter oder neuen Objekte. Seine gesamte Wirkung folgt dem Regler Stilmischung.

Der gemeinsame PMDD-Schritt begrenzt großflächigen Dunst unabhängig vom überproportionalen Tiefengewinn, reduziert Detailüberschwinger an harten Kanten und nutzt den verfügbaren Tonwertraum statt dunkle Werte linear abzuschneiden. Neue Rezepte verwenden 96 Abtastebenen, Tiefe 1,25, Bewegung 0,7 und Parallaxe 1,0. Gespeicherte Werte werden weiter gelesen; Rendering-Verbesserungen gelten beim erneuten Rendern auch für vorhandene Rezepte.

`ShutterSound` lädt und spielt `MediaActionSound.SHUTTER_CLICK` auf einem seriellen Hintergrund-Executor. Nur `ImageCapture.OnImageSavedCallback.onCaptureStarted()` löst den Klick aus. Die optionale SharedPreferences-Einstellung ist standardmäßig `false`. `CameraInfo.mustPlayShutterSound()` wird beim Vorladen und erneut beim Aufnahmebeginn abgefragt; verpflichtende Gerätevorgaben haben Vorrang. Schlägt die Richtlinienabfrage fehl, bleibt der Ton vorsichtshalber aktiv. Beim Schließen wird Audio nach ausstehenden Aufnahmen genau einmal freigegeben. Lautstärke und Systemrichtlinien werden nicht umgangen.

Offizielle Android-Schnittstellen: [CameraInfo.mustPlayShutterSound](https://developer.android.com/reference/androidx/camera/core/CameraInfo#mustPlayShutterSound()), [onCaptureStarted](https://developer.android.com/reference/androidx/camera/core/ImageCapture.OnImageSavedCallback#onCaptureStarted()), [MediaActionSound](https://developer.android.com/reference/android/media/MediaActionSound). Die Geräteabfrage ist eine technische Richtlinie, keine weltweite rechtliche Zertifizierung der App.

## Dateien pro Projekt

- `original.image`: originale JPEG-/PNG-/WebP-/HEIF-Bytes. Niemals Renderziel.
- `project.json`: versioniertes Rezept und Objektrollen; atomare Schreibvorgänge.
- `depth-base.bin`: Modell-Tiefenkarte als Ausgangspunkt.
- `depth.bin`: aktuell korrigiertes Tiefenfeld, Float32, Breite/Höhe und Formatkennung.
- `thumb.jpg`: erneuerbare Vorschau.

Das Original wird vor KI-Verarbeitung gespeichert. Unfertige Projekte bleiben in der Sammlung und können erneut geöffnet werden. Rotation/Activity-Neustart lädt das aktive Projekt erneut. Archive enthalten ausschließlich die bekannten Dateien, werden größenbegrenzt gelesen und erlauben keine Pfade außerhalb des Zielordners. Ein importiertes Archiv bekommt eine neue Projekt-ID.

## Modelle

MiDaS v2.1 Small ONNX, 256×256 RGB NCHW [0,1]. Die offizielle ONNX-Datei enthält bereits ImageNet-Normalisierung. Keine zweite Normalisierung in der App. Ausgaben werden robust über 2./98. Perzentil normalisiert; NaN/Inf und konstante Felder sind behandelt.

Modelldatei: https://github.com/isl-org/MiDaS/releases/download/v2_1/model-small.onnx

SHA-256: `2d8c6cb8f415229daf1eb041024208e2608c9f98e17c81cc7c6ecb449c56fd58`

Vorverarbeitung: https://github.com/isl-org/MiDaS/blob/master/tf/run_onnx.py

SSD-MobileNet V1-12 (ONNX Model Zoo, MIT) ist ebenfalls fest eingebaut: 300×300 uint8 RGB NHWC, 80 COCO-Klassen, bis zu 20 Bereiche ab 40 % Modellkonfidenz. Die Ausgabe enthält normierte Y/X-Rahmen. SHA-256: `b8fba5e404077d4048d27fcd1667e85e27e192eb9bf51e696c46a3acd7d21058`, Größe 29.461.455 Bytes. Anders als die dynamisch nachgeladene ML-Kit-Objekterkennung benötigt dieser Pfad keine Play-Services-Modulinstallation. Die gebündelten ML-Kit-Modelle ergänzen Gesichtsanker und eine Personenmaske. Fehlende Teilanalysen werden sichtbar gemeldet; eine fehlgeschlagene Tiefeninferenz wird nicht durch eine erfundene KI-Tiefenkarte ersetzt.

## Ressourcen und Datenschutz

Keine Cloud-Verarbeitung und keine Internetberechtigung. Fotos und Betrachterkameraframes werden nicht hochgeladen. Kopfsteuerung ist eine ausdrücklich gewählte Betriebsart und stoppt beim Wechsel der Ansicht und beim Verlassen der App. Native Modelle und Tensoren werden geschlossen. Verarbeitung läuft außerhalb des UI-Threads, Rendering prüft Abbruchsignale, Vorschaugröße ist begrenzt. Exporte skalieren abhängig vom App-Heap. Das ist keine Zusage einer bestimmten Renderzeit auf jedem Gerät.

## Bewusste Grenzen

Die App erzeugt lokale Stiltransformationen, keine generativen Neukompositionen. Eine wissenschaftlich validierte Garantie einer Bewegungsillusion ist nicht implementiert. Besonders glatte, unscharfe oder kontrastarme Motive können schwächer wirken. Verdeckte Objektseiten, große Perspektivänderungen und neue semantische Sekundärbewegungen benötigen zusätzliche Bilddaten oder ein generatives Modell. Diese Fähigkeiten werden in der Preview nicht behauptet.
