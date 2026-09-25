package net.icxd.dungeons.dungeons.generation;

import java.util.Map;

import net.icxd.dungeons.dungeons.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;

/**
 * Knobs for one generation run. {@link #forFloor} gives the defaults; everything marked "tuning"
 * is not documented anywhere for Hypixel and was picked to make maps look right.
 *
 * @param specialColumn      puzzles, trap and miniboss all go in the map's last column (F4-F6, from
 *                           Odin's {@code SpecialColumn} map heuristic)
 * @param entranceOnEdge     tuning: entrance always on the map's border
 * @param bloodOnEdge        tuning: blood room always on the map's border
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
    boolean entranceOnEdge,
    boolean bloodOnEdge,
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
        true,
        true,
        0.6,
        2.0,
        DungeonGenerator.defaultShapeWeights(),
        0.5,
        1.5
    );
  }
}
