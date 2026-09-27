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
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.classes.ClassBonus;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Utils;

/**
 * The members' classes once the run has started (research critic.md 3.2): each one's class and level
 * as they started, their stats ({@link ClassBonus}, doubled for a class nobody else plays, with the
 * recorded chat), the Berserk's passives, and the Dungeon Orb abilities: the class ability (right
 * click the orb, or ctrl+drop) and the ultimate (left click the orb, or drop). The Berserk's Throwing
 * Axe and Ragnarok are as recorded (messages, timings, the axe's flight; not the axe's damage, see
 * {@link ThrownAxe}); the Archer's Explosive Shot and Rapid Fire, the Tank's Seismic Wave and Castle
 * of Stone and the Healer's Wish follow their lore ({@link ClassAbilities}), with the Berserk's
 * messages; the Healer's Healing Circle and the Mage's aren't built.
 *
 * <p>Stats go in through {@link PlayerStats#addModifier}, damage dealt through {@link
 * Combat#addMultiplier} and damage taken through {@link PlayerDamage#addTakenMultiplier} (see {@link
 * #register}). Main thread.
 */
final class RunClasses {
    /** Ragnarok's first "ready to use" (RUN1 23.4 s, RUN2 23.2 s after the start), then every 30 s while unused. */
    private static final int FIRST_REMINDER = 466;
    private static final int REMINDER_EVERY = 600;
    private static final long AXE_COOLDOWN = 10_000;
    private static final long SEISMIC_WAVE_COOLDOWN = 15_000;
    private static final long RAGNAROK_COOLDOWN = 60_000;
    private static final long RAGNAROK_MILLIS = 15_000;
    private static final long RAPID_FIRE_COOLDOWN = 100_000;
    private static final long CASTLE_OF_STONE_COOLDOWN = 150_000;
    private static final long CASTLE_OF_STONE_MILLIS = 20_000;
    /** Rapid Fire's arrows "deal 75.0% of your highest Bow hit". */
    private static final double RAPID_FIRE_SHARE = 0.75;
    /** "your highest hit in the last minute". */
    private static final long HIGHEST_HIT_WINDOW = 60_000;
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
        long abilityReadyAt;
        boolean abilityAnnounced = true;
        long ultimateReadyAt;
        int nextReminder = FIRST_REMINDER;
        /** Until when Ragnarok or Castle of Stone lasts. */
        long ultimateUntil;
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
    private final ClassAbilities abilities;
    private int ticks;

    RunClasses(DungeonRun run) {
        this.run = run;
        this.abilities = new ClassAbilities(run);
    }

    /** What a class's ability is called; null where it isn't built. */
    static String abilityName(DungeonClass dungeonClass) {
        return switch (dungeonClass) {
            case BERSERK -> "Throwing Axe";
            case ARCHER -> "Explosive Shot";
            case TANK -> "Seismic Wave";
            case HEALER, MAGE -> null;
        };
    }

    /** What a class's ultimate is called; null where it isn't built. */
    static String ultimateName(DungeonClass dungeonClass) {
        return switch (dungeonClass) {
            case BERSERK -> "Ragnarok";
            case ARCHER -> "Rapid Fire";
            case TANK -> "Castle of Stone";
            case HEALER -> "Wish";
            case MAGE -> null;
        };
    }

