package net.icxd.dungeons.hex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.utils.Text;

/**
 * "The Hex", its main menu, as the wiki's screens (The Hex/UI and its Weapon, Armor, Accessory and Miscellaneous
 * tabs) and NEU's Hex have it: glass all over, the item's slot in 22, eight panes around it that say what the Hex
 * makes of the item, the category buttons in the 3x3 block on the right (15-17, 24-26, 33-35), and Close in 49
 * (no Go Back). The item is put in and taken out of 22 as in a chest (see {@link HexScreen}); the panes and buttons
 * follow it on the next tick.
 *
 * <p>The panes: gray, "Give your life to The Hex!", with nothing in the slot; red, "The Hex is displeased!", for
 * an item it can't do anything with (not a SkyBlock item, or no category is for it); else purple, "Give your mind
 * to The Hex!" ("time" for an accessory), with every category's summary for the item. The buttons: the categories
 * that are for the item, in their order (see {@link HexCategories}). Main thread.
 */
public final class HexMenu extends HexScreen {
    public static final String TITLE = "The Hex";
    public static final int ITEM = 22;
    public static final int CLOSE = 49;
    static final int[] PANES = {12, 13, 14, 21, 23, 30, 31, 32};
    /** The 3x3 block the buttons go in, row by row. */
    static final int[] BUTTONS = {15, 16, 17, 24, 25, 26, 33, 34, 35};
    /** How wide the purple panes' first lines get, in pixels (see {@link #paneLore}). */
    static final int PANE_WIDTH = 154;
    static final List<String> FOOTER = List.of("&8The Hex - Your one-stop shop", "&8for personal refinement!");

    public HexMenu(HexSession session) {
        super(session, TITLE);
    }

    @Override
    public int inputSlot() {
        return ITEM;
    }

    /**
     * What the menu shows for an item: whether there's one in the slot, the SkyBlock item it is (null for a stack
     * that isn't one), the categories there are, and which Carpentry levels are met (all, on a Sandbox profile).
     */
    record View(boolean hasItem, HexItem item, List<HexCategory> categories, IntPredicate carpentry) {
        /** The categories with a button for the item, in order. */
        List<HexCategory> shown() {
            if (item == null) return List.of();
            List<HexCategory> shown = new ArrayList<>();
            for (HexCategory category : categories) if (category.applies(item)) shown.add(category);
            return shown;
        }
    }

    @Override
    protected void draw() {
        fill(filler());
        View view = new View(session.item() != null, session.hexItem(), HexCategories.all(),
                level -> HexRequirements.carpentry(viewer, level));
        Map<Integer, Icon> icons = icons(view);
        List<HexCategory> shown = view.shown();
        List<Integer> slots = buttonSlots(shown.size());
        for (Map.Entry<Integer, Icon> e : icons.entrySet()) {
            int slot = e.getKey();
            ItemStack stack = e.getValue().stack();
            int button = slots.indexOf(slot);
            if (button < 0) {
                set(slot, stack);
                continue;
            }
            HexCategory category = shown.get(button);
            set(button(slot, stack, () -> choose(category)));
        }
        set(GUIClickableItem.close(CLOSE));
    }

    /** A category's button: its page, if it's still for the item and they meet its requirement (else they're told what it is). */
    private void choose(HexCategory category) {
        HexItem item = session.hexItem();
        if (item == null || !category.applies(item)) {
            redraw();
            return;
        }
        if (!HexRequirements.carpentry(viewer, category.requiredCarpentry())) {
            say(HexRequirements.carpentryLine(category.requiredCarpentry()));
            return;
        }
        category.open(session);
    }

    // What it shows

