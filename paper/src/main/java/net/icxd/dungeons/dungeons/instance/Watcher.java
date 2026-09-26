package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.utils.Utils;
import net.icxd.dungeons.utils.Text;

/**
 * The Watcher's fight in the Blood Room, as recorded on Hypixel. When the Blood Door opens he
 * speaks, then summons his undeads: four close together, a pause, then five more. For each he flies
 * to one of the {@link DisplayCases}, hovers a moment, and sends its head spinning to the middle of
 * the room, where the undead appears and drops to the floor. He can't be hurt; hitting him gets
 * you zapped. Once every undead is dead he lets you pass, and (with no bosses yet) the run ends.
 *
 * <p>Times are in ticks after the Blood Door opened, from two recordings of Entrance runs.
 */
final class Watcher implements DungeonMobs.Mob {
    /** He floats this high over the blood room's floor, his name and speech this far over him. */
    private static final double ABOVE_FLOOR = 4;
    private static final double NAME_ABOVE = 1.96875;
    private static final double SPEECH_ABOVE = 2.375;
    /** A line stays over his head this long, and he says at most one this often (bar the last). */
    private static final int SPEECH_LASTS = 40;
    private static final int SPEECH_GAP = 40;
    /** Placeholder for the icon in his name (a resource pack glyph on Hypixel). */
    private static final String ICON = "✦";
    private static final String NAME = "&e﴾ &5" + ICON + " &c&lThe Watcher &e﴿";

    private static final int UNDEADS = 9;
    private static final int FIRST_WAVE = 4;
    private static final int[] INTRO_AT = {4, 66, 128, 210, 292};
    /**
     * When heads leave their cases. The first at 19.8 s, and the next three as soon as he's been to
     * their cases; the fifth 13.7 s after the fourth (so their undeads are 11.7 s apart, as in both
     * recordings); the rest 4.5 to 7 s apart, or 1 to 3 s once there's nobody left to fight.
     */
    private static final int FIRST_RELEASE = 396;
    private static final int WAVE_PAUSE = 274;
    private static final int LATER_GAP_MIN = 90;
    private static final int LATER_GAP_MAX = 140;
    private static final int CLEARED_GAP_MIN = 20;
    private static final int CLEARED_GAP_MAX = 60;
    /**
     * A head flies straight at the middle for a set time, turning 30 degrees a tick: the first
     * wave's at 0.2 blocks a tick for 78 ticks, the second's at 0.3 for 40 (so some fall a little
     * short or overshoot, as on Hypixel). Its undead appears where it ends, 1.78 higher (where the
     * head was on the armor stand).
     */
    private static final double FIRST_WAVE_SPEED = 0.2;
    private static final int FIRST_WAVE_FLIGHT = 78;
    private static final double SECOND_WAVE_SPEED = 0.3;
    private static final int SECOND_WAVE_FLIGHT = 40;
    private static final float HEAD_SPIN = 30;
    private static final double HEAD_TO_FEET = 1.78;
    /** His line comes this long before the undead appears. */
    private static final int LINE_BEFORE_SUMMON = 8;
    private static final int ENOUGH_AFTER = 14;
    private static final int PASS_AFTER = 20;
    /** "You may pass." to the end of the run (Hypixel's 4.5 to 5.5 seconds). */
    private static final int END_AFTER = 100;
    /** Flying to the cases: speed, and how long he hovers at one before its head leaves. He stops a block in front of it. */
    private static final double FLY = 0.7;
    private static final int HOVER = 12;
    private static final double HOVER_ABOVE_HEAD = 0.75;
    private static final int ZAP_LASTS = 20;

    private static final List<String> SUMMON_LINES = List.of("Go, fight!", "Go and live again!", "Let's see how you can handle this.",
            "Hmmm... this one!", "You'll do.", "This one looks like a fighter.", "Oops. Wasn't meant to revive that one.");
    private static final List<String> KILL_LINES = List.of("Not bad.", "I'm impressed.", "Very nice.", "That one was weak anyway.",
            "Aw, I liked that one.");
    private static final List<String> HIT_LINES = List.of("That tickles.", "I am not your enemy.", "Stop attacking me.",
            "Don't make me shoot you.", "Ouch, just kidding...");

