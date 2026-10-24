package net.icxd.dungeons.hub;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.npc.Npc;
import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.shop.Shop;
import net.icxd.dungeons.shop.ShopMenu;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * The Farm Merchant, in the Hub's Farm (from npcs/hub.json), as recorded (2026-10-24): the first time,
 * his two lines, high-voiced, and his shop ({@link Shop#FARM_MERCHANT}) opens 2.1 s after the last,
 * as Ophelia's does; after that a click opens it. He speaks as it opens. Main thread.
 */
final class FarmMerchant {
    static final String ID = "FARM_MERCHANT";
    static final Voice VOICE = Voice.villager(2);
    static final List<String> LINES = List.of(
            "&e[NPC] Farm Merchant&f: &r&fYou can buy and sell harvested crops with me!",
            "&e[NPC] Farm Merchant&f: &r&fWheat, carrots, potatoes, and melon are my specialities!");
    /** Ticks before each line (recorded at 06:03.1 and 06:04.5), and before his shop opens (06:06.6). */
    static final long[] LINE_TICKS = {0, 28};
    static final long SHOP_TICKS = 70;

    private static final Set<UUID> TALKING = new HashSet<>();

    private FarmMerchant() {
    }

    static void click(Plugin plugin, Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || TALKING.contains(player.getUniqueId())) return;
        if (!Npc.firstTalk(user.profile(), ID)) {
            openShop(player);
            return;
        }
        UUID id = player.getUniqueId();
        TALKING.add(id);
        for (int i = 0; i < LINES.size(); i++) {
            String line = LINES.get(i);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                player.sendMessage(Text.line(line));
                VOICE.speak(player);
            }, LINE_TICKS[i]);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TALKING.remove(id);
            if (player.isOnline()) openShop(player);
        }, SHOP_TICKS);
    }

    private static void openShop(Player player) {
        new ShopMenu(Shop.FARM_MERCHANT, player).open(player);
        VOICE.speak(player);
    }
}
