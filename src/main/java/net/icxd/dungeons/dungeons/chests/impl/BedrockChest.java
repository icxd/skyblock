package net.icxd.dungeons.dungeons.chests.impl;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dungeons.chests.ChestType;
import net.icxd.dungeons.dungeons.chests.RewardChest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.EulerAngle;

public class BedrockChest extends RewardChest {
    public BedrockChest() {
        super(ChestType.BEDROCK);
    }

    @Override
    public void spawn(Location location) {
        location = location.clone().add(0, -0.7, 0);

        ArmorStand b_tr = getArmorStand(location.clone().add(0.3, 0, -0.1), false, true);
        ArmorStand b_tm = getArmorStand(location.clone().add(0, 0, -0.1), false, true);
        ArmorStand b_tl = getArmorStand(location.clone().add(-0.3, 0, -0.1), false, true);
        ArmorStand b_br = getArmorStand(location.clone().add(0.05, 0.1, 0.25), false, true);
        ArmorStand b_bl = getArmorStand(location.clone().add(-0.05, 0.1, 0.25), false, true);
        ArmorStand b_m_h = getArmorStand(location.clone().add(0, -0.02, 0.48), false, true);

        ArmorStand m_tr = getArmorStand(location.clone().add(0.3, -0.25, 0.3));
        ArmorStand m_tl = getArmorStand(location.clone().add(-0.3, -0.25, 0.3));
        ArmorStand m_br = getArmorStand(location.clone().add(0.3, -0.25, -0.3));
        ArmorStand m_bl = getArmorStand(location.clone().add(-0.3, -0.25, -0.3));

        ArmorStand c_bbr = getArmorStand(location.clone().add(0.2 + Math.cos(Math.toRadians(location.getYaw() + 45)), 1, 0.2 + Math.sin(Math.toRadians(location.getYaw() + 45))), false, true);
        ArmorStand c_bbl = getArmorStand(location.clone().add(-0.2 - Math.cos(Math.toRadians(location.getYaw() + 45)), 1, -0.2 - Math.sin(Math.toRadians(location.getYaw() + 45))), false, true);
        ArmorStand c_fbr = getArmorStand(location.clone().add(0.2 + Math.sin(Math.toRadians(location.getYaw() + 45)), 1, -0.2 - Math.cos(Math.toRadians(location.getYaw() + 45))), false, true);
        ArmorStand c_fbl = getArmorStand(location.clone().add(-0.2 - Math.sin(Math.toRadians(location.getYaw() + 45)), 1, 0.2 + Math.cos(Math.toRadians(location.getYaw() + 45))), false, true);

        ArmorStand c_btr = getArmorStand(location.clone().add(0.2, 1.4, -0.2), false, true);
        ArmorStand c_btl = getArmorStand(location.clone().add(-0.2, 1.4, -0.2), false, true);
        ArmorStand c_ftr = getArmorStand(location.clone().add(0.2, 1.4, 0.2), false, true);
        ArmorStand c_ftl = getArmorStand(location.clone().add(-0.2, 1.4, 0.2), false, true);

        setHead(b_tr, new ItemStack(Material.BEDROCK));
        setHead(b_tm, new ItemStack(Material.BEDROCK));
        setHead(b_tl, new ItemStack(Material.BEDROCK));
        setHead(b_br, new ItemStack(Material.BEDROCK));
        setHead(b_bl, new ItemStack(Material.BEDROCK));
        setHead(b_m_h, "http://textures.minecraft.net/texture/3bcbbf94d603743a1e7147026e1c1240bd98fe87cc4ef04dcab51a31c30914fd");
        b_m_h.setHeadPose(new EulerAngle(-0.25, 0, 0));

        setHead(m_tr, new ItemStack(Material.STEP, 1, (short) 7));
        setHead(m_tl, new ItemStack(Material.STEP, 1, (short) 7));
        setHead(m_br, new ItemStack(Material.STEP, 1, (short) 7));
        setHead(m_bl, new ItemStack(Material.STEP, 1, (short) 7));

        setHead(c_bbr, "http://textures.minecraft.net/texture/aae235f295dc9dda432ce20f6693f79b1ad72014545c2e209bd32474021430d9");
        setHead(c_bbl, "http://textures.minecraft.net/texture/2668aa517ff9ad951fee4a99a0af1cd6117963ae9098977118da21cf21f51277");
        setHead(c_fbr, "http://textures.minecraft.net/texture/2668aa517ff9ad951fee4a99a0af1cd6117963ae9098977118da21cf21f51277");
        setHead(c_fbl, "http://textures.minecraft.net/texture/aae235f295dc9dda432ce20f6693f79b1ad72014545c2e209bd32474021430d9");

        setHead(c_btr, "http://textures.minecraft.net/texture/38db4e0404591c68863815d2e58ff8785232838ebe142e2dedcaa50da2c5885e");
        setHead(c_btl, "http://textures.minecraft.net/texture/74ac64633249f13f1b0c9525c6efa44cde96abcd647e095a171fe244c1a244e5");
        setHead(c_ftr, "http://textures.minecraft.net/texture/74ac64633249f13f1b0c9525c6efa44cde96abcd647e095a171fe244c1a244e5");
        setHead(c_ftl, "http://textures.minecraft.net/texture/38db4e0404591c68863815d2e58ff8785232838ebe142e2dedcaa50da2c5885e");

        c_bbr.setHeadPose(new EulerAngle(0, Math.PI, 0));
        c_bbl.setHeadPose(new EulerAngle(0, Math.PI, 0));
        c_btr.setHeadPose(new EulerAngle(0, Math.PI, 0));
        c_btl.setHeadPose(new EulerAngle(0, Math.PI, 0));

        getArmorStands().forEach(as -> as.setMetadata("rewardchest", new FixedMetadataValue(Dungeons.getInstance(), true)));
    }
}
