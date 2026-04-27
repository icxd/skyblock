package net.icxd.dungeons.mining;

import net.icxd.dungeons.mining.blocks.MithrilBlock;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class BlockRegistry {
    public static final HashMap<ItemStack, MinableBlock> MINABLE_BLOCKS = new HashMap<>() {{
        put(new ItemStack(Material.WOOL, 1, (short) 7), new MithrilBlock.GrayMithrilBlock());
    }};
    public static MinableBlock getMinableBlock(ItemStack stack) { return MINABLE_BLOCKS.get(stack); }
}
