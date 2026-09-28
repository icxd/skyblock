package net.icxd.dungeons.command.commands.user;

import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandSender;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.recipe.RecipeView;
import net.icxd.dungeons.recipe.Recipes;

/**
 * {@code /viewrecipe <item id>}: an item's recipe, "works also when item is not unlocked" (the wiki's
 * Commands). What Hypixel says for an item with none is UNKNOWN.
 */
@CommandParameters(description = "Shows an item's recipe", usage = "/<command> <item id>")
public class ViewRecipeCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        String id = args.length == 0 ? "" : args[0].toUpperCase(Locale.ROOT);
        if (Recipes.data().recipe(id) == null) {
            send("&cThere's no recipe for " + (args.length == 0 ? "that" : args[0]) + "!");
            return;
        }
        new RecipeView(source.getPlayer(), id, null, null).open(source.getPlayer());
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        String typed = args.length == 0 ? "" : args[args.length - 1].toUpperCase(Locale.ROOT);
        if (typed.isEmpty()) return List.of();
        return Recipes.data().recipes().keySet().stream().filter(id -> id.startsWith(typed)).limit(100).toList();
    }
}
