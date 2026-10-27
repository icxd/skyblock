# Dungeon runs

What happens from entering a dungeon to leaving it, as on Hypixel. Everything here was taken from
Replay Mod recordings of Entrance runs on Hypixel (and, for the boss room, three Floor I fights),
decoded with `tools/replay`, unless it says otherwise. The code is in `paper/src/main/java/net/icxd/dungeons/dungeons/instance`
(`DungeonRun` for the stages below, `RunManager` for worlds and players) and the proxy's
`dungeon` package.

## Stages

**Entering.** `/joininstance CATACOMBS_ENTRANCE` (or `/joindungeon e`) from the party leader. The
party sees "Alice entered The Catacombs, Entrance!" between blue rules and goes to a dungeon
server, which builds the floor in a world of its own (players wait on a glass platform above it
meanwhile). The leader needs Combat XV (the Catacombs Gate's "Combat Skill 15."); below it Mort
says "Wanderer!", "Save your own skin, seek challenge elsewhere first." and "You need Combat Level
XV before leading a dungeon." (MCW), and nobody goes. Staff and players on a Sandbox profile lead
in whatever their Combat level (ours: a sandbox is for trying things).

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
  share of the floor completed, with Hypixel's in-run score; see Score), then the other members
  (`[B] Name 1,234❤`) or "Solo". The tab list shows each member's class and level, and Opened and
  Completed Rooms in cells, as the score counts them. A room behind the entrance door, a wither
  door or the Blood Door is opened when that door opens (3 as the run starts), any other when
  someone walks in, as both recordings have it.
- Damage dealt, kills and deaths are counted for EXTRA STATS and the tab list, and deaths for the
  score.
- Dying makes you a ghost (see Deaths and ghosts).
- Your class's stats count once it starts; if nobody else plays your class they're doubled, with
  "Your Berserk stats are doubled because you are the only player using this class!" and a line per
  stat (see Classes).

**Doors and keys** (`RunDoors`, `DoorAnimation`).
- Wither doors are coal blocks and the Blood Door red terracotta, 3x4x3 across the gap between the
  rooms like the entrance door; normal and fairy doors are open from the start.
- Every door opens the same way: each block becomes a falling block riding an invisible bat (sent
  as packets, so only the run sees them), 0.65625 below the block; the bats sink 0.3125 a tick
  from the 5th tick, the doorway is barrier until the 12th, and they're gone on the 22nd.
- A key drops from the last starred mob (or the miniboss) of the room before its door, with the
  rest of that room's loot (see Room mobs; the fairy room is skipped, going back one more room). A
  key room with no starred mobs (the entrance room, when the fairy room comes first) has it waiting
  from the start, on the floor near the middle of its first cell at about doorway height. Either
  way it's a floating head with "Wither Key" (dark grey) or "Blood Key" (red), on two invisible
  armor stands 0.71875 and 0.46875 below the floor. The head turns 2 degrees a tick and bobs 0.01 a tick
  between 0.18 below and 0.22 above where it started, pausing a tick at each end; the name stays
  still. Walking into it picks it up for the team: "Name has obtained Wither Key!" and the RIGHT
  CLICK hint, to everyone.
- Right-clicking a shut door with the key opens it: "Name opened a WITHER door!", or "The BLOOD
  DOOR has been opened!" and "A shiver runs down your spine...". Without: "You do not have the key
  for this door!". Each key opens one door.

**Secrets** (`RunSecrets`, `SecretData`, `SecretEvents`).
- Where each room's secrets are comes from the private data folder, `rooms/_secrets/<room id>.json`
  (Skyblocker's and BetterMap's waypoints in the capture frame, with how many secrets Hypixel counts
  for the room; `RoomLibrary` skips folders starting with `_`). `RoomFrame` turns them with the room.
- When the run starts, every secret lever is switched off (a floor lever facing west, as Hypixel
  sends it) and the chests Hypixel has out from the start are put out (those behind levers, and the
  ones recorded so). The rest come when someone first walks into the room: chests (facing as
  recorded where they were seen, else towards an open side), Wither Essence heads (the profile the
  mods know them by, with the Wither Key's skin), Redstone Key heads, items dropped on the floor, and
  bats (100 health, a secret for whoever kills one).
- A chest holds a blessing 3 times in 4 (as the four recorded ones did), level I or II: it stays
  open, five harp notes play, a second later its name floats over it and "DUNGEON BUFF! You found a
  Blessing of Stone I!" follows. Otherwise it's a plain "Chest" menu with a dungeon item in the
  middle, that only gives; what's left in it falls out when it closes. Opened again: "This chest
  has already been searched!". On the Entrance every item secret is a Defuse Kit, as the three
  recorded were; other floors pick one of the wiki's evenly.
- An item counts when it's picked up ("You don't have enough space in your inventory to pick up
  this item!" if it doesn't fit); a Wither Essence when it's right-clicked (an essence for everyone,
  on the profile they play on); a bat when it's killed (a blessing or an item for the killer); a
  Redstone Key when its head, once taken, is put on the room's Redstone Node (its redstone block).
- A lever: "You hear the sound of something opening...", and where its wall was recorded (Tic Tac
  Toe's, Long Hall's) the wall sinks into the floor like a door. Again: "This lever has already been
  used.".
- The action bar shows `N/M Secrets` for the room you're in (ten spaces after the mana), the tab
  list the team's secrets, Discoveries (secrets and crypts) and the share of the floor's secrets
  (19%, 4.8%), and EXTRA STATS the secrets found. `totalSecrets()` and `secretsFound()` are the
  run's `ScoreCounts`: the score's explore part and the experience over the floor's share read them.

**Blessings** (`Blessing`, `RunBlessings`). Power, Wisdom, Stone, Life (and Time, from the Quiz on
later floors): each level adds a flat amount and multiplies the stat (Stone I: +4 and x1.02
Defense, +6 Damage), on every member's stats until they leave; levels of one kind add up, and from
Floor III they're 20% stronger. The tab list footer lists them under "Dungeon Buffs" (always Power,
Wisdom, Stone, Life, as recorded). One out of a chest or a bat comes with "DUNGEON BUFF! You found a
Blessing of Life I!" and the "Granted you ..." lines; one picked up where a room's last mob died
also with the run's time ("(14s)").

