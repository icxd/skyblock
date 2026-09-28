package net.icxd.dungeons.hex.reforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory.Look;
import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexPage;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.hex.category.Reforges;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.reforge.Reforge;
import net.icxd.dungeons.reforge.ReforgeStone;
import net.icxd.dungeons.reforge.ReforgeTable;

/**
 * "The Hex ➜ Reforges" (the wiki's Weapon tab): the Anvil "Apply Reforges" in 28, the Reforge Stones that go on the
 * item at its rarity now, centred and paged, and a Random Basic Reforge in 48 when a basic pool is for the item. A
 * stone costs its fee at that rarity and the stone itself; the random reforge its price at that rarity (Hypixel's
 * first reforge for 10 Coal is the Blacksmith's, not the Hex's). On a Sandbox profile every stone is there to apply
 * without having it, and it's all free (HexCosts). Applying one sets the item's reforge, makes it again, and says
 * "You reforged your <old name> into a <new name>!". Main thread.
 */
public final class ReforgePage extends HexPage {
    public static final String TITLE = "The Hex ➜ Reforges";

    public ReforgePage(HexSession session) {
        super(session, TITLE);
    }

    @Override
    protected Icon header() {
        return new Icon(Material.ANVIL, "&aApply Reforges", Reforges.DESCRIPTION);
    }

    @Override
    protected Placement placement() {
        return Placement.CENTRED;
    }

    /**
     * Each stone that goes on the item at its rarity, in the table's order (by reforge name: UNKNOWN, Hypixel's order;
     * the wiki's five sword stones aren't in any order we can tell). A stone the plugin has no item for (the Boo
     * Stone) isn't shown.
     */
    @Override
    protected List<Entry> entries() {
        HexItem item = session.hexItem();
        Rarity rarity = item.rarity();
        Reforge current = Reforge.of(item.tag());
        int level = ItemBuilder.catacombsLevel(viewer);
        List<Entry> entries = new ArrayList<>();
        for (ReforgeStone stone : ReforgeTable.get().stones(item.item(), rarity)) {
            SkyBlockItem stoneItem = ItemRegistry.get(stone.item());
            if (stoneItem == null) continue;
            HexCosts cost = HexCosts.of(new HexCosts.Coins(stone.cost(rarity)), new HexCosts.Items(stone.item(), 1));
            boolean has = current != null && current.id().equals(stone.reforge().id());
            List<String> costLore = cost.lore(session, ReforgeLore.CLICK);
            if (has) costLore = ReforgeLore.withAction(costLore, ReforgeLore.ALREADY);
            Look look = Reforges.stoneLook(stone.item());
            Icon icon = new Icon(look.material(), stoneItem.rarity().getColor() + stoneItem.name(),
                    ReforgeLore.stone(stoneItem.lore(), stone.reforge(), rarity, level, costLore), look.texture());
            entries.add(Entry.of(icon, has ? () -> say(ReforgeLore.ALREADY) : () -> reforge(stone.reforge(), cost)));
        }
        return entries;
    }

    /** Random Basic Reforge in 48, when a basic pool is for the item (and the table has a price for its rarity). */
    @Override
    protected Map<Integer, Entry> extras() {
        HexItem item = session.hexItem();
        ReforgeTable table = ReforgeTable.get();
        ReforgeTable.Pool pool = table.pool(item.item());
        Long price = table.randomPrice(item.rarity());
        if (pool == null || price == null) return Map.of();
        HexCosts cost = HexCosts.of(new HexCosts.Coins(price));
        Icon icon = new Icon(Material.ANVIL, "&aRandom Basic Reforge", ReforgeLore.random(price, session.sandbox(), cost.affordable(session)));
        Reforge current = Reforge.of(item.tag());
        return Map.of(LEFT, Entry.of(icon, () -> {
            Reforge next = ReforgeTable.roll(pool, current, ThreadLocalRandom.current());
            if (next != null) reforge(next, cost);
        }));
    }

    /** Pays (checked first: what's missing is said) and puts the reforge on the item. */
    private void reforge(Reforge reforge, HexCosts cost) {
        HexItem before = session.hexItem();
        if (before == null) return;
        if (!cost.affordable(session)) {
            say(ReforgeLore.missing(cost.lore(session, ReforgeLore.CLICK)));
            return;
        }
        NBTTagCompound tag = session.tag();
        tag.setString("reforge", reforge.id());
        if (!session.upgrade(cost, tag, null)) return;
        HexItem after = session.hexItem();
        say(ReforgeLore.reforged(before.name(), after == null ? before.name() : after.name()));
    }
}
