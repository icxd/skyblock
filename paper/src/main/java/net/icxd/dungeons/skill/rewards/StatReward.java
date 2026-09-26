package net.icxd.dungeons.skill.rewards;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.stats.Stats;

@Getter
@AllArgsConstructor
public class StatReward extends Reward {
    private final Stats stat;
}
