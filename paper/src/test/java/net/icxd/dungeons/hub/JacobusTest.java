package net.icxd.dungeons.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.bson.Document;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.storage.Bag;
import net.icxd.dungeons.storage.BagUpgrades;

/** Jacobus and his menu against the recording (2026-10-25, 00:52.7 to 00:57.1). */
class JacobusTest {
    @Test
    void hisLine() {
        assertEquals("&e[NPC] Jacobus&f: &rAn investment in knowledge pays the best interest.", Jacobus.line(Jacobus.LINES.get(3)));
        assertEquals(9, Jacobus.LINES.size());
    }

    /** As recorded with none bought and no coins (Elizabeth's 12 were that account's; there's no Community Shop here). */
    @Test
    void theUpgradeAsRecorded() {
        Icon upgrade = JacobusMenu.upgrade(new Document(), 9, 0);
        assertEquals("&aAccessory Bag Upgrades", upgrade.name());
        assertEquals(List.of("&7Increase the size of your", "&aAccessory Bag&7.", "", "&7Redstone collection: &c9 Slots", "&7Elizabeth: &d+0 Slots", "",
                "&7Buying: &6+2 Slots", "&7Cost: &61,500,000 Coins", "", "&cCan't afford upgrade!"), upgrade.lore());
        assertEquals(JacobusMenu.REGISTER_HEAD, upgrade.texture());
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aAccessory Bag Shortcut", List.of("&7Quickly access your accessory bag", "&7from right here!", "",
                "&eClick to open!"), Bag.ACCESSORY_BAG.texture()), JacobusMenu.shortcut());
    }

    @Test
    void afterSomeAndAll() {
        Document profile = new Document();
        BagUpgrades.bought(profile, 5);
        List<String> lore = JacobusMenu.upgrade(profile, 15, 8_000_000).lore();
        assertEquals("&7Jacobus: &6+10 Slots", lore.get(5));
        assertEquals("&7Cost: &68,000,000 Coins", lore.get(8));
        assertEquals("&eClick to purchase!", lore.getLast());
        BagUpgrades.bought(profile, BagUpgrades.MOST);
        lore = JacobusMenu.upgrade(profile, 15, 1e12).lore();
        assertEquals("&7Jacobus: &6+198 Slots", lore.getLast());
    }
}
