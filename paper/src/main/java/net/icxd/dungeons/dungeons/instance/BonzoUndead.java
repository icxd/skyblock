package net.icxd.dungeons.dungeons.instance;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.MobKinds;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.goals.AvatarControl;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * One of Bonzo's summons: a player in one of three skins with nothing in its hands (as recorded), "༕ Undead 50,000❤"
 * over it in grey, that runs at the nearest player in the boss room and hits them once a second
 * ({@link MobKinds#BONZO_UNDEAD}: the wiki's 1,440). It drops where it's summoned, from the middle of the ring or
 * the top of a pillar.
 */
final class BonzoUndead implements DungeonMobs.Mob {
    private static final List<String> SKINS = List.of("Bonzo's Undead 1", "Bonzo's Undead 2", "Bonzo's Undead 3");
    /** About a sprinting player's, as the Watcher's undeads. */
    private static final double RUN = 0.28;
    private static final double REACH = 2.8;
    private static final int SWING_EVERY = 20;
    private static final int STAGGER = 8;
    /** The name tag's height above its feet (recorded: 2.13). */
    private static final double TAG_ABOVE = 2.13;

    private final Bonzo bonzo;
    final Mannequin body;
    private final ArmorStand tag;
    private final MobKind.Variant stats;
    private double health;
    private int age;
    private int nextSwing;
    private int staggeredUntil;
    private boolean dead;

    BonzoUndead(Bonzo bonzo, Location at) {
        this.bonzo = bonzo;
        this.stats = MobKinds.BONZO_UNDEAD.variant(DungeonFloor.FLOOR_1, null);
        this.health = stats.health();
        String skin = SKINS.get(ThreadLocalRandom.current().nextInt(SKINS.size()));
        this.body = at.getWorld().spawn(at, Mannequin.class, m -> {
            m.setProfile(DungeonTextures.profile(skin));
            m.setDescription(null);
            m.setPersistent(false);
            m.setSilent(true);
            m.setRemoveWhenFarAway(false);
            // Its health is ours; the entity just needs to survive the hits.
            var max = m.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) max.setBaseValue(1024);
            m.setHealth(1024);
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

    boolean isDead() {
        return dead;
    }

    /** "&2༕ &7Undead &a50,000&c❤", as recorded (grey, where the Watcher's undeads are gold), yellow at 0. */
    static String tagText(double health) {
        String number = (health > 0 ? "&a" : "&e") + Utils.getFormattedNumber((int) Math.ceil(Math.max(0, health)));
        return "&" + MobType.UNDEAD.color() + MobType.UNDEAD.symbol() + " &7Undead " + number + "&c❤";
    }

    private void updateTag() {
        tag.customName(Text.line(tagText(health)));
    }

    void tick() {
        if (dead) return;
        age++;
        if (!body.isValid()) {
            die(null);
            return;
        }
        Player target = bonzo.nearest(body.getLocation());
        if (target != null && age >= staggeredUntil) {
            Location them = target.getLocation();
            AvatarControl.face(body, them);
            double distance = body.getLocation().distance(them);
            if (distance > 1.2) AvatarControl.steer(body, them, RUN);
            if (distance <= REACH && age >= nextSwing) {
                nextSwing = age + SWING_EVERY;
                body.swingMainHand();
                DungeonMobs.hit(target, stats.damage(), body);
            }
        }
        tag.teleport(body.getLocation().add(0, TAG_ABOVE, 0));
    }

    @Override
    public double health() {
        return health;
    }

    @Override
    public double maxHealth() {
        return stats.health();
    }

    @Override
    public double magicResistance() {
        return MobKinds.BONZO_UNDEAD.magicResistance();
    }

    @Override
    public Set<MobType> types() {
        return Set.copyOf(MobKinds.BONZO_UNDEAD.types());
    }

    @Override
    public void hurt(Player by, double amount) {
        if (dead) return;
        health -= amount;
        bonzo.run().damageDealt(by.getUniqueId(), amount);
        updateTag();
        if (health <= 0) {
            die(by);
            return;
        }
        staggeredUntil = age + STAGGER;
    }

    private void die(Player killer) {
        dead = true;
        DungeonMobs.remove(body);
        if (killer != null) bonzo.run().killed(killer.getUniqueId());
        // SkyBlock's drops for the killer, and the death event (Combat XP), as for the Watcher's undeads.
        Mobs.kindDied(MobKinds.BONZO_UNDEAD, DungeonFloor.FLOOR_1, body.getLocation(), killer);
        if (body.isValid()) body.setHealth(0);
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), tag::remove, 20);
    }

    void remove() {
        dead = true;
        DungeonMobs.remove(body);
        body.remove();
        tag.remove();
    }
}
