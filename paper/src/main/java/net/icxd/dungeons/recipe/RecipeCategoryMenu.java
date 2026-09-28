package net.icxd.dungeons.recipe;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.collection.CollectionGUI;
import net.icxd.dungeons.collection.MenuSlot;
import net.icxd.dungeons.recipe.RecipeData.BookEntry;
import net.icxd.dungeons.user.User;

/**
 * A page of a category's recipes ("(1/4) Combat Recipes", recorded 01:11.7-01:13.3): an unlocked one opens
 * its recipe, the arrows turn the page. Main thread.
 */
public final class RecipeCategoryMenu extends CollectionGUI {
    private final RecipeMenus.Category category;
    private final int page;

    private RecipeCategoryMenu(Player viewer, RecipeMenus.Category category, int page, int pages) {
        super(RecipeMenus.pageTitle(category, page, pages), viewer);
        this.category = category;
        this.page = page;
    }

    /** Opens page {@code page} of it (the title counts the pages, so they're counted first). */
    public static void open(Player viewer, RecipeMenus.Category category, int page) {
        User user = User.ifLoaded(viewer.getUniqueId());
        int entries = user == null ? 0 : RecipeMenus.ordered(user.profile(), category.id()).size();
        int pages = RecipeMenus.pages(entries);
        int shown = Math.max(1, Math.min(page, pages));
        new RecipeCategoryMenu(viewer, category, shown, pages).open(viewer);
    }

    @Override
    protected Map<Integer, MenuSlot> slots(Document profile) {
        return RecipeMenus.page(profile, category, page);
    }

    @Override
    protected void buttons(Document profile) {
        List<BookEntry> entries = RecipeMenus.ordered(profile, category.id());
        int from = (page - 1) * RecipeMenus.INSIDE.length;
        for (int i = 0; i < RecipeMenus.INSIDE.length && from + i < entries.size(); i++) {
            BookEntry entry = entries.get(from + i);
            if (!Recipes.unlocked(profile, entry)) continue;
            on(RecipeMenus.INSIDE[i], () -> new RecipeView(viewer, entry.item(), () -> open(viewer, category, page), getTitle()).open(viewer));
        }
        int pages = RecipeMenus.pages(entries.size());
        if (page > 1) on(RecipeMenus.PREVIOUS, () -> open(viewer, category, page - 1));
        if (page < pages) on(RecipeMenus.NEXT, () -> open(viewer, category, page + 1));
        on(RecipeMenus.BACK, () -> new RecipeBook(viewer).open(viewer));
    }
}