    private final DungeonRun run;
    private final RunLayout layout;
    private final PlacedRoom room;
    private final DungeonFloor floor;
    private final List<String> intro;
    private final Location home;
    private final Zombie body;
    private final ArmorStand name;
    private ArmorStand speech;
    private int speechUntil;
    private final BossBar bar;
    private final DisplayCases cases;
    private final List<Undead> undeads = new ArrayList<>();
    private final List<Undead.Parasite> parasites = new ArrayList<>();
    private final List<Flight> flights = new ArrayList<>();
    private int age;
    private int lastLine = -SPEECH_GAP;
    /** Heads sent off, and undeads that have appeared. */
    private int released;
    private int summoned;
    private int killed;
    /** The case he's going to, the earliest tick its head may leave (-1: none planned), and until when he hovers there. */
    private DisplayCases.Case fetching;
    private int nextRelease = FIRST_RELEASE;
    private int hoverUntil = -1;
    private int lastWall = -1;
    private boolean won;

    /** A head on its way to the middle. */
    private static final class Flight {
        final DisplayCases.Case from;
        final ArmorStand head;
        final Vector step;
        int left;

        Flight(DisplayCases.Case from, ArmorStand head, Vector step, int left) {
            this.from = from;
            this.head = head;
            this.step = step;
            this.left = left;
        }
    }

    Watcher(DungeonRun run, RunLayout layout, PlacedRoom room, DisplayCases cases) {
        this.run = run;
        this.layout = layout;
        this.room = room;
        this.cases = cases;
        this.floor = run.floor;
        this.intro = intro(floor);
        Location center = layout.center(run.world, RunLayout.firstCell(room));
        this.home = center.add(0, ABOVE_FLOOR, 0);
        this.body = run.world.spawn(home, Zombie.class, z -> {
            z.setAI(false);
            z.setGravity(false);
            z.setSilent(true);
            z.setPersistent(false);
            z.setRemoveWhenFarAway(false);
            z.setInvisible(true);
            z.setAdult();
            z.setShouldBurnInDay(false);
            z.setCanPickupItems(false);
            z.getEquipment().clear();
            z.getEquipment().setHelmet(DungeonTextures.head("The Watcher"));
            z.getEquipment().setDropChance(EquipmentSlot.HEAD, 0);
        });
        this.name = stand(home.clone().add(0, NAME_ABOVE, 0), NAME);
        this.bar = Bukkit.createBossBar(Utils.color("&c&lThe Watcher"), BarColor.RED, BarStyle.SOLID);
        DungeonMobs.add(body, this);
    }

    /** His welcome for each floor (the wiki's quotes; the Entrance's as recorded). */
    private static List<String> intro(DungeonFloor floor) {
        return switch (floor.getNumber()) {
            case 0 -> List.of("Congratulations, you made it through the Entrance.", "I have been watching you closely...",
                    "Unfortunately, for now, this is the end of the road.", "I sense many dead spirits eager to return to the living..",
                    "I will bring some of them back. Watch this.");
            case 1 -> List.of("Ah, you've finally arrived.", "I have watching you closely since we last met.",
                    "I don't know if you are ready for what's behind this door.", "So I will decide if you are strong enough.");
            case 2 -> List.of("Ah, we meet again...", "I have done some experiments to develop new abilities for my Skulls.",
                    "Let's see how you handle this!");
            case 3 -> List.of("So you made it this far... interesting.", "You are much stronger than I was expecting.",
                    "Not to worry, I recently added a very fine piece to my collection!");
            case 4 -> List.of("You've managed to scratch and claw your way here, eh?", "Don't even think about trying to outwit me this time!",
                    "My Watchful Eyes are keeping their ...eyes... on you!");
            case 5 -> List.of("I'm starting to get tired of seeing you around here...", "This time I've imbued my minions with special properties!");
            case 6 -> List.of("Oh.. hello?", "You've arrived too early, I haven't even set up...", "Anyway, let's fight... I guess");
            default -> List.of("Things feel a little more roomy now, eh?", "I've knocked down those pillars to go for a more... open concept.",
                    "Plus I needed to give my new friends some space to roam...");
        };
    }

