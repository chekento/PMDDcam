# Datenschutz, KI- & Drittanbietertransparenz — PMDDcam

**Stand:** 1. Oktober 2026  
**App:** PMDDcam 0.4.0 Preview (`cloud.kosch.pmddcam`)  
**Projekt:** https://github.com/chekento/PMDDcam  
**Kontakt / Anbieterinformationen:** https://kosch.cloud

Diese Seite dokumentiert die Datenflüsse, KI-/ML-Funktionen, Drittanbieter-Komponenten und die Signierung der aktuellen PMDDcam-Preview. Sie beschreibt den technischen Stand des Repositorys und ersetzt keine individuelle Rechtsberatung.

## 1. Kurzfassung

Die native Android-App ist auf lokale Bildverarbeitung ausgelegt.

- Kein Benutzerkonto.
- Keine Werbung.
- Kein Analytics- oder Tracking-SDK.
- Keine Foto-Uploads für die normale Android-Bildverarbeitung.
- Die Android-App entfernt die Berechtigungen `INTERNET` und `ACCESS_NETWORK_STATE` aus dem Manifest.
- Die Kamera wird nur für Aufnahmefunktionen und – bei aktivierter optionaler Kopfsteuerung – für die relative Betrachterposition verwendet.
- Originalfoto, Tiefenkarte, PMDD-Rezept und Projekte verbleiben im privaten App-Speicher, bis der Nutzer exportiert oder löscht.

## 2. Android-Berechtigungen

Die aktuelle Android-App fordert:

- `CAMERA` — Aufnahme von Fotos und optional kamerabasierte Betrachter-/Kopfsteuerung.

Die App fordert **keine Internetberechtigung** für die native Fotoverarbeitung. Netzwerkzugriffe der separat angebotenen WebApp laufen dagegen im Browser und unterliegen dessen Berechtigungs- und Netzwerkmodell.

## 3. KI / Machine Learning — was läuft tatsächlich?

PMDDcam verwendet KI/ML als **lokale Analysehilfe**. Es handelt sich nicht um einen generativen Chatbot und nicht um einen Dienst, der Bilder an ChatGPT oder OpenAI sendet.

| Komponente | Aufgabe in PMDDcam | Datenfluss |
|---|---|---|
| **MiDaS v2.1 Small** | Schätzt aus einem Einzelbild eine **relative Tiefenkarte**. Sie dient der PMDD-Tiefenstaffelung und dem 2.5D-Viewer. | Inferenz lokal auf dem Gerät; keine metrische 3D-Vermessung. |
| **SSD-MobileNet** | Unterstützt die Erkennung von Objekt-/Bereichsklassen, damit Bildbereiche als statisch, dynamisch oder atmosphärisch behandelt werden können. | Lokale Inferenz. |
| **Google ML Kit — Selfie Segmentation** | Unterstützt die lokale Trennung relevanter Vordergrund-/Personenbereiche. | Lokale App-Verarbeitung; kein PMDDcam-Cloudkonto. |
| **Google ML Kit — Face Detection** | Unterstützt lokale Gesichts-/Betrachterlogik. Es dient nicht zur Identitätsfeststellung und erzeugt kein Personenprofil. | Lokale Verarbeitung. |
| **Depth Anything V2 (WebApp, optional)** | Kann im Browser eine Tiefenkarte für den Web-Viewer erzeugen. | Modell kann bei Aktivierung über das Netz geladen werden; Inferenz danach im Browser. |
| **MediaPipe Face Landmarker (WebApp, optional)** | Liefert bei aktivierter Head-Tracking-Funktion relative Gesichts-/Kopfpositionen zur Perspektivsteuerung. | Verarbeitung im Browser; keine Gesichtserkennung zur Identität. |

### Was die KI ausdrücklich nicht macht

- Sie identifiziert keine Person namentlich.
- Sie bewertet keine Gesundheit, Emotion oder Persönlichkeit.
- Sie trainiert nicht auf den aufgenommenen Fotos.
- Sie erzeugt in der Android-App keine neuen semantischen Bildobjekte.
- Sie sendet Fotos nicht an ChatGPT/OpenAI.
- MiDaS liefert relative Tiefe, keine verlässliche reale Entfernung in Metern.

## 4. Kamera- und Bilddaten

Ein aufgenommenes oder importiertes Foto wird für PMDD-Effekte, Tiefenschätzung und optionale Objektanalyse verarbeitet. Das Original kann im privaten Projektspeicher erhalten bleiben, damit Bearbeitungen nicht-destruktiv rückgängig gemacht werden können.

Exporte wie PNG, JPEG, Tiefenkarte oder ZIP-Projekt werden erst nach Nutzeraktion an einen gewählten Speicherort geschrieben. Ab diesem Zeitpunkt gelten zusätzlich die Datenschutz- und Synchronisationseinstellungen des gewählten Dateispeichers oder Cloud-Anbieters.

