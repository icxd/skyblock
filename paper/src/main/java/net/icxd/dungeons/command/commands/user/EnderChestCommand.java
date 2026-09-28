package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.storage.Storage;

/**
 * {@code /enderchest [page]}, {@code /viewenderchest}, {@code /echest} and {@code /ec} (the wiki's Commands
 * page): a page of the Ender Chest, the first without one.
 */
@CommandParameters(description = "Opens a page of your Ender Chest", usage = "/<command> [page]", aliases = "viewenderchest,echest,ec")
public class EnderChestCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        Storage.openEnderChest(source.getPlayer(), args.length == 0 ? 1 : number(args[0]));
    }

    /** 0 for what isn't a number: no page has it. */
    static int number(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
