package net.icxd.dungeons.dungeons.chests;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import net.icxd.dungeons.utils.Utils;

/**
 * A reward chest built as recorded ({@link ChestModel}): its stands as Hypixel's are, invisible with their arms
 * showing, small or not, wearing their part and facing the chest's way.
 */
public final class RecordedChest extends RewardChest {
    private final List<ArmorStand> lid = new ArrayList<>();
    private Location anchor;

    public RecordedChest(ChestType type) {
        super(type);
        if (!ChestModel.recorded(type)) throw new IllegalArgumentException(type + " wasn't recorded");
    }

    /** Builds it with its anchor here (the middle of its block at the floor), facing this location's yaw. */
    @Override
    public void spawn(Location location) {
        anchor = location.clone();
        for (ChestModel.Part part : ChestModel.parts(getChestType())) {
            double[] xz = ChestModel.offset(part.across(), part.forward(), location.getYaw());
            Location at = new Location(location.getWorld(), location.getX() + xz[0], location.getY() + part.up(), location.getZ() + xz[1],
                    location.getYaw(), 0);
            ArmorStand stand = location.getWorld().spawn(at, ArmorStand.class, s -> {
                s.setInvisible(true);
                s.setArms(true);
                s.setSmall(part.small());
                s.setGravity(false);
                s.setSilent(true);
                s.setInvulnerable(true);
                s.setPersistent(false);
                s.setCanPickupItems(false);
                s.setHeadPose(new EulerAngle(Math.toRadians(part.headPitch()), 0, 0));
                s.getEquipment().setHelmet(item(part));
            });
            getArmorStands().add(stand);
            if (part.lid()) lid.add(stand);
        }
    }

    private static ItemStack item(ChestModel.Part part) {
        String hash = part.head();
        if (hash == null) return new ItemStack(Material.valueOf(part.item().toUpperCase(java.util.Locale.ROOT)));
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        Utils.skull(head, Utils.texture(hash));
        return head;
    }

    /** Where it was built: the middle of its block at the floor, facing its way. */
    public Location anchor() {
        return anchor == null ? null : anchor.clone();
    }

    /** Its lid's stands, while they're there. */
    public List<ArmorStand> lid() {
        return lid;
    }

    /** Takes its lid off (it's open). */
    public void removeLid() {
        for (ArmorStand stand : lid) {
            stand.remove();
            getArmorStands().remove(stand);
        }
        lid.clear();
    }

    /** Whether this is one of its stands. */
    public boolean isPart(Entity entity) {
        for (ArmorStand stand : getArmorStands()) if (stand.getUniqueId().equals(entity.getUniqueId())) return true;
        return false;
    }

    @Override
    public void remove() {
        super.remove();
        lid.clear();
    }
}
