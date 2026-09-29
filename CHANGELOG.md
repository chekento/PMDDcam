# Änderungen

## 0.2.0 · 29.09.2026 · Fullscreen & Clean Depth

- Vollbildkamera mit abgestimmtem Vorschau-/Aufnahmeausschnitt, Pinch-Zoom und kompakten Symbolmenüs.
- Bildbetonte Bearbeitungsansicht mit drei Menüs: Looks, PMDD und Werkzeuge; lesbare Pop-ups und kompakte Dialoge.
- **Original ↔ PMDD** bleibt als eigener Umschalter auf dem Hauptschirm; aktueller Zustand wird markiert.
- Kontinuierliche Tiefenwerte und bildgeführte Interpolation ersetzen Stufen-/Reliefschatten an Tiefenkanten. Keine großflächigen periodischen Atmosphärenmuster; Licht und Bewegung verwenden vorhandene Bildstruktur.
- Tiefe bis 250 %, Bewegung und interaktive Parallaxe bis 200 %; stärkeres Intensiv-Preset und interpolierte Viewer-Abtastung.
- 60 FX-Rezepte überarbeitet: weichere Illustration, Pigment-/Papierdetails, differenzierte Filmtönung, Duotonpaletten, Neon, Pixel- und Druckstile. Stilmischung 0 schaltet nun auch Pixeleffekte vollständig ab.
- Regressionen für Muster auf glatten Flächen, Reglerwirkung, Stilmischung und kontinuierliche Tiefe; Gerätetests für Vollbild, Vergleichsbutton, Pop-ups und Originalerhalt.

## 0.1.2 · 29.09.2026 · Android Preview

- Dynamisch geladene Objekterkennung durch fest eingebautes SSD-MobileNet mit 80 Klassen ersetzen.
- Offline-Initialisierung aller Modelle und tatsächliche Hunde-Erkennung mit Referenzfoto absichern.
- Emulatortests im Flugmodus mit ausgeschaltetem WLAN und mobilen Daten ausführen.
- Test-APKs vor dem Emulatorstart kompilieren; einen blockierenden Quickstep-Dialog gezielt behandeln und vollständige Gerätediagnosen sichern. Kamera-, Offline- und Originalerhalt-Prüfungen bleiben verpflichtend.

## 0.1.1 · 29.09.2026 · Android Preview

- Kameraaufnahme im Emulator bis zum automatisch gespeicherten PMDD-Projekt prüfen.
- Stilwechsel und Erhalt der Originalbytes über Activity-Neustarts prüfen.
- Zugängliche Beschriftung des Auslösers für Screenreader.
- Screenshots der vollständigen Aufnahme- und Bearbeitungsoberfläche in der CI sichern.

## 0.1.0 · 29.09.2026 · Android Preview

- Erstimplementierung im zuvor leeren Repository.
- Native Kamera mit automatischer PMDD-Verarbeitung nach der Originalspeicherung.
- Gebündelte lokale Tiefen-, Objekt- und Gesichtsanalyse.
- Hohe Standards: 64 Ebenen, 85 % Tiefe; bis zu 128 Tiefenstufen.
- 60 unterschiedliche lokale Stilrezepte, alle mit PMDD-Nachverarbeitung.
- Getrennte Objekte, stabile Anker, sechs Bewegungsidentitäten und manuelle Tiefenkorrektur.
- Touch-, Neigungs- und Kopfsteuerung der interaktiven 2.5D-Ansicht.
- Bearbeitbare Projekte mit Originalen, Undo/Redo, Sicherung und Wiederherstellung.
- Statische PNG-/JPEG-Ausgabe, Original- und Tiefenkartenexport.
- CI-Build, Unit-Tests, Lint und Modell-/Lifecycle-Prüfungen im Emulator.
