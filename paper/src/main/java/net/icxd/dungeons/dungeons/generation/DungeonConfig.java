package net.icxd.dungeons.dungeons.generation;

import java.util.Map;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;

/**
 * Knobs for one generation run. {@link #forFloor} gives the defaults; everything marked "tuning"
 * is not documented anywhere for Hypixel and was picked to make maps look right.
 *
 * @param specialColumn      the map's last column is not part of the dungeon proper: it's empty
 *                           except for the odd special room sticking out (F4-F6; Odin's
 *                           {@code SpecialColumn} heuristic, confirmed on F6 captures)
 * @param specialColumnChance tuning: chance for each puzzle/trap/miniboss to go in that column
 * @param entranceOnEdge     entrance always on the map's border (4/4 captured runs)
 * @param bloodEdgeWeight    how much more likely the blood room is on the border (3 of 4 captured
 *                           runs; one had it inside)
 * @param bloodDistance      tuning: min entrance-blood distance as a fraction of the map's diagonal
 * @param deadEndEdgeWeight  tuning: how much more likely puzzles/trap/miniboss are on the border
 * @param shapeWeights       tuning: relative chance of each regular room shape
 * @param newestBias         tuning: growing-tree parameter, chance to keep extending the newest
 *                           branch instead of branching off a random earlier room (1 = long
 *                           corridors, 0 = bushy)
 * @param pathNoise          tuning: how much the critical path may wander (0 = always shortest)
 */
public record DungeonConfig(
    DungeonFloor floor,
    int width,
    int height,
    boolean fairy,
    int minPuzzles,
    int maxPuzzles,
    int traps,
    int minibosses,
    boolean specialColumn,
    double specialColumnChance,
    boolean entranceOnEdge,
    double bloodEdgeWeight,
    double bloodDistance,
    double deadEndEdgeWeight,
    Map<RoomShape, Double> shapeWeights,
    double newestBias,
    double pathNoise
) {

  public static DungeonConfig forFloor(DungeonFloor floor) {
    return new DungeonConfig(
        floor,
        floor.getMaxXSize(),
        floor.getMaxZSize(),
        true,
        floor.getMinPuzzles(),
        floor.getMaxPuzzles(),
        floor.getTraps(),
        floor.getMinibosses(),
        floor.isSpecialColumn(),
        0.1,
        true,
        4.0,
        0.6,
        2.0,
        DungeonGenerator.defaultShapeWeights(),
        0.5,
        1.5
    );
  }
}
