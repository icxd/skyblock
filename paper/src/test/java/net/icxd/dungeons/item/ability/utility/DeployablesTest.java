package net.icxd.dungeons.item.ability.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** Power Orbs, Flares and Lanterns: what they buff, from their text, and who gets which. */
class DeployablesTest {
    private static final UUID WORLD = UUID.randomUUID();
    private static final UUID OTHER_WORLD = UUID.randomUUID();

    /** A made-up orb in items.json's shape (the lines are a Radiant Power Orb's). */
    private static final String ORB = """
            {"format":1,"items":{"TEST_ORB":{"abilities":[{"activation":"RIGHT_CLICK","header":"&6Ability: Deploy  &e&lRIGHT CLICK",\
            "kind":"ABILITY","mana_percent":50,"name":"Deploy","text":["&7Place an orb for &a1m &7buffing up to &b5",\
            "&7players within &a18 &7blocks."],"vitality":50}],"lore":["&aOrb Buff: Radiant","&a• &7Heal yourself for &c30❤&7 per second.",\
            "&a• &7Heal others for &c15❤&7 per second.","&a• &7Gain &c+25❣ Health Regen&7.","&7Grants &b+50% &7base mana regen.",\
            "","&8Only one deployable buff applies."],"material":"PLAYER_HEAD","name":"Test Orb","rarity":"UNCOMMON","type":"DEPLOYABLE"}}}""";

    private static Deployables.Kind kind(String name, Rarity rarity, double radius, int players) {
        return new Deployables.Kind("TEST", name, rarity, "§a" + name, false, 60_000, radius, players, 0, 0, 0, 0, new Stats());
    }

    private static Deployables.Placed placed(UUID owner, Deployables.Kind kind, double x, long at) {
        return new Deployables.Placed(owner, kind, WORLD, x, 64, 0, at, at + kind.millis());
    }

    private static Deployables.Near near(UUID player, double x) {
        return new Deployables.Near(player, WORLD, x, 64, 0);
    }

    @Test
    void readsItsText() throws IOException {
        DataItem item = ItemData.load(new StringReader(ORB)).items().get("TEST_ORB");
        ItemBlock block = item.blocks().get(0);
        Deployables.Kind kind = Deployables.read(item, block);
        assertNotNull(kind);
        assertEquals("§aRadiant", kind.label());
        assertFalse(kind.flare());
        assertEquals(60_000, kind.millis());
        assertEquals(18, kind.radius(), 1e-9);
        assertEquals(5, kind.players());
        assertEquals(30, kind.healOwner(), 1e-9);
        assertEquals(15, kind.healOthers(), 1e-9);
        assertEquals(0.5, kind.manaRegen(), 1e-9);
        assertEquals(new Stats().set(Stat.HEALTH_REGEN, 25), kind.stats());
        assertTrue(kind.doesSomething());
        // Its 50% of max mana is its data's, which the framework takes.
        assertEquals(0, Deployables.textManaShare(block), 1e-9);
    }

    /** What the stand shows, as SkyHanni and Skytils read it. */
    @Test
    void standNames() {
        assertEquals("§aRadiant §e60s", Deployables.standName("§aRadiant", 60_000));
        assertEquals("§aRadiant §e57s", Deployables.standName("§aRadiant", 56_001));
        assertEquals("§d§lPlasmaflux", Deployables.label('6', "Plasmaflux", Rarity.LEGENDARY));
        assertEquals("§6§lWill-o'-wisp", Deployables.label('6', "Will-o'-wisp", Rarity.LEGENDARY));
        assertEquals("§9Mana Flux", Deployables.label('9', "Mana Flux", Rarity.RARE));
    }

