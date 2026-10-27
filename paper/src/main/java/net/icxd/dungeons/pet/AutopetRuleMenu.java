package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
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
 * An Autopet rule being made or changed (never recorded, UNKNOWN): the pet it equips (a click chooses another), when
 * (left-click the next trigger, right-click the one before) and with what (the same, shift ten), its five exceptions
 * (an empty one's click adds one; left-click its next kind, right-click its next choice, shift-click removes it),
 * Go Back (nothing kept), Save Rule and, for one that's kept, Delete Rule. Main thread.
 */
final class AutopetRuleMenu extends GUI {
    static final String TITLE = "Autopet Rule";
    static final int PET = 13;
    static final int TRIGGER = 29;
    static final int ARG = 31;
    static final int FIRST_EXCEPTION = 38;
    static final int BACK = 48;
    static final int SAVE = 49;
    static final int DELETE = 50;

    private final Player viewer;
    /** Its place in their rules; -1 for a new one. */
    private final int index;
    private String pet;
    private Autopet.Trigger trigger = Autopet.Trigger.LOGIN;
    private String arg;
    private final List<Autopet.Except> exceptions = new ArrayList<>();

    AutopetRuleMenu(Player viewer, int index, Autopet.Rule rule) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
        this.index = index;
        if (rule != null) {
            pet = rule.pet();
            trigger = rule.trigger();
            arg = rule.arg();
            exceptions.addAll(rule.exceptions());
        }
    }

    /** Their rule {@code index} (VIEW RULE's, the menu's), or a new one if there's none there. */
    static void edit(Player player, int index) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        List<Autopet.Rule> rules = Autopet.rules(user.profile());
        if (index >= 0 && index < rules.size()) new AutopetRuleMenu(player, index, rules.get(index)).open(player);
        else new AutopetRuleMenu(player, -1, null).open(player);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        PetData data = Pets.data();
        OwnedPet chosen = pet == null ? null : Pets.find(user.profile(), pet);
        set(GUIClickableItem.button(PET, petItem(chosen, data), viewer, () -> ChoosePetMenu.show(viewer, 1, "&7To Autopet Rule",
                () -> open(viewer, MenuSound.CLICK), uuid -> {
                    pet = uuid;
                    open(viewer, MenuSound.CLICK);
                })));
        set(GUIClickableItem.button(TRIGGER, triggers(trigger, arg, data).stack(), viewer, click -> {
            Autopet.Trigger[] all = Autopet.Trigger.values();
            trigger = all[Math.floorMod(trigger.ordinal() + (click.isRightClick() ? -1 : 1), all.length)];
            arg = trigger.arg == null ? null : first(trigger.arg, data);
            redraw();
        }));
        if (trigger.arg != null) {
            set(GUIClickableItem.button(ARG, argItem(trigger.arg, arg, data).stack(), viewer, click -> {
                arg = step(trigger.arg.choices(data), arg, click);
                redraw();
            }));
        }
        for (int i = 0; i < Autopet.MOST_EXCEPTIONS; i++) {
            int at = i;
            if (i < exceptions.size()) {
                set(GUIClickableItem.button(FIRST_EXCEPTION + i, exceptionItem(exceptions.get(i), data).stack(), viewer, click -> {
                    changeException(at, click, data);
                    redraw();
                }));
            } else {
                set(GUIClickableItem.button(FIRST_EXCEPTION + i, NO_EXCEPTION.stack(), viewer, () -> {
                    if (exceptions.size() >= Autopet.MOST_EXCEPTIONS) return;
                    exceptions.add(new Autopet.Except(Autopet.Unless.IN_ISLAND, first(Autopet.Arg.ISLAND, data)));
                    redraw();
                }));
            }
        }
        set(GUIClickableItem.button(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Autopet Rules").stack(), viewer,
                () -> new AutopetMenu(viewer).open(viewer, MenuSound.CLICK)));
        set(GUIClickableItem.button(SAVE, save(chosen != null).stack(), viewer, () -> save(user)));
        if (index >= 0) set(GUIClickableItem.button(DELETE, DELETE_ICON.stack(), viewer, () -> delete(user)));
    }

    private void changeException(int at, ClickType click, PetData data) {
        Autopet.Except e = exceptions.get(at);
        if (click.isShiftClick()) {
            exceptions.remove(at);
            return;
        }
        if (click.isRightClick()) {
            exceptions.set(at, new Autopet.Except(e.type(), step(e.type().arg.choices(data), e.arg(), ClickType.LEFT)));
            return;
        }
        Autopet.Unless[] all = Autopet.Unless.values();
        Autopet.Unless next = all[(e.type().ordinal() + 1) % all.length];
        exceptions.set(at, new Autopet.Except(next, next.arg == e.type().arg ? e.arg() : first(next.arg, data)));
    }

    private void save(User user) {
        if (pet == null || user.isReleased()) return;
        List<Autopet.Rule> rules = Autopet.rules(user.profile());
        Autopet.Rule rule = new Autopet.Rule(pet, trigger, arg, exceptions);
        if (index >= 0 && index < rules.size()) rules.set(index, rule);
        else if (rules.size() < Autopet.slots(user.profile())) rules.add(rule);
        else {
            viewer.sendMessage(Text.line("&cYou don't have a free rule slot!"));
            return;
        }
        Autopet.put(user.profile(), rules);
        user.save();
        new AutopetMenu(viewer).open(viewer, MenuSound.CLICK);
    }

    private void delete(User user) {
        if (user.isReleased()) return;
        List<Autopet.Rule> rules = Autopet.rules(user.profile());
        if (index < rules.size()) rules.remove(index);
        Autopet.put(user.profile(), rules);
        user.save();
        new AutopetMenu(viewer).open(viewer, MenuSound.CLICK);
    }

    private void redraw() {
        if (GUI_MAP.get(viewer.getUniqueId()) != this) return;
        beforeOpen(viewer);
        refresh(viewer.getOpenInventory().getTopInventory());
    }

    // Its items

    static final Icon NO_EXCEPTION = new Icon(Material.GRAY_STAINED_GLASS_PANE, "&7No exception", "&eClick to add one!");
    static final Icon DELETE_ICON = new Icon(Material.RED_TERRACOTTA, "&cDelete Rule", "&eClick to delete!");

    private static ItemStack petItem(OwnedPet pet, PetData data) {
        if (pet == null) return new Icon(Material.BONE, "&eChoose a Pet", "&7The pet this rule equips.", "", "&eClick to choose!").stack();
        ItemStack stack = PetsMenu.stack(pet, data, Where.CHOOSE);
        stack.editMeta(meta -> meta.lore(Text.lines(List.of("&7The pet this rule equips.", "", "&eClick to choose another!"))));
        return stack;
    }

    /** Every trigger, "&e▶ On gain Farming XP" for the chosen. */
    static Icon triggers(Autopet.Trigger chosen, String arg, PetData data) {
        List<String> lore = new ArrayList<>();
        for (Autopet.Trigger t : Autopet.Trigger.values()) {
            String text = t.arg == null ? t.text : t.text.formatted(t == chosen ? t.arg.shown(arg, data) : "[" + George.titled(t.arg.name()) + "]");
            lore.add(t == chosen ? "&e▶ " + text : "&7" + text);
        }
        lore.addAll(List.of("", "&eLeft-click for the next!", "&eRight-click for the one before!"));
        return new Icon(Material.CLOCK, "&aWhen", lore);
    }

    static Icon argItem(Autopet.Arg kind, String arg, PetData data) {
        return new Icon(Material.OAK_SIGN, "&a" + George.titled(kind.name()), "&b▶ " + kind.shown(arg, data), "", "&eLeft-click for the next!",
                "&eRight-click for the one before!", "&8(Shift-click for ten)");
    }

    static Icon exceptionItem(Autopet.Except e, PetData data) {
        return new Icon(Material.REDSTONE_TORCH, "&cException", "&7" + e.text(data), "", "&eLeft-click for its next kind!",
                "&eRight-click for its next choice!", "&cShift-click to remove!");
    }

    static Icon save(boolean ready) {
        return ready ? new Icon(Material.LIME_TERRACOTTA, "&aSave Rule", "&eClick to save!")
                : new Icon(Material.RED_TERRACOTTA, "&aSave Rule", "&cChoose a pet first!");
    }

    private static String first(Autopet.Arg kind, PetData data) {
        List<String> choices = kind.choices(data);
        return choices.isEmpty() ? null : choices.getFirst();
    }

    /** The next choice (the one before for a right-click; shift, ten on). */
    static String step(List<String> choices, String current, ClickType click) {
        if (choices.isEmpty()) return current;
        int by = (click.isShiftClick() ? 10 : 1) * (click.isRightClick() ? -1 : 1);
        int at = current == null ? -1 : choices.indexOf(current);
        // From none (or one that's no longer there): the first, or the last going back.
        if (at < 0) return by > 0 ? choices.getFirst() : choices.getLast();
        return choices.get(Math.floorMod(at + by, choices.size()));
    }
}
