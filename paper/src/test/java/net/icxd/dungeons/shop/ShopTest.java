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
import net.icxd.dungeons.item.enums.GenericItemType;
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

    /** The recorded sales to the Farm Merchant (2026-10-24, 06:13.2 and 06:13.5), and buys. */
    @Test
    void theSaleLine() {
        assertEquals("&aYou sold &fCobblestone&8 x20 &afor &620 Coins&a!", Selling.sold("&fCobblestone", 20, 20));
        assertEquals("&aYou sold &fOak Sapling&8 x2 &afor &60.5 Coins&a!", Selling.sold("&fOak Sapling", 2, 0.5));
        assertEquals("&aYou sold &5Cicada Symphony Vinyl&8 x1 &afor &650,000 Coins&a!", Selling.sold("&5Cicada Symphony Vinyl", 1, 50_000));
        // The recorded buys (2026-10-24, 03:53.7 and 05:44.8; Bea's, 2026-10-25, 00:34.8); with no coins, UNKNOWN.
        assertEquals("&aYou bought &fRookie Hoe &afor &610 Coins&a!", Selling.bought("&fRookie Hoe", 1, 10));
        assertEquals("&aYou bought &fExperience Bottle&8 x8 &afor &6240 Coins&a!", Selling.bought("&fExperience Bottle", 8, 240));
        assertEquals("&aYou bought &fBee Pet &afor &650 Coins&a!", Selling.bought("&fBee Pet", 1, 50));
        assertEquals("&aYou bought &9Superboom TNT&a!", Selling.bought("&9Superboom TNT", 1, 0));
    }

    /** The recorded wares' lore under their items' own (the Farm Merchant's at 06:06.6, Ophelia's at 08:31.2). */
    @Test
    void wareLoreAsRecorded() {
        assertEquals(List.of("", "&7Cost", "&630 Coins", "", "&7Stock", "&6640 &7remaining", "", "&eClick to trade!",
                "&eRight-click for more trading options!"), ShopMenu.wareLore(List.of("&630 Coins"), 640, false, true), "Wheat x3");
        assertEquals(List.of("", "&7Cost", "&610 Coins", "", "&cNot unlocked!"), ShopMenu.wareLore(List.of("&610 Coins"), -1, true, false),
                "the Rookie Hoe, without Farming Skill 1");
        assertEquals(List.of("", "&7Cost", "&6250 Coins", "", "&eClick to trade!"), ShopMenu.wareLore(List.of("&6250 Coins"), -1, false, false),
                "the Beginner Agronomy Sack");
        assertEquals(List.of("", "&7Cost", "&65 Coins", "", "&eClick to trade!", "&eRight-click for more trading options!"),
                ShopMenu.wareLore(List.of("&65 Coins"), -1, false, true), "Ophelia's Flint Arrow");
        assertEquals(List.of("", "&7Cost", "&680,000 Coins", "&aCleaver", "", "&eClick to trade!"),
                ShopMenu.wareLore(List.of("&680,000 Coins", "&aCleaver"), -1, false, false), "Ophelia's Super Cleaver");
        assertEquals("&61,234 &7remaining", ShopMenu.wareLore(List.of(), 1234, false, true).get(4));
    }

    /** What offers "more trading options": what stacks (crops, arrows, TNT), not gear or a sack. */
    @Test
    void whatIsBoughtInBulk() {
        assertTrue(ShopMenu.bulk(false, null, 64), "Wheat");
        assertTrue(ShopMenu.bulk(false, GenericItemType.OTHER, 64), "Flint Arrow");
        assertFalse(ShopMenu.bulk(true, GenericItemType.OTHER, 64), "the sack, a head the API calls unstackable");
        assertFalse(ShopMenu.bulk(false, GenericItemType.ARMOR, 64), "Dark Goggles, a head");
        assertFalse(ShopMenu.bulk(false, GenericItemType.WEAPON, 1), "Super Cleaver");
        assertFalse(ShopMenu.bulk(false, GenericItemType.TOOL, 1), "Rookie Hoe");
        assertFalse(ShopMenu.bulk(false, null, 1), "a potion");
    }

    /** The recorded Farm Merchant's shop, in its order: what, how many, for what, and which have stock. */
    @Test
    void theFarmMerchant() {
        Shop shop = Shop.FARM_MERCHANT;
        assertEquals("Farm Merchant", shop.name());
        assertEquals(List.of("WHEAT x3 30", "CARROT_ITEM x3 30", "POTATO_ITEM x3 30", "MELON x10 40", "SUGAR_CANE x3 30", "PUMPKIN x1 25",
                        "INK_SACK:3 x1 5", "RED_MUSHROOM x1 25", "BROWN_MUSHROOM x1 25", "SAND x2 8", "CACTUS x1 15", "ENCHANTED_BONE_MEAL x3 6",
                        "ROOKIE_HOE x1 10", "ROOKIE_FARMING_AXE x1 10", "BEGINNER_AGRONOMY_SACK x1 250"),
                shop.wares(0).stream().map(w -> w.item() + " x" + w.amount() + " " + ((CoinCost) w.costs().getFirst()).getAmount()).toList());
        assertEquals(12, shop.wares().stream().filter(w -> w.stock() == ShopStock.DAILY).count());
        assertEquals(0, shop.wares().getLast().stock());
        assertTrue(shop.wares().stream().noneMatch(Shop.Ware::locked), "the Rookie tools lock on their items' requirement");
    }

    /** 640 of each a day, counted in items, each shop's and day's apart. */
    @Test
    void stock() {
        Document profile = new Document();
        Shop.Ware wheat = Shop.FARM_MERCHANT.wares().getFirst();
        Shop.Ware sack = Shop.FARM_MERCHANT.wares().getLast();
        assertEquals(640, ShopStock.remaining(profile, Shop.FARM_MERCHANT, wheat, NOW));
        ShopStock.record(profile, Shop.FARM_MERCHANT, wheat, NOW);
        ShopStock.record(profile, Shop.FARM_MERCHANT, wheat, NOW + MINUTE);
        assertEquals(634, ShopStock.remaining(profile, Shop.FARM_MERCHANT, wheat, NOW + MINUTE));
        assertEquals(640, ShopStock.remaining(profile, Shop.FARM_MERCHANT, Shop.FARM_MERCHANT.wares().get(1), NOW), "carrots apart");
        Shop other = new Shop("Windmill Operator", List.of(wheat));
        assertEquals(640, ShopStock.remaining(profile, other, wheat, NOW), "UNKNOWN: Hypixel's share with the Farm Merchant");
        // The next UTC day.
        assertEquals(640, ShopStock.remaining(profile, Shop.FARM_MERCHANT, wheat, NOW + 12 * 60 * MINUTE));
        for (int i = 0; i < 211; i++) ShopStock.record(profile, Shop.FARM_MERCHANT, wheat, NOW);
        assertEquals(1, ShopStock.remaining(profile, Shop.FARM_MERCHANT, wheat, NOW));
        assertFalse(ShopStock.allows(profile, Shop.FARM_MERCHANT, wheat, NOW), "one left, three a trade");
        ShopStock.record(profile, Shop.FARM_MERCHANT, sack, NOW);
        assertTrue(ShopStock.allows(profile, Shop.FARM_MERCHANT, sack, NOW), "no stock, no limit");
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
    void twoStacksAlikeSoldAtOnceAreBoughtBackOneAtATime() {
        // Two stacks of 64 of the same stackable item, shift-clicked in the same millisecond.
        Document profile = new Document();
        Buyback.add(profile, sale(64, NOW), NOW);
        Buyback.add(profile, sale(64, NOW), NOW);
        assertTrue(Buyback.remove(profile, Buyback.latest(profile, NOW), NOW));
        assertEquals(1, Buyback.entries(profile, NOW).size(), "the other is still there");
        assertTrue(Buyback.remove(profile, Buyback.latest(profile, NOW), NOW));
        assertNull(Buyback.latest(profile, NOW));
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
        // The wiki's Default tab (Ophelia/UI), without the Dungeon Potion; then NEU's Dungeonbreaker.
        assertEquals(List.of("UNDEAD_BOW", "ARROW", "SUPER_CLEAVER", "STONE_CHESTPLATE", "MENDER_HELMET", "DARK_GOGGLES", "SUPERBOOM_TNT",
                "DUNGEONBREAKER"), items(Shop.OPHELIA.wares(0)));
        assertEquals(items(Shop.OPHELIA.wares(0)), items(Shop.OPHELIA.wares(2)));
        // Floor III adds the second tier after Superboom TNT, Floor VI the third after that.
        assertEquals(List.of("UNDEAD_BOW", "ARROW", "SUPER_CLEAVER", "STONE_CHESTPLATE", "MENDER_HELMET", "DARK_GOGGLES", "SUPERBOOM_TNT",
                "SUPER_UNDEAD_BOW", "HYPER_CLEAVER", "METAL_CHESTPLATE", "MENDER_FEDORA", "SHADOW_GOGGLES", "DUNGEONBREAKER"),
                items(Shop.OPHELIA.wares(3)));
        assertEquals(items(Shop.OPHELIA.wares(3)), items(Shop.OPHELIA.wares(5)));
        assertEquals(List.of("UNDEAD_BOW", "ARROW", "SUPER_CLEAVER", "STONE_CHESTPLATE", "MENDER_HELMET", "DARK_GOGGLES", "SUPERBOOM_TNT",
                "SUPER_UNDEAD_BOW", "HYPER_CLEAVER", "METAL_CHESTPLATE", "MENDER_FEDORA", "SHADOW_GOGGLES", "DEATH_BOW", "GIANT_CLEAVER",
                "STEEL_CHESTPLATE", "MENDER_CROWN", "WITHER_GOGGLES", "DUNGEONBREAKER"), items(Shop.OPHELIA.wares(6)));
        assertEquals(Shop.OPHELIA.wares(), Shop.OPHELIA.wares(7));
        Shop.Ware arrows = Shop.OPHELIA.wares(0).get(1);
        assertEquals(20, arrows.amount());
        assertEquals(200, ((CoinCost) arrows.costs().getFirst()).getAmount());
    }

    @Test
    void opheliasThirdTierIsNotUnlocked() {
        // The wiki shows it "Not unlocked!" on the Floor VI and VII tabs, and nothing that can be bought
        // costs a million coins or more (where her Shop Confirmations would ask).
        for (Shop.Ware ware : Shop.OPHELIA.wares()) {
            boolean thirdTier = ware.costs().stream().anyMatch(c -> c instanceof CoinCost coins && coins.getAmount() >= 1_000_000);
            assertEquals(thirdTier, ware.locked(), ware.item());
        }
        assertEquals(5, Shop.OPHELIA.wares().stream().filter(Shop.Ware::locked).count());
        // The golden heads wait for 100 boss kills, which aren't counted.
        assertTrue(Shop.OPHELIA.wares().stream().noneMatch(w -> w.item().startsWith("GOLD_")));
    }

    private static List<String> items(List<Shop.Ware> wares) {
        return wares.stream().map(Shop.Ware::item).toList();
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
