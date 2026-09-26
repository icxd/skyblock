# Dungeon generation (reverse engineered)

Hypixel's generator isn't public. A dev-forum reply says as much ("the dungeons generation
algorithm isn't open-source"). So this document works backwards from what the output is known to
look like, then describes a generator that reproduces all of it. Every fact below is tagged with
how sure it is:

- **confirmed**: stated by Hypixel, or hard-coded / recorded in the data of several map mods that
  have to get it right to work.
- **inferred**: follows from confirmed facts.
- **played**: from your own play experience.
- **tuning**: not documented anywhere; picked so the maps look right.
- **captured**: seen in the 5 runs recorded with the dungeon scanner (1 Entrance, 4 F6).

The code lives in `src/main/java/net/icxd/dungeons/dungeons/generation`. `/dungeon [floor] [seed]`
prints a map like the ones below to the console and builds a block preview. `/dungeon paste
[floor] [seed]` builds it for real out of rooms captured on Hypixel (section 7).

```
F7, seed 2024. E = entrance door, W = wither door, F = fairy door, B = blood door, # = normal door.
r* = room on the critical path. S entrance, F fairy, BL blood, P puzzle, T trap, M miniboss.
+-------+-------+-------+-------+-------+-------+
|  r23  #  r22  #  r20  #  P6   |  r11  #  T9   |
+---#---+-------+-------+-------+---#---+-------+
| r*21  W r*19          W  F2   | r*12  E  S0   |
+---B---+-------+-------+---F---+       +-------+
|  BL1  |  P8   |  r15  | r*16  W       |  P4   |
+-------+---#---+       +       +       +---#---+
|  r10  #               #       |       |  r13  |
+       +-------+---#---+-------+       +       +
|       |  M5   #  r18  #  r17  |       #       |
+---#---+-------+-------+---#---+-------+---#---+
|  P7   |  r14                          |  P3   |
+-------+-------+-------+-------+-------+-------+
```

---

## 1. What the output looks like

### Grid
| Fact | Confidence | Source |
|---|---|---|
| Rooms sit on a grid of 31x31-block cells with a 1-block gap (32-block pitch). The north-west corner is at -200,-200. | confirmed | DungeonRoomsMod `MapUtils`, Skyblocker `DungeonMapUtils`, IllegalMap `utils.js` |
| Grid size: Entrance 4x4, F1 4x5, F2–F3 5x5, F4 6x5, F5–F7 6x6. Master Mode uses the same sizes. | confirmed (mods). The wiki lists F5 as 6x5 and M7 as 6x7, which the mods contradict. | Odin `DungMap.kt`/`DungeonScan.kt`, Skytils `MapUtils`, wiki *Catacombs/Floors* |
| Every cell is filled, except on floors with a special column (below). | confirmed | forum map-layout guide, all 5 captured runs |
| Room shapes: 1x1, 1x2, 1x3, 1x4, 2x2 and L (a 2x2 with one cell missing). | confirmed | wiki, every mod |
| Doors sit only in the middle of a cell edge, so every cell edge is exactly one possible door. Cells of the same room have no door between them. | confirmed | Skytils/FunnyMap scanners (11x11 "room + connector" grid), Odin `DungeonDoor` |

### Connections
| Fact | Confidence | Source |
|---|---|---|
| The rooms form a **tree** rooted at the entrance. There are no loops, and every room has exactly one way in. | confirmed, captured | IllegalMap `setupTree` builds exactly this parent/child tree. Every captured run has exactly one door fewer than rooms. |
| The **entrance has exactly one door**. | confirmed | IllegalMap `rooms.json`: entrance `doors: "1000"` |
| Blood, every puzzle, trap, yellow (miniboss) and rare room has **exactly one door**. They are dead ends. | confirmed | IllegalMap `rooms.json`, BetterMap `roomdata.json` ("D"). Patch 0.8: special rooms are "on the edges of the dungeon". |
| The fairy room can have any number of doors. | confirmed | BetterMap: fairy `"V"`, IllegalMap: no value |
| The fairy room is always on the entrance → blood path. | confirmed | 0.8 patch notes ("On the main path, you will always find…"), wiki |
| **Every 1x1 regular room has a fixed door layout**: 22 straight (I), 20 corner (L), 12 T, 8 cross (X). **None of them is a dead end.** | confirmed | IllegalMap `rooms.json` `doors` field (N,E,S,W in the room's own frame), which matches BetterMap's independently collected I/L/T/X data for all but 2 rooms |
| "Rare" rooms are 1x1 brown dead ends that show up "on very rare occasions", only when the generator "still ha[s] 1x1 spaces", usually at the map edge. | confirmed | forum guides |
| Multi-cell rooms have no recorded door layout; they can have a door on any wall and can be dead ends. | inferred + played | mod data (no field for them) |

This is the answer to "some rooms can only have doors on certain walls". Every 1x1 prefab is
built with a fixed set of doorways. The generator can only rotate it, so the door layout around a
1x1 cell decides which prefabs can go there. It also explains rare rooms: a 1x1 cell that ends up
as a leaf of the tree can't hold any regular 1x1 prefab (none is a dead end), so it gets a rare
room.

### Doors on the critical path
| Fact | Confidence | Source |
|---|---|---|
| On the entrance → blood path, the first door is the **entrance door** (infested stone), the last is the **blood door** (stained clay), and **every other door is a wither door** (coal), except the one into the fairy room. No door off the path is special. | confirmed | IllegalMap `setupTree`, wiki ("the critical path … is always marked using doors requiring wither keys") |
| The key for a door drops in the room before it. The blood key drops "in the last room before the Blood Room". | confirmed | wiki *Wither Key* / *Blood Key* |
| The **door into the fairy room needs no key**. It's pink on the map. | played + Odin (separate `Fairy` door type, drawn in the fairy room's colour) | |
| So the rooms just before the fairy and just before blood are regular rooms. The fairy room has no mobs, so the key for the door after it has to come from the room before it. | inferred | |

### Special rooms per floor
| Fact | Confidence | Source |
|---|---|---|
| Every floor: 1 entrance, 1 fairy, 1 blood, 1 yellow (miniboss) room, and at least 2 puzzles. F7 has up to 5 puzzles. Trap room from F3 up. | confirmed | wiki *Dungeons*, forum guides |
| Puzzles never repeat in a dungeon. Available puzzles: Tic Tac Toe, Three Weirdos, Creeper Beams, Water Board and Teleport Maze on all floors; Higher or Lower, Boulder and Ice Path from F3; Quiz from F4; Ice Fill on F7. Bomb Defuse was removed in 0.20.5. | confirmed | wiki *Dungeon Puzzle Rooms* |
| **On F4–F6 the map is one column narrower than its grid. The last column (x = 5) only holds the odd puzzle, trap or miniboss sticking out; the rest of it is empty.** | captured | 3 complete F6 runs: two 5x6 maps with nothing at x = 5, one with a single puzzle there and the other 5 cells of the column empty. Odin's `SpecialColumn.kt` expects specials there too. |
| Puzzles: usually 2 up to F3, more from F4 on. | played + captured | Entrance run: 3. F6 runs: 3, 4, 3. |
| The entrance is on the map border. The blood room usually is, far from the entrance. | played + captured | Entrance on the border in 5/5 runs, blood in 3/4 (once one cell in). |

The special column is the most surprising find. It suggests that on F4–F6 Hypixel generates a
5-wide dungeon and then lets a few special rooms stick out of its east side. A fan recreation made
the same point independently: the extra space on those floors is "more of an extension of the map
than a subtraction".

---

## 2. The algorithm

The generator runs five stages. Every stage can fail on an unlucky roll, and the whole attempt is
then re-rolled with the same `Random`, so a seed always gives the same dungeon. On average it
needs 1.1–1.7 attempts, which takes 3–7 ms.

### Stage 1a: special rooms (`SpecialPlacer`)
1. **Entrance** on a random border cell. **Blood** far away from it: at least 60% of the map's
   Manhattan diameter (tuning), 4x more likely on the border than inside (tuning, from 3 of 4
   captured bloods).
2. **Fairy** on a cell roughly between them (`d(E,F) + d(F,B) ≤ d(E,B) + 2`), at least 2 cells
   from each. That guarantees at least one regular room before it and one after it.
3. **Puzzles, trap, miniboss**: random cells, twice as likely on the border (tuning). On F4–F6
   the last column starts out empty; each of them goes there with a 10% chance (tuning), and the
   column cells nobody took stay empty for good. Border checks use the 5-wide part of the map.

Every placement must keep two invariants, otherwise it's re-picked:
- all non-dead-end cells stay 4-connected;
- every dead end touches a free cell.

Together these guarantee that a tree exists.

### Stage 1b: regular rooms (`Tiler`)
Fills the rest like wave function collapse:
1. Pick the empty cell with the fewest placements covering it.
2. Choose a shape by weight among the shapes that fit there.
3. Choose a random placement of that shape.

1x1 always fits, so this can't fail. It avoids:
- placements that wall a cell in against a single room (the notch of an L), since that cell could
  only ever be a dead end;
- placements where no template has any usable door, e.g. a corridor that only opens at its ends,
  with an end against the border.

It never makes more rooms of a shape than there are templates of it (rooms are unique per dungeon).

### Stage 2: doors (`Connector`). This is where the placement rules are handled.
Each room keeps a list of **options**: every (template, rotation) pair that fits its footprint and
is still consistent with the doors it has so far (`RoomNode`, `Placement`). A door can only go
through a wall that at least one option of *both* rooms allows. Opening it throws away the options
that don't allow it. Nothing commits to a template until the end, so door rules never cause
backtracking. They just steer the tree.

1. **Critical path**: a DFS from the entrance to the fairy to the blood room, through regular rooms
   only. Moves are ordered by BFS distance to the next waypoint plus noise, so the path is direct
   but not straight. It backtracks across waypoints: if the fairy can't reach blood, the first leg
   is re-routed.
2. **Growing tree** over everything else ([the maze algorithm][gt]):
   - Rooms that still *need* a door go first. That's a 1x1 whose options all need two or more
     doors and that only has its entrance so far.
   - Otherwise take the newest room (50%) or a random one.
   - A one-step lookahead rejects moves that would leave such a 1x1 with no unvisited neighbour.
   - Dead ends (puzzles, trap, miniboss) are attached **last**, preferably to a room that still
     needs a door. Attaching them early steals the only exit a 1x1 pocket had.
3. **Repair**: for a 1x1 still stuck with one door, try to move a neighbouring subtree so it hangs
   off it (swap one tree edge). That's only done when every room involved still has a template.

### Stage 2b: stranded 1x1 rooms (`Merger`)
A 1x1 regular room left with a single door has no regular template. If it and its parent together
form a valid shape, they are merged into one room:
- 1x1 + 1x1 → 1x2
- 1x2 + 1 → 1x3 or L
- 1x3 + 1 → 1x4
- L + its notch → 2x2

The merged room keeps the parent's doors, so the tree doesn't change. Anything left over becomes a
**rare room**, about 1 in 12 dungeons.

### Stage 3: templates (`TemplateAssigner`)
Each room now has one or more options that fit its final doors exactly. Picking templates so none
repeats is a bipartite matching (Kuhn's algorithm, randomised, most constrained room first). A
repeat only happens when the pool has fewer templates of a kind than the map has rooms of it.

### Stage 4: door types (`DoorTyper`)
Along the critical path: the first door is ENTRANCE, the door into the fairy room is FAIRY
(keyless), the last door is BLOOD, and every other door is WITHER. Doors off the path are NORMAL.

`LayoutValidator` checks every rule above. The tests run it over 300 seeds per floor, plus a pool
where the only multi-cell rooms are corridors that open only at their ends and 2x2s with a sealed
wall.

[gt]: https://weblog.jamisbuck.org/2011/1/27/maze-generation-growing-tree-algorithm

### Output
`DungeonLayout` holds:
- rooms, each with an id, type, shape, template, `rotation` (clockwise quarter turns), cells,
  parent, depth and doors;
- doors, each with a wall edge, parent room, child room and type;
- the critical path;
- `render()`, which draws the ASCII map.

To paste a room: rotate its schematic `rotation × 90°` clockwise (seen from above, +x east,
+z south) and put its min corner at `origin() × 32`. The door slots in its template are written in
the schematic's own (rotation 0) frame, so they line up automatically. Section 7 does exactly
this with captured rooms.

---

## 3. Templates and door rules

`HypixelRooms` holds all 139 current Hypixel rooms. It was generated from IllegalMap's MIT-licensed
`rooms.json`. 1x1 rooms carry their real door layout. Your own templates use the same builder:

```java
// 1x1 with a fixed corner layout (doors north and east in the schematic).
Room.builder().id("andesite").type(RoomType.REGULAR).shape(RoomShape.ONE_BY_ONE)
    .doorSlots(Set.of(DoorSlot.of(0, 0, Direction.NORTH), DoorSlot.of(0, 0, Direction.EAST)))
    .exactDoors(true)   // every slot must be a door
    .build();

// 1x2 that may only have doors on its short ends (any subset of them).
Room.builder().id("corridor").type(RoomType.REGULAR).shape(RoomShape.ONE_BY_TWO)
    .doorSlots(Set.of(DoorSlot.of(0, 0, Direction.WEST), DoorSlot.of(1, 0, Direction.EAST)))
    .build();

// No doorSlots = a door may be on any outside wall. maxDoors caps the count.
```

Shapes in their rotation-0 frame (x → east, y → south):
- `ONE_BY_N`: `(0,0)…(N-1,0)`
- `TWO_BY_TWO`: `(0,0) (1,0) (0,1) (1,1)`
- `L_SHAPE`: `(0,0) (1,0) (0,1)`, with the south-east cell missing

Hypixel never turns a straight room or a 2x2 around. On all 23 captured multi-cell rooms the roof
marker was north-west for horizontal straight rooms and 2x2s, and north-east for vertical straight
rooms (`rotations` 0 and 1, and 0 for 2x2, set by `HypixelRooms.rotations`). L rooms can face any
way, and every L room so far was built with its north-east cell missing.

---

## 4. Tuning knobs (`DungeonConfig`)

| Knob | Default | Effect |
|---|---|---|
| `shapeWeights` | 1x1 4, 1x2 2, 1x3 2, 1x4 4, 2x2 6, L 2 | how often each regular shape is picked (big weights on the big shapes because they often don't fit) |
| `newestBias` | 0.5 | 1 = long winding branches, 0 = bushy |
| `pathNoise` | 1.5 | 0 = shortest critical path |
| `entranceOnEdge` | true | entrance on the border (played, captured) |
| `bloodEdgeWeight` | 4.0 | how much more likely the blood room is on the border |
| `bloodDistance` | 0.6 | how far apart entrance and blood must be |
| `deadEndEdgeWeight` | 2.0 | how much special rooms prefer the border |
| `specialColumnChance` | 0.1 | chance for each puzzle/trap/miniboss to go in the special column |
| puzzles per floor | E–F3 2–3, F4–F6 3–4, F7 4–5 | `DungeonFloor` (played, captured) |

Averages over 1000 seeds, with the real F6 runs for comparison:

| Floor | Rooms | Critical path (rooms) | Wither doors | Rare rooms / dungeon |
|---|---|---|---|---|
| Entrance | 12.3 | 5.9 | 1.9 | 0.07 |
| F6 | 19.6 | 7.0 | | |
| F6, 3 captured runs | 18, 18, 19 | 7–9 | | 0 |
| F7 | 22.7 | 7.2 | 3.2 | 0.10 |

Shapes per F6 dungeon: 1x1 5.0, 1x2 2.3, 1x3 1.0, 1x4 0.9, 2x2 0.4, L 1.5 (captured: 1x1 3–5,
1x2 2, 1x3 0–2, 1x4 0–2, 2x2 0–2, L 0–2). Three runs are too few to tune further.

## 5. Notes
- Multi-cell rooms are believed to have no restricted walls. The generator still supports them
  (`doorSlots` on a multi-cell template), so a prefab that needs it can use it.

## 6. Capturing real rooms

`dungeon-scanner/` is a client-side Fabric mod (Minecraft 26.2). During a Catacombs run it saves
every room to a schematic in its own frame (roof marker north-west), both as a modern `.schem`
and as a 1.8 `.schematic`. Next to each schematic is a JSON with the room's cells and door slots
in that same frame, which is what the generator's templates need. It also writes each run's full
layout to `runs/*.json`, which is real data to check the generator's statistics against. See
`dungeon-scanner/README.md`.

The captures are Hypixel's builds, so they stay out of this (public) repository. The plugin reads
them from its data folder at runtime.

## 7. Pasting captured rooms (`paste/`)

`/dungeon paste [floor] [seed]` generates a dungeon out of the captured rooms only and pastes it
with WorldEdit 7 at Hypixel's coordinates (cell centres at -185 + 32i), then teleports you into
the entrance. From the console it pastes into the main world. Put the scanner's `rooms/` folder
(a copy or a symlink) in `plugins/dungeons/dungeon-rooms/`; it reads the modern `.schem` files,
so block states and armor stands come through as captured. The scanner's `doors/` folder isn't
used: the doors come out of the room captures (see below).

**Doorways.** Each outer wall of a cell has a doorway in its middle: 5 blocks along the wall
(13–17), 3 deep (the outer wall and two layers inside) and 7 high (y 67–73, bedrock at the
bottom). Comparing captures of the same room with a doorway open and walled up, exactly that
5x7x3 box differs. Open, it holds the door's frame, one of 13 styles (stone bricks, cobblestone,
spruce, acacia with skulls, wither coal, blood clay, ...); the frame is the same on both sides and
in the gap, which is why every captured door schematic's three layers match. Walled up, it holds
something built for that wall of that room, often a fireplace (194 of the 245 walled-up doorways
captured are unique to their room). So a door is 7 blocks deep: the gap plus the doorway on each
side.

Use a world with `/gamerule random_tick_speed 0`. Hypixel's dungeons don't random-tick; with it
on, the ice in rooms like Ice Path melts and floods them (the command warns about this).

- **`RoomLibrary`** reads the captures. Each room id becomes one template; its captures are
  variants (Lower and Higher Blaze are both `blaze`). Types map NORMAL → REGULAR,
  CHAMPION → MINIBOSS, ENTRANCE → START. 1x1 rooms other than the fairy room get exactly the
  doorways they were captured with (they match IllegalMap's door layouts). Everything else may
  have a door on any wall. Minimum floors come from `HypixelRooms`. With the 51 rooms captured
  so far every floor generates (0 failures in 300 seeds each).
- **`PastePlan`** works out every paste without a world, so it's tested. A capture's frame is
  the template frame turned `frameTurns` times (0, except 3 for L rooms), so a room with
  rotation `r` is pasted turned `r - frameTurns` times. If a room has several captures, it uses the
  one whose open doorways best match the doors it needs. Each door is copied from a captured open
  doorway with the same kind of door (wither, blood, or normal, which includes entrance and
  fairy doors), preferably one of its own two rooms' (about 80% of doors), into both rooms and the
  gap. That also replaces whatever a walled-up doorway held, like the fireplace that used to sit
  in front of some doors. A doorway open in the capture that has no door now gets what another
  capture of the room shows there; if no capture has it walled up, the outer wall next to it is
  copied across (of the three fallbacks tried, that one matched the real fillers best).
- **`WorldEditPaster`** carries it out over several ticks (4-layer slices, about 25 ms per tick
  including WorldEdit's lighting pass, no undo history), so the server keeps running. Schematics
  are read off the main thread first. In this order:
  1. clear the gaps, empty cells and the space above and below each room (only blocks that
     aren't air already);
  2. paste the rooms (only their own blocks: an L room's schematic box also holds the missing
     cell);
  3. paste the doors: both rooms' doorways and the gap, 5 wide, 7 deep, y 67–73;
  4. paste the captured fillers of doorways that aren't doors now;
  5. wall up the rest of them by copying the wall beside them.

Pasted leaves are made persistent and armor stands get no gravity: on Hypixel neither changes
(no random ticks; the stands are only sent to clients), on a normal server leaves would decay
and the stands would fall.

Checked on a real Paper 26.2 server with WorldEdit 7.4.5: an F7 dungeon pastes in 5–9 s at
~20 TPS. 25,000 random positions matched a simulation of the plan (block types and turned
`facing`/`axis`/`rotation` states), also after pasting a different dungeon over it, and all
56 armor stands were where they belong. A test player was teleported into the entrance.
With the 7-deep doors, all 5,635 door blocks of an F7 dungeon matched the simulation.

## Sources
- IllegalMap (`utils/rooms.json`, `components/DungeonMap.js`): https://github.com/UnclaimedBloom6/IllegalMap
- BetterMap (`Data/roomdata.json`): https://github.com/BetterMap/BetterMap
- Odin (`features/impl/dungeon/map/SpecialColumn.kt`, `MapScan.kt`, `DungeonScan.kt`): https://github.com/odtheking/Odin
- Skytils Catlas / FunnyMap (`DungeonScanner.kt`): https://github.com/Skytils/SkytilsMod, https://github.com/Harry282/FunnyMap
- DungeonRoomsMod (`utils/MapUtils.java`): https://github.com/Quantizr/DungeonRoomsMod
- Skyblocker (`skyblock/dungeon/secrets/DungeonMapUtils.java`): https://github.com/SkyblockerMod/Skyblocker
- Patch 0.8: https://hypixel.net/threads/skyblock-patch-0-8-first-dungeon-the-catacombs.2851991/
- Map layout guide: https://hypixel.net/threads/catacombs-dungeon-map-layout-scoring-and-mob-generation-updated-for-f7-release.3166798/
- Rare rooms: https://hypixel.net/threads/extreme-effort-full-dungeons-guide-entrance-f7.4777276/
- Fan recreation: https://hypixel.net/threads/randomized-dungeon-generation-algorithm.4354393/
- Wiki: https://hypixel-skyblock.fandom.com/wiki/Catacombs/Floors, https://hypixelskyblock.minecraft.wiki/w/Dungeon_Puzzle_Rooms
