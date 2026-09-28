package net.icxd.dungeons.hex.gem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexScreen;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.item.gemstone.Gem;
import net.icxd.dungeons.item.gemstone.GemSlots;
import net.icxd.dungeons.item.gemstone.GemSlots.Slot;
import net.icxd.dungeons.item.gemstone.GemstoneQuality;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.menu.Icon;

/**
 * On a Sandbox profile only, and our own design (the owner's "easier customization"; Hypixel's grinder has no gem
 * picker): the gems an open slot takes, in all five qualities, to put one in without having it, for nothing. Each
 * gem a column (centred as the grinder's slots are, seven a page), each quality a row, Rough at the top; Go Back to
 * the grinder in 48, Close in 49, pages in 45 and 53. Main thread.
 */
final class GemPicker extends HexScreen {
    static final String TITLE = "Choose a Gemstone";
    static final int PREVIOUS = 45;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int NEXT = 53;
    static final int PER_PAGE = 7;
    static final Icon BACK_TO_GRINDER = new Icon(Material.ARROW, "&aGo Back", "&7To Gemstone Grinder");

    private final int index;
    private final boolean fromHex;
    private final int page;

    GemPicker(HexSession session, int index, boolean fromHex, int page) {
        super(session, TITLE);
        this.index = index;
        this.fromHex = fromHex;
        this.page = page;
    }

    @Override
    protected void draw() {
        fill(filler());
        Slot slot = slot();
        if (slot != null && slot.empty()) {
            for (Map.Entry<Integer, Gem> e : gems(slot.type(), page).entrySet()) {
                Gem gem = e.getValue();
                set(button(e.getKey(), GemstoneGrinder.withLore(GemstoneGrinder.gemStack(gem), List.of("", "&eClick to apply!")), () -> picked(gem)));
            }
            for (Map.Entry<Integer, Icon> e : arrows(slot.type(), page).entrySet()) {
                int to = e.getKey() == PREVIOUS ? page - 1 : page + 1;
                set(button(e.getKey(), e.getValue().stack(), () -> session.open(new GemPicker(session, index, fromHex, to))));
            }
        }
        set(button(BACK, BACK_TO_GRINDER.stack(), this::back));
        set(GUIClickableItem.close(CLOSE));
    }

    /** A page's gems by slot: the slot's gems across (a page's seven centred), qualities down from Rough in the top row. */
    static Map<Integer, Gem> gems(GemstoneType slot, int page) {
        List<GemstoneType> accepted = slot.accepted();
        int from = Math.min(page * PER_PAGE, accepted.size());
        List<GemstoneType> shown = accepted.subList(from, Math.min(accepted.size(), from + PER_PAGE));
        List<Integer> columns = GemstoneGrinder.row(shown.size(), 0);
        Map<Integer, Gem> gems = new LinkedHashMap<>();
        for (GemstoneQuality quality : GemstoneQuality.values()) {
            for (int i = 0; i < shown.size(); i++) gems.put(quality.ordinal() * 9 + columns.get(i), new Gem(shown.get(i), quality));
        }
        return gems;
    }

    /** The page arrows, where there's a page that way ("&ePage 2", as the plugin's other menus page). */
    static Map<Integer, Icon> arrows(GemstoneType slot, int page) {
        Map<Integer, Icon> arrows = new LinkedHashMap<>();
        if (page > 0) arrows.put(PREVIOUS, new Icon(Material.ARROW, "&aPrevious Page", "&ePage " + page));
        if ((page + 1) * PER_PAGE < slot.accepted().size()) arrows.put(NEXT, new Icon(Material.ARROW, "&aNext Page", "&ePage " + (page + 2)));
        return arrows;
    }

    private Slot slot() {
        HexItem item = session.hexItem();
        List<Slot> slots = item == null ? List.of() : GemSlots.of(item.item(), item.tag());
        return index < slots.size() ? slots.get(index) : null;
    }

    /** The gem goes in, free, if they're still on a Sandbox profile and the slot still takes it; back to the grinder. */
    private void picked(Gem gem) {
        HexItem item = session.hexItem();
        Slot slot = slot();
        if (item != null && slot != null && session.sandbox() && slot.empty() && slot.type().accepts(gem.type())) {
            NBTTagCompound tag = session.tag();
            GemSlots.apply(tag, item.item(), index, gem);
            if (session.upgrade(HexCosts.NOTHING, tag, null)) session.applied(GemstoneGrinder.gemName(gem));
        }
        back();
    }

    private void back() {
        session.open(new GemstoneGrinder(session, fromHex));
    }
}
