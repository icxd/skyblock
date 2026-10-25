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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.ability.utility.UtilityListener;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.pet.PetData.Variant;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * Summoned pets in the world: a small invisible marker armor stand wearing the pet's head, and just over it another
 * with the name, "&8[&7Lv1&8] &aBee". Hypixel's (recorded 2026-10-25, a Bee summoned at 02:31.8) is a full-size stand
 * holding the head in its right hand, with the name 1.4375 over it; a small one wearing it looks nicer. The name can't
 * be the small stand's own: a marker's shows half a block over its feet, under the head, and one that isn't a marker
 * has a hitbox, which would take the hits and arrows meant for mobs. So it's on a marker stand as high over it as the
 * small stand is tall, where its own name would be. Others see "Bea's Bee" (recorded, Bea's pet in the Hub): a second
 * name stand, shown to them and not to its owner. Each viewer's Pets menu settings say which they see (see
 * {@link Pets.Visibility}, and the name tags' toggle).
 *
 * <p>Each tick the pet goes after its owner, faster the farther it is (recorded: about 0.47 x distance^0.575 blocks every
 * 3 ticks), stopping short of them, facing them, 0.75 over their feet; standing near them it bobs 0.55 up and down
 * every 3.9 seconds (recorded). A pet that falls behind by much, or is left in another world, comes to them. Every
 * half second each online player's summoned pet is checked against their profile (summoned, changed, levelled,
 * gone), which also covers joins, profile switches and yoinks. None are saved with the world. Main thread.
 */
public final class PetFollowers {
    /** How far over the holder its name stand is: a small armor stand's height, where its own name would show. */
    static final double NAME_UP = 0.9875;
    /** Over the owner's feet, and how far it bobs (recorded: 71.17 to 72.30 over a player at 71). */
    static final double HEIGHT = 0.75;
    static final double BOB = 0.55;
    static final double BOB_TICKS = 78;
    /** How close it comes, and how fast (blocks a tick: a third of the recorded 3 ticks' 0.47 x distance^0.575). */
    static final double NEAR = 0.8;
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

    /** A summoned pet: whose, which (and as what level and rarity, for its name), its stands and where it bobs. */
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
        String shown = active.type() + ":" + active.tier() + ":" + Pets.level(active, data);
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
        Variant variant = kind.variant(pet.tier());
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (variant != null && variant.texture() != null) Utils.skull(head, Utils.texture(variant.texture()));
        ArmorStand holder = at.getWorld().spawn(at, ArmorStand.class, stand -> {
            prop(stand);
            stand.setSmall(true);
            stand.getEquipment().setHelmet(head);
        });
        ArmorStand ownName = name(nameSpot(at), PetText.floating(pet, data, null));
        ArmorStand othersName = name(nameSpot(at), PetText.floating(pet, data, owner.getName()));
        return new Follower(owner.getUniqueId(), pet.uuid(), shown, holder, ownName, othersName);
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

    /** Where its name stand is: right over the holder's head. */
    static Location nameSpot(Location holder) {
        Location at = holder.clone().add(0, NAME_UP, 0);
        at.setYaw(0);
        at.setPitch(0);
        return at;
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
