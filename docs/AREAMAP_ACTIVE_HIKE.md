# Active hike runtime integration

The existing NavigationController / NavMenu sheet is the active-hike surface.
No second navigation screen, routing engine or weather service is introduced.

| Existing data/functionality | Current source | UI destination |
| --- | --- | --- |
| Native remaining distance, ETA, completion | RoutingInfo from GPS update | Existing sheet header and progress bar |
| GPX distance, ETA, deviation | GpxNavigation / GpxTrack | Same sheet, without turn instructions |
| Route name, checkpoint distances/ETA/elevation | TripPlan saved by TripSafety at registration | Header, next checkpoint, list and graph |
| Elevation profile | RouteAltitudeData / imported GPX elevations | Read-only profile with progress marker |
| Route-point hourly forecast and cache timestamp | TripWeatherRepository | Hourly strip and compact summary |
| Existing hazard classification | TripWeatherRepository / TripWeatherNotifier | Alert card |
| Planned timing and breaks | TripSafety / TripScheduleNotifier | Signed schedule drift and return ETA |
| OK, break, SOS, completion | Existing TripSafety / TripReportSender / activity callbacks | Safety and finish controls |
| Persistent monitoring | TripMonitoringService / NavigationService | Existing lifecycle, unchanged |
| Global tabs and system insets | MwmActivity | Dock below the existing sheet |

The forecast remains for the existing destination/highest-route-point target.
Its location is explicitly labelled; a single-point cache is not presented as
current-position or next-checkpoint weather. GPX without imported checkpoint
metadata shows no checkpoints instead of generating another collection.

## UI and ownership

The existing sheet has a compact hike header and scrollable expanded details:
remaining distance/time/arrival, measured altitude/speed/pace, ascent, timing drift,
next checkpoint and the saved checkpoint list, hourly weather/cache age/hazard,
read-only elevation profile, and existing safety/finish actions. Unknown values
remain unavailable. GPS, preference/cache changes and lifecycle events refresh
the presentation; the old five-second GPX banner polling is removed.

The global dock remains available during pedestrian/GPX navigation, selects Trip,
and reserves its measured height below the sheet. Search, place and point-selection
overlays still control visibility. Native Route opens the active sheet instead of
restarting route preparation. Car navigation retains its existing presentation.

TripSafety saves the exact TripPlan at registration. Preview and active checkpoints
therefore share distances, names, coordinates, elevations and planned timing. Old
registrations use their existing saved fields; absent profiles/coordinates are not
fabricated. Monitoring, delivery retry, routing and voice engines are reused.

## Changed files

Paths below are relative to the repository root.

- Runtime integration: `android/app/src/main/java/app/organicmaps/MwmActivity.java`,
  `home/HomeLayoutPolicy.java`, `routing/NavigationController.java`,
  `routing/HikePanelProgress.java`, `widget/menu/NavMenu.java`,
  `widget/menu/ActiveHikePanelController.java` (remaining Java paths share that package root).
- Existing data sources: `safety/TripPlan.java`, `safety/TripSafety.java`,
  `safety/TripWeatherRepository.java`, `safety/TripWeatherNotifier.java`.
- Resources under `android/app/src/main/res/`: `layout/layout_nav_bottom.xml`,
  `layout/active_hike_header.xml`, `layout/active_hike_details.xml`,
  `values/active_hike_strings.xml`, `values-ru/active_hike_strings.xml`.
- JVM tests under `android/app/src/test/java/app/organicmaps/`:
  `home/HomeLayoutPolicyTest.java`, `routing/HikePanelProgressTest.java`,
  `safety/TripPlanSnapshotTest.java`, `safety/TripWeatherHoursTest.java`.
- Test JSON dependency: `android/app/build.gradle`; this document.

## Validation on 2026-10-03

- 13 host JUnit tests passed for dock gating, progress/checkpoint/timing helpers,
  saved TripPlan roundtrip and hourly-cache parsing. JSON model tests use minimal
  Android/native boundary stubs; they do not exercise the Android UI.
- Java 17 syntax/signature checks, changed resource reference/duplicate checks,
  XML parsing, clang-format 23 and `git diff --check` passed.
- `./gradlew :app:compileFdroidDebugJavaWithJavac`: FAILED before compilation,
  Gradle distribution download reports `Network is unreachable`.
- `./gradlew :app:assembleFdroidDebug`: FAILED before compilation for the same reason.
- RUNTIME VERIFICATION NOT AVAILABLE: no Android device or emulator is available.
  APK compilation, visual layout and lifecycle behavior are not confirmed.

Required device checks before release: native and GPX navigation; expand/collapse;
light/dark, portrait/landscape and large fonts; system/gesture insets; Search/Guides
and return without cancelling the route; background/recreation; offline cached
weather and refresh; checkpoint crossing and profile marker; OK/break/finish;
SOS offline then reconnection without duplicate delivery. Safety delivery must be
checked with a test recipient, not a real emergency channel.

The pre-change rollback point on `feature/areamap-unified` is
`ee5406656f8cb5f22869a9664015921a2be08ecb`. Original redesign and functional
branches are not modified by this change.
