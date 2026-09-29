# PMDDcam

**Fotografiere einen tieferen Raum.** Native Android-Kamera mit PMDD 4.0, lokalen KI-Tiefenkarten, Objekterkennung und 60 Stilen.

[![Android Build](https://github.com/chekento/PMDDcam/actions/workflows/android.yml/badge.svg)](https://github.com/chekento/PMDDcam/actions/workflows/android.yml)

## [⬇ PMDDcam 0.2.0 · Android-APK herunterladen](https://github.com/chekento/PMDDcam/releases/download/v0.2.0/PMDDcam-0.2.0.apk)

Android 8.0 oder neuer · ARM64 / ARMv7 / x86_64 · persönliche Preview-Version. Der Download wird nach erfolgreichem Build, Unit-Tests, Lint und Emulatorprüfung veröffentlicht. [Release & Prüfsumme](https://github.com/chekento/PMDDcam/releases/tag/v0.2.0) · [Build-Artefakte](https://github.com/chekento/PMDDcam/actions).

**Neu in 0.2.0:** bildfüllende Kamera, drei kompakte Menüs und ein dauerhaft sichtbarer **Original ↔ PMDD**-Button. Kontinuierliche Tiefenverarbeitung ersetzt künstliche Tiefenkanten-Schatten; die Stilrezepte und der obere Regelbereich wurden überarbeitet.

## Vom Foto zum PMDD-Projekt

1. Foto mit Front- oder Rückkamera aufnehmen oder ein Bild importieren.
2. Das Original wird zuerst im privaten Projektspeicher gesichert.
3. Das gebündelte MiDaS-Modell schätzt räumliche Tiefe. Das ebenfalls gebündelte SSD-MobileNet erkennt Objektbereiche aus 80 Klassen. ML Kit ergänzt Gesichter und eine Personenmaske.
4. PMDDcam verarbeitet das Foto automatisch mit **64 Tiefenebenen und 85 % Tiefenstärke**.
5. Stil, Licht, Tiefe und lokale Bewegungsidentitäten nachträglich ändern. Das Original wird nie überschrieben.
6. Ein statisches PNG/JPEG oder ein vollständig bearbeitbares Projekt mit Original, Tiefenkarte und Rezept exportieren.

## Funktionen

| Bereich | Enthalten |
|---|---|
| Kamera | Bildfüllendes CameraX mit passendem Aufnahmeausschnitt, Front/Rückkamera, Fokus per Tippen, Pinch-Zoom, Blitz, Raster, 3-/10-Sekunden-Timer |
| Originalarchiv | Unveränderte Originaldatei, getrennte Rezepte und Tiefenkarten, Projektliste, Wiederaufnahme nach Abbruch |
| Tiefenmodell | MiDaS v2.1 Small direkt in der APK; relative Tiefe aus einem Einzelbild; kein API-Schlüssel |
| Ebenen | 8–128 Abtastebenen im Viewer, Standard 64; kontinuierliches Tiefenfeld, Tiefenstärke bis 250 %, Trennung, Fokus, Lichtrelief, Atmosphäre, Bokeh und Schattenzeichnung |
| Objektlogik | Anker / dynamisch / Atmosphäre, eigene Bewegungsrichtung, Intensität, Tempo und Tiefenposition je Bereich |
| Bewegungsidentitäten | Annäherung, Entfernung, Drift, Rotation, Fließen, Pulsieren; lokale statische Kontrastfolgen |
| Schutz | Stabile Anker und erkannte Gesichter vor Bewegungsmustern schützen |
| Manuelle Korrektur | Tiefenpinsel mit weicher Kante, Strich-Undo, Modelltiefe wiederherstellen; eigene Objektbereiche markieren |
| Stil | 60 lokale Stilrezepte in sechs Gruppen, mit echten Vorschauen und einstellbarer Stilmischung |
| Betrachtermodus | Touch, Pinch, Geräteneigung oder zuschaltbare Kopfsteuerung per Frontkamera; links/rechts, oben/unten, näher/weiter |
| Betrachtung | Achsen einzeln aktivieren, Parallaxenstärke, Bildbreite und Betrachtungsabstand einstellen, neu kalibrieren |
| Bearbeitung | Looks / PMDD / Werkzeuge mit lesbaren Untermenüs, direkter Original/PMDD-Umschalter, Undo/Redo, Aufnahme-Vorgaben, natürliches und intensives Preset |
| Export | PNG, JPEG, Original, Tiefenkarte, ZIP-Projekt; Projektimport; Android-Teilen-Menü |
| Verarbeitung | Lokal auf dem Gerät, keine Foto-Uploads, kein Cloudkonto, keine App-Internetberechtigung |

Die Sammlung liegt im privaten App-Speicher und bleibt bei Neustarts und kompatiblen Updates erhalten. Für ein Backup außerhalb der App **„Bearbeitbares PMDD-Projekt“** exportieren; das enthält auch das Original. Eine Deinstallation entfernt privaten App-Speicher.

## Die 60 Stile

| Gruppe | Stile |
|---|---|
| Foto | PMDD Natural, Cinematic, Soft Portrait, Alpine Clarity, Golden Hour, Blue Hour, Film Noir, Silver Gelatin, Matte Editorial, Chrome Color |
| Illustration | Comic Classic, Manga Ink, Anime Cel, Graphic Novel, Pop Art, Ligne Claire, Pastel Cel, Superhero, Storybook, Riso Comic |
| Atelier | Wasserfarben, Aquarell Warm, Gouache, Ölgemälde, Impression, Tusche, Bleistift, Buntstift, Kohle, Kupferstich |
| Retro | Retro 70s, Retro 80s, Instant Film, Sepia, Cyanotypie, VHS Print, 8-Bit Arcade, 16-Bit Adventure, Pocket Green, Newspaper |
| Zukunft | Futuretech, Cyberpunk, Neon Tokyo, Holographic, Synthwave, Blueprint, Infrared Dream, Matrix Green, Deep Space, Liquid Metal |
| Atmosphäre | Dream Pastel, Nordic Mist, Desert Sand, Emerald Forest, Sakura Bloom, Underwater, Lava Light, Arctic Ice, Moonlight, Velvet Dusk |

Die Stile verändern Pixel mit lokalen Bildverfahren (u. a. Kantenzeichnung, Quantisierung, Pigment-/Papiertextur, Raster, Neon, Duoton). Sie erzeugen keine neuen semantischen Objekte wie ein generatives Bildmodell. Alle Stile durchlaufen anschließend dieselbe PMDD-Verarbeitung.

## PMDD und Betrachterbewegung

**PMDD-Foto:** Ein statisches Einzelbild mit Tiefenstaffelung, lokaler Kontrastarchitektur, ruhigen Ankern, atmosphärischer Trennung und objektgebundenen Bewegungshinweisen. Blickwechsel können subjektive Bewegung verstärken. Die Wirkung ist motiv-, display- und betrachterabhängig und nicht bei jedem Foto gleich stark.

**Interaktive Ansicht:** Eine zusätzliche, tatsächlich auf Bewegung reagierende 2.5D-Ansicht in der App. Die Frontkamera erfasst Position und relative Größe des Gesichts, ohne Aufnahmen zu speichern. Das Neigen des Geräts steuert zwei Achsen; näher/weiter lässt sich dort per Pinch steuern. Nur die Kopfsteuerung reagiert auf Kopfbewegung vor einem ruhenden Gerät. PNG/JPEG enthalten diese Interaktion nicht.

Ein einzelnes Foto liefert keine echten verdeckten Rückseiten oder metrisch gemessene 3D-Geometrie. MiDaS schätzt relative Tiefe. Große Blickwinkel können Kanten dehnen; die Bewegung ist deshalb begrenzt. Die Ebenenzahl beschreibt die Abtastung im interaktiven Viewer, nicht 64 automatisch ausgeschnittene semantische Objekte. SSD-MobileNet erkennt bis zu 20 Objektbereiche aus 80 COCO-Klassen zusätzlich zu Gesichtern; weitere Bereiche können manuell ergänzt werden. Die allgemeinen Objektfelder sind weich und tiefengeführt, keine universell pixelgenaue Segmentierung.

## PMDD-Herkunft

**PMDD — Perceptual Motion & Depth Design** ist das von **Kolja Werner Schumann (KoSch)** entwickelte Gestaltungsframework. Die Systematisierung entstand im Human-AI-Co-Design mit ChatGPT. PMDDcam übersetzt seine Tiefen- und Bewegungsprinzipien in eine lokale Fotoverarbeitung und ergänzt einen interaktiven Viewer.

[Die Geschichte und Wahrnehmungsarchitektur von PMDD](https://kosch.cloud/blog/pmdd---die-magie-hinter-der-illusion--wie-wahrnehmung-und-ki-zu-lebendigen-bildern-verschmelzen) · [kosch.cloud](https://kosch.cloud) · [Technische Umsetzung](docs/ARCHITECTURE.md) · [Changelog](CHANGELOG.md)

## Selbst bauen

JDK 17, Android SDK 35, Build Tools 35.0.0. Der Gradle-Wrapper lädt Gradle 8.9 mit Prüfsumme.

```sh
bash scripts/fetch-model.sh
./gradlew testDebugUnitTest lintDebug assembleRelease
```

Beide ONNX-Modelle werden nur **beim Build** aus den offiziellen MiDaS- bzw. ONNX-Model-Zoo-Veröffentlichungen geladen und SHA-256-geprüft. Auf dem Smartphone werden sie aus der APK verwendet. APK: `app/build/outputs/apk/release/app-release.apk`.

Die Preview verwendet einen absichtlich öffentlichen Entwicklungsschlüssel in `signing/`, damit Updates derselben Preview installierbar bleiben. Dieser Schlüssel ist keine private Produktionssignatur. Für Play Store/Produktion eine eigene Signing-Konfiguration nutzen.

## Prüfungen und Grenzen der Preview

Die CI prüft Rezepte, Grenzwerte, konstante/ungültige Tiefenwerte, 60 unterschiedliche Stilresultate, vollständig abschaltbare Stilmischung, glatte Flächen ohne künstliche Schatten/Wellen, wirksame hohe Reglerwerte, deterministisches Rendering, unveränderte Originalbytes, Archive, lokalen Modelllauf einschließlich bekannter Hunde-Erkennungen, echte CameraX-Aufnahme über die virtuelle Kamera, Vollbild-Aufnahmeausschnitt, Original-Umschalter, Pop-up-Menüs, Stilwechsel und Activity-Neustart auf einem Android-35-Emulator im Flugmodus (WLAN und mobile Daten aus). Reale Kameraqualität, Latenz und die subjektive PMDD-Wirkung müssen zusätzlich auf echten Geräten und mit unterschiedlichen Fotos beurteilt werden. Export bis 4096 Pixel längste Kante; bei kleinem App-Heap maximal 2048 Pixel. Originale behalten ihre ursprünglichen Bytes und Auflösung.

MiDaS, SSD-MobileNet-Modell und ONNX Runtime: MIT. AndroidX/Kotlin: jeweilige Apache-2.0-Lizenzen. ML Kit: Google-Bedingungen. Hinweise in [THIRD_PARTY.txt](app/src/main/assets/THIRD_PARTY.txt). Für den eigenen App-Quellcode wurde keine zusätzliche Open-Source-Lizenz festgelegt.
