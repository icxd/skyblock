# Dungeon runs

What happens from entering a dungeon to leaving it, as on Hypixel. Everything here was taken from
Replay Mod recordings of Entrance runs on Hypixel (decoded with `tools/replay`), unless it says
otherwise. The code is in `paper/src/main/java/net/icxd/dungeons/dungeons/instance`
(`DungeonRun` for the stages below, `RunManager` for worlds and players) and the proxy's
`dungeon` package.

## Stages

**Entering.** `/joininstance CATACOMBS_ENTRANCE` (or `/joindungeon e`) from the party leader. The
party sees "Alice entered The Catacombs, Entrance!" between blue rules and goes to a dungeon
server, which builds the floor in a world of its own (players wait on a glass platform above it
meanwhile).

**Waiting** (up to 2 minutes).
- Everyone arrives 4 blocks from the room's centre towards the back, at y 76.5, facing the door,
  and drops onto the raised back of the room (no fall damage before the start). The door is shut:
  its doorway is filled 3x4x3 with infested chiseled stone bricks.
- Active potion effects are cleared, with Hypixel's "You are not allowed to use Potion Effects
  while in Dungeon..." message (only if there were any). Effects are per server here, so the ones
  from the hub are still there when they get back.
- Mort stands 11 blocks from the centre towards the door, facing the room, with "Mort" and
  "CLICK" above him. Clicking him opens **Ready Up**: the party's heads along the top (slot 4
  alone, 2-6 for five), the ready toggle, the five classes (left click picks one; the one you have
  glows) with who picked each underneath, and the auto ready up toggle.
- Sidebar: `[B] Name [Lv20]` per member (red until ready, then green), and "Auto-closing in:
  1:57". Tab list: Party / Player Stats / Dungeon Stats / Account Info, with "(EMPTY)" for classes.
- "Warning! This instance will close in 30 seconds if it isn't started!", then at 15, 10, 5, 4, 3,
  2 and 1 seconds; at 0 everyone goes to the Dungeon Hub.
- Auto ready up (`/togglereadyup`, or the menu toggle; saved per player) readies you 2 seconds
  after you arrive.

**Starting.** Once everyone who's here is ready: "Starting in 4 seconds." ... "Starting in 1
second." (the sidebar shows "Starting in: 0:04"). Anyone readying down stops it.

**Running.**
- Mort: "Here, I found this map when I first entered the dungeon.", 2 s later "You should find it
  useful if you get lost.", 1.5 s after that "Good luck."
- The Magical Map goes in hotbar slot 9 (it's blank until clearing is done). Whatever was there
  moves to a free slot. Run items are never saved to the player's stored inventory.
- The door turns to barrier 0.2 s after the start and opens 0.8 s after it.
- Sidebar: Keys, Time Elapsed, Cleared (with the score), then the other members
  (`[B] Name 1,234❤`) or "Solo". The tab list shows each member's class and level.
- Damage dealt, kills and deaths are counted for EXTRA STATS and the tab list.

**Ending** (for now only `/dungeon end`, staff):
- The summary between bold green rules: "The Catacombs - Entrance", "Team Score: 84 (D)",
  "☠ Defeated The Watcher in 01m 49s", and "> EXTRA STATS <" (click: `/showextrastats`). Lines
  are centred the way Hypixel does it (the vanilla font's widths around 160 pixels).
- The map becomes "Your Score Summary": skill, explore, speed and bonus, the grade and the total.
- +2.1 s: "Click HERE to re-queue into The Catacombs!" (`/instancerequeue`, which queues the
  party for the same floor). +10.1 s: "Warning! The instance will close in 10s." +20.2 s: everyone
  to the Dungeon Hub (a hub if there's no `DUNGEON_HUB` server). The sidebar stops at the end.

## Score

Skytils' formulas (`Score`), which the mods use to predict Hypixel's: skill 20 + 80 x rooms
cleared, minus deaths and unfinished puzzles; explore 60 x rooms + 40 x secrets (of the share the
floor asks for); speed 100 until the floor's time limit; bonus for crypts, the mimic and Paul. On
the Entrance each part counts 70%. Grades: D under 100, C 100, B 160, A 230, S 270, S+ 300.

Hypixel's own numbers don't match these exactly (its sidebar shows 0 at the start; ours shows 84
on the Entrance, the base skill plus full speed). Rooms, secrets, puzzles and crypts come with
clearing, so for now the score is the base.

## Not yet

- The Watcher, keys and doors, clearing, the map's contents, the boss bar compass.
- Deaths as ghosts, revives.
- Classes beyond picking one: their stats in the Ready Up menu, Class Details (right click), the
  Dungeon Orb, "stats are doubled because you are the only player using this class".
- XP and Bits at the end, stars on the score card, the "Creating instance..." and "Undersized
  party!" menus, the Catacombs Gate menu in the Dungeon Hub.
