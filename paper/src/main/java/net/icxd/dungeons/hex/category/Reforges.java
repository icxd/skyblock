package net.icxd.dungeons.hex.category;

import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.menu.Icon;

/**
 * Reforges: Reforge Stones and a random basic reforge, "The Hex ➜ Reforges" (the wiki's Weapon tab). No
 * requirement. Its button is the Luxurious Spool's head (from the item data). Stage 1: the button as the wiki has
 * it; LATER (the reforges part): which items, the summary ("  &7Reforge &c✖"), the page.
 */
public final class Reforges extends HexCategory {
    private static final List<String> DESCRIPTION = List.of("&7Apply &aReforges &7to your item", "&7with &aReforge Stones &7or by",
            "&7rolling a &brandom &7reforge.");

    public Reforges() {
        super("Reforges", 0, DESCRIPTION);
    }

    @Override
    public Look look() {
        return lookOf("LUXURIOUS_SPOOL", new Look(Material.PLAYER_HEAD, null));
    }

    @Override
    public boolean applies(HexItem item) {
        return false;
    }

    @Override
    public List<String> summary(HexItem item) {
        return List.of();
    }

    @Override
    public void open(HexSession session) {
        session.open(new Placeholder(session, "The Hex ➜ Reforges", new Icon(Material.ANVIL, "&aApply Reforges", DESCRIPTION)));
    }
}
