package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.Modifier;
import net.icxd.dungeons.mob.SpawnOptions;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Spawns a mob of any kind where you stand, for testing: a dungeon kind at its Entrance variant (the
 * lowest level, or {@code lv<N>}), optionally starred, with a modifier, or times a room multiplier
 * ({@code x1.05}).
 */
@CommandParameters(aliases = "se", description = "Spawn a SkyBlock mob", usage = "/<command> <mob> [starred] [modifier] [lv<level>] [x<room multiplier>]",
        permission = Rank.STAFF)
public class SpawnEntityCommand extends SCommand {
    private static final String OPTIONS = "[starred] [" + String.join("|", Stream.of(Modifier.values()).map(m -> m.name().toLowerCase(Locale.ROOT)).toList())
            + "] [lv<level>] [x<room multiplier>]";

    @Override
    public void run(CommandSource source, String[] args) {
        if (args.length == 0) {
            send("§cUsage: /se <" + String.join("|", ids()) + "> " + OPTIONS);
            return;
        }
        if (source.getPlayer() == null) {
            send("§cOnly players can spawn mobs where they stand.");
            return;
        }
        MobKind kind = Mobs.kind(args[0]);
        if (kind == null) {
            send("§cNo mob called " + args[0] + ".");
            return;
        }
        SpawnOptions options = SpawnOptions.NONE;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i].toLowerCase(Locale.ROOT);
            Modifier modifier = Modifier.parse(arg);
            try {
                if (arg.equals("starred") || arg.equals("star")) options = options.starred(true);
                else if (modifier != null) options = options.modifier(modifier);
                else if (arg.startsWith("lv")) options = options.level(Integer.parseInt(arg.substring(2)));
                else if (arg.startsWith("x")) options = options.roomMultiplier(Double.parseDouble(arg.substring(1)));
                else throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                send("§cWhat's " + args[i] + "? Usage: /se " + args[0] + " " + OPTIONS);
                return;
            }
        }
        DungeonFloor floor = kind.dungeon() ? DungeonFloor.ENTRANCE : null;
        try {
            Mobs.Live live = Mobs.spawn(kind, floor, options, source.getPlayer().getLocation());
            send(Utils.color("&aSpawned &c" + kind.name() + " &7(Lv" + live.type().getLevel() + ")"));
        } catch (IllegalArgumentException e) {
            send("§c" + e.getMessage() + ".");
        }
    }

    private static List<String> ids() {
        return Mobs.registry().keySet().stream().map(id -> id.toLowerCase(Locale.ROOT)).toList();
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if (args.length <= 1) options.addAll(ids());
        else {
            options.add("starred");
            for (Modifier modifier : Modifier.values()) options.add(modifier.name().toLowerCase(Locale.ROOT));
        }
        return options.stream().filter(o -> o.startsWith(typed)).toList();
    }
}
