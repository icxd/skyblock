package net.icxd.dungeons.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.MobKinds;

class KillCoinsTest {
    private static NBTTagCompound sword(String... enchantments) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "ASPECT_OF_THE_JERRY");
        NBTTagList list = new NBTTagList();
        for (String enchantment : enchantments) {
            NBTTagCompound e = new NBTTagCompound();
            e.setString("name", enchantment.split(" ")[0]);
            e.setShort("lvl", Short.parseShort(enchantment.split(" ")[1]));
            list.add(e);
        }
        tag.set("enchantments", list);
        return tag;
    }

    @Test
    void theMobsOwnCoins() {
        MobKind.Variant grunt = MobKinds.ZOMBIE_GRUNT.firstVariant();
        assertEquals(1, KillCoins.coins(grunt.coins(), grunt.level(), 0, 0));
        assertEquals(20, KillCoins.coins(MobKinds.MAGMA_CUBE.firstVariant().coins(), 75, 0, 0));
        assertEquals(1_000, KillCoins.coins(MobKinds.BLADESOUL.firstVariant().coins(), 200, 0, 0));
        assertEquals(1, MobKinds.CRYPT_UNDEAD.firstVariant().coins(), "the wiki's Crypt Undead (research critic.md C7)");
    }

    /** The wiki's Scavenger: I-VI scavenge 0.3, 0.6, 0.9, 1.2, 1.5 and 1.8 coins per monster level. */
    @Test
    void scavengerIsPointThreeATierPerLevel() {
        double[] perLevel = {0, 0.3, 0.6, 0.9, 1.2, 1.5, 1.8};
        for (int tier = 1; tier <= 6; tier++) assertEquals(perLevel[tier], KillCoins.coins(0, 1, tier, 0), "Scavenger " + tier);
        // A Lv40 Zombie Grunt with Scavenger V: 1 + 40 x 1.5.
        assertEquals(61, KillCoins.coins(1, 40, 5, 0));
        // A Lv47 Crypt Dreadlord with Scavenger III: 1 + 47 x 0.9.
        assertEquals(43.3, KillCoins.coins(1, 47, 3, 0));
    }

    @Test
    void accessoriesAddToScavenger() {
        assertEquals(0.5, KillCoins.accessory(List.of("SCAVENGER_TALISMAN", "HYPERION")));
        assertEquals(0.75, KillCoins.accessory(List.of("SCAVENGER_TALISMAN", "SCAVENGER_ARTIFACT", "SCAVENGER_RING")), "the best only");
        assertEquals(0, KillCoins.accessory(List.of("HYPERION")));
        // "2.3 per level with Scavenger VI and the Accessory".
        assertEquals(230, KillCoins.coins(0, 100, 6, 0.5));
        assertEquals(1 + 45 * 0.6, KillCoins.coins(1, 45, 0, 0.6), 1e-9);
    }

    @Test
    void readsTheTierOffTheWeapon() {
        assertEquals(4, KillCoins.scavenger(sword("sharpness 5", "scavenger 4")));
        assertEquals(0, KillCoins.scavenger(sword("sharpness 5")));
        assertEquals(0, KillCoins.scavenger(null), "not a SkyBlock item");
    }
}
