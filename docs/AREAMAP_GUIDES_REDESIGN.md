# AreaMap Guides redesign

## Runtime ownership

`MwmActivity`'s existing Guides action launches `GuideListActivity`, which now uses
`activity_areamap_guides.xml` and attaches the same `GuideListFragment`.
The fragment inflates the replaced `fragment_guide_list.xml`. `GuideArticleActivity`
uses the same activity shell and existing `GuideArticleFragment` destination,
which inflates the replaced `fragment_guide_article.xml`. No parallel Guides screen
or legacy list is layered behind the new layout. The activity shell has no toolbar.
Exactly one `areamap_bottom_nav.xml` include exists in the list; Guides is selected.

## Implemented behavior

- Floating branded header; real title/body/tag search, category and favorite filters.
- Rounded photographic hero with four selectable indicators and a CTA to the article list.
- Seven model-driven categories; counts derive from the actual sixteen-article catalog.
- Two-column phone grid; wide screens use four categories in the first row, three in the second.
- Horizontal popular and compact route carousels; local persistent favorites; explicitly demo ratings.
- Route state derives from Organic Maps built/navigation, foreground GPX or actual TripSafety session.
  No route shows the choose-route action. Existing routing action is dispatched after map rendering
  is initialized through a one-shot Intent extra. No routing engine changes.
- Route recommendations are explicitly general preparation, not claimed local water/hazard intelligence.
- Article destinations retain the first nine legacy indices and their original safety text.
  Seven preparation articles added using EN/RU string resources, with NPS/CDC source links.
- Structured headings, paragraphs, bullets, interactive checklists, warning cards and a 112 dialer action
  for relevant articles. Dialer opens only; no automatic call. Back returns to Guides.
- Search/Route navigation returns to existing map actions; Trip opens existing TripSafetyActivity;
  Profile opens existing SettingsActivity. No new fake destinations.
- Full scroll, font-scale-aware cards, insets for status/gesture bars, cutouts and keyboard.
- Twelve optimized local landscape WebP assets; sources, authors, licenses, actual locations,
  download dates, transformations and checksums are retained and exposed from the list/articles.

## Deliberate differences from reference

The reference's original images are not redistributed. Real licensed photographs replace them.
The Three Brothers filename contains a regional Alma-Arasan / Trans-Ili Alatau photograph;
it is not claimed to depict Three Brothers. This is disclosed in metadata and hero accessibility.
Storm and bear photographs are generic illustrations outside Kazakhstan, clearly identified in credits;
plant/water imagery is not evidence of toxicity/protection or drinking-water safety.
No fabricated route, review counts, category counts or verified local hazards are shown.
Ratings are explicitly demo; times are estimated from text length.

## Verification and limits

PASS: git diff --check; guide XML parse and Android resource reference resolution;
EN/RU keys and printf signatures; twelve WebP image checksums/aspect/size;
all image usages mapped; existing list/article runtime source links and single nav include.
PASS: Eclipse JDT Java 17 syntax parser for all modified Guides Java and MwmActivity.
PASS: actual GuideArticles model run on JVM with minimal Android resource fixtures:
sixteen unique IDs; stable destinations/categories across EN/RU; localized case/whitespace search.
JUnit regression tests included; Gradle tests could not execute.

Attempted `./gradlew :app:assembleFdroidDebug -Parm64 --console=plain`.
FAILED before compilation: wrapper download of Gradle 9.6.0 reports `Network is unreachable`.
No Android SDK/emulator/device is available here. Successful Android type/resource compilation,
actual visibility, taps, typography and screenshot comparison remain unverified.
RUNTIME VISUAL VERIFICATION NOT AVAILABLE
Do not treat this draft as having passed the user's complete Definition of Done.

## Every changed file

- `android/app/src/main/assets/areamap/guides/GUIDE_IMAGE_SOURCES.md`
- `android/app/src/main/assets/areamap/guides/guide_big_almaty_lake.webp`
- `android/app/src/main/assets/areamap/guides/guide_butakovsky_waterfall.webp`
- `android/app/src/main/assets/areamap/guides/guide_hero_almaty.webp`
- `android/app/src/main/assets/areamap/guides/guide_image_sources.json`
- `android/app/src/main/assets/areamap/guides/guide_kok_zhailau.webp`
- `android/app/src/main/assets/areamap/guides/guide_no_signal.webp`
- `android/app/src/main/assets/areamap/guides/guide_plants.webp`
- `android/app/src/main/assets/areamap/guides/guide_route_planning.webp`
- `android/app/src/main/assets/areamap/guides/guide_survival_fire.webp`
- `android/app/src/main/assets/areamap/guides/guide_three_brothers.webp`
- `android/app/src/main/assets/areamap/guides/guide_water_source.webp`
- `android/app/src/main/assets/areamap/guides/guide_weather_storm.webp`
- `android/app/src/main/assets/areamap/guides/guide_wildlife_bear.webp`
- `android/app/src/main/java/app/organicmaps/MwmActivity.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideArticleActivity.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideArticleAdapter.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideArticleFragment.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideArticles.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideCategory.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideCategoryAdapter.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideListActivity.java`
- `android/app/src/main/java/app/organicmaps/safety/GuideListFragment.java`
- `android/app/src/main/java/app/organicmaps/safety/GuidePhotos.java`
- `android/app/src/main/res/drawable/guide_accent_button.xml`
- `android/app/src/main/res/drawable/guide_dark_gradient.xml`
- `android/app/src/main/res/drawable/guide_icon_compass.xml`
- `android/app/src/main/res/drawable/guide_icon_fire.xml`
- `android/app/src/main/res/drawable/guide_icon_leaf.xml`
- `android/app/src/main/res/drawable/guide_icon_paw.xml`
- `android/app/src/main/res/drawable/guide_icon_water.xml`
- `android/app/src/main/res/drawable/guide_icon_weather.xml`
- `android/app/src/main/res/layout/activity_areamap_guides.xml`
- `android/app/src/main/res/layout/areamap_guide_card.xml`
- `android/app/src/main/res/layout/areamap_guide_category.xml`
- `android/app/src/main/res/layout/fragment_guide_article.xml`
- `android/app/src/main/res/layout/fragment_guide_list.xml`
- `android/app/src/main/res/values-ru/areamap_guides_strings.xml`
- `android/app/src/main/res/values/areamap_guides_colors.xml`
- `android/app/src/main/res/values/areamap_guides_dimens.xml`
- `android/app/src/main/res/values/areamap_guides_strings.xml`
- `android/app/src/main/res/values/areamap_guides_styles.xml`
- `android/app/src/test/java/app/organicmaps/safety/GuideArticlesTest.java`
- `docs/AREAMAP_GUIDES_REDESIGN.md`

