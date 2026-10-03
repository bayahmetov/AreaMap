# AreaMap destination presentation and content

Status: IMPLEMENTED IN DRAFT; ANDROID BUILD AND RUNTIME VERIFICATION BLOCKED.
Checked 2026-10-03. This is not a claim of a release-ready APK.

## Source mapping

Descriptions are short factual paraphrases in Russian and English, stored in the
existing `HikeRecommendation` model with `sourceName`, `sourceUrl` and related
routes. Articles and unlicensed photos have not been copied. Source figures
belong to named hikes, never to a peak/lake as universal properties.

| Destination | Content and altitude | Source | Image |
| --- | --- | --- | --- |
| Три Брата | Rocks near Kumbel, approach through Kok Zhailau; 2860 m retained from the previous documented coordinate source | https://zabugorshiki.com/mega-pohod/ | Original AreaMap vector artwork |
| БАО | Mountain basin, seasonal water colour, walking road approach; 2511 m | https://zabugorshiki.com/big-almaty-lake/ | Existing licensed exact-place WebP |
| Бутаковский водопад | Lower waterfall and distinction from Upper; 2159 m, source table for Lower | https://zabugorshiki.com/butakovka/ | Existing licensed Lower waterfall WebP |
| Кок-Жайляу | Plateau between the two Almaty gorges, city view, Prosveshchenets approach; 2200 m | https://zabugorshiki.com/kok-zhailau/ | Existing licensed exact-place WebP |
| Пик Фурманова | Kim Asar approach, swings, continuation to Panorama; 3053 m retained | https://zabugorshiki.com/furmanova-panorama/ | Existing licensed approach photo, with factual caption |
| Медеу / Шымбулак | Shymbulak resort, cable-car and walking access; 2280 m for the represented resort | https://zabugorshiki.com/medeo-shymbulak-marshrut/ | Existing licensed Shymbulak WebP |
| Пик Турист | Kosmostantsiya approach and summit views; 3954 m retained | https://zabugorshiki.com/peak-tourist/ | Original AreaMap vector artwork |

Existing asset authors, original pages and CC licenses remain in
`android/app/src/main/assets/areamap/hikes/photo_sources.json` and
`PHOTO_CREDITS.txt`. No hotlinks or new image downloads. Credits are reached
through a small action, rather than a large technical block.

## Related hikes and routing

| Related card | Published source figures | Planning waypoints |
| --- | --- | --- |
| Barrier → BAO, one way | 7 km; 2.5–4 hours; 3/5. Conflicting ascent figures omitted | Seven simplified points from the publicly visible article map; finish is the lakeside road, not the water |
| Lower + Upper Butakovsky waterfalls | 13.5 km; 7 hours; 901 m ascent; 3/5 | Fifteen points from the public map, retaining both waterfall branches and return |
| Furmanov + Panorama | 20.5 km; 8–9 hours; 1670 m ascent; 4/5 | Twenty-one points from the public map, retaining outbound and return order |
| Tourist Peak from Kosmostantsiya | 9 km; 5 hours 30 minutes; 1059 m ascent; 3/5 | Seven points from the public map |
| Prosveshchenets → Kok Zhailau | Omitted: these figures would describe a different published itinerary | Known approach start from the public map and existing plateau coordinate |
| Kok Zhailau → Tri Bratya | Omitted: the mega-traverse statistics do not describe this approach | Existing documented place coordinates |
| Medeu → Shymbulak on foot | Omitted: no unambiguous figures in the selected article | Medeu coordinate from https://kazakhstan.travel/en/attractions/399 and existing resort coordinate |

These are explicitly labelled **walking planning variants**. They are not
imports of source GPX files and do not claim to reproduce the exact source
track. The public map provides factual planning coordinates; protected/download
GPX endpoints were not accessed. Sparse waypoints require the installed walking
map and can produce a different path from the original hike. The existing native
router computes the actual distance, time and elevation for its Route Preview.
Missing maps and route-building failures remain in the existing routing flow.

`RoutingController.prepare(List<MapObject>, Router)` installs all points before
its existing build callback. Native optimization is disabled for these ordered
intermediates so the return leg is not reordered. The native limit is 102 total
points. Native navigation, GPX navigation and an active safety trip block
replacement. Ordinary current-location → destination routing remains separate.

## Presentation changes

- HOME was dark in both themes because `home_sheet` / `home_floating` used
  hardcoded almost-opaque dark colours. They now alias AreaMap semantic surfaces
  and borders, resolving separately in light and dark themes.
- The photograph gradient covered the full card with an almost-opaque bottom
  colour. It now follows only the measured title/metadata block at the bottom.
  Photograph titles and metadata use a theme-independent white token.
- Unrelated peak photos, regional-photo disclaimers, missing-image/description
  boilerplate, demo HOME weather/wind and the fake rating view/resources removed.
- HOME title 20sp, maximum two lines; metadata 14sp. All destinations switches
  to a responsive grid inside the existing scrollable sheet. Carousel/grid
  positions and mode survive Activity state restoration; detail dismissal
  leaves the existing HOME position in place.
- Detail hero follows 16:10; rounded corners; 48dp dark-backed icon controls.
  Title 30sp, subtype 17sp, description 17sp with four collapsed lines and
  expansion. Altitude is a compact inline place metric. Location has region,
  coordinates and the existing map-centering action.
- Weather uses the same repository and cache. Presentation shows temperature,
  condition and localized relative update age. Refresh is a secondary 14sp /
  48dp action. Main route CTA is 16sp / minimum 54dp.
- Shared ActionButton is 16sp with semantic on-green contrast. SecondaryButton
  is 14sp / minimum 48dp. New destination, route, active-hike, track-stop and
  safety retry buttons use these explicit styles/sizes. Profile text actions
  already use 15sp / 48dp. No button text uses px.
- Insets, sticky CTA and the existing bottom dock reservation retained.
  No new Activity, weather downloader, favorites store or share system.

## Verification

- 25 host JUnit tests passed: nine destination/catalog/matching, five snapshot
  and weather parser, eight HOME/progress, three destination-cache isolation.
  Android/native boundaries use test stubs; these are not device tests.
- Java 17 syntax/member/signature audit passed for eight changed production
  classes. Framework JNI signature and native waypoint append/order handling
  checked against the repository. Clang-format 23 and `git diff --check` passed.
- 1132 Android resource XML files parsed. Changed resource references resolved,
  and new AreaMap button typography audit found no violations.
- `:app:compileFdroidDebugJavaWithJavac` and `:app:assembleFdroidDebug` both stop
  while downloading Gradle 9.6.0: `java.net.SocketException: Network is unreachable`.
  Neither task reached Android compilation. Android SDK / emulator are not
  installed in this environment. A Python HTTPS probe can reach the Gradle URL,
  but does not supply the missing Android SDK/NDK or resolve Java build access.
- **RUNTIME VISUAL VERIFICATION NOT AVAILABLE**. No device screenshots or
  assertion that light/dark/landscape UI has been visually verified.

Before marking the PR ready: run both Android build tasks and exercise the
seven destinations, light/dark themes, large fonts, landscape, external source,
share, bookmarks, HOME restoration, offline weather and native route preview.

Rollback baseline for this change: remote commit
`907b6c54a06d526f805cb237094badd55c924993` on `feature/areamap-unified`.
Original functional and redesign branches remain untouched.
