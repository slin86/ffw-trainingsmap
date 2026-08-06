Lies AGENTS.md. Deliverable dieser Session: Bugfixes an den bestehenden
Controllern, description-Feld ueberall, Formular-Auslagerung +
Editierbarkeit fuer Station und Incident - alles in einem Rutsch, inkl.
Tests. Reihenfolge unten einhalten (Datenschicht/Bugfixes zuerst, dann
UI), damit nichts doppelt gebaut wird.

## 1. Bugfixes an bestehenden Controllern

- StationController: fehlenden Update-Endpoint ergaenzen -
  PUT /api/stations/{id}, analog zu IncidentController.update()
  (Name/lat/lng aktualisieren, @PreAuthorize ADMIN-only, 404 bei
  unbekannter ID).
- StationController.findAllWithVehicles(): Methode macht aktuell nur ein
  einfaches findAll() ohne Fetch-Join, obwohl der Name etwas anderes
  suggeriert. Zu `findAll()` umbenennen und den irrefuehrenden Namen
  entfernen, es sei denn Vehicles werden aktuell schon irgendwo aus der
  Station-Response gelesen - das vorher pruefen, dann ggf. echten
  Fetch-Join ergaenzen statt umzubenennen.
- Unbenutzte Imports (Incident, Location) aus StationController.java
  entfernen - IntelliJ "Optimize Imports" (Ctrl+Alt+O) verwenden statt
  manuell.
- Fehlermeldungen vereinheitlichen: IncidentController.update() wirft
  "incident not found" (klein), StationController "Station not found"
  (gross) - auf Grossschreibung wie in StationController bringen.

## 2. Description-Feld (Datenschicht + Backend)

- Flyway-Migration Vx: nullable Spalte `description` (TEXT) auf Tabelle
  `location` (gilt fuer beide Subtypen, Single-Table-Inheritance).
- Location.java: Feld `protected String description;` + Getter/Setter
  in der Basisklasse.
- LocationRequest-DTO: description-Feld ergaenzen (optional/nullable).
- StationController: description in create() und dem neuen PUT /{id}
  aus Abschnitt 1 uebernehmen.
- IncidentController: description in create() und update() uebernehmen.

## 3. Formulare auslagern + Editierbarkeit (Admin-UI)

- Neues Template admin/station-form.html: Felder Name, Description
  (<textarea>), Leaflet-Positionskarte (Klick + Drag). Schaltet per
  Model-Attribut zwischen Anlegen- und Bearbeiten-Modus um (Attribut
  `station` null vs. befuellt, `formAction` je nach Modus - bei Anlegen
  POST /admin/stations, bei Bearbeiten POST /admin/stations/{id}). Im
  Edit-Modus ist description mit vorhandenem Wert vorbefuellt.
- Neues Template admin/incident-form.html: analog, zusaetzlich ein
  Aktiv/Inaktiv-Checkbox-Feld (nur im Edit-Modus relevant - bei
  Neu-Anlage immer aktiv, kein Feld noetig).
- AdminStationController erweitern:
    - GET /admin/stations/new -> station-form.html im Anlegen-Modus.
    - GET /admin/stations/{id}/edit -> station-form.html im Edit-Modus,
      Positions-Marker startet an vorhandenen Koordinaten, 404 bei
      unbekannter ID.
    - POST /admin/stations (create) und POST /admin/stations/{id}
      (update): name, lat, lng, description entgegennehmen/aktualisieren,
      Redirect zu /admin/stations mit Flash-Message.
- AdminIncidentController erweitern:
    - GET /admin/incidents/new -> incident-form.html im Anlegen-Modus.
    - GET /admin/incidents/{id}/edit -> incident-form.html im Edit-Modus
      inkl. aktuellem active-Status, 404 bei unbekannter ID.
    - POST /admin/incidents (create) und POST /admin/incidents/{id}
      (update): name, lat, lng, description, active
      entgegennehmen/aktualisieren, Redirect mit Flash-Message.
- admin/stations.html: inline Create-Formular oben entfernen,
  stattdessen Button "Neue Feuerwache anlegen" -> /admin/stations/new.
  Pro Tabellenzeile einen "Bearbeiten"-Link -> /admin/stations/{id}/edit
  ergaenzen (neben Loeschen-Button). description NICHT als eigene Spalte
  anzeigen.
- admin/incidents.html: analog - Create-Formular raus, Button "Neuen
  Einsatzort anlegen" -> /admin/incidents/new, "Bearbeiten"-Link pro
  Zeile neben Toggle- und Loeschen-Button. description ebenfalls nicht
  als Tabellenspalte.

## 4. Tests

Vier @WebMvcTest-Klassen (orientiere dich an bestehendem Muster, z.B.
VehicleControllerTest.java, fuer Mocking/Security-Context ADMIN vs.
VIEWER):

- **StationControllerTest**: GET (Liste), GET /{id} (inkl. 404), POST
  (inkl. description, ADMIN-only, 403 fuer VIEWER), PUT /{id} (inkl.
  description, 404- und 403-Fall), DELETE (ADMIN-only).
- **IncidentControllerTest**: GET (Liste, ?all=true-Unterscheidung),
  GET /{id}, POST (inkl. description), PUT /{id} (inkl. description),
  PATCH /{id}/active (inkl. Fehlerfall fehlendes "active"-Feld), DELETE.
  Alle Schreibzugriffe ADMIN-only pruefen.
- **AdminStationControllerTest**: GET /admin/stations (Liste +
  Flash-Message), GET /admin/stations/new (200, leeres Formular), GET
  /admin/stations/{id}/edit (200 mit befuelltem Formular; 404 bei
  unbekannter ID), POST /admin/stations (create inkl. description), POST
  /admin/stations/{id} (update inkl. description, Redirect), POST
  /admin/stations/{id}/delete. 403/Redirect-to-Login fuer nicht-ADMIN
  auf allen schreibenden Routen.
- **AdminIncidentControllerTest**: analog inkl. GET .../new, GET
  .../{id}/edit, POST create/update (inkl. description und active),
  POST .../{id}/toggle, POST .../{id}/delete, Security-Faelle.

Nicht-Ziele: kein Typwechsel Station<->Incident, description ist
optional/nullable ohne Laengenbegrenzung, description nicht in den
Listen-Tabellen anzeigen.

## Abnahme

- `./gradlew test` gruen.
- `git diff --stat` zeigt: neue/geloeschte Migrations-Datei, Location.java
  geaendert, beide REST-Controller geaendert, beide Admin-Controller
  geaendert, zwei neue Form-Templates, beide Listen-Templates geaendert,
  UND alle vier Test-Klassen geaendert - falls eine Testklasse nicht
  angefasst wurde, ist die Session nicht abgeschlossen.
- curl POST/PUT auf /api/stations und /api/incidents mit description im
  Body -> Response enthaelt das Feld.
- Browser: /admin/stations zeigt nur noch Liste + "Neue Feuerwache
  anlegen"-Button, kein inline-Formular mehr. Anlegen unter
  /admin/stations/new funktioniert inkl. description und Kartenklick.
  Station danach bearbeiten (Name/Koordinaten/description aendern,
  Reload zeigt neue Werte).
- Browser: dasselbe fuer /admin/incidents inkl. aktiv/inaktiv-Aenderung
  im Edit-Formular.

Danach: git status pruefen, commit, AGENTS.md aktualisieren (damit ist
der urspruengliche Vier-Punkte-Plan komplett - "Noch offen" entsprechend
bereinigen).
