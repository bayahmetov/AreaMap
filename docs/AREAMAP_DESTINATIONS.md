# Popular destination details

## Flow and existing sources

`AreaMapHomeController` → existing `HikeRecommendation` catalog →
`DestinationDetailsFragment` in the same activity. Back dismisses the panel; the
HOME controller, bottom-sheet state and carousel scroll are retained. No new
navigation engine or destination database is introduced.

The previous card action opened search. All seven cards now open details before
routing. Demo ratings, difficulty and walking-time estimates are removed from
these cards. No review dataset or best-season metadata exists. Missing descriptions
show the requested fallback; their expand control is hidden. The description
presentation supports long resource-backed content and retained expansion state.

## Functionality / click audit

| Element | Action / source |
| --- | --- |
| Back and system Back | Dismiss current panel without recreating HOME |
| Gallery | Local optimized WebP, horizontal RecyclerView + page snapping; one actual image per destination, no duplicated pages/indicator |
| Photo caption | Existing bundled attribution viewer |
| Favorite | Existing BookmarkManager saved places; old HOME boolean favorites migrate to bookmarks |
| Share | Existing SharingUtils Android chooser, title + coordinates + geo URI |
| Weather and refresh | Existing TripWeatherRepository downloader/parser; forecast time, condition, cache timestamp, loading/unavailable |
| Description | Actual resource if provided, otherwise fallback with no inert expand action |
| Location | Existing Framework nativeZoomToPoint, exact catalog coordinates |
| Build route | Existing RoutingController.prepare with pedestrian target; current navigation is protected |
| Related route | Current real TripPlan only if destination matches start/intermediate/finish within 100 m, opens existing preview |

There is no curated destination-to-route repository or saved route favorite
metadata. No related routes, ratings, difficulty or season are fabricated. Empty
related routes are explicitly disclosed; a matching current plan displays its
real endpoints, distance/time and the destination photograph with its disclosure.

Destination forecasts use per-catalog-ID preference caches inside the existing
repository. They cannot overwrite the active trip's cache or hazard notification
state. Offline reads require matching coordinates/elevation and a valid current
hour. No second weather API/client/worker is added.

The scrollable panel uses existing AreaMap semantic light/night colors, card
radii, spacing and typography, with a fixed bottom route action and system insets.
Photo controls use a dark translucent background with contrasting icons. A route
request without a known starting position retains the existing origin-selection
flow. Geographic catalog points do not constitute vetted hiking tracks.

## Coordinate and image provenance

No new images are downloaded. Existing `assets/areamap/hikes/photo_sources.json`
and `PHOTO_CREDITS.txt` retain author/license/source details. Three Brothers and
Tourist Peak use the existing Furmanov approach image as a **labelled regional
landscape**, not a purported photo of those peaks. The Furmanov image is labelled
as the approach. Other bundled photographs depict their named destinations.

| Catalog ID | Representative coordinate | Source |
| --- | --- | --- |
| tri_bratya | 43.127825, 77.013213; 2860 m | Adilet ministerial list, item 55 |
| furmanov_peak | 43.149454, 77.116494; 3053 m | Adilet list, item 58 |
| tourist_peak | 43.028769, 76.952072; 3954 m | Adilet list, item 163 |
| big_almaty_lake | 43.05060, 76.98530 | Kazakhstan Travel destination coordinates; previous catalog height retained |
| kok_zhailau | 43.139722, 76.997778; 2200 m | UNFF reference UNM-058 (representative plateau point) |
| butakovsky_waterfall | 43.17231, 77.11401 | Centre of named lower-waterfall OSM footprint in topographic-map listing; unsupported old height removed |
| medeu_shymbulak | 43.12827, 77.08142 | Kazakhstan Travel Shymbulak resort coordinates; detail subtitle identifies Shymbulak, old height removed |

Sources checked 2026-10-03:

- https://old.adilet.zan.kz/rus/docs/V2500036696/compare/eng
- https://www.kazakhstan.travel/en/attractions/510
- https://kazakhstan.travel/ru/attractions/398
- https://unff.kz/page.php?lang=1&page_id=38
- https://en-za.topographic-map.com/map-zhkltj/Бутаковский-водопад/

## Verification

- 7 destination catalog / geographic relationship tests passed.
- 3 additional destination weather tests passed: offline cache isolation, wrong-point
  rejection and no borrowing of active-trip forecasts.
- Existing 13 progress/dock/snapshot/hour-parser tests passed.
- Total: 23 host JUnit tests passed; Android/native boundaries use test stubs.
- Java 17 syntax/signature checks, XML/resource checks, clang-format 23 and
  `git diff --check` passed. These do not substitute for Android compilation.
- `compileFdroidDebugJavaWithJavac`: FAILED before compilation; Gradle download
  reports `Network is unreachable`.
- `assembleFdroidDebug`: FAILED before compilation for the same reason.
- RUNTIME VISUAL VERIFICATION NOT AVAILABLE: no emulator/device.

Device checks still required: all seven cards, photo/caption and page behavior,
Back/scroll restoration, favorite persistence and migration, sharing chooser,
online/offline forecast and cache isolation, location centering, existing route
preview and route construction, light/dark, landscape and large fonts.

Rollback point before this change: `044414af3786a5ff89cbf60e00ee6ff16c7c2e65`
on `feature/areamap-unified`. Original source branches remain unchanged.

## Files changed

Java package root: `android/app/src/main/java/app/organicmaps/`.

- `MwmActivity.java`
- `home/AreaMapHomeController.java`
- `home/HikeRecommendation.java`
- `home/HikeRecommendationAdapter.java`
- `home/DestinationDetailsFragment.java`
- `home/DestinationBookmarks.java`
- `home/DestinationRouteMatch.java`
- `safety/TripWeatherRepository.java`
- `safety/RouteCheckpointBookmarks.java`
- `util/SharingUtils.java`

Resources: `android/app/src/main/res/`.

- `layout/areamap_home_sheet.xml`
- `layout/areamap_hike_card.xml`
- `layout/areamap_destination_details.xml`
- `drawable/destination_back.xml`
- `drawable/destination_card.xml`
- `values/areamap_destination_strings.xml`
- `values-ru/areamap_destination_strings.xml`

Tests: `android/app/src/test/java/app/organicmaps/`.

- `home/HikeRecommendationTest.java`
- `home/DestinationRouteMatchTest.java`
- `safety/TripWeatherDestinationTest.java`

Documentation: this file.
