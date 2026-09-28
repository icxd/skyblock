package net.icxd.dungeons.item.enchanting.armor;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.utility.Heals;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.requirement.Requirement;
import net.icxd.dungeons.item.requirement.slayer.SlayerRequirement;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Habanero Tactics, the armor ultimate: "Heal +7.5% more from wands. Deal +25% damage with Slayer weapons. Gain
 * +5 Combat Wisdom with Slayer weapons. With Smoldering Polarization, gain +15 Magic Find on your slayer weapon."
 * The first three are here (the Combat Wisdom with the other stats, {@link StatEnchants}); the last waits for the
 * Re-heated Gummy Polar Bear's Smoldering Polarization, which isn't here. Each piece's adds up. Main thread.
 */
final class Habanero {
    static final String ID = "habanero_tactics";
    /** The kind of heal a Healing Wand's is ({@link Heals#overTime}: WandHeal's). */
    static final String WAND = "wand";

    private Habanero() {
    }

    static void register() {
        // "Deal +25% damage with Slayer weapons": an additive buff (the wiki's Damage Calculation, Additive
        // Sources) on their melee hits and arrows with one.
        Combat.addHitBuffs((player, attacker, target, landing) -> {
            double percent = damage(WornEnchants.of(player));
            return percent > 0 && slayerWeapon(landing.weapon()) ? new Combat.HitBuff(percent, 1) : null;
        });
        // "Heal +7.5% more from wands": their Healing Wands' heals (UNKNOWN whether it adds to Reaper Armor's
        // "+50%" rather than multiplying, as here).
        Heals.addKindFactor((healer, kind) -> healer != null && WAND.equals(kind) ? 1 + wandHeal(WornEnchants.of(healer)) / 100 : 1);
    }

    /** Its damage with Slayer weapons, in percent, on all their pieces. */
    static double damage(List<WornEnchants.Piece> pieces) {
        return sum(pieces, 1);
    }

    /** Its "more from wands", in percent, on all their pieces. */
    static double wandHeal(List<WornEnchants.Piece> pieces) {
        return sum(pieces, 0);
    }

    private static double sum(List<WornEnchants.Piece> pieces, int index) {
        double sum = 0;
        for (WornEnchants.Piece piece : pieces) {
            int level = piece.level(ID);
            if (level > 0) sum += EnchantNumbers.get(ID, level, index);
        }
        return sum;
    }

    /** Whether what they hold is a Slayer weapon (see {@link #slayerWeapon(SkyBlockItem)}). */
    static boolean holdsSlayerWeapon(Player player) {
        return slayerWeapon(Combat.skyBlockData(player.getInventory().getItemInMainHand()));
    }

    static boolean slayerWeapon(NBTTagCompound weapon) {
        return weapon != null && slayerWeapon(ItemRegistry.get(weapon.getString("id")));
    }

    /**
     * Whether it's a "Slayer weapon": a sword, longsword or bow that asks for a Slayer level (the daggers, the
     * katanas, the scythes, the Halberd of the Shredded, the Pooch Sword...). UNKNOWN: Hypixel names no list; the
     * requirement is the one mark items.json has (the Voidwalker Katana, which has none, isn't one here, nor the
     * wands, which aren't weapons).
     */
    static boolean slayerWeapon(SkyBlockItem item) {
        if (item == null || item.requirements() == null) return false;
        SpecificItemType type = item.specificItemType();
        if (type != SpecificItemType.SWORD && type != SpecificItemType.LONGSWORD && type != SpecificItemType.BOW) return false;
        for (Requirement requirement : item.requirements().getRequirements()) if (requirement instanceof SlayerRequirement) return true;
        return false;
    }
}
