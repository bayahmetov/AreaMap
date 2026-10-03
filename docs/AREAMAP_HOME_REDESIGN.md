# AreaMap HOME redesign implementation status

Scope: ordinary map, no selected place, no planned or active route. Based on branch feature/areamap-redesign, commit 4ac6aac.

## Implemented

- Floating translucent header with brand and actual search entry.
- Replacement HOME layers/location actions call existing handlers. Legacy duplicates are hidden only while HOME owns presentation; zoom remains available.
- Dedicated BottomSheetBehavior, scrollable destinations, default 310 dp collapsed height capped on short screens, 130 dp landscape peek, large-text header, system/cutout margins, restored collapsed/expanded state.
- Horizontal destination adapter with seven localized catalog entries and persistent local favorites. Card taps use the existing place search flow.
- Real downloaded-map state and downloader entry. Weather, ratings and durations are explicitly demo estimates, not live conditions.
- One bottom navigation with Search, Route, Trip, Guides, Profile. Profile opens existing settings.
- Launcher no longer opens legacy empty search feed. Real persisted query restoration remains supported. Search, place, routing, GPX and point chooser retain separate ownership.

## Blocking limitations

- No destination photographs could be downloaded: Wikimedia requests timed out at the network proxy. No fake photos or photo placeholders were substituted. Asset filenames and optimization guidance: android/app/src/main/assets/areamap/hikes/README.md. Until installed, cards are compact text recommendations and HOME displays a missing-photo notice.
- fdroidDebug attempted: Gradle 9.6 distribution download fails with Network is unreachable before project compilation. Android SDK and emulator are unavailable. No APK was produced and Android type/resource compilation is not verified.
- RUNTIME VISUAL VERIFICATION NOT AVAILABLE. Runtime wiring is verified in source, not by opening the app. Old-UI exclusion is implemented but unverified on a device.
- The reference uses photographic mountain relief. Existing Organic Maps vector tiles are retained; UI XML cannot reproduce that underlying map imagery.
- Source parsing, resource reference checks and layout policy checks are not an Android build or visual acceptance.

## Verification completed

- Java sources parsed with JDK compiler parser: pass.
- Authored XML parsed; referenced local resource names resolve: pass.
- EN/RU resource names and printf arguments agree: pass.
- HOME ownership: 16 combinations; collapsed height: 1500 viewport sizes and landscape: pass (standalone JDK check). JUnit counterpart included; Gradle JUnit run blocked.
- MwmActivity -> activity_map -> four HOME includes; sheet Behavior set on actual include; constructor and visibility path wired: checked.
- git diff --check: pass.

## Every changed file

- `android/app/src/main/assets/areamap/hikes/README.md`
- `android/app/src/main/java/app/organicmaps/MwmActivity.java`
- `android/app/src/main/java/app/organicmaps/home/AreaMapHomeController.java`
- `android/app/src/main/java/app/organicmaps/home/HikeRecommendation.java`
- `android/app/src/main/java/app/organicmaps/home/HikeRecommendationAdapter.java`
- `android/app/src/main/java/app/organicmaps/home/HomeInfoRepository.java`
- `android/app/src/main/java/app/organicmaps/home/HomeLayoutPolicy.java`
- `android/app/src/main/res/drawable/home_badge.xml`
- `android/app/src/main/res/drawable/home_circle.xml`
- `android/app/src/main/res/drawable/home_filters.xml`
- `android/app/src/main/res/drawable/home_heart.xml`
- `android/app/src/main/res/drawable/home_indicator.xml`
- `android/app/src/main/res/drawable/home_logo.xml`
- `android/app/src/main/res/drawable/home_photo_gradient.xml`
- `android/app/src/main/res/drawable/home_profile.xml`
- `android/app/src/main/res/drawable/home_sheet.xml`
- `android/app/src/main/res/drawable/home_surface.xml`
- `android/app/src/main/res/layout/activity_map.xml`
- `android/app/src/main/res/layout/areamap_bottom_nav.xml`
- `android/app/src/main/res/layout/areamap_hike_card.xml`
- `android/app/src/main/res/layout/areamap_home_controls.xml`
- `android/app/src/main/res/layout/areamap_home_header.xml`
- `android/app/src/main/res/layout/areamap_home_info.xml`
- `android/app/src/main/res/layout/areamap_home_sheet.xml`
- `android/app/src/main/res/values-land/areamap_home_dimens.xml`
- `android/app/src/main/res/values-ru/areamap_home_strings.xml`
- `android/app/src/main/res/values/areamap_home_colors.xml`
- `android/app/src/main/res/values/areamap_home_dimens.xml`
- `android/app/src/main/res/values/areamap_home_strings.xml`
- `android/app/src/main/res/values/areamap_home_styles.xml`
- `android/app/src/test/java/app/organicmaps/home/HomeLayoutPolicyTest.java`
- `docs/AREAMAP_HOME_REDESIGN.md`
