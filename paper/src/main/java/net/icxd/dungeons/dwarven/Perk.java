package net.icxd.dungeons.dwarven;

import lombok.Getter;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
public enum Perk {
  // ---------- HOTM Tier 2 ----------
  TITANIUM_INSANIUM(
      "Titanium Insanium", level -> String.format("&7When mining Mithril Ore, you have a &a%.1f%% &7chance to convert the block into Titanium Ore.", 2 + (level * 0.1)),
      50, HOTMTier.TWO, (level, user) -> { },
      nextLevel -> (int)Math.pow(nextLevel + 1, 3.1), PowderType.MITHRIL),
  QUICK_FORGE(
      "Quick Forge", level -> String.format("&7Decreases the time it takes to forge by &d%.1f%%&7.", Math.min(30, 10 + (level * 0.5) + (double)(level / 20) * 20)),
      20, HOTMTier.TWO, (level, user) -> {},
      nextLevel -> (int)Math.pow(nextLevel + 1, 4), PowderType.MITHRIL),

  MINING_FORTUNE(
      "Mining Fortune", level -> String.format("&7Grants &a+%d &6☘ Mining Fortune&7.", level * 5),
      50, HOTMTier.TWO, (level, user) -> {
        Stats stats = Stats.STATS_CACHE.get(user.getUuid());
        stats.add(new Stats().setMiningFortune(level * 5));
      }, nextLevel -> (int) Math.pow(nextLevel + 1, 3.05), PowderType.MITHRIL,
      TITANIUM_INSANIUM, QUICK_FORGE),

  // ---------- HOTM Tier 1 ----------
  MINING_SPEED(
      "Mining Speed", level -> String.format("&7Grants &a+%d &6⸕ Mining Speed&7.", level * 20),
      50, HOTMTier.ONE, (level, user) -> {
        Stats stats = Stats.STATS_CACHE.get(user.getUuid());
        stats.add(new Stats().setMiningSpeed(level * 20));
      }, nextLevel -> (int) Math.pow(nextLevel + 1, 3), PowderType.MITHRIL,
      MINING_FORTUNE);

  private final String name;
  private final Function<Integer, String> description;
  private final int maxLevel;
  private final HOTMTier requiredTier;
  private final BiConsumer<Integer, User> function;
  private final Function<Integer, Integer> costFormula;
  private final PowderType type;
  private final List<Perk> nextPerks;

  Perk(
      String name,
      Function<Integer, String> description,
      int maxLevel,
      HOTMTier requiredTier,
      BiConsumer<Integer, User> function,
      Function<Integer, Integer> costFormula,
      PowderType type,
      Perk... nextPerks
  ) {
    this.name = name;
    this.description = description;
    this.maxLevel = maxLevel;
    this.requiredTier = requiredTier;
    this.function = function;
    this.costFormula = costFormula;
    this.type = type;
    this.nextPerks = List.of(nextPerks);
  }
}
