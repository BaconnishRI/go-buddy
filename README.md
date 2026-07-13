# Go Buddy

A free Pokémon GO companion app for Android. Track your buddies and
power-up goals: how much candy, stardust, walking, and time it will really
take to max out a Pokémon and make it your Best Buddy.

**Go Buddy is a free, noncommercial fan project.** It is not affiliated
with, endorsed by, or supported by Niantic, Nintendo, Creatures Inc., or
GAME FREAK. Pokémon and Pokémon GO are trademarks of their respective
owners. No assets from the game are included, and the app never touches
the game or your account.

## What it does

- **Finds a Pokémon's level and IVs** — scan a screenshot (or use the
  floating overlay while playing) and Go Buddy reads the CP, HP, and
  appraisal bars to work out the exact level and IVs.
- **Plans your power-ups** — exact stardust, candy, and Candy XL costs to
  your target level, with Shadow, Purified, and Lucky pricing.
- **Plans your walking** — km per candy for every species, distance left
  at normal vs. excited mood, and days at your own pace.
- **Tracks Best Buddy progress** — hearts, tiers, and time estimates, with
  one-tap heart logging.
- **Tracks Hyper Training** — Bottle Cap stat progress and the CP payoff
  when maxed.
- **Plans the whole quest** — order your Pokémon by priority and see what
  everything costs end to end.
- **Keeps your data yours** — everything stays on your phone; export and
  import a backup file whenever you like. No accounts, no analytics, no
  network access.

## Screenshots

_Coming soon._

## Install

Download the latest APK from the
[Releases page](https://github.com/BaconnishRI/go-buddy/releases) and open
it on your phone. Android will ask you to allow installs from your browser
the first time. Every release is signed with the project's key, so updates
install cleanly over older versions.

## Build it yourself

1. Install [Android Studio](https://developer.android.com/studio) (free).
2. Clone the repo and open the folder in Android Studio:

   ```bash
   git clone https://github.com/BaconnishRI/go-buddy.git
   ```

3. Press ▶ to run on a device or emulator, or build an installable APK
   from the command line:

   ```bash
   ./gradlew assembleDebug
   # -> app/build/outputs/apk/debug/app-debug.apk
   ```

To build a signed release, copy `keystore.properties.example` to
`keystore.properties`, point it at your own keystore, and run
`./gradlew assembleRelease`.

## License

[PolyForm Noncommercial 1.0.0](LICENSE.md) — use it, change it, share it,
but don't sell it. This keeps Go Buddy free for the community, always.

Required Notice: Copyright (c) 2026 BaconnishRI
