package net.icxd.dungeons.skill;

import net.icxd.dungeons.skill.rewards.Reward;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Skill {
    COMBAT("Combat", "Warrior", true, Arrays.asList("&fDeal &8%s➜%&as% &fmore damage", "&fto mobs."));

    private final String name;
    private final String alternateName;
    private final List<String> description;
    private final boolean levelSixty;

    public static final List<Integer> XP_GOALS = Arrays.asList(0, 50, 175, 375, 675, 1175, 1925, 2925, 4425, 6425, 9925, 14925, 22425, 32425, 47425,
            67425, 97425, 147425, 222425, 322425, 522425, 822425, 1222425, 1722425, 2322425, 3022425, 3822425, 4722425,
            5722425, 6822425, 8022425, 9322425, 10722425, 12222425, 13822425, 15522425, 17322425, 19222425, 21222425,
            23322425, 25522425, 27822425, 30222425, 32722425, 35322425, 38072425, 40972425, 44072425, 47472425, 51172425,
            55172425, 59472425, 64072425, 68972425, 74172425, 79672425, 85472425, 91572425, 97972425, 104672425, 111672425);
    public static final List<Integer> COIN_REWARDS = Arrays.asList(0, 25, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1300,
            1400, 1500, 1600, 1700, 1800, 1900, 2000, 2200, 2400, 2600, 2800, 3000, 3500, 4000, 5000, 6000, 7500, 10000, 12500,
            15000, 17500, 20000, 25000, 30000, 35000, 40000, 45000, 50000, 60000, 70000, 80000, 90000, 100000, 125000, 150000,
            175000, 200000, 250000, 300000, 350000, 400000, 450000, 500000, 600000, 700000, 800000, 1000000);
    public static final List<Integer> SKYBLOCK_XP_REWARDS = Arrays.asList(
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
            20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
            30, 30, 30, 30, 30, 30, 30, 30, 30, 30);

    Skill(String name, String alternateName, boolean levelSixty, List<String> description) {
        this.name = name;
        this.alternateName = alternateName;
        this.levelSixty = levelSixty;
        this.description = description;
    }

    public String getName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }

    public static int getLevelFromXP(int xp) {
        int level = 0;
        for (int i = 0; i < XP_GOALS.size(); i++) {
            if (xp >= XP_GOALS.get(i)) {
                level = i;
            }
        }
        return level;
    }

    public static int getCoinReward(int level) { return COIN_REWARDS.get(level); }
    public static int getSkyblockXPReward(int level) { return SKYBLOCK_XP_REWARDS.get(level); }
}
