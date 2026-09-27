package net.icxd.dungeons.item.ability.utility;

import org.bukkit.entity.Player;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.utils.Utils;

/**
 * An ability that only works somewhere this network doesn't have yet, saying so as Hypixel does there and
 * costing nothing: the Aspiring Leap's "&cYou can only use this item on your private island!" (the wiki's
 * game message; there are no private islands or gardens here).
 */
final class Refusal implements AbilityHandler {
    private final String message;

    Refusal(String message) {
        this.message = message;
    }

    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        player.sendMessage(Utils.color(message));
        return false;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
    }
}
