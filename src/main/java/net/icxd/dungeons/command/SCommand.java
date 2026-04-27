package net.icxd.dungeons.command;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.command.exceptions.CommandArgumentException;
import net.icxd.dungeons.command.exceptions.CommandFailException;
import net.icxd.dungeons.command.exceptions.CommandPermissionException;
import net.icxd.dungeons.command.exceptions.PlayerNotFoundException;
import net.icxd.dungeons.user.Rank;
import net.icxd.dungeons.user.User;
import org.apache.logging.log4j.core.Core;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public abstract class SCommand implements CommandExecutor, TabCompleter {
    public static final String COMMAND_SUFFIX = "Command";
    protected static final Dungeons instance = Dungeons.getInstance();
    private CommandParameters params = this.getClass().getAnnotation(CommandParameters.class);
    private String name = this.getClass().getSimpleName().replace("Command", "").toLowerCase();
    private String description = this.params.description();
    private String usage = this.params.usage();
    private List<String> aliases = Arrays.asList(this.params.aliases().split(","));
    private Rank permission = this.params.permission();
    private SECommand command = new SECommand(this);
    private CommandSource sender;


    public abstract void run(CommandSource var1, String[] var2);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

    public void register() {
        SCommand.instance.commandMap.register("", this.command);
    }

    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        return null;
    }

    public void send(String message, CommandSource sender) {
        sender.send(ChatColor.GRAY + message.replace("&", "\u00a7"));
    }

    public void send(String message) {
        this.send(message.replace("&", "\u00a7"), this.sender);
    }

    public void send(String message, Player player) {
        player.sendMessage(ChatColor.GRAY + message.replace("&", "\u00a7"));
    }

    private static class SECommand
            extends Command {
        private final SCommand sc;

        public SECommand(SCommand xc) {
            super(xc.name, xc.description, xc.usage, xc.aliases);
            this.setPermissionMessage("\u00a7cYou must be " + xc.permission.name() + " to use this command.");
            this.sc = xc;
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            this.sc.sender = new CommandSource(sender);
            try {
                if (sender instanceof Player) {
                    if (Rank.valueOf(User.getUser(((Player) sender).getPlayer().getUniqueId()).getDocument().getString("rank")).isEqualOrStrongerThan(this.sc.permission)) {
                        sc.run(this.sc.sender, args);
                        return true;
                    }
                    sender.sendMessage("\u00a7cYou need " + this.sc.permission.name().toUpperCase() + " or above to do this command");
                    return true;
                }
                this.sc.run(this.sc.sender, args);
                return true;
            } catch (CommandFailException | CommandPermissionException | PlayerNotFoundException ex) {
                sender.sendMessage(ex.getMessage());
                return true;
            } catch (CommandArgumentException ex) {
                return false;
            } catch (Exception ex) {
                sender.sendMessage(ChatColor.RED + "Error: " + ex.getMessage());
                ex.printStackTrace();
                return true;
            }
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return this.sc.tabCompleters(sender, alias, args);
        }
    }
}