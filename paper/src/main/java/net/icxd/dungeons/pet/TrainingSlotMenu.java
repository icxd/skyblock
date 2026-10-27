package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Fann's "Training Slot 1": five rows of glass, as the wiki's Fann/UI has it with no pet ("&cNo Pet" where the
 * session's settings go, "&eClick a pet in your inventory!", "&eClick an Owl Pet in your inventory!"). A pet clicked in
 * the inventory goes in its slot, an Owl in the Owl's (both back on a click, or when the menu closes, as Kat's: see
 * {@link PlacedPet}). Then, where SkyHanni's FannCost reads them: the Training Duration ("&b▶ Amount of Days" or "&b▶
 * Until Specific Level"), the days or level, the Training Type ("&a▶ Light"), and Begin Training ("&7EXP Per Day:
 * &b250,000 &8(+10%)", "&8Will earn a total of 275,000 EXP" or "&7Will take: &e0d 10h 3m 4s", the coins "&8(1% off)"
 * and Bits). Their names and other lines, and changing the days or level with clicks (left more, right less, shift
 * ten), are UNKNOWN. Main thread.
 */
final class TrainingSlotMenu extends GUI {
    static final int MODE = 15;
    static final int PET = 20;
    static final int BEGIN = 22;
    static final int AMOUNT = 24;
    static final int OWL = 25;
    static final int TYPE = 33;
    static final int BACK = 39;
    static final int CLOSE = 40;
    static final int MOST_DAYS = 100;

    private final Player viewer;
    private final int slot;
    private PlacedPet pet;
    private PlacedPet owl;
    private boolean untilLevel;
    private int days = 1;
    private int level;
    private Fann.Training type = Fann.Training.FREE;

    TrainingSlotMenu(Player viewer, int slot) {
        super("Training Slot " + (slot + 1), Size.FIVE);
        this.viewer = viewer;
        this.slot = slot;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        set(GUIClickableItem.button(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Pet Training").stack(), viewer,
                () -> PetTrainingMenu.show(viewer, MenuSound.CLICK)));
        set(GUIClickableItem.close(CLOSE));
        set(GUIClickableItem.button(PET, pet == null ? PICK_PET.stack() : pet.item(), viewer, () -> takeBack(false)));
        set(GUIClickableItem.button(OWL, owl == null ? PICK_OWL.stack() : owl.item(), viewer, () -> takeBack(true)));
        User user = User.ifLoaded(viewer.getUniqueId());
        if (pet == null || user == null) {
            for (int s : new int[]{MODE, AMOUNT, TYPE}) set(s, NO_PET.stack());
            return;
        }
        PetData data = Pets.data();
        int taming = Skills.level(user.profile(), Skill.TAMING);
        List<Fann.Training> allowed = Fann.allowed(taming);
        if (!allowed.contains(type)) type = allowed.isEmpty() ? Fann.Training.FREE : allowed.getLast();
        int current = Pets.level(pet.pet(), data);
        PetData.Pet kind = data.pet(pet.pet().type());
        int most = kind == null ? current : data.maxLevel(kind);
        int least = Math.min(current + 1, most);
        level = Math.max(least, Math.min(level, most));
        set(GUIClickableItem.button(MODE, mode(untilLevel).stack(), viewer, () -> {
            untilLevel = !untilLevel;
            redraw();
        }));
        set(GUIClickableItem.button(AMOUNT, amount(untilLevel, days, level).stack(), viewer, click -> {
            if (untilLevel) level = stepped(level, click, least, most);
            else days = stepped(days, click, 1, MOST_DAYS);
            redraw();
        }));
        set(GUIClickableItem.button(TYPE, type(type, taming).stack(), viewer, click -> {
            if (allowed.isEmpty()) return;
            int at = allowed.indexOf(type);
            type = allowed.get(Math.floorMod(at + (click.isRightClick() ? -1 : 1), allowed.size()));
            redraw();
        }));
        double owlBonus = owl == null ? 0 : Fann.owlBonus(owl.pet(), data);
        double refund = Fann.refund(Fann.spent(user.profile()));
        Fann.Plan plan = untilLevel ? Fann.forXp(type, taming, owlBonus, refund, Fann.xpTo(pet.pet(), data, level))
                : Fann.forDays(type, taming, owlBonus, refund, days);
        boolean trainable = Fann.trainable(pet.pet(), data);
        set(GUIClickableItem.button(BEGIN, begin(type, taming, owlBonus, refund, plan, untilLevel, trainable).stack(), viewer, () -> {
            if (trainable) begin(user, plan);
        }));
    }

    private void begin(User user, Fann.Plan plan) {
        if (pet == null || !pet.ownedBy(this) || InventorySyncListener.frozen(viewer)) return;
        String refusal = Fann.begin(viewer, user, slot, pet, owl, type, plan);
        if (refusal != null) {
            viewer.sendMessage(Text.line(refusal));
            return;
        }
        PetTrainingMenu.show(viewer, MenuSound.CLICK);
    }

