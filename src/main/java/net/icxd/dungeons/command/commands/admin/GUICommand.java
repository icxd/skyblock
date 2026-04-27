package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.user.Rank;

@CommandParameters(permission = Rank.ADMIN, usage = "/<command>", description = "Opens the GUI.", aliases = "gui")
public class GUICommand extends SCommand {

    @Override
    public void run(CommandSource source, String[] args) {
        source.send("&cDeprecated command.");
//        if (args.length < 1) {
//            source.send("Usage: " + this.getClass().getAnnotation(CommandParameters.class).usage());
//            return;
//        }
//        String id = args[0].toUpperCase();
//        GUI gui = GUIRegistry.get(id);
//        if (gui == null) {
//            source.send("GUI not found.");
//            return;
//        }
//
//        gui.open(source.getPlayer());
    }
}