## 5. Kopfsteuerung / Frontkamera

Bei aktivierter Kopfsteuerung dient das Kamerabild dazu, die **relative Position des Betrachters** für die Perspektivverschiebung zu bestimmen. PMDDcam ist nicht als biometrisches Identifikationssystem ausgelegt. Die Kameraaufnahme wird für diese Funktion nicht als Fotoarchiv gespeichert oder an einen PMDDcam-Server übertragen.

## 6. Drittanbieter-Komponenten und Tools

### Laufzeit in der Android-App

- **AndroidX CameraX** — Kamerazugriff und Preview.
- **AndroidX Core / Activity / Lifecycle / ExifInterface** — Android-Grundfunktionen.
- **Microsoft ONNX Runtime for Android** — lokale Ausführung von ONNX-Modellen.
- **Google ML Kit** — lokale Selfie-Segmentierung und Gesichtserkennung.
- **Kotlin Coroutines** — asynchrone lokale Verarbeitung.

### WebApp

- **WebGL / Browser APIs** — 2.5D-Darstellung.
- **IndexedDB** — lokale Browser-Projekteinstellungen und Daten.
- **Depth Anything V2** — optionale Tiefenschätzung.
- **MediaPipe Face Landmarker** — optionales Head Tracking.
- **PWA / Service Worker** — lokale Web-App-Funktionalität und Caching.

### Entwicklung und Distribution

- **GitHub / GitHub Releases / GitHub Actions** — Quellcode, CI und APK-Verteilung.
- **Gradle, Android SDK, JDK/Kotlin** — Build-Werkzeuge.
- **raw.githack.com** — öffentliche Browser-Auslieferung der optionalen WebApp-Preview.

Drittanbieter-Komponenten bleiben unter ihren jeweiligen Lizenzen und Nutzungsbedingungen. PMDDcam erhebt keinen Anspruch darauf, diese Drittprojekte zu besitzen oder zu zertifizieren.

## 7. Externe Verbindungen

Die **native Android-App** benötigt für die Fotoverarbeitung keine Internetverbindung.

Die **WebApp** kann beim erstmaligen Einsatz bestimmter ML-Funktionen externe Modell- oder Bibliotheksressourcen laden. Dabei können die jeweiligen Hosting-Anbieter technisch notwendige Verbindungsdaten wie IP-Adresse, Zeitpunkt, User-Agent und angeforderte Ressource verarbeiten. Die eigentliche Bildinferenz soll danach im Browser erfolgen.

Beim Öffnen von GitHub-, kosch.cloud- oder anderen externen Links gelten die Datenschutzbedingungen der jeweiligen Website.

## 8. Lokale Speicherung und Löschung

Je nach Nutzung können lokal gespeichert werden:

- Originalbilder innerhalb eines Projekts,
- Tiefenkarten,
- PMDD-Rezepte und Style-Einstellungen,
- manuelle Tiefen-/Objektmasken,
- Projektmetadaten.

Lokale App-Daten können über die PMDDcam-Funktionen, Androids App-Speicherverwaltung oder durch Deinstallation entfernt werden. Exportierte Dateien müssen am gewählten Speicherort separat gelöscht werden.

## 9. Zertifikate, Signierung und Prüfsummen

Die aktuell direkt über GitHub verteilte PMDDcam-Preview verwendet eine **öffentliche Entwicklungs-/Preview-Signieridentität**. Diese dient der technischen APK-Signierung und Update-Konsistenz; sie ist **kein Play-Store-Produktionszertifikat und keine unabhängige Sicherheits- oder Datenschutz-Zertifizierung**.

Für Releases werden zusätzlich SHA-256-Prüfsummen veröffentlicht. Eine Prüfsumme belegt Dateiintegrität gegenüber dem veröffentlichten Hash, stellt aber ebenfalls keine externe Zertifizierung der App dar.

PMDDcam installiert keine eigene Root-CA und verlangt keine Benutzerzertifikate.

## 10. Keine Fremdwerbung, kein Analytics-Profil

Die aktuelle Preview enthält keine Werbe-SDKs, keine Entwickler-Analytics-Plattform und kein Benutzerkonto. Es wird kein serverseitiges PMDDcam-Nutzungsprofil geführt.

## 11. Änderungen

Wenn zukünftige Versionen Cloud-Synchronisation, Online-Modelle, Analytics, Accounts, neue Berechtigungen oder andere Datenflüsse einführen, muss diese Datei vor der Veröffentlichung entsprechend aktualisiert werden.

---

**Repository:** https://github.com/chekento/PMDDcam  
**Anbieter / Kontakt:** https://kosch.cloud
