package net.icxd.dungeons.leveling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.leveling.LevelingData.Reward;
import net.icxd.dungeons.leveling.LevelingData.RewardKind;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.User;

/**
 * Leveling Rewards, as recorded (01:53.4): the four kinds of reward besides stats, each with the next one and how many
 * are unlocked, opening its list ({@link RewardListMenu}), and the stats every level gives. Main thread.
 */
public final class RewardsMenu extends GUI {
    public static final String TITLE = "Leveling Rewards";
    static final int FEATURES = 11;
    static final int PREFIXES = 12;
    static final int EMBLEMS = 13;
    static final int STATS = 14;
    static final int BONUSES = 15;
    static final int BACK = 30;
    static final int CLOSE = 31;

    private final Player viewer;

    public RewardsMenu(Player viewer) {
        super(TITLE, Size.FOUR);
        this.viewer = viewer;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        LevelingView view = LevelingView.of(viewer, user);
        Map<Integer, Icon> icons = icons(view);
        for (Map.Entry<Integer, Icon> entry : icons.entrySet()) {
            int slot = entry.getKey();
            RewardKind kind = switch (slot) {
                case FEATURES -> RewardKind.FEATURE;
                case PREFIXES -> RewardKind.PREFIX;
                case EMBLEMS -> RewardKind.EMBLEM;
                case BONUSES -> RewardKind.BONUS;
                default -> null;
            };
            if (kind == null) set(slot, entry.getValue().stack());
            else set(GUIClickableItem.button(slot, entry.getValue().stack(), viewer, () -> new RewardListMenu(viewer, kind).open(viewer)));
        }
        set(GUIClickableItem.button(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Leveling").stack(), viewer,
                () -> new LevelingMenu(viewer).open(viewer)));
        set(GUIClickableItem.close(CLOSE));
    }

    static Map<Integer, Icon> icons(LevelingView view) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(FEATURES, kind(view, RewardKind.FEATURE, true));
        icons.put(PREFIXES, kind(view, RewardKind.PREFIX, true));
        icons.put(EMBLEMS, kind(view, RewardKind.EMBLEM, true));
        icons.put(STATS, stats(view.level()));
        icons.put(BONUSES, kind(view, RewardKind.BONUS, true));
        return icons;
    }

    /** What a kind of reward is, as its item says. */
    static List<String> description(RewardKind kind) {
        return switch (kind) {
            case FEATURE -> List.of("&7Specific game features such as the", "&7Bazaar or Community Shop.");
            case PREFIX -> List.of("&7New colors for your level prefix", "&7shown in TAB and in chat!");
            case EMBLEM -> List.of("&7Emblems to show next to your name", "&7that signify special achievements.");
            case BONUS -> List.of("&7Bonuses that upgrade your Book of", "&7Progression which will provide you", "&7better rewards!");
        };
    }

    /** "Feature Rewards" and the rest. */
    static String name(RewardKind kind) {
        return switch (kind) {
            case FEATURE -> "&aFeature Rewards";
            case PREFIX -> "&aPrefix Color Rewards";
            case EMBLEM -> "&aPrefix Emblem Rewards";
            case BONUS -> "&aBonus Rewards";
        };
    }

    /**
     * A kind's item: what it is, the next reward of it and how many they've unlocked; with {@code click}, "Click to
     * view rewards!" (its list's top has none). Prefix colours show their current colour's dye (gray for none yet:
     * the wiki's, at level 0). With none left, there's no Next Reward (UNKNOWN on Hypixel).
     */
    static Icon kind(LevelingView view, RewardKind kind, boolean click) {
        List<Reward> rewards = view.data().rewards(kind);
        Reward next = null, last = null;
        int unlocked = 0;
        for (Reward reward : rewards) {
            if (reward.level() <= view.level()) {
                unlocked++;
                last = reward;
            } else if (next == null) {
                next = reward;
            }
        }
        List<String> lore = new ArrayList<>(description(kind));
        lore.add("");
        if (next != null) lore.addAll(List.of("&7Next Reward:", next.name(), "&8at Level " + next.level(), ""));
        lore.addAll(LevelingText.unlocked("Rewards Unlocked", unlocked, rewards.size()));
        if (click) lore.addAll(List.of("", "&eClick to view rewards!"));
        Material material = switch (kind) {
            case FEATURE -> Material.NETHER_STAR;
            case PREFIX -> last == null ? Material.GRAY_DYE : last.look().material();
            case EMBLEM -> Material.NAME_TAG;
            case BONUS -> Material.BOOK;
        };
        return new Icon(material, click ? name(kind) : "&aRewards", lore);
    }

    /** Stat Rewards: the next level's, and what every level and every fifth give. Not a button (as recorded). */
    static Icon stats(int level) {
        List<String> lore = new ArrayList<>(List.of("&7Statistic bonuses that will power you", "&7up as you level up.", "", "&7Next Reward:"));
        lore.addAll(LevelRewards.statLines(level + 1));
        lore.addAll(List.of("&8at Level " + (level + 1), "", "&7For every level:", LevelRewards.healthLine(LevelRewards.HEALTH), "",
                "&7For every 5 levels:", LevelRewards.strengthLine(LevelRewards.STRENGTH)));
        return new Icon(Material.DIAMOND_HELMET, "&aStat Rewards", lore);
    }
}
