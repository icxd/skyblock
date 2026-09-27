package net.icxd.dungeons.shop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.enums.Soulbound;

class ShopTest {
    private static final long NOW = Instant.parse("2026-09-27T12:00:00Z").toEpochMilli();
    private static final long MINUTE = 60_000;

    private static SkyBlockItem item(String id, double price, Soulbound soulbound) {
        return new SkyBlockItem() {
            @Override public String id() { return id; }
            @Override public String name() { return id; }
            @Override public Material material() { return Material.STONE; }
            @Override public double npcSellPrice() { return price; }
            @Override public Soulbound soulbound() { return soulbound; }
        };
    }

    private static Buyback.Entry sale(int n, long at) {
        return new Buyback.Entry(new byte[] {(byte) n}, n, at);
    }

    @Test
    void aStackSellsForItsPriceTimesItsAmount() {
        assertEquals(20_480, Selling.total(320, 64), "Enchanted Bone");
        assertEquals(9_600_000, Selling.total(150_000, 64));
        assertEquals(19.2, Selling.total(0.3, 64), "Torches: fractions stay");
        assertEquals(0.75, Selling.total(0.25, 3));
        assertEquals(1, Selling.total(1, 1));
    }

    @Test
    void theSaleLine() {
        assertEquals("&aYou sold &fTorch&a x64 for &619.2 Coins&a!", Selling.sold("&fTorch", 64, 19.2));
        assertEquals("&aYou sold &5Cicada Symphony Vinyl&a x1 for &650,000 Coins&a!", Selling.sold("&5Cicada Symphony Vinyl", 1, 50_000));
        assertEquals("&aYou bought &9Superboom TNT&a!", Selling.bought("&9Superboom TNT", 1));
        assertEquals("&aYou bought &fFlint Arrow&a x20!", Selling.bought("&fFlint Arrow", 20));
    }

    @Test
    void whatSells() {
        assertTrue(Selling.sellable(item("ENCHANTED_BONE", 320, Soulbound.NONE)));
        assertTrue(Selling.sellable(item("TORCH", 0.3, Soulbound.NONE)));
        assertFalse(Selling.sellable(item("HYPERION", 0, Soulbound.NONE)), "no price");
        assertFalse(Selling.sellable(item("GOLD_BONZO_HEAD", 5, Soulbound.COOP)), "soulbound");
        assertFalse(Selling.sellable(item("SOLO_THING", 5, Soulbound.SOLO)), "soulbound");
        assertFalse(Selling.sellable(item("SKYBLOCK_MENU", 1, Soulbound.NONE)));
        assertFalse(Selling.sellable(null), "not a SkyBlock item");
    }

    @Test
    void buybackKeepsTheLastTen() {
        Document profile = new Document();
        for (int i = 1; i <= 12; i++) Buyback.add(profile, sale(i, NOW + i), NOW + i);
        List<Buyback.Entry> entries = Buyback.entries(profile, NOW + 20);
        assertEquals(Buyback.KEPT, entries.size());
        assertEquals(12, entries.getFirst().price(), "newest first");
        assertEquals(3, entries.getLast().price(), "the two oldest are gone");
        assertArrayEquals(new byte[] {12}, Buyback.latest(profile, NOW + 20).item());
    }

    @Test
    void buybackForgetsAfterAnHour() {
        Document profile = new Document();
        Buyback.add(profile, sale(1, NOW), NOW);
        Buyback.add(profile, sale(2, NOW + 30 * MINUTE), NOW + 30 * MINUTE);
        assertEquals(2, Buyback.entries(profile, NOW + 59 * MINUTE).size());
        assertEquals(1, Buyback.entries(profile, NOW + 60 * MINUTE).size(), "the first is an hour old");
        assertEquals(2, Buyback.latest(profile, NOW + 60 * MINUTE).price());
        assertNull(Buyback.latest(profile, NOW + 90 * MINUTE));
        // Selling again drops what's expired from the profile.
        Buyback.add(profile, sale(3, NOW + 2 * 60 * MINUTE), NOW + 2 * 60 * MINUTE);
        assertEquals(1, profile.getList(Buyback.FIELD, Document.class).size());
    }

