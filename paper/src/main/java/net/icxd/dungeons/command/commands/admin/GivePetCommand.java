package net.icxd.dungeons.command.commands.admin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.pet.OwnedPet;
import net.icxd.dungeons.pet.PetData;
import net.icxd.dungeons.pet.PetItems;
import net.icxd.dungeons.pet.Pets;

/**
 * "/givepet BEE [RARITY] [LEVEL] [XP]": a pet as an item, at its lowest rarity unless one's given, level 1 unless one's
 * given, so much XP into it. Staff only (not one of the Sandbox tools, which are the owner's list).
 */
@CommandParameters(description = "Gives a pet as an item", permission = Rank.STAFF)
public class GivePetCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();
        if (player == null) return;
        PetData data = Pets.data();
        PetData.Pet pet = args.length == 0 ? null : data.pet(args[0]);
        if (pet == null) {
            send("&cUsage: /givepet <pet> [rarity] [level] [xp]");
            return;
        }
        Rarity rarity = pet.variants().keySet().iterator().next();
        if (args.length > 1) {
            try {
                rarity = Rarity.valueOf(args[1].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                send("&cNo such rarity: " + args[1]);
                return;
            }
            if (!pet.variants().containsKey(rarity)) {
                send("&cThat pet has no " + rarity.name().toLowerCase(Locale.ROOT) + " rarity.");
                return;
            }
        }
        int level = 1;
        if (args.length > 2) {
            try {
                level = Math.max(1, Math.min(data.maxLevel(pet), Integer.parseInt(args[2])));
            } catch (NumberFormatException e) {
                send("&cNot a level: " + args[2]);
                return;
            }
        }
        double extra = 0;
        if (args.length > 3) {
            try {
                extra = Math.max(0, Double.parseDouble(args[3]));
            } catch (NumberFormatException e) {
                send("&cNot an amount of XP: " + args[3]);
                return;
            }
        }
        if (InventorySyncListener.frozen(player)) {
            send("&cYou can't take items right now.");
            return;
        }
        OwnedPet given = OwnedPet.of(pet.id(), rarity).withExp(data.totalXp(pet, rarity, level) + extra);
        player.getInventory().addItem(PetItems.item(given));
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        PetData data = Pets.data();
        if (args.length <= 1) return data.pets().stream().map(PetData.Pet::id).toList();
        if (args.length == 2) {
            PetData.Pet pet = data.pet(args[0]);
            return pet == null ? List.of() : pet.variants().keySet().stream().map(Rarity::name).toList();
        }
        return Arrays.asList("1", "50", "100");
    }
}
