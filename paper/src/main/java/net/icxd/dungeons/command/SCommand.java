package net.icxd.dungeons.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.user.User;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A command: its name is the class's without "Command" ("ItemCommand" is /item), the rest is in its
 * {@link CommandParameters}. Registered with Paper's command registrar (see {@link #register}), so a
 * player who may not use it (their rank is too low, or their data isn't loaded) doesn't see it at all:
 * not in suggestions, not in the command list. The console can run anything.
 */
public abstract class SCommand {
    protected static final Dungeons instance = Dungeons.getInstance();
    private final CommandParameters params = this.getClass().getAnnotation(CommandParameters.class);
    private final String name = this.getClass().getSimpleName().replace("Command", "").toLowerCase();
    private final Rank permission = this.params.permission();
    private CommandSource sender;

    public abstract void run(CommandSource source, String[] args);

    /** Suggestions for the argument being typed (the last of {@code args}); null for none. */
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        return null;
    }

    public void register(Commands commands) {
        List<String> aliases = Arrays.stream(params.aliases().split(",")).map(String::trim).filter(a -> !a.isEmpty() && !a.equals(name)).toList();
        commands.register(name, params.description(), aliases, new Command());
    }

    /** With {@code &} colours, to whoever ran the command. */
    public void send(String message) {
        sender.send("§7" + message.replace("&", "§"));
    }

    /** Players need the command's rank, and their data loaded. */
    private boolean allowed(CommandSender sender) {
        if (!(sender instanceof Player player)) return true;
        User user = User.cached(player.getUniqueId());
        return user != null && user.isLoaded() && user.getRank().isEqualOrStrongerThan(permission);
    }

    private final class Command implements BasicCommand {
        @Override
        public void execute(CommandSourceStack stack, String[] args) {
            CommandSender from = stack.getSender();
            if (!allowed(from)) {
                from.sendMessage("§cYou need " + permission.name() + " or above to do this command");
                return;
            }
            sender = new CommandSource(from);
            try {
                run(sender, args);
            } catch (RuntimeException e) {
                from.sendMessage("§cError: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public Collection<String> suggest(CommandSourceStack stack, String[] args) {
            if (!allowed(stack.getSender())) return List.of();
            List<String> suggestions = tabCompleters(stack.getSender(), name, args);
            return suggestions == null ? List.of() : suggestions;
        }

        @Override
        public boolean canUse(CommandSender sender) {
            return allowed(sender);
        }
    }
}
