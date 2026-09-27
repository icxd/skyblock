package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Made-up items with set bonus blocks for the tests (Hypixel's text stays out of this repository). */
final class TestPieces {
    private TestPieces() {
    }

    /** A full set bonus's block as items' data has it: "&6Full Set Bonus: NAME &7(0/N)". */
    static String fullSet(String name, int pieces) {
        return "{\"kind\":\"FULL_SET\",\"name\":\"" + name + "\",\"header\":\"&6Full Set Bonus: " + name + " &7(0/" + pieces + ")\",\"pieces\":"
                + pieces + ",\"text\":[\"&7Test set text.\"]}";
    }

    /** A tiered bonus's block: "&8Tiered Bonus: NAME (0/N)", with this text. */
    static String tiered(String name, int pieces, String... text) {
        StringBuilder lines = new StringBuilder();
        for (String line : text) lines.append(lines.isEmpty() ? "" : ",").append('"').append(line).append('"');
        return "{\"kind\":\"TIERED\",\"name\":\"" + name + "\",\"header\":\"&8Tiered Bonus: " + name + " (0/" + pieces + ")\",\"pieces\":" + pieces
                + ",\"text\":[" + lines + "]}";
    }

    /** A piece's own bonus's block. */
    static String piece(String kind, String name) {
        return "{\"kind\":\"" + kind + "\",\"name\":\"" + name + "\",\"header\":\"&6Piece Bonus: " + name + "\",\"text\":[\"&7Test.\"]}";
    }

    /** An armor piece (its type from the id's end: _HELMET, _CHESTPLATE, ...) with these blocks. */
    static DataItem item(String id, String... blocks) {
        String type = id.substring(id.lastIndexOf('_') + 1).toUpperCase(Locale.ROOT);
        String material = switch (type) {
            case "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS" -> "LEATHER_" + type;
            default -> "PLAYER_HEAD";
        };
        String json = "{\"format\":1,\"items\":{\"" + id + "\":{\"material\":\"" + material + "\",\"name\":\"Test " + type + "\",\"type\":\"" + type
                + "\",\"abilities\":[" + String.join(",", blocks) + "]}}}";
        try {
            ItemData.Result result = ItemData.load(new StringReader(json));
            assertEquals(List.of(), result.errors());
            return result.items().values().iterator().next();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The item, worn. */
    static Worn.Piece worn(DataItem item) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", item.id());
        return new Worn.Piece(item, tag, item.blocks(), null);
    }

    static Worn wearing(DataItem... items) {
        return new Worn(java.util.Arrays.stream(items).map(TestPieces::worn).toList());
    }
}