## IMAGES ADDED

filename | usage | real location/generic | source | author | license
--- | --- | --- | --- | --- | ---
guide_hero_almaty.webp | Hero | Trans-Ili Alatau seen from Shymbulak, Almaty (real location) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Shymbulak%2C_Almaty_%28P1180203%29.jpg) | Matti Blume | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)
guide_three_brothers.webp | Regional illustration; NOT a photograph of Three Brothers | Alma-Arasan valley, Trans-Ili Alatau (regional image) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:At_Alma-Arasan_valley%2C_Trans-Ili_Alatau._Panoramic_view_-_panoramio.jpg) | Igors Jefimovs | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/)
guide_big_almaty_lake.webp | Route planning article | Big Almaty Lake (real location) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Big_Almaty_Lake.jpg) | Igors Jefimovs | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/)
guide_butakovsky_waterfall.webp | Local destinations / water illustration; not evidence of potability | Lower Butakovsky waterfall (real location) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:%D0%91%D1%83%D1%82%D0%B0%D0%BA%D0%BE%D0%B2%D1%81%D0%BA%D0%B8%D0%B9_%D0%B2%D0%BE%D0%B4%D0%BE%D0%BF%D0%B0%D0%B4_12.jpg) | Sane4o | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)
guide_kok_zhailau.webp | Route checklist | Kok Zhailau (real location) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:The_nature_of_Kazakhtan._Kok_Zhailau.jpg) | Matkarimoffart | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)
guide_route_planning.webp | Trail navigation category | Medeu regional nature park, Almaty (real location) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Path_to_Shymbulak.jpg) | Nikita Mikhailovskiy | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)
guide_weather_storm.webp | Weather category; never labelled Almaty | Spin Ghar, Pakistan (generic illustration) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Lightning_over_white_mountains.jpg) | Mujtaba Hassan | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)
guide_survival_fire.webp | Survival category; not permission to light fires | Not specified (generic illustration) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Campfire_4213.jpg) | Dirk Beyer | [CC BY-SA 2.5](https://creativecommons.org/licenses/by-sa/2.5/)
guide_wildlife_bear.webp | Wildlife category; never labelled a Kazakhstan bear | Banff National Park, Canada (generic illustration) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:BrownBear.jpg) | Bugabusu | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)
guide_plants.webp | Vegetation illustration; no claim this plant is poisonous | Medeu park, Almaty (regional image) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:%D0%90%D0%BB%D0%BC%D0%B0%D1%82%D1%8B%2C_%D0%9C%D0%B5%D0%B4%D0%B5%D0%BE%2C_%D1%80%D0%B5%D0%BA%D0%B0_%D0%9C%D0%B0%D0%BB%D0%B0%D1%8F_%D0%90%D0%BB%D0%BC%D0%B0%D1%82%D0%B8%D0%BD%D0%BA%D0%B0%2C_%D0%B1%D0%BE%D0%B4%D1%8F%D0%BA_%D0%BE%D0%B1%D1%8B%D0%BA%D0%BD%D0%BE%D0%B2%D0%B5%D0%BD%D0%BD%D1%8B%D0%B9_%28cropped%29.jpg) | ElenaLitera | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)
guide_water_source.webp | Water category; not evidence of potability | Assy plateau, Almaty Region (real location) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:%D0%94%D0%BE%D0%BB%D0%B8%D0%BD%D0%B0_%D1%81_%D1%80%D0%B5%D0%BA%D0%BE%D0%B9_%D0%BD%D0%B0_%D0%9F%D0%BB%D0%B0%D1%82%D0%BE_%D0%90%D1%81%D1%8B.jpg) | Viktorius001 | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)
guide_no_signal.webp | Navigation context; no claim a person was lost or coverage was absent | Approach to Furmanov peak, Ile-Alatau (regional image) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:On_the_way_to_Furmanov_peak.jpg) | Jpeg2art | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)
