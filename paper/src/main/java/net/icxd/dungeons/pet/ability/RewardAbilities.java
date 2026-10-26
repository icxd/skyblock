package net.icxd.dungeons.pet.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.economy.ExpOrbs;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.DropEnchants;
import net.icxd.dungeons.mob.KillingBlow;
import net.icxd.dungeons.mob.MobDrop;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.session.Absorption;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Pets' abilities on kills, experience, coins and drops, mana and cooldowns, and what they let them do (flight, Jump
 * Boost), their numbers from their text.
 */
final class RewardAbilities {
    /** How long Overheal's shield lasts once it's up: until it's used (as good as). */
    static final long OVERHEAL_MILLIS = 60 * 60_000;
    /** How long High Stride's Jump Boost is given for, again each second. */
    static final int JUMP_TICKS = 3 * 20;

    private RewardAbilities() {
    }

    static List<PetAbility> all() {
        return List.of(
                // Combat XP on kills of a kind: multiplicative (the wiki's Wolf Pet), as "Combat Wisdom towards" reads.
                killXp("ENDERMAN", "Enderman Slayer", "Gain +#☯ Combat Wisdom towards Endermen.", type -> type == EntityType.ENDERMAN, false),
                killXp("GHOUL", "Undead Slayer", "Gain +#☯ Combat Wisdom towards Zombies.",
                        type -> type == EntityType.ZOMBIE || type == EntityType.ZOMBIE_VILLAGER || type == EntityType.HUSK || type == EntityType.DROWNED, false),
                killXp("TARANTULA", "Arachnid Slayer", "Gain +#☯ Combat Wisdom towards Spiders.",
                        type -> type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER, false),
                killXp("HOUND", "Pack Slayer", "Gain #x Combat XP against Wolves.", type -> type == EntityType.WOLF, true),
                // Coins.
                new Ability("HOUND", "Scavenger", "Gain +# coins per monster kill") {
                    @Override
                    public void killed(Player player, Active a, SkyBlockMobDeathEvent event) {
                        coins(player, a.n(0));
                    }
                },
                new Symbiosis(),
                new Ability("PHOENIX", "Eternal Coins", "Don't lose coins from death.") {
                    @Override
                    public double deathCoins(Player player, Active a) {
                        return 1;
                    }
                },
                new KillCombo(),
                new WebBattlefield("SPIDER"),
                new WebBattlefield("TARANTULA"),
                // Drops and experience.
                new Ability("HOUND", "Finder", "Increases the chance for monsters to drop their armor by #%.") {
                    @Override
                    public double dropChance(Player killer, Active a, KillingBlow blow, MobDrop drop) {
                        return DropEnchants.armor(drop) ? 1 + a.n(0) / 100 : 1;
                    }
                },
                new Ability("BLACK_CAT", "Looting", "Gain #% more collection items from monsters!") {
                    // More of a drop is a chance past all of it: past 100% a drop drops again (Mobs.copies).
                    @Override
                    public double dropChance(Player killer, Active a, KillingBlow blow, MobDrop drop) {
                        return collectionItem(drop) ? 1 + a.n(0) / 100 : 1;
                    }
                },
                new Ability("RIFT_FERRET", "Orbs are Fun", "Gain +#% experience from XP Orbs.") {
                    @Override
                    public double experience(Player player, Active a, ExpOrbs.Source source) {
                        return a.n(0);
                    }
                },
                // Mana and cooldowns.
                new Ability("SHEEP", "Mana Saver", "Reduces the mana cost of abilities by #%.") {
                    @Override
                    public double manaCost(Player player, Active a) {
                        return 1 - a.n(0) / 100;
                    }
                },
                new Ability("TARANTULA", "Eight Legs", "Decreases the mana cost of Spider, Tarantula, Primordial and Spirit Boots by #%.") {
                    private static final Set<String> BOOTS = Set.of("SPIDER_BOOTS", "TARANTULA_BOOTS", "PRIMORDIAL_BOOTS", "SPIRIT_BOOTS");

                    @Override
                    public double itemManaCost(Player player, Active a, NBTTagCompound item) {
                        return item != null && BOOTS.contains(item.getString("id").toUpperCase()) ? 1 - a.n(0) / 100 : 1;
                    }
                },
                new Ability("CROW", "Quick Hands", "Lowers the cooldown of your Weapon abilities by #.") {
                    // "By 15.0": percent, as the other cooldown cuts say (UNKNOWN; the text has no "%").
                    @Override
                    public double cooldown(Player player, Active a, ItemBlock block) {
                        return holdsWeapon(player) ? Math.max(0, 1 - a.n(0) / 100) : 1;
                    }
                },
                new Ability("GUARDIAN", "Mana Pool", "Regenerate #% extra mana, doubled when near or in water") {
                    private static final Set<Material> WATER = Set.of(Material.WATER, Material.BUBBLE_COLUMN);

                    @Override
                    public double manaRegen(Player player, Active a) {
                        boolean water = player.isInWater() || StatAbilities.nearBlock(player, StatAbilities.NEAR_LIQUID, WATER);
                        return a.n(0) / 100 * (water ? 2 : 1);
                    }
                },
                new Camouflage(),
                new Ability("CROW", "Insightful", "Gives a # chance to not consume Mana when using an ability.") {
                    // The mana comes back as it's spent ("15.0 chance": percent, UNKNOWN).
                    @Override
                    public void manaSpent(Player player, Active a, int amount, String source) {
                        PlayerSession session = PlayerSession.of(player);
                        // Below none it's full (the pool isn't known yet).
                        if (session.getMana() < 0 || ThreadLocalRandom.current().nextDouble() * 100 >= a.n(0)) return;
                        session.setMana(Math.min(session.maxMana(), session.getMana() + amount));
                    }
                },
                new Ability("ENDERMAN", "Teleport Savvy", "Buffs Transmission abilities, granting # weapon damage for #s on use") {
                    @Override
                    public void manaSpent(Player player, Active a, int amount, String source) {
                        if (source == null || !source.contains("Transmission")) return;
                        PlayerSession.of(player).buff(source(player), new Stats().add(Stat.DAMAGE, a.n(0)), (long) (a.n(1) * 1000));
                    }
                },
                new Overheal(),
                new StoredEnergy(),
                // What they can do.
                new HighStride("HORSE", "Grants permanent Jump Boost II.", 2),
                new HighStride("HORSE", "Grants permanent Jump Boost III.", 3),
                new HighStride("HORSE", "Grants permanent Jump Boost IV.", 4),
                new HighStride("SKELETON_HORSE", "Grants permanent Jump Boost IV.", 4),
                new MagicBird(),
                new Ability("PARROT", "Flamboyant", "Adds # levels to intimidation accessories") {
                    @Override
                    public double intimidation(Player player, Active a) {
                        return a.n(0);
                    }
                },
                // The Jerry's, which do as they say: nothing (and a Jerry Candy "Tiny chance" isn't a number).
                new Ability("JERRY", "Jerry", "Gain #% chance to deal your regular damage") {
                },
                new Ability("JERRY", "Jerry", "Gain #% chance to receive a normal amount of drops from mobs") {
                },
                new Ability("JERRY", "Jerry", "Tiny chance to find Jerry Candies when killing mobs") {
                });
    }

