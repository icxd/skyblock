package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.skill.SkillsMenu;

/** {@code /skills}: Your Skills. */
@CommandParameters(description = "Opens Your Skills")
public class SkillsCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        new SkillsMenu(source.getPlayer()).open(source.getPlayer());
    }
}
