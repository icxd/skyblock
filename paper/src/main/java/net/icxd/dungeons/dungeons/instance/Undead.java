package net.icxd.dungeons.dungeons.instance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.utils.Utils;
import net.icxd.dungeons.utils.Text;

/**
 * One of the Watcher's undeads: a player-shaped mob in random iron, chainmail, gold or leather
 * armor with an axe, as recorded. It runs at the nearest player in the blood room and hits them;
 * when there's nobody in the room it goes back to the Watcher.
 */
final class Undead implements DungeonMobs.Mob {
    /** Running speed in blocks a tick (about a sprinting player's), and walking speed back to the Watcher. */
    private static final double RUN = 0.28;
    private static final double WALK = 0.2;
    private static final double REACH = 2.8;
    private static final int SWING_EVERY = 20;
    private static final int STAGGER = 8;
    /** The name tag's height above its feet, as on Hypixel. */
    private static final double TAG_ABOVE = 2.1;
    /** Placeholder for Hypixel's undead icon (from its resource pack). */
    static final String ICON = "☠";

    private static final List<Material> CHESTPLATES = List.of(Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.LEATHER_CHESTPLATE);
    private static final List<Material> LEGGINGS = List.of(Material.IRON_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.AIR);
    private static final List<Material> BOOTS = List.of(Material.CHAINMAIL_BOOTS, Material.LEATHER_BOOTS, Material.AIR);
    private static final List<Material> WEAPONS = List.of(Material.GOLDEN_AXE, Material.IRON_AXE);

    final UndeadType type;
    private final Watcher watcher;
    private final DungeonFloor floor;
    final Mannequin body;
    private final ArmorStand tag;
    private final double maxHealth;
    private double health;
    private final double damage;
    private final Location home;
    private int nextSwing;
    /** Knocked back: it doesn't steer until then. */
    private int staggeredUntil;
    private int age;
    private boolean dead;

