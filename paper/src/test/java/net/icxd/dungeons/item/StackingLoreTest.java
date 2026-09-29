package net.icxd.dungeons.item;

import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enchanting.FakeEnchantments;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A stacking enchantment in lore, as live lore has it: its count after its name while there's a tier to go ("&9Compact
 * VIII &8202,861"), and after its description what the next tier takes ("&8500k blocks to tier up!"). On the made-up
 * table's Champion I.
 */
class StackingLoreTest {
    private static final DataItem SWORD;

    static {
        try {
            ItemData.Result result = ItemData.load(new StringReader("{\"format\":1,\"items\":{\"TEST_STACK_SWORD\":{\"material\":\"IRON_SWORD\","
                    + "\"name\":\"Test Stack Sword\",\"rarity\":\"RARE\",\"stats\":{\"DAMAGE\":50},\"type\":\"SWORD\"}}}"));
            assertEquals(List.of(), result.errors());
            SWORD = result.items().values().iterator().next();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeEach
    void enchantments() {
        FakeEnchantments.use();
    }

    @AfterEach
    void reset() {
        FakeEnchantments.reset();
    }

    private static NBTTagCompound champion() {
        NBTTagCompound tag = ItemBuilder.newData(SWORD);
        tag.remove("attribute_1");
        tag.remove("attribute_2");
        NBTTagList list = new NBTTagList();
        NBTTagCompound enchantment = new NBTTagCompound();
        enchantment.setString("name", "champion");
        enchantment.setInt("lvl", 1);
        list.add(enchantment);
        tag.set("enchantments", list);
        return tag;
    }

    @Test
    void withItsCount() {
        NBTTagCompound tag = champion();
        tag.setDouble("champion_combat_xp", 467.74);
        assertEquals(List.of("&9Champion I &8467", "&7Gain &a3% &7extra Combat XP.", "&850k Combat XP to tier up!"),
                ItemBuilder.enchantmentLines(SWORD, tag));
    }

    /** Nothing counted yet: no count, still the next tier. */
    @Test
    void withoutACount() {
        assertEquals(List.of("&9Champion I", "&7Gain &a3% &7extra Combat XP.", "&850k Combat XP to tier up!"),
                ItemBuilder.enchantmentLines(SWORD, champion()));
    }
}
