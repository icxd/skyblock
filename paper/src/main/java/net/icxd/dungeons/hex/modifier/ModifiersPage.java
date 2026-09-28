package net.icxd.dungeons.hex.modifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory.Look;
import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexPage;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.menu.Icon;

/**
 * "The Hex ➜ Modifiers" (the wiki's The Hex/UI tabs): the frame, an Anvil "Apply Modifiers" in 28, and what the
 * Hex offers the item (see HexModifiers), centred. Each is its item as the item data has it: its look, its name
 * (a Power Scroll's in its gem's colour, as the wiki's screen has them, and so is its Cost line), its own text,
 * then the Cost block, one of the item, and "&eClick to apply!" (UNKNOWN, U4: Hypixel's words when it can be paid).
 * A click applies it, paid
 * for (nothing on a Sandbox profile), and says "You applied a <modifier> to your <item>!" (HexSession#upgrade).
 * One that's done stays, saying so, and does nothing.
 */
public final class ModifiersPage extends HexPage {
    public static final String TITLE = "The Hex ➜ Modifiers";
    static final String APPLY = "&eClick to apply!";
    /** UNKNOWN (U5: where Hypixel's "Item Maxed Out" goes, and its words): what a done one says. */
    static final String APPLIED = "&aApplied!";
    static final String MAXED = "&aMaxed out!";
    /** UNKNOWN: Hypixel's words; the Cost block's own for a missing item. */
    static final String MISSING = "&cYou don't have that in your inventories!";

    private final Icon header;
    private final Function<String, Look> looks;

    /**
     * @param description the header's lines (the button's, without its summary: the wiki's)
     * @param looks       an item's look, by id (its material and head: HexCategory#lookOf)
     */
    public ModifiersPage(HexSession session, List<String> description, Function<String, Look> looks) {
        super(session, TITLE);
        this.header = new Icon(Material.ANVIL, "&aApply Modifiers", description);
        this.looks = looks;
    }

    @Override
    protected Icon header() {
        return header;
    }

    @Override
    protected Placement placement() {
        return Placement.CENTRED;
    }

    @Override
    protected List<Entry> entries() {
        HexItem item = session.hexItem();
        List<Entry> entries = new ArrayList<>();
        for (HexModifiers.Offer offer : HexModifiers.offers(item)) entries.add(entry(offer));
        return entries;
    }

    private Entry entry(HexModifiers.Offer offer) {
        HexCosts cost = HexCosts.of(new HexCosts.Items(offer.itemId(), 1));
        String name = name(offer);
        Look look = looks.apply(offer.itemId());
        Icon icon = new Icon(look.material(), name, lore(offer, offer.done() ? List.of() : cost.lore(session, APPLY)), look.texture());
        return offer.done() ? Entry.shown(icon) : Entry.of(icon, () -> apply(offer, cost, name));
    }

    /**
     * An entry's lore: its item's own text, a blank line, then the {@code cost} block, whose line for the item is in
     * the offer's colour when it has one ("&cRuby Power Scroll &c✖", the wiki's screen; the block's own is its
     * rarity's). One that's done says so instead.
     */
    static List<String> lore(HexModifiers.Offer offer, List<String> cost) {
        List<String> lore = new ArrayList<>(description(offer.itemId()));
        if (!lore.isEmpty()) lore.add("");
        if (offer.done()) {
            lore.add(offer.counted() ? MAXED : APPLIED);
            return lore;
        }
        SkyBlockItem item = ItemRegistry.get(offer.itemId());
        // The block's line for the item: its name in its rarity's colour, then ✔ or ✖ (HexCosts).
        String own = item == null ? null : item.rarity().getColor() + item.name() + " ";
        for (String line : cost) {
            boolean recolour = offer.colour() != null && own != null && line.startsWith(own);
            lore.add(recolour ? offer.colour() + item.name() + line.substring(own.length() - 1) : line);
        }
        return lore;
    }

    /** Paid for and applied to the item as it is now (the one it was drawn for: HexScreen#button). */
    private void apply(HexModifiers.Offer offer, HexCosts cost, String name) {
        NBTTagCompound tag = session.tag();
        if (tag == null) return;
        if (!cost.affordable(session)) {
            say(MISSING);
            return;
        }
        session.upgrade(cost, offer.apply().apply(tag), name);
    }

    /** "&6Recombobulator 3000": in its rarity's colour, or the offer's own; the id if there's no such item. */
    static String name(HexModifiers.Offer offer) {
        SkyBlockItem item = ItemRegistry.get(offer.itemId());
        if (item == null) return "&f" + offer.itemId();
        return (offer.colour() != null ? offer.colour() : "&" + item.rarity().getCode()) + item.name();
    }

    /** The item's own text (items.json's, as Hypixel's is); none without the item data. */
    static List<String> description(String itemId) {
        SkyBlockItem item = ItemRegistry.get(itemId);
        return item == null ? List.of() : item.lore();
    }
}