    private ArmorStand stand(Location at, String text) {
        return run.world.spawn(at, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setMarker(true);
            s.setPersistent(false);
            s.customName(Text.line(text));
            s.setCustomNameVisible(true);
        });
    }

    DungeonRun run() {
        return run;
    }

    // Every tick

    void tick() {
        age++;
        for (int i = 0; i < intro.size(); i++) {
            if (age == INTRO_AT[i]) say(intro.get(i));
        }
        if (fetching == null && nextRelease >= 0) {
            fetching = cases.take(lastWall);
            if (fetching == null) fetching = emptyCase();
        }
        fly();
        for (Flight flight : List.copyOf(flights)) flyHead(flight);
        if (speech != null && age >= speechUntil) {
            speech.remove();
            speech = null;
        }
        for (Undead undead : List.copyOf(undeads)) undead.tick();
        parasites.removeIf(p -> !p.body.isValid());
        if (age >= 40 && !won) showBar();
    }

    private void say(String line) {
        lastLine = age;
        run.tell("&c[BOSS] The Watcher&f: " + line);
        if (speech != null) speech.remove();
        speech = stand(body.getLocation().add(0, SPEECH_ABOVE, 0), "&f&l" + line);
        speechUntil = age + SPEECH_LASTS;
    }

    private static <T> T any(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private static int between(int min, int max) {
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }

    // Summoning

    /** Sends the head he's hovering at off to the middle, and plans the next. */
    private void release(DisplayCases.Case from) {
        boolean firstWave = released < FIRST_WAVE;
        ArmorStand head = from.spawnHead();
        Vector step = home.toVector().subtract(head.getLocation().toVector()).normalize()
                .multiply(firstWave ? FIRST_WAVE_SPEED : SECOND_WAVE_SPEED);
        flights.add(new Flight(from, head, step, firstWave ? FIRST_WAVE_FLIGHT : SECOND_WAVE_FLIGHT));
        released++;
        lastWall = from.wall;
        fetching = null;
        hoverUntil = -1;
        if (released >= UNDEADS) nextRelease = -1;
        else if (released < FIRST_WAVE) nextRelease = age;
        else if (released == FIRST_WAVE) nextRelease = age + WAVE_PAUSE;
        else nextRelease = age + between(LATER_GAP_MIN, LATER_GAP_MAX);
    }

    private void flyHead(Flight flight) {
        Location at = flight.head.getLocation().add(flight.step);
        at.setYaw((at.getYaw() + HEAD_SPIN) % 360);
        flight.head.teleport(at);
        flight.left--;
        if (flight.left == LINE_BEFORE_SUMMON && age - lastLine >= SPEECH_GAP) say(any(SUMMON_LINES));
        if (flight.left > 0) return;
        flights.remove(flight);
        flight.head.remove();
        summon(flight.from.type, at.clone().add(0, HEAD_TO_FEET, 0));
    }

    private void summon(UndeadType type, Location at) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location rest = home.clone().subtract(0, ABOVE_FLOOR, 0).add(random.nextDouble(-3, 3), 0, random.nextDouble(-3, 3));
        undeads.add(new Undead(this, type, floor, at, rest));
        summoned++;
        if (summoned >= UNDEADS) {
            run.later(ENOUGH_AFTER, () -> {
                if (undeads.stream().anyMatch(u -> !u.isDead()) && !won) say("That will be enough for now.");
            });
        }
    }

    /** With no heads left in the cases (a room without them), one comes from somewhere along a wall. */
    private DisplayCases.Case emptyCase() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int wall = random.nextInt(4);
        double along = random.nextDouble(-9, 9);
        double dx = wall == 0 ? 11 : wall == 1 ? -11 : along;
        double dz = wall == 2 ? 11 : wall == 3 ? -11 : along;
        Location at = home.clone().add(dx, random.nextDouble(-1.25, 6.75), dz);
        at.setDirection(home.toVector().subtract(at.toVector()).setY(0));
        return new DisplayCases.Case(at, wall, UndeadType.random());
    }

    void undeadDied(Undead undead) {
        undeads.remove(undead);
        killed++;
        bar.setProgress(Math.max(0, 1 - killed / (double) UNDEADS));
        if (killed >= UNDEADS) {
            if (age - lastLine >= SPEECH_GAP) say(any(KILL_LINES));
            run.later(PASS_AFTER, this::pass);
            return;
        }
        if (age - lastLine >= SPEECH_GAP) say(any(KILL_LINES));
        // The later ones come sooner once there's nobody left to fight.
        if (released > FIRST_WAVE && nextRelease >= 0 && undeads.isEmpty() && flights.isEmpty()) {
            nextRelease = Math.min(nextRelease, age + between(CLEARED_GAP_MIN, CLEARED_GAP_MAX));
        }
    }

    void addParasite(Undead.Parasite parasite) {
        parasites.add(parasite);
    }

    private void pass() {
        if (won) return;
        won = true;
        say("You have proven yourself. You may pass.");
        bar.removeAll();
        run.bloodRoomCleared();
        // With no bosses yet, that's the run done.
        run.later(END_AFTER, run::end);
    }

    // Flying

    /**
     * To the case he's fetching from, leaving in time to get there and hover before its head is
     * due to leave; otherwise back to the middle.
     */
    private void fly() {
        Location at = body.getLocation();
        Location target = home;
        if (fetching != null) {
            Location spot = hoverSpot(fetching);
            int travel = (int) Math.ceil(at.distance(spot) / FLY);
            if (age + travel + HOVER >= nextRelease) target = spot;
        }
        Vector step = target.toVector().subtract(at.toVector());
        double length = step.length();
        Location next = length <= FLY ? target.clone() : at.clone().add(step.multiply(FLY / length));
        if (fetching != null && target != home && length <= FLY) {
            if (hoverUntil < 0) hoverUntil = age + HOVER;
            if (age >= hoverUntil && age >= nextRelease) release(fetching);
        }
        Player look = nearestInRoom(next);
        if (look != null) {
            Vector to = look.getEyeLocation().toVector().subtract(next.toVector());
            next.setDirection(to);
        } else {
            next.setYaw(at.getYaw());
            next.setPitch(0);
        }
        if (next.distanceSquared(at) > 1e-6 || next.getYaw() != at.getYaw()) {
            body.teleport(next);
            name.teleport(next.clone().add(0, NAME_ABOVE, 0));
            if (speech != null) speech.teleport(next.clone().add(0, SPEECH_ABOVE, 0));
        }
    }

    /** A block in front of a case, his head level with the one in it. */
    private Location hoverSpot(DisplayCases.Case c) {
        Vector in = home.toVector().subtract(c.at.toVector()).setY(0);
        if (in.lengthSquared() > 0) in.normalize();
        return c.at.clone().add(in).add(0, HOVER_ABOVE_HEAD, 0);
    }

    // Who's in the fight

    private boolean fighting(Player player) {
        return !player.isDead() && player.getGameMode() != GameMode.SPECTATOR && player.getGameMode() != GameMode.CREATIVE
                && layout.inside(room, player.getLocation());
    }

    private Player nearestInRoom(Location from) {
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : run.players()) {
            if (!fighting(player)) continue;
            double distance = player.getLocation().distanceSquared(from);
            if (distance < bestDistance) {
                best = player;
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * Who an undead goes for: the nearest player in the Blood Room, and from Floor 2 players of its
     * class first. Nobody in the room, nobody to fight.
     */
    Player targetFor(Undead undead, DungeonClass prefer) {
        Location from = undead.body.getLocation();
        Player best = null;
        boolean bestPreferred = false;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : run.players()) {
            if (!fighting(player)) continue;
            boolean preferred = prefer != null && run.classOf(player.getUniqueId()) == prefer;
            double distance = player.getLocation().distanceSquared(from);
            if (best == null || (preferred && !bestPreferred) || (preferred == bestPreferred && distance < bestDistance)) {
                best = player;
                bestPreferred = preferred;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void showBar() {
        for (Player player : run.players()) {
            boolean inside = layout.inside(room, player.getLocation());
            if (inside && !bar.getPlayers().contains(player)) bar.addPlayer(player);
            else if (!inside && bar.getPlayers().contains(player)) bar.removePlayer(player);
        }
    }

    // Being hit

    @Override
    public boolean invulnerable() {
        return true;
    }

    /** He can't be hurt: he zaps whoever tries, with an elder guardian's beam. */
    @Override
    public void hurt(Player by, double damage) {
        zap(by);
        if (age - lastLine >= SPEECH_GAP) say(any(HIT_LINES));
    }

    private void zap(Player player) {
        Location eye = body.getLocation().add(0, 1.2, 0);
        ElderGuardian beam = run.world.spawn(eye, ElderGuardian.class, g -> {
            g.setAI(false);
            g.setGravity(false);
            g.setInvisible(true);
            g.setInvulnerable(true);
            g.setPersistent(false);
            g.setCollidable(false);
            // Small, so it doesn't get in the way of hits.
            var scale = g.getAttribute(Attribute.SCALE);
            if (scale != null) scale.setBaseValue(0.0625);
        });
        beam.setTarget(player);
        beam.setLaser(true);
        DungeonMobs.hit(player, UndeadType.zapDamage(floor), body);
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), beam::remove, ZAP_LASTS);
    }

    // Leaving

    void dispose() {
        DungeonMobs.remove(body);
        body.remove();
        name.remove();
        if (speech != null) speech.remove();
        bar.removeAll();
        for (Undead undead : undeads) undead.remove();
        undeads.clear();
        for (Undead.Parasite parasite : parasites) parasite.remove();
        parasites.clear();
        for (Flight flight : flights) flight.head.remove();
        flights.clear();
        if (fetching != null && fetching.head != null) fetching.head.remove();
    }
}
