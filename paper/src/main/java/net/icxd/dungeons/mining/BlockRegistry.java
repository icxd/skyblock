package net.icxd.dungeons.mining;

import net.icxd.dungeons.mining.blocks.MithrilBlock;
import org.bukkit.Material;

import java.util.HashMap;

public class BlockRegistry {
    public static final HashMap<Material, MinableBlock> MINABLE_BLOCKS = new HashMap<>() {{
        put(Material.GRAY_WOOL, new MithrilBlock.GrayMithrilBlock());
    }};
    public static MinableBlock getMinableBlock(Material material) { return MINABLE_BLOCKS.get(material); }
}
