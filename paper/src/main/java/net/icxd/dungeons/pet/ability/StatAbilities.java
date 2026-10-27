package net.icxd.dungeons.pet.ability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.bonus.Worn;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.upgrade.Book;
import net.icxd.dungeons.pet.HeldItems;
import net.icxd.dungeons.pet.Pets;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;

/**
 * Pets' abilities that change stats, their numbers from their text: flat ones, shares of a stat ("Increases all Combat
 * stats by 10%"), stats from other stats ("Gain 50% of your Defense as Strength"), ones that need something (night, the
 * Crimson Isle, a sword, armor, nearby players) and auras for those near.
 */
final class StatAbilities {
    /** "Near water or lava": within this many blocks (UNKNOWN: the text doesn't say). */
    static final int NEAR_LIQUID = 3;
    /** A "nearby wolf monster": within this many blocks (UNKNOWN). */
    static final double NEARBY_WOLVES = 10;

    private StatAbilities() {
    }

    static List<PetAbility> all() {
        return List.of(
                // Flat stats their pets' stat lines don't list.
                stat("FLYING_FISH", "Quick Reel", "Grants +#☂ Fishing Speed.", Stat.FISHING_SPEED),
                stat("GIRAFFE", "Good Heart", "Grants +#❣ Health Regen", Stat.HEALTH_REGEN),
                stat("GUARDIAN", "Enchanting Wisdom Boost", "Grants +#☯ Enchanting Wisdom.", Stat.ENCHANTING_WISDOM),
                stat("SQUID", "Fishing Wisdom Boost", "Grants +#☯ Fishing Wisdom.", Stat.FISHING_WISDOM),
                stat("WOLF", "Combat Wisdom Boost", "Grants +#☯ Combat Wisdom.", Stat.COMBAT_WISDOM),
                // Shares of stats.
                new Ability("TIGER", "Merciless Swipe", "Gain +#% ⫽ Ferocity.") {
                    // "It multiplies the player's Ferocity" (the wiki's Tiger Pet), what everything else gives.
                    @Override
                    public void derivedStats(Player player, Active a, Stats stats) {
                        Ability.scale(stats, Stat.FEROCITY, a.n(0));
                    }
                },
                new Ability("ENDER_DRAGON", "Superior", "Increases all Combat stats and ✯ Magic Find by #%.") {
                    @Override
                    public void derivedStats(Player player, Active a, Stats stats) {
                        Ability.scale(stats, COMBAT, a.n(0));
                        Ability.scale(stats, Stat.MAGIC_FIND, a.n(0));
                    }
                },
                new Ability("BLAZE", "Nether Embodiment", "Increases Combat and Miscellaneous stats by #% while on the Crimson Isle.") {
                    @Override
                    public void derivedStats(Player player, Active a, Stats stats) {
                        if (!onCrimsonIsle()) return;
                        Ability.scale(stats, COMBAT, a.n(0));
                        Ability.scale(stats, MISC, a.n(0));
                    }
                },
                new Ability("BLUE_WHALE", "Archimedes", "Gain +#% Max ❤ Health.") {
                    // Before Bulk, which counts the Health it makes.
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        Ability.scale(stats, Stat.HEALTH, a.n(0));
                    }
                },
                new Ability("GRIFFIN", "Sacred Strength", "Gain +#% ❁ Strength when above #% ❤ Health.") {
                    @Override
                    public void derivedStats(Player player, Active a, Stats stats) {
                        if (healthShare(player, stats) * 100 > a.n(1)) Ability.scale(stats, Stat.STRENGTH, a.n(0));
                    }
                },
                new TurtleTactics(),
                // Stats from stats.
                new Ability("ANKYLOSAURUS", "Armored Tank", "Gain #% of your ❈ Defense as ❁ Strength. (Max +#)") {
                    @Override
                    public void derivedStats(Player player, Active a, Stats stats) {
                        stats.add(Stat.STRENGTH, Math.min(a.n(1), Math.max(0, stats.get(Stat.DEFENSE)) * a.n(0) / 100));
                    }
                },
                new Ability("BLUE_WHALE", "Bulk", "Gain #❈ Defense per # Max ❤ Health.") {
                    @Override
                    public void derivedStats(Player player, Active a, Stats stats) {
                        if (a.n(1) > 0) stats.add(Stat.DEFENSE, a.n(0) * Math.floor(Math.max(0, stats.get(Stat.HEALTH)) / a.n(1)));
                    }
                },
                new Ability("GIRAFFE", "Higher Ground",
                        "Increases your ☠ Crit Damage and ❁ Strength by #% for every #Ⓢ Swing Range over #Ⓢ (up to #Ⓢ)") {
                    @Override
                    public void derivedStats(Player player, Active a, Stats stats) {
                        double over = Math.min(stats.get(Stat.SWING_RANGE), a.n(3)) - a.n(2);
                        if (over <= 0 || a.n(1) <= 0) return;
                        double percent = a.n(0) * Math.floor(over / a.n(1) + 1e-9);
                        Ability.scale(stats, Stat.CRIT_DAMAGE, percent);
                        Ability.scale(stats, Stat.STRENGTH, percent);
                    }
                },
                new Ability("GOLDEN_DRAGON", "Dragon's Greed", "Grants +#%❁ Strength per #✯ Magic Find. (Max +#%)") {
                    @Override
                    public void derivedStats(Player player, Active a, Stats stats) {
                        if (a.n(1) <= 0) return;
                        double percent = Math.min(a.n(2), a.n(0) * Math.floor(Math.max(0, stats.get(Stat.MAGIC_FIND)) / a.n(1)));
                        Ability.scale(stats, Stat.STRENGTH, percent);
                    }
                },
                new ShiningScales(),
                new Tyrant(),
                // When or where.
                new Nightmare(),
                new Ability("BLACK_CAT", "Hunter", "Increases your ✦ Speed and speed cap by +#.") {
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        stats.add(Stat.SPEED, a.n(0));
                    }

                    @Override
                    public double speedCap(Player player, Active a) {
                        return a.n(0);
                    }
                },
                new Bender("Water Bender", "Gives # ❁ Strength and ❈ Defense when near water", false),
                new Bender("Lava Bender", "Gives # ❁ Strength and ❈ Defense when near water or lava", true),
                new PackLeader(),
                new Ability("BEE", "Hive", "For each player within # blocks: +#✎ Intelligence +#❁ Strength +#❈ Defense Max # players") {
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        int players = Math.min(near(player, a.n(0)).size(), (int) a.n(4));
                        stats.add(Stat.INTELLIGENCE, a.n(1) * players).add(Stat.STRENGTH, a.n(2) * players).add(Stat.DEFENSE, a.n(3) * players);
                    }
                },
                new Ability("SHEEP", "Dungeon Wizard", "Increases your total mana by #% while in dungeons") {
                    @Override
                    public double maxMana(Player player, Active a) {
                        return inDungeon(player) ? 1 + a.n(0) / 100 : 1;
                    }
                },
                // What they hold.
                held("ENDER_DRAGON", "One With The Dragon", "Buffs the Aspect of the Dragons sword by # ❁ Damage and # ❁ Strength.",
                        "ASPECT_OF_THE_DRAGON"::equals),
                held("PIGMAN", "Pork Master", "Buffs the Pigman Sword by # ❁ Damage and # ❁ Strength.", "PIGMAN_SWORD"::equals),
                held("SQUID", "Ink Specialty", "Buffs the Ink Wand by # ❁ Damage and # ❁ Strength.", "INK_WAND"::equals),
                new Ability("BABY_YETI", "Yeti Fury", "Buffs the Yeti Sword by # ❁ Damage and ✎ Intelligence and reduces its cooldown by #%.") {
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        if ("YETI_SWORD".equals(held(player))) stats.add(Stat.DAMAGE, a.n(0)).add(Stat.INTELLIGENCE, a.n(0));
                    }

                    @Override
                    public double cooldown(Player player, Active a, net.icxd.dungeons.item.data.ItemBlock block) {
                        return "YETI_SWORD".equals(held(player)) ? 1 - a.n(1) / 100 : 1;
                    }
                },
                new Ability("JERRY", "Jerry", "Actually adds # damage to the Aspect of the Jerry") {
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        String held = held(player);
                        if (held != null && held.startsWith("ASPECT_OF_THE_JERRY")) stats.add(Stat.DAMAGE, a.n(0));
                    }
                },
                new Ability("LION", "Primal Force", "Adds +# ❁ Damage and +# ❁ Strength to your weapons.") {
                    // The Damage is the weapon's ("multiplicative with other factors, such as One For All", the wiki's Lion Pet).
                    @Override
                    public void stats(Player player, Active a, Stats stats) {
                        if (holdsWeapon(player)) stats.add(Stat.DAMAGE, a.n(0)).add(Stat.STRENGTH, a.n(1));
                    }
                },
                new FusionStylePotato(),
                // What they wear.
                armor("BLAZE", "Bling Armor", "Upgrades Blaze Armor stats and ability by #%.", id -> id.startsWith("BLAZE_")),
                armor("FLYING_FISH", "Deep Sea Diver", "Increases the stats of Diver Armor and Abyssal Armor by #%.",
                        id -> id.startsWith("DIVER_") || id.startsWith("ABYSSAL_")),
                armor("FLYING_FISH", "Magmatic Diver", "Increases the stats of Diver Armor, Magma Lord Armor, and Abyssal Armor by #%.",
                        id -> id.startsWith("DIVER_") || id.startsWith("MAGMA_LORD_") || id.startsWith("ABYSSAL_")),
                armor("MAGMA_CUBE", "Hot Ember", "Buffs the stats of Rekindled Ember Armor by #%.", id -> id.startsWith("REKINDLED_EMBER_")),
                new LivingDead(),
                new OneWithTheSpider(),
                // Auras.
                new BirdDiscourse(),
                new GeniusAmniote());
    }

    /** "Grants +#☯ Enchanting Wisdom.": the stat, flat. */
    static PetAbility stat(String pet, String name, String text, Stat stat) {
        return new Ability(pet, name, text) {
            @Override
            public void stats(Player player, Active a, Stats stats) {
                stats.add(stat, a.n(0));
            }
        };
    }

    /** "Buffs the Pigman Sword by # ❁ Damage and # ❁ Strength.": while they hold it. */
    static PetAbility held(String pet, String name, String text, Predicate<String> ids) {
        return new Ability(pet, name, text) {
            @Override
            public void stats(Player player, Active a, Stats stats) {
                String held = held(player);
                if (held != null && ids.test(held)) stats.add(Stat.DAMAGE, a.n(0)).add(Stat.STRENGTH, a.n(1));
            }
        };
    }

    /** "Buffs the stats of Rekindled Ember Armor by #%.": a share more of what the worn pieces give. */
    static PetAbility armor(String pet, String name, String text, Predicate<String> ids) {
        return new Ability(pet, name, text) {
            @Override
            public void stats(Player player, Active a, Stats stats) {
                addShare(stats, wornStats(player, ids), a.n(0));
            }
        };
    }

    /** Whether there's a block of these within {@code radius} of their feet (their own too). */
    static boolean nearBlock(Player player, int radius, Set<Material> materials) {
        Location at = player.getLocation();
        World world = at.getWorld();
        int x = at.getBlockX();
        int y = at.getBlockY();
        int z = at.getBlockZ();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (materials.contains(world.getBlockAt(x + dx, y + dy, z + dz).getType())) return true;
                }
            }
        }
        return false;
    }

    /**
     * The Turtle's Turtle Tactics: "Gain +30% Defense and an additional +10% Defense when standing still": still is
     * where they were a second before (UNKNOWN how still Hypixel's is).
     */
    static final class TurtleTactics extends Ability {
        private final Map<UUID, Location> lastSecond = new HashMap<>();
        private final Map<UUID, Boolean> still = new HashMap<>();

        TurtleTactics() {
            super("TURTLE", "Turtle Tactics", "Gain +#% ❈ Defense and an additional +#% ❈ Defense when standing still.");
        }

        @Override
        public void derivedStats(Player player, Active a, Stats stats) {
            Ability.scale(stats, Stat.DEFENSE, a.n(0) + (still.getOrDefault(player.getUniqueId(), false) ? a.n(1) : 0));
        }

        @Override
        public void second(Player player, Active a) {
            Location now = player.getLocation();
            Location before = lastSecond.put(player.getUniqueId(), now);
            still.put(player.getUniqueId(), before != null && before.getWorld() == now.getWorld() && before.distanceSquared(now) < 0.01);
        }

        @Override
        public void ended(Player player) {
            forget(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            lastSecond.remove(player);
            still.remove(player);
        }
    }

    /**
     * The Golden Dragon's Shining Scales: "Grants +11.1❁ Strength and +2.2✯ Magic Find to your pet for each digit in your Gold
     * Collection. (Max 100M collection)": the pet's own stats (Chimera copies them).
     */
    static final class ShiningScales extends Ability {
        ShiningScales() {
            super("GOLDEN_DRAGON", "Shining Scales",
                    "Grants +#❁ Strength and +#✯ Magic Find to your pet for each digit in your Gold Collection. (Max #M collection)");
        }

        @Override
        public void petStats(Player player, Active a, Stats pet) {
            User user = User.ifLoaded(player.getUniqueId());
            if (user == null || user.isReleased()) return;
            int digits = digits(Collections.count(user.profile(), "GOLD_INGOT"), (long) (a.n(2) * 1_000_000));
            pet.add(Stat.STRENGTH, a.n(0) * digits).add(Stat.MAGIC_FIND, a.n(1) * digits);
        }

        /** How many digits a collection of {@code count} has, counting no more than {@code most}; none for none. */
        static int digits(long count, long most) {
            long counted = Math.min(Math.max(0, count), most);
            return counted <= 0 ? 0 : Long.toString(counted).length();
        }
    }

    /** The T-Rex's Tyrant: "Combat stats granted by pet items on this pet are increased by 100%": more of the pet's own. */
    static final class Tyrant extends Ability {
        Tyrant() {
            super("TYRANNOSAURUS", "Tyrant", "Combat stats granted by pet items on this pet are increased by #%.");
        }

        @Override
        public void petStats(Player player, Active a, Stats pet) {
            Stats held = HeldItems.granted(a.pet().heldItem(), Pets.stats(a.pet(), Pets.data()), a.level());
            for (Stat stat : COMBAT) if (held.has(stat)) pet.add(stat, held.get(stat) * a.n(0) / 100);
        }
    }

    /**
     * The Bat's Nightmare: "During night, gain 30.0 ✎ Intelligence, 50.0 ✦ Speed, and Night Vision": the night vision
     * given a second at a time while it's night, and taken back once it isn't.
     */
    static final class Nightmare extends Ability {
        /** Long enough that the screen doesn't flicker as it's given again each second. */
        static final int NIGHT_VISION_TICKS = 15 * 20;

        Nightmare() {
            super("BAT", "Nightmare", "During night, gain # ✎ Intelligence, # ✦ Speed, and Night Vision.");
        }

        @Override
        public void stats(Player player, Active a, Stats stats) {
            if (night()) stats.add(Stat.INTELLIGENCE, a.n(0)).add(Stat.SPEED, a.n(1));
        }

        @Override
        public void second(Player player, Active a) {
            if (night()) player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, NIGHT_VISION_TICKS, 0, false, false, false));
            else ended(player);
        }

        @Override
        public void ended(Player player) {
            PotionEffect effect = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
            if (effect != null && effect.getDuration() <= NIGHT_VISION_TICKS && !effect.hasParticles()) player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    /** The Flying Fish's Water and Lava Bender: "Gives 100 ❁ Strength and ❈ Defense when near water (or lava)", looked for once a second. */
    static final class Bender extends Ability {
        private static final Set<Material> WATER = Set.of(Material.WATER, Material.BUBBLE_COLUMN);
        private static final Set<Material> WATER_OR_LAVA = Set.of(Material.WATER, Material.BUBBLE_COLUMN, Material.LAVA);
        private final Set<Material> liquids;
        private final Map<UUID, Boolean> near = new HashMap<>();

        Bender(String name, String text, boolean lava) {
            super("FLYING_FISH", name, text);
            this.liquids = lava ? WATER_OR_LAVA : WATER;
        }

        @Override
        public void stats(Player player, Active a, Stats stats) {
            if (near.getOrDefault(player.getUniqueId(), false)) stats.add(Stat.STRENGTH, a.n(0)).add(Stat.DEFENSE, a.n(0));
        }

        @Override
        public void second(Player player, Active a) {
            near.put(player.getUniqueId(), nearBlock(player, NEAR_LIQUID, liquids));
        }

        @Override
        public void ended(Player player) {
            near.remove(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            near.remove(player);
        }
    }

    /** The Wolf's Pack Leader: "Gain 15.0 ☠ Crit Damage for every nearby wolf monster Max 10 wolves", counted once a second. */
    static final class PackLeader extends Ability {
        private final Map<UUID, Integer> wolves = new HashMap<>();

        PackLeader() {
            super("WOLF", "Pack Leader", "Gain # ☠ Crit Damage for every nearby wolf monster Max # wolves");
        }

        @Override
        public void stats(Player player, Active a, Stats stats) {
            stats.add(Stat.CRIT_DAMAGE, a.n(0) * Math.min(wolves.getOrDefault(player.getUniqueId(), 0), (int) a.n(1)));
        }

        @Override
        public void second(Player player, Active a) {
            int count = 0;
            for (Entity entity : player.getNearbyEntities(NEARBY_WOLVES, NEARBY_WOLVES, NEARBY_WOLVES)) {
                if (entity.getType() == EntityType.WOLF && entity instanceof LivingEntity living && mob(living)) count++;
            }
            wolves.put(player.getUniqueId(), count);
        }

        @Override
        public void ended(Player player) {
            wolves.remove(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            wolves.remove(player);
        }
    }

    /**
     * The Blaze's Fusion-Style Potato: "Double effects of hot potato books": what the potato books on the weapon they hold
     * (when its stats count in the hand) and the armor they wear give, again.
     */
    static final class FusionStylePotato extends Ability {
        FusionStylePotato() {
            super("BLAZE", "Fusion-Style Potato", "Double effects of hot potato books");
        }

        @Override
        public void stats(Player player, Active a, Stats stats) {
            SkyBlockItem held = heldItem(player);
            if (held != null && held.statsWhenHeld()) {
                NBTTagCompound tag = ItemNBT.read(player.getInventory().getItemInMainHand());
                if (tag != null) stats.add(Book.HOT_POTATO.stats(held, tag));
            }
            for (Worn.Piece piece : worn(player)) {
                if (piece.item().genericItemType() == GenericItemType.ARMOR) stats.add(Book.HOT_POTATO.stats(piece.item(), piece.tag()));
            }
        }
    }

    /** The Zombie's Living Dead: "Increases all stats on Undead ༕ armor by 25%": armor whose text says "This item is Undead ༕!". */
    static final class LivingDead extends Ability {
        LivingDead() {
            super("ZOMBIE", "Living Dead", "Increases all stats on Undead ༕ armor by #%.");
        }

        @Override
        public void stats(Player player, Active a, Stats stats) {
            for (Worn.Piece piece : worn(player)) {
                if (piece.item().genericItemType() == GenericItemType.ARMOR && marked(piece.item(), "Undead")) {
                    addShare(stats, net.icxd.dungeons.stats.ItemStats.of(piece.stack(), player), a.n(0));
                }
            }
        }
    }

    /**
     * The Spider's One With The Spider: "Applies +15.0❁ Strength to all Arachnal Ж weapons, armor, and equipment you have
     * equipped": so much for each, what they hold (a weapon) and wear, whose text says "This item is Arachnal Ж!".
     */
    static final class OneWithTheSpider extends Ability {
        OneWithTheSpider() {
            super("SPIDER", "One With The Spider", "Applies +#❁ Strength to all Arachnal Ж weapons, armor, and equipment you have equipped.");
        }

        @Override
        public void stats(Player player, Active a, Stats stats) {
            int arachnal = 0;
            SkyBlockItem held = heldItem(player);
            if (held != null && held.genericItemType() == GenericItemType.WEAPON && marked(held, "Arachnal")) arachnal++;
            for (Worn.Piece piece : worn(player)) if (marked(piece.item(), "Arachnal")) arachnal++;
            stats.add(Stat.STRENGTH, a.n(0) * arachnal);
        }
    }

    /**
     * The Parrot's Bird Discourse: "Gives +30.00❁ Strength to players within 20 blocks Doesn't stack": its owner too
     * (UNKNOWN), once whoever's Parrots are near (the highest level's).
     */
    static final class BirdDiscourse extends Ability {
        BirdDiscourse() {
            super("PARROT", "Bird Discourse", "Gives +#❁ Strength to players within # blocks Doesn't stack");
        }

        @Override
        public double auraRange(Active a) {
            return a.n(1);
        }

        @Override
        public void aura(Player owner, Active a, Player near, Stats stats) {
            stats.add(Stat.STRENGTH, a.n(0));
        }
    }

    /**
     * The Turtle's Genius Amniote: "Grants +2.50% ❈ Defense to 4 players within 50 blocks of you": the four nearest
     * others (UNKNOWN whether its owner counts; "to players ... of you" reads as others).
     */
    static final class GeniusAmniote extends Ability {
        GeniusAmniote() {
            super("TURTLE", "Genius Amniote", "Grants +#% ❈ Defense to # players within # blocks of you.");
        }

        @Override
        public double auraRange(Active a) {
            return a.n(2);
        }

        @Override
        public void aura(Player owner, Active a, Player near, Stats stats) {
            if (owner == near) return;
            List<Player> nearest = Ability.near(owner, a.n(2));
            int i = nearest.indexOf(near);
            if (i >= 0 && i < a.n(1)) Ability.scale(stats, Stat.DEFENSE, a.n(0));
        }
    }
}
