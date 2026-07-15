# Elendheim Overwatch Challenges

Tap to roll. The app hands you an Overwatch hero and a constraint to play
under that game. Some rolls are warmup-serious, some are pure chaos, and a
toggle keeps the two apart so a comp warmup never hands you "no shooting".

Dark mode only, no accounts, no network, nothing to set up. Open it, roll,
queue.

## How a roll works

- Pick a role (or leave it on any) and pick a pool: Warmup, Mixed, or Chaos.
- Tap roll. You get a hero and a challenge that fits their role.
- Role-specific pools exist too: supports can get triage rules, tanks get
  space-making rules, and neither leaks onto the wrong role.

## The modes

- **Mutate** keeps the hero and rerolls just the challenge. Hate the
  constraint, not the pick? Mutate it. A counter tracks how many times you
  chickened out, and resets on the next roll.
- **Escalate** stacks a new constraint on top of what you already have. Keep
  going until you break. Swipe an added constraint away if it turns out
  unplayable; the first one is locked in.
- **Stakes** rolls a punishment alongside the challenge. Fail the challenge
  and the punishment applies to your next game. Toggling stakes off and on
  brings back the same punishment; it only changes when you reroll.
- **Squad sync** gives everyone who enters the same word the same roll
  sequence, so the whole group shares one chaos theme. Filters and hero bans
  have to match and everyone taps in the same order. Lives in settings.
- **Hero bans** live in settings too: the full roster is listed by role, and
  tapping a hero removes them from the roller until you tap them back in.
  Bans stick between launches.
- **??? rolls**: about one roll in fifty skips the hero entirely and hands
  you a wildcard constraint that decides who you play. There's a switch in
  settings if you'd rather every roll land on a hero.
- **No Challenge Mode**: rolls hand you a hero and nothing else. Escalate
  deals the constraint the roll was holding back, locked in like any opener.
  Lives in settings.
- **Rule packs**: bundle your own constraints into named packs and flip them
  on per session. Tag rules Warmup or Chaos to join those pools, or invent
  your own tags, which roll in Mixed. The standard pool has its own switch,
  so you can run built-ins, packs, or both.
- **Accessibility**: text size, high contrast, reduce motion and spin length
  all live in their own settings page. Settings is organized into sections:
  squad sync, challenges, hero pool (with live active counts you can hide),
  and accessibility.
- **I died**: one big button under the card. Tap it when you die and the
  stack escalates automatically, with a death counter keeping score.

## Building it

Open the project in Android Studio and run it, or from the command line:

```
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. Min SDK is 26 (Android 8.0).

Tagged releases on GitHub carry a ready-built APK if you just want to install
it.

## Expanding it

The challenge pool is one list in
`app/src/main/java/com/elendheim/overwatchchallenges/data/ChallengePool.kt`.
Add an entry with a category, an intensity tag, and optionally a role
restriction, and the roller, the mutate/escalate loop, and the pool filters
all pick it up automatically. New heroes go in `Roster.kt` the same way.

## License

MIT. See [LICENSE](LICENSE).
