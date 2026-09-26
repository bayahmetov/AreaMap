# AreaMap — Android trip-safety prototype

AreaMap is an independent fork of Organic Maps. Organic Maps supplies the map,
offline maps, routing and native track recorder; AreaMap adds the trip plan,
local check-in policy and SOS information card. Existing licenses and credits
are retained. This is the first implementation stage, not a rescue dispatch service.

## Open and build

In an existing checkout, save/commit your own changes before switching branches:

```bash
git fetch origin
git switch feature/areamap-trip-safety
cd android
./gradlew :app:assembleFdroidDebug -Parm64
```

In Windows Git Bash, the same commands work. In Android Studio open `android`,
sync Gradle, select `fdroidDebug`, and Run on the phone. Follow
[INSTALL.md](INSTALL.md) for SDK, NDK and native dependencies.

The app label is **AreaMap Debug** and the application ID is `app.areamap.debug`.
It installs separately from Organic Maps; previously downloaded maps and tracks
are not automatically copied. The JNI/source namespace stays `app.organicmaps`.
The APK permissions allowlist was updated for the new application ID.

Open the map menu → **AreaMap · Поход / Trip safety**.

## Implemented scenario

1. Enter a route/plan, group size, callback/contact and expected duration.
2. Start the trip and grant precise location. Notifications are requested on
   Android 13+; the screen warns when notifications are disabled.
3. The existing `TrackRecordingService` maintains recording in the background.
   AreaMap consumes its location callbacks, without a second GPS provider.
4. See the last accepted coordinates, precision, measurement time and age.
5. Confirm **I am OK**, or take a **15-minute rest**. Recording continues on rest.
6. After continuous stationary GPS evidence, receive a local check-in. No reply
   changes the local state to **unanswered**; nothing is transmitted automatically.
7. SOS opens an offline card with the plan, group and last recorded position.
   The dial button opens `ACTION_DIAL` for 112; sharing opens the system chooser.
   Neither action is claimed to confirm delivery or summon rescuers.
8. Finish the trip: stop checks and save a nonempty track in **Bookmarks and tracks**.
   An existing independent recording must first be stopped before starting a new trip.

English and Russian strings come from `data/strings/strings.txt`. Other UI
languages fall back to English for these new strings. The separate demo uses the
same engine with short intervals and synthetic points; it shows stop, unanswered,
acknowledged and GPS-unavailable outcomes without modifying the real trip or
making calls/messages.

## Local policy and persistence

- Only finite coordinates in valid ranges, accuracy `(0, 50]` metres, and fixes
  no older than 120 seconds are accepted; future and out-of-order fixes are rejected.
- 15 minutes within `max(50 m, sum of anchor/current accuracy)` triggers a question.
  These are prototype constants, not validated accident-detection thresholds.
- The response deadline is 2 minutes; elapsed realtime is used for policy timers.
- A gap over 120 seconds, a timeout or disabled GPS clears stationary evidence.
  GPS loss is not interpreted as immobility. An existing question remains pending
  until acknowledged, rested or the trip is stopped/interrupted.
- The native recorder stores the full track. App-private preferences retain the
  plan and last accepted GPS snapshot (coordinates, accuracy, measurement time).
- Service stop or process death invalidates monitoring. The plan remains, but
  the screen requires explicit resumption and does not infer movement during the gap.
- The expected return time is information in the SOS card only; no return-deadline
  watchdog/server has been implemented. A force-stopped phone cannot notify anyone.
- Android/OEM battery restrictions, Doze and permission revocation can interrupt
  recording or delay checks. Physical-device background testing is required.

## Validation

Ten JUnit 4 policy tests cover continuous stops, response timeout, movement,
GPS jitter, stale/invalid/inaccurate/duplicate/future fixes, signal loss, gaps,
acknowledgement, rest and a new process. Run in a configured Android checkout:

```bash
cd android
./gradlew :app:testFdroidDebugUnitTest :app:lintFdroidDebug -Parm64
```

The policy tests were compiled with Java 17 and run directly with JUnit 4.13.2:
**10 tests passed**. Java formatting was checked with clang-format 23.1.0;
XML parsing, resource references and whitespace were checked. Full Android build
and lint were attempted but blocked while downloading the Gradle distribution
(network unavailable in the editing environment). No APK/device test is claimed.

Before merging, build fdroidDebug and check on a physical phone:

- Start outdoors, lock for several minutes, reopen; coordinates/track advance.
- Stay still for 15 minutes; notification opens the trip; acknowledge/rest clear it.
- Deny notifications: visible warning. Deny precise location: no recording start;
  the SOS card still opens. Disable location: unknown position, not a new stop alert.
- Stop recording via the native recording notification: trip reports interrupted.
- Kill/restart the process: old plan remains and explicit resume is required.
- Finish: native track is saved; new trip has no old GPS snapshot.
- Rotate with an unfinished form; test light/dark themes and large font settings.
- Open SOS on a phone with no dialer/sharing app; no crash. Do not call 112 for tests.
- Toggle airplane mode: card and maps (if downloaded) remain available; the app
  does not claim transmission, delivery or fresh coordinates without evidence.

## Next implementation stages

- Backend-acknowledged trip registration, offline outbox and a server deadline
  worker; coordinator dashboard with last-received-point age and event states.
- Real recipient authentication/access control and a confirmed delivery channel.
- Reviewed offline emergency guide bundled in the APK.
- Permission-cleared, verified GPX routes with sources and date/variant metadata.

These blocks are not wired into this PR; no Supabase secrets, invented routes,
or unreviewed medical algorithms are shipped.
