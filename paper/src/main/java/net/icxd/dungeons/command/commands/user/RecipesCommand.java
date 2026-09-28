package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.recipe.RecipeBook;

/** {@code /recipes}: the Recipe Book ("Also accessible via /recipes.", its item says; the other names are the wiki's Commands). */
@CommandParameters(aliases = "recipebook,viewrecipebook,recipemenu", description = "Opens the Recipe Book")
public class RecipesCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        new RecipeBook(source.getPlayer()).open(source.getPlayer());
    }
}
