package net.icxd.dungeons.item.enchanting.weapon;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.reforge.CombatReforges;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * What weapon enchantments do to stats, through the stat pipeline so the Stats menu, lore and hits agree: the stats
 * their text grants that EnchantmentType's reading doesn't (Tabasco's, Toxophilite's; these show in the lore's
 * totals, as live items do), what the lore doesn't list (Ultimate Jerry's base damage: a live Aspect of the Jerry with
 * Ultimate Jerry V shows its own Damage), Fatal Tempo's Ferocity (a {@link PlayerStats} modifier) and Ultimate Wise's
 * cheaper abilities (an item mana factor, and its Mana Cost lines). Main thread.
 */
public final class WeaponStats {
    private WeaponStats() {
    }

    /** Registers what isn't a hook in the item code itself: Fatal Tempo's modifier and Ultimate Wise's mana factor. */
    public static void enable() {
        PlayerStats.addModifier(WeaponStats::tempo);
        Abilities.addItemManaCostFactor((player, tag) -> wise(WeaponEnchant.WISE.on(tag)));
    }

    /**
     * {@code stats} with what this enchantment's text grants that the text reading (EnchantmentType) doesn't:
     * Tabasco's "Grants +2 weapon damage if you don't have a Dragon pet equipped" (there are no pets, so always) and
     * Toxophilite's "Grants +3.7☣ Crit Chance" after its Combat XP sentence.
     */
    public static Stats listed(String id, String text, Stats stats) {
        WeaponEnchant enchant = WeaponEnchant.of(id);
        if (enchant == null || text == null) return stats;
        switch (enchant) {
            case TABASCO -> {
                double[] numbers = EnchantText.numbers(text);
                if (numbers.length > 0) stats.add(Stat.DAMAGE, numbers[0]);
            }
            case TOXOPHILITE -> stats.add(AbilityText.stats(AbilityText.plain(List.of(text))));
            default -> {
            }
        }
        return stats;
    }

    /**
     * Adds what its enchantments and reforge give that its lore doesn't list: Ultimate Jerry's "Increases the base
     * damage of Aspect of the Jerry by 1000%" (its Signature Edition's too: UNKNOWN, a live one shows its own 2 Damage
     * either way) and the reforges' (see {@link CombatReforges#unlisted}). Not grown by a dungeon's boost (UNKNOWN).
     */
    public static void addUnlisted(SkyBlockItem item, NBTTagCompound tag, Stats stats) {
        int jerry = WeaponEnchant.JERRY.on(tag);
        if (jerry > 0 && item.name() != null && item.name().startsWith("Aspect of the Jerry")) {
            stats.add(Stat.DAMAGE, item.stats().get(Stat.DAMAGE) * EnchantText.at(WeaponEnchant.JERRY, jerry, 0) / 100);
        }
        CombatReforges.unlisted(item, tag, stats);
    }

    /**
     * Fatal Tempo's "Attacking increases your ⫽ Ferocity by 10% per hit, capped at 200% for 3 seconds after your last
     * attack", while they hold a weapon with it: their Ferocity times the boost (Ferocity's cap of 500 still stops its
     * strikes: UNKNOWN whether Fatal Tempo goes past it). Their weapon is only looked at while a boost may be on.
     */
    private static void tempo(Player player, Stats stats) {
        if (!WeaponEnchants.tempoRunning(player)) return;
        int level = WeaponEnchant.FATAL_TEMPO.on(ItemNBT.read(player.getInventory().getItemInMainHand()));
        if (level <= 0) return;
        double boost = WeaponEnchants.tempoBoost(player, level);
        if (boost > 0) stats.set(Stat.FEROCITY, WeaponRules.withTempo(stats.get(Stat.FEROCITY), boost));
    }

    /** Ultimate Wise's "Reduces the ability mana cost of this item by 10%" a level, as a factor (1 without it). */
    public static double wise(int level) {
        return level <= 0 ? 1 : Math.max(0, 1 - EnchantText.at(WeaponEnchant.WISE, level, 0) / 100);
    }

    /**
     * The item's abilities as its lore shows them, with Ultimate Wise's cheaper mana: a live Aspect of the Void with
     * Ultimate Wise V shows "Mana Cost: 23" for its 45 and 90 for its 180, rounded as the cost is (Abilities); the
     * same blocks without it.
     */
    public static List<ItemBlock> withWise(NBTTagCompound tag, List<ItemBlock> blocks) {
        double factor = wise(WeaponEnchant.WISE.on(tag));
        if (factor == 1) return blocks;
        List<ItemBlock> out = new ArrayList<>(blocks.size());
        for (ItemBlock block : blocks) {
            if (!block.isAbility() || (block.mana() <= 0 && block.manaPercent() <= 0)) {
                out.add(block);
                continue;
            }
            out.add(new ItemBlock(block.kind(), block.name(), block.header(), block.activation(), block.text(),
                    Math.round(block.mana() * factor), block.manaPercent() * factor, block.cooldown(), block.soulflow(), block.healthCost(),
                    block.vitality(), block.pieces()));
        }
        return out;
    }
}
