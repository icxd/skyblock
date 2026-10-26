package net.icxd.dungeons.hub;

import org.bukkit.entity.Player;

import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.shop.Shop;
import net.icxd.dungeons.shop.ShopMenu;

/**
 * Bea, at the Hub's Pet Care (from npcs/hub.json): a click opens her shop ({@link Shop#BEA}) as she speaks, a villager's
 * yes or hum, high (recorded, 2026-10-25: 00:32.6 and 00:55.1; what she says the first time, UNKNOWN: nothing). Main
 * thread.
 */
final class Bea {
    static final String ID = "BEA";
    static final Voice VOICE = Voice.villager(2);

    private Bea() {
    }

    static void click(Player player) {
        new ShopMenu(Shop.BEA, player).open(player);
        VOICE.speak(player);
    }
}
