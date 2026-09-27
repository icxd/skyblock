package net.icxd.dungeons.skill;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * One skill's menu ("Combat Skill"), as recorded (research skills.md 1.3): its levels on a path that
 * snakes through the top four rows, nine columns of it at a time, scrolled with the arrows and
 * re-centred with the nether star. The path is a strip of columns: column 0 holds the skill's own
 * item at the bottom, then each ten levels take four columns, up the second and down the fourth.
 * Opening it puts the level in progress in the fifth column. Only Combat's was recorded; the other
 * skills' are made the same way (their items too, which is UNKNOWN), and Combat's Bestiary and Slayer
 * items are Combat's only. Main thread.
 */
public final class SkillMenu extends GUI {
    private static final int ROWS = 4;
    private static final int COLUMNS = 9;
    /** Where the level in progress goes when the menu opens or re-centres. */
    private static final int CENTRE = 4;
    /** How far a right-click scrolls: UNKNOWN (no recorded clicks), so one level block's four columns. */
    static final int FAST = 4;
    private static final String SLAYER_HEAD = "738c687dd8887c509d55865ebb62ab6087ef743e9425cd5dc6b4c7b93b747ff2";

    private final Player viewer;
    private final Skill skill;
    private int offset = -1;

    public SkillMenu(Player viewer, Skill skill) {
        super(skill.getName() + " Skill", Size.SIX);
        this.viewer = viewer;
        this.skill = skill;
    }

    // Where levels go

    /** A level's column on the strip. */
    static int column(int level) {
        int block = (level - 1) / 10, k = (level - 1) % 10, c = 1 + 4 * block;
        return switch (k) {
            case 0 -> c;
            case 1, 2, 3, 4 -> c + 1;
            case 5 -> c + 2;
            default -> c + 3;
        };
    }

    /** A level's row (0 at the top). */
    static int row(int level) {
        int k = (level - 1) % 10;
        return switch (k) {
            case 0, 1, 9 -> 3;
            case 2, 8 -> 2;
            case 3, 7 -> 1;
            default -> 0;
        };
    }

    /** The furthest the view scrolls for a skill with this many levels (16 for 60). */
    static int maxOffset(int levels) {
        int last = 0;
        for (int level = 1; level <= levels; level++) last = Math.max(last, column(level));
        return Math.max(0, last + 1 - COLUMNS);
    }

    /** The view that puts this level in the fifth column, as far as the strip allows. */
    static int centred(int level, int levels) {
        return Math.max(0, Math.min(column(level) - CENTRE, maxOffset(levels)));
    }

    /** The slot for this spot on the strip in this view; -1 if it's out of view. */
    static int slot(int row, int column, int offset) {
        int shown = column - offset;
        return shown < 0 || shown >= COLUMNS ? -1 : row * COLUMNS + shown;
    }

    /** The level's slot in this view; -1 if it's out of view. */
    static int levelSlot(int level, int offset) {
        return slot(row(level), column(level), offset);
    }

    // The menu

    /** The level whose progress is shown: the next one, or the last once maxed. */
    private static int current(Skill skill, double xp) {
        return Math.min(skill.level(xp) + 1, skill.cap());
    }

    @Override
    public void beforeOpen(Player player) {
        render();
    }

    private void render() {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        double xp = Skills.xp(user.profile(), skill);
        int levels = skill.cap();
        int centre = centred(current(skill, xp), levels);
        if (offset < 0) offset = centre;
        offset = Math.max(0, Math.min(offset, maxOffset(levels)));

        int header = slot(ROWS - 1, 0, offset);
        if (header >= 0) set(header, SkillsMenu.menuItem(skill.icon(), "&a" + skill.getName() + " Skill", header(skill, xp)));
        for (int level = 1; level <= levels; level++) {
            int slot = levelSlot(level, offset);
            if (slot >= 0) set(slot, levelItem(skill, level, xp));
        }

        if (skill == Skill.COMBAT) {
            set(39, SkillsMenu.menuItem(Material.WRITABLE_BOOK, "&3Bestiary", bestiary()));
            ItemStack slayer = SkillsMenu.menuItem(Material.PLAYER_HEAD, "&5Slayer", List.of("&5Slayer Quests &7involve killing a large",
                    "&7number of a certain mob before", "&7defeating a powerful &cboss&7. Each", "&7type of &cboss &7has its own powerful",
                    "&7abilities and rare drops.", "", "&7Take on a &5Slayer Quest &7by speaking", "&7to &5Maddox the Slayer &7in the &bHub&7!"));
            Utils.skull(slayer, Utils.texture(SLAYER_HEAD));
            set(41, slayer);
        }
        if (offset > 0) set(scroll(45, "&aScroll Left", -1));
        if (offset < maxOffset(levels)) set(scroll(53, "&aScroll Right", 1));
        set(GUIClickableItem.button(48, item(Material.ARROW, "&aGo Back", "&7To Your Skills"), viewer, () -> new SkillsMenu(viewer).open(viewer)));
        set(GUIClickableItem.close(49));
        if (offset != centre) {
            set(GUIClickableItem.button(50, item(Material.NETHER_STAR, "&aRe-Center", "&7Centers the menu around your", "&7current tier.", "",
                    "&eClick to re-center!"), viewer, () -> scrollTo(centre)));
        }
    }

