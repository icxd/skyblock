package net.icxd.dungeons.dungeons.generation.room;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.RoomPool;
import net.icxd.dungeons.dungeons.generation.utils.Direction;

/**
 * Every room in Hypixel's Catacombs, with the door layout of each 1x1 room.
 *
 * <p>Data from IllegalMap's {@code utils/rooms.json} (MIT, (c) 2024 UnclaimedBloom6,
 * https://github.com/UnclaimedBloom6/IllegalMap), which matches BetterMap's independently collected
 * {@code roomdata.json} for all but two rooms. Door strings are N, E, S, W in the room's own
 * (rotation 0) frame, {@code 1} = door. 1x1 rooms always have exactly those doors: there are
 * 22 straight ({@code 0101}), 20 corner ({@code 0011}), 12 T ({@code 1011}) and 8 cross
 * ({@code 1111}) regular 1x1s, and no regular 1x1 dead end. Every special room except the fairy
 * has exactly one door. Multi-cell rooms have no recorded door layout, so they allow a door on
 * any outside wall.
 *
 * <p>Puzzle floors are from the wiki's puzzle table (Bomb Defuse was removed in 0.20.5).
 */
public final class HypixelRooms {
    private HypixelRooms() {
    }

    public static final List<Room> ALL = List.of(
            // START ONE_BY_ONE (1)
            room("entrance", RoomType.START, RoomShape.ONE_BY_ONE, "1000", 0),
            // BLOOD ONE_BY_ONE (1)
            room("blood", RoomType.BLOOD, RoomShape.ONE_BY_ONE, "1000", 0),
            // FAIRY ONE_BY_ONE (1)
            room("fairy", RoomType.FAIRY, RoomShape.ONE_BY_ONE, null, 0),
            // PUZZLE ONE_BY_ONE (10)
            room("blaze", RoomType.PUZZLE, RoomShape.ONE_BY_ONE, "1000", 3),
            room("boulder", RoomType.PUZZLE, RoomShape.ONE_BY_ONE, "1000", 3),
            room("creeper_beams", RoomType.PUZZLE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("ice_fill", RoomType.PUZZLE, RoomShape.ONE_BY_ONE, "1000", 7),
            room("ice_path", RoomType.PUZZLE, RoomShape.ONE_BY_ONE, "1000", 3),
            room("quiz", RoomType.PUZZLE, RoomShape.ONE_BY_ONE, "1000", 4),
            room("teleport_maze", RoomType.PUZZLE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("three_weirdos", RoomType.PUZZLE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("tic_tac_toe", RoomType.PUZZLE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("water_board", RoomType.PUZZLE, RoomShape.ONE_BY_ONE, "1000", 0),
            // TRAP ONE_BY_ONE (2)
            room("new_trap", RoomType.TRAP, RoomShape.ONE_BY_ONE, "1000", 0),
            room("old_trap", RoomType.TRAP, RoomShape.ONE_BY_ONE, "1000", 0),
            // MINIBOSS ONE_BY_ONE (4)
            room("default", RoomType.MINIBOSS, RoomShape.ONE_BY_ONE, "0010", 0),
            room("dragon", RoomType.MINIBOSS, RoomShape.ONE_BY_ONE, "1000", 0),
            room("king_midas", RoomType.MINIBOSS, RoomShape.ONE_BY_ONE, "1000", 0),
            room("shadow_assassin", RoomType.MINIBOSS, RoomShape.ONE_BY_ONE, "1000", 0),
            // RARE ONE_BY_ONE (11)
            room("lava_pit", RoomType.RARE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("mini_rail_track", RoomType.RARE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("pillars", RoomType.RARE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("rare_carpets", RoomType.RARE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("rare_overgrown", RoomType.RARE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("sand_dragon", RoomType.RARE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("stone_window", RoomType.RARE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("three_floors", RoomType.RARE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("tombstone", RoomType.RARE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("trinity", RoomType.RARE, RoomShape.ONE_BY_ONE, "1000", 0),
            room("vinny_8_ball", RoomType.RARE, RoomShape.ONE_BY_ONE, "1000", 0),
            // REGULAR ONE_BY_ONE (62)
            room("andesite", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("basement", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("big_red_flag", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("blue_skulls", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("chains", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("double_diamond", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("locked_away", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("long_hall", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("mural", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("mushroom", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("painting", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("perch", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("prison_cell", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("raccoon", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("redstone_crypt", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("scaffolding", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("small_waterfall", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("staircase", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("tomioka", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("zodd", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0011", 0),
            room("admin", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("arrow_trap", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("cages", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("cell", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("criss_cross", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("drop", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("dueces", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("end", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("granite", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("hall", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("jumping_skulls", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("mirror", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("overgrown_chains", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("redstone_key", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("ritual", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("sarcophagus", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("silvers_sword", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("slabs", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("small_stairs", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("spikes", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("steps", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("water", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "0101", 0),
            room("banners", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1011", 0),
            room("beams", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1011", 0),
            room("cage", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1011", 0),
            room("dip", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1011", 0),
            room("dome", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1011", 0),
            room("duncan", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1011", 0),
            room("leaves", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1011", 0),
            room("lots_of_floors", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1011", 0),
            room("overgrown", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1011", 0),
            room("red_green", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1011", 0),
            room("sloth", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1011", 0),
            room("temple", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1011", 0),
            room("black_flag", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1111", 0),
            room("carpets", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1111", 0),
            room("cobble_wall_pillar", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1111", 0),
            room("golden_oasis", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1111", 0),
            room("knight", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1111", 0),
            room("logs", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1111", 0),
            room("multicolored", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1111", 0),
            room("quad_lava", RoomType.REGULAR, RoomShape.ONE_BY_ONE, "1111", 0),
            // REGULAR ONE_BY_TWO (14)
            room("archway", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("balcony", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("bridges", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("crypt", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("doors", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("gold", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("grand_library", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("grass_ruin", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("mage", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("pedestal", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("pressure_plates", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("purple_flags", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("redstone_warrior", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            room("skull", RoomType.REGULAR, RoomShape.ONE_BY_TWO, null, 0),
            // REGULAR ONE_BY_THREE (7)
            room("catwalk", RoomType.REGULAR, RoomShape.ONE_BY_THREE, null, 0),
            room("deathmite", RoomType.REGULAR, RoomShape.ONE_BY_THREE, null, 0),
            room("diagonal", RoomType.REGULAR, RoomShape.ONE_BY_THREE, null, 0),
            room("gravel", RoomType.REGULAR, RoomShape.ONE_BY_THREE, null, 0),
            room("red_blue", RoomType.REGULAR, RoomShape.ONE_BY_THREE, null, 0),
            room("slime", RoomType.REGULAR, RoomShape.ONE_BY_THREE, null, 0),
            room("wizard", RoomType.REGULAR, RoomShape.ONE_BY_THREE, null, 0),
            // REGULAR ONE_BY_FOUR (6)
            room("hallway", RoomType.REGULAR, RoomShape.ONE_BY_FOUR, null, 0),
            room("mossy", RoomType.REGULAR, RoomShape.ONE_BY_FOUR, null, 0),
            room("pipes", RoomType.REGULAR, RoomShape.ONE_BY_FOUR, null, 0),
            room("pit", RoomType.REGULAR, RoomShape.ONE_BY_FOUR, null, 0),
            room("quartz_knight", RoomType.REGULAR, RoomShape.ONE_BY_FOUR, null, 0),
            room("waterfall", RoomType.REGULAR, RoomShape.ONE_BY_FOUR, null, 0),
            // REGULAR TWO_BY_TWO (9)
            room("atlas", RoomType.REGULAR, RoomShape.TWO_BY_TWO, null, 0),
            room("buttons", RoomType.REGULAR, RoomShape.TWO_BY_TWO, null, 0),
            room("cathedral", RoomType.REGULAR, RoomShape.TWO_BY_TWO, null, 0),
            room("flags", RoomType.REGULAR, RoomShape.TWO_BY_TWO, null, 0),
            room("mines", RoomType.REGULAR, RoomShape.TWO_BY_TWO, null, 0),
            room("museum", RoomType.REGULAR, RoomShape.TWO_BY_TWO, null, 0),
            room("rails", RoomType.REGULAR, RoomShape.TWO_BY_TWO, null, 0),
            room("stairs", RoomType.REGULAR, RoomShape.TWO_BY_TWO, null, 0),
            room("supertall", RoomType.REGULAR, RoomShape.TWO_BY_TWO, null, 0),
            // REGULAR L_SHAPE (11)
            room("altar", RoomType.REGULAR, RoomShape.L_SHAPE, null, 0),
            room("chambers", RoomType.REGULAR, RoomShape.L_SHAPE, null, 0),
            room("dino_site", RoomType.REGULAR, RoomShape.L_SHAPE, null, 0),
            room("lava_ravine", RoomType.REGULAR, RoomShape.L_SHAPE, null, 0),
            room("layers", RoomType.REGULAR, RoomShape.L_SHAPE, null, 0),
            room("market", RoomType.REGULAR, RoomShape.L_SHAPE, null, 0),
            room("melon", RoomType.REGULAR, RoomShape.L_SHAPE, null, 0),
            room("pirate", RoomType.REGULAR, RoomShape.L_SHAPE, null, 0),
            room("spider", RoomType.REGULAR, RoomShape.L_SHAPE, null, 0),
            room("well", RoomType.REGULAR, RoomShape.L_SHAPE, null, 0),
            room("withermancer", RoomType.REGULAR, RoomShape.L_SHAPE, null, 0)
    );

    public static RoomPool pool() {
        return new RoomPool(ALL, true);
    }

    private static Room room(String id, RoomType type, RoomShape shape, String doors, int minFloor) {
        Room.RoomBuilder b = Room.builder().id(id).type(type).shape(shape).minimumFloor(floor(minFloor)).rotations(rotations(shape));
        if (doors != null) b.doorSlots(slots(doors)).exactDoors(true);
        return b.build();
    }

    /**
     * Straight rooms are only ever horizontal (0) or vertical (1) and 2x2s never turn: on 23 captured
     * multi-cell rooms the roof marker was always in that position.
     */
    public static Set<Integer> rotations(RoomShape shape) {
        return switch (shape) {
            case ONE_BY_TWO, ONE_BY_THREE, ONE_BY_FOUR -> Set.of(0, 1);
            case TWO_BY_TWO -> Set.of(0);
            default -> Set.of(0, 1, 2, 3);
        };
    }

    /** "N E S W" bit string -> door slots of a 1x1 room. */
    static Set<DoorSlot> slots(String doors) {
        List<DoorSlot> out = new ArrayList<>();
        Direction[] order = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int i = 0; i < 4; i++) if (doors.charAt(i) == '1') out.add(DoorSlot.of(0, 0, order[i]));
        return Set.copyOf(out);
    }

    private static DungeonFloor floor(int number) {
        for (DungeonFloor f : DungeonFloor.values()) if (!f.isMasterMode() && f.getNumber() == number) return f;
        throw new IllegalArgumentException("floor " + number);
    }
}
