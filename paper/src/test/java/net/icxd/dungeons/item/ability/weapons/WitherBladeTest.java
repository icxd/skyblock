package net.icxd.dungeons.item.ability.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/** What a right click with a Necron's Blade does, by the scrolls on it: all three are Wither Impact. */
class WitherBladeTest {
    private static final String BLADE = """
            "HYPERION":{"dungeon_item":true,"lore":["&eRight-click to use your class ability!"],"material":"IRON_SWORD",\
            "name":"Test Hyperion","rarity":"LEGENDARY","stats":{"DAMAGE":260},"type":"SWORD"}""";

    private static String scroll(String id, String ability, int mana, int vitality) {
        return "\"" + id + "\":{\"abilities\":[{\"activation\":\"RIGHT_CLICK\",\"cooldown\":10,\"header\":\"&6Ability: " + ability
                + "  &e&lRIGHT CLICK\",\"kind\":\"ABILITY\",\"mana\":" + mana + (vitality > 0 ? ",\"vitality\":" + vitality : "")
                + ",\"name\":\"" + ability + "\",\"text\":[]}],\"material\":\"PAPER\",\"name\":\"" + ability + "\"}";
    }

    @TempDir
    Path folder;

    /** Loading a file that isn't there leaves no items. */
    @AfterEach
    void noItems() {
        ItemRegistry.loadData(folder.resolve("none.json"));
    }

    private SkyBlockItem blade() throws IOException {
        Path file = folder.resolve("items.json");
        Files.writeString(file, "{\"format\":1,\"items\":{" + String.join(",", BLADE, scroll("IMPLOSION_SCROLL", "Implosion", 300, 0),
                scroll("WITHER_SHIELD_SCROLL", "Wither Shield", 150, 50), scroll("SHADOW_WARP_SCROLL", "Shadow Warp", 300, 0)) + "}}");
        ItemRegistry.loadData(file);
        return ItemRegistry.get("HYPERION");
    }

    /** The ability a right click uses with these scrolls on (null for none: the class ability's). */
    private static ItemBlock rightClick(SkyBlockItem blade, boolean implosion, boolean shield, boolean warp) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("implosion", implosion);
        tag.setBoolean("wither_shield", shield);
        tag.setBoolean("shadow_warp", warp);
        List<ItemBlock> blocks = ItemBehaviours.of(blade).blocks(blade, tag, blade.blocks());
        return Abilities.forClick(blocks, true, false, name -> Abilities.get(name) != null);
    }

    @Test
    void scrolls() throws IOException {
        SkyBlockItem blade = blade();
        assertNull(rightClick(blade, false, false, false));
        assertEquals("Implosion", rightClick(blade, true, false, false).name());
        assertEquals("Wither Shield", rightClick(blade, false, true, false).name());
        assertEquals("Shadow Warp", rightClick(blade, false, false, true).name());
        // Two scrolls: the first of them in the scrolls' order (whether Hypixel casts both is UNKNOWN).
        assertEquals("Implosion", rightClick(blade, true, false, true).name());
        ItemBlock impact = rightClick(blade, true, true, true);
        assertEquals("Wither Impact", impact.name());
        assertEquals(300, impact.mana());
    }

    @Test
    void handlers() {
        assertInstanceOf(WitherBlade.Implosion.class, Abilities.get("Implosion"));
        assertInstanceOf(WitherBlade.WitherShield.class, Abilities.get("Wither Shield"));
        assertInstanceOf(WitherBlade.ShadowWarp.class, Abilities.get("Shadow Warp"));
        assertInstanceOf(WitherBlade.WitherImpact.class, Abilities.get("Wither Impact"));
        // Too little Vitality leaves out the shield, not the cast.
        assertTrue(Abilities.get("Wither Impact").vitalityOptional());
    }
}
