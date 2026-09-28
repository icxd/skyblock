package net.icxd.dungeons.mob;

import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * The damage that killed a mob (see {@link SkyBlockMobDeathEvent#killingBlow}): what dealt it (a melee hit,
 * an arrow, a Ferocity strike, an ability, a damage over time's tick, another effect), the item it was
 * dealt with (the held weapon, the bow an arrow left, the item an ability was cast with; null for none or
 * not known), how much it was, and how much of it was more than the mob had left.
 */
public record KillingBlow(HitKind kind, NBTTagCompound weapon, double damage, double overkill) {
    /**
     * The blow being dealt to one of the Blood Room's undeads just now, whose deaths come through
     * {@link Mobs#kindDied} with no hit of their own (DungeonMobs sets it around the hurt). Main thread.
     */
    private static KillingBlow dealing;

    public KillingBlow {
        overkill = Math.max(0, overkill);
    }

    /** A blow of this much on a mob that had {@code healthBefore} left. */
    public static KillingBlow of(HitKind kind, NBTTagCompound weapon, double damage, double healthBefore) {
        return new KillingBlow(kind, weapon, damage, damage - Math.max(0, healthBefore));
    }

    /** Sets the blow being dealt (null for none), and returns the one it replaces, to put back once it's done. */
    public static KillingBlow dealing(KillingBlow blow) {
        KillingBlow before = dealing;
        dealing = blow;
        return before;
    }

    /** The blow being dealt now; null for none. */
    static KillingBlow dealing() {
        return dealing;
    }
}
