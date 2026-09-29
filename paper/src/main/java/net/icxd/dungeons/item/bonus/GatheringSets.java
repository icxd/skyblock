package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.economy.ExpOrbs;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;

/**
 * Sets for mining, experience, fishing and the islands, what the plugin's systems carry of them: Mithril's Dwarf
 * Wannabe and Titanium's True Dwarf (equipment, on the Dwarven Mines), Lapis Armor's Magnetic and Shimmering Light's
 * Shimmer (experience), Angler's Deepness Within (Health by Fishing level, whatever Fishing is here), Abyssal's and
 * Diver's One with the Fish (air) and Magma Lord's Fireproof (the Crimson Isle's lava). Their numbers are their
 * text's, the wiki's where the tiers grow.
 */
final class GatheringSets {
    private GatheringSets() {
    }

    static List<Bonus> all() {
        return List.of(new OnIsland("Dwarf Wannabe", ServerType.DWARVEN_MINES), new OnIsland("True Dwarf", ServerType.DWARVEN_MINES),
                new Magnetic(), new Shimmer(), new DeepnessWithin(), new OneWithTheFish(), new Fireproof());
    }

    /**
     * A full set's stats on one island: Mithril equipment's Dwarf Wannabe, "Grants +30⸕ Mining Speed and +5☘ Mining
     * Fortune while in the Dwarven Mines", and Titanium's True Dwarf (+50 and +40): every "+N Stat" in its text,
     * while this server is that island.
     */
    record OnIsland(String name, ServerType island) implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            ItemBlock block = BonusText.block(active);
            if (block != null && Bonuses.on(island)) stats.add(BonusText.stats(block.text()));
        }
    }

    /** Lapis Armor's Magnetic, each piece: "Earn +50% more Exp when mining", from ores (see ExpOrbs). */
    static final class Magnetic implements Bonus {
        @Override
        public String kind() {
            return "PIECE";
        }

        @Override
        public String name() {
            return "Magnetic";
        }

        @Override
        public double experience(Player player, Active active, ExpOrbs.Source source) {
            return source == ExpOrbs.Source.ORE ? active.count() * BonusText.after(BonusText.block(active), "Earn", 50) : 0;
        }
    }

    /**
     * Shimmering Light Armor's Shimmer (2+): "Increases all Experience Orbs gained from monsters and ores by 200%",
     * 200% with 3 pieces and 300% with 4 (the wiki's Shimmering Light Armor).
     */
    static final class Shimmer extends TieredSets.Tiered {
        static final Tiers PERCENT = new Tiers(2, 200, 200, 300);

        Shimmer() {
            super(2);
        }

        @Override
        public String name() {
            return "Shimmer";
        }

        @Override
        public double experience(Player player, Active active, ExpOrbs.Source source) {
            return source == ExpOrbs.Source.OTHER ? 0 : PERCENT.at(active.count());
        }

        @Override
        public List<String> text(List<String> text, int count) {
            return PERCENT.text(text, "&a", count, needs());
        }
    }

    /**
     * Angler Armor's Deepness Within: "Gain 6❤ per Fishing level", "6-10" by pieces (the wiki's Angler Armor gives
     * no more): 6 until all four, then 10 (UNKNOWN in between). Its "Fishing: Level" line shows their level.
     */
    static final class DeepnessWithin extends TieredSets.Tiered {
        static final Tiers PER_LEVEL = new Tiers(1, 6, 6, 6, 10);

        DeepnessWithin() {
            super(1);
        }

        @Override
        public String name() {
            return "Deepness Within";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            stats.add(Stat.HEALTH, PER_LEVEL.at(active.count()) * Skills.level(player, Skill.FISHING));
        }

        @Override
        public List<String> text(List<String> text, int count) {
            return PER_LEVEL.text(text, "&c", count, needs());
        }

        @Override
        public List<String> text(List<String> text, int count, Player holder) {
            return level(text(text, count), Skills.level(holder, Skill.FISHING));
        }

        /** The "Fishing: Level" line with this level (the data's is obfuscated). */
        static List<String> level(List<String> text, int level) {
            return CountedSets.line(text, "Fishing:", "&7Fishing: &eLevel " + level);
        }
    }

    /**
     * Abyssal and Diver Armor's One with the Fish: "Grants the ability to breathe permanently while underwater":
     * their air is kept full while they're in water, every second, before it can run out.
     */
    static final class OneWithTheFish implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "One with the Fish";
        }

        @Override
        public void second(Player player, Active active) {
            if (player.isInWater() || player.getRemainingAir() < player.getMaximumAir()) player.setRemainingAir(player.getMaximumAir());
        }
    }

    /**
     * Magma Lord Armor's Fireproof (2+): "Provides immunity to Crimson Isle Lava": lava's damage while this server is
     * the Crimson Isle (not fire's: UNKNOWN whether burning counts). Kuudra's Hollow isn't here.
     */
    static final class Fireproof extends TieredSets.Tiered {
        Fireproof() {
            super(2);
        }

        @Override
        public String name() {
            return "Fireproof";
        }

        @Override
        public boolean immune(Player player, Active active, EntityDamageEvent.DamageCause cause) {
            return cause == EntityDamageEvent.DamageCause.LAVA && Bonuses.on(ServerType.CRIMSON_ISLE);
        }
    }
}