    /** "Only one deployable buff applies": the higher rarity, whoever's it is. */
    @Test
    void strongestInRangeWins() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), me = UUID.randomUUID();
        Deployables.Placed radiant = placed(a, kind("Radiant", Rarity.UNCOMMON, 18, 5), 0, 1_000);
        Deployables.Placed plasmaflux = placed(b, kind("Plasmaflux", Rarity.LEGENDARY, 20, 5), 10, 0);
        Map<UUID, Deployables.Placed> buffs = Deployables.assign(List.of(radiant, plasmaflux), List.of(near(me, 5), near(a, 0)));
        assertSame(plasmaflux, buffs.get(me));
        // Its owner stands 10 from the Plasmaflux too: it's theirs to have, not their own Radiant.
        assertSame(plasmaflux, buffs.get(a));
        // Out of the Plasmaflux's 20, only the Radiant: at x -15 that's 15 from the Radiant and 25 from the other.
        assertSame(radiant, Deployables.assign(List.of(radiant, plasmaflux), List.of(near(me, -15))).get(me));
        // Out of both, nothing.
        assertNull(Deployables.assign(List.of(radiant, plasmaflux), List.of(near(me, -40))).get(me));
    }

    @Test
    void sameRarityTheLaterOne() {
        UUID me = UUID.randomUUID();
        Deployables.Placed first = placed(UUID.randomUUID(), kind("Overflux", Rarity.EPIC, 18, 5), 0, 1_000);
        Deployables.Placed second = placed(UUID.randomUUID(), kind("Overflux", Rarity.EPIC, 18, 5), 0, 2_000);
        assertSame(second, Deployables.assign(List.of(first, second), List.of(near(me, 1))).get(me));
        assertSame(second, Deployables.assign(List.of(second, first), List.of(near(me, 1))).get(me));
    }

    /** "buffing up to 5 players": the nearest, its owner first; nobody in another world. */
    @Test
    void upToItsNumberOfPlayers() {
        UUID owner = UUID.randomUUID(), near = UUID.randomUUID(), far = UUID.randomUUID();
        Deployables.Placed orb = placed(owner, kind("Radiant", Rarity.UNCOMMON, 18, 2), 0, 0);
        Map<UUID, Deployables.Placed> buffs = Deployables.assign(List.of(orb), List.of(near(far, 10), near(owner, 15), near(near, 2)));
        assertSame(orb, buffs.get(owner));
        assertSame(orb, buffs.get(near));
        assertNull(buffs.get(far));
        UUID elsewhere = UUID.randomUUID();
        assertTrue(Deployables.assign(List.of(orb), List.of(new Deployables.Near(elsewhere, OTHER_WORLD, 0, 64, 0))).isEmpty());
    }

    /** Every Deploy in the real items.json reads, and only those with nothing this server has aren't put down. */
    @Test
    void everyRealDeployable() throws IOException {
        Path items = Path.of(System.getProperty("items.file", "../../skyblock-dungeon-data/items/items.json"));
        assumeTrue(Files.exists(items), "no " + items);
        Map<String, DataItem> all;
        try (Reader reader = Files.newBufferedReader(items)) {
            all = ItemData.load(reader).items();
        }
        int orbs = 0;
        for (DataItem item : all.values()) {
            for (ItemBlock block : item.blocks()) {
                if (!block.isAbility() || !"Deploy".equals(block.name())) continue;
                Deployables.Kind kind = Deployables.read(item, block);
                if (item.id().equals("TOTEM_OF_CORRUPTION")) {
                    assertTrue(kind == null || !kind.doesSomething(), "the Totem's buff is fishing's");
                    continue;
                }
                assertNotNull(kind, item.id());
                assertTrue(kind.doesSomething(), item.id());
                if (item.id().endsWith("_POWER_ORB")) {
                    orbs++;
                    assertTrue(kind.healOwner() > kind.healOthers() && kind.healOthers() > 0, item.id());
                    assertEquals(5, kind.players(), item.id());
                }
                // A Lantern's "Costs 50% of max mana" is only its text's; the orbs' is their data's.
                boolean costInText = item.id().endsWith("LANTERN") || item.id().equals("WILL_O_WISP") || item.id().equals("UMBERELLA");
                assertEquals(costInText ? 0.5 : 0, Deployables.textManaShare(block), 1e-9, item.id());
                if (item.id().endsWith("_FLARE")) {
                    assertTrue(kind.flare() && kind.islandHeal() > 0 && kind.stats().get(Stat.VITALITY) > 0, item.id());
                    assertEquals(40, kind.radius(), 1e-9);
                }
            }
        }
        assertEquals(4, orbs);
        Deployables.Kind plasmaflux = Deployables.read(all.get("PLASMAFLUX_POWER_ORB"), all.get("PLASMAFLUX_POWER_ORB").blocks().get(0));
        assertEquals("§d§lPlasmaflux", plasmaflux.label());
        assertEquals(20, plasmaflux.radius(), 1e-9);
        assertEquals(1.25, plasmaflux.manaRegen(), 1e-9);
        assertEquals(new Stats().set(Stat.HEALTH_REGEN, 125).set(Stat.STRENGTH, 35), plasmaflux.stats());
    }
}
