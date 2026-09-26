package net.icxd.dungeons.proxy.dungeon;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.proxy.Chat;

/**
 * {@code /joininstance CATACOMBS_FLOOR_SEVEN} (Hypixel's names), or the short
 * {@code /joindungeon [catacombs] f7} ({@code e}, {@code f1}-{@code f7}, {@code m1}-{@code m7}).
 */
public final class JoinInstanceCommand implements SimpleCommand {
    private final DungeonQueue queue;

    public JoinInstanceCommand(DungeonQueue queue) {
        this.queue = queue;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            Chat.send(invocation.source(), "§cOnly players can enter dungeons.");
            return;
        }
        String[] args = invocation.arguments();
        // "/joindungeon catacombs 7", as Hypixel once had it.
        if (args.length > 1 && args[0].equalsIgnoreCase("catacombs")) args = Arrays.copyOfRange(args, 1, args.length);
        DungeonFloor floor = args.length == 1 ? DungeonFloor.parse(args[0]) : null;
        if (floor == null) {
            Chat.send(player, "§cUsage: /" + invocation.alias() + " <floor>, like CATACOMBS_FLOOR_SEVEN, F7, M3 or E.");
            return;
        }
        queue.join(player, floor);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        String typed = args.length == 0 ? "" : args[args.length - 1].toUpperCase(Locale.ROOT);
        boolean shortNames = invocation.alias().equalsIgnoreCase("joindungeon");
        return Arrays.stream(DungeonFloor.values())
                .map(f -> shortNames ? f.getShortName() : f.getInstanceName())
                .filter(n -> n.startsWith(typed))
                .toList();
    }
}
