package net.icxd.dungeons.item.ability.utility;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.instance.RunItems;
import net.icxd.dungeons.item.ability.weapons.Explosions;
import net.icxd.dungeons.item.ability.weapons.Hits;
import net.icxd.dungeons.mob.DataMob;
import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.goals.AvatarControl;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * The dungeon secret items that do something when used (right click), "Dungeons only!", as their text and the wiki
 * say: the Decoy, the Inflatable Jerry, the Architect's First Draft and the Dungeon Trap. None has an ability block,
 * so they're found by id. Each is used up. Only alive in a running run (a ghost can't use them). Their vanilla use
 * (a spawn egg, a pressure plate) never happens. Main thread.
 */
final class SecretItems {
    static final String DECOY = "DUNGEON_DECOY";
    static final String JERRY = "INFLATABLE_JERRY";
    static final String DRAFT = "ARCHITECT_FIRST_DRAFT";
    static final String TRAP = "DUNGEON_TRAP";
    /** On the Decoy's body: nobody hurts it. */
    static final String DECOY_TAG = "skyblock_decoy";

    /** The Decoy: how long it stays, how far it draws mobs from, and its chance to be a dud (all UNKNOWN). */
    static final long DECOY_MILLIS = 20_000;
    static final double DECOY_RANGE = 10;
    static final double DUD_CHANCE = 0.1;
    /** How far it wanders from where it was put, and how fast (a walk). */
    private static final double WANDER = 4;
    private static final double WALK = 0.15;
    /** The Inflatable Jerry: "There is a 20% chance that it will pop when used and not activate" (the wiki). */
    static final double POP_CHANCE = 0.2;
    /** How far it scares mobs from, and how long they run (UNKNOWN). */
    static final double JERRY_RANGE = 10;
    static final long JERRY_MILLIS = 5_000;
    private static final double FLEE_SPEED = 1.2;
    /** The Dungeon Trap: "dealing 20,000-250,000 damage based on the dungeon floor you are on", and how far its blast reaches (UNKNOWN). */
    static final double TRAP_LEAST = 20_000;
    static final double TRAP_MOST = 250_000;
    static final double TRAP_RADIUS = 3;

    private static final GoalKey<Mob> DISTRACTED = GoalKey.of(Mob.class, new NamespacedKey("dungeons", "decoy"));
    private static final GoalKey<Mob> SCARED = GoalKey.of(Mob.class, new NamespacedKey("dungeons", "inflatable_jerry"));

    /** A Decoy out: its body, where it wanders about, until when, the mobs it has drawn, and where it's going. */
    private static final class Decoy {
        final Mannequin body;
        final Location home;
        final long until;
        final Set<Mob> drawn = new HashSet<>();
        Location heading;
        int ticks;

        Decoy(Mannequin body, Location home, long until) {
            this.body = body;
            this.home = home;
            this.until = until;
        }
    }

    /** A scared mob: until when, and the words over its head. */
    private record Scared(Mob mob, long until, TextDisplay words) {
    }

    /** A Dungeon Trap put down: its plate, whose it is, and what it deals. */
    private record Trap(Block plate, UUID by, double damage) {
    }

    private static final List<Decoy> DECOYS = new ArrayList<>();
    private static final List<Scared> SCARED_MOBS = new ArrayList<>();
    private static final List<Trap> TRAPS = new ArrayList<>();

    private SecretItems() {
    }

    /** Whether it's one of them (its vanilla use is called off whether it's used or not). */
    static boolean is(String id) {
        return DECOY.equals(id) || JERRY.equals(id) || DRAFT.equals(id) || TRAP.equals(id);
    }

