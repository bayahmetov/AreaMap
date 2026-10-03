# Guides functional integration

The existing Guides screen and reader are retained. This pass changes content selection and navigation, not the visual system. No photographs or attribution metadata are changed.

## Audit before changes

- `GuideArticles.java` contained nine legacy first-aid/navigation articles in Java and seven newer articles in RU/EN resources. Its context-free overload returned only the legacy subset; its context overload returned all 16.
- `GuideListFragment` had functional category/search filters, article clicks and persisted favorites. Hero dots switched photographs only. Route recommendations used four hardcoded IDs; “All by route” only scrolled. Cards and Search's “For your hike” shortcuts passed numeric article indices.
- Destinations were `GuideListActivity`/`GuideListFragment` and `GuideArticleActivity`/`GuideArticleFragment`. No second reader was needed. The original nine articles remained reachable, but index-based routing was fragile.
- Stable existing keys are `legacy_0` through `legacy_8`, plus `route`, `weather`, `gear`, `signal`, `water`, `wildlife`, `plants`. Favorite preference keys are preserved.

## Result

`GuideRepository` is the only public catalog entry point. `GuideArticles.catalog(Context)` supplies the same canonical model, with all content localized in resources. Search includes localized category labels, title, keywords, route tags and body. Unknown IDs return null and the reader renders a localized unavailable state; it never substitutes the first article.

All/category/favorites/search/route selections use a vertical listing within the existing fragment. System Back returns from listing to the hub, then follows the existing activity back stack. Article Back finishes the reader and returns to the previous selection. Hero indicators select canonical featured articles, update title/photo/details and bind the CTA to that selected ID.

Favorites continue to use `areamap_guide_favorites` preferences. Missing photos use the existing credited regional photograph, with the AreaMap logo as a last-resort resource fallback.

### Route rules

`GuideRouteState` reads the foreground GPX session first, then built/navigating Organic Maps routes, then the registered TripSafety trip. It does not mutate them. GPX points provide real altitude and regional tags; Organic Maps provides router type and destination; TripSafety provides stored weather target coordinates and altitude. An Almaty-region coordinate box labels the region only, never a specific summit. Calendar season supplies cold/hot tags; it is not a weather forecast.

Each matching route tag adds 100 priority points. Emergency/weather/water articles receive 10 universal points. Stable IDs break ties. Absent route context returns no recommendations. Full route listing preserves priority order. The catalog currently contains no specific-peak article: the region tag adjusts the contextual subtitle, not an invented local article.

Guides bottom navigation returns to the existing MwmActivity with CLEAR_TOP/SINGLE_TOP and dispatches Search/Route/Trip/Profile to its existing handlers. Profile remains Settings. The current Guides tab resets the hub. No new root activity is added for filtered lists.

Debug logs contain stable guide/category IDs and non-identifying context source/tags only, never route names, coordinates or personal trip details.

## A. Migrated old guides

| Old title (RU) | New guideId | Category | Status |
|---|---|---|---|
| Сильное кровотечение | legacy_0 | survival | migrated |
| Перелом, вывих или сильное растяжение | legacy_1 | survival | migrated |
| Травма головы | legacy_2 | survival | migrated |
| Переохлаждение | legacy_3 | survival | migrated |
| Перегрев и тепловой удар | legacy_4 | survival | migrated |
| Нет сознания / человек не дышит нормально | legacy_5 | survival | migrated |
| Высотная болезнь | legacy_6 | survival | migrated |
| Если потерялись | legacy_7 | lost | migrated |
| Гроза в горах | legacy_8 | weather | migrated |

All nine titles, keywords and full bodies in both languages were compared byte-for-byte against the preceding commit. No medical advice was rewritten. Forecast preparation versus immediate thunderstorm response, and coverage loss versus being lost, remain separate useful articles rather than being incorrectly merged.

## B. Clickable UI

“Static” means a traced source path, not a device click. “JVM” means executed actual repository/model logic with minimal Android resource stubs, not Android UI execution.

