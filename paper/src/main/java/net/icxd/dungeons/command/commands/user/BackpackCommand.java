package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.storage.Storage;

/**
 * {@code /backpack <slot>}, {@code /backpacks}, {@code /bps} and {@code /bp} (the wiki's Commands page and
 * Backpack: "Added a /backpack [slot] command"): the backpack in a slot. Without one it's Storage, where the
 * backpacks are (UNKNOWN: what Hypixel's does then).
 */
@CommandParameters(description = "Opens the backpack in a slot of your Storage", usage = "/<command> <slot>", aliases = "backpacks,bps,bp")
public class BackpackCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        if (args.length == 0) Storage.openStorage(source.getPlayer());
        else Storage.openBackpack(source.getPlayer(), EnderChestCommand.number(args[0]));
    }
}
