package net.icxd.dungeons.dungeons.chests;

import lombok.Getter;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;

@Getter
public abstract class RewardChest {
    private final ChestType chestType;
    private final ArrayList<ArmorStand> armorStands;
    public RewardChest(ChestType chestType) {
        this.chestType = chestType;
        this.armorStands = new ArrayList<>();
    }

    public abstract void spawn(Location location);

    public ArmorStand getArmorStand(Location location, boolean visible, boolean mini) {
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setBasePlate(false);
        armorStand.setArms(false);
        armorStand.setVisible(visible);
        armorStand.setCanPickupItems(false);
        armorStand.setGravity(false);
        armorStand.setSmall(mini);
        armorStand.setBodyPose(new EulerAngle(0, Math.toRadians(location.getYaw()),0));
        armorStands.add(armorStand);
        return armorStand;
    }

    public ArmorStand getArmorStand(Location location) {
        return getArmorStand(location, false, false);
    }

    public ArmorStand getArmorStand(Location location, boolean visible) {
        return getArmorStand(location, visible, false);
    }

    public ArmorStand setHead(ArmorStand armorStand, String url) {
        armorStand.setHelmet(Utils.setSkullItem(new ItemStack(Material.SKULL_ITEM, 1, (short) 3), url));
        return armorStand;
    }

    public ArmorStand setHead(ArmorStand armorStand, ItemStack itemStack) {
        armorStand.setHelmet(itemStack);
        return armorStand;
    }
}
