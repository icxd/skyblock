package net.icxd.dungeons.museum;

import java.util.List;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.museum.MuseumData.Milestone;
import net.icxd.dungeons.skill.SkillMenu;
import net.icxd.dungeons.user.User;

/**
 * Museum Milestones, as recorded (2026-10-25, 02:08.0 to 02:15.6): the 40 milestones on the path a skill's levels take
 * (see {@link SkillMenu}), nine columns at a time, the Museum Milestones item at its start; scrolled with the arrows
 * (the recording went one column a click, to the end at 8, then re-centred) and re-centred with the nether star. Each is
 * a glass pane, as many as its number: yellow for the next, red past it, lime once reached (UNKNOWN: never recorded;
 * as a skill's). Their rewards are the recorded lines; none of them are given here yet (no Bits, bank upgrades, Hex
 * gating or emblems for them). A click on an item in their inventory donates it. Main thread.
 */
final class MilestonesMenu extends MuseumGui {
    static final String TITLE = "Museum Milestones";
    private static final int ROWS = 4;
    /** How far a right-click scrolls: UNKNOWN (the recording's clicks went one at a time), as a skill menu's. */
    static final int FAST = 4;

    private int offset = -1;

    MilestonesMenu(Player viewer) {
        super(TITLE, Size.SIX, viewer);
    }

    @Override
    void reopen(Player player) {
        new MilestonesMenu(player).open(player, MenuSound.CLICK);
    }

    @Override
    public void beforeOpen(Player player) {
        render();
    }

    /** The milestone whose progress shows: the next, or the last once all are reached. */
    static int current(int xp, int count) {
        return Math.max(1, Math.min(Museum.milestone(xp) + 1, count));
    }

    private void render() {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        Document profile = user == null ? new Document() : user.profile();
        MuseumData data = Museum.data();
        List<Milestone> milestones = data.milestones();
        int xp = Museum.xp(profile, data);
        int count = milestones.size();
        int centre = SkillMenu.centred(current(xp, count), count);
        if (offset < 0) offset = centre;
        offset = Math.max(0, Math.min(offset, SkillMenu.maxOffset(count)));
        int header = SkillMenu.slot(ROWS - 1, 0, offset);
        if (header >= 0) set(header, new Icon(Material.GOLD_BLOCK, "&6Museum Milestones", MuseumText.milestonesLore(xp, data.totalXp())).stack());
        for (Milestone milestone : milestones) {
            int slot = SkillMenu.levelSlot(milestone.number(), offset);
            if (slot >= 0) set(slot, milestoneItem(milestone, xp));
        }
        if (offset > 0) set(scroll(45, "&aScroll Left", -1));
        if (offset < SkillMenu.maxOffset(count)) set(scroll(53, "&aScroll Right", 1));
        set(GUIClickableItem.button(48, new Icon(Material.ARROW, "&aGo Back", "&7To Your Museum").stack(), viewer,
                () -> MuseumMenu.show(viewer, MenuSound.CLICK)));
        set(GUIClickableItem.close(49));
        if (offset != centre) {
            set(GUIClickableItem.button(50, new Icon(Material.NETHER_STAR, "&aRe-Center", "&7Centers the menu around your", "&7current tier.", "",
                    "&eClick to re-center!").stack(), viewer, () -> scrollTo(centre)));
        }
    }

    static Material pane(Milestone milestone, int xp) {
        if (xp >= milestone.xp()) return Material.LIME_STAINED_GLASS_PANE;
        return xp >= milestone.xp() - Museum.MILESTONE_XP ? Material.YELLOW_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
    }

    /** "&aMuseum Milestone 5", whatever its colour (recorded), as many as its number. */
    static ItemStack milestoneItem(Milestone milestone, int xp) {
        ItemStack stack = new Icon(pane(milestone, xp), "&aMuseum Milestone " + milestone.number(), MuseumText.milestoneLore(milestone, xp)).stack();
        stack.setData(DataComponentTypes.MAX_STACK_SIZE, 64);
        stack.setAmount(Math.max(1, Math.min(64, milestone.number())));
        return stack;
    }

    private GUIClickableItem scroll(int slot, String name, int direction) {
        ItemStack arrow = new Icon(Material.ARROW, name, "&eLeft-click to scroll!", "&eRight-click to scroll fast!").stack();
        return GUIClickableItem.button(slot, arrow, viewer, click -> scrollTo(offset + direction * (click.isRightClick() ? FAST : 1)));
    }

    /** Scrolls the open menu in place (a new one would put their cursor back in the middle). */
    private void scrollTo(int to) {
        if (GUI_MAP.get(viewer.getUniqueId()) != this) return;
        offset = to;
        render();
        Inventory inventory = viewer.getOpenInventory().getTopInventory();
        if (inventory.getSize() == getSize()) refresh(inventory);
    }
}
