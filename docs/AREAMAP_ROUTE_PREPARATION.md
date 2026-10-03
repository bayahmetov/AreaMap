# Route Preview preparation

One section in `areamap_route_panel_v2.xml`, after the existing metric row and
before the elevation graph. The section uses the existing AreaMap surface,
text and green resources in both themes. Packing labels wrap inside a
`ChipGroup`; they are ordinary informational TextViews so large fonts and long
translations can grow vertically. The Start button remains outside the scroll
container, with its existing dock spacing.

## Content and attribution

Curated on 2026-10-03. Short paraphrases, not complete article imports:

| Place | Primary content source | Fact used |
| --- | --- | --- |
| Boukreev | https://zabugorshiki.com/boukreev/ | Exposed stretches on the climb and wind near the summit |
| Lower Butakovsky Waterfall | https://zabugorshiki.com/butakovka/ | The climb onto rocks above the falls is steep and the descent difficult |
| Big Almaty Lake | https://zabugorshiki.com/big-almaty-lake/ | Bring identification and check access before departure |
| Furmanov | https://zabugorshiki.com/furmanova-panorama/ and https://zabugorshiki.com/en/furmanova-panorama/ | Choose a break location according to summit wind conditions |

The existing `HikeRecommendation` destination content model owns this data.
No new catalog, scraper, repository, or weather downloader was introduced.
The source link is labelled “About this place”, rather than attributing the
entire equipment policy to an article.

Boukreev has content only; the home catalog is unchanged. Its representative
coordinate, 43.1669, 77.1344, is corroborated by:
https://tengrinews.kz/zailiyskiy-alatau-routes/medeu-pereval-lesnoy-pik-bukreeva-turbaza-almatau-447366/
No conflicting published altitude or route duration is imported from that page.

Association uses actual destination or native intermediate route-mark
coordinates, not a title or interpolated preview checkpoint. The default match
radius remains 100 m. Existing curated GPX approach/viewpoints within 500 m of a
catalog location are also recognised within 100 m of those waypoints. This
covers BAO's shore and Furmanov's approach without associating a loop's distant
trailhead with its summit. Other locations receive neutral practical advice.

## Presentation policy

Native distance, duration, ascent and maximum altitude select equipment. A long
route means at least 10 km, four hours, or 800 m ascent; this selects food,
light/charging and a first aid kit, with an early-departure note. At least 2,500 m
selects warm clothing and sun protection. These are conservative presentation
thresholds, not a difficulty classification or a guarantee of safety.

The existing controller's point-matched forecast can add rain protection, a
warm layer, sun protection and one weather note. It must cover the expected
arrival hour within 90 minutes. Thunderstorms take priority over other weather
notes. No article weather values, copied timings, fixed water quantities or
published hike distances are used for the newly built route.

Default presentation: 2–8 equipment labels and 2–4 short notes. No expansion,
horizontal scroller, additional overlay or debug copy. The source link is shown
only for a matched curated place.

## Verification

- Fourteen Java 17 host checks passed: nine new policy/location tests and five
  existing catalog tests. Production model/policy classes were compiled with
  generated resource constants and minimal annotation/JUnit assertion boundaries.
  This does not replace Android compilation or Gradle's JUnit task.
- XML section order, unique IDs, preservation of every old XML element,
  resource references and dark/light color contrast checked statically.
- Java controller syntax parsed with the Java 17 compiler; Android type checking
  remains unverified.
- `git diff --check` passed. The requested style check could not run because
  clang-format is not installed in this environment.
- Both requested Fdroid Gradle tasks stop while downloading Gradle 9.6.0 with
  `Network is unreachable`, before compilation. No APK produced.
- No Android SDK/emulator/device available for runtime visual verification.

Pending device checks: open a Boukreev preview; confirm placement, wrapping at
small widths/large fonts, source link, light/dark appearance, scroll and dock
clearance. Then verify graph interaction, checkpoint taps, Start and forecast
refresh. Repeat with a short unknown route, a waterfall and a round trip.
