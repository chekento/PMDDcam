# PMDDcam 0.1.0 – Umsetzung

## Datenfluss

CameraX / System-Dateiauswahl → unveränderte Originaldatei → korrekt ausgerichtete Arbeitskopie → MiDaS → Objekt-/Gesichtserkennung → Tiefenfeld → Stil → statisches PMDD → Vorschau / Export.

Die interaktive Ansicht liest das fertige Bild und dasselbe effektive Tiefenfeld. Es gibt keinen Zeitparameter im Shader. Nur die Betrachterposition verändert die Ansicht. Damit sind statische Bewegungsillusion und echte interaktive Verschiebung klar getrennt.

## Zuordnung des PMDD-Frameworks

| PMDD-Prinzip | Implementierung | Grenze |
|---|---|---|
| L1/L2 Depth Foundation | Relative KI-Tiefe, 8–128 Stufen, Kontrasthierarchie, Atmosphäre, Lichtrelief, Schärfe, Hintergrundweichzeichnung | Die Aufnahme liefert Perspektive und reale Überdeckungen; die App erfindet keine neue Kamera-Geometrie |
| L3 Depth Dynamics | Radiale statische Mikro-Kontraste für Approach/Retreat, tiefengekoppelte Stärke | Wahrnehmungshinweis, keine physische Bewegung |
| L4 Selective Motion | SceneObject mit Rolle, Richtung, Motion Identity, Tempo, Intensität; weiches lokales Feld | Allgemeine Objektgrenzen sind tiefengeführte Regionen; Personenmaske ist separat |
| L5 Viewing Geometry | Peripheriegewichtung, Distanz/Bildbreite, interaktiver Viewer mit drei Steuerachsen | Ein einzelnes Foto ist kein Stereo-Paar |
| OMM | Objekt → Tiefe → Rolle → Bewegungsidentität → Richtung → Tempo → Intensität | Automatische Erkennung ist grob; Rollen lassen sich korrigieren |
| PMG / Hierarchie | Hauptsignal plus gerichtetes sekundäres Mikrosignal | Keine automatisch rekonstruierte komplette semantische Kausalkette, z. B. neue Partikel hinter einem Fahrzeug |
| Depth Locking | Gesichts- und Ankerbereiche unterdrücken Bewegungsmuster; Tiefen und Originalüberdeckungen bleiben gemeinsam | Das 2.5D-Feld kennt keine verdeckten Oberflächen |
| Object-bound Fields | Gefiederte Bereichsgrenzen, Tiefe als zusätzliche Zugehörigkeit, keine globale Wellenverformung | Manuelle Bereiche haben weiche statt exakter semantischer Grenzen |

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

SHA-256: `f319b72b1d6fc28097b1d6474a467d76aa44c156af283b752eb9c325a88a8af1`

Vorverarbeitung: https://github.com/isl-org/MiDaS/blob/master/tf/run_onnx.py

Die gebündelten ML-Kit-Modelle ergänzen allgemeine Objektbereiche, Gesichtsanker und eine Personenmaske. Fehlende Teilanalysen werden sichtbar gemeldet; eine fehlgeschlagene Tiefeninferenz wird nicht durch eine erfundene KI-Tiefenkarte ersetzt.

## Ressourcen und Datenschutz

Keine Cloud-Verarbeitung und keine Internetberechtigung. Fotos und Betrachterkameraframes werden nicht hochgeladen. Kopfsteuerung ist eine ausdrücklich gewählte Betriebsart und stoppt beim Wechsel der Ansicht und beim Verlassen der App. Native Modelle und Tensoren werden geschlossen. Verarbeitung läuft außerhalb des UI-Threads, Rendering prüft Abbruchsignale, Vorschaugröße ist begrenzt. Exporte skalieren abhängig vom App-Heap. Das ist keine Zusage einer bestimmten Renderzeit auf jedem Gerät.

## Bewusste Grenzen

Die App erzeugt lokale Stiltransformationen, keine generativen Neukompositionen. Eine wissenschaftlich validierte Garantie einer Bewegungsillusion ist nicht implementiert. Besonders glatte, unscharfe oder kontrastarme Motive können schwächer wirken. Verdeckte Objektseiten, große Perspektivänderungen und neue semantische Sekundärbewegungen benötigen zusätzliche Bilddaten oder ein generatives Modell. Diese Fähigkeiten werden in der Preview nicht behauptet.
