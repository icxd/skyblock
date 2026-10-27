package net.icxd.dungeons.pet.ability;

import java.util.List;
import java.util.Locale;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.combat.VanillaDamage;
import net.icxd.dungeons.dungeons.instance.RunItems;
import net.icxd.dungeons.dwarven.PowderType;
import net.icxd.dungeons.economy.ExpOrbs;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.accessory.AccessoryEffects;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mining.MinableBlock;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.stats.PlayerAttributes;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;

/**
 * The gathering pets' abilities the plugin's systems can do (mining's speed and powder, fortunes, wisdoms, stats from
 * other stats, from skill levels, collections, other pets and players near), and the Pets menu's pets' (Owl,
 * Spirit, Kuudra: "This pet's perks are active even when the pet is not summoned!"), their numbers from their text.
 */
final class GatheringAbilities {
    private GatheringAbilities() {
    }

    static List<PetAbility> all() {
        return List.of(
                // Flat stats their pets' stat lines don't list.
                StatAbilities.stat("DOLPHIN", "Echolocation", "Grants +#α Sea Creature Chance.", Stat.SEA_CREATURE_CHANCE),
                StatAbilities.stat("ELEPHANT", "Trunk Efficiency", "Grants +#☘ Farming Fortune, which increases your chance for multiple drops.",
                        Stat.FARMING_FORTUNE),
                StatAbilities.stat("MONKEY", "Treeborn", "Grants +# ☘ Foraging Fortune, which increases your chance at double logs.", Stat.FORAGING_FORTUNE),
                StatAbilities.stat("OCELOT", "Foraging Wisdom Boost", "Grants +#☯ Foraging Wisdom.", Stat.FORAGING_WISDOM),
                StatAbilities.stat("RABBIT", "Farming Wisdom Boost", "Grants +#☯ Farming Wisdom.", Stat.FARMING_WISDOM),
                StatAbilities.stat("SILVERFISH", "Experienced Burrower", "Grants +#☯ Mining Wisdom.", Stat.MINING_WISDOM),
                StatAbilities.stat("SLUG", "Pest Friends", "Grants +#ൠ Bonus Pest Chance.", Stat.BONUS_PEST_CHANCE),
                new Ability("SILVERFISH", "Dexterity", "Grants +#⸕ Mining Speed and permanent Haste #.") {
                    // Where mining is SkyBlock's own (all of it here) the Haste is Mining Speed, and "does not stack with the
                    // conversion of Haste from other sources" (the wiki's Silverfish Pet): only what it has past a Haste Ring's.
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        double haste = Math.max(0, a.n(1) - AccessoryEffects.hasteLevel(player));
                        stats.add(Stat.MINING_SPEED, a.n(0) + AccessoryEffects.HASTE_MINING_SPEED * haste);
                    }
                },
                // Where.
                new Ability("CHICKEN", "Free Range", "Grants +#☘ Farming Fortune while on Public Islands.") {
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        if (onPublicIsland()) stats.add(Stat.FARMING_FORTUNE, a.n(0));
                    }
                },
                new Ability("CHICKEN", "Light Feet", "Reduces fall damage by #%.") {
                    @Override
                    public double vanillaDamage(Player player, Active a, VanillaDamage.Cause cause) {
                        return cause == VanillaDamage.Cause.FALL ? Math.max(0, 1 - a.n(0) / 100) : 1;
                    }
                },
                new Ability("MITHRIL_GOLEM", "Subterranean Battler", "Increases all Combat Stats by +#% on Mining Islands.") {
                    @Override
                    public void derivedStats(Player player, Active a, Stats stats) {
                        if (onMiningIsland()) Ability.scale(stats, COMBAT, a.n(0));
                    }
                },
                new Ability("MITHRIL_GOLEM", "Refined Senses", "Grants +#% ✯ Magic Find while on Mining Islands.") {
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        if (onMiningIsland()) stats.add(Stat.MAGIC_FIND, a.n(0));
                    }
                },
                new Ability("KUUDRA", "Kuudra Fortune", "Grants +#☘ Mining Fortune while on the Crimson Isle.") {
                    @Override
                    public boolean always() {
                        return true;
                    }

                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        if (onCrimsonIsle()) stats.add(Stat.MINING_FORTUNE, a.n(0));
                    }
                },
                new Ability("KUUDRA", "Trophy Bait", "Grants +#♔ Trophy Chance while on the Crimson Isle.") {
                    @Override
                    public boolean always() {
                        return true;
                    }

                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        if (onCrimsonIsle()) stats.add(Stat.TROPHY_FISH_CHANCE, a.n(0));
                    }
                },
                // Mining.
                new Ability("MITHRIL_GOLEM", "Mithril Affinity", "Grants +#* Mining Speed when mining Mithril.") {
                    @Override
                    public double miningSpeed(Player player, Active a, MinableBlock block) {
                        return block.powderType() == PowderType.MITHRIL ? a.n(0) : 0;
                    }
                },
                powder("MITHRIL_GOLEM", "The Smell Of Powder", "Grants +#% ᠅ Mithril Powder from all sources.", PowderType.MITHRIL),
                powder("SCATHA", "Bejeweled Eyes", "Earn +#% Gemstone Powder from all sources.", PowderType.GEMSTONE),
                powder("GLACITE_GOLEM", "Powder-powered", "#% more Glacite Powder from most sources.", PowderType.GLACITE),
                new Ability("SCATHA", "Drill Infusion", "Grants +#☘ Gemstone Fortune to Drills.") {
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        SkyBlockItem held = heldItem(player);
                        if (held != null && held.specificItemType() == SpecificItemType.DRILL) stats.add(Stat.GEMSTONE_FORTUNE, a.n(0));
                    }
                },
                new Ability("SILVERFISH", "Magnetic", "Earn +#% more Exp when mining.") {
                    @Override
                    public double experience(Player player, Active a, ExpOrbs.Source source) {
                        return source == ExpOrbs.Source.ORE ? a.n(0) : 0;
                    }
                },
                // Stats from stats.
                per("ELEPHANT", "Stomp", "Gain #❈ Defense for every # ✦ Speed.", Stat.SPEED, Stat.DEFENSE),
                per("ELEPHANT", "Walking Fortress", "Gain #❤ Health for every # ❈ Defense.", Stat.DEFENSE, Stat.HEALTH),
                new Ability("MOOSHROOM_COW", "Farming Strength", "Grants +#☘ Farming Fortune for every # ❁ Strength you have.") {
                    // "⌊Strength / 20 × 0.7⌋" (the wiki's Mooshroom Cow Pet): rounded down once, not per 20.
                    @Override
                    public void derivedStats(Player player, Active a, Stats stats) {
                        if (a.n(1) > 0) stats.add(Stat.FARMING_FORTUNE, Math.floor(Math.max(0, stats.get(Stat.STRENGTH)) / a.n(1) * a.n(0)));
                    }
                },
                per("JADE_DRAGON", "Dragon's Pride", "Grants #☘ Foraging Fortune per #∮ Sweep.", Stat.SWEEP, Stat.FORAGING_FORTUNE),
                per("MAMMOTH", "Tusk Luck", "Gain +#✯ Magic Find for every # ☘ Mining Fortune, doubled in the Glacite Tunnels and Glacite Mineshafts.",
                        Stat.MINING_FORTUNE, Stat.MAGIC_FIND),
                new Ability("ORCHID_MANTIS", "Swift Sickles", "Convert every # ✦ Speed you have above # into +#☘ Farming Fortune.") {
                    // It keeps their Speed and counts it only up to their speed cap, unlike the Snail's (the wiki's Orchid Mantis Pet).
                    @Override
                    public void derivedStats(Player player, Active a, Stats stats) {
                        double steps = steps(Math.min(stats.get(Stat.SPEED), PlayerAttributes.speedCap(player)), a);
                        stats.add(Stat.FARMING_FORTUNE, steps * a.n(2));
                    }
                },
                new Ability("SNAIL", "Slow and Steady", "Convert every #✦ Speed you have above # into +#☘ Block Fortune.") {
                    // It takes the Speed it converts, past the speed cap too ("Every Speed buff, even beyond the speed cap, counts", the wiki).
                    @Override
                    public void derivedStats(Player player, Active a, Stats stats) {
                        double steps = steps(stats.get(Stat.SPEED), a);
                        stats.add(Stat.SPEED, -steps * a.n(0)).add(Stat.BLOCK_FORTUNE, steps * a.n(2));
                    }
                },
                // From skill levels, collections and what they hold.
                new Ability("AMMONITE", "Gift of the Ammonite", "Each Mining and Fishing level grants +#☂ Fishing Speed, +#✦ Speed and +#❈ Defense.") {
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        User user = User.ifLoaded(player.getUniqueId());
                        if (user == null || user.isReleased()) return;
                        int levels = Skills.level(user.profile(), Skill.MINING) + Skills.level(user.profile(), Skill.FISHING);
                        stats.add(Stat.FISHING_SPEED, a.n(0) * levels).add(Stat.SPEED, a.n(1) * levels).add(Stat.DEFENSE, a.n(2) * levels);
                    }
                },
                new Ability("ROSE_DRAGON", "Garden Power", "Grants +#☘ Farming Fortune per Farming level.") {
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        User user = User.ifLoaded(player.getUniqueId());
                        if (user != null && !user.isReleased()) stats.add(Stat.FARMING_FORTUNE, a.n(0) * Skills.level(user.profile(), Skill.FARMING));
                    }
                },
                new Ability("JADE_DRAGON", "Jade Scale",
                        "Grants #☘ Foraging Fortune and #∮ Sweep for every digit in your Mangrove Collection. (Max #M collection)") {
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        User user = User.ifLoaded(player.getUniqueId());
                        if (user == null || user.isReleased()) return;
                        int digits = StatAbilities.ShiningScales.digits(Collections.count(user.profile(), "MANGROVE_LOG"), (long) (a.n(2) * 1_000_000));
                        stats.add(Stat.FORAGING_FORTUNE, a.n(0) * digits).add(Stat.SWEEP, a.n(1) * digits);
                    }
                },
                new Ability("JADE_DRAGON", "Forest Power", "Adds #❁ Strength and #✦ Speed to all your Axes.") {
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        SkyBlockItem held = heldItem(player);
                        if (held != null && held.specificItemType() == SpecificItemType.AXE) stats.add(Stat.STRENGTH, a.n(0)).add(Stat.SPEED, a.n(1));
                    }
                },
                symbiosis("JADE_DRAGON", "If you own a level # Jade Dragon, grants +#☘ Foraging Fortune for every other unique maxed Foraging Pet that you own.",
                        "Foraging", Stat.FORAGING_FORTUNE),
                symbiosis("ROSE_DRAGON", "If you own a level # Rose Dragon, grants +#☘ Farming Fortune for every other unique maxed Farming Pet that you own.",
                        "Farming", Stat.FARMING_FORTUNE),
                // Players near.
                perPlayer("DOLPHIN", "Pod Tactics", "Grants +#☂ Fishing Speed for each player within # blocks, up to # players.", Stat.FISHING_SPEED, null),
                perPlayer("PENGUIN", "Chilly Reception", "Grants +#❄ Cold Resistance for each player within # blocks, up to # players.",
                        Stat.COLD_RESISTANCE, null),
                perPlayer("HERMIT_CRAB", "Crab Rave", "Grants +#⛃ Treasure Chance for each player with a Hermit Crab Pet within # blocks, up to # players.",
                        Stat.TREASURE_CHANCE, "HERMIT_CRAB"),
                new HappyTreeFriends("Grants #☘ Foraging Fortune for every other Frog Pet on the island, up to # frogs.", false),
                new HappyTreeFriends("Grants #☘ Foraging Fortune and #☂ Fishing Speed for every other Frog Pet on the island, up to # frogs.", true),
                new Ability("FROG", "Hop", "Grants #☘ Foraging Fortune for #s every time you jump. Gain Jump Boost IV while on Foraging Islands.") {
                    // A jump starts it again (UNKNOWN whether Hypixel's stack); there are no Foraging Islands here for the Jump Boost.
                    @Override
                    public void jumped(Player player, Active a) {
                        PlayerSession.of(player).buff(source(player), new Stats().add(Stat.FORAGING_FORTUNE, a.n(0)), (long) (a.n(1) * 1000));
                    }
                },
                // The Wisps' Blaze Slayer, as the slayer pets' Combat Wisdom.
                RewardAbilities.killXp("DROPLET_WISP", "Blaze Slayer", "Gain +#☯ Combat Wisdom towards Blazes.", type -> type == EntityType.BLAZE, false),
                RewardAbilities.killXp("FROST_WISP", "Blaze Slayer", "Gain +#☯ Combat Wisdom towards Blazes.", type -> type == EntityType.BLAZE, false),
                RewardAbilities.killXp("GLACIAL_WISP", "Blaze Slayer", "Gain +#☯ Combat Wisdom towards Blazes.", type -> type == EntityType.BLAZE, false),
                RewardAbilities.killXp("SUBZERO_WISP", "Blaze Slayer", "Gain +#☯ Combat Wisdom towards Blazes.", type -> type == EntityType.BLAZE, false),
                new Ability("SUBZERO_WISP", "Ephemeral Stability", "Regenerate mana #% faster") {
                    @Override
                    public double manaRegen(Player player, Active a) {
                        return a.n(0) / 100;
                    }
                },
                new Ability("ENDERMITE", "Daily Commuter", "Transmission Abilities cost #% less mana") {
                    @Override
                    public double itemManaCost(Player player, Active a, NBTTagCompound item) {
                        String id = item == null ? "" : item.getString("id").toUpperCase(Locale.ROOT);
                        return id.startsWith("ASPECT_OF_THE_END") || id.startsWith("ASPECT_OF_THE_VOID") ? 1 - a.n(0) / 100 : 1;
                    }
                },
                // The Pets menu's pets ("This pet's perks are active even when the pet is not summoned!"). The Bingo Pet's
                // aren't here: they "do not work on non-Bingo profiles" (the wiki's Bingo Pet), and there are none.
                new Ability("OWL", "Fast Learner", "Passively grants +#☯ Taming Wisdom.") {
                    @Override
                    public boolean always() {
                        return true;
                    }

                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        stats.add(Stat.TAMING_WISDOM, a.n(0));
                    }
                },
                new Ability("SPIRIT", "Spirit Cooldowns", "Reduces the cooldown of your ghost abilities in dungeons by #%.") {
                    @Override
                    public boolean always() {
                        return true;
                    }

                    @Override
                    public double cooldown(Player player, Active a, ItemBlock block) {
                        return RunItems.ghost(player) ? 1 - a.n(0) / 100 : 1;
                    }
                },
                new HalfLife());
    }

    /** "Grants +#% ᠅ Mithril Powder from all sources.": more of that powder from what they mine. */
    static PetAbility powder(String pet, String name, String text, PowderType type) {
        return new Ability(pet, name, text) {
            @Override
            public double powder(Player player, Active a, PowderType mined) {
                return mined == type ? a.n(0) : 0;
            }
        };
    }

    /** "Gain #❈ Defense for every # ✦ Speed.": so much of one stat for each whole so much of another. */
    static PetAbility per(String pet, String name, String text, Stat from, Stat to) {
        return new Ability(pet, name, text) {
            @Override
            public void derivedStats(Player player, Active a, Stats stats) {
                if (a.n(1) > 0) stats.add(to, a.n(0) * Math.floor(Math.max(0, stats.get(from)) / a.n(1)));
            }
        };
    }

    /** "Convert every 3 ✦ Speed you have above 100": how many whole 3s of this Speed are over 100. */
    static double steps(double speed, PetAbility.Active a) {
        double over = speed - a.n(1);
        return over > 0 && a.n(0) > 0 ? Math.floor(over / a.n(0)) : 0;
    }

    /**
     * "If you own a level 200 Jade Dragon, grants +4☘ Foraging Fortune for every other unique maxed Foraging Pet that you
     * own.": the pets of that kind in their menu at their most level, a type once.
     */
    static PetAbility symbiosis(String pet, String text, String kind, Stat stat) {
        return new Ability(pet, "Symbiosis", text) {
            @Override
            public void stats(Player player, Active a, Stats stats) {
                if (a.level() >= a.n(0)) stats.add(stat, a.n(1) * maxedPets(player, pet, kind));
            }
        };
    }

    /**
     * "Grants +10☂ Fishing Speed for each player within 30 blocks, up to 5 players.": the others near them (with a pet of
     * that type summoned, for one that says so).
     */
    static PetAbility perPlayer(String pet, String name, String text, Stat stat, String withPet) {
        return new Ability(pet, name, text) {
            @Override
            public void stats(Player player, Active a, Stats stats) {
                int count = 0;
                for (Player other : near(player, a.n(1))) if (withPet == null || summons(other, withPet)) count++;
                stats.add(stat, a.n(0) * Math.min(count, (int) a.n(2)));
            }
        };
    }

    /** Whether they have a pet of this type summoned (its abilities count for them, and they're its, not the menu's). */
    static boolean summons(Player player, String type) {
        for (PetAbility.Active a : PetAbilities.active(player)) if (a.pet().type().equals(type) && !a.ability().always()) return true;
        return false;
    }

    /**
     * The Frog's Happy Tree Friends: "Grants 10.0☘ Foraging Fortune (and 2.0☂ Fishing Speed) for every other Frog Pet on
     * the island, up to 10 frogs": the others in their world with one summoned.
     */
    static final class HappyTreeFriends extends Ability {
        private final boolean fishing;

        HappyTreeFriends(String text, boolean fishing) {
            super("FROG", "Happy Tree Friends", text);
            this.fishing = fishing;
        }

        @Override
        public void stats(Player player, Active a, Stats stats) {
            int frogs = 0;
            for (Player other : player.getWorld().getPlayers()) if (other != player && PetAbilities.inPlay(other) && summons(other, "FROG")) frogs++;
            frogs = Math.min(frogs, (int) a.n(fishing ? 2 : 1));
            stats.add(Stat.FORAGING_FORTUNE, a.n(0) * frogs);
            if (fishing) stats.add(Stat.FISHING_SPEED, a.n(1) * frogs);
        }
    }

    /**
     * The Spirit's Half Life: "If you are the first player to die in a dungeon, the score penalty for that death is reduced
     * to 1", from the Pets menu too: the score asks whether it's theirs (see PetAbilities#enable and DungeonRun#setHalfLife).
     */
    static final class HalfLife extends Ability {
        HalfLife() {
            super("SPIRIT", "Half Life", "If you are the first player to die in a dungeon, the score penalty for that death is reduced to #.");
        }

        @Override
        public boolean always() {
            return true;
        }
    }
}
