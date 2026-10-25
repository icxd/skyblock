package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.pet.PetsMenu;

/** {@code /pets} ("Also accessible via /pets.", recorded) and the wiki's others for it: the Pets menu. */
@CommandParameters(description = "Opens the Pets menu", aliases = "petsmenu,petmenu,viewpetsmenu")
public class PetsCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        PetsMenu.show(source.getPlayer(), 1, null);
    }
}