| UI element | Action | Destination | Verified |
|---|---|---|---|
| Hero CTA | Open selected featured ID | Existing article reader | Static; featured selection JVM |
| Hero dots | Select article/title/photo/details | Featured catalog entry | Static; catalog JVM |
| All seven categories | Open category listing | Repository category selection | Static; filtering JVM |
| Both All guides links | Open full vertical listing | All 16 canonical articles | Static; catalog JVM |
| Popular/search/category cards | Open stable ID | Existing reader | Static; lookup JVM |
| Favorite heart | Toggle persistent preference | Current card / favorites filter | Static only |
| Search | Filter title/category/tags/body | Clickable canonical results | Static; search JVM |
| Clear empty search | Clear query/category/favorites | Full listing | Static only |
| Filters | All/favorites/category selection | Existing fragment listing | Static; categories JVM |
| Route cards | Open stable ID | Existing reader | Static; recommendation JVM |
| All by route | Open prioritized route listing; choose route if absent | Existing listing/map route flow | Static; rules JVM |
| Choose route | Return to existing route action | MwmActivity route handler | Static only |
| Article Back | Finish reader | Prior Guides/search screen | Static only |
| System Back in listing | Restore hub | Existing Guides fragment | Static only |
| Search bottom tab | Return to existing map action | MwmActivity search | Static only |
| Route bottom tab | Return to existing map action | OM/GPX route flow | Static only |
| Trip bottom tab | Return to existing map action | TripSafetyActivity | Static only |
| Guides bottom tab | Restore hub | Existing Guides fragment | Static only |
| Profile bottom tab | Return to existing map action | Existing Settings | Static only |
| Search For your hike shortcuts | Pass legacy stable ID | Existing reader | Static; catalog JVM |
| Article emergency action | ACTION_DIAL when supported | System dialer (112) | Static only |
| Photo credits / article source links | Open credits or linked source | Existing dialog / system link handler | Static only |

## Dead resources

A whole-Android search found no remaining references to `areamap_article.xml` or `guide_hero_cold/first_aid/mountain/storm.xml` after removing `imageResId`. Those five unused resources were removed. The existing dark reader layout, all photos and source metadata are retained. Unrelated working-tree changes are excluded from this commit.

## C. Remaining limitations

- Android build is blocked before compilation by Gradle distribution download (`Network is unreachable`). There is no successful APK build for this pass.
- No SDK/emulator/adb is available. UI taps, back stack, map/search/routing/navigation/Trip/SOS/offline maps, locale switching and logcat cannot be runtime verified here.
- Existing demo ratings remain explicitly labeled demo; no real review dataset is claimed.
- Profile uses existing Settings; no standalone profile screen is claimed.

## D. Tests/build

- Five JUnit 4 tests: PASS, compiled by ECJ Java 17 and run directly against the actual repository/model with resource stubs. Covers search, category isolation, absent-route behavior, relevant-tag priority, featured canonical articles, duplicate IDs, unknown/null IDs and geographic tagging.
- Catalog harness: PASS, 16 unique identical IDs/categories across RU/EN.
- Content migration comparison: PASS, every original localized title/keyword/body preserved exactly.
- XML/resource reference checks, RU/EN key/format parity and all 12 original photo checksums: PASS.
- Java 17 syntax checks: PASS; Android UI type checking is not covered by these syntax checks.
- `git diff --check`: PASS.
- `./gradlew :app:assembleFdroidDebug -Parm64 --console=plain`: BLOCKED while downloading Gradle 9.6.0, before project compilation.
- `RUNTIME VISUAL VERIFICATION NOT AVAILABLE`.

## Every changed file

- `android/app/src/main/java/app/organicmaps/MwmActivity.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideArticleActivity.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideArticleAdapter.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideArticleFragment.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideArticles.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideListFragment.java`
- `android/app/src/main/java/app/organicmaps/safety/GuidePhotos.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideRepository.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideRouteState.java`
- `android/app/src/main/java/app/organicmaps/safety/RouteGuideContext.java`
- `android/app/src/main/java/app/organicmaps/search/SearchFragment.java`
- `android/app/src/main/res/drawable/guide_hero_cold.xml`
- `android/app/src/main/res/drawable/guide_hero_first_aid.xml`
- `android/app/src/main/res/drawable/guide_hero_mountain.xml`
- `android/app/src/main/res/drawable/guide_hero_storm.xml`
- `android/app/src/main/res/layout/areamap_article.xml`
- `android/app/src/main/res/layout/fragment_guide_list.xml`
- `android/app/src/main/res/values-ru/areamap_guides_strings.xml`
- `android/app/src/main/res/values/areamap_guides_strings.xml`
- `android/app/src/test/java/app/organicmaps/safety/GuideArticlesTest.java`
- `docs/AREAMAP_GUIDES_FUNCTIONAL_INTEGRATION.md`
