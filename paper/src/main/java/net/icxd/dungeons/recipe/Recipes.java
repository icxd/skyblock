package net.icxd.dungeons.recipe;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.profile.Profiles;
import net.icxd.dungeons.recipe.RecipeData.BookEntry;
import net.icxd.dungeons.recipe.RecipeData.Kind;
import net.icxd.dungeons.recipe.RecipeData.Recipe;
import net.icxd.dungeons.recipe.RecipeData.Requirement;

/**
 * The recipes there are ({@link #data}, loaded with the collections when the plugin starts) and which of
 * them a profile has unlocked: a recipe is unlocked once its collection tier is reached ({@link Collections}),
 * or from the start if nothing unlocks it. A slayer's recipes stay locked (there are no slayers yet), and so
 * do the ones waiting for a system the plugin doesn't have (minions, pets). A Sandbox profile, which collects
 * nothing (see {@link Collections#counts}), has every recipe but the waiting ones: a sandbox is for trying
 * things (UNKNOWN: no owner decision covers recipes). No server needed.
 */
public final class Recipes {
    private static volatile RecipeData data = RecipeData.empty();
    private static volatile Crafting.Index index = new Crafting.Index(List.of());

    private Recipes() {
    }

    public static RecipeData data() {
        return data;
    }

    /** The recipes there are from now on (the plugin's, once read: see CollectionFiles). */
    public static void use(RecipeData loaded) {
        RecipeData d = loaded == null ? RecipeData.empty() : loaded;
        index = new Crafting.Index(d.recipes().values());
        data = d;
    }

    /** Recipes by the items they take. */
    public static Crafting.Index index() {
        return index;
    }

    /** Whether the profile has what unlocks it. */
    public static boolean unlocked(Document profile, Requirement requires) {
        if (requires == null || requires.none() || Profiles.mode(profile) == ProfileMode.SANDBOX) return true;
        if (requires.collection() != null) return Collections.tier(profile, requires.collection()) >= requires.tier();
        // Slayers, museum donations and the rest aren't in the plugin yet.
        return false;
    }

    /** Whether the profile can craft it: unlocked, and nothing it waits for (a minion's recipe waits for minions). */
    public static boolean unlocked(Document profile, Recipe recipe) {
        return recipe != null && recipe.later() == null && unlocked(profile, recipe.requires());
    }

    /** Whether a Recipe Book entry shows as unlocked: only a craftable one can be. */
    public static boolean unlocked(Document profile, BookEntry entry) {
        if (entry.kind() != Kind.RECIPE) return false;
        Recipe recipe = data.recipe(entry.item());
        return recipe != null && recipe.later() == null && unlocked(profile, entry.requires());
    }

    /** A category's entries (the Recipe Book's "FARMING" or "SLAYER"). */
    public static List<BookEntry> category(String category) {
        List<BookEntry> entries = new ArrayList<>();
        for (BookEntry entry : data.book()) if (entry.category().equals(category)) entries.add(entry);
        return entries;
    }

    /** How many of the entries the profile has unlocked. */
    public static int unlocked(Document profile, List<BookEntry> entries) {
        int unlocked = 0;
        for (BookEntry entry : entries) if (unlocked(profile, entry)) unlocked++;
        return unlocked;
    }
}
