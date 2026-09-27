# AreaMap map and hiking UI checks

The map menu reserves space below compact search. Expanded search, a selected
place and route planning own their controls instead of sharing the menu overlay.
The destination button is first in the place actions. New routes from a place use
the walking router; users can change the mode in the preview.

## Checks performed without an Android SDK

- Parsed changed Java source with the JDK compiler parser (syntax only).
- Parsed changed resource XML and checked added local resource references.
- Checked explicit AreaMap style parents and matching EN/RU format arguments.
- Ran the standalone GPX geometry checks:

```sh
mkdir -p /tmp/areamap-gpx-check
java com.sun.tools.javac.Main -d /tmp/areamap-gpx-check \
  android/app/src/main/java/app/organicmaps/safety/GpxTrack.java \
  tools/tests/AreaMapGpxGeometryCheck.java
java -cp /tmp/areamap-gpx-check app.organicmaps.safety.AreaMapGpxGeometryCheck
```

The geometry checks cover projection, distance, ascent, endpoint clamping,
crossings, the date line, missing elevations and invalid input.

## Required device checks

The full Gradle build and these device checks have not been run in the editing
environment. Gradle distribution download is unavailable there.

1. Build `fdroidDebug`; open the app in light and dark themes.
2. Search for Furmanov, select a result and tap Route in the place card.
3. Long press a location and use the same Route action.
4. Expand/collapse route preview by dragging and tapping its handle. The map
   remains accessible before Start. Open route details without losing the route.
5. Repeat on a small screen, in landscape, with increased font size and keyboard.
6. Read/search guides, open an article, import a GPX and open the SOS card.
7. Import a continuous GPX, choose Follow GPX track, and check remaining distance,
   ETA, off-track distance, GPS loss/recovery, rotation and Stop.
8. Start ordinary routing during GPX guidance; only ordinary guidance remains.

## GPX guidance scope

This follows the original geometry without road snapping. It is foreground-only,
with no turn instructions, automatic rerouting or background service. The
in-memory session survives rotation but does not survive process termination.
Reimport the file to restart. Start and finish follow file order.

Only a single continuous GPX track/route with 2–20,000 points is accepted for
guidance. Other files keep the existing map-import path. Remaining time is a
hiking estimate, then adapts to observed progress; missing elevations reduce its
accuracy. Locations older than two minutes or with accuracy worse than 50 m are
not used for guidance. At intersecting segments, projection prefers continuity.