    @Override
    public boolean onPlayerInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack cursor = event.getCursor();
        if (!GUIClickableItem.pressed(event.getClick()) || (cursor != null && !cursor.isEmpty())) return true;
        int from = event.getSlot();
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            if (viewer.isOnline() && GUI_MAP.get(viewer.getUniqueId()) == this) place(from);
        });
        return true;
    }

    private void place(int from) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(viewer)) return;
        ItemStack stack = viewer.getInventory().getItem(from);
        OwnedPet clicked = stack == null || stack.isEmpty() ? null : PetItems.pet(ItemNBT.read(stack));
        if (clicked == null) return;
        boolean isOwl = Fann.OWL.equals(clicked.type());
        PlacedPet before = isOwl ? owl : pet;
        PlacedPet placed = PlacedPet.take(viewer, user.profile(), from, this);
        if (isOwl) owl = placed;
        else pet = placed;
        if (before != null) before.giveBack(viewer);
        redraw();
    }

    private void takeBack(boolean isOwl) {
        PlacedPet placed = isOwl ? owl : pet;
        if (placed == null) return;
        placed.giveBack(viewer);
        if (isOwl) owl = null;
        else pet = null;
        redraw();
    }

    private void redraw() {
        if (GUI_MAP.get(viewer.getUniqueId()) != this) return;
        beforeOpen(viewer);
        refresh(viewer.getOpenInventory().getTopInventory());
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (pet != null && pet.ownedBy(this)) pet.giveBack(viewer);
        if (owl != null && owl.ownedBy(this)) owl.giveBack(viewer);
    }

    // Its items

    static final Icon NO_PET = new Icon(Material.RED_STAINED_GLASS_PANE, "&cNo Pet", "&7Click a pet in your inventory to the",
            "&7left to begin training!");
    static final Icon PICK_PET = new Icon(Material.OAK_BUTTON, "&eClick a pet in your inventory!", "&7Selects it for training");
    static final Icon PICK_OWL = new Icon(Material.OAK_BUTTON, "&eClick an Owl Pet in your inventory!",
            "&7You don't have to include one, but it always helps!");

    static Icon mode(boolean untilLevel) {
        return new Icon(Material.CLOCK, "&aTraining Duration", (untilLevel ? "&7" : "&b▶ ") + "Amount of Days",
                (untilLevel ? "&b▶ " : "&7") + "Until Specific Level", "", "&eClick to switch!");
    }

    static Icon amount(boolean untilLevel, int days, int level) {
        String shown = untilLevel ? "&b▶ Level " + level : "&b▶ " + days + (days == 1 ? " Day" : " Days");
        return new Icon(Material.OAK_SIGN, untilLevel ? "&aSpecific Level" : "&aAmount of Days", shown, "", "&eLeft-click for more!",
                "&eRight-click for less!", "&8(Shift-click for ten)");
    }

    /** Each type, "&a▶ Light" for the one chosen, "&c" with its Taming level for one theirs is too low for. */
    static Icon type(Fann.Training chosen, int taming) {
        List<String> lore = new ArrayList<>();
        for (Fann.Training t : Fann.Training.values()) {
            if (t == chosen) lore.add("&" + t.color + "▶ " + t.label);
            else if (taming >= t.taming) lore.add("&7" + t.label);
            else lore.add("&c" + t.label + " &8(Taming " + t.taming + ")");
        }
        lore.addAll(List.of("", "&eClick to switch!"));
        return new Icon(Material.BONE, "&aTraining Type", lore);
    }

    static Icon begin(Fann.Training type, int taming, double owlBonus, double refund, Fann.Plan plan, boolean untilLevel, boolean trainable) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Training Type: " + type.colored());
        double perDay = type.xpPerDay(taming) * (1 + owlBonus / 100);
        lore.add("&7EXP Per Day: &b" + Text.number(Math.floor(perDay)) + (owlBonus > 0 ? " &8(+" + Text.number(owlBonus) + "%)" : ""));
        if (untilLevel) lore.add("&7Will take: &e" + Fann.duration(Math.round(plan.days() * Fann.DAY)));
        else lore.add("&8Will earn a total of " + Text.number(Math.floor(plan.xp())) + " EXP");
        lore.add("");
        lore.add("&7Cost");
        if (plan.coins() <= 0 && plan.bits() <= 0) lore.add("&aFree");
        if (plan.coins() > 0) lore.add("&6" + Fann.decimal(plan.coins()) + " Coins" + (refund > 0 ? " &8(" + Text.number(refund) + "% off)" : ""));
        if (plan.bits() > 0) lore.add("&b" + Fann.decimal(plan.bits()) + " Bits");
        lore.add("");
        lore.add(trainable ? "&eClick to begin training!" : "&cThis pet can't be trained!");
        return new Icon(trainable ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA, "&aBegin Training", lore);
    }

    /** What a click on the days or level does: left one more, right one less, shift ten, within these. */
    static int stepped(int value, ClickType click, int least, int most) {
        int step = (click.isShiftClick() ? 10 : 1) * (click.isRightClick() ? -1 : 1);
        return Math.max(least, Math.min(most, value + step));
    }
}
