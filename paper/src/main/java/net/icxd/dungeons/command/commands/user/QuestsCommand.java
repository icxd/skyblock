package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.quest.QuestMenu;

/** {@code /quests} ({@code /questlog}, which NEW OBJECTIVE's lines run): the Quest Log. */
@CommandParameters(description = "Opens your Quest Log", aliases = "questlog")
public class QuestsCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() != null) QuestMenu.questLog(source.getPlayer());
    }
}
