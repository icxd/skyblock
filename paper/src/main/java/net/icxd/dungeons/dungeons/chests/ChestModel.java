package net.icxd.dungeons.dungeons.chests;

import java.util.List;

/**
 * The reward chests as Hypixel builds them: invisible armor stands wearing slabs, blocks and heads, every one facing the
 * way the chest does (recorded after Bonzo, 2026_10_26_00_59_04_0 at 11:46.7, a 301 S+ run with all five of Floor I's;
 * the same stands in both other recordings). Each part is where it stands from the chest's anchor (the middle of its
 * block, or of its four for Obsidian, at the floor), across, up and forward (to its front, where its latch is), so a
 * chest can stand facing any way. The latch is the small head tilted 16 degrees forward; the heads on top are the lid,
 * which shakes as it opens and goes once it has (recorded for Wood; the other chests' lids are taken to be their other
 * heads, which is UNKNOWN). The Bedrock chest wasn't recorded.
 */
public final class ChestModel {
    /**
     * A stand: across, up and forward of the anchor; small or not; what it wears ({@code "spruce_slab"}, a block, or
     * {@code "head:<texture hash>"}); its head's forward tilt in degrees; whether it's the lid.
     */
    public record Part(double across, double up, double forward, boolean small, String item, float headPitch, boolean lid) {
        /** The texture hash it wears, or null for a block. */
        public String head() {
            return item.startsWith("head:") ? item.substring("head:".length()) : null;
        }
    }

    private static Part part(double across, double up, double forward, boolean small, String item, float headPitch, boolean lid) {
        return new Part(across, up, forward, small, item, headPitch, lid);
    }

    private static final List<Part> WOOD = List.of(
            part(0.062, -1.406, -0.062, false, "spruce_slab", 0, false),
            part(0, -0.469, -0.062, true, "oak_log", 0, false),
            part(0.031, -0.875, -0.031, false, "spruce_slab", 0, false),
            part(0, 0.313, 0, true, "head:5a736eb1a7ce5a5f5d5b168931fa31396386c160568b41964ba86cdb9ed6be", 0, true),
            part(0.031, -0.719, 0.156, true, "head:1ae3855f952cd4a03c148a946e3f812a5955ad35cbcb52627ea4acd47d3081", -16, false));

    private static final List<Part> GOLD = List.of(
            part(-0.062, -0.469, -0.031, true, "gold_block", 0, false),
            part(-0.062, -1.406, -0.031, false, "smooth_stone_slab", 0, false),
            part(-0.031, -0.406, 0, false, "head:f37cae5c51eb1558ea828f58e0dff8e6b7b0b1a183d737eecf714661761", 0, true),
            part(-0.062, -0.875, -0.063, false, "smooth_stone_slab", 0, false),
            part(-0.062, -0.687, 0.156, true, "head:4ad0476e8671696af3a8949afa2a814b9bdde65eccd1a8b593aeeff5a0318d", -16, false));

    private static final List<Part> DIAMOND = List.of(
            part(-0.437, -1.406, 0, false, "smooth_stone_slab", 0, false),
            part(0.188, -1.406, 0, false, "smooth_stone_slab", 0, false),
            part(-0.375, -0.469, 0, true, "diamond_block", 0, false),
            part(0.094, -0.469, 0, true, "diamond_block", 0, false),
            part(-0.125, -0.719, 0.188, true, "head:77b9dfd281deaef2628ad5840d45bcda436d6626847587f3ac76498a51c861", -16, false),
            part(0.156, -0.875, 0, false, "smooth_stone_slab", 0, false),
            part(-0.469, -0.875, 0, false, "smooth_stone_slab", 0, false),
            part(-0.406, -0.375, 0, false, "head:2bb7b385e0aa748a7c6efdf3bcf209928e82471cc36e954337767c5e3d2e150e", 0, true),
            part(0.125, -0.375, 0, false, "head:726ec3d844380a27d58569bda58b4fa21a46d560282527341cf1e0a228261aa3", 0, true));

    private static final List<Part> EMERALD = List.of(
            part(0.344, -1.406, -0.094, false, "smooth_stone_slab", 0, false),
            part(0, -0.719, 0.125, true, "head:73ecdb847e101849f335472ac02db0d88996a6918eee95796dcf86cb47f217e", -16, false),
            part(-0.313, -1.406, -0.094, false, "smooth_stone_slab", 0, false),
            part(0.344, -0.875, -0.094, false, "smooth_stone_slab", 0, false),
            part(-0.281, -0.375, -0.063, false, "head:1824d454e0508c57e3fe8337030a0bc52e2065e19f02e30b457d352d4a312537", 0, true),
            part(-0.188, -0.469, -0.094, true, "emerald_block", 0, false),
            part(0.219, -0.469, -0.094, true, "emerald_block", 0, false),
            part(-0.281, -0.875, -0.063, false, "smooth_stone_slab", 0, false),
            part(0.312, -0.375, -0.063, false, "head:e0ed94bf5fccdd9d3b7ca7fa38e2ceb501332e6b3353404276566b5216c1304f", 0, true));

