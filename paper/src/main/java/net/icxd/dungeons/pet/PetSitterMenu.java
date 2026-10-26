package net.icxd.dungeons.pet;

import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.user.User;

/**
 * Kat's "Pet Sitter", as recorded (2026-10-25, 00:29.5 to 01:01.1): five rows of glass, the pet's slot
 * ({@link PetSlotMenu#PET}), Pet Sitter under it ("&7Place a pet above for Kat to take" ...), which becomes Hire Kat
 * with a pet she upgrades (see {@link Kat#hire(Kat.Offer, PetData, String)}), and Close. Hire Kat in green opens
 * Confirm Hiring Kat; in red it does nothing (UNKNOWN). For a pet she doesn't upgrade, a barrier ({@link
 * Kat#CANT_UPGRADE}). Main thread.
 */
final class PetSitterMenu extends PetSlotMenu {
    static final String TITLE = "Pet Sitter";
    static final int HIRE = 22;
    static final int CLOSE = 40;

    PetSitterMenu(Player viewer, PlacedPet placed) {
        super(TITLE, Size.FIVE, viewer, placed);
    }

    @Override
    void draw() {
        set(GUIClickableItem.close(CLOSE));
        if (placed == null) {
            set(HIRE, Kat.PET_SITTER.stack());
            return;
        }
        PetData data = Pets.data();
        Kat.Offer offer = Kat.offer(placed.pet(), data);
        User user = User.ifLoaded(viewer.getUniqueId());
        if (offer == null || user == null) {
            set(HIRE, Kat.CANT_UPGRADE.stack());
            return;
        }
        String refusal = Kat.refusal(viewer, user, offer);
        set(GUIClickableItem.button(HIRE, Kat.hire(offer, data, refusal).stack(), viewer, () -> {
            // As recorded, with no sound.
            if (refusal == null && placed != null && placed.ownedBy(this)) new ConfirmHiringKatMenu(viewer, placed).open(viewer, null);
        }));
    }
}
