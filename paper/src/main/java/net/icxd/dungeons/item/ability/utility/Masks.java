package net.icxd.dungeons.item.ability.utility;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.storage.AccessoryBag;

/**
 * The Carnival masks' passives, while one is on their head (a mask isn't armor, so set bonuses don't count it as worn:
 * it's read off the helmet slot here), as their text says: the Salmon Mask's Slippery Slope ("Gain +10✦ Speed and
 * +10☂ Fishing Speed while in water."), the Zombie Mask's Exanimate Blessing ("Increases the stats of Undead ༕ armor
 * pieces by +5%.": their other worn pieces whose text says "This item is Undead ༕!") and the Parrot Mask's Party
 * Flock ("Nearby players within 20 blocks gain +1❁ Strength for each upgrade of your Intimidation Accessory.": the
 * wearer's best Intimidation accessory that counts, the Talisman 1 to the Relic 4; the wearer among the "nearby
 * players", UNKNOWN; of several wearers near, the most counts, UNKNOWN). Their stats go in with the other modifiers.
 */
final class Masks {
    private static final Pattern WHILE_IN_WATER = Pattern.compile("Gain (.+?) while in water");
    private static final Pattern UNDEAD_PIECES = Pattern.compile("Increases the stats of Undead ༕ armor pieces by \\+([\\d.]+)%");
    private static final Pattern FLOCK = Pattern.compile("Nearby players within ([\\d.]+) blocks gain \\+([\\d.]+)❁ Strength for each upgrade of your Intimidation");
    private static final String UNDEAD = "This item is Undead ༕!";
    /** The Intimidation line, lowest first: each one an upgrade on the one before. */
    private static final List<String> INTIMIDATION = List.of("INTIMIDATION_TALISMAN", "INTIMIDATION_RING", "INTIMIDATION_ARTIFACT", "INTIMIDATION_RELIC");

    private Masks() {
    }

    /** Their masks' stats, and a Parrot Mask's near them. */
    static void stats(Player player, Stats stats) {
        ItemBlock slope = mask(player, "Slippery Slope");
        if (slope != null && player.isInWater()) {
            Matcher m = WHILE_IN_WATER.matcher(AbilityText.plain(slope.text()));
            if (m.find()) stats.add(AbilityText.stats(m.group(1)));
        }
        ItemBlock blessing = mask(player, "Exanimate Blessing");
        if (blessing != null) {
            Matcher m = UNDEAD_PIECES.matcher(AbilityText.plain(blessing.text()));
            if (m.find()) undeadPieces(player, stats, Double.parseDouble(m.group(1)) / 100);
        }
        stats.add(Stat.STRENGTH, flock(player));
    }

    /** The mask on their head's ABILITY block with this name; null for none. */
    private static ItemBlock mask(Player player, String name) {
        NBTTagCompound tag = ItemNBT.read(player.getInventory().getHelmet());
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null || !"CARNIVAL_MASK".equals(item.typeKey())) return null;
        for (ItemBlock block : item.blocks()) if (block.isAbility() && name.equals(block.name())) return block;
        return null;
    }

    /** {@code share} of each worn Undead piece's own stats on top. */
    private static void undeadPieces(Player player, Stats stats, double share) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack piece : new ItemStack[] {inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots()}) {
            NBTTagCompound tag = ItemNBT.read(piece);
            SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
            if (item == null || !AbilityText.plain(item.lore()).contains(UNDEAD)) continue;
            Stats own = ItemStats.of(piece, player);
            for (Stat stat : Stat.values()) if (own.has(stat)) stats.add(stat, own.get(stat) * share);
        }
    }

    /** The most Strength a Parrot Mask near them gives. */
    private static double flock(Player player) {
        double most = 0;
        for (Player wearer : player.getWorld().getPlayers()) {
            if (wearer.isDead() || wearer.isInvulnerable()) continue;
            ItemBlock block = mask(wearer, "Party Flock");
            if (block == null) continue;
            Matcher m = FLOCK.matcher(AbilityText.plain(block.text()));
            if (!m.find()) continue;
            double range = Double.parseDouble(m.group(1));
            if (wearer.getLocation().distanceSquared(player.getLocation()) > range * range) continue;
            most = Math.max(most, Double.parseDouble(m.group(2)) * upgrades(AccessoryBag.countedIds(wearer)));
        }
        return most;
    }

    /** Which upgrade of the Intimidation line the best of these accessories is (0 for none, the Relic 4). */
    static int upgrades(List<String> accessories) {
        int best = 0;
        for (int i = 0; i < INTIMIDATION.size(); i++) if (accessories.contains(INTIMIDATION.get(i))) best = i + 1;
        return best;
    }
}
