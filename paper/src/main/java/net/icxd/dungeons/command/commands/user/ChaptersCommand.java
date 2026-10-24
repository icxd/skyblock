package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.quest.QuestMenu;

/** {@code /chapters}: the islands' Chapters (none of them is here yet, so it only shows them). */
@CommandParameters(description = "Opens the Chapters")
public class ChaptersCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() != null) QuestMenu.chapters(source.getPlayer());
    }
}
