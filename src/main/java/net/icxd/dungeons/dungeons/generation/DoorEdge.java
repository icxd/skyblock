package net.icxd.dungeons.dungeons.generation;

import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * Phase 2: single door on the tree edge from parent to child. Cells are 4-adjacent grid positions.
 */
public record DoorEdge(
    int parentInstanceId,
    int childInstanceId,
    Position parentCell,
    Position childCell
) {
}
