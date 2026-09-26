package net.icxd.dungeons.item.items.none;

import net.icxd.dungeons.attributes.Attribute;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Material;

import java.util.List;

/** One random attribute, for the attribute system this plugin keeps (Hypixel's moved off items in 2025). */
public class AttributeShard implements SkyBlockItem {
    @Override public String id() { return "ATTRIBUTE_SHARD"; }
    @Override public String name() { return "Attribute Shard"; }
    @Override public Material material() { return Material.PRISMARINE_SHARD; }
    @Override public List<String> lore() { return List.of("&7Combine with items in the &bAttribute Fusion", "&7menu to apply attributes."); }
    @Override public boolean unstackable() { return true; }

    @Override
    public NBTTagCompound nbt() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("attribute_1", Attribute.random(null, null).name());
        nbt.setInt("attribute_1_level", Utils.random(1, 2));
        return nbt;
    }
}
