package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Pet Food's confirmation (Hypixel's since 2023, never recorded: UNKNOWN, made as the recorded Confirm Sale is): three
 * rows with nothing in them but Confirm (what it feeds, the XP, and for Carrot Candy the pet's count after) and
 * Cancel. Main thread.
 */
final class FeedPetMenu extends GUI {
    static final String TITLE = "Feed Pet";
    static final int CONFIRM = 11;
    static final int CANCEL = 15;

    private final Player viewer;
    private final PetFood.Food food;

    FeedPetMenu(Player viewer, PetFood.Food food) {
        super(TITLE, Size.THREE);
        this.viewer = viewer;
        this.food = food;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(ItemStack.empty());
        User user = User.ifLoaded(viewer.getUniqueId());
        OwnedPet pet = user == null ? null : Pets.active(user.profile());
        if (pet != null) {
            double xp = PetFood.xp(food, pet, Skills.level(user.profile(), Skill.TAMING));
            set(GUIClickableItem.button(CONFIRM, confirm(pet, Pets.data(), food, xp).stack(), viewer, () -> PetFood.feed(viewer, food)));
        }
        set(GUIClickableItem.button(CANCEL, ConfirmPetMenu.CANCEL_ICON.stack(), viewer, () -> viewer.closeInventory()));
    }

    static Icon confirm(OwnedPet pet, PetData data, PetFood.Food food, double xp) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Feed: " + food.name());
        lore.add("&7To: " + PetText.colored(pet, data));
        lore.add("&7Grants: &e" + Text.number(Math.floor(xp)) + " Pet EXP");
        if (food.candy()) lore.add("&7Candy used: &a" + (pet.candyUsed() + 1) + "&7/&a" + PetFood.MOST_CANDY);
        return new Icon(Material.GREEN_TERRACOTTA, "&aConfirm", lore);
    }

    @Override
    public boolean onPlayerInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        return true;
    }
}
