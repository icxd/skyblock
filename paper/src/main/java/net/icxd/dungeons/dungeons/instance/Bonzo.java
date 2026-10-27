package net.icxd.dungeons.dungeons.instance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.dungeons.paste.BossArena;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.mob.MobKinds;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.goals.AvatarControl;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * Floor I's boss, in his circus ring ({@link BossArena#BONZO}), as three recorded fights have him (2026-10-26 and 27),
 * with the wiki's numbers (MCW Bonzo) where they can't tell. Times are ticks from the fight's start, which is when the
 * first player comes through the Blood Room's portal.
 *
 * <ol>
 *   <li>He stands in the middle and talks (three lines 4.1 s apart). From 11.5 s he moves about the ring, running (0.33
 *       blocks a tick) or, somewhere far or up a pillar, leaping (a block a tick), and stops between. Standing, he
 *       fires wither skulls at the nearest player he can see, one every 0.5 to 0.95 s: they speed up as vanilla's do,
 *       and hit for 5,328 (twice his 2,664), "Bonzo's Wither Skull hit you for 5,328 damage.". From 13.6 s he summons
 *       Undead ({@link BonzoUndead}), the first in the middle, the rest anywhere in the ring, 0.4 to 4.6 s apart. Hit,
 *       he complains: "Oh noes, you got me.. what ever will I do?!" the first time, then one of the wiki's lines at
 *       most every 4.1 s.</li>
 *   <li>At 0 he dies, but heads for the middle anyway: "Oh I'm dead!" a second later, "Sike" 3.1 s after that, and
 *       1.1 s later he's back in his second skin, with all his health and his staff.</li>
 *   <li>Now he chases the nearest player and hits them (2,664), and at one farther than 6 blocks fires three balloons
 *       half a second apart (0.6 of his damage: "Bonzo's Balloon hit you for 1598.3999999999999 damage.", printed as
 *       Java prints it, as recorded; "Do you want a balloon Name?" the first time). 7.1 s in, and every 33.5 s after
 *       that: "SHOOOOOOOOWWWWWWW TIMEEEEEEE!". He goes to the middle, can't be hurt, and sends a balloon off every 3
 *       ticks in any direction for 8.4 s (a line of the wiki's for it 4.1 s in), then ten quick ones, and is back at
 *       it 13.6 s after his line.</li>
 *   <li>At 0 again: "Alright, maybe I'm just weak after all..", and the run's over 2.1 s later. 2 s after the summary
 *       the north pillar sinks into the floor like a door, and 2 s after that so does the door of the tunnel behind it;
 *       his last two lines come 4.15 and 8.25 s after he died.</li>
 * </ol>
 *
 * <p>Not recorded, so not here: whether he can be hurt while he talks at the start, what decides where he goes
 * (here: away from players, up a pillar two times in five), and how often a volley of balloons comes (here: 5 s at
 * the most). Main thread; {@link DungeonRun} ticks him.
 */
final class Bonzo implements DungeonMobs.Mob {
    enum Stage { FIRST, DOWN, SECOND, DEAD }

    // The start
    static final List<String> INTRO = List.of("Gratz for making it this far, but I'm basically unbeatable.",
            "I don't even need to fight, this is the life!", "I can summon lots of undead! Check this out.");
    static final int[] INTRO_AT = {2, 84, 168};
    static final int MOVES_FROM = 230;
    static final int FIRST_SUMMON = 272;
    static final int SUMMON_GAP_MIN = 8;
    static final int SUMMON_GAP_MAX = 92;
    static final int SKULL_GAP_MIN = 10;
    static final int SKULL_GAP_MAX = 19;
    /** His lines when hit come at most this often (all recorded ones 4.1 s apart or more). */
    static final int LINE_GAP = 82;
    static final String FIRST_HURT = "Oh noes, you got me.. what ever will I do?!";
    /** The wiki's "During First Phase" lines (recorded: Cringe., Cheater!, Hacker!). */
    static final List<String> HURT_LINES = List.of("Stop.", "You're not funny.", "Wait..", "Cringe.", "Give me 1 sec..", "Lame.",
            "Leave me alone!", "Fight my Undeads instead.", "Why are you running?", "Cheater!", "Hacker!", "Not fair!", "OUCH!");
    // Dead, for a while
    static final int DEAD_LINE_AFTER = 20;
    static final int SIKE_AFTER = DEAD_LINE_AFTER + 62;
    static final int BACK_AFTER = SIKE_AFTER + 22;
    // The second phase, from when he's back
    static final int SHOW_FIRST = 142;
    static final int SHOW_EVERY = 670;
    static final String SHOW_TIME = "SHOOOOOOOOWWWWWWW TIMEEEEEEE!";
    /** The wiki's Balloon Barrage lines (recorded: Weeeeeeeee, Do you want a balloon?). */
    static final List<String> BARRAGE_LINES = List.of("All I have are positive thoughts.", "Do you want a balloon?", "Run, run, run, RUN!",
            "Weeeeeeeee");
    /** From his SHOW TIME line: the balloons, his line among them, the quick ones and when he's done. */
    static final int BARRAGE_FROM = 4;
    static final int BARRAGE_TO = 172;
    static final int BARRAGE_EVERY = 3;
    static final int BARRAGE_LINE = 82;
    static final int FINALE_FROM = 256;
    static final int FINALE_COUNT = 10;
    static final int FINALE_EVERY = 2;
    static final int SHOW_ENDS = 272;
    static final int VOLLEY = 3;
    static final int VOLLEY_GAP = 10;
    static final int VOLLEY_EVERY = 100;
    static final double VOLLEY_FROM = 6;
    // The end
    static final String DEATH_LINE = "Alright, maybe I'm just weak after all..";
    static final int SUMMARY_AFTER = 42;
    static final int MASTERS_AFTER = 83;
    static final int WAIT_AFTER = 165;
    static final int PILLAR_AFTER = SUMMARY_AFTER + 40;
    static final int TUNNEL_AFTER = PILLAR_AFTER + 40;

    // How he moves and hits
    static final double RUN = 0.33;
    static final double LEAP = 1.0;
    static final double REACH = 2.8;
    static final int SWING_EVERY = 20;
    static final int STAGGER = 8;
    /** Balloons: the barrage's (recorded 0.75 to 0.8 blocks a tick), the quick ones' and the aimed ones'. */
    static final double BALLOON_SPEED = 0.78;
    static final double FINALE_SPEED = 1.4;
    static final double AIMED_SPEED = 0.8;
    static final int BALLOON_LIFE = 40;
    /** A balloon's head is this far above the stand's feet; the stands leave half a block below his. */
    private static final double BALLOON_HEAD = 1.6;
    private static final double BALLOON_BELOW = 0.5;
    /** The recorded skulls left 1.47 blocks above his feet, at 0.1 blocks a tick, gaining 0.1 a tick as vanilla's do. */
    private static final double SKULL_FROM = 1.47;
    private static final int SKULL_LIFE = 100;
    private static final double SIGHT = 30;

    static final double BASE_DAMAGE = 2_664;
    static final double SKULL_DAMAGE = 2 * BASE_DAMAGE;
    static final double BALLOON_DAMAGE = 0.6 * BASE_DAMAGE;

    // Name tags over him (recorded heights over his feet), and his lines (3.1 s each)
    private static final double NAME_ABOVE = 2.13;
    private static final double BOSS_ABOVE = 2.44;
    private static final double SPEECH_ABOVE = 2.84;
    /** Lying dead in the middle, his lines are lower. */
    private static final double DOWN_SPEECH_ABOVE = 1.09;
    private static final int SPEECH_LASTS = 62;

    // The ring, as captured
    static final double FLOOR_Y = 71;
    static final double PILLAR_TOP = 76;
    /** Spots are this far from the middle at most (the wall is about 23 out). */
    static final double RING = 18;
    /** The seven pillars' middle blocks, x and z: 3x3, from the floor to y 75. */
    static final int[][] PILLARS = {{-43, 5}, {-53, 10}, {-33, 10}, {-58, 20}, {-28, 20}, {-53, 30}, {-33, 30}};
    /** What sinks once he's beaten: the north pillar, then the tunnel's door behind it (x y z, min then max). */
    static final int[] PILLAR_BLOCKS = {-44, 71, 4, -42, 75, 6};
    static final int[] TUNNEL_BLOCKS = {-44, 71, -6, -42, 75, -4};

    private static final List<String> BALLOONS = List.of("Balloon (Orange)", "Balloon (Yellow)", "Balloon (Red)", "Balloon (Green)",
            "Balloon (Cyan)", "Balloon (Blue)", "Balloon (Purple)", "Balloon (Pink)", "Balloon (Lime)");
    /** His wither skulls in flight, for their hits (see {@link #skullHit}). */
    private static final Map<UUID, Bonzo> SKULLS = new HashMap<>();
    /**
     * On each of his skulls, so it's still known as his when it blows up: Paper runs a skull's hit on a block even when
     * the hit's event is cancelled, after {@link #skullHit} has let it go.
     */
    private static final String SKULL_TAG = "bonzo_skull";

    private final DungeonRun run;
    private final BossArena arena;
    private final World world;
    private final Location center;
    private final double maxHealth = MobKinds.BONZO.variant(DungeonFloor.FLOOR_1, null).health();
    private Mannequin body;
    private final ArmorStand name;
    private final ArmorStand boss;
    private ArmorStand speech;
    private int speechUntil;
    private final BossBar bar;
    private final List<BonzoUndead> undeads = new ArrayList<>();
    private final List<Balloon> balloons = new ArrayList<>();
    private final Map<WitherSkull, Integer> skulls = new HashMap<>();

    private Stage stage = Stage.FIRST;
    private double health = maxHealth;
    private int age;
    /** Ticks since the stage began. */
    private int stageAge;
    private int lastLine = -LINE_GAP;
    private boolean hurtYet;
    private int staggeredUntil;

    // Moving about (first phase)
    private Location goal;
    private Location leapFrom;
    private int leapTicks;
    private int leapTick;
    private double leapHeight;
    private int standUntil;
    /** A run that hasn't got there by then (a pillar in the way) ends where he is. */
    private int runUntil;
    private int nextSkull;
    private int nextSummon = FIRST_SUMMON;

    // Second phase
    private int nextSwing;
    private int nextVolley;
    private int volleyLeft;
    private int nextVolleyShot;
    private Player volleyAt;
    private boolean volleyedYet;
    private int showAt = SHOW_FIRST;
    /** When his SHOW TIME began (in stage ticks), or -1 when it's not on. */
    private int showStart = -1;

    Bonzo(DungeonRun run, BossArena arena) {
        this.run = run;
        this.arena = arena;
        this.world = run.world;
        double[] c = arena.center();
        this.center = new Location(world, c[0], c[1], c[2], 180, 0);
        this.body = spawnBody(center, "Bonzo", "DARK_ORB", Material.PAPER);
        this.name = stand(center.clone().add(0, NAME_ABOVE, 0), nameTag(false, health));
        this.boss = stand(center.clone().add(0, BOSS_ABOVE, 0), "&c&lBOSS");
        this.bar = Bukkit.createBossBar(Utils.color("&c&lBonzo"), BarColor.RED, BarStyle.SOLID);
        DungeonMobs.add(body, this);
    }

    DungeonRun run() {
        return run;
    }

    private Mannequin spawnBody(Location at, String skin, String item, Material fallback) {
        return world.spawn(at, Mannequin.class, m -> {
            m.setProfile(DungeonTextures.profile(skin));
            m.setDescription(null);
            m.setPersistent(false);
            m.setRemoveWhenFarAway(false);
            // His health is ours; the entity just needs to survive the hits.
            var max = m.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) max.setBaseValue(1024);
            m.setHealth(1024);
            m.getEquipment().setItemInMainHand(item(item, fallback));
        });
    }

    private static ItemStack item(String id, Material fallback) {
        SkyBlockItem item = ItemRegistry.get(id);
        return item == null ? new ItemStack(fallback) : ItemBuilder.build(item);
    }

    private ArmorStand stand(Location at, String text) {
        return world.spawn(at, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setMarker(true);
            s.setPersistent(false);
            s.customName(Text.line(text));
            s.setCustomNameVisible(true);
        });
    }

    // What his tags say

    /**
     * "&e✰&5♃ &cBonzo &a250k&c❤" (recorded with Hypixel's glyphs: Humanoid and Arcane, Undead and Arcane in his second
     * phase), yellow at 0.
     */
    static String nameTag(boolean secondPhase, double health) {
        MobType first = secondPhase ? MobType.UNDEAD : MobType.HUMANOID;
        return "&" + first.color() + first.symbol() + "&" + MobType.ARCANE.color() + MobType.ARCANE.symbol() + " &cBonzo "
                + (health > 0 ? "&a" : "&e") + shortHealth(health) + "&c❤";
    }

    /** "250k", "236.4k", "97.6k", "2.8k", "0" (recorded): to a tenth of a thousand, without ".0". */
    static String shortHealth(double health) {
        if (health <= 0) return "0";
        if (health < 1_000) return String.valueOf((long) Math.ceil(health));
        boolean millions = health >= 999_950;
        BigDecimal shown = BigDecimal.valueOf(health / (millions ? 1_000_000 : 1_000)).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        return shown.toPlainString() + (millions ? "M" : "k");
    }

    /** What a skull's hit says (recorded: thousands grouped). */
    static String skullMessage() {
        return "&7Bonzo's Wither Skull hit you for &c" + Utils.getFormattedNumber((int) SKULL_DAMAGE) + "&7 damage.";
    }

    /** What a balloon's hit says: the damage as Java prints a double, as recorded ("1598.3999999999999"). */
    static String balloonMessage() {
        return "&7Bonzo's Balloon hit you for &c" + BALLOON_DAMAGE + "&7 damage.";
    }

    private void say(String line) {
        say(line, SPEECH_ABOVE);
    }

    private void say(String line, double above) {
        lastLine = age;
        run.tell("&c[BOSS] Bonzo&f: " + line);
        if (speech != null) speech.remove();
        speech = stand(body.getLocation().add(0, above, 0), "&l" + line);
        speechUntil = age + SPEECH_LASTS;
    }

    private static <T> T any(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private static int between(int min, int max) {
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }

    // Every tick

    void tick() {
        age++;
        stageAge++;
        switch (stage) {
            case FIRST -> first();
            case DOWN -> down();
            case SECOND -> second();
            case DEAD -> {
            }
        }
        for (BonzoUndead undead : List.copyOf(undeads)) {
            undead.tick();
            if (undead.isDead()) undeads.remove(undead);
        }
        for (Balloon balloon : List.copyOf(balloons)) balloon.tick();
        skulls.entrySet().removeIf(e -> {
            if (e.getKey().isValid() && age - e.getValue() < SKULL_LIFE) return false;
            SKULLS.remove(e.getKey().getUniqueId());
            e.getKey().remove();
            return true;
        });
        if (speech != null && age >= speechUntil) {
            speech.remove();
            speech = null;
        }
        if (stage != Stage.DEAD) {
            Location at = body.getLocation();
            name.teleport(at.clone().add(0, NAME_ABOVE, 0));
            boss.teleport(at.clone().add(0, BOSS_ABOVE, 0));
            if (age % 10 == 0) showBar();
        }
    }

    /** Players in the ring who can be fought: alive (not ghosts), playing. */
    List<Player> fighters() {
        List<Player> out = new ArrayList<>();
        for (Player player : run.players()) {
            Location at = player.getLocation();
            if (player.isDead() || run.isGhost(player.getUniqueId()) || player.getGameMode() == GameMode.SPECTATOR
                    || player.getGameMode() == GameMode.CREATIVE || !arena.contains(at.getX(), at.getY(), at.getZ())) continue;
            out.add(player);
        }
        return out;
    }

    /** The nearest of them, or null. */
    Player nearest(Location from) {
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : fighters()) {
            double distance = player.getLocation().distanceSquared(from);
            if (distance < bestDistance) {
                best = player;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void showBar() {
        for (Player player : run.players()) {
            Location at = player.getLocation();
            boolean inside = arena.contains(at.getX(), at.getY(), at.getZ());
            if (inside && !bar.getPlayers().contains(player)) bar.addPlayer(player);
            else if (!inside && bar.getPlayers().contains(player)) bar.removePlayer(player);
        }
    }

    private void updateHealth() {
        name.customName(Text.line(nameTag(stage == Stage.SECOND || stage == Stage.DEAD, health)));
        bar.setProgress(Math.clamp(health / maxHealth, 0, 1));
    }

    // The first phase

    private void first() {
        for (int i = 0; i < INTRO.size(); i++) {
            if (age == INTRO_AT[i]) say(INTRO.get(i));
        }
        if (age >= nextSummon) {
            summon(age == FIRST_SUMMON ? center.clone() : spot(false));
            nextSummon = age + between(SUMMON_GAP_MIN, SUMMON_GAP_MAX);
        }
        Player nearest = nearest(body.getLocation());
        if (age < MOVES_FROM) {
            if (nearest != null) AvatarControl.face(body, nearest.getLocation());
            return;
        }
        if (leapFrom != null) {
            leap();
            return;
        }
        if (age < staggeredUntil) return;
        if (goal != null) {
            AvatarControl.face(body, goal);
            AvatarControl.steer(body, goal, RUN);
            if (horizontal(body.getLocation(), goal) <= 1 || age >= runUntil) arrived();
            return;
        }
        // Standing: off again when it's time, or someone's at him; skulls at whoever's nearest meanwhile.
        if (age >= standUntil || (nearest != null && nearest.getLocation().distanceSquared(body.getLocation()) < 25)) {
            moveOn();
            return;
        }
        if (nearest == null) return;
        AvatarControl.face(body, nearest.getLocation());
        if (age >= nextSkull && body.getLocation().distanceSquared(nearest.getLocation()) <= SIGHT * SIGHT && body.hasLineOfSight(nearest)) {
            nextSkull = age + between(SKULL_GAP_MIN, SKULL_GAP_MAX);
            skull(nearest);
        }
    }

    private static double horizontal(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Somewhere new: up a pillar two times in five, else a spot on the floor away from the players; far or high is a leap. */
    private void moveOn() {
        boolean pillar = ThreadLocalRandom.current().nextInt(5) < 2;
        Location to = spot(pillar);
        Location me = body.getLocation();
        if (pillar || horizontal(me, to) > 8 || Math.abs(to.getY() - me.getY()) > 1) {
            startLeap(to, Math.max(1.5, to.getY() - me.getY() + 1.5));
        } else {
            goal = to;
            runUntil = age + (int) Math.ceil(horizontal(me, to) / RUN) + 20;
        }
    }

    private void arrived() {
        goal = null;
        boolean onPillar = body.getLocation().getY() >= PILLAR_TOP - 0.5;
        standUntil = age + (onPillar ? between(120, 240) : between(20, 60));
        nextSkull = Math.max(nextSkull, age + between(SKULL_GAP_MIN, SKULL_GAP_MAX));
    }

    private void startLeap(Location to, double height) {
        leapFrom = body.getLocation();
        goal = to;
        leapHeight = height;
        leapTicks = Math.max(6, (int) Math.ceil(horizontal(leapFrom, to) / LEAP));
        leapTick = 0;
        body.setGravity(false);
    }

    /** Along an arc to the goal, a block a tick (as recorded), landing on it. */
    private void leap() {
        leapTick++;
        double t = Math.min(1, leapTick / (double) leapTicks);
        Location at = leapFrom.clone().add(goal.clone().subtract(leapFrom).toVector().multiply(t));
        at.add(0, 4 * leapHeight * t * (1 - t), 0);
        at.setDirection(goal.toVector().subtract(leapFrom.toVector()).setY(0));
        body.teleport(at);
        if (t < 1) return;
        leapFrom = null;
        body.setGravity(true);
        body.setVelocity(new Vector());
        if (stage == Stage.FIRST) arrived();
        else goal = null;
    }

    /**
     * Somewhere in the ring: a pillar's top, or a spot on the floor that isn't in one, the farthest from the players of
     * a few tried.
     */
    private Location spot(boolean pillar) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (pillar) {
            int[] p = PILLARS[random.nextInt(PILLARS.length)];
            return new Location(world, p[0] + 0.5, PILLAR_TOP, p[1] + 0.5);
        }
        Location best = null;
        double bestDistance = -1;
        List<Player> players = fighters();
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble(Math.PI * 2);
            double r = RING * Math.sqrt(random.nextDouble());
            Location at = new Location(world, Math.floor(center.getX() + r * Math.cos(angle)) + 0.5, FLOOR_Y,
                    Math.floor(center.getZ() + r * Math.sin(angle)) + 0.5);
            if (inPillar(at)) continue;
            double distance = Double.MAX_VALUE;
            for (Player player : players) distance = Math.min(distance, horizontal(player.getLocation(), at));
            if (distance > bestDistance) {
                best = at;
                bestDistance = distance;
            }
        }
        return best != null ? best : center.clone();
    }

    static boolean inPillar(Location at) {
        for (int[] p : PILLARS) {
            if (Math.abs(Math.floor(at.getX()) - p[0]) <= 2 && Math.abs(Math.floor(at.getZ()) - p[1]) <= 2) return true;
        }
        return false;
    }

    private void summon(Location at) {
        body.swingMainHand();
        undeads.add(new BonzoUndead(this, at));
    }

    private void skull(Player at) {
        body.swingMainHand();
        Location from = body.getLocation().add(0, SKULL_FROM, 0);
        Vector direction = at.getEyeLocation().toVector().subtract(from.toVector());
        if (direction.lengthSquared() < 1e-6) return;
        Vector aim = direction.normalize();
        WitherSkull skull = world.spawn(from.add(aim.clone().multiply(0.6)), WitherSkull.class, s -> {
            s.setShooter(body);
            s.setPersistent(false);
            s.addScoreboardTag(SKULL_TAG);
            s.setAcceleration(aim.clone().multiply(0.1));
            s.setVelocity(aim.clone().multiply(0.1));
        });
        skulls.put(skull, age);
        SKULLS.put(skull.getUniqueId(), this);
    }

    /**
     * One of his skulls hit something: it's gone (it doesn't blow up), and a player it hit takes its damage. False if
     * it isn't his.
     */
    static boolean skullHit(Entity projectile, Entity hit) {
        Bonzo bonzo = SKULLS.remove(projectile.getUniqueId());
        if (bonzo == null) return false;
        bonzo.skulls.remove(projectile);
        projectile.remove();
        if (hit instanceof Player player && bonzo.fighters().contains(player)) {
            double taken = PlayerDamage.hit(player, SKULL_DAMAGE, PlayerDamage.Kind.NORMAL, bonzo.body, 1);
            if (taken > 0) player.sendMessage(Utils.color(skullMessage()));
        }
        return true;
    }

    /** Whether this is one of his skulls (they don't blow up). */
    static boolean isSkull(Entity entity) {
        return entity.getScoreboardTags().contains(SKULL_TAG);
    }

    // Dead, for a while

    private void fallDown() {
        stage = Stage.DOWN;
        stageAge = 0;
        health = 0;
        updateHealth();
        goal = null;
        // He falls over, as anything dying does, and stays down in the middle.
        body.playEffect(EntityEffect.ENTITY_DEATH);
        body.getEquipment().setItemInMainHand(null);
        startLeap(center.clone(), 1);
    }

    private void down() {
        if (leapFrom != null) leap();
        if (stageAge == DEAD_LINE_AFTER) say("Oh I'm dead!", DOWN_SPEECH_ABOVE);
        if (stageAge == SIKE_AFTER) say("Sike", DOWN_SPEECH_ABOVE);
        if (stageAge == BACK_AFTER) back();
    }

    /** Back in his second skin, in the middle, with all his health and his staff. */
    private void back() {
        DungeonMobs.remove(body);
        body.remove();
        leapFrom = null;
        body = spawnBody(center, "Bonzo (Phase 2)", "BONZO_STAFF", Material.BLAZE_ROD);
        DungeonMobs.add(body, this);
        stage = Stage.SECOND;
        stageAge = 0;
        health = maxHealth;
        updateHealth();
        nextVolley = stageAge;
    }

    // The second phase

    private void second() {
        if (showStart >= 0) {
            show();
            return;
        }
        if (stageAge >= showAt) {
            showStart = stageAge;
            show();
            return;
        }
        if (leapFrom != null) {
            leap();
            return;
        }
        volley();
        Player target = nearest(body.getLocation());
        if (target == null || age < staggeredUntil) return;
        Location them = target.getLocation();
        AvatarControl.face(body, them);
        double distance = body.getLocation().distance(them);
        if (distance > 1.2) AvatarControl.steer(body, them, RUN);
        if (distance <= REACH && age >= nextSwing) {
            nextSwing = age + SWING_EVERY;
            body.swingMainHand();
            DungeonMobs.hit(target, BASE_DAMAGE, body);
        }
        if (distance >= VOLLEY_FROM && volleyLeft == 0 && stageAge >= nextVolley) {
            volleyLeft = VOLLEY;
            volleyAt = target;
            nextVolleyShot = age;
            nextVolley = stageAge + VOLLEY_EVERY;
        }
    }

    /** Three balloons at someone, half a second apart; the first time, with a line for them. */
    private void volley() {
        if (volleyLeft <= 0 || age < nextVolleyShot) return;
        if (volleyAt == null || !volleyAt.isOnline() || !fighters().contains(volleyAt)) {
            volleyLeft = 0;
            return;
        }
        Location from = body.getLocation().subtract(0, BALLOON_BELOW, 0);
        Vector aim = volleyAt.getLocation().add(0, 0.9, 0).toVector().subtract(from.clone().add(0, BALLOON_HEAD, 0).toVector());
        balloon(from, aim, AIMED_SPEED);
        volleyLeft--;
        nextVolleyShot = age + VOLLEY_GAP;
        if (!volleyedYet && volleyLeft == 1) {
            volleyedYet = true;
            say("Do you want a balloon " + volleyAt.getName() + "?");
        }
    }

    /** SHOW TIME: to the middle, can't be hurt, balloons every way, then the quick ones, and on again. */
    private void show() {
        int t = stageAge - showStart;
        if (t == 0) {
            say(SHOW_TIME);
            volleyLeft = 0;
            startLeap(center.clone(), 1);
            leapTicks = Math.max(1, Math.min(leapTicks, BARRAGE_FROM));
        }
        if (leapFrom != null) leap();
        if (t >= BARRAGE_FROM && t <= BARRAGE_TO && (t - BARRAGE_FROM) % BARRAGE_EVERY == 0) balloon(BALLOON_SPEED);
        if (t == BARRAGE_LINE) say(any(BARRAGE_LINES));
        if (t >= FINALE_FROM && t < FINALE_FROM + FINALE_COUNT * FINALE_EVERY && (t - FINALE_FROM) % FINALE_EVERY == 0) balloon(FINALE_SPEED);
        if (t >= SHOW_ENDS) {
            showStart = -1;
            showAt = stageAge - t + SHOW_EVERY;
        }
    }

    /** A balloon off from him in any direction along the floor. */
    private void balloon(double speed) {
        double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        balloon(body.getLocation().subtract(0, BALLOON_BELOW, 0), new Vector(Math.cos(angle), 0, Math.sin(angle)), speed);
    }

    private void balloon(Location from, Vector direction, double speed) {
        if (direction.lengthSquared() < 1e-6) return;
        Vector step = direction.clone().normalize().multiply(speed);
        ArmorStand stand = world.spawn(from, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setMarker(true);
            s.setPersistent(false);
            s.setInvulnerable(true);
            s.getEquipment().setHelmet(DungeonTextures.head(any(BALLOONS)));
        });
        balloons.add(new Balloon(stand, step));
    }

    /** A balloon on its way: it pops on the first player it reaches (their hit) or anything solid, or once it's gone far. */
    private final class Balloon {
        final ArmorStand stand;
        final Vector step;
        int left = BALLOON_LIFE;

        Balloon(ArmorStand stand, Vector step) {
            this.stand = stand;
            this.step = step;
        }

        void tick() {
            Location at = stand.getLocation().add(step);
            stand.teleport(at);
            left--;
            Location head = at.clone().add(0, BALLOON_HEAD, 0);
            for (Player player : fighters()) {
                if (!player.getBoundingBox().expand(0.3).contains(head.toVector())) continue;
                pop(head, true);
                double taken = PlayerDamage.hit(player, BALLOON_DAMAGE, PlayerDamage.Kind.NORMAL, body, 1.5);
                if (taken > 0) player.sendMessage(Utils.color(balloonMessage()));
                return;
            }
            if (left <= 0 || head.getBlock().getType().isSolid()) pop(head, false);
        }

        /** Gone; on a player, with a firework's burst (as recorded: a rocket that went off where it hit). */
        void pop(Location head, boolean burst) {
            balloons.remove(this);
            stand.remove();
            if (!burst) return;
            world.spawnParticle(Particle.FIREWORK, head, 20, 0.2, 0.2, 0.2, 0.15);
            world.playSound(head, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
        }
    }

    // Being hit

    @Override
    public boolean invulnerable() {
        return stage == Stage.DOWN || stage == Stage.DEAD || showStart >= 0;
    }

    @Override
    public double health() {
        return health;
    }

    @Override
    public double maxHealth() {
        return maxHealth;
    }

    @Override
    public double magicResistance() {
        return MobKinds.BONZO.magicResistance();
    }

    @Override
    public Set<MobType> types() {
        return stage == Stage.FIRST ? Set.of(MobType.HUMANOID, MobType.ARCANE) : Set.of(MobType.UNDEAD, MobType.ARCANE);
    }

    @Override
    public void hurt(Player by, double damage) {
        if (invulnerable()) return;
        health -= damage;
        run.damageDealt(by.getUniqueId(), damage);
        staggeredUntil = age + STAGGER;
        if (stage == Stage.FIRST && leapFrom == null) {
            if (!hurtYet) {
                hurtYet = true;
                say(FIRST_HURT);
            } else if (age - lastLine >= LINE_GAP) say(any(HURT_LINES));
        }
        if (health > 0) {
            updateHealth();
            return;
        }
        if (stage == Stage.FIRST) fallDown();
        else defeated(by);
    }

    // The end

    /** Beaten: his line, his kill, and the run's over 2.1 s later, with the pillar and the tunnel's door after. */
    private void defeated(Player killer) {
        stage = Stage.DEAD;
        health = 0;
        updateHealth();
        Location at = body.getLocation();
        sayAt(at, DEATH_LINE);
        bar.removeAll();
        for (Balloon balloon : List.copyOf(balloons)) balloon.pop(balloon.stand.getLocation(), false);
        name.remove();
        boss.remove();
        DungeonMobs.remove(body);
        body.playEffect(EntityEffect.ENTITY_DEATH);
        run.killed(killer.getUniqueId());
        // SkyBlock's drops for the killer, and the death event (Combat XP, the bestiary), as for his undeads.
        Mobs.kindDied(MobKinds.BONZO, DungeonFloor.FLOOR_1, at, killer);
        Mannequin corpse = body;
        run.later(20, corpse::remove);
        run.later(SUMMARY_AFTER, run::end);
        run.later(MASTERS_AFTER, () -> sayAt(at, "But my masters are a lot stronger.."));
        run.later(WAIT_AFTER, () -> sayAt(at, "Just you wait..."));
        run.later(PILLAR_AFTER, () -> sink(PILLAR_BLOCKS));
        run.later(TUNNEL_AFTER, () -> sink(TUNNEL_BLOCKS));
    }

    /** A line over where he fell, for 3.1 s like any other (the run ticks him after its end). */
    private void sayAt(Location at, String line) {
        lastLine = age;
        run.tell("&c[BOSS] Bonzo&f: " + line);
        if (speech != null) speech.remove();
        speech = stand(at.clone().add(0, DOWN_SPEECH_ABOVE, 0), "&l" + line);
        speechUntil = age + SPEECH_LASTS;
    }

    /** Blocks that go down into the floor, as the run's doors do. */
    private void sink(int[] box) {
        List<int[]> blocks = new ArrayList<>();
        List<BlockData> looks = new ArrayList<>();
        for (int x = box[0]; x <= box[3]; x++) {
            for (int y = box[1]; y <= box[4]; y++) {
                for (int z = box[2]; z <= box[5]; z++) {
                    BlockData look = world.getBlockAt(x, y, z).getBlockData();
                    if (look.getMaterial().isAir()) continue;
                    blocks.add(new int[]{x, y, z});
                    looks.add(look);
                }
            }
        }
        if (!blocks.isEmpty()) DoorAnimation.play(run.plugin(), world, blocks, looks, run.players(), DoorAnimation.DOOR);
    }

    /** Takes everything of his away (the run's over). */
    void dispose() {
        DungeonMobs.remove(body);
        body.remove();
        name.remove();
        boss.remove();
        if (speech != null) speech.remove();
        bar.removeAll();
        for (BonzoUndead undead : undeads) undead.remove();
        undeads.clear();
        for (Balloon balloon : List.copyOf(balloons)) balloon.stand.remove();
        balloons.clear();
        for (WitherSkull skull : skulls.keySet()) {
            SKULLS.remove(skull.getUniqueId());
            skull.remove();
        }
        skulls.clear();
    }
}
