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
     * Whether it can happen now, asked once its costs are known to be there and before they're paid, so
     * a use that can't happen costs nothing (Instant Transmission with no room to go); if not, it tells
     * the player why. Yes unless it says otherwise.
     */
    default boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        return true;
    }
}
