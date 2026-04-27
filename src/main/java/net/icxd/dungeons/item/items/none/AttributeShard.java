package net.icxd.dungeons.item.items.none;

import net.icxd.dungeons.attributes.Attribute;
import net.icxd.dungeons.item.*;
import net.icxd.dungeons.item.cost.*;
import net.icxd.dungeons.item.cost.coins.*;
import net.icxd.dungeons.item.cost.essence.*;
import net.icxd.dungeons.item.cost.item.*;
import net.icxd.dungeons.item.enums.*;
import net.icxd.dungeons.item.gemstone.*;
import net.icxd.dungeons.item.prestige.*;
import net.icxd.dungeons.item.requirement.*;
import net.icxd.dungeons.item.requirement.skill.*;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Utils;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Material;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AttributeShard implements SkyBlockItem {
    @Override public Material material() { return Material.PRISMARINE_SHARD; }
    @Override public String name() { return "Attribute Shard"; }
    @Override public String id() { return "ATTRIBUTE_SHARD"; }
    @Override public String description() { return "\n§7Combine with items in the §bAttribute Fusion §7menu to apply attributes."; }
    @Override public boolean unstackable() { return true; }

    @Override
    public NBTTagCompound nbt() {
        NBTTagCompound nbt = new NBTTagCompound();
        Attribute attribute1 = Attribute.getRandomAttribute();
        nbt.setString("attribute_1", attribute1.name());
        nbt.setInt("attribute_1_level", Utils.random(1, 2));
        return nbt;
    }
}
