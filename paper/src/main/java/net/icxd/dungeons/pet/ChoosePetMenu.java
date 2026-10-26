package net.icxd.dungeons.pet;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.pet.PetText.Where;
import net.icxd.dungeons.user.User;

/**
 * Choosing a pet for an Exp Share Slot (never recorded, UNKNOWN): their pets as the Pets menu has them (28 a page, in
 * its sort), "&eClick to choose!" under each; a click puts it in the slot (out of another it was in) and goes back to
 * Exp Sharing. Main thread.
 */
final class ChoosePetMenu extends GUI {
    static final String TITLE = "Choose a Pet";
    static final int PREVIOUS = 45;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int NEXT = 53;

    private final Player viewer;
    private final int slot;
    private final int page;

    private ChoosePetMenu(Player viewer, int slot, int page, int pages) {
        super(pages > 1 ? "(" + page + "/" + pages + ") " + TITLE : TITLE, Size.SIX);
        this.viewer = viewer;
        this.slot = slot;
        this.page = page;
    }

    /** Its page (1 first) for Exp Share Slot {@code slot} (0 for #1). */
    static void show(Player player, int slot, int page) {
        User user = User.ifLoaded(player.getUniqueId());
        int pages = PetsMenu.pages(user == null ? 0 : Pets.pets(user.profile()).size());
        new ChoosePetMenu(player, slot, Math.max(1, Math.min(page, pages)), pages).open(player);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        for (int s = 0; s < getSize(); s++) {
            boolean inside = s >= 9 && s < 45 && s % 9 != 0 && s % 9 != 8;
            if (!inside) set(s, filler());
        }
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        PetData data = Pets.data();
        List<OwnedPet> pets = Pets.sorted(Pets.pets(user.profile()), Pets.sort(user.profile()), data);
        int pages = PetsMenu.pages(pets.size());
        for (int i = 0; i < PetsMenu.PER_PAGE; i++) {
            int index = (page - 1) * PetsMenu.PER_PAGE + i;
            if (index >= pets.size()) break;
            OwnedPet pet = pets.get(index);
            set(GUIClickableItem.button(PetsMenu.slot(i), PetsMenu.stack(pet, data, Where.CHOOSE), viewer, () -> choose(pet.uuid())));
        }
        if (page > 1) set(GUIClickableItem.button(PREVIOUS, PetsMenu.pageItem("&aPrevious Page", page - 1).stack(), viewer, () -> show(viewer, slot, page - 1)));
        if (page < pages) set(GUIClickableItem.button(NEXT, PetsMenu.pageItem("&aNext Page", page + 1).stack(), viewer, () -> show(viewer, slot, page + 1)));
        set(GUIClickableItem.button(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Exp Sharing").stack(), viewer,
                () -> new ExpSharingMenu(viewer).open(viewer, MenuSound.CLICK)));
        set(GUIClickableItem.close(CLOSE));
    }

    private void choose(String uuid) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || user.isReleased() || Pets.find(user.profile(), uuid) == null) return;
        ExpSharing.put(user.profile(), slot, uuid);
        new ExpSharingMenu(viewer).open(viewer, MenuSound.CLICK);
    }
}
