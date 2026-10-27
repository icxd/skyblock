package net.icxd.dungeons.command.commands.admin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.dungeons.chests.ChestModel;
import net.icxd.dungeons.dungeons.chests.ChestType;
import net.icxd.dungeons.dungeons.chests.RecordedChest;
import net.icxd.dungeons.dungeons.chests.RewardChest;
import net.icxd.dungeons.dungeons.chests.impl.BedrockChest;

/**
 * "/spawnrewardchest wooden": builds a reward chest's model where you stand (in the middle of your block, facing the
 * nearest of the four ways you look), to look at; it stays until the chunk unloads. Bedrock's wasn't recorded, so it's
 * the old approximation.
 */
@CommandParameters(aliases = "spawnrewardchest", description = "Spawn a reward chest", usage = "/<command> <chest type>", permission = Rank.STAFF)
public class SpawnRewardChestCommand extends SCommand {

    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();
        if (player == null) return;
        if (args.length == 0) {
            send("&cUsage: /spawnrewardchest <chest type>");
            return;
        }
        ChestType chestType;
        try {
            chestType = ChestType.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            send("&cInvalid chest type.");
            return;
        }
        RewardChest rewardChest = ChestModel.recorded(chestType) ? new RecordedChest(chestType) : new BedrockChest();
        Location at = player.getLocation();
        float yaw = Math.round(at.getYaw() / 90f) * 90f;
        rewardChest.spawn(new Location(at.getWorld(), at.getBlockX() + 0.5, at.getY(), at.getBlockZ() + 0.5, yaw, 0));
        send("&aSpawned a reward chest.");
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        if (args.length > 1) return null;
        return Arrays.stream(ChestType.values()).map(t -> t.name().toLowerCase(Locale.ROOT)).toList();
    }
}