    private static final List<Part> OBSIDIAN = List.of(
            part(0.281, -1.344, 0.219, false, "nether_brick_slab", 0, false),
            part(-0.031, -0.656, 0.406, true, "head:3bcbbf94d603743a1e7147026e1c1240bd98fe87cc4ef04dcab51a31c30914fd", -16, false),
            part(-0.375, -1.344, 0.219, false, "nether_brick_slab", 0, false),
            part(0.219, -0.812, 0.187, false, "nether_brick_slab", 0, false),
            part(-0.25, -0.406, 0.094, true, "obsidian", 0, false),
            part(0.187, -0.406, 0.062, true, "obsidian", 0, false),
            part(-0.375, -0.812, 0.187, false, "nether_brick_slab", 0, false),
            part(-0.375, -1.344, -0.438, false, "nether_brick_slab", 0, false),
            part(0.25, -1.344, -0.469, false, "nether_brick_slab", 0, false),
            part(0.187, -0.562, -0.313, true, "obsidian", 0, false),
            part(-0.25, -0.562, -0.344, true, "obsidian", 0, false),
            part(0.25, -0.812, -0.438, false, "nether_brick_slab", 0, false),
            part(-0.344, -0.812, -0.438, false, "nether_brick_slab", 0, false),
            part(-0.281, 0.375, 0.094, true, "head:fb82156fae023b021e1c2275d4cb4c9940e989260d3862578ee27bdbc2b995aa", 0, true),
            part(0.125, 0.375, 0.125, true, "head:8933eefab8fcb3fa0f47bb0399a508ef6c911dae4b114557f906897aecedf85a", 0, true),
            part(-0.25, 0.375, -0.344, true, "head:fb82156fae023b021e1c2275d4cb4c9940e989260d3862578ee27bdbc2b995aa", 0, true),
            part(0.125, 0.375, -0.313, true, "head:8933eefab8fcb3fa0f47bb0399a508ef6c911dae4b114557f906897aecedf85a", 0, true),
            part(-0.25, 0.781, 0.062, true, "head:4dc962ca4fdb7817c9c28604fca39d4c05cab1acdd036866096ea6498fcba95c", 0, true),
            part(-0.25, 0.781, -0.281, true, "head:4dc962ca4fdb7817c9c28604fca39d4c05cab1acdd036866096ea6498fcba95c", 0, true),
            part(0.094, 0.781, 0.094, true, "head:a93b29c10d82dc1483663bd1cd7ee7896dd3bdd5bce7b629536f0214ed9c0820", 0, true),
            part(0.125, 0.781, -0.344, true, "head:ad91926e4e595a7e800267ac705a4f1e58997d57f459e7921b97b66406ebad6d", 0, true));

    private ChestModel() {
    }

    /** A chest's stands; none for Bedrock (not recorded). */
    public static List<Part> parts(ChestType type) {
        return switch (type) {
            case WOODEN -> WOOD;
            case GOLD -> GOLD;
            case DIAMOND -> DIAMOND;
            case EMERALD -> EMERALD;
            case OBSIDIAN -> OBSIDIAN;
            case BEDROCK -> List.of();
        };
    }

    /** Whether this chest was recorded (and is built from {@link #parts}). */
    public static boolean recorded(ChestType type) {
        return !parts(type).isEmpty();
    }

    /**
     * Across and forward of something facing {@code yaw} (Minecraft's: 0 is south, -90 east), as x and z: forward is
     * the way it faces.
     */
    public static double[] offset(double across, double forward, float yaw) {
        double y = Math.toRadians(yaw);
        return new double[]{across * Math.cos(y) - forward * Math.sin(y), across * Math.sin(y) + forward * Math.cos(y)};
    }

    /** How far from the anchor a chest's rewards come out (recorded: 2 blocks). */
    public static final double REWARD_REACH = 2;

    /**
     * Where the {@code i}th of {@code n} rewards comes out, as x and z from the anchor: evenly round a circle 2 blocks
     * out, the first in front (recorded: three, 120 degrees apart, the book in front).
     */
    public static double[] rewardSpot(int i, int n, float yaw) {
        return offset(0, REWARD_REACH, yaw + 360f * i / n);
    }
}
