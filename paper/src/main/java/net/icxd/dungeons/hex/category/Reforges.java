package net.icxd.dungeons.hex.category;

import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexData;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.hex.reforge.ReforgeLore;
import net.icxd.dungeons.hex.reforge.ReforgePage;
import net.icxd.dungeons.reforge.Reforge;
import net.icxd.dungeons.reforge.ReforgeTable;

/**
 * Reforges: Reforge Stones and a random basic reforge, "The Hex ➜ Reforges" (the wiki's Weapon tab; see
 * {@link ReforgePage}). No requirement (Hypixel's Booster Cookie, which is waived with the rest). Its button is the
 * Luxurious Spool's head (from the item data). It's for the items a basic pool or a stone is for (see
 * ReforgeTable#reforgeable); its summary says whether the item has a reforge, and which. Its table, the private
 * data's reforges.json, is the one every item's reforge is read from too (see REFORGES.md).
 */
public final class Reforges extends HexCategory {
    public static final List<String> DESCRIPTION = List.of("&7Apply &aReforges &7to your item", "&7with &aReforge Stones &7or by",
            "&7rolling a &brandom &7reforge.");

    public Reforges() {
        super("Reforges", 0, DESCRIPTION);
        HexData.add("reforges", (folder, problems) -> ReforgeTable.read(HexData.json(folder, ReforgeTable.FILE, problems), problems),
                ReforgeTable::set);
    }

    @Override
    public Look look() {
        return lookOf("LUXURIOUS_SPOOL", new Look(Material.PLAYER_HEAD, null));
    }

    /** A stone's look, as the item data has it (a Wither Blood head); paper if there's no such item. */
    public static Look stoneLook(String id) {
        return lookOf(id, new Look(Material.PAPER, null));
    }

    @Override
    public boolean applies(HexItem item) {
        return ReforgeTable.get().reforgeable(item.item());
    }

    @Override
    public List<String> summary(HexItem item) {
        return ReforgeLore.summary(Reforge.of(item.tag()));
    }

    @Override
    public void open(HexSession session) {
        session.open(new ReforgePage(session));
    }
}
