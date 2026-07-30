# struct

GPS activity tracking, route building, and route following — a free, no-login, no-subscription
Strava-alike. Local-first: everything lives in Room on-device. Maps are OpenStreetMap via
osmdroid (no Google Maps, no API key).

## Stack
- Kotlin + Jetpack Compose, Material3, Navigation-Compose
- Room (local DB, no backend)
- osmdroid (OSM tiles, no key)
- Plain `android.location.LocationManager` for GPS (no Google Play Services dependency)
- OSRM public demo server for optional road-snapped route building (`router.project-osrm.org`)
- Vico for the elevation profile chart
- "Liquid glass" UI kit (`ui/components/GlassComponents.kt`) + Inter typography

## What's implemented
1. **Recording** (`TrackingScreen` + `LocationTrackingService`) — foreground service, start/pause/
   resume/stop, auto-pause under ~1.4 km/h, manual lap markers, live distance/pace/time/elevation.
2. **Route builder** (`RouteBuilderScreen`) — tap to add waypoints, freehand or OSRM road-snapped
   mode, undo last point, reverse direction, save.
3. **Library** (`LibraryScreen`) — list/rename/delete saved routes, GPX import/export.
4. **Follow mode** (`FollowRouteScreen`) — live position on the target route, off-route alert
   (>35m from the line), distance remaining/completed, rough ETA, bearing-to-next-waypoint arrow.
5. Post-activity **summary** — route polyline, stats, elevation chart, splits.

## Fonts
Real Inter, committed directly at `app/src/main/res/font/inter_variable.ttf` — the actual OFL
`Inter[opsz,wght].ttf` variable font from `google/fonts`. Weights (Regular/Medium/SemiBold/Bold)
are pulled from the one file via Compose's `FontVariation` weight axis rather than needing
separate static files, since Google dropped the old per-weight static files from that repo.
The file was verified as valid sfnt/TrueType data before being committed — an earlier version
fetched fonts unverified at CI build time via `curl`, and a silent download failure (the URL had
gone stale) bundled a broken font file that crashed the app on every single launch. Lesson
learned: don't pull binaries into the build unverified.

## Gradle wrapper
`gradlew`/`gradlew.bat` aren't committed (couldn't generate the wrapper binary jar in this sandbox
— no network access to `services.gradle.org` from here). Two options:
- Easiest: install Gradle 8.7 locally (`brew install gradle` / `sdk install gradle 8.7`) and run
  `gradle wrapper` once inside the repo — that generates `gradlew`/`gradlew.bat` for you, commit
  them, and CI will happily use `./gradlew` from then on too.
- Or just keep building with a system `gradle` install, same as CI does.

## CI
`.github/workflows/build-apk.yml` runs on every push to `main`: builds `assembleDebug` with
Gradle 8.7 on the GitHub-hosted runner (Android SDK is preinstalled on `ubuntu-latest`), and
uploads the APK as a workflow artifact — grab it from the Actions run's "Artifacts" section.
Trigger it manually anytime via the "Run workflow" button (`workflow_dispatch`).

## Known simplifications / next steps
- **OSRM**: the public demo server is rate-limited and technically driving-only; road-snap mode
  requests the `foot` profile which the demo may reject — self-host OSRM if you want this to be
  reliable in production. Freehand mode has no such dependency.
- **Elevation on the builder screen**: `routing/ElevationClient.kt` (Open-Topo-Data) is wired but
  not yet called from `RouteBuilderScreen` — hook it up if you want a projected elevation profile
  before you've actually walked/ridden a built route.
- **True backdrop blur**: `GlassCard` approximates the "liquid glass" look with translucent
  gradients + a soft self-blur; it doesn't sample what's actually behind it (stock Compose has no
  API for that). For real backdrop blur, add `dev.chrisbanes.haze:haze` and swap the background
  in `GlassComponents.kt` for a `hazeChild` modifier.
- **Offline tile caching** (`util/TileCacher.kt`) is built but not wired into Follow Mode's entry
  point — call `TileCacher.cacheRouteArea(...)` when a route is selected, if you want the map
  tiles pre-fetched for a fully offline follow session.
- No tests yet.

## App icon
Pixel-art black/orange "B" mark, generated programmatically — see `scripts/gen_icon.py`
(re-run with `python3 scripts/gen_icon.py` from repo root if you want to tweak the glyph; needs
`pillow`).
