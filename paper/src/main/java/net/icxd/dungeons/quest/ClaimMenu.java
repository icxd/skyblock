package net.icxd.dungeons.quest;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.icxd.dungeons.economy.CoinSound;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.kyori.adventure.text.Component;

/**
 * Claim Reward, as recorded (2026-10-25, Marco's Spray Can and the Bartender's Undead Sword): six rows
 * of black glass, the item at 22 with "Click to claim!" under its lore, Close at 49. Claiming puts it
 * in their inventory, says "You claimed an Undead Sword!" with the coins' chime, and closes the menu.
 * Until then it waits (see {@link Claims}), and the reward's chat line opens this again. Main thread.
 */
public final class ClaimMenu extends GUI {
    static final String TITLE = "Claim Reward";
    static final int ITEM = 22;
    static final int CLOSE = 49;
    /** This plugin's words for these (UNKNOWN on Hypixel). */
    static final String NOTHING = "&cYou don't have that to claim!";
    static final String FULL = "&cYour inventory is full!";

    private final Player viewer;
    private final String claim;

    private ClaimMenu(Player viewer, String claim) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
        this.claim = claim;
    }

    /** Opens it for this claim key, with this sound; says so if there's nothing waiting under it. */
    public static void open(Player viewer, String claim, MenuSound sound) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        SkyBlockItem item = ItemRegistry.get(Claims.item(claim));
        if (!Claims.has(user.profile(), claim) || item == null) {
            viewer.sendMessage(Text.line(NOTHING));
            return;
        }
        new ClaimMenu(viewer, claim).open(viewer, sound);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        for (int slot = 0; slot < getSize(); slot++) if (slot != ITEM && slot != CLOSE) set(slot, filler());
        SkyBlockItem item = ItemRegistry.get(Claims.item(claim));
        ItemStack shown = ItemBuilder.shownTo(item, 1, viewer);
        ItemLore lore = shown.getData(DataComponentTypes.LORE);
        List<Component> lines = new ArrayList<>(lore == null ? List.of() : lore.lines());
        lines.addAll(Text.lines(List.of("", "&eClick to claim!")));
        shown.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
        set(GUIClickableItem.button(ITEM, shown, viewer, this::claim));
        set(GUIClickableItem.close(CLOSE));
    }

    private void claim() {
        User user = User.ifLoaded(viewer.getUniqueId());
        SkyBlockItem item = ItemRegistry.get(Claims.item(claim));
        if (user == null || user.isReleased() || item == null || !Claims.has(user.profile(), claim)) return;
        if (viewer.getInventory().firstEmpty() < 0) {
            viewer.sendMessage(Text.line(FULL));
            return;
        }
        Claims.take(user.profile(), claim);
        viewer.getInventory().addItem(ItemBuilder.build(item));
        user.save();
        viewer.sendMessage(Text.line(Claims.claimed(QuestRewards.itemName(item.id()))));
        CoinSound.play(viewer);
        viewer.closeInventory();
    }
}