    private GUIClickableItem scroll(int slot, String name, int direction) {
        ItemStack arrow = item(Material.ARROW, name, "&eLeft-click to scroll!", "&eRight-click to scroll fast!");
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

    /** The skill's own item: its progress and its perk now (none at level 0). */
    static List<String> header(Skill skill, double xp) {
        List<String> lore = new ArrayList<>(skill.description());
        lore.add("");
        int level = skill.level(xp);
        if (skill.maxed(xp)) {
            lore.add("&7Max Skill level reached!");
            lore.add(SkillText.strip(1) + " &e" + SkillText.number(xp));
        } else {
            lore.add("&7Progress to Level " + Utils.getRomanNumeral(level + 1) + ": &e" + SkillText.percent(skill.progress(xp)) + "%");
            lore.add(SkillText.bar(skill.progress(xp), skill.xpIntoLevel(xp), skill.xpFor(level + 1)));
        }
        String perk = SkillRewards.perkName(skill);
        if (perk != null && level > 0) {
            lore.add("");
            lore.add("&e" + perk + " " + Utils.getRomanNumeral(level));
            for (String line : SkillRewards.perkDescription(skill, level, false)) lore.add("  " + line);
        }
        lore.add("");
        lore.add("&8Increase your " + skill.getName() + " Level to");
        lore.add("&8unlock Perks, statistic bonuses, and");
        lore.add("&8more!");
        return lore;
    }

    /**
     * A level on the path: green glass once reached (a diamond helmet every fifth level), yellow for the
     * one in progress (with its progress), red past it; as many as the level.
     */
    static ItemStack levelItem(Skill skill, int level, double xp) {
        int reached = skill.level(xp);
        boolean done = level <= reached;
        boolean next = level == reached + 1;
        Material material = done ? (level % 5 == 0 ? Material.DIAMOND_HELMET : Material.LIME_STAINED_GLASS_PANE)
                : next ? Material.YELLOW_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String color = done ? "&a" : next ? "&e" : "&c";
        ItemStack stack = SkillsMenu.menuItem(material, color + skill.getName() + " Level " + Utils.getRomanNumeral(level), levelLore(skill, level, xp));
        // A helmet only stacks to 1, and a stack past its item's limit can't be sent.
        stack.setData(DataComponentTypes.MAX_STACK_SIZE, 64);
        stack.setAmount(level);
        return stack;
    }

    static List<String> levelLore(Skill skill, int level, double xp) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Rewards:");
        for (String line : SkillRewards.lines(skill, level)) lore.add("  " + line);
        int reached = skill.level(xp);
        if (level <= reached) {
            lore.add("");
            lore.add("&a&lUNLOCKED");
        } else if (level == reached + 1) {
            lore.add("");
            lore.add("&7Progress: &e" + SkillText.percent(skill.progress(xp)) + "%");
            lore.add(SkillText.bar(skill.progress(xp), skill.xpIntoLevel(xp), skill.xpFor(level)));
        }
        return lore;
    }

    /**
     * Hypixel's Bestiary item with none found, as this plugin has no Bestiary yet (so no "Click to
     * view!" either); 359 families is Hypixel's recorded count.
     */
    private static List<String> bestiary() {
        String none = SkillText.strip(0) + " &b0&3/&b359";
        return List.of("&7The Bestiary is a compendium of", "&7mobs in SkyBlock. It contains detailed", "&7information on loot drops, your mob",
                "&7stats, and more!", "", "&7Kill mobs within &aFamilies &7to progress", "&7and earn &arewards&7, including &b✯ Magic",
                "&bFind &7bonuses towards mobs in the", "&7Family.", "", "&7Families Found: &e0%", none, "",
                "&7Families Completed: &e0%", none);
    }
}
