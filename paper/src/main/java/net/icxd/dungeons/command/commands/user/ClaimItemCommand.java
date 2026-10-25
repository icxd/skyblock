package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.quest.ClaimMenu;

/** {@code /claimitem <key>}, which a quest reward's item line runs: its Claim Reward menu again, until it's claimed. */
@CommandParameters(description = "Claims an item a quest gave you")
public class ClaimItemCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null || args.length == 0) return;
        ClaimMenu.open(source.getPlayer(), args[0], MenuSound.CLICK);
    }
}
