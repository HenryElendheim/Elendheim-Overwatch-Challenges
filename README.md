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
  constraint, not the pick? Mutate it.
- **Escalate** stacks a new constraint on top of what you already have. Keep
  going until you break.
- **Stakes** rolls a punishment alongside the challenge. Fail the challenge
  and the punishment applies to your next game.
- **Squad sync** gives everyone who enters the same word the same roll
  sequence, so the whole group shares one chaos theme. Filters have to match
  and everyone taps in the same order.

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
