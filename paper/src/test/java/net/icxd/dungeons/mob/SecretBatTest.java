package net.icxd.dungeons.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;

/** The wiki's Bat (Catacombs): a secret on every floor. */
class SecretBatTest {
    @Test
    void wikisNumbers() {
        MobKind bat = MobKinds.SECRET_BAT;
        assertEquals(EntityType.BAT, bat.entityType());
        assertEquals(MobKinds.SECRET_BAT, MobKinds.get("dungeon_secret_bat"));
        assertFalse(bat.roomScaled());
        for (DungeonFloor floor : DungeonFloor.values()) {
            MobKind.Variant variant = bat.variant(floor, null);
            if (floor.isMasterMode()) {
                assertNull(variant);
                continue;
            }
            assertNotNull(variant, floor.name());
            assertEquals(1, variant.level());
            assertEquals(100, variant.health());
            assertEquals(0, variant.damage());
            assertEquals(100, variant.combatXp());
            assertEquals(1, variant.coins());
        }
    }
}
