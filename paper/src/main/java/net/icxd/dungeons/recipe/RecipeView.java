package net.icxd.dungeons.recipe;

import java.util.Map;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.collection.CollectionGUI;
import net.icxd.dungeons.collection.MenuSlot;
import net.icxd.dungeons.menu.SkyBlockMenu;

/**
 * An item's recipe ("Enchanted Bone Recipe"), laid out as the wiki's Crafting UI has it (the recording
 * opened none): the grid, the crafting table that makes it, the result. {@code /viewrecipe} shows it
 * whether it's unlocked or not, as on Hypixel (the wiki's Commands). Go Back goes where it was opened from, and
 * says so with that menu's title ("To (1/4) Combat Recipes"; UNKNOWN from the Recipe Book, where none was
 * opened: the wiki's Collection UI names the Rewards menu it came from). Main thread.
 */
public final class RecipeView extends CollectionGUI {
    private final String item;
    private final Runnable back;
    private final String backTo;

    /** {@code back}: what Go Back opens ({@code backTo}, its title); null for the SkyBlock Menu. */
    public RecipeView(Player viewer, String item, Runnable back, String backTo) {
        super(RecipeMenus.viewTitle(item), viewer);
        this.item = item;
        this.back = back;
        this.backTo = back == null ? SkyBlockMenu.TITLE : backTo;
    }

    @Override
    protected Map<Integer, MenuSlot> slots(Document profile) {
        Map<Integer, MenuSlot> slots = RecipeMenus.view(item, backTo);
        return slots == null ? Map.of() : slots;
    }

    @Override
    protected void buttons(Document profile) {
        on(RecipeMenus.BACK, back != null ? back : () -> new SkyBlockMenu(viewer).open(viewer));
    }
}
