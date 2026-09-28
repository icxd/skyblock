package net.icxd.dungeons.hex.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexPage;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.item.DungeonItems;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.UpgradeCost;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.upgrade.Stars;
import net.icxd.dungeons.menu.Icon;

/**
 * "The Hex ➜ Item Upgrades" (Lunar Client's name for it; NEU's Hex reads it): the item's stars in the grid, one entry
 * a star (NEU knows them by the ✪ in their names), each bought in order for that star's essence and items (the item
 * data's upgrade costs, as at Malik; see {@link Stars}), and Convert to Dungeon Item in 48 (an anvil, NEU's), which
 * says "This item is already a Dungeon Item" once it's done (the fragment NEU looks for). Free on a Sandbox profile
 * (see HexCosts). A click buys one star, the next.
 *
 * <p>UNKNOWN (U12), our own simplest version: the header (an anvil, as NEU needs it to be), each star's look (a
 * nether star), name (the stars as the item's name shows them) and lore (the Essence Guide's line on what a star
 * gives, then the cost, or that it's on, or which comes first), their placement (centred, as the other pages'
 * few entries), Convert's lore past its name (Malik's Convert to Dungeon Item's text, then the cost), and the lines
 * said in chat.
 */
public final class UpgradesPage extends HexPage {
    public static final String TITLE = "The Hex ➜ Item Upgrades";
    public static final int CONVERT = LEFT;
    static final String UPGRADE = "&eClick to upgrade!";
    static final String CONVERT_ACTION = "&eClick to convert!";
    static final List<String> CONVERT_TEXT = List.of("&7Converts an item to a &bDungeon item&7,", "&7which boosts the item's stats based",
            "&7on your &aDungeoneering Skill &7level", "&7while you are in Dungeons.");
    static final List<String> CONVERTED = List.of("&aThis item is already a Dungeon", "&aItem!");

    private final Icon header;

    public UpgradesPage(HexSession session, List<String> description) {
        super(session, TITLE);
        // UNKNOWN (U12): NEU's Hex only says it's an anvil or an enchantment table.
        this.header = new Icon(Material.ANVIL, "&aItem Upgrades", description);
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
        int next = ItemBuilder.starCount(item.tag()) + 1;
        for (int star = 1; star <= Stars.max(item.item()); star++) {
            Icon icon = star(item.item(), item.tag(), star, cost -> cost.lore(session, UPGRADE));
            int n = star;
            entries.add(Entry.of(icon, star == next ? () -> upgrade(n) : null));
        }
        return entries;
    }

    @Override
    protected Map<Integer, Entry> extras() {
        HexItem item = session.hexItem();
        Icon icon = convert(item.item(), item.tag(), cost -> cost.lore(session, CONVERT_ACTION));
        if (icon == null) return Map.of();
        return Map.of(CONVERT, Entry.of(icon, DungeonItems.convertible(item.item(), item.tag()) ? this::convert : null));
    }

    /** Star {@code n}, if it's still the next, paid for with its costs. */
    private void upgrade(int n) {
        SkyBlockItem kind = session.skyBlockItem();
        NBTTagCompound tag = session.tag();
        UpgradeCost cost = kind == null || tag == null ? null : Stars.next(kind, tag);
        if (cost == null || ItemBuilder.starCount(tag) + 1 != n) {
            redraw();
            return;
        }
        Stars.add(tag);
        // UNKNOWN: what Hypixel says; our own words.
        if (session.upgrade(cost(cost), tag, null)) say("&aYou upgraded your " + ItemBuilder.name(kind, tag) + "&a!");
        else redraw();
    }

    /** Makes it a dungeon item, paid for with its conversion cost. */
    private void convert() {
        SkyBlockItem kind = session.skyBlockItem();
        NBTTagCompound tag = session.tag();
        if (kind == null || tag == null || !DungeonItems.convertible(kind, tag)) {
            redraw();
            return;
        }
        DungeonItems.convert(tag);
        // UNKNOWN: what Hypixel says; our own words.
        if (session.upgrade(cost(kind.dungeonConversionCost()), tag, null)) say("&aYou converted your " + ItemBuilder.name(kind, tag) + " &ainto a Dungeon Item!");
        else redraw();
    }

    /** An upgrade's costs as the Hex takes them, in the item data's order (the API's). */
    static HexCosts cost(UpgradeCost upgrade) {
        List<HexCosts.Part> parts = new ArrayList<>();
        for (Cost cost : upgrade.getCosts()) {
            switch (cost) {
                case CoinCost c -> parts.add(new HexCosts.Coins(c.getAmount()));
                case ItemCost i -> parts.add(new HexCosts.Items(i.getItemId(), i.getAmount()));
                case EssenceCost e -> parts.add(new HexCosts.Essence(e.getEssenceType(), e.getAmount()));
                // The item data only makes those three (ItemData#costs).
                default -> throw new IllegalArgumentException("a cost the Hex can't take: " + cost);
            }
        }
        return HexCosts.of(parts);
    }

    /** "&6✪✪✪": how the item's name shows {@code n} stars (a dungeon item's gold; past 5 on others, purple and aqua). */
    static String starName(SkyBlockItem item, NBTTagCompound tag, int n) {
        NBTTagCompound stars = tag.copy();
        stars.setInt("upgrade_count", n);
        stars.remove("dungeon_star");
        return ItemBuilder.stars(item, stars).substring(1);
    }

    /**
     * Star {@code n}'s entry for an item with this data: the Essence Guide's line on what a star gives, a blank line,
     * then the star's Cost block ({@code costLore}: HexCosts#lore) if it's the next, else that it's on, or which
     * star comes first.
     */
    static Icon star(SkyBlockItem item, NBTTagCompound tag, int n, Function<HexCosts, List<String>> costLore) {
        boolean dungeon = DungeonItems.is(item, tag);
        List<String> lore = new ArrayList<>();
        lore.add("&7Each item level upgrade &6✪ &7grants a");
        if (dungeon) lore.addAll(List.of("&a+2% &7stat bonus and a &a+10% &7bonus", "&7while in Dungeons."));
        else lore.add("&a+2% &7stat bonus.");
        lore.add("");
        int has = ItemBuilder.starCount(tag);
        if (n <= has) lore.add("&aThis upgrade has been applied!");
        else if (n == has + 1) lore.addAll(costLore.apply(cost(Stars.cost(item, n))));
        else lore.add("&cUpgrade to " + starName(item, tag, n - 1) + " &cfirst!");
        return new Icon(Material.NETHER_STAR, starName(item, tag, n), lore);
    }

    /**
     * Convert to Dungeon Item for an item with this data: Malik's text on it, a blank line, then the Cost block
     * ({@code costLore}) or, once it's one, "This item is already a Dungeon Item". Null for an item that neither is
     * one nor can be made one.
     */
    static Icon convert(SkyBlockItem item, NBTTagCompound tag, Function<HexCosts, List<String>> costLore) {
        boolean dungeon = DungeonItems.is(item, tag);
        if (!dungeon && item.dungeonConversionCost() == null) return null;
        List<String> lore = new ArrayList<>(CONVERT_TEXT);
        lore.add("");
        lore.addAll(dungeon ? CONVERTED : costLore.apply(cost(item.dungeonConversionCost())));
        return new Icon(Material.ANVIL, "&aConvert to Dungeon Item", lore);
    }
}
