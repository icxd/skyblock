package net.icxd.dungeons.leveling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.leveling.LevelingData.Reward;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.menu.SkyBlockMenu;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * SkyBlock Leveling ({@code /levels}, or its head in the SkyBlock Menu), as recorded (the SkyBlock Menu tour: Banana's
 * at level 88, 01:34.5, and the new profile Lemon's at 0, 04:39.7): their level and XP, their level and the four after
 * it with their rewards, the next milestone, and the ways into Ways to Level Up, the SkyBlock XP Guide, Leveling
 * Rewards and the emblems, with the SkyBlock Levels in Chat switch. There are no level rankings here: the ranking
 * item says what it says while Hypixel's is loading (the recording shows that first, then the rank). Main thread.
 */
public final class LevelingMenu extends GUI {
    public static final String TITLE = "SkyBlock Leveling";
    static final int RANKING = 4;
    static final int WAYS = 16;
    /** Their level, then the next four. */
    static final int[] LEVELS = {19, 20, 21, 22, 23};
    static final int GUIDE = 25;
    static final int MILESTONE = 30;
    static final int REWARDS = 34;
    static final int EMBLEMS = 43;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int CHAT = 50;
    /** The guide's head, recorded (the same skin as the SkyBlock Menu's leveling head). */
    static final String GUIDE_HEAD = "3255327dd8e90afad681a19231665bea2bd06065a09d77ac1408837f9e0b242";

    private final Player viewer;

    public LevelingMenu(Player viewer) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
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
        LevelingView view = LevelingView.of(viewer, user);
        Map<Integer, Icon> icons = icons(view);
        for (Map.Entry<Integer, Icon> entry : icons.entrySet()) set(entry.getKey(), entry.getValue().stack());
        button(WAYS, icons, () -> new WaysMenu(viewer).open(viewer));
        for (int i = 0; i < LEVELS.length; i++) {
            int level = view.level() + i;
            button(LEVELS[i], icons, () -> new LevelMenu(viewer, level).open(viewer));
        }
        button(GUIDE, icons, () -> GuideMenu.openCurrent(viewer));
        int milestone = LevelRewards.nextMilestone(view.data(), view.level());
        if (milestone > 0) button(MILESTONE, icons, () -> new LevelMenu(viewer, milestone).open(viewer));
        button(REWARDS, icons, () -> new RewardsMenu(viewer).open(viewer));
        button(EMBLEMS, icons, () -> new EmblemsMenu(viewer).open(viewer));
        button(BACK, icons, () -> new SkyBlockMenu(viewer).open(viewer));
        set(GUIClickableItem.close(CLOSE));
        button(CHAT, icons, () -> {
            User u = User.ifLoaded(viewer.getUniqueId());
            if (u == null || GUI_MAP.get(viewer.getUniqueId()) != this) return;
            SkyBlockLevels.toggleLevelsInChat(viewer, u);
            render();
            Inventory inventory = viewer.getOpenInventory().getTopInventory();
            if (inventory.getSize() == getSize()) refresh(inventory);
        });
    }

    private void button(int slot, Map<Integer, Icon> icons, Runnable action) {
        Icon icon = icons.get(slot);
        if (icon != null) set(GUIClickableItem.button(slot, icon.stack(), viewer, action));
    }

    // What each slot shows

    /** Every slot but the glass and Close, by slot. */
    static Map<Integer, Icon> icons(LevelingView view) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        int level = view.level(), xp = view.total();
        icons.put(RANKING, ranking(view.mode(), xp, view.data().maxXp()));
        icons.put(WAYS, new Icon(Material.REDSTONE_TORCH, "&aWays to Level Up", "&7Learn more about the different ways", "&7to earn SkyBlock XP.",
                "", "&7Also see a specific breakdown of", "&7where all your XP comes from!", "", "&eClick to view!"));
        for (int i = 0; i < LEVELS.length; i++) icons.put(LEVELS[i], level(view.data(), level + i, i, xp));
        icons.put(GUIDE, new Icon(Material.PLAYER_HEAD, "&a⚑ SkyBlock XP Guide", List.of("&7Your &6SkyBlock XP Guide &7tracks the",
                "&7progress you have made through", "&7SkyBlock.", "", "&7Complete tasks within your current", "&7game stage to increase your",
                "&bSkyBlock Level &7and become a &dMaster", "&7of SkyBlock!", "", "&8Also accessible via /skyblockxp", "", "&eClick to view!"), GUIDE_HEAD));
        Icon milestone = milestone(view.data(), level, xp);
        if (milestone != null) icons.put(MILESTONE, milestone);
        icons.put(REWARDS, rewards(view.data(), level));
        icons.put(EMBLEMS, new Icon(Material.NAME_TAG, "&aPrefix Emblems", "&7Add some spice by having an emblem",
                "&7next to your name in chat and in tab!", "", "&7Emblems are unlocked through", "&7various activities such as leveling up",
                "&7or completing achievements!", "", "&7Emblems also show important data", "&7associated with them in chat!", "", "&eClick to view!"));
        icons.put(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Menu"));
        icons.put(CHAT, chat(view.levelsInChat()));
        return icons;
    }

    /** "Classic": what Hypixel calls a Normal profile's mode. Sandbox is this server's own, so its name here is UNKNOWN. */
    static String modeName(ProfileMode mode) {
        return mode == ProfileMode.SANDBOX ? "Sandbox" : "Classic";
    }

    /**
     * Their level and XP, and what share of all the tasks' XP that is. The recorded 15.2% for 8,834 XP needs the tasks
     * to be worth more than 57,928, and the recorded categories add up to 57,874 (15.3%): UNKNOWN what else Hypixel
     * counts, so it's the categories' total.
     */
    static Icon ranking(ProfileMode mode, int xp, int maxXp) {
        int level = SkyBlockXp.level(xp);
        List<String> lore = new ArrayList<>(List.of("&8" + modeName(mode) + " Mode", "", "&7Your level: " + SkyBlockXp.color(level) + level,
                "&7You have: &b" + Text.number(xp) + " XP", ""));
        lore.addAll(LevelingText.wrap("&7You have completed &3" + LevelingText.percent(maxXp == 0 ? 0 : (double) xp / maxXp)
                + "%&7 of the total SkyBlock XP Tasks."));
        lore.addAll(List.of("", "&7Ranking information requires", "&7SkyBlock Level 10 or higher.", "&8Level rankings may take time to", "&8refresh."));
        return new Icon(Material.PAINTING, "&aYour SkyBlock Level Ranking", lore);
    }

    /**
     * One of the five levels: theirs (lime glass), the next with its progress (yellow), the rest (red); a level with a
     * reward besides its stats is a glass block, the rest panes (Level 3 and Level 90 as recorded). Their own level as a
     * block is UNKNOWN (never recorded at a milestone), and so is the next.
     */
    static Icon level(LevelingData data, int level, int position, int xp) {
        boolean block = LevelRewards.milestone(data, level);
        String color = SkyBlockXp.color(level);
        List<String> lines = LevelRewards.lines(data, level);
        List<String> lore = new ArrayList<>();
        if (position == 0) lore.addAll(List.of("&8Your Level", ""));
        if (position == 1) lore.addAll(List.of("&8Next Level", ""));
        lore.add(LevelRewards.heading(lines.size()));
        for (String line : lines) lore.add(" " + line);
        lore.add("");
        if (position == 0) lore.addAll(List.of("&a&lUNLOCKED", ""));
        if (position == 1) {
            int into = SkyBlockXp.intoLevel(xp);
            lore.addAll(List.of("&7Progress to Level Up:", LevelingText.strip(color, (double) into / SkyBlockXp.PER_LEVEL) + " " + color + into + "&8/"
                    + color + SkyBlockXp.PER_LEVEL + " XP", ""));
        }
        lore.add("&eClick to view rewards!");
        Material material = switch (position) {
            case 0 -> block ? Material.LIME_STAINED_GLASS : Material.LIME_STAINED_GLASS_PANE;
            case 1 -> block ? Material.YELLOW_STAINED_GLASS : Material.YELLOW_STAINED_GLASS_PANE;
            default -> block ? Material.RED_STAINED_GLASS : Material.RED_STAINED_GLASS_PANE;
        };
        return new Icon(material, color + "Level " + level, lore);
    }

    /**
     * The next level with a reward besides its stats, as its first reward's item, and the XP left to it; null when
     * there's none left (UNKNOWN what Hypixel shows then).
     */
    static Icon milestone(LevelingData data, int level, int xp) {
        int milestone = LevelRewards.nextMilestone(data, level);
        if (milestone < 0) return null;
        List<Reward> rewards = data.rewards(milestone);
        List<String> lines = LevelRewards.lines(data, milestone);
        List<String> lore = new ArrayList<>(List.of("&8Next Milestone Level", "", LevelRewards.heading(lines.size())));
        for (String line : lines) lore.add(" " + line);
        int levels = milestone - level;
        lore.addAll(List.of("", "&7XP Left to Gain: &b" + Text.number((long) milestone * SkyBlockXp.PER_LEVEL - xp) + " XP &8(" + levels
                + (levels == 1 ? " Level)" : " Levels)"), "", "&eClick to view rewards!"));
        return rewards.getFirst().look().icon(SkyBlockXp.color(milestone) + "Level " + milestone, lore);
    }

    /** Leveling Rewards: how many of all the rewards but stats they've unlocked ("Progress to Max: 44.4%", 24/54). */
    static Icon rewards(LevelingData data, int level) {
        int unlocked = 0;
        for (Reward reward : data.rewards()) if (reward.level() <= level) unlocked++;
        List<String> lore = new ArrayList<>(List.of("&7View all the rewards you can unlock", "&7by leveling up your SkyBlock Level.", ""));
        lore.addAll(LevelingText.unlocked("Progress to Max", unlocked, data.rewards().size()));
        lore.addAll(List.of("", "&eClick to view rewards!"));
        return new Icon(Material.CHEST, "&aLeveling Rewards", lore);
    }

    /** The switch, on as recorded; off, a gray dye (the wiki's menu has one). */
    static Icon chat(boolean on) {
        return new Icon(on ? Material.LIME_DYE : Material.GRAY_DYE, "&bSkyBlock Levels in Chat", "&7View other players' SkyBlock Level",
                "&7and their selected emblem in their", "&7chat messages.", "", on ? "&a&lENABLED" : "&c&lDISABLED", "",
                on ? "&eClick to disable!" : "&eClick to enable!");
    }
}
