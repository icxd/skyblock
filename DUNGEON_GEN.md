# Dungeon Generation Algorithm

Generation runs in **two phases**. Phase 1 only decides **which cells belong to which room** and **how rooms connect** (topology). Phase 2 assigns **doors**, prefab choice, and any edge decoration—nothing door-related happens during Phase 1.

---

## Core rule — tree layout (non-negotiable)

The **room graph must be a tree** rooted at the start room. Equivalently:

- **No alternate routes:** there are never two distinct paths from the start to the same room. Every room is reachable from the start in exactly one way.
- **One entrance per room:** every room except the start has exactly **one** inter-room link that acts as its **entrance** (the edge to its unique parent in the tree). It may have **many exits** (edges to child rooms), but never two entrances from two different neighboring rooms.

Phase 1 must **enforce** this while tiling (never attach a new room such that it would border two already-placed rooms on different “coming from start” branches in a way that creates a second route, or connect two grown regions in a way that forms a cycle). Phase 2 places **at most one door** on the unique parent–child boundary between any two room instances; there is no second door “into” the same room from elsewhere.

---

## Phase 1 — Topology and full tiling

Goal: a **uniform grid** fully covered by rooms (no empty gaps), with a clear main path and **tree-shaped** branching growth.

1. **Start and end**
   - Start is in the bottom-left corner.
   - End is in the top-right corner.

2. **Main path (A*)**
   - Use A* with a **diagonal-biased** heuristic / tie-breaking so the spine tends toward the target quadrant.
   - Pathfinding operates on the grid: treat cells not yet assigned to a room as unwalkable for the *current* expansion step so the growing layout stays coherent (see step 3).

3. **Rooms along the main path**
   - Generate **randomly sized** rooms on the main path: each piece is one of 1×1, 1×2, 1×3, 1×4, 2×2, or an **L-shape**; all pieces may be **rotated** to fit.
   - While extending the path, only place rooms on valid footprints; unassigned cells outside the placed mass behave as blocked so the path **follows placed room space** as it grows.
   - **Do not place doors in this phase.** Record **tree edges** only: parent–child links between room instances (each child has one parent toward start). Phase 2 turns each parent–child boundary into a single door.

4. **Backtrack expansion**
   - After the main path reaches the end cell, walk **backward** along the main path, room by room toward the start.
   - For each **attachment side** of the current room that faces empty grid (not yet tiled), attempt to place another room from the allowed set (with rotation) until you hit an **existing room** or the **map edge**.
   - Each **new** room must attach to exactly **one** already-placed room (its parent): that attachment is its sole **entrance**. Do not grow in a way that lets a room share a border with two distinct earlier rooms such that both would be “upstream” connections (violates the tree—see **Core rule**).
   - For each newly placed room, repeat: grow from free sides into empty grid only, preserving the tree property at every step.

5. **Until fully tiled**
   - Repeat branching / filler placement until **no empty gaps** remain: the map is a **uniform grid coverage** of rooms with varying sizes.
   - Any **filler** or gap closure must still obey the **Core rule** (no merging fronts into cycles; each new room keeps exactly one entrance).
   - If perfect tiling fails with the current random choices, **restart or backtrack** Phase 1 (implementation detail); Phase 2 is not responsible for fixing holes.

**Phase 1 output:** for each grid cell, a room id and shape footprint; a **tree** of room-to-room adjacency (parent/child along shared edges), not an arbitrary graph (no door objects yet).

---

## Phase 2 — Doors and edge rules

Goal: turn the Phase 1 adjacency graph into **doors** on eligible edges, consistent with prefabs and map bounds.

1. **Inputs**
   - Tiled grid from Phase 1 (room membership per cell).
   - Which internal edges are between **two rooms** (must become passable connections) vs **room vs void** (should not appear as interior door candidates).

2. **Door placement rules**
   - **Tree consistency:** between two **distinct** room instances there is **at most one** shared grid edge that is the **parent–child** link from Phase 1. Place exactly **one** door (or tagged opening) on that edge—the **entrance** into the child room. Never place a second door into the same room from another neighbor.
   - **Eligible edges for doors** (subject to the above): internal edges of the map where all of the following hold:
     1. The edge is **not** on the **outer boundary** of the map.
     2. That edge does not already have a door (each edge at most one door).
     3. The edge separates two cells that belong to **different** room ids **and** that pair is the unique tree adjacency for those rooms (not a spurious extra touch).
   - **Inside a single multi-cell room:** no door—cells of the same room are open to each other.
   - Optional polish: edges that are **only** same-room interior can follow art rules (no door); the diagram illustrates door positions on **connecting** boundaries, not every geometric crease between cells.

3. **Prefab and rotation**
   - For each room instance, choose the concrete prefab matching its polyomino type and **rotation** chosen in Phase 1; align **door sockets** in Phase 2 with the door graph so instances line up at shared edges.

4. **Output**
   - Final dungeon: geometry + door entities (or flags) on edges, ready for gameplay.

---

## Room set (both phases)

- Shapes: **1×1**, **1×2**, **1×3**, **1×4**, **2×2**, **L-shape** (all rotatable).
- Phase 1 chooses **footprint and rotation**; Phase 2 aligns **doors** with neighbors.

## Diagram (Phase 2 door placement on cell edges)

Example of door positions `D` on shared edges between cells; `S` start, numbers arbitrary cell labels; `x` interior.

    +---+---+---+
    |   |   |   |
    |   |   |   |
    |   |   |   |
    +---+-D-+---+
    |   |   |   |
    |   D 1 D   |
    |   | x |   |
    +---+ 2 +---+
    |   |   |   |
    | S D   D   |
    |   |   |   |
    +---+---+---+
