package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The Catacombs' armor: Shadow Assassin's set and its pieces' bonuses, Dungeon Lord, Zombie Knight,
 * Adaptive's, the skeleton sets' arrow damage, Heavy's Vindicate, Rotten's Sieve Body, Zombie
 * Soldier's Shoal and the Wither sets' pieces. Their numbers are the items' text's. Not here: the
 * Witherborn wither (how much its explosion deals isn't on the wiki), Skeletor's and Zombie Commander's
 * kill counts (kills of mobs this plugin doesn't have, kept on the item), Super Heavy's Seismic Wave
 * cooldown (the Tank's ability would need to ask).
 */
final class DungeonSets {
    private static final String PIECE = "PIECE";
    private static final Pattern ARMOR = Pattern.compile("_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)$");

    private DungeonSets() {
    }

    static List<Bonus> all() {
        return List.of(new ShadowAssassin(), new Pursuit(), new Sinew(), new Fluxation(), new Salubrious(), new Bloodrush(),
                new DungeonLord(), new ZombieKnight(), new EfficientTraining(), new AdaptiveClasses(), new ArrowPieces(),
                new ArrowSet("Skeleton Master"), new ArrowSet("Skeleton Soldier"), new KeepsArrows(), new Vindicate(), new SieveBody(),
                new RottenPieces(), new WitherPieces(), new Shoal());
    }

    /** Whether the id is one of these sets' armor pieces ("ROTTEN" for ROTTEN_HELMET to ROTTEN_BOOTS). */
    static boolean armorOf(String id, String... sets) {
        if (!ARMOR.matcher(id).find()) return false;
        for (String set : sets) if (id.startsWith(set + "_") && id.indexOf('_', set.length() + 1) < 0) return true;
        return false;
    }

    // ---------- Shadow Assassin ----------

    /**
     * "Grants +1 Strength for every mob you kill in The Catacombs. (Strength resets after each run)":
     * kills while the set is worn (its text before 0.26.1: "Collect the shadows of the enemies you kill
     * increasing your damage for the rest of the dungeon while wearing this set"), counted for the run.
     * No cap is given (UNKNOWN).
     */
    static final class ShadowAssassin implements Bonus {
        private final Map<UUID, RunCounter> kills = new HashMap<>();

        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Shadow Assassin";
        }

        @Override
        public void killed(Player player, Active active, SkyBlockMobDeathEvent event) {
            DungeonRun run = RunManager.of(player);
            if (run != null) kills.computeIfAbsent(player.getUniqueId(), id -> new RunCounter()).add(run);
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            RunCounter counter = kills.get(player.getUniqueId());
            if (counter != null) stats.add(Stat.STRENGTH, counter.get(RunManager.of(player)));
        }

        @Override
        public void forget(UUID player) {
            kills.remove(player);
        }
    }

    /** A Shadow Assassin piece's "On teleport" bonus: again only once its cooldown is over (its block's, 3s). */
    abstract static class OnTeleport implements Bonus {
        @Override
        public String kind() {
            return PIECE;
        }

        @Override
        public void teleported(Player player, Active active) {
            if (Bonuses.ready(player, name(), Bonuses.cooldown(active))) apply(player);
        }

        abstract void apply(Player player);
    }

    /**
     * Pursuit: "On teleport, gain invisibility and +20 Speed for 10 seconds." The recorded runs show the
     * invisibility: 200 ticks at level I, ambient and with particles but no icon, and none for a
     * teleport 0.1s after one that had it (the cooldown).
     */
    static final class Pursuit extends OnTeleport {
        @Override
        public String name() {
            return "Pursuit";
        }

        @Override
        void apply(Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0, true, true, false));
            Bonuses.buff(player, name(), new Stats().set(Stat.SPEED, 20), 10_000);
        }
    }

    /** Sinew: "On teleport, gain +10 Strength for 10 seconds." */
    static final class Sinew extends OnTeleport {
        @Override
        public String name() {
            return "Sinew";
        }

        @Override
        void apply(Player player) {
            Bonuses.buff(player, name(), new Stats().set(Stat.STRENGTH, 10), 10_000);
        }
    }

    /** Fluxation: "On teleport, regen 10 Mana", never past their pool. */
    static final class Fluxation extends OnTeleport {
        @Override
        public String name() {
            return "Fluxation";
        }

        @Override
        void apply(Player player) {
            PlayerSession session = PlayerSession.of(player);
            int pool = session.maxMana();
            int mana = session.getMana() < 0 ? pool : session.getMana();
            session.setMana(Math.min(pool, mana + 10));
        }
    }

    /**
     * Salubrious: "On kill, heal +15", 3s apart, as the leggings say in game (the SkyBlock Menu tour, and
     * the item data; the wiki has it on teleport).
     */
    static final class Salubrious implements Bonus {
        @Override
        public String kind() {
            return PIECE;
        }

        @Override
        public String name() {
            return "Salubrious";
        }

        @Override
        public void killed(Player player, Active active, SkyBlockMobDeathEvent event) {
            if (Bonuses.ready(player, name(), Bonuses.cooldown(active))) PlayerHealth.heal(player, 15);
        }
    }

    /**
     * Bloodrush, the Shadow Assassin Cloak's (equipment): "On teleport: Your next melee hit within 5s deals
     * 10% more damage." Taken as +10 in the additive buffs, as the armor's "+k% damage" are (UNKNOWN).
     */
    static final class Bloodrush extends OnTeleport {
        private final Map<UUID, Long> until = new HashMap<>();

        @Override
        public String name() {
            return "Bloodrush";
        }

        @Override
        void apply(Player player) {
            until.put(player.getUniqueId(), System.currentTimeMillis() + 5_000);
        }

        @Override
        public Combat.HitBuff hit(Player player, Active active, boolean ranged, Damage.Target target) {
            if (ranged) return null;
            Long end = until.remove(player.getUniqueId());
            return end != null && end >= System.currentTimeMillis() ? new Combat.HitBuff(10, 1) : null;
        }

        @Override
        public void forget(UUID player) {
            until.remove(player);
        }
    }

    // ---------- Dungeon Lord, Zombie Knight ----------

    /**
     * "Gains +5 Strength and +10 Crit Damage every minute spent in the Dungeon" (Skeleton Lord Armor), or
     * "+10 Strength and +10 Defense" (Zombie Lord Armor): for each whole minute the run has been going. No
     * cap is given (UNKNOWN). Worn mixed, each piece gives a quarter of its own set's (UNKNOWN).
     */
    static final class DungeonLord implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Dungeon Lord";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            DungeonRun run = RunManager.of(player);
            if (run != null) stats.add(perMinute(active.pieces(), active.set().pieces(), minutes(run.elapsedMillis())));
        }

        static int minutes(long millis) {
            return (int) Math.max(0, millis / 60_000);
        }

        /** What these pieces (of a set of {@code of}) give after this many minutes. */
        static Stats perMinute(List<Worn.Piece> pieces, int of, int minutes) {
            Stats stats = new Stats();
            double share = (double) minutes / Math.max(1, of);
            for (Worn.Piece piece : pieces) {
                if (piece.id().startsWith("SKELETON_LORD_")) stats.add(Stat.STRENGTH, 5 * share).add(Stat.CRIT_DAMAGE, 10 * share);
                else stats.add(Stat.STRENGTH, 10 * share).add(Stat.DEFENSE, 10 * share);
            }
            return stats;
        }
    }

    /**
     * The armor's "Gains +50 Defense when used with the Zombie Knight Sword", and the sword's own "Gains
     * +30 Strength when used with the Zombie Knight Armor": both while the whole set is worn and the sword
     * held.
     */
    static final class ZombieKnight implements Bonus {
        static final String SWORD = "ZOMBIE_KNIGHT_SWORD";

        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Zombie Knight";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            if (SWORD.equals(SetBonuses.held(player))) stats.add(Stat.DEFENSE, 50).add(Stat.STRENGTH, 30);
        }
    }

    // ---------- Adaptive ----------

    /**
     * Efficient training: "Every 5 Catacombs levels, this armor piece gains +2% stats": each worn piece's
     * stats, by their Catacombs level for stats (at most 50: UNKNOWN whether levels past it count).
     */
    static final class EfficientTraining implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Efficient training";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            double share = share(Bonuses.catacombsLevel(player));
            if (share <= 0) return;
            for (Worn.Piece piece : active.pieces()) {
                Stats own = ItemStats.of(piece.stack(), player);
                for (Stat stat : Stat.values()) if (own.has(stat)) stats.add(stat, own.get(stat) * share);
            }
        }

        /** +2% for every 5 levels. */
        static double share(int catacombsLevel) {
            return 0.02 * Math.max(0, catacombsLevel / 5);
        }
    }

    /**
     * "Grants additional bonuses based on your selected dungeon class!": each Adaptive piece in the
     * Catacombs ("While inside the Catacombs, each piece of Adaptive Armor gains increased stats depending
     * on the user's selected Class", the wiki's Adaptive Armor): Berserk +20 Strength, Healer +5 Mending
     * and +40 Health, Mage +50 Intelligence, Tank +30 Defense, Archer +5 Crit Chance and +15 Crit Damage;
     * the Adaptive Belt's text has its own (+10 Strength; +10 Health and +5 Mending; +25 Intelligence; +5
     * Health and +10 Defense; +2 Crit Chance and +5 Crit Damage). A Tank also "reduces damage taken by 5%
     * for each piece you have equipped if you are hit by the same monster within 10 seconds". The class
     * is the one they've selected (the run's class is the same unless they changed it after the start).
     */
    static final class AdaptiveClasses implements Bonus {
        private static final Pattern IDS = Pattern.compile("^(STARRED_)?ADAPTIVE_(HELMET|CHESTPLATE|LEGGINGS|BOOTS|BELT)$");
        private record LastHit(UUID by, long at) {
        }

        private final Map<UUID, LastHit> lastHit = new HashMap<>();

        @Override
        public String kind() {
            return ITEM;
        }

        @Override
        public String name() {
            return "Adaptive";
        }

        @Override
        public boolean item(String id) {
            return IDS.matcher(id).matches();
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            DungeonClass dungeonClass = dungeonClass(player);
            if (dungeonClass == null) return;
            for (Worn.Piece piece : active.pieces()) stats.add(bonus(dungeonClass, piece.id().endsWith("_BELT")));
        }

        /** One piece's for this class ({@code belt}: the belt's). */
        static Stats bonus(DungeonClass dungeonClass, boolean belt) {
            Stats stats = new Stats();
            return switch (dungeonClass) {
                case BERSERK -> stats.set(Stat.STRENGTH, belt ? 10 : 20);
                case HEALER -> stats.set(Stat.MENDING, 5).set(Stat.HEALTH, belt ? 10 : 40);
                case MAGE -> stats.set(Stat.INTELLIGENCE, belt ? 25 : 50);
                case TANK -> belt ? stats.set(Stat.HEALTH, 5).set(Stat.DEFENSE, 10) : stats.set(Stat.DEFENSE, 30);
                case ARCHER -> stats.set(Stat.CRIT_CHANCE, belt ? 2 : 5).set(Stat.CRIT_DAMAGE, belt ? 5 : 15);
            };
        }

        @Override
        public double takenFrom(Player player, Active active, Entity by) {
            Entity mob = PlayerDamage.attacker(by);
            LastHit last = lastHit.get(player.getUniqueId());
            if (mob == null || last == null || !last.by().equals(mob.getUniqueId()) || dungeonClass(player) != DungeonClass.TANK) return 1;
            if (System.currentTimeMillis() - last.at() > 10_000) return 1;
            long armor = active.pieces().stream().filter(p -> !p.id().endsWith("_BELT")).count();
            return Math.max(0, 1 - 0.05 * armor);
        }

        @Override
        public void hurt(Player player, Active active, Entity by, double taken) {
            Entity mob = PlayerDamage.attacker(by);
            if (mob != null) lastHit.put(player.getUniqueId(), new LastHit(mob.getUniqueId(), System.currentTimeMillis()));
        }

        /** Their class, in a run; null out of one. */
        private static DungeonClass dungeonClass(Player player) {
            if (!RunManager.inRun(player)) return null;
            User user = User.ifLoaded(player.getUniqueId());
            return user == null ? null : DungeonProfile.selectedClass(user);
        }

        @Override
        public void forget(UUID player) {
            lastHit.remove(player);
        }
    }

    // ---------- the skeletons' arrows ----------

    /**
     * "Increase the damage you deal with arrows by 5%" on each Skeleton Grunt, Soldier and Master piece,
     * and Maxor's "increases your arrow damage by 5%": +5 in the additive buffs a piece, as the wiki's
     * Additive Sources has Maxor's ("5 × Pieces"; the skeletons' taken the same, UNKNOWN).
     */
    static final class ArrowPieces implements Bonus {
        @Override
        public String kind() {
            return ITEM;
        }

        @Override
        public String name() {
            return "Arrow damage";
        }

        @Override
        public boolean item(String id) {
            return armorOf(id, "SKELETON_GRUNT", "SKELETON_SOLDIER", "SKELETON_MASTER", "SPEED_WITHER");
        }

        @Override
        public Combat.HitBuff hit(Player player, Active active, boolean ranged, Damage.Target target) {
            return ranged ? new Combat.HitBuff(5 * active.count(), 1) : null;
        }
    }

    /** Skeleton Master's and Skeleton Soldier's "Increase the damage you deal with arrows by (an extra) 25%" (additive, UNKNOWN). */
    record ArrowSet(String name) implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public Combat.HitBuff hit(Player player, Active active, boolean ranged, Damage.Target target) {
            return ranged ? new Combat.HitBuff(25, 1) : null;
        }
    }

    /** The Skeleton Master Chestplate's "Your bows don't consume arrows." */
    static final class KeepsArrows implements Bonus {
        @Override
        public String kind() {
            return ITEM;
        }

        @Override
        public String name() {
            return "Keeps arrows";
        }

        @Override
        public boolean item(String id) {
            return id.equals("SKELETON_MASTER_CHESTPLATE");
        }

        @Override
        public void shot(Player player, Active active, EntityShootBowEvent event) {
            event.setConsumeArrow(false);
        }
    }

    // ---------- Heavy, Rotten, Wither, Zombie Soldier ----------

    /** Heavy and Super Heavy Armor's Vindicate: "Grants +1 walk speed for every 50 defense that you have" (whole fifties, UNKNOWN). */
    static final class Vindicate implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Vindicate";
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            stats.add(Stat.SPEED, speed(stats.get(Stat.DEFENSE)));
        }

        static double speed(double defense) {
            return Math.floor(Math.max(0, defense) / 50);
        }
    }

    /** Rotten Armor's Sieve Body: "Gain an additional 20% knockback resistance to arrows." */
    static final class SieveBody implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Sieve Body";
        }

        @Override
        public double knockbackResistance(Player player, Active active, Entity by) {
            return by instanceof AbstractArrow ? 0.2 : 0;
        }
    }

    /** Each Rotten piece: "This rotten piece of armor grants 15% knockback resistance to arrows per piece worn." */
    static final class RottenPieces implements Bonus {
        @Override
        public String kind() {
            return ITEM;
        }

        @Override
        public String name() {
            return "Rotten";
        }

        @Override
        public boolean item(String id) {
            return armorOf(id, "ROTTEN");
        }

        @Override
        public double knockbackResistance(Player player, Active active, Entity by) {
            return by instanceof AbstractArrow ? 0.15 * active.count() : 0;
        }
    }

    /**
     * Each Wither Armor piece (Maxor's, Storm's, Goldor's and Necron's too): "Reduces the damage you take
     * from withers by 10%", from mobs of the Wither type. Pieces add up, 40% for all four (UNKNOWN: they
     * might multiply).
     */
    static final class WitherPieces implements Bonus {
        @Override
        public String kind() {
            return ITEM;
        }

        @Override
        public String name() {
            return "Wither";
        }

        @Override
        public boolean item(String id) {
            return armorOf(id, "WITHER", "POWER_WITHER", "SPEED_WITHER", "TANK_WITHER", "WISE_WITHER");
        }

        @Override
        public double takenFrom(Player player, Active active, Entity by) {
            return SetBonuses.types(by).contains(MobType.WITHER) ? factor(active.count()) : 1;
        }

        static double factor(int pieces) {
            return Math.max(0, 1 - 0.1 * pieces);
        }
    }

    /**
     * Zombie Soldier Armor's Shoal: "Gain +30 Defense for each Zombie Soldier Set within 30 blocks", theirs
     * among them (UNKNOWN: it may only count others').
     */
    static final class Shoal implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Shoal";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            stats.add(Stat.DEFENSE, 30 * Bonuses.playersNear(player, 30, other -> SetBonuses.active(other, name())));
        }
    }
}
