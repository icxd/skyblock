package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.collection.CollectionsMenu;
import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;

/** {@code /collection}: Collections ("Also accessible via /collection.", its item says; the other names are the wiki's Commands). */
@CommandParameters(aliases = "collections,viewcollectionmenu,collectionlog", description = "Opens Collections")
public class CollectionCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        new CollectionsMenu(source.getPlayer()).open(source.getPlayer());
    }
}
