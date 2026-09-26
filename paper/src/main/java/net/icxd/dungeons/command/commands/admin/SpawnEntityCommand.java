package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.entity.CustomEntity;
import net.icxd.dungeons.entity.EntityBuilder;
import net.icxd.dungeons.entity.EntityRegistry;
import net.icxd.dungeons.user.Rank;

@CommandParameters(aliases = "se", description = "Spawn an entity", usage = "/<command> <entity>", permission = Rank.STAFF)
public class SpawnEntityCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (args.length == 0) {
            send("§cUsage: /se <entity>");
            return;
        }
        CustomEntity ce = EntityRegistry.get(args[0]);
        EntityBuilder.spawn(ce, source.getPlayer().getLocation());
        send("§aSpawned §c" + ce.getName());
    }
}
