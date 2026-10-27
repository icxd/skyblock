package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

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
 * Fann's "Pet Training", as the wiki's Fann/UI has it: six rows of glass, the three Training Slots (#1 "&8Empty" ...
 * "&eClick to start a session!", #2 and #3 locked till Hootie is fed), the Owl pet she sells, Extra Free Training, and
 * Close (UNKNOWN: not in the wiki's). A slot with a pet training in it (UNKNOWN, never seen): its head, its training,
 * its time left and XP so far, right-click to stop it; done, a click picks it up. Main thread.
 */
final class PetTrainingMenu extends GUI {
    static final String TITLE = "Pet Training";
    static final int[] SLOTS = {11, 13, 15};
    static final int OWL = 31;
    static final int FREE = 48;
    static final int CLOSE = 49;

    private final Player viewer;

    private PetTrainingMenu(Player viewer) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
    }

    static void show(Player player, MenuSound sound) {
        new PetTrainingMenu(player).open(player, sound);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Document profile = user.profile();
        PetData data = Pets.data();
        List<Fann.Session> sessions = Fann.sessions(profile);
        long now = System.currentTimeMillis();
        for (int i = 0; i < Fann.SLOTS; i++) {
            int slot = i;
            Fann.Session session = sessions.get(i);
            if (session != null) {
                set(GUIClickableItem.button(SLOTS[i], training(session, data, now), viewer, click -> clicked(slot, session.done(now), click)));
            } else if (i < Fann.OPEN_SLOTS) {
                set(GUIClickableItem.button(SLOTS[i], EMPTY.stack(), viewer, () -> new TrainingSlotMenu(viewer, slot).open(viewer)));
            } else {
                set(SLOTS[i], (i == 1 ? LOCKED_2 : LOCKED_3).stack());
            }
        }
        ItemStack owl = owl();
        if (owl != null) set(GUIClickableItem.button(OWL, owl, viewer, () -> {
            Fann.buyOwl(viewer);
            show(viewer, null);
        }));
        set(FREE, freeTraining(Skills.level(profile, Skill.TAMING)).stack());
        set(GUIClickableItem.close(CLOSE));
    }

    private void clicked(int slot, boolean done, ClickType click) {
        if (!done && !click.isRightClick()) return;
        Fann.pickUp(viewer, slot);
        show(viewer, null);
    }

    static final Icon EMPTY = new Icon(Material.WHITE_STAINED_GLASS_PANE, "&cTraining Slot #1", "&8Empty", "", "&7This slot is currently empty!",
            "&8You can level up pets by training", "&8them here.", "", "&eClick to start a session!");
    static final Icon LOCKED_2 = new Icon(Material.RED_STAINED_GLASS_PANE, "&cTraining Slot #2", "&8Locked", "", "&7Unlock this slot by feeding &6Hootie",
            "&9Great Carrot Candy&7! (0/5)");
    static final Icon LOCKED_3 = new Icon(Material.RED_STAINED_GLASS_PANE, "&cTraining Slot #3", "&8Locked", "", "&7Unlock this slot by feeding &6Hootie",
            "&6Ultimate Carrot Candy&7!");

    /** A slot with a pet in training (UNKNOWN). */
    static ItemStack training(Fann.Session session, PetData data, long now) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Training: " + session.type().colored());
        if (session.done(now)) {
            lore.add("&7EXP gained: &b" + Text.number(Math.floor(session.xp())));
            lore.add("");
            lore.add("&aTraining complete!");
            lore.add("&eClick to pick up!");
        } else {
            lore.add("&7Time left: &e" + Fann.duration(session.end() - now));
            lore.add("&7EXP gained: &b" + Text.number(Math.floor(session.xpBy(now))) + "&7/&b" + Text.number(Math.floor(session.xp())));
            lore.add("");
            lore.add("&eRight-click to stop training!");
        }
        ItemStack head = PetsMenu.stack(session.pet(), data, Where.CHOOSE);
        head.editMeta(meta -> meta.lore(Text.lines(lore)));
        return head;
    }

    /** The Owl pet she sells, as the wiki's UI: its level 100's preview, "&7Cost", "&62,200,000 Coins", "&eClick to trade!". */
    static ItemStack owl() {
        OwnedPet owl = PetItems.newPet(Fann.OWL);
        ItemStack stack = owl == null ? null : PetItems.preview(Fann.OWL);
        if (stack == null) return null;
        PetData data = Pets.data();
        OwnedPet shown = owl.withExp(data.totalXp(data.pet(Fann.OWL), owl.tier(), 100));
        List<String> lore = new ArrayList<>(PetText.lore(shown, data, Where.SHOP, false));
        lore.addAll(List.of("", "&7Cost", "&6" + Text.number(Fann.OWL_PRICE) + " Coins", "", "&eClick to trade!"));
        stack.editMeta(meta -> meta.lore(Text.lines(lore)));
        return stack;
    }

    /** "&7Taming Level: &a10 &8(+0 EXP)", "&7Free Training: &b20,000 EXP/day", as the wiki's UI. */
    static Icon freeTraining(int taming) {
        double free = Fann.freeXp(taming);
        return new Icon(Material.POLAR_BEAR_SPAWN_EGG, "&aExtra Free Training", "&7Fann's Free Training session gets",
                "&7increasingly better the higher the", "&aTaming Level &7of her customers.", "",
                "&7Taming Level: &a" + taming + " &8(+" + Text.number(free - Fann.freeXp(Fann.TAMING)) + " EXP)",
                "&7Free Training: &b" + Text.number(free) + " EXP/day");
    }
}