    /** Stats and damage dealt and taken for everyone in a run, from their class (once, when the server starts runs). */
    static void register() {
        Combat.addMultiplier((player, ranged) -> {
            DungeonRun run = RunManager.of(player);
            return run == null ? 1 : run.classes().damageMultiplier(player, ranged);
        });
        PlayerStats.addModifier((player, stats) -> {
            DungeonRun run = RunManager.of(player);
            if (run != null) run.classes().stats(player, stats);
        });
        PlayerDamage.addTakenMultiplier(player -> {
            DungeonRun run = RunManager.of(player);
            return run == null ? 1 : run.classes().damageTaken(player);
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

    /** Their class's stats on top of the rest (see {@link #classStats}). */
    void stats(Player player, Stats stats) {
        State s = active(player);
        if (s != null) classStats(s, stats, System.currentTimeMillis());
    }

    /**
     * A class's stats, at {@code now}, on top of everything else's in {@code stats}: its bonuses, then
     * what scales the total (the Healer's Renew multiplies all their Mending, the Tank's Protective
     * Barrier all their Defense, the Berserk's Indomitable adds a share of all their Strength).
     */
    static void classStats(State s, Stats stats, long now) {
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
                if (now < s.ultimateUntil) {
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
                // Protective Barrier: "Grants 1.3x Defense". (Castle of Stone is no Defense: see damageTaken.)
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
        return s == null ? 1 : damageFactor(s, ranged, System.currentTimeMillis());
    }

    /** {@link #damageMultiplier} for a member in this state, at {@code now}. */
    static double damageFactor(State s, boolean ranged, long now) {
        return switch (s.dungeonClass) {
            case BERSERK -> {
                if (ranged) yield 1;
                double factor = 1 + s.value(ClassBonus.BERSERK_MELEE_DAMAGE) / 100;
                if (now < s.bloodlustUntil) factor *= 1 + s.value(ClassBonus.BERSERK_BLOODLUST_DAMAGE) / 100;
                if (now < s.ultimateUntil) factor *= 1.5;
                yield factor;
            }
            case ARCHER -> 1 + (ranged ? s.value(ClassBonus.ARCHER_ARROW_DAMAGE) : s.value(ClassBonus.ARCHER_MELEE_DAMAGE)) / 100;
            default -> 1;
        };
    }

    /** The factor on what hits take from them: 0.3 in Castle of Stone (see {@link #damageTakenFactor}). */
    double damageTaken(Player player) {
        State s = active(player);
        return s == null ? 1 : damageTakenFactor(s, System.currentTimeMillis());
    }

    /**
     * Castle of Stone's "reducing the damage you take by 70%", as a factor on what hits take after their
     * Defense (rather than as more Defense, which Seismic Wave's damage and the stats shown would count).
     */
    static double damageTakenFactor(State s, long now) {
        return s.dungeonClass == DungeonClass.TANK && now < s.ultimateUntil ? ClassAbilities.CASTLE_OF_STONE_TAKEN : 1;
    }

    /** Whether this member is a Tank in Castle of Stone now, alive (the undead go for them). */
    boolean inCastleOfStone(UUID id) {
        State s = states.get(id);
        return s != null && s.dungeonClass == DungeonClass.TANK && System.currentTimeMillis() < s.ultimateUntil
                && run.phase() == DungeonRun.Phase.RUNNING && !run.ghosts().isGhost(id);
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
        // Bloodlust's boosted hit is used up. Its lore also has it take a second off Throwing Axe's
        // cooldown ("on activation"), but the recordings don't: the axe is back exactly 10.0 s after
        // each throw (RUN1 00:33.1, 00:57.6, 01:58.7; RUN2 01:59.1) with kills all through. Left out.
        if (now < s.bloodlustUntil) s.bloodlustUntil = 0;
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
        String name = s == null ? null : abilityName(s.dungeonClass);
        if (name == null || onCooldown(player, s.abilityReadyAt)) return;
        long now = System.currentTimeMillis();
        s.abilityAnnounced = false;
        player.sendMessage(Utils.color("&aUsed &6" + name + "&a!"));
        switch (s.dungeonClass) {
            case BERSERK -> {
                s.abilityReadyAt = now + AXE_COOLDOWN;
                new ThrownAxe(player, highestHit(s)).fly();
            }
            case ARCHER -> {
                s.abilityReadyAt = now + ClassAbilities.explosiveShotCooldown(s.level);
                // "your highest bow hit": bow hits aren't told apart from the rest here.
                abilities.explosiveShot(player, highestHit(s));
            }
            case TANK -> {
                s.abilityReadyAt = now + SEISMIC_WAVE_COOLDOWN;
                abilities.seismicWave(player);
            }
            default -> {
            }
        }
    }

    /**
     * Left click on the orb, or drop: the class's ultimate. "Used ...!" is Throwing Axe's line: no
     * ultimate was ever cast in a recording.
     */
    void ultimate(Player player) {
        State s = active(player);
        String name = s == null ? null : ultimateName(s.dungeonClass);
        if (name == null || onCooldown(player, s.ultimateReadyAt)) return;
        long now = System.currentTimeMillis();
        long cooldown = switch (s.dungeonClass) {
            case BERSERK -> {
                // Its 3 Zombie minions aren't built.
                s.ultimateUntil = now + RAGNAROK_MILLIS;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
                yield RAGNAROK_COOLDOWN;
            }
            case ARCHER -> {
                abilities.rapidFire(player, RAPID_FIRE_SHARE * highestHit(s), ClassAbilities.rapidFireSeconds(s.level));
                yield RAPID_FIRE_COOLDOWN;
            }
            case TANK -> {
                s.ultimateUntil = now + CASTLE_OF_STONE_MILLIS;
                abilities.castleOfStone(player);
                yield CASTLE_OF_STONE_COOLDOWN;
            }
            case HEALER -> ClassAbilities.wishCooldown(abilities.wish(player));
            case MAGE -> 0;
        };
        usedUltimate(s, ticks, now, cooldown);
        player.sendMessage(Utils.color("&aUsed &6" + name + "&a!"));
    }

    /** One of their arrows landed; false if it isn't an ability's. */
    boolean arrowLanded(org.bukkit.entity.Projectile projectile, Entity hit) {
        return abilities.landed(projectile, hit);
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

    /** The run is over: axes and arrows still flying go. */
    void dispose() {
        for (ArmorStand axe : axes) axe.remove();
        axes.clear();
        abilities.dispose();
    }

    /**
     * Every tick: "Throwing Axe is now available!" when its cooldown is over, and "Ragnarok is ready to
     * use! Press DROP to activate it!" while it's unused (the Berserk's, as recorded; the other built
     * ones' the same way, as SkyHanni's chat filter has Rapid Fire's and Castle of Stone's).
     */
    void tick() {
        ticks++;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, State> entry : states.entrySet()) {
            State s = entry.getValue();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.getWorld().equals(run.world)) continue;
            String ability = abilityName(s.dungeonClass);
            if (ability != null && !s.abilityAnnounced && now >= s.abilityReadyAt) {
                s.abilityAnnounced = true;
                player.sendMessage(Utils.color("&6" + ability + " &ais now available!"));
            }
            String ultimate = ultimateName(s.dungeonClass);
            // Not for ghosts (UNKNOWN whether Hypixel reminds them).
            if (ultimate != null && reminderDue(s, ticks, now) && !run.ghosts().isGhost(entry.getKey())) {
                player.sendMessage(Utils.color("&6" + ultimate + "&a is ready to use! Press &6&lDROP&a to activate it!"));
            }
        }
    }

    /**
     * Whether the ultimate's "ready to use" line is due at run tick {@code tick}, {@code now}: 466 ticks
     * in, then every 600 while it's unused, and when a cooldown is over (see {@link #usedUltimate}).
     * Moves the next one on either way.
     */
    static boolean reminderDue(State s, int tick, long now) {
        if (tick < s.nextReminder) return false;
        s.nextReminder = tick + REMINDER_EVERY;
        return now >= s.ultimateReadyAt;
    }

    /** The ultimate was used at run tick {@code tick}, {@code now}: it's ready, and reminded of, when the cooldown is over. */
    static void usedUltimate(State s, int tick, long now, long cooldown) {
        s.ultimateReadyAt = now + cooldown;
        s.nextReminder = tick + (int) (cooldown / 50);
    }

    /**
     * Throwing Axe in flight, as recorded: an invisible armor stand holding what the thrower holds (their
     * sword, in both recordings), flying straight ahead until it hits a mob, a wall or its range. It hits
     * the first mob (not a crit: grey, as abilities' numbers are), and the thrower is told "&7Your
     * Throwing Axe hit &c1 &7enemy for &c14,689,667.3 &7damage.". APPROXIMATION: how much it hits for.
     * The lore's "the same damage as your highest hit in the last minute" is used, but the one recorded
     * throw (RUN2 01:59.6, 14,689,667.3) was 0.90 of the highest number shown in the minute before it
     * (16,295,779 at 01:57.8), so the real formula is something else, UNKNOWN.
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

        private boolean hit(Entity entity) {
            return ClassAbilities.hurt(entity, thrower, damage);
        }
    }
}
