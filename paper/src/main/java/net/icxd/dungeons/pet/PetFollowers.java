package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.ability.utility.UtilityListener;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * Summoned pets in the world, as recorded (2026-10-25, a Bee summoned at 02:31.8): an invisible marker armor stand
 * with arms holding the pet's head in its right hand (the arm at -45°, -135°, 0°), and over it another with the name,
 * "&8[&7Lv1&8] &aBee" (0.4 behind it, 0.06 to its left and 1.45 up: over the head). Others see "Bea's Bee" (recorded,
 * Bea's pet in the Hub): a second name stand, shown to them and not to its owner. Each viewer's Pets menu settings say
 * which they see (see {@link Pets.Visibility}, and the name tags' toggle).
 *
 * <p>Each tick the pet goes after its owner, faster the farther it is (recorded: about 0.47 x distance^0.575 blocks every
 * 3 ticks), stopping short of them, facing them, 0.75 over their feet; standing near them it bobs 0.55 up and down
 * every 3.9 seconds (recorded). A pet that falls behind by much, or is left in another world, comes to them. Every
 * half second each online player's summoned pet is checked against their profile (summoned, changed, levelled,
 * gone), which also covers joins, profile switches and yoinks. None are saved with the world. Main thread.
 */
public final class PetFollowers {
    /**
     * The name stand's place by the holder, turned as it faces: its left, back and up. Recorded: at the summon, 8.1
     * 73.2805 -51.56 by a holder at 8.5 71.8305 -51.5 (sent facing +z, but the name was put as for one facing +x); then
     * 0.37 to 0.43 back and 0.03 to 0.08 left at each of the 26 turns it made following.
     */
    static final double NAME_LEFT = 0.06;
    static final double NAME_BACK = 0.4;
    static final double NAME_UP = 1.45;
    /**
     * Where the head it holds shows, by the holder as the name is: its left, back and up, as vanilla's models draw a
     * player head in a right hand posed so (a quarter block wide: held items are half size; 1.8's draw it at 0.14, 0.36
     * and 1.16). How near a look has to pass it to be at it: more than that, as it's small and bobs (UNKNOWN: Hypixel's
     * stand can't be clicked either; see {@link #lookingAtOwn}).
     */
    static final double HEAD_LEFT = 0.2;
    static final double HEAD_BACK = 0.425;
    static final double HEAD_UP = 1.03;
    static final double AIM = 0.45;
    /** Over the owner's feet, and how far it bobs (recorded: 71.17 to 72.30 over a player at 71). */
    static final double HEIGHT = 0.75;
    static final double BOB = 0.55;
    static final double BOB_TICKS = 78;
    /** How close it comes, and how fast (blocks a tick: a third of the recorded 3 ticks' 0.47 x distance^0.575). */
    static final double NEAR = 1.5;
    static final double SPEED = 0.157;
    static final double SPEED_POWER = 0.575;
    /** How fast it rises and falls to its height: a third of the recorded 0.156 every 3 ticks. */
    static final double CLIMB = 0.052;
    /** Farther than this, it comes to its owner at once. */
    static final double TOO_FAR = 24;
    static final int CHECK_TICKS = 10;

    private static final Map<UUID, Follower> FOLLOWING = new HashMap<>();
    private static int ticks;

    private PetFollowers() {
    }

    /** A summoned pet: whose, which (and as what, for its head and name), its stands and where it bobs. */
    private static final class Follower {
        final UUID owner;
        String pet;
        String shown;
        final ArmorStand holder;
        final ArmorStand ownName;
        final ArmorStand othersName;
        int still;

        Follower(UUID owner, String pet, String shown, ArmorStand holder, ArmorStand ownName, ArmorStand othersName) {
            this.owner = owner;
            this.pet = pet;
            this.shown = shown;
            this.holder = holder;
            this.ownName = ownName;
            this.othersName = othersName;
        }

        List<Entity> entities() {
            return List.of(holder, ownName, othersName);
        }

        void remove() {
            for (Entity e : entities()) e.remove();
        }
    }

    /** Starts the ticking. Once, when the plugin starts. */
    public static void start(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, PetFollowers::tick, 1, 1);
    }

    /** Checks every online player's pet against their profile now (the data came in). */
    public static void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
    }

    /** Their summoned pet as their profile has it now: spawned, changed, renamed or gone. */
    public static void refresh(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        PetData data = Pets.data();
        OwnedPet active = user == null || user.isReleased() || !player.isOnline() || player.isDead() ? null : Pets.active(user.profile());
        Pet kind = active == null ? null : data.pet(active.type());
        Follower follower = FOLLOWING.get(player.getUniqueId());
        if (kind == null) {
            if (follower != null) despawn(player.getUniqueId());
            return;
        }
        String shown = active.type() + ":" + Pets.tier(active, data) + ":" + Pets.level(active, data) + ":" + active.heldItem()
                + ":" + active.skin();
        if (follower != null && (!follower.holder.isValid() || !follower.pet.equals(active.uuid()))) {
            despawn(player.getUniqueId());
            follower = null;
        }
        if (follower == null) {
            FOLLOWING.put(player.getUniqueId(), spawn(player, active, kind, data, shown));
            showTo(Bukkit.getOnlinePlayers());
            return;
        }
        if (!shown.equals(follower.shown)) {
            follower.shown = shown;
            follower.holder.getEquipment().setItemInMainHand(head(active, kind, data));
            follower.ownName.customName(Text.line(PetText.floating(active, data, null)));
            follower.othersName.customName(Text.line(PetText.floating(active, data, player.getName())));
        }
    }

    /** Takes their pet out of the world (despawned, or they left). */
    public static void despawn(UUID owner) {
        Follower follower = FOLLOWING.remove(owner);
        if (follower != null) follower.remove();
    }

    /** When the plugin stops. */
    public static void removeAll() {
        for (Follower follower : List.copyOf(FOLLOWING.values())) follower.remove();
        FOLLOWING.clear();
    }

    /** Whether this entity is one of the summoned pets' stands. */
    public static boolean isPet(Entity entity) {
        for (Follower follower : FOLLOWING.values()) if (follower.entities().contains(entity)) return true;
        return false;
    }

    private static Follower spawn(Player owner, OwnedPet pet, Pet kind, PetData data, String shown) {
        Location at = holderSpot(owner.getLocation(), 0);
        ItemStack head = head(pet, kind, data);
        ArmorStand holder = at.getWorld().spawn(at, ArmorStand.class, stand -> {
            prop(stand);
            stand.setArms(true);
            stand.setRightArmPose(new EulerAngle(Math.toRadians(-45), Math.toRadians(-135), 0));
            stand.getEquipment().setItemInMainHand(head);
        });
        ArmorStand ownName = name(nameSpot(at), PetText.floating(pet, data, null));
        ArmorStand othersName = name(nameSpot(at), PetText.floating(pet, data, owner.getName()));
        return new Follower(owner.getUniqueId(), pet.uuid(), shown, holder, ownName, othersName);
    }

    /** Its head: its skin's, else the rarity it's as's (a Tier Boost's, see Pets#tier). */
    private static ItemStack head(OwnedPet pet, Pet kind, PetData data) {
        String texture = Pets.texture(pet, data);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (texture != null) Utils.skull(head, Utils.texture(texture));
        return head;
    }

    private static ArmorStand name(Location at, String name) {
        return at.getWorld().spawn(at, ArmorStand.class, stand -> {
            prop(stand);
            stand.customName(Text.line(name));
            stand.setCustomNameVisible(true);
        });
    }

    /** What both stands are, as recorded: invisible, silent markers; and here, not saved and never a mob. */
    private static void prop(ArmorStand stand) {
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setSilent(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setPersistent(false);
        stand.addScoreboardTag(UtilityListener.NOT_A_MOB);
    }

    /** Where the holder is over a spot, bobbing this far. */
    static Location holderSpot(Location feet, double bob) {
        Location at = feet.clone().add(0, HEIGHT + bob, 0);
        at.setPitch(0);
        return at;
    }

    /**
     * Whether they're looking at their own summoned pet's head, no farther than they reach entities: to give it a Pet Item
     * ("Right-click on your summoned pet to give it this item!"), as a marker stand can't be clicked.
     */
    public static boolean lookingAtOwn(Player player) {
        Follower follower = FOLLOWING.get(player.getUniqueId());
        if (follower == null || !follower.holder.isValid() || !follower.holder.getWorld().equals(player.getWorld())) return false;
        AttributeInstance range = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        return aimedAt(player.getEyeLocation(), headSpot(follower.holder.getLocation()), range == null ? 3 : range.getValue());
    }

    /** Whether a look from {@code eye} passes within {@link #AIM} of {@code head}, ahead of it and within {@code reach}. */
    static boolean aimedAt(Location eye, Location head, double reach) {
        Vector look = eye.getDirection();
        Vector to = head.toVector().subtract(eye.toVector());
        double along = to.dot(look);
        if (along <= 0 || along > reach + AIM) return false;
        return to.subtract(look.multiply(along)).lengthSquared() <= AIM * AIM;
    }

    /** Where its name stand is: behind the holder, a little to its left, up. */
    static Location nameSpot(Location holder) {
        Location at = by(holder, NAME_LEFT, NAME_BACK, NAME_UP);
        at.setYaw(0);
        at.setPitch(0);
        return at;
    }

    /** Where the head it holds shows: behind the holder, a little to its left, up. */
    static Location headSpot(Location holder) {
        return by(holder, HEAD_LEFT, HEAD_BACK, HEAD_UP);
    }

    /** A spot this far to the holder's left, back and up, turned as it faces (at yaw 0, facing +z: +x, -z). */
    private static Location by(Location holder, double left, double back, double up) {
        double yaw = Math.toRadians(holder.getYaw());
        double leftX = Math.cos(yaw), leftZ = Math.sin(yaw);
        double backX = Math.sin(yaw), backZ = -Math.cos(yaw);
        return holder.clone().add(left * leftX + back * backX, up, left * leftZ + back * backZ);
    }

    /** How far it moves this tick towards an owner {@code distance} away: none once it's near. */
    static double step(double distance) {
        if (distance <= NEAR) return 0;
        return Math.min(distance - NEAR, SPEED * Math.pow(distance, SPEED_POWER));
    }

    private static void tick() {
        ticks++;
        if (ticks % CHECK_TICKS == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
            for (UUID owner : List.copyOf(FOLLOWING.keySet())) if (Bukkit.getPlayer(owner) == null) despawn(owner);
            showTo(Bukkit.getOnlinePlayers());
        }
        List<Player> moved = new ArrayList<>();
        for (Follower follower : FOLLOWING.values()) {
            Player owner = Bukkit.getPlayer(follower.owner);
            if (owner == null || !follower.holder.isValid()) continue;
            // Left in another world: made again by them, not dragged across.
            if (!Objects.equals(follower.holder.getWorld(), owner.getWorld())) moved.add(owner);
            else follow(follower, owner);
        }
        for (Player owner : moved) {
            despawn(owner.getUniqueId());
            refresh(owner);
        }
    }

    private static void follow(Follower follower, Player owner) {
        Location now = follower.holder.getLocation();
        Location feet = owner.getLocation();
        World world = feet.getWorld();
        double dx = feet.getX() - now.getX();
        double dz = feet.getZ() - now.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        Location next;
        if (distance > TOO_FAR || Math.abs(feet.getY() - now.getY()) > TOO_FAR) {
            next = holderSpot(feet, 0);
            follower.still = 0;
        } else {
            double step = step(distance);
            follower.still = step > 0 ? 0 : follower.still + 1;
            // Down first, as recorded.
            double bob = follower.still > 0 ? -BOB * Math.sin(2 * Math.PI * follower.still / BOB_TICKS) : 0;
            double wantY = feet.getY() + HEIGHT + bob;
            double y = now.getY() + Math.max(-CLIMB, Math.min(CLIMB, wantY - now.getY()));
            // Far below or above (a fall, a jump pad), it keeps up.
            if (Math.abs(wantY - y) > 2) y = wantY;
            next = new Location(world, now.getX() + (distance > 0 ? dx / distance * step : 0), y, now.getZ() + (distance > 0 ? dz / distance * step : 0));
        }
        if (distance > 0.05) next.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        else next.setYaw(now.getYaw());
        next.setPitch(0);
        follower.holder.teleport(next);
        Location name = nameSpot(next);
        follower.ownName.teleport(name);
        follower.othersName.teleport(name);
    }

    /** Each viewer sees the pets their settings say: every one's, none, or only their own; names only if they're on. */
    private static void showTo(Iterable<? extends Player> viewers) {
        Plugin plugin = Dungeons.getInstance();
        if (plugin == null) return;
        for (Player viewer : viewers) {
            User user = User.ifLoaded(viewer.getUniqueId());
            Document profile = user == null || user.isReleased() ? null : user.profile();
            Pets.Visibility visibility = profile == null ? Pets.Visibility.SHOW_ALL : Pets.visibility(profile);
            boolean names = profile == null || Pets.nametags(profile);
            for (Follower follower : FOLLOWING.values()) {
                boolean own = follower.owner.equals(viewer.getUniqueId());
                boolean pet = visibility == Pets.Visibility.SHOW_ALL || (visibility == Pets.Visibility.ONLY_YOURS && own);
                see(viewer, plugin, follower.holder, pet);
                see(viewer, plugin, follower.ownName, pet && names && own);
                see(viewer, plugin, follower.othersName, pet && names && !own);
            }
        }
    }

    /** Settings changed: what this viewer sees, now. */
    public static void settingsChanged(Player viewer) {
        showTo(List.of(viewer));
    }

    private static void see(Player viewer, Plugin plugin, Entity entity, boolean visible) {
        if (visible && !viewer.canSee(entity)) viewer.showEntity(plugin, entity);
        else if (!visible && viewer.canSee(entity)) viewer.hideEntity(plugin, entity);
    }
}
