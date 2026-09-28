package net.icxd.dungeons.recipe;

import java.util.Map;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.collection.CollectionGUI;
import net.icxd.dungeons.collection.MenuSlot;
import net.icxd.dungeons.menu.SkyBlockMenu;

/**
 * The Recipe Book ({@code /recipes}, or the SkyBlock Menu's book), as recorded (01:08.0): each category opens
 * its first page. Trades and Search Recipes do nothing yet (LATER). Main thread.
 */
public final class RecipeBook extends CollectionGUI {
    public static final String TITLE = "Recipe Book";

    public RecipeBook(Player viewer) {
        super(TITLE, viewer);
    }

    @Override
    protected Map<Integer, MenuSlot> slots(Document profile) {
        return RecipeMenus.book(profile);
    }

    @Override
    protected void buttons(Document profile) {
        for (RecipeMenus.Category category : RecipeMenus.CATEGORIES) {
            on(category.slot(), () -> RecipeCategoryMenu.open(viewer, category, 1));
        }
        on(RecipeMenus.BACK, () -> new SkyBlockMenu(viewer).open(viewer));
    }
}
