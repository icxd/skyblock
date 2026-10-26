package net.icxd.dungeons.pet;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.pet.PetText.Where;
import net.icxd.dungeons.user.User;

/**
 * Choosing one of their pets (for an Exp Share Slot, an Autopet rule; never recorded, UNKNOWN): their pets as the Pets
 * menu has them (28 a page, in its sort), "&eClick to choose!" under each; a click chooses it. Main thread.
 */
final class ChoosePetMenu extends GUI {
    static final String TITLE = "Choose a Pet";
    static final int PREVIOUS = 45;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int NEXT = 53;

    private final Player viewer;
    private final int page;
    private final String backTo;
    private final Runnable back;
    private final Consumer<String> chosen;

    private ChoosePetMenu(Player viewer, int page, int pages, String backTo, Runnable back, Consumer<String> chosen) {
        super(pages > 1 ? "(" + page + "/" + pages + ") " + TITLE : TITLE, Size.SIX);
        this.viewer = viewer;
        this.page = page;
        this.backTo = backTo;
        this.back = back;
        this.chosen = chosen;
    }

    /** Its page (1 first); Go Back says {@code backTo} ("&7To Exp Sharing") and does {@code back}; {@code chosen} gets the pet's uuid. */
    static void show(Player player, int page, String backTo, Runnable back, Consumer<String> chosen) {
        User user = User.ifLoaded(player.getUniqueId());
        int pages = PetsMenu.pages(user == null ? 0 : Pets.pets(user.profile()).size());
        new ChoosePetMenu(player, Math.max(1, Math.min(page, pages)), pages, backTo, back, chosen).open(player);
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
        if (page > 1) {
            set(GUIClickableItem.button(PREVIOUS, PetsMenu.pageItem("&aPrevious Page", page - 1).stack(), viewer,
                    () -> show(viewer, page - 1, backTo, back, chosen)));
        }
        if (page < pages) {
            set(GUIClickableItem.button(NEXT, PetsMenu.pageItem("&aNext Page", page + 1).stack(), viewer,
                    () -> show(viewer, page + 1, backTo, back, chosen)));
        }
        set(GUIClickableItem.button(BACK, new Icon(Material.ARROW, "&aGo Back", backTo).stack(), viewer, back));
        set(GUIClickableItem.close(CLOSE));
    }

    private void choose(String uuid) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || user.isReleased() || Pets.find(user.profile(), uuid) == null) return;
        chosen.accept(uuid);
    }
}