    Undead(Watcher watcher, UndeadType type, DungeonFloor floor, Location at, Location home) {
        this.watcher = watcher;
        this.type = type;
        this.floor = floor;
        this.home = home;
        this.maxHealth = type.rollHealth(floor);
        this.health = maxHealth;
        this.damage = type.damage(floor);
        this.body = at.getWorld().spawn(at, Mannequin.class, m -> {
            m.setProfile(DungeonTextures.profile(type.displayName));
            m.setDescription(null);
            m.setPersistent(false);
            m.setSilent(true);
            m.setRemoveWhenFarAway(false);
            // Its health is ours; the entity just needs to survive the hits.
            var max = m.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) max.setBaseValue(1024);
            m.setHealth(1024);
            dress(m.getEquipment());
        });
        this.tag = at.getWorld().spawn(at.clone().add(0, TAG_ABOVE, 0), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setMarker(true);
            s.setPersistent(false);
            s.setCustomNameVisible(true);
        });
        updateTag();
        DungeonMobs.add(body, this);
    }

    private static <T> T any(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private static void dress(EntityEquipment gear) {
        gear.setChestplate(new ItemStack(any(CHESTPLATES)));
        Material legs = any(LEGGINGS);
        if (legs != Material.AIR) gear.setLeggings(new ItemStack(legs));
        Material boots = any(BOOTS);
        if (boots != Material.AIR) gear.setBoots(new ItemStack(boots));
        gear.setItemInMainHand(new ItemStack(any(WEAPONS)));
    }

    boolean isDead() {
        return dead;
    }

    /** "☠ Leech 20,000❤": green down to half health, then yellow. */
    private void updateTag() {
        String number = (health >= maxHealth / 2 ? "&a" : "&e") + Utils.getFormattedNumber((int) Math.ceil(health));
        tag.customName(Text.line("&2" + ICON + " &6" + type.displayName + " " + number + "&c❤"));
    }

    void tick() {
        if (dead) return;
        age++;
        if (!body.isValid()) {
            die(null);
            return;
        }
        boolean perks = UndeadType.perksOn(floor);
        if (perks && type.perk == UndeadType.Perk.HEALS && age % 20 == 0 && health < maxHealth) {
            health = Math.min(maxHealth, health + maxHealth * 0.02);
            updateTag();
        }
        Player target = watcher.targetFor(this, perks ? type.targets : null);
        if (age < staggeredUntil) face(target != null ? target.getLocation() : home);
        else if (target != null) chase(target);
        else walkTo(home, WALK, 2);
        tag.teleport(body.getLocation().add(0, TAG_ABOVE, 0));
    }

    private void chase(Player target) {
        Location me = body.getLocation();
        Location them = target.getLocation();
        face(them);
        double distance = me.distance(them);
        if (distance > 1.2) move(them, RUN);
        if (distance <= REACH && age >= nextSwing) {
            nextSwing = age + SWING_EVERY;
            body.swingMainHand();
            DungeonMobs.hit(target, damage, body);
        }
    }

    private void walkTo(Location to, double speed, double close) {
        Location me = body.getLocation();
        double dx = to.getX() - me.getX();
        double dz = to.getZ() - me.getZ();
        if (dx * dx + dz * dz <= close * close) return;
        face(to);
        move(to, speed);
    }

    /** Steers towards a spot, jumping onto anything a block high in the way. */
    private void move(Location to, double speed) {
        Location me = body.getLocation();
        Vector direction = to.toVector().subtract(me.toVector()).setY(0);
        if (direction.lengthSquared() < 1e-6) return;
        direction.normalize();
        Vector velocity = body.getVelocity();
        double vy = velocity.getY();
        Block ahead = me.clone().add(direction.clone().multiply(0.8)).getBlock();
        if (body.isOnGround() && ahead.getType().isSolid() && !ahead.getRelative(0, 1, 0).getType().isSolid()) vy = 0.42;
        body.setVelocity(direction.multiply(speed).setY(vy));
    }

    private void face(Location at) {
        Location me = body.getLocation();
        double dx = at.getX() - me.getX();
        double dz = at.getZ() - me.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        body.setRotation(yaw, 0);
    }

    @Override
    public void hurt(Player by, double amount) {
        if (dead) return;
        health -= amount;
        watcher.run().damageDealt(by.getUniqueId(), amount);
        if (health <= 0) {
            health = 0;
            updateTag();
            die(by);
            return;
        }
        updateTag();
        staggeredUntil = age + STAGGER;
        if (UndeadType.perksOn(floor) && type.perk == UndeadType.Perk.TELEPORTS && ThreadLocalRandom.current().nextInt(3) == 0) {
            Location behind = by.getLocation().clone().subtract(by.getLocation().getDirection().setY(0).normalize().multiply(1.5));
            behind.setYaw(by.getLocation().getYaw());
            body.teleport(behind);
        }
    }

    private void die(Player killer) {
        dead = true;
        DungeonMobs.remove(body);
        if (killer != null) watcher.run().killed(killer.getUniqueId());
        if (UndeadType.perksOn(floor)) {
            if (type.perk == UndeadType.Perk.EXPLODES) explode();
            if (type.perk == UndeadType.Perk.SILVERFISH) for (int i = 0; i < 3; i++) watcher.addParasite(new Parasite(watcher, floor, body.getLocation()));
        }
        // Its drops are cleared (RunManager), as it's tagged.
        if (body.isValid()) body.setHealth(0);
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), tag::remove, 20);
        watcher.undeadDied(this);
    }

    private void explode() {
        Location at = body.getLocation();
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 1);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
        for (Player player : watcher.run().players()) {
            if (player.getLocation().distanceSquared(at) > 16) continue;
            var max = player.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) DungeonMobs.hit(player, max.getValue() * 0.01, body);
        }
    }

    void remove() {
        dead = true;
        DungeonMobs.remove(body);
        body.remove();
        tag.remove();
    }

    /** A Parasite's silverfish: vanilla silverfish movement, SkyBlock health and damage. */
    static final class Parasite implements DungeonMobs.Mob {
        private final Watcher watcher;
        final Silverfish body;
        private double health;
        private final double damage;

        Parasite(Watcher watcher, DungeonFloor floor, Location at) {
            this.watcher = watcher;
            this.health = UndeadType.parasiteHealth(floor);
            this.damage = UndeadType.parasiteDamage(floor);
            this.body = at.getWorld().spawn(at, Silverfish.class, s -> {
                s.setPersistent(false);
                s.setRemoveWhenFarAway(false);
                s.customName(Text.line("&6Parasite"));
                s.setCustomNameVisible(true);
            });
            DungeonMobs.add(body, this);
        }

        @Override
        public double attackDamage() {
            return damage;
        }

        @Override
        public void hurt(Player by, double amount) {
            health -= amount;
            watcher.run().damageDealt(by.getUniqueId(), amount);
            if (health > 0) return;
            DungeonMobs.remove(body);
            watcher.run().killed(by.getUniqueId());
            body.setHealth(0);
        }

        void remove() {
            DungeonMobs.remove(body);
            body.remove();
        }
    }
}
