package net.icxd.dungeons.pet;

import org.bukkit.entity.Player;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.User;

/**
 * George's "Offer Pets", as recorded (2026-10-25, 04:04.3 to 04:07.7): five rows of glass, the pet's slot
 * ({@link PetSlotMenu#PET}), Offer a Pet under it, which becomes Accept Offer with a pet he buys ({@link
 * George#accept}), Close, and +1 Taming Level Cap beside it ({@link George#cap}). Accept Offer opens Confirm Sale
 * ({@link ConfirmPetMenu}); for a pet he doesn't buy, a barrier ({@link George#WONT_BUY}). With a pet he collects in
 * the slot, +1 Taming Level Cap gives it him, after Confirm Donation (UNKNOWN). Main thread.
 */
final class OfferPetsMenu extends PetSlotMenu {
    static final String TITLE = "Offer Pets";
    static final int OFFER = 22;
    static final int CLOSE = 40;
    static final int CAP = 41;

    OfferPetsMenu(Player viewer, PlacedPet placed) {
        super(TITLE, Size.FIVE, viewer, placed);
    }

    @Override
    void draw() {
        set(GUIClickableItem.close(CLOSE));
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        PetData data = Pets.data();
        boolean donatable = placed != null && George.wanted(user.profile(), placed.pet()) != null;
        set(GUIClickableItem.button(CAP, George.cap(user.profile(), data, donatable).stack(), viewer, () -> {
            if (donatable && placed != null && placed.ownedBy(this)) confirm(George.CONFIRM_DONATION, George.confirmDonation(placed.pet(), data), true);
        }));
        if (placed == null) {
            set(OFFER, George.OFFER_A_PET.stack());
            return;
        }
        long price = George.price(placed.pet(), data);
        if (price <= 0) {
            set(OFFER, George.WONT_BUY.stack());
            return;
        }
        set(GUIClickableItem.button(OFFER, George.accept(price).stack(), viewer, () -> {
            if (placed != null && placed.ownedBy(this)) confirm(George.CONFIRM_SALE, George.confirmSale(placed.pet(), data, price), false);
        }));
    }

    /** Confirm Sale (as recorded, with no sound), or Confirm Donation. */
    private void confirm(String title, Icon icon, boolean donation) {
        PlacedPet pet = placed;
        Runnable confirmed = donation ? () -> George.donate(Dungeons.getInstance(), viewer, pet) : () -> George.sell(viewer, pet);
        new ConfirmPetMenu(title, viewer, pet, icon, confirmed, () -> new OfferPetsMenu(viewer, pet)).open(viewer, null);
    }
}
