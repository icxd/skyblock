package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.pet.PetText.Where;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * "Exp Sharing", from the Pets menu, as recorded (2026-10-25, 03:18.6): six rows of glass, Exp Sharing at the top,
 * the three Exp Share Slots ("&c&lINACTIVE"; #2 and #3 "&c&lDISABLED" "&8Enabled by a Mayor Diana perk.") over what's
 * in them ("&7No pet in slot" "&eClick to choose a pet!"), Go Back ("&7To Pets") and Close. Never recorded with a pet
 * in a slot (UNKNOWN): the slot in green, "&a&lACTIVE" with its Exp Share rate, and the pet under it, a click taking
 * it out; choosing is {@link ChoosePetMenu}. See {@link ExpSharing}. Main thread.
 */
final class ExpSharingMenu extends GUI {
    static final String TITLE = "Exp Sharing";
    static final int INFO = 4;
    /** Slot #1's; #2 and #3 follow. */
    static final int FIRST_SLOT = 21;
    static final int FIRST_PET = 30;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final List<String> ABOUT = List.of("&7Let your pets gain &aPet Exp &7without", "&7them being actively deployed!");

    private final Player viewer;

    ExpSharingMenu(Player viewer) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Document profile = user.profile();
        PetData data = Pets.data();
        set(INFO, new Icon(Material.YELLOW_DYE, "&6Exp Sharing", ABOUT).stack());
        List<OwnedPet> slots = ExpSharing.slots(profile);
        int taming = Skills.level(profile, Skill.TAMING);
        for (int i = 0; i < ExpSharing.SLOTS; i++) {
            OwnedPet pet = slots.get(i);
            set(FIRST_SLOT + i, slot(i, pet, taming).stack());
            int index = i;
            if (pet == null) {
                set(GUIClickableItem.button(FIRST_PET + i, EMPTY.stack(), viewer, () -> choose(viewer, index)));
            } else {
                set(GUIClickableItem.button(FIRST_PET + i, PetsMenu.stack(pet, data, Where.EXP_SHARE), viewer, () -> remove(viewer, index)));
            }
        }
        set(GUIClickableItem.button(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Pets").stack(), viewer,
                () -> PetsMenu.show(viewer, 1, MenuSound.CLICK)));
        set(GUIClickableItem.close(CLOSE));
    }

    static final Icon EMPTY = new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7No pet in slot", "&eClick to choose a pet!");

    /** An Exp Share Slot (0 for #1), as recorded empty; with a pet, UNKNOWN. */
    static Icon slot(int slot, OwnedPet pet, int taming) {
        List<String> lore = new ArrayList<>(ABOUT);
        lore.add("");
        String name = "Exp Share Slot #" + (slot + 1);
        if (!ExpSharing.active(slot)) {
            lore.add("&c&lDISABLED");
            lore.add("&8Enabled by a Mayor Diana perk.");
            return new Icon(Material.RED_STAINED_GLASS_PANE, "&c" + name, lore);
        }
        if (pet == null) {
            lore.add("&c&lINACTIVE");
            return new Icon(Material.RED_STAINED_GLASS_PANE, "&c" + name, lore);
        }
        lore.add("&7Exp Share rate: &a" + Text.number(ExpSharing.rate(taming, pet.heldItem())) + "%");
        lore.add("");
        lore.add("&a&lACTIVE");
        return new Icon(Material.LIME_STAINED_GLASS_PANE, "&a" + name, lore);
    }

    /** Choosing a pet for a slot ({@link ChoosePetMenu}), out of another it was in, then back here. */
    private static void choose(Player player, int slot) {
        ChoosePetMenu.show(player, 1, "&7To Exp Sharing", () -> new ExpSharingMenu(player).open(player, MenuSound.CLICK), uuid -> {
            User user = User.ifLoaded(player.getUniqueId());
            if (user == null || user.isReleased()) return;
            ExpSharing.put(user.profile(), slot, uuid);
            new ExpSharingMenu(player).open(player, MenuSound.CLICK);
        });
    }

    private static void remove(Player player, int slot) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        ExpSharing.put(user.profile(), slot, null);
        new ExpSharingMenu(player).open(player, null);
    }
}
