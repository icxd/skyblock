package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.storage.Storage;

/** {@code /storage}, {@code /st} and {@code /er} (the wiki's Commands page): Storage. */
@CommandParameters(description = "Opens your Storage", aliases = "st,er")
public class StorageCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        Storage.openStorage(source.getPlayer());
    }
}
