package net.icxd.dungeons.collection;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.icxd.dungeons.recipe.RecipeData;
import net.icxd.dungeons.recipe.Recipes;

/**
 * The collections and recipes are Hypixel's, kept in the private data folder ({@code collections/}, which
 * servermgr links into the plugin's data folder like {@code items/}): read off the main thread when the
 * plugin starts, what was wrong logged. Until they're in, and without them, there are no collections or
 * recipes (the menus show none).
 */
public final class CollectionFiles {
    private record Loaded(CollectionData collections, RecipeData recipes) {
    }

    private CollectionFiles() {
    }

    public static void load(JavaPlugin plugin) {
        Path folder = plugin.getDataFolder().toPath().resolve(CollectionData.FOLDER);
        Logger log = plugin.getLogger();
        CompletableFuture.supplyAsync(() -> new Loaded(CollectionData.load(folder), RecipeData.load(folder))).whenComplete((loaded, error) -> {
            if (error != null) {
                log.log(Level.SEVERE, "Couldn't read the collections in " + folder, error);
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                Collections.setData(loaded.collections());
                Recipes.use(loaded.recipes());
                loaded.collections().problems().forEach(p -> log.warning("Collections: " + p));
                loaded.recipes().problems().forEach(p -> log.warning("Recipes: " + p));
                log.info("Collections: " + loaded.collections().size() + " collections, " + loaded.collections().bosses().size()
                        + " boss collections, " + loaded.recipes().recipes().size() + " recipes, " + loaded.recipes().book().size()
                        + " Recipe Book entries");
            });
        });
    }
}