**Deaths and ghosts** (`Ghosts`, `GhostEvents`, `ReviveStones`, `Fairies`; no recording has a death,
so this is the wiki's (MCW Ghosts, Revive Stone, Catacombs) with the mods' message patterns).
- At 0 health you don't die: "☠ You were killed by Zombie Grunt and became a ghost." (the others
  see your name), or "... died to a trap", "... fell into a deep hole", "... burnt to death" and
  so on. Leaving the server while it runs does the same ("... disconnected from the Dungeon"), and
  you're a ghost when you're back ("... reconnected.").
- A ghost flies (without going through blocks), is invisible to the others but for a head that
  follows it, can't be hurt or targeted, doesn't push mobs or stop projectiles (they fly through
  it), can't hit, open, pick up or drop anything, and can only kill fairies; its abilities still in
  flight when it died don't hit.
- It comes back ("❣ Alice was revived!", full health and mana): by itself after 15 seconds on the
  Entrance (45 on Floor 1, 100 on Floor 2, never later); when a teammate right-clicks a Revive
  Stone (5 seconds later, next to them; with several dead, a menu to pick one; with none, "There
  are no players available to revive right now!"; one stone per ghost, and it's given back if the
  ghost is back another way first); or when it, or a teammate, kills a fairy. A
  Revive Stone in your inventory when you die brings you straight back. Revive Stones go when you
  leave, and are never saved with your items.
