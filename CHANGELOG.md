# Änderungen

## 0.4.0 · 01.10.2026 · Lebendige Objekte & 80 Looks

- Eigenständige, laufende Objektanimation im interaktiven Betrachter: Stärke und Tempo separat regelbar, unterschiedliche Phasen und Geschwindigkeiten je Objekt, langsamere Atmosphäre. Bewegungsweg an Objektgröße begrenzt, feste Anker und geschützte Gesichter ausgespart. Bei Pause, Originalvergleich und statischer Ansicht stoppt die Animation.
- Statische Bewegungshinweise auch in der Bildmitte stärker erhalten. Die Wahrnehmungswirkung bleibt abhängig von Motiv und Betrachtung; zeitliche Animation ist nur im Viewer vorhanden.
- Comic/Cel mit geglätteten Farbflächen und klaren Konturen; Manga mit Grauraster. Aquarell mit Pigmentflächen und Papier, Öl mit größeren Farbfeldern und Pinselstruktur, Bleistift mit Tonwertschraffur.
- Neon mit räumlich auslaufendem Kantenschein; Druckstile mit farbigen Rasterplatten; Retro mit festen Pixelblöcken und Paletten. PMDD-Schärfung und Unschärfe mischen keine unpassenden Fotodetails mehr in stark stilisierte Flächen zurück.
- 19 zusätzliche Looks: Slide Film, Bleach Bypass, Dramatic B&W, Soft Bloom, Comic Noir, Flat Cel, Poster Pop, Linolschnitt, Sumi-e, Kreidepastell, Dry Brush, Pointillismus, CGA 4, C64 16, CRT Arcade, Thermal Vision, Neon Wire, Sunset Rose und Dream Bloom. Thermal ist ein Helligkeits-Falschfarbenlook, keine Temperaturmessung.
- Look-Karten erklären ihre Wirkung; Auswahl und Vorschau verwenden volle Stilmischung, anschließend bleibt die Stärke einstellbar. Alle bestehenden Stil-IDs und Projekte bleiben lesbar.
- Tests für Vierfarb-Paletten, Pixelblöcke, Malflächen, Neonränder, Animationsregler sowie GL-Prüfungen für Bewegung bei festem Blick, ruhige Anker, Abschalten und Pause/Fortsetzung.

## 0.3.1 · 30.09.2026 · Kleine Objekte

- Zusätzlich zum Gesamtbild bis zu sechs Detailausschnitte analysieren, damit kleine Fahrzeuge und Tiere auch in hochkant aufgenommenen Fotos erkannt werden können. Doppelte Treffer zusammenführen, weiterhin höchstens 20 Objektbereiche.
- Ein Modell und ein Eingabepuffer pro Analyse; Abbruch zwischen den Durchläufen möglich. Alle Modelle bleiben offline.
- Regressionen für Koordinatenrückrechnung und doppelte bzw. benachbarte Treffer.

## 0.3.0 · 30.09.2026 · PMDD Vivid

- Neuer Standardstil PMDD Vivid mit kantenbewusster Schattenaufhellung, kräftigeren Farben, geschützten Lichtern und lokaler Zeichnung. Die bisherigen 60 Stile bleiben verfügbar (61 insgesamt).
- Stark reduzierter Dunst, begrenzte Schärfungs-/Reliefüberschwinger und Tonwertkurve mit geschützten Endpunkten statt linearer Schwarz-/Weißbeschneidung.
- Neue Projekte starten mit 96 Ebenen, 125 % Tiefe, 70 % Bewegungshinweisen und 100 % Parallaxe. Vorhandene Rezepte und eigene Aufnahmevorgaben werden nicht überschrieben; die vollständige neue Abstimmung ist als Preset auswählbar.
- Bewegungsfähige Klassen erhalten passende Dynamikrollen statt pauschal Anker. Statische Bewegungshinweise folgen asymmetrischen lokalen Kontrastfolgen; flache und nicht betroffene Flächen erhalten keine Bewegungsmuster.
- Globales Atmosphärenfeld durch begrenzte, gespeicherte Wolken-/Nebelvorschläge ersetzt. Die fotografischen Vorschläge sind keine zuverlässige semantische Wettererkennung und bleiben editierbar.
- Head-Tracking: Kalibrierung über mehrere Messungen, Translation plus Kopfneigung, logarithmischer Abstand, sanftes Rückstellen bei Gesichtsverlust. Viewer mit objektgebundener Zusatzverschiebung; echte GL-Pixelprüfung für alle sechs Bewegungsrichtungen.
- Beschrifteter Button „3D ansehen“ neben Original ↔ PMDD; lange Pop-up-Menüs können scrollen.
- Optionaler Android-Systemauslöseton, Standard aus. CameraX-Gerätevorgaben haben Vorrang und werden beim Aufnahmebeginn erneut abgefragt. Audioausgabe läuft außerhalb des UI-Threads; Import, Bearbeitung und Export lösen keinen Klick aus.
- Regressionen für Tonwertabstufungen, Farbtreue, alte Rezepte, Tonrichtlinie und Audio-Lifecycle; Gerätetest für Auslöseton-Einstellung über Neustarts.
- Lokale Verarbeitung: keine generative Neukomposition und keine Foto-Uploads.

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
