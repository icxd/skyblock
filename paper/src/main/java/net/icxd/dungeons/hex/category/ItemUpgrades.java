package net.icxd.dungeons.hex.category;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.hex.upgrade.UpgradesPage;
import net.icxd.dungeons.item.DungeonItems;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.upgrade.Stars;

/**
 * Item Upgrades: Star Upgrades and Convert to Dungeon Item, "The Hex ➜ Item Upgrades" ({@link UpgradesPage}; the
 * wiki's The Hex). Carpentry 25. For an item that takes stars or can be made a dungeon item. Its button is Dragon
 * Essence's head, as the wiki's table of categories pictures it (the item data's, else NEU's ESSENCE_DRAGON skin).
 * Its summary, as the official screenshot has it: "  &7Dungeon Item &a✔" and "  &7Upgrade Level &6✪✪✪✪✪" (the stars
 * as the item's name shows them). UNKNOWN (U1, U12): the button's words (ours, below); the Dungeon Item line on an
 * item that can't be made one (none, here), and the Upgrade Level line with no stars ("&c✖", as the other lines
 * without their upgrade).
 */
public final class ItemUpgrades extends HexCategory {
    /** NEU's ESSENCE_DRAGON head, for when the item data has no Dragon Essence (it doesn't, today). */
    static final String DRAGON_ESSENCE_HEAD = "33ff416aa8bec1665b92701fbe68a4effff3d06ed9147454fa77712dd6079b33";

    public ItemUpgrades() {
        // UNKNOWN: our own words.
        super("Item Upgrades", 25, List.of("&7Upgrade your item with &6Stars", "&7or convert it into a &cDungeon", "&citem&7!"));
    }

    @Override
    public Look look() {
        return lookOf("ESSENCE_DRAGON", new Look(Material.PLAYER_HEAD, DRAGON_ESSENCE_HEAD));
    }

    @Override
    public boolean applies(HexItem item) {
        return Stars.max(item.item()) > 0 || item.item().dungeonConversionCost() != null;
    }

    @Override
    public List<String> summary(HexItem item) {
        return summary(item.item(), item.tag());
    }

    static List<String> summary(SkyBlockItem item, NBTTagCompound tag) {
        List<String> lines = new ArrayList<>();
        boolean dungeon = DungeonItems.is(item, tag);
        if (dungeon || item.dungeonConversionCost() != null) lines.add("  &7Dungeon Item " + (dungeon ? "&a✔" : "&c✖"));
        if (Stars.max(item) > 0) {
            String stars = ItemBuilder.stars(item, tag);
            lines.add("  &7Upgrade Level " + (stars.isEmpty() ? "&c✖" : stars.substring(1)));
        }
        return lines;
    }

    @Override
    public void open(HexSession session) {
        session.open(new UpgradesPage(session, description()));
    }
}
