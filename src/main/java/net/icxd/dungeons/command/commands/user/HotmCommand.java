package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.dwarven.HeartOfTheMountain;
import net.icxd.dungeons.gui.guis.HeartOfTheMountainGUI;
import net.icxd.dungeons.user.User;

@CommandParameters(aliases = "hotm", description = "Opens the Heart of the Mountain GUI")
public class HotmCommand extends SCommand {
  @Override
  public void run(CommandSource source, String[] args) {
    HeartOfTheMountain hotm = new HeartOfTheMountain();
    new HeartOfTheMountainGUI(source.getUser(), hotm).open(source.getPlayer());
  }
}
