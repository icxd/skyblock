package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.dungeons.chests.ChestType;
import net.icxd.dungeons.dungeons.chests.RewardChest;
import net.icxd.dungeons.dungeons.chests.impl.BedrockChest;
import net.icxd.dungeons.user.Rank;

@CommandParameters(aliases = "spawnrewardchest", description = "Spawn a reward chest", usage = "/<command> <chest type>", permission = Rank.STAFF)
public class SpwanRewardChestCommand extends SCommand {

    @Override
    public void run(CommandSource source, String[] args) {
        if (args.length == 0) {
            send("&cUsage: /spawnrewardchest <chest type>");
            return;
        }

        ChestType chestType = ChestType.valueOf(args[0].toUpperCase());

        RewardChest rewardChest = null;
        switch (chestType) {
            case BEDROCK -> rewardChest = new BedrockChest();
        }

        if (rewardChest == null) {
            send("&cInvalid chest type.");
            return;
        }

        rewardChest.spawn(source.getPlayer().getLocation());
        send("&aSpawned a reward chest.");
    }
}
