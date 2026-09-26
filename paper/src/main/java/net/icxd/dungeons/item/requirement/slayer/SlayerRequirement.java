package net.icxd.dungeons.item.requirement.slayer;

import net.icxd.dungeons.item.requirement.Requirement;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

public class SlayerRequirement extends Requirement {
    private SlayerBossType bossType;
    private int level;

    public SlayerRequirement(SlayerBossType bossType, int level) {
        this.bossType = bossType;
        this.level = level;
    }

    public SlayerBossType getBossType() {
        return bossType;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public Predicate<Player> requirement() {
        return player -> false;
    }
}
