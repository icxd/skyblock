package net.icxd.dungeons.pet;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.leveling.SkyBlockLevels;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * What a player does with their pets, as recorded (2026-10-25): summoning one ("&aYou summoned your &6Lion&a!", an egg's
 * pop, the menu closing), despawning it, making one a favourite, making one an item again, and adding a pet item to the
 * menu ("&aSuccessfully added &fBee &ato your pet menu!", and a level-up when the Pet Score goes up). Main thread.
 */
public final class PetActions {
    private PetActions() {
    }

    /** A click on a pet in the menu: summons it, or despawns it if it's the one out (its line then is "Click to despawn!"). */
    static void summonOrDespawn(Player player, User user, OwnedPet pet) {
        Document profile = user.profile();
        PetData data = Pets.data();
        OwnedPet active = Pets.active(profile);
        player.closeInventory();
        if (active != null && active.uuid().equals(pet.uuid())) {
            Pets.activate(profile, null);
            // UNKNOWN: what despawning says (never recorded); this, and the summon's pop.
            player.sendMessage(Text.line("&aYou despawned your " + PetText.colored(pet, data) + "&a!"));
        } else {
            Pets.activate(profile, pet.uuid());
            player.sendMessage(Text.line("&aYou summoned your " + PetText.colored(pet, data) + "&a!"));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, SoundCategory.NEUTRAL, 1, 1);
        PetFollowers.refresh(player);
        PlayerSession.of(player).invalidateStats();
        user.save();
    }

    /** Shift-click: a favourite now (first in the menu, "&e⭐ " before its name), or not. */
    static void favorite(Player player, User user, OwnedPet pet) {
        Pets.replace(user.profile(), pet.withFavorite(!pet.favorite()));
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.NEUTRAL, 1, 1);
    }

    /**
     * Right-click: out of the menu and into their inventory as an item (despawned if it was out). The Pet Score goes
     * down; the SkyBlock XP it gave stays. With no room, UNKNOWN what Hypixel does: it stays in the menu.
     */
    static void toItem(Player player, User user, OwnedPet pet) {
        if (player.getInventory().firstEmpty() < 0) {
            player.sendMessage(Text.line("&cYour inventory is full!"));
            return;
        }
        Document profile = user.profile();
        if (Pets.remove(profile, pet.uuid()) == null) return;
        Map<Integer, ItemStack> left = new HashMap<>(player.getInventory().addItem(PetItems.item(pet.withFavorite(false))));
        if (!left.isEmpty()) {
            Pets.add(profile, pet);
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.NEUTRAL, 1, 1);
        PetFollowers.refresh(player);
        PlayerSession.of(player).invalidateStats();
        user.save();
    }

    /** A pet item right-clicked: into the menu, as recorded; false (and it stays an item) if the menu is full. */
    static boolean add(Player player, User user, OwnedPet pet) {
        Document profile = user.profile();
        PetData data = Pets.data();
        if (!Pets.add(profile, pet)) {
            // UNKNOWN: what a full menu says.
            player.sendMessage(Text.line("&cYour pet menu is full!"));
            return false;
        }
        boolean higher = Pets.raiseBest(profile, data);
        if (higher) player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1, 1);
        player.sendMessage(Text.line("&aSuccessfully added " + PetText.colored(pet, data) + " &ato your pet menu!"));
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.MASTER, 1, 1);
        PlayerSession.of(player).invalidateStats();
        user.save();
        if (higher) SkyBlockLevels.changed(player);
        return true;
    }
}