    /** A right click with one of them, on {@code clicked}'s {@code face} (null for the air). */
    static void used(Player player, String id, Block clicked, BlockFace face) {
        if (DRAFT.equals(id)) {
            draft(player);
            return;
        }
        if (!RunItems.alive(player)) return;
        switch (id) {
            case DECOY -> {
                if (!takeOne(player, id)) return;
                if (ThreadLocalRandom.current().nextDouble() < DUD_CHANCE) {
                    // The wiki's words ("It was a dud."); their colour is UNKNOWN.
                    player.sendMessage(Utils.color("&cIt was a dud."));
                    return;
                }
                decoy(player, clicked != null && face == BlockFace.UP ? clicked.getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5) : player.getLocation());
            }
            case JERRY -> {
                if (takeOne(player, id)) jerry(player);
            }
            case TRAP -> {
                if (clicked == null || face != BlockFace.UP) return;
                Block plate = clicked.getRelative(BlockFace.UP);
                if (!plate.getType().isAir() || !clicked.getType().isSolid() || !takeOne(player, id)) return;
                plate.setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
                TRAPS.add(new Trap(plate, player.getUniqueId(), trapDamage(RunItems.floor(player))));
                player.playSound(plate.getLocation(), Sound.BLOCK_STONE_PLACE, 1, 1);
            }
            default -> {
            }
        }
    }

    /** Uses up one of the stack they hold. */
    private static boolean takeOne(Player player, String id) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.isEmpty()) return false;
        held.setAmount(held.getAmount() - 1);
        player.getInventory().setItemInMainHand(held.getAmount() <= 0 ? null : held);
        return true;
    }

    // ---------- the Decoy ----------

    /**
     * The Decoy: "Runs around and draws attention from nearby mobs, very handy for creating a distraction!" (its
     * text), "a spawn egg that when used, spawns a human entity that aggros entities that would normally attack the
     * player towards it", "immune to player damage" (the Fandom wiki's Decoy). A player-shaped body wanders about
     * where it's put, and SkyBlock's mobs near it go for it instead of any player while it lasts; nothing hurts it.
     * Its look, how long it lasts and how far it draws from are UNKNOWN.
     */
    private static void decoy(Player player, Location at) {
        at.setYaw(player.getLocation().getYaw() + 180);
        at.setPitch(0);
        Mannequin body = at.getWorld().spawn(at, Mannequin.class, m -> {
            m.setDescription(null);
            m.setPersistent(false);
            m.setInvulnerable(true);
            m.setRemoveWhenFarAway(false);
            m.customName(Text.line("&aDecoy"));
            m.setCustomNameVisible(true);
            m.addScoreboardTag(DECOY_TAG);
        });
        DECOYS.add(new Decoy(body, at.clone(), System.currentTimeMillis() + DECOY_MILLIS));
        at.getWorld().playSound(at, Sound.ENTITY_CHICKEN_EGG, 1, 1);
    }

    /** It goes for its decoy while that's there, instead of anyone else. */
    private static final class Distracted implements Goal<Mob> {
        private final Mob mob;
        private final Decoy decoy;

        Distracted(Mob mob, Decoy decoy) {
            this.mob = mob;
            this.decoy = decoy;
        }

        @Override
        public boolean shouldActivate() {
            return decoy.body.isValid() && decoy.body.getWorld().equals(mob.getWorld());
        }

        @Override
        public void tick() {
            if (mob.getTarget() != decoy.body) mob.setTarget(decoy.body);
        }

        @Override
        public GoalKey<Mob> getKey() {
            return DISTRACTED;
        }

        @Override
        public EnumSet<GoalType> getTypes() {
            return EnumSet.of(GoalType.TARGET);
        }
    }

    /** Each tick: decoys wander, draw the mobs near them once a second, and go when their time is up. */
    private static void tickDecoys(long now) {
        for (Iterator<Decoy> it = DECOYS.iterator(); it.hasNext(); ) {
            Decoy decoy = it.next();
            if (!decoy.body.isValid() || now >= decoy.until) {
                end(decoy);
                it.remove();
                continue;
            }
            if (decoy.ticks++ % 40 == 0 || decoy.heading == null) decoy.heading = wanderTo(decoy.home);
            if (decoy.body.getLocation().distanceSquared(decoy.heading) > 1) {
                AvatarControl.face(decoy.body, decoy.heading);
                AvatarControl.steer(decoy.body, decoy.heading, WALK);
            }
            if (decoy.ticks % 20 != 1) continue;
            for (LivingEntity near : Hits.near(decoy.body.getLocation(), DECOY_RANGE)) {
                if (!(near instanceof Mob mob) || boss(mob) || !decoy.drawn.add(mob)) continue;
                Bukkit.getMobGoals().addGoal(mob, -1, new Distracted(mob, decoy));
            }
        }
    }

    private static Location wanderTo(Location home) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return home.clone().add(random.nextDouble(-WANDER, WANDER), 0, random.nextDouble(-WANDER, WANDER));
    }

    private static void end(Decoy decoy) {
        for (Mob mob : decoy.drawn) {
            if (!mob.isValid()) continue;
            Bukkit.getMobGoals().removeGoal(mob, DISTRACTED);
            if (mob.getTarget() == decoy.body) mob.setTarget(null);
        }
        decoy.body.getWorld().spawnParticle(Particle.POOF, decoy.body.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
        decoy.body.remove();
    }

    /** Whether it's a boss's or a miniboss's (neither is fooled: the wiki's Inflatable Jerry; UNKNOWN for the Decoy). */
    private static boolean boss(LivingEntity entity) {
        Mobs.Live live = Mobs.of(entity);
        if (live == null) return false;
        if (live.type().isBoss()) return true;
        return live.type() instanceof DataMob data && data.kind().style() == MobKind.NameStyle.MINIBOSS;
    }

    // ---------- the Inflatable Jerry ----------

    /**
     * The Inflatable Jerry "can be used to scare away Non-Boss mobs in Dungeons. The mobs will run in the opposite
     * direction, while text displays above their head that reads: 'JERRY, RUN'. There is a 20% chance that it will
     * pop when used and not activate. ... Bosses and Mini-Bosses are not affected by Jerry. Furthermore, it will
     * always fail to activate around Bosses" (the wiki). SkyBlock's mobs near them run away from where it was used;
     * how near, for how long and what's said when it pops or fails are UNKNOWN.
     */
    private static void jerry(Player player) {
        Location at = player.getLocation();
        List<LivingEntity> near = Hits.near(at, JERRY_RANGE);
        boolean aroundBoss = false;
        for (LivingEntity mob : near) {
            Mobs.Live live = Mobs.of(mob);
            if (live != null && live.type().isBoss()) aroundBoss = true;
        }
        if (aroundBoss || ThreadLocalRandom.current().nextDouble() < POP_CHANCE) {
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 2);
            player.sendMessage(Utils.color("&cYour Inflatable Jerry popped!"));
            return;
        }
        at.getWorld().playSound(at, Sound.ENTITY_VILLAGER_AMBIENT, 1, 1.4f);
        long until = System.currentTimeMillis() + JERRY_MILLIS;
        for (LivingEntity near1 : near) {
            if (!(near1 instanceof Mob mob) || boss(mob)) continue;
            Bukkit.getMobGoals().removeGoal(mob, SCARED);
            Bukkit.getMobGoals().addGoal(mob, -1, new Fleeing(mob, at.clone(), until));
            mob.setTarget(null);
            SCARED_MOBS.add(new Scared(mob, until, words(mob)));
        }
    }

    private static TextDisplay words(Mob mob) {
        return mob.getWorld().spawn(over(mob), TextDisplay.class, d -> {
            d.text(Text.line("&c&lJERRY, RUN"));
            d.setBillboard(Display.Billboard.CENTER);
            d.setPersistent(false);
            d.setTeleportDuration(1);
        });
    }

    private static Location over(Entity mob) {
        return mob.getLocation().add(0, mob.getHeight() + 0.8, 0);
    }

    /** It runs from where the Jerry was used, until its time is up (the plugin's FleeGoal's way of running). */
    private static final class Fleeing implements Goal<Mob> {
        private final Mob mob;
        private final Location from;
        private final long until;
        private int replan;

        Fleeing(Mob mob, Location from, long until) {
            this.mob = mob;
            this.from = from;
            this.until = until;
        }

        @Override
        public boolean shouldActivate() {
            return System.currentTimeMillis() < until;
        }

        @Override
        public void tick() {
            if (mob.getTarget() != null) mob.setTarget(null);
            if (--replan > 0) return;
            replan = 10;
            Location me = mob.getLocation();
            Vector away = me.toVector().subtract(from.toVector()).setY(0);
            if (away.lengthSquared() < 1e-6) away = new Vector(1, 0, 0);
            mob.getPathfinder().moveTo(me.clone().add(away.normalize().multiply(JERRY_RANGE)), FLEE_SPEED);
        }

        @Override
        public void stop() {
            mob.getPathfinder().stopPathfinding();
        }

        @Override
        public GoalKey<Mob> getKey() {
            return SCARED;
        }

        @Override
        public EnumSet<GoalType> getTypes() {
            return EnumSet.of(GoalType.MOVE, GoalType.LOOK, GoalType.TARGET);
        }
    }

    private static void tickScared(long now) {
        for (Iterator<Scared> it = SCARED_MOBS.iterator(); it.hasNext(); ) {
            Scared scared = it.next();
            if (now < scared.until() && scared.mob().isValid()) {
                scared.words().teleport(over(scared.mob()));
                continue;
            }
            scared.words().remove();
            if (scared.mob().isValid()) Bukkit.getMobGoals().removeGoal(scared.mob(), SCARED);
            it.remove();
        }
    }

    // ---------- the Architect's First Draft ----------

    /**
     * The Architect's First Draft: "Can be used to reset a puzzle room when in dungeons.", a failed one in the room
     * they're in ("reset a failed Dungeon Puzzle in the room the player is in. It is consumed on use."), with the
     * wiki's lines.
     */
    private static void draft(Player player) {
        if (RunItems.floor(player) == null) {
            player.sendMessage(Utils.color("&cYou can only use this item in dungeons!"));
            return;
        }
        String puzzle = RunItems.alive(player) ? RunItems.resetFailedPuzzle(player) : null;
        if (puzzle == null) {
            player.sendMessage(Utils.color("&cThere are no failed puzzles to reset... for now ;)"));
            return;
        }
        takeOne(player, DRAFT);
        player.sendMessage(Utils.color("&aYou used the &5Architect's First Draft &ato reset &6" + puzzle + "&a!"));
    }

    // ---------- the Dungeon Trap ----------

    /**
     * "Place an explosive trap that triggers when mobs walk over it dealing 20,000-250,000 damage based on the dungeon
     * floor you are on.": how it goes from one to the other is UNKNOWN, evenly by floor (the Entrance's 20,000 to Floor
     * VII's 250,000, the same in Master Mode).
     */
    static double trapDamage(DungeonFloor floor) {
        int number = floor == null ? 0 : floor.getNumber();
        return TRAP_LEAST + (TRAP_MOST - TRAP_LEAST) * Math.max(0, Math.min(7, number)) / 7.0;
    }

    /**
     * Each tick: a trap one of SkyBlock's mobs stands on goes off, every mob within 3 blocks of it taking its damage
     * (the placer's, as an effect's; times Consolidated: it's an explosion), and its plate goes.
     */
    private static void tickTraps() {
        for (Iterator<Trap> it = TRAPS.iterator(); it.hasNext(); ) {
            Trap trap = it.next();
            if (trap.plate().getType() != Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
                it.remove();
                continue;
            }
            BoundingBox on = BoundingBox.of(trap.plate()).expand(0, 0.5, 0);
            if (trap.plate().getWorld().getNearbyEntities(on, MobHits::hittable).isEmpty()) continue;
            it.remove();
            trap.plate().setType(Material.AIR, false);
            Location at = trap.plate().getLocation().add(0.5, 0.2, 0.5);
            at.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, at, 1);
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
            Player by = Bukkit.getPlayer(trap.by());
            if (by == null) continue;
            double damage = trap.damage() * Explosions.factor(by);
            for (LivingEntity mob : Hits.near(at, TRAP_RADIUS)) MobHits.deal(by, mob, damage, DamageIndicators.Look.NORMAL, HitKind.OTHER, null);
        }
    }

    /** Every tick. */
    static void tick() {
        if (DECOYS.isEmpty() && SCARED_MOBS.isEmpty() && TRAPS.isEmpty()) return;
        long now = System.currentTimeMillis();
        tickDecoys(now);
        tickScared(now);
        tickTraps();
    }

    /** The plugin is going: what they put in the world goes. */
    static void removeAll() {
        for (Decoy decoy : DECOYS) end(decoy);
        DECOYS.clear();
        for (Scared scared : SCARED_MOBS) scared.words().remove();
        SCARED_MOBS.clear();
        for (Trap trap : TRAPS) if (trap.plate().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) trap.plate().setType(Material.AIR, false);
        TRAPS.clear();
    }
}
