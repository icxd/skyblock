package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.SkyBlockMob;
import net.icxd.dungeons.utils.Utils;

@CommandParameters(aliases = "se", description = "Spawn a SkyBlock mob", usage = "/<command> <mob>", permission = Rank.STAFF)
public class SpawnEntityCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (args.length == 0) {
            send("§cUsage: /se <" + String.join("|", Mobs.registry().keySet()) + ">");
            return;
        }
        if (source.getPlayer() == null) {
            send("§cOnly players can spawn mobs where they stand.");
            return;
        }
        SkyBlockMob mob = Mobs.get(args[0]);
        if (mob == null) {
            send("§cNo mob called " + args[0] + ".");
            return;
        }
        Mobs.spawn(mob, source.getPlayer().getLocation());
        send(Utils.color("&aSpawned &c" + mob.getName()));
    }
}
