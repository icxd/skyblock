package net.icxd.dungeons.hex;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * The Hex (see HEX.md): Hypixel's place to upgrade an item every way there is, here opened with {@code /hex} (its
 * Hexatorum and The Handler aren't here). Its main menu is {@link HexMenu}; its categories' pages plug in through
 * {@link HexCategory}; the item's life in it is {@link HexSession}'s; what things cost, {@link HexCosts}. Main thread.
 */
public final class Hex {
    /** UNKNOWN: Hypixel's words (the wiki's Catacombs: the Hex can't be used there, even with a Booster Cookie). */
    static final String NOT_IN_DUNGEONS = "&cYou can't use The Hex in a dungeon!";
    /** UNKNOWN: Hypixel's words. Not said for now: nobody is without the Museum or the Cookie Buff (see HexRequirements). */
    static final String NOT_ALLOWED = "&cYou need a Booster Cookie and the Prosperous Museum rank to use The Hex!";

    private Hex() {
    }

    /** Once, at startup: the item in the Hex is saved with their data, and the categories' tables are read. */
    public static void start(JavaPlugin plugin) {
        StoredInventory.captureWith((player, profile) -> {
            HexSession session = HexSession.of(player);
            if (session != null) session.save(profile);
        });
        HexData.start(plugin);
    }

    /** {@code /hex}: the main menu, empty; refused in a dungeon, and until their data is here. */
    public static void open(Player player) {
        if (Dungeons.getSkyBlockServer().getServerType() == ServerType.DUNGEONS) {
            player.sendMessage(Text.line(NOT_IN_DUNGEONS));
            return;
        }
        if (User.ifLoaded(player.getUniqueId()) == null || InventorySyncListener.frozen(player) || player.isDead()) return;
        if (!HexRequirements.mayOpen(player)) {
            player.sendMessage(Text.line(NOT_ALLOWED));
            return;
        }
        HexSession.open(player, HexMenu::new);
    }
}
