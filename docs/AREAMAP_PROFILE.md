# AreaMap Profile implementation and verification

Continued from commit `6439156` and the existing, untracked Profile scaffolding.
No existing redesign code was rolled back.

## Navigation

- `MwmActivity.areamap_nav_profile` now opens `AreaMapProfileActivity`, not Settings.
- The existing Guides `profile` action reaches that same destination through MwmActivity.
- Trip Safety has a Profile entry. Reordering an existing Profile activity avoids duplicate instances.
- Profile reuses `areamap_bottom_nav.xml`, highlights Profile and provides all five actions.
- Settings opens `SettingsActivity` normally; Back returns to Profile, then to its caller.
- Leaving Profile for map actions uses CLEAR_TOP/SINGLE_TOP and consumes the existing navigation action.
- Achievements and hike-history pages are internal Profile states; Back restores the profile page.

## OpenStreetMap integration

The existing editor `ProfileActivity`, `ProfileFragment`, `OsmLoginActivity`, OAuth, avatar,
changesets, logout, history and notes were not changed. Settings still opens the existing OSM
account. A separate optional account row in AreaMap Profile opens OSM Profile when authorized,
or OSM Login with its existing redirect when disconnected. AreaMap identity never requires OAuth.

## Identity and avatar

`AreaMapUserProfile` and `AreaMapProfileRepository` use private `areamap_profile` SharedPreferences.
Name, bio, region and avatar path survive recreation. Editor drafts are saved across rotation.
AndroidX Photo Picker falls back to the system document picker; no broad storage permissions are
added. The selected image is bounded to 20 MiB, decoded at a reduced resolution and copied to a
private JPEG file on a worker thread. The saved profile does not depend on temporary URI access.
Failed imports retain the previous avatar. Preference observation refreshes a recreated screen
when an in-flight avatar import finishes.

## Real data sources

| Counter / badge | Source | Meaning |
| --- | --- | --- |
| Saved routes | Sum of BookmarkManager categories' getTracksCount() | Saved/imported/recorded tracks present in the existing collection |
| Offline regions | MapManager.nativeGetDownloadedCount() | Actual downloaded map count |
| Completed hikes | 0 | TripSafety retains an active/last plan, not completed-trip history; saved tracks do not prove completion |
| Peaks | 0 | No summit-completion evidence exists |
| First peak / 10 hikes / Almaty explorer | Locked, 0 progress | No confirmed summit, completed-hike or unique regional-visit tracking |
| Prepared hiker | TripSafety emergency name/phone + at least one downloaded map | 0–2 preparation steps; unlock timestamp stored only when both are present |

Favorites uses the existing BookmarkCategoriesActivity. My routes lists existing categories
containing tracks and opens BookmarkListActivity. Empty favorites/routes show explanatory states
with an action to the same existing collection. Hike history has an honest empty state and a
route-planning action. Safety settings edits the existing TripSafety profile and opens the existing
Trip/SOS screen. My Content is omitted because no such content system exists.

## Appearance

The screen has a scrollable branded header, avatar/identity, responsive statistics grid,
grouped menu, local vector achievement artwork, and a shared bottom navigation component.
Phone grids have two columns; wider screens have four. Natural text heights handle long strings
and larger fonts. System bars, cutouts and keyboard insets are applied. Profile semantic color
resources have light and values-night variants without recoloring unrelated HOME/Guides screens.
Night tokens reuse AreaMap colors; light mode uses a darker green for text contrast.
RU and EN resources have the same 56 keys. No screenshot identity, ratings or counters are copied.

## Verification

| Feature | Data source | Action | Verified |
| --- | --- | --- | --- |
| Profile tab / Guides / Trip entry | MwmActivity, TripSafetyFragment | Open AreaMap Profile | Source inspected; device click not run |
| Edit text fields | SharedPreferences | Save identity, restore editor draft | Repository host checks passed with test adapters; device recreation not run |
| Avatar | Photo Picker, private storage | Bounded worker import | Failure/retention host checks passed; actual picker and image rendering not run |
| Share | Android ACTION_SEND | Open Sharesheet | Source inspected; device not run |
| Settings / Back | SettingsActivity, activity stack | Return to Profile/caller | Source inspected; device not run |
| Favorites / routes | BookmarkManager | Existing collections / track categories | Resource/source checks passed; JNI/device not run |
| Safety | TripSafety.Profile | Save contacts / open Trip Safety | Source inspected; device not run |
| Achievements | Evidence-based model | Locked/progress/detail page | Three JUnit model tests passed; host aggregation checks passed |
| OSM account | OsmOAuth | Existing profile/login | Existing flow preserved; device not run |
| Light/dark, fonts, orientation | Qualified semantic colors, responsive layouts | Recreate/layout | XML/Java references checked; screenshots/device not run |

Java 17 syntax parsing: PASS (seven production Java files).
XML parsing, new resource references and RU/EN parity: PASS.
Model JUnit: PASS (3 tests).
Repository host checks: PASS (test doubles for Android persistence and SDK adapters; not runtime verification).

Final build attempted: `cd android && ./gradlew :app:assembleFdroidDebug -Parm64`.
Blocked before project configuration by downloading Gradle 9.6.0 from services.gradle.org:
`java.net.SocketException: Network is unreachable`. Android compilation, aapt resource compilation,
APK generation and on-device visual/back-stack checks remain unverified. No repeated retry loop.
