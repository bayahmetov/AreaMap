# AreaMap integration branch

`feature/areamap-unified` combines the complete UI on
`feature/areamap-redesign` (`1d5085c9249f`) with
`feature/areamap-functional-stabilization` (`d844ab95222d`). Both source
branches remain separate. This integration is not merged into either source
branch or the default branch.

## Included behavior

- HOME recommendations, licensed photographs, favorites and offline-map status.
- Redesigned Guides catalog, stable article IDs and route context.
- AreaMap Profile, separate from the OpenStreetMap account.
- Pedestrian Route Preview: route statistics, elevation chart, checkpoints,
  route fitting, offline-map status and forecast/cache state.
- Persisted hike and GPX state, foreground monitoring notification, explicit
  completion, and protection against registering an already active hike again.
- Automatic Telegram reports through a persistent outbox and WorkManager network
  constraints. Offline messages retain their original body and event ID until a
  delivery attempt succeeds. A configured bot key and recipient access are
  required. A successful queue save is not reported as successful delivery.

## Integration fixes

The map activity owns one bottom navigation bar with Search, Route, Trip,
Guides and Profile. HOME and Route Preview share the HOME search header and
map controls. Route Preview owns its sheet, forecast card and map viewport;
HOME recommendations are hidden during planning. The global dock reserves its
height once, including system insets. Covering search, place selection, point
selection, fullscreen and navigation hide the dock.

The Route tab in a preview fits the current route instead of starting a new
one. The earlier duplicate GPX banner implementation and search-feed click
handlers were removed; persisted GPX recovery and the new banner remain.

## Validation on 2026-10-03

- 19 host JVM tests passed: HOME visibility/geometry, Profile model, Guides
  catalog, report persistence/retry/deduplication, GPX parsing and preview bounds.
  Android resource boundaries were stubbed for pure model tests; these are not
  instrumented Android tests.
- Changed Java sources parsed with the Java 17 compiler.
- 713 application resource XML files parsed; changed Java resource references
  resolved against checked-out Android module resources.
- Changed Java/C++ sources formatted with clang-format 23; `git diff --check`
  passed.
- `./gradlew app:assembleFdroidDebug -Parm64` could not start the build because
  downloading Gradle 9.6.0 failed with `Network is unreachable`. APK, Android
  lint, native tests and device acceptance tests remain unverified.

## Device acceptance before release

1. Open HOME, Guides and Profile; verify photographs, favorites, article links,
   map controls and navigation in both orientations and themes.
2. Build a walking route. Verify shared search/header controls, preview chart,
   checkpoint markers and Route-tab fitting. Search/place selection must hide
   the preview controls; dismissing the overlay must restore them.
3. Register a hike, leave the activity and swipe away the task. Verify the
   monitoring notification and resume the same hike without another start report.
4. Import GPX, begin the hike, restart the app and verify restored geometry and
   progress. Complete the hike explicitly and verify monitoring stops.
5. With a test bot/recipient configured, enable airplane mode, press SOS and
   verify pending status. Restore connectivity and verify bot delivery/status.
   Repeat across app restart; reopening must not generate another start report.
6. Test denied notification/location permissions and an invalid bot key. The
   app must preserve hike data and distinguish pending/blocked from delivered.

For rollback, switch back to the original redesign branch or its commit
`1d5085c9249f95d2cf445256801cafe8218a52fc`; no source branch reset is needed.
