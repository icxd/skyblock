package net.icxd.dungeons.hex;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;

/**
 * One of the Hex's seven categories (the buttons on its main menu, see {@link HexCategories} for the order):
 * what its button looks like, which items it's for, its group of lines in the purple panes' summary and on its
 * button, the Carpentry level it needs on a Normal profile, and its page. Each is in a file of its own under
 * {@code hex/category}, so each is built on its own; see HEX.md.
 *
 * <p>How the main menu uses it: the button shows while {@link #applies} is true for the item in the Hex, named
 * {@code &a<name>}, its lore the {@link #description}, a blank line, the {@link #summary}, a blank line and
 * "&eClick to view!" (or the Carpentry line, when that isn't met); a click opens its page with {@link #open}
 * (the item applies and the requirement is met then). The purple panes list every applying category's summary,
 * a blank line between them. Main thread, but for {@link #applies} and {@link #summary}, which are plain.
 */
public abstract class HexCategory {
    private final String name;
    private final int carpentry;
    private final List<String> description;

    /**
     * @param name        the button's name without its colour, "Enchantments"
     * @param carpentry   the Carpentry level it needs on a Normal profile (0 for none, 20 or 25)
     * @param description the button's lines above its summary
     */
    protected HexCategory(String name, int carpentry, List<String> description) {
        this.name = name;
        this.carpentry = carpentry;
        this.description = List.copyOf(description);
    }

    public final String name() {
        return name;
    }

    public final int requiredCarpentry() {
        return carpentry;
    }

    public final List<String> description() {
        return description;
    }

    /** The button's item: its material, and a head's texture hash (null for none). */
    public record Look(Material material, String texture) {
    }

    public abstract Look look();

    /** Whether the Hex offers this category for the item (its button shows, its summary is in the panes). */
    public abstract boolean applies(HexItem item);

    /**
     * Its lines in the panes' summary and on its button, for an item it applies to, each indented two spaces as
     * Hypixel's are ("  &7Reforge &c✖"). Empty for none.
     */
    public abstract List<String> summary(HexItem item);

    /** Opens its page for the session's item: {@code session.open(new ...Page(session))}. */
    public abstract void open(HexSession session);

    // For the looks

    private static final Pattern SKIN_URL = Pattern.compile("textures\\.minecraft\\.net/texture/([0-9a-fA-F]+)");

    /**
     * An item's own look from the item data (items.json): its material and head texture, as the Hex's buttons
     * show the items they're named after. {@code fallback} if the data doesn't have the item.
     */
    protected static Look lookOf(String id, Look fallback) {
        SkyBlockItem item = ItemRegistry.get(id);
        if (item == null) return fallback;
        return new Look(item.material(), item.skin() == null ? null : textureHash(item.skin()));
    }

    /** The hash at the end of a skin's textures.minecraft.net address (a skin is that JSON in base64); null if it has none. */
    static String textureHash(String skin) {
        String json;
        try {
            json = new String(Base64.getDecoder().decode(skin), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Matcher m = SKIN_URL.matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
