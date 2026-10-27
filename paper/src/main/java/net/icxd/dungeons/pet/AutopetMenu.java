package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.pet.PetText.Where;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * "Autopet Rules", from the Pets menu (never recorded, UNKNOWN): each rule with its pet's head, what it equips, when and
 * its exceptions (left-click edits it, {@link AutopetRuleMenu}; right-click removes it), Create Rule in a free slot, +2
 * Rule Slots for Bits, Go Back and Close. Main thread.
 */
final class AutopetMenu extends GUI {
    static final String TITLE = "Autopet Rules";
    static final int SLOTS = 50;
    static final int BACK = 48;
    static final int CLOSE = 49;

    private final Player viewer;

    AutopetMenu(Player viewer) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
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
        Document profile = user.profile();
        PetData data = Pets.data();
        List<Autopet.Rule> rules = Autopet.rules(profile);
        int slots = Autopet.slots(profile);
        for (int i = 0; i < Math.max(slots, rules.size()) && i < Autopet.MOST; i++) {
            int index = i;
            if (i < rules.size()) {
                set(GUIClickableItem.button(PetsMenu.slot(i), rule(rules.get(i), i, Pets.find(profile, rules.get(i).pet()), data), viewer,
                        click -> clicked(index, click)));
            } else {
                set(GUIClickableItem.button(PetsMenu.slot(i), CREATE.stack(), viewer, () -> new AutopetRuleMenu(viewer, -1, null).open(viewer)));
            }
        }
        if (slots < Autopet.MOST) {
            set(GUIClickableItem.button(SLOTS, moreSlots(slots, user.getBits()).stack(), viewer, () -> buySlots(viewer)));
        }
        set(GUIClickableItem.button(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Pets").stack(), viewer,
                () -> PetsMenu.show(viewer, 1, MenuSound.CLICK)));
        set(GUIClickableItem.close(CLOSE));
    }

    private void clicked(int index, ClickType click) {
        if (click.isRightClick()) {
            User user = User.ifLoaded(viewer.getUniqueId());
            if (user == null || user.isReleased()) return;
            List<Autopet.Rule> rules = Autopet.rules(user.profile());
            if (index < rules.size()) rules.remove(index);
            Autopet.put(user.profile(), rules);
            user.save();
            new AutopetMenu(viewer).open(viewer, null);
            return;
        }
        AutopetRuleMenu.edit(viewer, index);
    }

    static final Icon CREATE = new Icon(Material.LIME_STAINED_GLASS_PANE, "&aCreate Rule", "&7Equip a pet when something", "&7happens!", "",
            "&eClick to create!");

    /** A rule: its pet's head, and what it does. */
    static ItemStack rule(Autopet.Rule rule, int index, OwnedPet pet, PetData data) {
        List<String> lore = new ArrayList<>(lines(rule, pet, data));
        lore.addAll(List.of("", "&eLeft-click to edit!", "&cRight-click to remove!"));
        ItemStack stack = pet == null ? new ItemStack(Material.BARRIER) : PetsMenu.stack(pet, data, Where.CHOOSE);
        stack.editMeta(meta -> {
            meta.displayName(Text.line("&aRule #" + (index + 1)));
            meta.lore(Text.lines(lore));
        });
        return stack;
    }

    /** "&7Equip: &7[Lvl 100] &6Lion", "&7When: &eOn gain Combat XP", "&7Except if: &cExcept if in Hub". */
    static List<String> lines(Autopet.Rule rule, OwnedPet pet, PetData data) {
        List<String> out = new ArrayList<>();
        out.add("&7Equip: " + (pet == null ? "&cA pet no longer in your menu" : PetText.name(pet, data, Where.ITEM)));
        out.add("&7When: &e" + rule.when(data));
        for (Autopet.Except e : rule.exceptions()) out.add("&7" + e.text(data).replaceFirst("^Except if", "Except if&c"));
        return out;
    }

    /** "+2 Rule Slots" for the Autopet Rules 2-Pack's 21,000 Bits (UNKNOWN: Hypixel's is bought from Elizabeth). */
    static Icon moreSlots(int slots, int bits) {
        return new Icon(Material.BOOK, "&a+2 Rule Slots", "&7Current Rule Slots: &a" + slots, "", "&7Cost",
                "&b" + Text.number(Autopet.PACK_BITS) + " Bits", "", bits >= Autopet.PACK_BITS ? "&eClick to purchase!" : "&cYou don't have enough Bits!");
    }

    private static void buySlots(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        Document profile = user.profile();
        if (Autopet.slots(profile) >= Autopet.MOST) return;
        if (user.getBits() < Autopet.PACK_BITS) {
            player.sendMessage(Text.line("&cYou don't have enough Bits!"));
            return;
        }
        profile.put("bits", user.getBits() - Autopet.PACK_BITS);
        Autopet.addSlots(profile);
        user.save();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1, 1);
        new AutopetMenu(player).open(player, null);
    }
}
