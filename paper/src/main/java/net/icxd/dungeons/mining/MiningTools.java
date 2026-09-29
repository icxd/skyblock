package net.icxd.dungeons.mining;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.SkyBlockServer;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.utils.Text;

/**
 * What the mining enchantments on a tool or helmet do to mining itself: Efficiency's Mining Speed and Aqua
 * Affinity's mining rate underwater. Mining tools are the wiki's: pickaxes, drills and the gauntlet.
 */
public final class MiningTools {
    private MiningTools() {
    }

    /** Whether it's a mining tool (the wiki's Mining Tools: pickaxes, drills and the Gemstone Gauntlet). */
    public static boolean isMiningTool(SkyBlockItem item) {
        if (item == null) return false;
        return switch (item.specificItemType()) {
            case PICKAXE, DRILL, GAUNTLET -> true;
            default -> false;
        };
    }

    /**
     * Efficiency's Mining Speed at a level: "+30" at I to "+110" at V and "+210" at X, 20 more a level (the wiki's
     * Efficiency table; the book's text has no number). Live lore off the Hub reads "Grants +110 ⸕ Mining Speed." at V.
     */
    public static double efficiencySpeed(int level) {
        return level < 1 ? 0 : 10 + 20 * level;
    }

    /**
     * Whether Efficiency grants Mining Speed on this kind of server: "On other islands, if applied to Mining Tools,
     * it instead grants Mining Speed" than the Private Island, the Garden, the Park, the Hub and the Farming Islands
     * (the wiki's Efficiency), which here is all but the Hub. Not on a server that isn't one (tests, the item
     * browser's renders): the book's text stays.
     */
    static boolean efficiencyGrantsSpeed(ServerType type) {
        return type != null && type != ServerType.LOBBY;
    }

    /**
     * Efficiency's text on a mining tool where it grants Mining Speed, as live lore has it there ("&7Grants &a+110
     * &6⸕ Mining Speed&7.", which the stats read); null for any other enchantment, item or server (the book's text).
     */
    public static String efficiencyText(String enchantment, SkyBlockItem item, int level) {
        if (!"efficiency".equals(enchantment) || !isMiningTool(item) || !efficiencyGrantsSpeed(serverType())) return null;
        return efficiencyText(level);
    }

    /** "&7Grants &a+110 &6⸕ Mining Speed&7." at V. */
    public static String efficiencyText(int level) {
        return "&7Grants &a+" + Text.number(efficiencySpeed(level)) + " " + Stat.MINING_SPEED.label() + "&7.";
    }

    private static ServerType serverType() {
        SkyBlockServer server = Dungeons.getSkyBlockServer();
        return server == null ? null : server.getServerType();
    }

    /**
     * How much faster they mine: Aqua Affinity (a helmet's) "increases mining rate in water" by "+100%" (the wiki's
     * Aqua Affinity), while their head is under water. Whether SkyBlock's own mining is slower under water without it
     * (vanilla's is) is UNKNOWN: it isn't here, so Aqua Affinity doubles it.
     */
    static double miningRate(boolean underwater, boolean aquaAffinity) {
        return underwater && aquaAffinity ? 2 : 1;
    }
}