    /**
     * "Gain +50☯ Combat Wisdom towards Zombies" (as 1.5x: multiplicative, the wiki) or "Gain 1.5x Combat XP against Wolves"
     * ({@code times}): the Combat XP of a kill of one of those.
     */
    static PetAbility killXp(String pet, String name, String text, Predicate<EntityType> kinds, boolean times) {
        return new Ability(pet, name, text) {
            @Override
            public double killXp(Player player, Active a, SkyBlockMobDeathEvent event) {
                if (event.kind() == null || !kinds.test(event.kind().entityType())) return 1;
                return times ? a.n(0) : 1 + a.n(0) / 100;
            }
        };
    }

    /** Coins into their purse. */
    static void coins(Player player, double coins) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user != null && !user.isReleased() && coins > 0) Purse.add(user, coins);
    }

    /** Whether every item a drop can be counts in a collection (an enchanted one too: it counts as its base's). */
    static boolean collectionItem(MobDrop drop) {
        if (drop.itemIds().isEmpty()) return false;
        for (String id : drop.itemIds()) if (Collections.data().counted(id) == null) return false;
        return true;
    }

    /**
     * The Golden Dragon's Symbiosis: "If you own a level 200 Golden Dragon, gain +5 coins per monster kill for every other
     * unique maxed Combat Pet that you own": its own level, and the Combat Pets in their menu at their most level, a type
     * once.
     */
    static final class Symbiosis extends Ability {
        Symbiosis() {
            super("GOLDEN_DRAGON", "Symbiosis",
                    "If you own a level # Golden Dragon, gain +# coins per monster kill for every other unique maxed Combat Pet that you own.");
        }

        @Override
        public void killed(Player player, Active a, SkyBlockMobDeathEvent event) {
            if (a.level() >= a.n(0)) coins(player, a.n(1) * maxedPets(player, "GOLDEN_DRAGON", "Combat"));
        }
    }

    /**
     * The Grandma Wolf's Kill Combo: "Gain buffs for combo kills. Effects stack as you increase your combo.", its table's
     * tiers (5 Combo, lasting 10s: +3% ✯ Magic Find; 10: +10 coins per kill; 15: +3% Magic Find; 20: +15☯ Combat Wisdom;
     * 25: +3% Magic Find; 30: +10 coins per kill), from the Pets menu too. A kill within the time of the tier reached
     * (the first's before it, UNKNOWN) adds one, and past it the combo ends; the Magic Find is flat ("an additive
     * percentage bonus", the wiki); none of it on Private Islands. The chat lines say "+5 Kill Combo" and "Your Kill Combo
     * has expired!" (Danker's Skyblock Mod hides lines with those); the rest of them is UNKNOWN.
     */
    static final class KillCombo extends Ability {
        /** A tier's numbers' places in the text: its combo, how long it lasts, what it gives. */
        private static final int TIERS = 6;
        private final Map<UUID, Integer> combo = new HashMap<>();
        private final Map<UUID, Long> lastKill = new HashMap<>();

        KillCombo() {
            super("GRANDMA_WOLF", "Kill Combo", "# Combo (lasts #s) +#% ✯ Magic Find # Combo (lasts #s) +# coins per kill # Combo (lasts #s) +#% ✯ Magic Find "
                    + "# Combo (lasts #s) +#☯ Combat Wisdom # Combo (lasts #s) +#% ✯ Magic Find # Combo (lasts #s) +# coins per kill");
        }

        @Override
        public boolean always() {
            return true;
        }

        private static double combo(Active a, int tier) {
            return a.n(tier * 3);
        }

        private static double lasts(Active a, int tier) {
            return a.n(tier * 3 + 1);
        }

        private static double gives(Active a, int tier) {
            return a.n(tier * 3 + 2);
        }

        /** How long they have for the next kill at this combo: the highest tier's they've reached, the first's before it. */
        private static long window(Active a, int combo) {
            double seconds = lasts(a, 0);
            for (int tier = 0; tier < TIERS; tier++) if (combo >= combo(a, tier)) seconds = lasts(a, tier);
            return (long) (seconds * 1000);
        }

        /** What its tiers of one kind (0: Magic Find, 1: coins, 3: Combat Wisdom) give at this combo. */
        private static double total(Active a, int combo, int kind) {
            double total = 0;
            for (int tier = 0; tier < TIERS; tier++) {
                int of = tier == 3 ? 3 : tier % 2 == 0 ? 0 : 1;
                if (of == kind && combo >= combo(a, tier)) total += gives(a, tier);
            }
            return total;
        }

        @Override
        public void stats(Player player, Active a, Stats stats) {
            int n = combo.getOrDefault(player.getUniqueId(), 0);
            if (n <= 0) return;
            stats.add(Stat.MAGIC_FIND, total(a, n, 0)).add(Stat.COMBAT_WISDOM, total(a, n, 3));
        }

        @Override
        public void killed(Player player, Active a, SkyBlockMobDeathEvent event) {
            if (!onPublicIsland()) return;
            UUID id = player.getUniqueId();
            long now = System.currentTimeMillis();
            int n = combo.getOrDefault(id, 0);
            if (n > 0 && now - lastKill.getOrDefault(id, 0L) > window(a, n)) {
                expired(player, n);
                n = 0;
            }
            n++;
            combo.put(id, n);
            lastKill.put(id, now);
            coins(player, total(a, n, 1));
            if (n % 5 == 0) player.sendMessage(Text.line("&a&l+" + n + " Kill Combo" + tierLine(a, n)));
            PlayerSession.of(player).invalidateStats();
        }

        /** What reaching this combo gave, for its line: "&8+&b3% ✯ Magic Find"; nothing past the table. */
        private static String tierLine(Active a, int n) {
            for (int tier = 0; tier < TIERS; tier++) {
                if (combo(a, tier) != n) continue;
                String amount = Text.number(gives(a, tier));
                return switch (tier == 3 ? 3 : tier % 2 == 0 ? 0 : 1) {
                    case 0 -> " &8+&b" + amount + "% ✯ Magic Find";
                    case 1 -> " &8+&6" + amount + " coins per kill";
                    default -> " &8+&3" + amount + "☯ Combat Wisdom";
                };
            }
            return "";
        }

        @Override
        public void second(Player player, Active a) {
            UUID id = player.getUniqueId();
            int n = combo.getOrDefault(id, 0);
            if (n > 0 && System.currentTimeMillis() - lastKill.getOrDefault(id, 0L) > window(a, n)) {
                expired(player, n);
                forget(id);
                PlayerSession.of(player).invalidateStats();
            }
        }

        private static void expired(Player player, int n) {
            player.sendMessage(Text.line("&cYour Kill Combo has expired! You reached a " + n + " Kill Combo"));
        }

        @Override
        public void ended(Player player) {
            forget(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            combo.remove(player);
            lastKill.remove(player);
        }
    }

    /**
     * The Spider's and Tarantula's Web Battlefield: "Killing mobs grants +6.00❁ Strength and +1.00✯ Magic Find for 40s to
     * all players staying within 20 blocks of where they died. Stacks up to 10 times.": each kill a battlefield where the
     * mob died; whoever's within its reach while it lasts gets its stats, the strongest of them up to the stacks.
     */
    static final class WebBattlefield extends Ability {
        /** The battlefields kept in a world at most (the oldest go). */
        static final int KEPT = 200;
        private static final Map<World, List<Field>> FIELDS = new HashMap<>();

        /** Where a mob died, until when, and what it gives. */
        private record Field(Location at, long until, double strength, double magicFind, double range, int stacks) {
        }

        static {
            PetAbilities.addAnyoneStats(WebBattlefield::anyone);
        }

        WebBattlefield(String pet) {
            super(pet, "Web Battlefield", "Killing mobs grants +#❁ Strength and +#✯ Magic Find for #s to all players staying within # blocks of where they died. "
                    + "Stacks up to # times.");
        }

        @Override
        public void killed(Player player, Active a, SkyBlockMobDeathEvent event) {
            Location at = event.location();
            if (at.getWorld() == null) return;
            List<Field> fields = FIELDS.computeIfAbsent(at.getWorld(), w -> new ArrayList<>());
            fields.add(new Field(at, System.currentTimeMillis() + (long) (a.n(2) * 1000), a.n(0), a.n(1), a.n(3), (int) a.n(4)));
            while (fields.size() > KEPT) fields.remove(0);
        }

        private static void anyone(Player player, Stats stats) {
            List<Field> fields = FIELDS.get(player.getWorld());
            if (fields == null || fields.isEmpty()) return;
            long now = System.currentTimeMillis();
            fields.removeIf(f -> f.until() <= now);
            List<Field> in = new ArrayList<>();
            int stacks = 0;
            for (Field f : fields) {
                if (f.at().distanceSquared(player.getLocation()) > f.range() * f.range()) continue;
                in.add(f);
                stacks = Math.max(stacks, f.stacks());
            }
            in.sort((x, y) -> Double.compare(y.strength(), x.strength()));
            for (int i = 0; i < Math.min(stacks, in.size()); i++) stats.add(Stat.STRENGTH, in.get(i).strength()).add(Stat.MAGIC_FIND, in.get(i).magicFind());
        }
    }

    /**
     * The Crow's Camouflage: "After casting an ability, increase your ❈ Defense by +20.0 for 20 seconds. Capped at 500
     * Defense": each cast adds to it, all of it lasting from the last (UNKNOWN whether each cast's lasts on its own).
     */
    static final class Camouflage extends Ability {
        private final Map<UUID, Double> defense = new HashMap<>();
        private final Map<UUID, Long> until = new HashMap<>();

        Camouflage() {
            super("CROW", "Camouflage", "After casting an ability, increase your ❈ Defense by +# for # seconds. Capped at # Defense");
        }

        @Override
        public void stats(Player player, Active a, Stats stats) {
            if (System.currentTimeMillis() < until.getOrDefault(player.getUniqueId(), 0L)) {
                stats.add(Stat.DEFENSE, defense.getOrDefault(player.getUniqueId(), 0.0));
            }
        }

        @Override
        public void manaSpent(Player player, Active a, int amount, String source) {
            UUID id = player.getUniqueId();
            long now = System.currentTimeMillis();
            double before = now < until.getOrDefault(id, 0L) ? defense.getOrDefault(id, 0.0) : 0;
            defense.put(id, Math.min(a.n(2), before + a.n(0)));
            until.put(id, now + (long) (a.n(1) * 1000));
            PlayerSession.of(player).invalidateStats();
        }

        @Override
        public void forget(UUID player) {
            defense.remove(player);
            until.remove(player);
        }
    }

    /**
     * The Jellyfish's Stored Energy: "While in dungeons, for every 2,000 HP you heal teammates the cooldown of Wish is
     * reduced by 1s, up to 30s": each whole 2,000 of their Ally Healing takes that off a Wish cooldown, none while Wish is
     * ready, and no more than 30s off one cooldown (UNKNOWN: whether healing while it's ready counts, and whether the 30s
     * is a cooldown's or a run's).
     */
    static final class StoredEnergy extends Ability {
        private final Map<UUID, Double> healed = new HashMap<>();

        StoredEnergy() {
            super("JELLYFISH", "Stored Energy", "While in dungeons, for every # HP you heal teammates the cooldown of Wish is reduced by #s, up to #s.");
        }

        @Override
        public void healedAllies(Player player, Active a, double healing) {
            DungeonRun run = RunManager.of(player);
            if (run == null || a.n(0) <= 0) return;
            double sum = healed.getOrDefault(player.getUniqueId(), 0.0) + healing;
            long steps = (long) (sum / a.n(0));
            healed.put(player.getUniqueId(), sum - steps * a.n(0));
            if (steps > 0) run.cutUltimate(player.getUniqueId(), DungeonClass.HEALER, (long) (steps * a.n(1) * 1000), (long) (a.n(2) * 1000));
        }

        @Override
        public void forget(UUID player) {
            healed.remove(player);
        }
    }

    /**
     * The Sheep's Overheal: "Gives a 10.0% shield after not taking damage for 10s": absorption of that share of their
     * max health, given once a second when they've gone that long unhurt and have none of it left.
     */
    static final class Overheal extends Ability {
        private final Map<UUID, Long> lastHurt = new HashMap<>();

        Overheal() {
            super("SHEEP", "Overheal", "Gives a #% shield after not taking damage for #s");
        }

        @Override
        public void hurt(Player player, Active a, org.bukkit.entity.Entity by, double taken) {
            lastHurt.put(player.getUniqueId(), System.currentTimeMillis());
        }

        @Override
        public void second(Player player, Active a) {
            long since = System.currentTimeMillis() - lastHurt.getOrDefault(player.getUniqueId(), 0L);
            if (since < a.n(1) * 1000 || Absorption.left(player, source(player)) > 0) return;
            Absorption.give(player, source(player), PlayerHealth.max(player) * a.n(0) / 100, OVERHEAL_MILLIS);
        }

        @Override
        public void ended(Player player) {
            Absorption.remove(player, source(player));
        }

        @Override
        public void forget(UUID player) {
            lastHurt.remove(player);
        }
    }

    /** The Horses' High Stride: "Grants permanent Jump Boost IV.": given a few seconds at a time, again each second. */
    static final class HighStride extends Ability {
        private final int level;

        HighStride(String pet, String text, int level) {
            super(pet, "High Stride", text);
            this.level = level;
        }

        @Override
        public void second(Player player, Active a) {
            PotionEffect now = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
            if (now != null && now.getAmplifier() > level - 1) return;
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, JUMP_TICKS, level - 1, false, false, false));
        }

        @Override
        public void ended(Player player) {
            PotionEffect now = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
            if (now != null && now.getDuration() <= JUMP_TICKS && !now.hasParticles()) player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        }
    }

    /**
     * The Phoenix's Magic Bird: "You may always fly on your private island and garden": on their own island they may fly
     * (there's no Garden here); off it, or with the pet away, the flight it gave goes.
     */
    static final class MagicBird extends Ability {
        private final Set<UUID> flying = new HashSet<>();

        MagicBird() {
            super("PHOENIX", "Magic Bird", "You may always fly on your private island and garden");
        }

        @Override
        public void second(Player player, Active a) {
            if (onOwnIsland(player)) {
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                    flying.add(player.getUniqueId());
                }
            } else {
                ended(player);
            }
        }

        @Override
        public void ended(Player player) {
            if (!flying.remove(player.getUniqueId())) return;
            if (player.getGameMode() == org.bukkit.GameMode.SURVIVAL || player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        }

        @Override
        public void forget(UUID player) {
            flying.remove(player);
        }
    }
}
