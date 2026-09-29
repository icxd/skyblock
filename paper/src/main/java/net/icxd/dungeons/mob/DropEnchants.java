package net.icxd.dungeons.mob;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enchanting.EnchantmentType;
import net.icxd.dungeons.item.enums.GenericItemType;

/**
 * The enchantments on a kill's drops ({@link Mobs#addDropChance}), each a factor on a drop's own chance, before Magic
 * Find (Mobs):
 * <ul>
 *   <li>Looting (swords, rods, the gauntlet): "Increases the chance of a Monster dropping an item by 15%" at I, times
 *   1 + 15% (the wiki's Looting: "BaseDropChance × (1 + 0.15 × LootingLevel)").</li>
 *   <li>Chance (bows): the same words, and the same.</li>
 *   <li>Luck (swords): "Increases the chance for Monsters to drop their armor by 5%" at I, on armor drops: times 1 +
 *   5% too (UNKNOWN: the wiki's Luck gives only "+5%"; read as Looting's).</li>
 * </ul>
 * Each percent is its text's. They're the killer's held item's when the mob dies: "Looting applies whenever a mob dies
 * and the death is attributed to the player holding the weapon with Looting" (the wiki; an ability's kill counts the
 * weapon switched to). The same for Chance and Luck (UNKNOWN: the wiki says it of Looting). No slayers or pets drop
 * here, which Looting skips. Main thread.
 */
public final class DropEnchants implements Listener {
    public DropEnchants() {
        Mobs.addDropChance((killer, blow, drop) -> factor(killer, drop));
    }

    /** The factor on this drop's chance for what the killer holds. */
    static double factor(Player killer, MobDrop drop) {
        Map<String, Integer> held = Combat.heldEnchantments(killer);
        if (held.isEmpty()) return 1;
        return factor(percent("looting", held), percent("chance", held), armor(drop) ? percent("luck", held) : 0);
    }

    /** The drop's chance times 1 + each percent / 100. */
    static double factor(double looting, double chance, double luck) {
        return (1 + looting / 100) * (1 + chance / 100) * (1 + luck / 100);
    }

    /** What its text says an enchantment the item has adds, in percent (Looting III's 45); 0 without it. */
    private static double percent(String id, Map<String, Integer> enchantments) {
        int level = enchantments.getOrDefault(id, 0);
        if (level <= 0) return 0;
        EnchantmentType type = EnchantmentType.getByNamespace(id);
        return type == null ? 0 : type.percent(level);
    }

    /** Whether it's a piece of armor the mob drops (the Rotten Armor pieces): every item it can be is armor. */
    static boolean armor(MobDrop drop) {
        for (String id : drop.itemIds()) {
            SkyBlockItem item = ItemRegistry.get(id);
            if (item == null || item.genericItemType() != GenericItemType.ARMOR) return false;
        }
        return true;
    }
}
