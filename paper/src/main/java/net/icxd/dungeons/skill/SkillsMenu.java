package net.icxd.dungeons.skill;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * "Your Skills" ({@code /skills}), as recorded (research skills.md 1.2): every skill with its level,
 * progress and the next level's rewards; clicking one opens its own menu ({@link SkillMenu}). Hypixel's
 * "Go Back" to the SkyBlock Menu isn't here (this plugin has no SkyBlock Menu yet), and neither is a
 * Dungeoneering menu (never recorded) or the skill rankings display. Main thread.
 */
public final class SkillsMenu extends GUI {
    /** Where each skill goes, in {@link Skill}'s order (slot 31 stays empty, as recorded). */
    private static final int[] SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 32, 33};
    private static final String DUNGEONEERING_HEAD = "9b56895b9659896ad647f58599238af532d46db9c1b0389b8bbeb70999dab33d";

    private final Player viewer;

    public SkillsMenu(Player viewer) {
        super("Your Skills", Size.SIX);
        this.viewer = viewer;
    }

    static int slot(Skill skill) {
        return SLOTS[skill.ordinal()];
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Document profile = user.profile();
        set(4, menuItem(Material.DIAMOND_SWORD, "&aYour Skills", summary(Skills.average(profile), false)));
        for (Skill skill : Skill.values()) {
            double xp = Skills.xp(profile, skill);
            ItemStack stack = menuItem(skill.icon(), "&a" + SkillText.named(skill, skill.level(xp)), lore(skill, xp));
            set(button(slot(skill), stack, () -> new SkillMenu(viewer, skill).open(viewer)));
        }
        ItemStack head = menuItem(Material.PLAYER_HEAD, "&aDungeoneering", List.of("&7Complete Dungeons to level up your",
                "&7classes! Unlock new gear and class", "&7upgrades by completing higher tier", "&7dungeons!", "",
                "&7Requires &bCombat Level 15 &7to enter a", "&7Dungeon.", "", "&eClick to view!"));
        Utils.skull(head, Utils.texture(DUNGEONEERING_HEAD));
        set(34, head);
        set(GUIClickableItem.close(49));
        set(53, menuItem(Material.OAK_SIGN, "&aShow Skill Rankings", List.of("&7Show the rankings display for your", "&7Skills.", "",
                "&eClick to show!")));
    }

    /**
     * The "Your Skills" item's lore (the SkyBlock Menu's has "&eClick to view!" too): "&621.6 Skill Avg.
     * &8(non-cosmetic)", one decimal. With no skill levels there's no average line (a new profile's
     * SkyBlock Menu, recorded 04:29.1).
     */
    public static List<String> summary(double average, boolean click) {
        List<String> lore = new ArrayList<>(List.of("&7View your Skill progression and", "&7rewards.", ""));
        if (average > 0) lore.addAll(List.of("&6" + SkillText.number(average) + " Skill Avg. &8(non-cosmetic)", ""));
        lore.add("&8Also accessible via /skills.");
        if (click) lore.addAll(List.of("", "&eClick to view!"));
        return lore;
    }

    /**
     * A skill's lore: how it's earned, how far into the next level it is and that level's rewards
     * ("Reward:" when there's one line, as Runecrafting's). A maxed skill says "Max Skill level
     * reached!" over a full bar with its XP (what mods read; the rest of that lore is UNKNOWN).
     */
    static List<String> lore(Skill skill, double xp) {
        List<String> lore = new ArrayList<>(skill.description());
        lore.add("");
        if (skill.maxed(xp)) {
            lore.add("&7Max Skill level reached!");
            lore.add(SkillText.strip(1) + " &e" + SkillText.number(xp));
        } else {
            int next = skill.level(xp) + 1;
            String roman = Utils.getRomanNumeral(next);
            lore.add("&7Progress to Level " + roman + ": &e" + SkillText.percent(skill.progress(xp)) + "%");
            lore.add(SkillText.bar(skill.progress(xp), skill.xpIntoLevel(xp), skill.xpFor(next)));
            lore.add("");
            List<String> rewards = SkillRewards.lines(skill, next);
            lore.add("&7Level " + roman + (rewards.size() == 1 ? " Reward:" : " Rewards:"));
            for (String line : rewards) lore.add("  " + line);
        }
        if (skill == Skill.RUNECRAFTING) {
            lore.add("");
            lore.add("&dLevel up Runecrafting to activate");
            lore.add("&dthe effects of rune-bearing items!");
        }
        lore.add("");
        lore.add("&eClick to view!");
        return lore;
    }

    /** A menu item with no attribute lines (swords and helmets would list theirs). */
    static ItemStack menuItem(Material material, String name, List<String> lore) {
        ItemStack stack = item(material, name, lore);
        stack.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS).build());
        return stack;
    }

    /** Runs {@code action} with the click on the next tick for a left or right click (shift or not), if they're still on. */
    static GUIClickableItem button(int slot, ItemStack stack, Player viewer, Consumer<ClickType> action) {
        return new GUIClickableItem() {
            @Override
            public void run(InventoryClickEvent event) {
                ClickType click = event.getClick();
                if (!click.isLeftClick() && !click.isRightClick()) return;
                Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
                    if (viewer.isOnline()) action.accept(click);
                });
            }

            @Override
            public int slot() {
                return slot;
            }

            @Override
            public ItemStack stack() {
                return stack;
            }
        };
    }

    private GUIClickableItem button(int slot, ItemStack stack, Runnable action) {
        return button(slot, stack, viewer, click -> action.run());
    }
}
