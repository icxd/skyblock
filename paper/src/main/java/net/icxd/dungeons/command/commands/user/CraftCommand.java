package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.recipe.CraftingTable;

/**
 * {@code /craft}: the crafting table ("Also accessible via /craft", the SkyBlock Menu's item says). The wiki's
 * Commands lists it with the commands that need no Cookie Buff, so everyone has it.
 */
@CommandParameters(aliases = "craftingtable,viewcraftingtable,craftingmenu", description = "Opens the crafting table")
public class CraftCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        new CraftingTable(source.getPlayer()).open(source.getPlayer());
    }
}
