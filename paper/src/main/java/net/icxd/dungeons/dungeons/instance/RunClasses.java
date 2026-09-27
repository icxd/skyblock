package net.icxd.dungeons.dungeons.instance;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.classes.ClassBonus;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Utils;

/**
 * The members' classes once the run has started (research critic.md 3.2): each one's class and level
 * as they started, their stats ({@link ClassBonus}, doubled for a class nobody else plays, with the
 * recorded chat), and the Berserk's passives and Dungeon Orb abilities: Throwing Axe (right click the
 * orb, or ctrl+drop) and Ragnarok (left click the orb, or drop), with the recorded messages and timings.
 * The other classes' abilities aren't built: only their menus and stats.
 *
 * <p>Stats go in through {@link PlayerStats#addModifier} and damage through {@link Combat#addMultiplier}
 * (see {@link #register}). Main thread.
 */
final class RunClasses {
    /** Ragnarok's first "ready to use" (RUN1 23.4 s, RUN2 23.2 s after the start), then every 30 s while unused. */
    private static final int FIRST_REMINDER = 466;
    private static final int REMINDER_EVERY = 600;
    private static final long AXE_COOLDOWN = 10_000;
    private static final long RAGNAROK_COOLDOWN = 60_000;
    private static final long RAGNAROK_MILLIS = 15_000;
    /** "your highest hit in the last minute". */
    private static final long HIGHEST_HIT_WINDOW = 60_000;
    /** Bloodlust takes this much off Throwing Axe's cooldown. */
    private static final long BLOODLUST_AXE = 1_000;
    /** A thrown axe: a block a tick (RUN1/RUN2's flew 7 to 13 blocks in 0.4 to 0.7 s) for at most a second (UNKNOWN). */
    private static final double AXE_SPEED = 1.0;
    private static final int AXE_TICKS = 20;
    private static final double AXE_REACH = 1.2;
    /** How far below an armor stand's feet its hand is. */
    private static final double HAND_HEIGHT = 1.4;
    private static final ThreadLocal<DecimalFormat> DAMAGE =
            ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.US)));

    /** A member's class as the run started. */
    static final class State {
        final DungeonClass dungeonClass;
        final int level;
        final boolean solo;
        long axeReadyAt;
        boolean axeAnnounced = true;
        long ultimateReadyAt;
        int nextReminder = FIRST_REMINDER;
        long ragnarokUntil;
        long bloodlustUntil;
        /** Their hits in the last minute: when, and how much. */
        final Deque<double[]> hits = new ArrayDeque<>();

        State(DungeonClass dungeonClass, int level, boolean solo) {
            this.dungeonClass = dungeonClass;
            this.level = level;
            this.solo = solo;
        }

        double value(ClassBonus bonus) {
            return bonus.value(level, solo);
        }
    }

    private final DungeonRun run;
    private final Map<UUID, State> states = new HashMap<>();
    /** Axes in flight. */
    private final List<ArmorStand> axes = new ArrayList<>();
    private int ticks;

    RunClasses(DungeonRun run) {
        this.run = run;
    }

    /** Stats and damage for everyone in a run, from their class (once, when the server starts runs). */
    static void register() {
        Combat.addMultiplier((player, ranged) -> {
            DungeonRun run = RunManager.of(player);
            return run == null ? 1 : run.classes().damageMultiplier(player, ranged);
        });
        PlayerStats.addModifier((player, stats) -> {
            DungeonRun run = RunManager.of(player);
            if (run != null) run.classes().stats(player, stats);
        });
    }

    /** The run started: everyone's class and level are set, and a class's only player hears it's doubled. */
    void start(List<DungeonRun.Member> members) {
        Map<DungeonClass, Integer> picked = new HashMap<>();
        for (DungeonRun.Member m : members) picked.merge(run.classOf(m.id), 1, Integer::sum);
        for (DungeonRun.Member m : members) {
            DungeonClass dungeonClass = run.classOf(m.id);
            State state = new State(dungeonClass, run.classLevel(m.id), picked.get(dungeonClass) == 1);
            states.put(m.id, state);
            Player player = Bukkit.getPlayer(m.id);
            if (state.solo && player != null) {
                for (String line : ClassBonus.soloMessage(dungeonClass, state.level)) player.sendMessage(Utils.color(line));
            }
        }
    }

    State state(UUID id) {
        return states.get(id);
    }

    // The Dungeon Orb

    /** Its id in the item data (the Dungeon Orb's). */
    static final String ORB = "DUNGEON_STONE";
    /** Its head, as recorded (and in the item data). */
    static final String ORB_TEXTURE = "ba49f9de12c813329f38d0af2da0f32f32a6dcd52c3221d19090c256376c418c";

    static boolean isOrb(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag != null && ORB.equals(tag.getString("id"));
    }

    static boolean hasOrb(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) if (isOrb(stack)) return true;
        return false;
    }

    /**
     * A Dungeon Orb for a member arriving without one, with Mort's lines (MCW Mort, "Without a Dungeon
     * Orb"; MCW Dungeon Orb: "given by Mort on every Dungeon run of Floor III and below if the player does
     * not have a Dungeon Orb in their inventory"). When he says them is UNKNOWN: as they arrive, as far
     * apart as his lines at the start. It's a real item: they keep it.
     *
     * @return whether they got one
     */
    static boolean claimOrb(DungeonRun run, Player player) {
        if (run.floor.isMasterMode() || run.floor.getNumber() > 3 || hasOrb(player)) return false;
        SkyBlockItem item = ItemRegistry.get(ORB);
        if (item == null || player.getInventory().firstEmpty() < 0) return false;
        player.sendMessage(Utils.color("&e[NPC] &bMort&f: Don't forget your &6Dungeon Orb&f!"));
        player.getInventory().addItem(ItemBuilder.build(item));
        player.sendMessage(Utils.color("&aYou claimed Dungeon Orb!"));
        run.later(40, () -> player.sendMessage(Utils.color("&e[NPC] &bMort&f: It gives each class 2 unique abilities inside Dungeons!")));
        run.later(70, () -> player.sendMessage(Utils.color("&e[NPC] &bMort&f: Right click the Orb for spells, and Left click (or Drop) to use your Ultimate!")));
        return true;
    }

    /** Alive, and the run going. */
    private State active(Player player) {
        if (run.phase() != DungeonRun.Phase.RUNNING || run.ghosts().isGhost(player.getUniqueId())) return null;
        return states.get(player.getUniqueId());
    }

    // Stats and damage

    /** Their class's stats on top of the rest. */
    void stats(Player player, Stats stats) {
        State s = active(player);
        if (s == null) return;
        switch (s.dungeonClass) {
            case HEALER -> {
                stats.add(Stat.VITALITY, s.value(ClassBonus.HEALER_VITALITY));
                stats.add(Stat.MENDING, s.value(ClassBonus.HEALER_MENDING));
                // Renew: "Grants 1.6x Mending".
                stats.set(Stat.MENDING, stats.get(Stat.MENDING) * (1 + s.value(ClassBonus.HEALER_RENEW) / 100));
            }
            case MAGE -> {
                stats.add(Stat.INTELLIGENCE, s.value(ClassBonus.MAGE_INTELLIGENCE));
                stats.add(Stat.ABILITY_DAMAGE, s.value(ClassBonus.MAGE_ABILITY_DAMAGE));
            }
            case BERSERK -> {
                stats.add(Stat.SPEED, s.value(ClassBonus.BERSERK_WALK_SPEED));
                stats.add(Stat.SWING_RANGE, s.value(ClassBonus.BERSERK_WEAPON_MASTER));
                if (System.currentTimeMillis() < s.ragnarokUntil) {
                    stats.add(Stat.ATTACK_SPEED, 100);
                    stats.add(Stat.SPEED, 400);
                }
                // Indomitable: a share of their Strength as Defense.
                stats.add(Stat.DEFENSE, stats.get(Stat.STRENGTH) * s.value(ClassBonus.BERSERK_INDOMITABLE) / 100);
            }
            case ARCHER -> {
            }
            case TANK -> {
                stats.add(Stat.HEALTH, s.value(ClassBonus.TANK_HEALTH));
                stats.add(Stat.DEFENSE, s.value(ClassBonus.TANK_DEFENSE));
                stats.add(Stat.VITALITY, s.value(ClassBonus.TANK_VITALITY));
                // Protective Barrier: "Grants 1.3x Defense".
                stats.set(Stat.DEFENSE, stats.get(Stat.DEFENSE) * (1 + s.value(ClassBonus.TANK_PROTECTIVE_BARRIER) / 100));
            }
        }
    }

    /**
     * Their class's damage factor on a hit: the Berserk's Melee Damage (multiplicative, MCW 2026/July 15),
     * Bloodlust's next hit after a kill and Ragnarok's 1.5x; the Archer's Arrow Damage and its -25%
     * melee. Lust For Blood and Weapon Master's extra targets need the target, which the hook doesn't
     * have: left out.
     */
    double damageMultiplier(Player player, boolean ranged) {
        State s = active(player);
        if (s == null) return 1;
        return switch (s.dungeonClass) {
            case BERSERK -> {
                if (ranged) yield 1;
                double factor = 1 + s.value(ClassBonus.BERSERK_MELEE_DAMAGE) / 100;
                long now = System.currentTimeMillis();
                if (now < s.bloodlustUntil) factor *= 1 + s.value(ClassBonus.BERSERK_BLOODLUST_DAMAGE) / 100;
                if (now < s.ragnarokUntil) factor *= 1.5;
                yield factor;
            }
            case ARCHER -> 1 + (ranged ? s.value(ClassBonus.ARCHER_ARROW_DAMAGE) : s.value(ClassBonus.ARCHER_MELEE_DAMAGE)) / 100;
            default -> 1;
        };
    }

    /** A member dealt damage (every hit counts, abilities too): the highest hit, and Bloodlust's heal and bonus. */
    void hit(UUID id, double damage) {
        State s = states.get(id);
        if (s == null || damage <= 0) return;
        long now = System.currentTimeMillis();
        s.hits.addLast(new double[]{now, damage});
        while (!s.hits.isEmpty() && now - s.hits.peekFirst()[0] > HIGHEST_HIT_WINDOW) s.hits.removeFirst();
        if (s.dungeonClass != DungeonClass.BERSERK) return;
        Player player = Bukkit.getPlayer(id);
        if (player == null || run.ghosts().isGhost(id)) return;
        // Bloodlust: the boosted hit takes a second off Throwing Axe's cooldown.
        if (now < s.bloodlustUntil) {
            s.bloodlustUntil = 0;
            if (s.axeReadyAt > now) s.axeReadyAt = Math.max(now, s.axeReadyAt - BLOODLUST_AXE);
        }
        // "Heals you for 3% of your missing health every hit."
        double missing = PlayerHealth.max(player) - PlayerHealth.get(player);
        if (missing > 0) PlayerHealth.heal(player, missing * s.value(ClassBonus.BERSERK_BLOODLUST_HEAL) / 100);
    }

    /** A member killed something: Bloodlust's next hit is boosted for its duration. */
    void kill(UUID id) {
        State s = states.get(id);
        if (s == null || s.dungeonClass != DungeonClass.BERSERK) return;
        s.bloodlustUntil = System.currentTimeMillis() + (long) (s.value(ClassBonus.BERSERK_BLOODLUST_DURATION) * 1000);
    }

    private static double highestHit(State s) {
        long now = System.currentTimeMillis();
        double best = 0;
        for (double[] hit : s.hits) if (now - hit[0] <= HIGHEST_HIT_WINDOW) best = Math.max(best, hit[1]);
        return best;
    }

    // Abilities

    /** Right click on the orb, or ctrl+drop: the class's ability. */
    void ability(Player player) {
        State s = active(player);
        if (s == null) return;
        if (s.dungeonClass == DungeonClass.BERSERK) throwAxe(player, s);
    }

    /** Left click on the orb, or drop: the class's ultimate. */
    void ultimate(Player player) {
        State s = active(player);
        if (s == null) return;
        if (s.dungeonClass == DungeonClass.BERSERK) ragnarok(player, s);
    }

    /** Whether its ultimate can be used, for the tab list's "Ultimate: Ready". */
    String ultimateTab(UUID id) {
        State s = states.get(id);
        long left = s == null ? 0 : s.ultimateReadyAt - System.currentTimeMillis();
        // On cooldown: UNKNOWN on Hypixel; the seconds left.
        return left <= 0 ? "&aReady" : "&e" + Abilities.cooldownSeconds(left) + "s";
    }

    private static boolean onCooldown(Player player, long readyAt) {
        long left = readyAt - System.currentTimeMillis();
        if (left <= 0) return false;
        // A class ability's own line is UNKNOWN: the items' one.
        player.sendMessage(Utils.color("&cThis ability is on cooldown for " + Abilities.cooldownSeconds(left) + "s."));
        return true;
    }

    private void throwAxe(Player player, State s) {
        if (onCooldown(player, s.axeReadyAt)) return;
        s.axeReadyAt = System.currentTimeMillis() + AXE_COOLDOWN;
        s.axeAnnounced = false;
        player.sendMessage(Utils.color("&aUsed &6Throwing Axe&a!"));
        new ThrownAxe(player, highestHit(s)).fly();
    }

    private void ragnarok(Player player, State s) {
        if (onCooldown(player, s.ultimateReadyAt)) return;
        long now = System.currentTimeMillis();
        s.ultimateReadyAt = now + RAGNAROK_COOLDOWN;
        s.ragnarokUntil = now + RAGNAROK_MILLIS;
        s.nextReminder = ticks + (int) (RAGNAROK_COOLDOWN / 50);
        // Never recorded (the recorded player never cast it): Throwing Axe's line. Its 3 Zombie minions aren't built.
        player.sendMessage(Utils.color("&aUsed &6Ragnarok&a!"));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
    }

    /** The run is over: axes still flying go. */
    void dispose() {
        for (ArmorStand axe : axes) axe.remove();
        axes.clear();
    }

    /** Every tick: "Throwing Axe is now available!" and Ragnarok's reminder. */
    void tick() {
        ticks++;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, State> entry : states.entrySet()) {
            State s = entry.getValue();
            if (s.dungeonClass != DungeonClass.BERSERK) continue;
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.getWorld().equals(run.world)) continue;
            if (!s.axeAnnounced && now >= s.axeReadyAt) {
                s.axeAnnounced = true;
                player.sendMessage(Utils.color("&6Throwing Axe &ais now available!"));
            }
            if (ticks >= s.nextReminder) {
                s.nextReminder = ticks + REMINDER_EVERY;
                // Not for ghosts (UNKNOWN whether Hypixel reminds them).
                if (now >= s.ultimateReadyAt && !run.ghosts().isGhost(entry.getKey())) {
                    player.sendMessage(Utils.color("&6Ragnarok&a is ready to use! Press &6&lDROP&a to activate it!"));
                }
            }
        }
    }

    /**
     * Throwing Axe in flight, as recorded: an invisible armor stand holding what the thrower holds (their
     * sword, in both recordings), flying straight ahead until it hits a mob, a wall or its range. It hits
     * the first mob for the thrower's highest hit in the last minute (not a crit: grey, as abilities'
     * numbers are), and the thrower is told "&7Your Throwing Axe hit &c1 &7enemy for &c14,689,667.3 &7damage.".
     */
    private final class ThrownAxe {
        private final Player thrower;
        private final double damage;
        private final Vector step;
        private final ArmorStand stand;
        private int age;

        ThrownAxe(Player thrower, double damage) {
            this.thrower = thrower;
            this.damage = damage;
            this.step = thrower.getEyeLocation().getDirection().normalize().multiply(AXE_SPEED);
            ItemStack held = thrower.getInventory().getItemInMainHand();
            ItemStack shown = held.getType() == Material.AIR ? new ItemStack(Material.IRON_AXE) : held.clone();
            Location at = thrower.getEyeLocation().subtract(0, HAND_HEIGHT, 0);
            this.stand = at.getWorld().spawn(at, ArmorStand.class, s -> {
                s.setVisible(false);
                s.setGravity(false);
                s.setMarker(true);
                s.setArms(true);
                s.setInvulnerable(true);
                s.setPersistent(false);
                s.getEquipment().setItem(EquipmentSlot.HAND, shown);
            });
            axes.add(stand);
        }

        void fly() {
            run.later(1, this::tick);
        }

        private void tick() {
            if (!stand.isValid()) return;
            age++;
            Location at = stand.getLocation().add(step);
            Location item = at.clone().add(0, HAND_HEIGHT, 0);
            stand.setRightArmPose(new EulerAngle(Math.toRadians(age * 60 % 360), 0, 0));
            if (age > AXE_TICKS || item.getBlock().getType().isSolid()) {
                remove();
                return;
            }
            stand.teleport(at);
            for (Entity entity : at.getWorld().getNearbyEntities(item, AXE_REACH, AXE_REACH, AXE_REACH)) {
                if (!(entity instanceof LivingEntity) || entity.equals(thrower) || !hit(entity)) continue;
                remove();
                thrower.sendMessage(Utils.color("&7Your Throwing Axe hit &c1 &7enemy for &c" + DAMAGE.get().format(damage) + " &7damage."));
                return;
            }
            run.later(1, this::tick);
        }

        private void remove() {
            stand.remove();
            axes.remove(stand);
        }

        /** Hurts a mob that can be hurt; false for anything else. */
        private boolean hit(Entity entity) {
            DungeonMobs.Mob dungeonMob = DungeonMobs.of(entity);
            if (dungeonMob != null) {
                if (dungeonMob.invulnerable()) return false;
                if (damage > 0) DungeonMobs.damage(entity, thrower, damage, false);
                return true;
            }
            Mobs.Live mob = Mobs.of(entity);
            if (mob == null || mob.type().isInvulnerable() || mob.health() <= 0) return false;
            if (damage > 0) Mobs.damage(mob, thrower, damage, false);
            return true;
        }
    }
}
