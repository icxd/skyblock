package net.icxd.dungeons.item.behaviour;

import net.icxd.dungeons.attributes.Attribute;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.utils.Utils;

/** One random attribute, for the attribute system this plugin keeps (Hypixel's moved off items in 2025). */
final class AttributeShard implements ItemBehaviour {
    @Override
    public NBTTagCompound nbt(SkyBlockItem item) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("attribute_1", Attribute.random(null, null).name());
        nbt.setInt("attribute_1_level", Utils.random(1, 2));
        return nbt;
    }
}
