# Dungeon generation (reverse engineered)

Hypixel's generator isn't public. A dev-forum reply says as much ("the dungeons generation
algorithm isn't open-source"). So this document works backwards from what the output is known to
look like, then describes a generator that reproduces all of it. Every fact below is tagged with
how sure it is:

- **confirmed**: stated by Hypixel, or hard-coded / recorded in the data of several map mods that
  have to get it right to work.
- **inferred**: follows from confirmed facts.
- **tuning**: not documented anywhere; picked so the maps look right.

The code lives in `src/main/java/net/icxd/dungeons/dungeons/generation`. `/dungeon [floor] [seed]`
prints a map like the ones below to the console and builds a block preview.

```
F7, seed 2024. E = entrance door, W = wither door, B = blood door, # = normal door.
r* = room on the critical path. S entrance, F fairy, BL blood, P puzzle, T trap, M miniboss.
+-------+-------+-------+-------+-------+-------+
|  M9   #  r24  #  r20          |  S0   E r*10  |
+-------+-------+---#---+---#---+-------+       +
|  P5   |  r23  #  r22  |  r21  #  P3   |       |
+---#---+---#---+-------+-------+-------+---W---+
|  r17          |  r19  #  P4   | r*13  W  F2   |
+-------+---#---+---#---+-------+       +-------+
|  BL1  B r*18  W r*16          W       #  r14  |
+-------+-------+---#---+       +---#---+---#---+
|  r11  #  T8   |  P6   |       |  r15  |  P7   |
+       +-------+-------+-------+---#---+-------+
|               #  r12                          |
+-------+-------+-------+-------+-------+-------+
```

---

## 1. What the output looks like

### Grid
| Fact | Confidence | Source |
|---|---|---|
| Rooms sit on a grid of 31x31-block cells with a 1-block gap (32-block pitch). The north-west corner is at -200,-200. | confirmed | DungeonRoomsMod `MapUtils`, Skyblocker `DungeonMapUtils`, IllegalMap `utils.js` |
| Grid size: Entrance 4x4, F1 4x5, F2–F3 5x5, F4 6x5, F5–F7 6x6. Master Mode uses the same sizes. | confirmed (mods). The wiki lists F5 as 6x5 and M7 as 6x7, which the mods contradict. | Odin `DungMap.kt`/`DungeonScan.kt`, Skytils `MapUtils`, wiki *Catacombs/Floors* |
| Every cell is always filled. There are no gaps. | confirmed | forum map-layout guide |
| Room shapes: 1x1, 1x2, 1x3, 1x4, 2x2 and L (a 2x2 with one cell missing). | confirmed | wiki, every mod |
| Doors sit only in the middle of a cell edge, so every cell edge is exactly one possible door. Cells of the same room have no door between them. | confirmed | Skytils/FunnyMap scanners (11x11 "room + connector" grid), Odin `DungeonDoor` |

### Connections
| Fact | Confidence | Source |
|---|---|---|
| The rooms form a **tree** rooted at the entrance. There are no loops, and every room has exactly one way in. | confirmed | IllegalMap `setupTree` builds exactly this parent/child tree. Your own observation agrees. |
| The **entrance has exactly one door**. | confirmed | IllegalMap `rooms.json`: entrance `doors: "1000"` |
| Blood, every puzzle, trap, yellow (miniboss) and rare room has **exactly one door**. They are dead ends. | confirmed | IllegalMap `rooms.json`, BetterMap `roomdata.json` ("D"). Patch 0.8: special rooms are "on the edges of the dungeon". |
| The fairy room can have any number of doors. | confirmed | BetterMap: fairy `"V"`, IllegalMap: no value |
| The fairy room is always on the entrance → blood path. | confirmed | 0.8 patch notes ("On the main path, you will always find…"), wiki |
| **Every 1x1 regular room has a fixed door layout**: 22 straight (I), 20 corner (L), 12 T, 8 cross (X). **None of them is a dead end.** | confirmed | IllegalMap `rooms.json` `doors` field (N,E,S,W in the room's own frame), which matches BetterMap's independently collected I/L/T/X data for all but 2 rooms |
| "Rare" rooms are 1x1 brown dead ends that show up "on very rare occasions", only when the generator "still ha[s] 1x1 spaces", usually at the map edge. | confirmed | forum guides |
| Multi-cell rooms have no recorded door layout. | inferred: they can have doors on any wall and can be dead ends | mod data (no field for them) |

This is the answer to "some rooms can only have doors on certain walls". Every 1x1 prefab is
built with a fixed set of doorways. The generator can only rotate it, so the door layout around a
1x1 cell decides which prefabs can go there. It also explains rare rooms: a 1x1 cell that ends up
as a leaf of the tree can't hold any regular 1x1 prefab (none is a dead end), so it gets a rare
room.

### Doors on the critical path
| Fact | Confidence | Source |
|---|---|---|
| On the entrance → blood path, the first door is the **entrance door** (infested stone), the last is the **blood door** (stained clay), and **every door in between is a wither door** (coal). No other door is special. | confirmed | IllegalMap `setupTree`, wiki ("the critical path … is always marked using doors requiring wither keys") |
| The key for a door drops in the room before it. The blood key drops "in the last room before the Blood Room". | confirmed | wiki *Wither Key* / *Blood Key* |
| So the rooms just before the fairy and just before blood are regular rooms (the fairy and blood rooms have no mobs to drop a key). | inferred | |
| The fairy door is black but reportedly opens without a key. FunnyMap shows "wither doors − 1". | conflicting sources | 2021 guide vs. a forum guide |

### Special rooms per floor
| Fact | Confidence | Source |
|---|---|---|
| Every floor: 1 entrance, 1 fairy, 1 blood, 1 yellow (miniboss) room, and at least 2 puzzles. F7 has up to 5 puzzles. Trap room from F3 up. | confirmed | wiki *Dungeons*, forum guides |
| Puzzles never repeat in a dungeon. Available puzzles: Tic Tac Toe, Three Weirdos, Creeper Beams, Water Board and Teleport Maze on all floors; Higher or Lower, Boulder and Ice Path from F3; Quiz from F4; Ice Fill on F7. Bomb Defuse was removed in 0.20.5. | confirmed | wiki *Dungeon Puzzle Rooms* |
| **On F4–F6 the last column (x = 5) holds all the puzzles, the trap and the miniboss room.** | confirmed as a working mod heuristic | Odin `SpecialColumn.kt` / `MapScan.kt` |
| Exact puzzle count per floor. | tuning (not documented) | `DungeonFloor` |
| Where the entrance and blood room go. | tuning (not documented) | defaults: both on the map border, far apart |

The special column is the most surprising find. It suggests that on F4–F6 Hypixel generates a
5-wide dungeon and then adds a column of special rooms. A fan recreation made the same point
independently: the extra space on those floors is "more of an extension of the map than a
subtraction".

---

## 2. The algorithm

The generator runs five stages. Every stage can fail on an unlucky roll, and the whole attempt is
then re-rolled with the same `Random`, so a seed always gives the same dungeon. On average it
needs 1.1–1.7 attempts, which takes 3–7 ms.

### Stage 1a: special rooms (`SpecialPlacer`)
1. **Entrance** on a random border cell. **Blood** on a border cell at least 60% of the map's
   Manhattan diameter away (tuning).
2. **Fairy** on a cell roughly between them (`d(E,F) + d(F,B) ≤ d(E,B) + 2`), at least 2 cells
   from each. That guarantees at least one regular room before it and one after it.
3. **Puzzles, trap, miniboss**: random cells, twice as likely on the border (tuning). On F4–F6
   they go only in the last column.

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
Along the critical path: the first door is ENTRANCE, the last is BLOOD, and everything in between
is WITHER. Every other door is NORMAL.

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
the schematic's own (rotation 0) frame, so they line up automatically.

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

---

## 4. Tuning knobs (`DungeonConfig`)

| Knob | Default | Effect |
|---|---|---|
| `shapeWeights` | 1x1 4, 1x2 3, 1x3 2, 1x4 1.5, 2x2 2, L 1.5 | how often each regular shape is picked |
| `newestBias` | 0.5 | 1 = long winding branches, 0 = bushy |
| `pathNoise` | 1.5 | 0 = shortest critical path |
| `entranceOnEdge`, `bloodOnEdge`, `bloodDistance` | true, true, 0.6 | where entrance and blood go |
| `deadEndEdgeWeight` | 2.0 | how much special rooms prefer the border |
| puzzles per floor | E–F1 2, F2–F3 2–3, F4 3, F5–F6 3–4, F7 3–5 | `DungeonFloor` |

Averages over 1000 seeds:

| Floor | Rooms | Critical path (rooms) | Wither doors | Rare rooms / dungeon |
|---|---|---|---|---|
| Entrance | 12.1 | 5.9 | 2.9 | 0.08 |
| F7 | 23.3 | 7.5 | 4.5 | 0.08 |

## 5. Open questions
- Where exactly the entrance and blood room may go, and whether Hypixel prefers corners.
- The puzzle count per floor.
- Whether the fairy door counts as a wither door for key purposes.
- Whether any multi-cell room has restricted walls. The model supports it; the data doesn't say.

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
