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
- The Magical Map goes in hotbar slot 9. Whatever was there moves to a free slot. Run items are
  never saved to the player's stored inventory.
- The entrance door opens 0.2 s after the start (see Doors).
- Sidebar: Keys (`■ ✓/✗` for the Blood Key, `■ 1x` for Wither Keys), Time Elapsed, Cleared (the
  share of rooms completed, with the score), then the other members (`[B] Name 1,234❤`) or "Solo".
  The tab list shows each member's class and level, and Opened and Completed Rooms from the map.
- Damage dealt, kills and deaths are counted for EXTRA STATS and the tab list.
- Until there are ghosts, dying brings you back in the entrance room, with everything you had
  (run worlds keep inventories).

**Doors and keys** (`RunDoors`, `DoorAnimation`).
- Wither doors are coal blocks and the Blood Door red terracotta, 3x4x3 across the gap between the
  rooms like the entrance door; normal and fairy doors are open from the start.
- Every door opens the same way: each block becomes a falling block riding an invisible bat (sent
  as packets, so only the run sees them), 0.65625 below the block; the bats sink 0.3125 a tick
  from the 5th tick, the doorway is barrier until the 12th, and they're gone on the 22nd.
- On Hypixel a key drops from the last starred mob (or the miniboss) of the room before its door.
  With no mobs yet, it waits in that room from the start (the fairy room is skipped, going back
  one more room), on the floor near the middle of its first cell at about doorway height: a
  floating head with "Wither Key" or "Blood Key", on two invisible armor stands 0.71875 and
  0.46875 below the floor. Walking into it picks it up for the team: "Name has obtained
  Wither Key!" and the RIGHT CLICK hint, to everyone.
- Right-clicking a shut door with the key opens it: "Name opened a WITHER door!", or "The BLOOD
  DOOR has been opened!" and "A shiver runs down your spine...". Without: "You do not have the key
  for this door!". Each key opens one door.

**The Magical Map** (`RunMap`), pixel for pixel Hypixel's: rooms 18 pixels (16 on 6-wide floors)
with 4 between, centred. A room shows once someone walks into it (the entrance from the start),
and each room behind its doors as a grey cell with a question mark. Doors are 7 pixels wide in the
gap, in the colour of the room they lead into (black for a shut wither door, red for the Blood
Door). Rooms with nothing to clear get their green tick when found (the fairy room), and the Blood
Room when the Watcher is done. You're a green arrow, the others blue.

**The Watcher** (`Watcher`, `Undead`, `UndeadType`). The Blood Door starts his fight.
- He's an invisible zombie wearing his head, floating 4 blocks over the middle of the room, with
  "﴾ ✦ The Watcher ﴿" over him and each line he says for 2 seconds over that. His lines go to chat
  as "[BOSS] The Watcher: ...", at most one every 2 seconds.
- His welcome: the Entrance's five lines as recorded, 3 to 4 seconds apart; other floors have the
  wiki's.
- The boss bar "The Watcher" (red) shows 2 seconds after the door opens, to whoever is in the
  Blood Room, and goes down a ninth per undead killed.
- He summons 9 undeads: the first 23.8 s after the door opens, three more 1 to 2.5 s apart, then
  11.7 s later five more, 4.5 to 7 s apart (1 to 3 s once none are left alive). Each summon has a
  line ("Go, fight!", "Go and live again!", ...) and each kill may get one ("Not bad.", ...). After
  the last summon, if any are still alive: "That will be enough for now.". While summoning he flies
  between the display cases on the walls at 14 blocks a second.
- Undeads appear over him and drop to the floor: player-shaped (the named skins we have, the
  generic Undead one otherwise), in random chainmail, iron or leather armour with a gold or iron
  axe, "☠ Leech 20,000❤" over them (green, yellow under half). They run at the nearest player in
  the Blood Room and hit every second; with nobody in the room they go back to him.
- Health: 12k to 20k on the Entrance (as recorded), and the same spread around the wiki's number on
  other floors. Damage per floor from the wiki. Their perks (healing, teleporting behind you,
  exploding, more health or damage, Parasite's silverfish, going for a class first) only from
  Floor 2.
- He can't be hurt: hitting him zaps you with an elder guardian's beam, for as much as an undead
  hits.
- When the last one dies: "You have proven yourself. You may pass.", and 5 seconds later the run
  ends (there are no bosses yet).

**Ending** (when the Watcher lets you pass, or `/dungeon end`, staff):
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

- Clearing rooms (mobs, secrets, puzzles, crypts), keys dropping from mobs, the bosses, the boss
  bar compass. The Watcher's floor 3+ extras (reanimated bosses, Watchful Eyes), Mute's perk, and
  his real icons (placeholders for now).
- Deaths as ghosts, revives.
- Classes beyond picking one: their stats in the Ready Up menu, Class Details (right click), the
  Dungeon Orb, "stats are doubled because you are the only player using this class".
- XP and Bits at the end, stars on the score card, the "Creating instance..." and "Undersized
  party!" menus, the Catacombs Gate menu in the Dungeon Hub.
