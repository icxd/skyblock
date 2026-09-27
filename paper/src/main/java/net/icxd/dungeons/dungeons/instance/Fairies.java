package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * The Fairy Room's four fairies (recorded in RUN1's fairy room: research critic.md 3.1). Each is a small
 * invisible armor stand wearing a head, with its name on another 0.28 above it, "&8[&7Lv0&8] &dMari
 * &a4&c❤", floating about the room. Killing one (any hit, a ghost's too) revives the killer if it's a
 * ghost, else a dead teammate, else gives the killer a Revive Stone, with the fairy's line.
 */
final class Fairies {
    static final List<String> NAMES = List.of("Mari", "Nymira", "Zana", "Q'ara");
    /** Their heads' skin, as recorded. */
    private static final String SKIN = "2eedcffc6a11a3834a28849cc316af7a2752a376d536cf84039cf79108b167ae";
    private static final double LABEL_ABOVE = 0.28;
    /**
     * Where they float: in RUN1 between y 68.5 and 74.5, over a 6 by 6 area of the room. Which part of
     * the room depends on its rotation, which isn't known for that capture: around its middle here.
     */
    private static final double LOW = 68.5;
    private static final double HIGH = 74.5;
    private static final double SPREAD = 3;
    /** Blocks a tick (RUN1's name tags drift about 0.03 to 0.05 a tick; their path is UNKNOWN). */
    private static final double SPEED = 0.05;

    private static final class Fairy {
        final String name;
        final ArmorStand head;
        final ArmorStand label;
        Location goal;

        Fairy(String name, ArmorStand head, ArmorStand label) {
            this.name = name;
            this.head = head;
            this.label = label;
        }

        boolean is(Entity entity) {
            return head.equals(entity) || label.equals(entity);
        }

        void remove() {
            head.remove();
            label.remove();
        }
    }

    private final DungeonRun run;
    private final Location middle;
    private final List<Fairy> fairies = new ArrayList<>();

    private Fairies(DungeonRun run, Location middle) {
        this.run = run;
        this.middle = middle;
    }

    /** In the floor's fairy room, if it has one; null if not. */
    static Fairies spawn(DungeonRun run, RunLayout layout, World world) {
        List<PlacedRoom> rooms = layout.layout.roomsOfType(RoomType.FAIRY);
        if (rooms.isEmpty()) return null;
        Location middle = layout.center(world, RunLayout.firstCell(rooms.get(0)));
        Fairies out = new Fairies(run, middle);
        for (String name : NAMES) out.add(name);
        return out;
    }

    private void add(String name) {
        Location at = somewhere();
        ArmorStand head = at.getWorld().spawn(at, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setSmall(true);
            s.setInvulnerable(true);
            s.setPersistent(false);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            Utils.skull(skull, Utils.texture(SKIN));
            s.getEquipment().setHelmet(skull);
        });
        ArmorStand label = at.getWorld().spawn(at.clone().add(0, LABEL_ABOVE, 0), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setMarker(true);
            s.setInvulnerable(true);
            s.setPersistent(false);
            s.customName(Text.line("&8[&7Lv0&8] &d" + name + " &a4&c❤"));
            s.setCustomNameVisible(true);
        });
        fairies.add(new Fairy(name, head, label));
    }

    private Location somewhere() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location at = middle.clone().add(random.nextDouble(-SPREAD, SPREAD), 0, random.nextDouble(-SPREAD, SPREAD));
        at.setY(random.nextDouble(LOW, HIGH));
        return at;
    }

    boolean is(Entity entity) {
        for (Fairy fairy : fairies) if (fairy.is(entity)) return true;
        return false;
    }

    /** They drift from spot to spot. */
    void tick() {
        for (Fairy fairy : fairies) {
            if (!fairy.head.isValid()) continue;
            Location at = fairy.head.getLocation();
            if (fairy.goal == null || at.distanceSquared(fairy.goal) < SPEED * SPEED * 4) fairy.goal = somewhere();
            Vector step = fairy.goal.toVector().subtract(at.toVector());
            step.multiply(Math.min(1, SPEED / step.length()));
            Location next = at.clone().add(step);
            next.setYaw((float) Math.toDegrees(Math.atan2(-step.getX(), step.getZ())));
            fairy.head.teleport(next);
            fairy.label.teleport(next.clone().add(0, LABEL_ABOVE, 0));
        }
    }

    /**
     * A player hit a fairy, which kills it. A ghost is revived by it; else it revives the first teammate
     * still dead, else gives a Revive Stone. The lines go to the killer (UNKNOWN whether others see them);
     * "Have a great life!" after a revive is our guess at when it's said.
     */
    void killed(Entity entity, Player killer) {
        Fairy fairy = null;
        for (Fairy f : fairies) if (f.is(entity)) fairy = f;
        if (fairy == null) return;
        fairies.remove(fairy);
        Location at = fairy.head.getLocation();
        fairy.remove();
        Ghosts ghosts = run.ghosts();
        if (ghosts.isGhost(killer.getUniqueId())) {
            killer.sendMessage(Utils.color(DeathText.fairyRevivesYou(fairy.name)));
            ghosts.revive(killer, at);
            killer.sendMessage(Utils.color(DeathText.fairyGoodbye(fairy.name)));
            return;
        }
        for (UUID id : ghosts.all()) {
            Player dead = Bukkit.getPlayer(id);
            DungeonRun.Member member = run.member(id);
            if (dead == null || member == null || !dead.getWorld().equals(run.world)) continue;
            killer.sendMessage(Utils.color(DeathText.fairyRevivesFriend(fairy.name, member.rankColor + member.name)));
            ghosts.revive(dead, at);
            killer.sendMessage(Utils.color(DeathText.fairyGoodbye(fairy.name)));
            return;
        }
        killer.sendMessage(Utils.color(DeathText.fairyGivesStone(fairy.name)));
        ReviveStones.give(killer);
    }

    void dispose() {
        for (Fairy fairy : fairies) fairy.remove();
        fairies.clear();
    }
}