    @Test
    void buyingBackTakesItOffTheList() {
        Document profile = new Document();
        Buyback.add(profile, sale(1, NOW), NOW);
        Buyback.add(profile, sale(2, NOW + 1), NOW + 1);
        Buyback.Entry latest = Buyback.latest(profile, NOW + 2);
        assertTrue(Buyback.remove(profile, latest, NOW + 2));
        assertFalse(Buyback.remove(profile, latest, NOW + 2), "once");
        assertEquals(1, Buyback.latest(profile, NOW + 2).price(), "the one before is next");
    }

    @Test
    void buybackSurvivesTheDatabase() {
        // Through Mongo, the bytes come back as Binary and the time as a Date: toDocument writes both so already.
        Document profile = new Document();
        Buyback.add(profile, new Buyback.Entry(new byte[] {7, 8}, 0.3, NOW), NOW);
        Document stored = profile.getList(Buyback.FIELD, Document.class).getFirst();
        assertTrue(stored.get("item") instanceof org.bson.types.Binary);
        assertTrue(stored.get("soldAt") instanceof java.util.Date);
        assertEquals(0.3, Buyback.latest(profile, NOW).price());
        // And junk is skipped, not a crash.
        profile.getList(Buyback.FIELD, Object.class).add(new Document("price", "x"));
        assertEquals(1, Buyback.entries(profile, NOW).size());
    }

    @Test
    void theDailyLimit() {
        Document profile = new Document();
        assertEquals(500_000_000, SellLimit.DAILY);
        assertTrue(SellLimit.allows(profile, 500_000_000, NOW));
        SellLimit.record(profile, 499_999_000, NOW);
        assertEquals(499_999_000, SellLimit.soldToday(profile, NOW));
        assertTrue(SellLimit.allows(profile, 1_000, NOW));
        assertFalse(SellLimit.allows(profile, 1_000.1, NOW));
        SellLimit.record(profile, 1_000, NOW);
        assertFalse(SellLimit.allows(profile, 0.3, NOW), "reached");
        // 00:00 UTC starts a new day.
        long midnight = Instant.parse("2026-09-28T00:00:00Z").toEpochMilli();
        assertFalse(SellLimit.allows(profile, 1, midnight - 1));
        assertEquals(0, SellLimit.soldToday(profile, midnight));
        assertTrue(SellLimit.allows(profile, 1_000_000, midnight));
    }

    @Test
    void opheliasWaresComeWithFloors() {
        Set<String> items = new HashSet<>();
        for (Shop.Ware ware : Shop.OPHELIA.wares()) assertTrue(items.add(ware.item()), "once each: " + ware.item());
        List<String> atFirst = Shop.OPHELIA.wares(0).stream().map(Shop.Ware::item).toList();
        assertEquals(List.of("SUPER_CLEAVER", "UNDEAD_BOW", "ARROW", "DARK_GOGGLES", "MENDER_HELMET", "SUPERBOOM_TNT", "STONE_CHESTPLATE",
                "DUNGEONBREAKER"), atFirst);
        assertTrue(Shop.OPHELIA.wares(3).stream().anyMatch(w -> w.item().equals("HYPER_CLEAVER")));
        assertFalse(Shop.OPHELIA.wares(5).stream().anyMatch(w -> w.item().equals("GIANT_CLEAVER")));
        assertEquals(Shop.OPHELIA.wares().size(), Shop.OPHELIA.wares(7).size());
        Shop.Ware arrows = Shop.OPHELIA.wares(0).get(2);
        assertEquals(20, arrows.amount());
        assertEquals(200, ((CoinCost) arrows.costs().getFirst()).getAmount());
    }

    @Test
    void costLines() {
        assertEquals("&680,000 Coins", ShopMenu.costLine(new CoinCost(80_000)));
        assertEquals("&65,000,000 Coins", ShopMenu.costLine(new CoinCost(5_000_000)));
        assertEquals("&dWither Essence &8x500", ShopMenu.costLine(new EssenceCost(EssenceType.WITHER, 500)));
        // An item this server doesn't have shows by its id.
        assertEquals("&fNOT_AN_ITEM &8x2", ShopMenu.costLine(new ItemCost("NOT_AN_ITEM", 2)));
    }
}