    /** The panes and the buttons, by slot (the rest is glass, the item's slot and Close). */
    static Map<Integer, Icon> icons(View view) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        List<HexCategory> shown = view.shown();
        Icon pane = pane(view, shown);
        for (int slot : PANES) icons.put(slot, pane);
        List<Integer> slots = buttonSlots(shown.size());
        for (int i = 0; i < shown.size(); i++) {
            HexCategory category = shown.get(i);
            icons.put(slots.get(i), button(category, view.item(), view.carpentry().test(category.requiredCarpentry())));
        }
        return icons;
    }

    /**
     * Where {@code count} buttons go in the 3x3 block: as many columns as it takes at three a column, filled row
     * by row (the wiki's: five in 15, 16, 24, 25 and 33; one in 15). UNKNOWN (no screen shows them): other counts
     * (seven, for an item every category is for, in 15-17, 24-26 and 33), and where Item Upgrades and Gemstones sit.
     */
    static List<Integer> buttonSlots(int count) {
        int columns = Math.max(1, (count + 2) / 3);
        List<Integer> slots = new ArrayList<>(count);
        for (int row = 0; row < 3 && slots.size() < count; row++) {
            for (int column = 0; column < columns && slots.size() < count; column++) slots.add(BUTTONS[row * 3 + column]);
        }
        return slots;
    }

    /** The eight panes' item: gray with nothing, red for an item the Hex can't do anything with, else purple with the summary. */
    static Icon pane(View view, List<HexCategory> shown) {
        if (!view.hasItem()) {
            List<String> lore = new ArrayList<>(List.of("&7Upgrade an item with a variety", "&7of bells and whistles, all in", "&7one place!", ""));
            lore.addAll(FOOTER);
            lore.add("&d&kGive your life to The Hex!");
            return new Icon(Material.GRAY_STAINED_GLASS_PANE, "&d&kGive your life to The Hex!", lore);
        }
        if (shown.isEmpty()) return new Icon(Material.RED_STAINED_GLASS_PANE, "&cThe Hex is displeased!", "&7You cannot modify this item!");
        String name = "&d&kGive your " + (view.item().accessory() ? "time" : "mind") + " to The Hex!";
        List<List<String>> groups = new ArrayList<>();
        for (HexCategory category : shown) groups.add(category.summary(view.item()));
        return new Icon(Material.PURPLE_STAINED_GLASS_PANE, name, paneLore(view.item().name(), groups, name));
    }

    /**
     * The purple panes' lore: "Upgrade your <item> with a variety of bells and whistles, all in one place!" wrapped
     * (the official screenshot of the Fabled Livid Dagger ✪✪✪✪✪'s: "Upgrade your Fabled Livid / Dagger ✪✪✪✪✪ with a
     * / variety of bells and whistles, / all in one place!"), each category's group of lines with a blank line
     * between, then the footer and the name again. UNKNOWN: the exact wrapping; at 154 pixels (152 to 156 would
     * do), a star as wide as the game's font draws it, it wraps the screenshot's and the gray pane's lines as they
     * are.
     */
    static List<String> paneLore(String itemName, List<List<String>> groups, String name) {
        List<String> lore = new ArrayList<>(Text.wrap("&7Upgrade your " + itemName + " &7with a variety of bells and whistles, all in one place!",
                PANE_WIDTH, HexMenu::width));
        for (List<String> group : groups) {
            if (group.isEmpty()) continue;
            lore.add("");
            lore.addAll(group);
        }
        lore.add("");
        lore.addAll(FOOTER);
        lore.add(name);
        return lore;
    }

    /** Text#width, with a star (✪, from the game's Unicode font) 9 pixels wide rather than 6. */
    static int width(String text) {
        int stars = 0;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '✪') stars++;
        return Text.width(text) + 3 * stars;
    }

    /**
     * A category's button: "&a<name>", its description, a blank line, its summary for the item, a blank line and
     * "&eClick to view!"; the Carpentry requirement line in place of that while it isn't met (UNKNOWN how Hypixel
     * shows it: the button as it is, and the plugin's requirement line).
     */
    static Icon button(HexCategory category, HexItem item, boolean met) {
        List<String> lore = new ArrayList<>(category.description());
        List<String> summary = category.summary(item);
        if (!summary.isEmpty()) {
            lore.add("");
            lore.addAll(summary);
        }
        lore.add("");
        lore.add(met ? "&eClick to view!" : HexRequirements.carpentryLine(category.requiredCarpentry()));
        HexCategory.Look look = category.look();
        return new Icon(look.material(), "&a" + category.name(), lore, look.texture());
    }
}
