package net.icxd.dungeons.bank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.Function;

import org.bson.Document;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.bank.BankAccount.Result;
import net.icxd.dungeons.bank.BankMenus.Layout;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.RefreshingGUI;
import net.icxd.dungeons.gui.SignInput;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.quest.QuestProgress;
import net.icxd.dungeons.quest.Quests;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * The bank's menus (see {@link BankMenus} for what they show), as they went in the recording: each
 * opens the next with a click, as menus do; a deposit or withdrawal says it's being made, chimes (a
 * pling, loud and high), says what it did and opens the account again, with a click. A refusal leaves
 * the menu open, silently. The first deposit completes Saving Up. Their amounts and countdown are kept
 * up to date while they're open. Main thread.
 */
public final class BankMenu extends GUI implements RefreshingGUI {
    /** The chime, as recorded: a pling at volume 8 and Hypixel's highest pitch (255/63, which plays as 2). */
    static final float CHIME_VOLUME = 8;
    static final float CHIME_PITCH = 255 / 63f;

    private final Player viewer;
    private final Function<Document, Layout> layout;
    private final Map<Integer, Runnable> buttons = new HashMap<>();

    private BankMenu(String title, Player viewer, Function<Document, Layout> layout) {
        super(title, Size.FOUR);
        this.viewer = viewer;
        this.layout = layout;
    }

    /** The Personal Bank Account, with this sound (null for none: the Banker speaks as it opens). */
    public static void account(Player viewer, MenuSound sound) {
        BankMenu menu = new BankMenu(BankMenus.ACCOUNT, viewer, profile -> BankMenus.account(profile, System.currentTimeMillis()));
        menu.buttons.put(BankMenus.DEPOSIT_COINS, () -> deposit(viewer));
        menu.buttons.put(BankMenus.WITHDRAW_COINS, () -> withdrawal(viewer));
        menu.buttons.put(BankMenus.CLOSE, viewer::closeInventory);
        menu.open(viewer, sound);
    }

    static void deposit(Player viewer) {
        BankMenu menu = new BankMenu(BankMenus.DEPOSIT, viewer, BankMenus::deposit);
        menu.buttons.put(BankMenus.WHOLE_PURSE, () -> deposit(viewer, BankAccount.wholePurse(profile(viewer))));
        menu.buttons.put(BankMenus.HALF_PURSE, () -> deposit(viewer, BankAccount.halfPurse(profile(viewer))));
        menu.buttons.put(BankMenus.DEPOSIT_AMOUNT, () -> typed(viewer, BankMenus.DEPOSIT_SIGN, amount -> deposit(viewer, amount)));
        menu.buttons.put(BankMenus.BACK, () -> account(viewer, MenuSound.CLICK));
        menu.open(viewer, MenuSound.CLICK);
    }

    static void withdrawal(Player viewer) {
        BankMenu menu = new BankMenu(BankMenus.WITHDRAWAL, viewer, BankMenus::withdrawal);
        menu.buttons.put(BankMenus.EVERYTHING, () -> withdraw(viewer, BankAccount.everything(profile(viewer))));
        menu.buttons.put(BankMenus.HALF, () -> withdraw(viewer, BankAccount.half(profile(viewer))));
        menu.buttons.put(BankMenus.FIFTH, () -> withdraw(viewer, BankAccount.fifth(profile(viewer))));
        menu.buttons.put(BankMenus.WITHDRAW_AMOUNT, () -> typed(viewer, BankMenus.WITHDRAW_SIGN, amount -> withdraw(viewer, amount)));
        menu.buttons.put(BankMenus.BACK, () -> account(viewer, MenuSound.CLICK));
        menu.open(viewer, MenuSound.CLICK);
    }

    private static Document profile(Player viewer) {
        User user = User.ifLoaded(viewer.getUniqueId());
        return user == null ? new Document() : user.profile();
    }

    private static void deposit(Player viewer, double amount) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || user.isReleased()) return;
        Result result = BankAccount.deposit(user.profile(), amount, viewer.getUniqueId(), System.currentTimeMillis());
        if (!result.done()) {
            viewer.sendMessage(Text.line(BankMenus.refusal(result.refusal(), true)));
            return;
        }
        viewer.sendMessage(Text.line(BankMenus.DEPOSITING));
        QuestProgress.complete(viewer, Quests.SAVING_UP, Quests.DEPOSIT_COINS);
        done(viewer, user, BankMenus.deposited(result.amount(), BankAccount.balance(user.profile())));
    }

    private static void withdraw(Player viewer, double amount) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || user.isReleased()) return;
        Result result = BankAccount.withdraw(user.profile(), amount, viewer.getUniqueId(), System.currentTimeMillis());
        if (!result.done()) {
            viewer.sendMessage(Text.line(BankMenus.refusal(result.refusal(), false)));
            return;
        }
        viewer.sendMessage(Text.line(BankMenus.WITHDRAWING));
        done(viewer, user, BankMenus.withdrawn(result.amount(), BankAccount.balance(user.profile())));
    }

    private static void done(Player viewer, User user, String said) {
        user.save();
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, CHIME_VOLUME, CHIME_PITCH);
        viewer.sendMessage(Text.line(said));
        account(viewer, MenuSound.CLICK);
    }

    /**
     * Asks how much on a sign (the menu closes first, so the server doesn't think it's still open);
     * nothing typed does nothing (UNKNOWN on Hypixel), and what isn't an amount says so.
     */
    private static void typed(Player viewer, List<String> sign, DoubleConsumer then) {
        viewer.closeInventory();
        SignInput.open(viewer, sign, lines -> {
            if (!viewer.isOnline()) return;
            String text = lines.isEmpty() ? "" : lines.getFirst().strip();
            if (text.isEmpty()) return;
            Double amount = BankMenus.typed(text);
            if (amount == null) viewer.sendMessage(Text.line(BankMenus.NOT_A_NUMBER));
            else then.accept(amount);
        });
    }

    @Override
    public void beforeOpen(Player player) {
        items();
    }

    @Override
    public void items() {
        getItems().clear();
        Layout shown = layout.apply(profile(viewer));
        for (int slot : shown.glass()) set(slot, filler());
        shown.icons().forEach((slot, icon) -> {
            ItemStack stack = icon.stack();
            stack.setAmount(shown.counts().getOrDefault(slot, 1));
            Runnable action = buttons.get(slot);
            if (action != null) set(GUIClickableItem.button(slot, stack, viewer, action));
            else set(slot, stack);
        });
    }

    /** Every second, for the countdown. */
    @Override
    public long refreshRate() {
        return 20;
    }
}
