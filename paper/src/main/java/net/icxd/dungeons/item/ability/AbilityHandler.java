package net.icxd.dungeons.item.ability;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.bukkit.entity.Player;

/** What an ability does (see {@link Abilities}). By the time it's used, its costs are paid and its cooldown started. */
@FunctionalInterface
public interface AbilityHandler {
    /** {@code tag} is the held item's data; {@code block} the ability or shortbow block being used. */
    void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block);

    /**
     * The use, told whether its Vitality cost was paid: only ever not for one whose Vitality part is
     * optional (see {@link #vitalityOptional}), which then casts without that part. The rest do the use.
     */
    default void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block, boolean vitalityPaid) {
        use(player, item, tag, block);
    }

    /**
     * Whether it still casts with too little Vitality, just without what the Vitality pays for (and
     * spending none): "Not having enough Vitality does not prevent you from casting Wither Impact - it
     * still deals damage, it just doesn't put up the shield" (0.26.1's release notes, and its June 10
     * alpha). No unless it says otherwise: too little of it stops the cast, as too little mana does.
     */
    default boolean vitalityOptional() {
        return false;
    }

    /**
     * Whether it can happen now, asked once its costs are known to be there and before they're paid, so
     * a use that can't happen costs nothing (Instant Transmission with no room to go); if not, it tells
     * the player why. Yes unless it says otherwise.
     */
    default boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        return true;
    }
}
