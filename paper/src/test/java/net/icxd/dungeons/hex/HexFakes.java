package net.icxd.dungeons.hex;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Material;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/** Made-up items and categories, for the Hex's rules without a server or the item data. */
public final class HexFakes {
    private HexFakes() {
    }

    /** A SkyBlock item of this kind (a diamond sword to look at). */
    public static SkyBlockItem item(String id, String name, Rarity rarity, SpecificItemType type) {
        return new SkyBlockItem() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public Material material() {
                return Material.DIAMOND_SWORD;
            }

            @Override
            public Rarity rarity() {
                return rarity;
            }

            @Override
            public SpecificItemType specificItemType() {
                return type;
            }
        };
    }

    /** One in the Hex, with nothing done to it (its data just its id), held by nobody. */
    public static HexItem hexItem(SkyBlockItem item) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", item.id());
        return new HexItem(item, tag, null);
    }

    public static HexItem sword() {
        return hexItem(item("TEST_SWORD", "Test Sword", Rarity.LEGENDARY, SpecificItemType.SWORD));
    }

    /** A category for the items {@code applies} says, with this summary; its page does nothing. */
    public static HexCategory category(String name, int carpentry, Predicate<HexItem> applies, Function<HexItem, List<String>> summary) {
        return new HexCategory(name, carpentry, List.of("&7What " + name + " does.")) {
            @Override
            public Look look() {
                return new Look(Material.BOOK, null);
            }

            @Override
            public boolean applies(HexItem item) {
                return applies.test(item);
            }

            @Override
            public List<String> summary(HexItem item) {
                return summary.apply(item);
            }

            @Override
            public void open(HexSession session) {
            }
        };
    }
}