- The Fairy Room has four fairies (Mari, Nymira, Zana, Q'ara, "[Lv0] Mari 4❤") floating about.
  Any hit kills one: it revives the ghost who hit it, or the first dead teammate, or gives a Revive
  Stone ("You killed me! Take this Revive Stone so that my death is not in vain!").
- Tab list: a ghost's class shows "DEAD"; "Downed" names the first ghost and "Time" its seconds
  until it's back; "Team Deaths" counts every death; "Revive Stones" what each member carries.
- The run fails when everyone still in it is a ghost (on floors whose ghosts come back by
  themselves, not while they're waiting to: a solo Entrance gets its 15 second revive), or after an
  hour: the summary as usual without "Defeated ...", Speed the share of rooms cleared instead of the
  time's, and the score 30% lower (the wiki's rules; no failed run was recorded). Its experience
  follows that score, without Bits or the day's bonus, and it isn't a completion (MCW Dungeoneering:
  failed runs give Catacombs experience but don't count for the bonus).

**Classes** (`ClassBonus`, `ClassDetails`, `RunClasses`, `ClassEvents`).
- Each class's stats at its level: base plus what the levels add, fitted to the one level of each
  that was recorded (Healer Vitality and Mending, Mage Intelligence and Ability Damage, Berserk
  Melee Damage and Walk Speed, Archer Arrow Damage and -25% melee, Tank Health, Defense and
  Vitality, and the passives' numbers). They're in the Ready Up menu, and count once the run
  starts (for a member who gets there later, once they're there): melee and arrow damage as
  multipliers on hits, the rest as stats (the Tank's Protective Barrier multiplies Defense, the
  Berserk's Indomitable adds a share of Strength as Defense). A class played by only one of those
  playing is doubled, with the recorded lines.
- Right-clicking a class in Ready Up opens its Class Details (passives, Dungeon Orb abilities,
  ghost abilities), as recorded, at your level. Mort gives a Dungeon Orb to anyone without one.
- Abilities: right-click the Dungeon Orb (or ctrl+drop) for the class ability, left-click it (or
  drop) for the ultimate; in a run the drop key never drops what you hold. The Berserk's are as
  recorded: Throwing Axe ("Used Throwing Axe!", your held item flies ahead and hits the first mob
  for your highest hit in the last minute (the lore's words: the one recorded throw did 90% of
  it, so the real rule is unknown), "Your Throwing Axe hit 1 enemy for ... damage.", 10
  second cooldown, "Throwing Axe is now available!") and Ragnarok (+100 Attack Speed, +400 Speed,
  1.5x melee for 15 seconds, 60 second cooldown; no minions), with "Ragnarok is ready to use! Press
  DROP to activate it!" 23 seconds in and every 30 seconds while it's unused. Bloodlust: the next
  hit within 5 seconds of a kill does 35% more, and every hit heals 3% of missing health (the
  lore's second off Throwing Axe isn't there: the recorded axe is always back after 10 seconds).
- The others follow their Class Details lore, with the Berserk's messages: Archer Explosive Shot
  (3 arrows that blow up for your highest hit within 4 blocks) and Rapid Fire (5 arrows a second
  at 75% of it), Tank Seismic Wave (20,000 +10% per 50 Defense to every mob along 12 blocks) and
  Castle of Stone (70% less damage from hits for 20 seconds, and mobs around go for you; the Blood
  Room's undead within 10 blocks for as long as it lasts), Healer
  Wish (everyone to full health; no shield). Healing Circle and the Mage's aren't built.

**The Magical Map** (`RunMap`), pixel for pixel Hypixel's: rooms 18 pixels (16 on 6-wide floors)
with 4 between, centred. A room shows once someone walks into it (the entrance from the start),
and each room behind its doors as a grey cell with a question mark. Doors are 7 pixels wide in the
gap, in the colour of the room they lead into (black for a shut wither door, red for the Blood
Door). Rooms with nothing to clear get their green tick when found (the fairy room), and the Blood
Room once the Watcher is done and the run's summary is out (see Ending). A cleared room gets a white tick, green once its secrets are all
found (the wiki). You're a green arrow, the others blue.

**Room mobs** (`RoomMobs`; research mobs.md, critic.md 3.3-3.4). The rooms' data is Hypixel's, so
it's in the private data with the captures (`rooms/_mobs/<room>.json`, in each capture's frame:
`RoomSpawnData`, `RoomFrame`).
- Every normal and champion room has its mobs from the start: the ones recorded on Hypixel (12
  rooms, though only Catwalk, Crypt, Default, Diagonal, Dragon and Red Green have captures, so
  only those are ever in a run; `RoomSpawnDataTest` lists the others), else planned
  (`FallbackSpawns`, fitted to the recordings: 5 to 16 starred mobs a
  square in groups of 2 to 6 on the main floor, kinds as often as recorded, a Lost Adventurer or
  Angry Archaeologist near the middle of a champion room). Planned mobs only go where someone
  can walk to from the door the room is entered by (a block up, any way down, ladders, water;
  nothing opened on the way), so none is sealed in a hidden part where it would keep the room,
  and its key, from ever clearing. Modifiers are rolled for each run. The mob kinds have the
  Entrance's and Floor I's variants (MCW's Floor I numbers, which match the three Floor I
  recordings' base health and Combat XP); on later floors the rooms have no mobs and clear when
  they open (the log says which kinds are missing, once each).
- Floor I (`FloorMobs`): the rooms' data is the Entrance's, and the three Floor I runs (887 room
  mobs) have its kinds in the same shares but for the skeletons. Where the Entrance has Skeleton
  Grunts and Scared Skeletons, Floor I has Skeleton Grunts, Skeleton Soldiers (leather, a bow),
  Scared Skeletons and Snipers (standing still, the Sniper's head) 72 : 102 : 61 : 10, so each such
  spot takes one of those that often (which spot gets which is unknown). A level the floor doesn't
  have becomes its first (the Entrance's Lv80 Angry Archaeologist is a Lv90).
- They wait at their base health, not moving, until their room opens (its door is opened, someone
  walks in, or someone hits one of them). Then they wake up with the room's multiplier on their
  health and damage: 1.05 for the first room, then 1 + 0.05 x (squares opened before, the
  Entrance not counted, - 1), as recorded.
- A room is cleared when its starred mobs are dead: its tick on the map, the sidebar's Cleared,
  a `RoomClearedEvent`. Where the last one died its loot lies, half a block apart: a level V
  blessing (rooms bigger than 1x1 and champion rooms, as recorded), the next door's key, and a
  Superboom TNT (two in three) or a Revive Stone. Walking into them picks them up ("Name has
  obtained Superboom TNT!"), into the inventory if there's room.
- Skeleton skulls (the capture's, else the recorded ones, else 1 to 7 a square) rise for 1.65
  seconds and become Undead Skeletons 6 to 28 seconds after their room opens, now and then with a
  lightning bolt on the rising skull; a killed skeleton leaves one that does the same 15 to 23
  seconds later.
- Superboom TNT (right click on a block) blows up the tombs and weak walls next to where it goes
  off: the recorded explosion sounds and squid ink, and the tomb's Crypt Undead; once that's killed
  the crypt counts (`cryptsBlown()`, the tab's Crypts), as the bonus score does on Hypixel. Tombs
  are known where they were recorded or Skyblocker marks them; weak walls are the cracked stone
  bricks at Skyblocker's superboom marks.
- Dungeon mobs' drops go into the inventory, and what doesn't fit into the item stash
  (`ItemStash`, `/pickupstash`, with Hypixel's messages and the reminder every minute). Essence
  (Crypt Undead, Lost Adventurer, Angry Archaeologist) goes onto the profile's
  `dungeons.essence`, with "+1 Undead Essence" on the action bar.

**The Watcher** (`Watcher`, `Undead`, `UndeadType`). The Blood Door starts his fight.
- He's an invisible zombie wearing his head, floating 4 blocks over the middle of the room, with
  "﴾ ✦ The Watcher ﴿" over him and each line he says for 2 seconds over that. His lines go to chat
  as "[BOSS] The Watcher: ...", at most one every 2 seconds.
- His welcome: the Entrance's five lines as recorded, 3 to 4 seconds apart; Floor I's one line,
  "Ah, we meet again. As I foresaw..." (all three runs); other floors have the wiki's.
- The boss bar "The Watcher" (red) shows 2 seconds after the door opens, to whoever is in the
  Blood Room, and goes down a ninth per undead killed.
- The display cases (`DisplayCases`): undead heads in niches 12 blocks out from the middle, in
  front of stained glass with fire behind it, at three heights. Along two walls at -4, 0 and 4,
  along the other two near the corners at -9 and 9 (30 cases in every capture of the room; found
  from the glass, so any rotation works). As in the recordings, every case along the walls has a
  head and about a third of the corner ones do.
- He summons 9 undeads, fetching each from a case: he flies there (14 blocks a second), hovers
  0.6 s, and its head flies straight at the middle of the room, turning 30 degrees a tick. The
  first four heads leave from 5.2 s after his welcome's last line (19.8 s after the door opens on
  the Entrance, 5.4 s on Floor I, where the runs had 6 and 10.7 s), one after the other; they fly
  at 0.2 blocks a tick for 78 ticks. The next five fly at 0.3 a tick for 40 ticks; the fifth
  leaves 13.7 s after the fourth, the rest 4.5 to 7 s apart (1 to 3 s once nobody's left to
  fight). So on the Entrance the undeads appear 23.7 s after the door opens, then 1 to 2.5 s
  apart, then 11.7 s later the last five, as recorded.
- An undead appears where its head ends (1.78 higher, where the head sat on the stand) and drops
  to the floor. Each summon has a line ("Go, fight!", "Go and live again!", ...) just before, and
  each kill may get one ("Not bad.", ...). After the last summon, if any are still alive: "That
  will be enough for now.".
- Undeads have their head's skin (the named skins we have, the generic Undead one otherwise),
  random chainmail, iron or leather armour with a gold or iron axe, and "☠ Leech 20,000❤" over
  them (green, yellow under half). They run at the nearest player in the Blood Room and hit every
  second; with nobody in the room they go back to him.
- Health: the floor's base + 1,000 x (-2 to 6), Hypixel staff's formula: 12k to 20k on the
  Entrance and 15k to 23k on Floor I, both as recorded. Damage per floor from the wiki. Their
  perks (healing, teleporting behind you, exploding, more health or damage, Parasite's silverfish,
  going for a class first) only from Floor 2.
- He can't be hurt: hitting him zaps you with an elder guardian's beam, for as much as an undead
  hits.
- When the last one dies: "You have proven yourself. You may pass.", and 5 seconds later the run
  ends on the Entrance (and floors whose boss isn't here yet). On Floor I the Blood Room's portal
  opens instead (below).

**The boss room** (`BossArena`, `ArenaLibrary`, `BloodPortal`; Floor I only so far).
- Bonzo's ring isn't one of the floor's rooms: it's read once when the server starts from the
  Floor I world capture (`plugins/dungeons/worlds`, sidebar area "The Catacombs (F1)"), blocks only,
  x -72 to -14, y 55 to 147, z -40 to 49, and pasted with the floor, beside its grid as on
  Hypixel. Without a capture the server logs it and Floor I runs end with the Watcher.
- 4.5 s after "You may pass." (all three recordings) lightning strikes the Blood Room's obsidian
  frame (14 blocks round a 3x4 opening, in every captured Blood Room) and it fills with portal.
  At the same time a Blessing of Power V and one of Life V appear a block to either side of the
  room's middle. Nobody picking them up, they go by themselves after 10.1 s (10.1 and 10.3
  recorded): "A Blessing of Power was picked up!", then "DUNGEON BUFF! A Blessing of Power V was
  found! (01m 34s)" with the run's time.
- Walking into the portal takes you to the ring at -42.5 71 34.5, facing its middle. The first one
  in starts the fight, and anyone who's a ghost then is revived where they are (MCW Ghosts). The
  portal never goes to the Nether.

**Bonzo** (`Bonzo`, `BonzoUndead`; MCW for what the recordings can't show). 250,000 health in each
phase, 2,664 damage; "✰♃ Bonzo 250k❤" over him (Undead for Humanoid in his second phase;
health to a tenth of a thousand, "236.4k"), "BOSS" above that, and a red boss bar to whoever's in
the ring.
- Phase 1: three lines 4.1 s apart ("Gratz for making it this far, but I'm basically
  unbeatable." ...), standing in the middle. From 11.5 s he moves: a run (0.33 blocks a tick) or a
  leap (a block a tick) somewhere else, often up one of the seven pillars, and stands between.
  Standing, he fires wither skulls at the nearest player he can see, every 0.5 to 0.95 s; they
  speed up as vanilla's do, don't explode, and hit for 5,328 ("Bonzo's Wither Skull hit you for
  5,328 damage."). From 13.6 s he summons Undead (50,000 health, 1,440 damage), the first in the
  middle, then one every 0.4 to 4.6 s anywhere in the ring. Hit, he says "Oh noes, you got me.. what
  ever will I do?!", then one of the wiki's lines at most every 4.1 s.
- At 0 he falls over in the middle: "Oh I'm dead!" 1 s later, "Sike" 3.1 s after that, and 1.1 s
  later he's back in his second skin with his staff and full health. He can't be hurt meanwhile.
- Phase 2: he chases the nearest player and hits them; at someone more than 6 blocks off he fires
  three balloons half a second apart ("Bonzo's Balloon hit you for 1598.3999999999999 damage.",
  Hypixel's own unrounded number; "Do you want a balloon Name?" the first time). 7.1 s in and every
  33.5 s after: "SHOOOOOOOOWWWWWWW TIMEEEEEEE!", and he goes to the middle and can't be hurt while
  balloons go out every 3 ticks in every direction (0.78 blocks a tick) for 8.4 s, a line 4.1 s in,
  ten fast ones at 12.8 s, and he's back at it at 13.6 s.
- At 0 again: "Alright, maybe I'm just weak after all..", and the run ends 2.1 s later ("☠ Defeated
  Bonzo in 02m 43s"). 2 s after the summary the north pillar sinks into the floor as a door does,
  and 2 s after that the tunnel door behind it. "But my masters are a lot stronger.." and "Just you
  wait..." come 4.15 and 8.25 s after his death. He gives the killer Combat XP and his kill, as a
  mob does.

**Reward chests** (`RewardChests`, `RewardChestMenu`, the `chests` package; all three recordings, and
MCW's Dungeon Reward Chest for what they can't show).
- They appear with the summary round the ring's middle, facing it: Wood to the west, Gold to the
  east, Diamond to the north, then Emerald to the south for a score of 230 and Obsidian in the
  middle for 270 (all five in the 301 S+ recording). Each is Hypixel's model, invisible armor stands
  wearing slabs, blocks and heads, as recorded (`ChestModel`); `/spawnrewardchest <type>` builds one
  where you stand.
- Right-clicking one opens its menu ("Wood", "Gold", "Diamond") with a lever click: your loot along
  the second row, "Open Reward Chest" listing it and its cost (FREE, 25,000, 50,000, 100,000 and
  250,000 coins on Floor I, more for Bonzo's Mask or Staff, a Recombobulator or a Fuming Potato
  Book in it), Close, and "Reroll Chest".
- The loot is each member's own, rolled the first time they look, as MCW describes: the chest's
  quality (x1.05 for S+, then the best Treasure accessory's multiplier, applied twice as Hypixel's
  bug does) is spent on rows picked by weight until none fits, what's left becomes Undead Essence,
  and the chest's essence is added. The tables are MCW's, in `dungeons/chest_loot.json`. All three
  recorded chests come out of it exactly with a Treasure Talisman.
- One chest a run; a Dungeon Chest Key in the inventory lets a second open, and is used up by it.
  A Kismet Feather rerolls one chest a run (not one already opened) and is used up. Coins come from
  the purse, and what it's short from the bank.
- Opening (recorded for Wood): the menu shuts, the lid shakes for 4 s with a wooden step every
  quarter second, then it's gone, a white firework bursts on the chest (it hurts nobody), "WOOD
  CHEST REWARDS" and the rewards go to chat, and each reward stands named on a circle 2 blocks
  round the chest, starting in front, until walked over (within 1.5 blocks): items into the
  inventory, essence onto the profile with "+7 Wither Essence" on the action bar.

**Ending** (when the boss is beaten, the Watcher lets you pass on a floor without one, or `/dungeon end`, staff):
- Each member's summary between bold green rules: "The Catacombs - Entrance", "Team Score: 182
  (B)", "☠ Defeated The Watcher in 03m 55s", "> EXTRA STATS <" (click: `/showextrastats`), then
  "+3 Bits", "+131.1 Catacombs Experience" and "+126.2 Berserk Experience" (their class first,
  then a quarter of it in each other class in the party). A best score or fastest time gets
  "(NEW RECORD!)". Lines are centred the way Hypixel does it (the vanilla font's widths around
  160 pixels).
- The experience and Bits go to the profile they play on, which also counts the floor's
  completions, its best score and fastest times (S and S+ too), the runs completed today and
  the highest floor completed. A member who was disconnected at the end gets their summary and
  rewards when they rejoin, if that's before the instance closes; one whose data was already
  handed off to another server gets nothing (it wouldn't be saved).
- +0.5 s: the Blood Room counts (only now, as on Hypixel), and the map becomes "Your Score
  Summary" (the item at +0.6 s): skill, explore, speed and bonus, the grade and the total. With
  the Blood Room in it, it's more than the chat said (182 and 189 in one recording).
- +2.0 s: "Click HERE to re-queue into The Catacombs!" (`/instancerequeue`, which queues the
  party for the same floor). +10.1 s: "Warning! The instance will close in 10s." +20.2 s: everyone
  to the Dungeon Hub (a hub if there's no `DUNGEON_HUB` server). The sidebar's time stops at the
  end, and its score turns into the card's at its next update.

## Puzzles

Each puzzle room starts when someone first walks in (`puzzle/RunPuzzles`). Where things are in
each room comes from `rooms/_puzzles/<room>.json` in the private data, in the room's capture frame;
a puzzle without its file shows in the tab list but can't be done. The tab list's Puzzles lines go
" ???: [✦]", then the name half a second after someone walks in, then a green tick or a red cross
with who failed it; a solved puzzle is a cleared room with its tick on the map, a failed one gets a
red cross and isn't. Every puzzle not solved costs 10 points of skill. Once the run is over the
puzzles stop: their levers, buttons and chests do nothing, and the Tic Tac Toe AI doesn't answer.

- **Tic Tac Toe**: nine stone buttons. The AI (X) takes the middle the moment someone walks in; a
  click on a button puts an O there (the button goes and an item frame with the O map hangs in its
  place), and 3 seconds later the AI answers. It plays perfectly, so the best a player can do is a
  tie: "PUZZLE SOLVED! Name tied Tic Tac Toe! Good job!"; three in a row for it is "PUZZLE FAIL!
  Name lost Tic Tac Toe! Yikes!". The X and O maps are drawn here, red on light grey like
  Hypixel's, with the middle pixel the mods look at in the same colour.
- **Water Board**: three of the five gates close when someone walks in, and the chest appears
  behind them. The board is vanilla: the water lever lets the water out at the top, each other lever
  moves every block of its material on the board in or out (a redstone block behind each piston, as
  Hypixel does it), and water reaching a hole at the bottom toggles that hole's gate. Opening the
  chest solves it (no chat line). Any captured board works; Hypixel has four, we have two.
- **Three Weirdos**: three NPCs with a chest each, one of Hypixel's six sets of statements and
  three random names. The right chest: "PUZZLE SOLVED! Name wasn't fooled by Hope! Good job!"; a
  wrong one fails it.
- **Creeper Beams**: a creeper on a sea lantern. Shoot (or click) two lanterns: if the line
  between them goes through the creeper they turn to prismarine and stay joined by a line of
  particles. Four and the creeper blows up, taking its lantern and the pillar under it, and the
  chest that appears there is the puzzle solved (opening it gives the blessing).
- A solved puzzle gives a Tier V blessing: from its chest, the way a blessing chest opens (lid up,
  harp notes, the blessing 1.3 seconds later), or for Tic Tac Toe at once. The Teleport Maze was
  never captured, so it's kept off the Entrance.

## Score

Hypixel's formulas (`Score`), which give all four recorded Entrance numbers (chat 182 and 109,
cards 189 and 116, each part as the card shows it):
- Rooms are map cells, and the Entrance room's don't count (11 rooms on 16 cells count 15).
- Skill: 20 + 80 x the share of cells done, minus 10 per puzzle failed or never found and 2 per
  death. Explore: 60 x the share of cells + 40 x the percentage of secrets found divided by the
  percentage the floor asks for (30% on the Entrance), at most 40. Speed: 100 until the floor's
  time limit (20 minutes on the Entrance, as the mods have it), then the wiki's curve. Bonus: a
  point per crypt (up to 5), 2 for the Mimic, 10 with Paul.
- On the Entrance each part is 70% of that, rounded, so it tops out at 214 (B). Grades: D under
  100, C 100, B 160, A 230, S 270, S+ 300.
- The chat's score comes before the Blood Room counts, the card's after (see Ending).

The sidebar's "Cleared: 87% (227)" is the share of cells done, rounded, red, gold or green, and
Hypixel's in-run indicator, worked out every 10 seconds from the start: skill without its 20,
explore, the cleared share, the bonus and 2.2 per Watcher undead killed (fitted to all 21
recorded values).

Secrets, crypts and puzzles reach the score through `ScoreCounts`: secrets, crypts and puzzles are
counted (`RunSecrets`, room mobs, `RunPuzzles`: 10 off skill for each one not solved); deaths are
the members' own count.

Experience (`RunRewards`) follows the chat's score: the floor's base at 300 (55 on the Entrance)
x score/300, with 300 more score for the first five completions of a floor and 40% more for the
first five runs of the day. Repeat completions add Catacombs experience (up to 76 of them below
Floor VI), and so do secrets over the floor's share (0.5% a percent, up to Floor VI). Class
experience is the base x 1.1 with the same secrets bonus (the Floor I runs' class experience
fits it), and each member also gets a quarter of their class experience in each other class in
the party.
Those multipliers are community reverse-engineering, unverified.

## Not yet

- The bosses after Floor I, the boss bar compass. What the recordings don't
  show of Bonzo: where he chooses to go (here away from players, a pillar 2 times in 5), how often
  his aimed balloons come (here at most every 5 s), whether he heals after SHOW TIME, whether his
  Undead go when he dies, and any cap on them. Of the reward chests: what chat says for a second
  chest without a key, a chest already opened, too few coins and a reroll (the plugin's own words);
  the colour of each chest's "REWARDS" title but Wood's; which parts of the other chests' lids shake
  and go (their heads, here); the menu's layout for 1, 2 or 5+ rewards; whether teammates see your
  rewards; what happens to rewards not picked up when the instance closes (given then, here); the
  Class Milestone 2 a chest needs; Croesus; dyes, discs and shards; the stars and upgrades weapons
  and armor can come with. Floor I's Lonely Spiders (no room's data places them) and Cellar
  Spiders, and which of its skeleton spots holds which kind. Unstarred mobs in the hidden parts of
  rooms nobody recorded; crypts where no tomb is known; the Dungeon Sack. The Watcher's floor 3+ extras (reanimated bosses, Watchful Eyes), Mute's perk, and
  his real icons (placeholders for now).
- The puzzles of later floors (Higher or Lower, Boulder, Ice Path, Quiz, Ice Fill) and the
  Teleport Maze; Water Boards 1 and 2 (only 3 and 4 were captured); the Three Weirdos' skins.
- Ghosts' abilities but Haunt, the Tank's and the Spirit items' (ABILITIES_UTILITY.md, "Dungeon ghosts");
  the Healer's revives, Healing Circle and Wish's shield; the Mage's abilities; Lust For Blood and Weapon
  Master's extra targets; Ragnarok's minions.
- Stars on the score card, Catacombs and class level-up messages, the "Creating instance..." and
  "Undersized party!" menus, the Catacombs Gate menu in the Dungeon Hub.
