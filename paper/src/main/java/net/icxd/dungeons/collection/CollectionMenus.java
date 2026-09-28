package net.icxd.dungeons.collection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Material;

import net.icxd.dungeons.collection.CollectionData.Category;
import net.icxd.dungeons.collection.CollectionData.Collection;
import net.icxd.dungeons.collection.CollectionData.Reward;
import net.icxd.dungeons.collection.CollectionData.Tier;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.skill.SkillText;

/**
 * What the collection menus show, slot by slot, for a profile: Collections, a category's ("Combat
 * Collections", "Boss Collections"), a collection's tiers ("Blaze Rod Collection") and a tier's rewards
 * ("Blaze Rod I Rewards"), as recorded (the SkyBlock Menu tour, 00:40-01:04). A slot that isn't in a map is
 * empty. No server needed.
 */
public final class CollectionMenus {
    public static final int TOP = 4;
    public static final int BACK = 48;
    public static final int CLOSE = 49;
    public static final int CRAFTED_MINIONS = 50;
    public static final int RANKINGS = 53;
    public static final int BOSSES = 31;
    /** The main menu's categories, in the recorded slots. */
    static final int[] CATEGORY_SLOTS = {20, 21, 22, 23, 24};
    /** A category menu's collections: the seven-wide inside of its border, row by row (the recorded Combat Collections). */
    static final int[] INSIDE = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39,
            40, 41, 42, 43};
    /** A category's icon in the menus (the recording's). */
    static final Map<String, Material> CATEGORY_ICONS = Map.of("FARMING", Material.GOLDEN_HOE, "MINING", Material.STONE_PICKAXE,
            "COMBAT", Material.STONE_SWORD, "FORAGING", Material.JUNGLE_SAPLING, "FISHING", Material.FISHING_ROD);
    /** Cobblestone Minion XI's head, Crafted Minions' (the recording's packets; the wiki's Collections UI names it). */
    static final String CRAFTED_MINIONS_ITEM = "COBBLESTONE_GENERATOR_11";
    /**
     * Where a collection's tiers go (and a tier's rewards): up to eight on the third row, as the wiki's
     * Collection UI module places them (the recorded 6, 8 and 9 agree); from nine on, a row each nine (UNKNOWN
     * past nine: the wiki's way).
     */
    private static final int[][] ROW = {{}, {22}, {21, 23}, {20, 22, 24}, {19, 21, 23, 25}, {20, 21, 22, 23, 24}, {19, 20, 21, 23, 24, 25},
            {19, 20, 21, 22, 23, 24, 25}, {18, 19, 20, 21, 23, 24, 25, 26}};

    private CollectionMenus() {
    }

    /** The slots {@code count} tiers (or rewards) go in, in order. */
    static int[] rowSlots(int count) {
        if (count < ROW.length) return ROW[count];
        int[] slots = new int[Math.min(count, 27)];
        for (int i = 0; i < slots.length; i++) slots[i] = 18 + i;
        return slots;
    }

    static MenuSlot back(String to) {
        return MenuSlot.of(new Icon(Material.ARROW, "&aGo Back", "&7To " + to));
    }

    static MenuSlot close() {
        return MenuSlot.of(new Icon(Material.BARRIER, "&cClose"));
    }

    /** Every slot glass but these. */
    private static Map<Integer, MenuSlot> glass(int... except) {
        Map<Integer, MenuSlot> slots = new LinkedHashMap<>();
        outer:
        for (int slot = 0; slot < 54; slot++) {
            for (int e : except) if (e == slot) continue outer;
            slots.put(slot, MenuSlot.FILLER);
        }
        return slots;
    }

    /** A category menu's glass: its border only (the recorded Combat Collections and Boss Collections). */
    private static Map<Integer, MenuSlot> border() {
        Map<Integer, MenuSlot> slots = new LinkedHashMap<>();
        for (int slot = 0; slot < 54; slot++) {
            int row = slot / 9, column = slot % 9;
            if (row == 0 || row == 5 || column == 0 || column == 8) slots.put(slot, MenuSlot.FILLER);
        }
        return slots;
    }

    /** The item a collection shows: its own item's look (a boss's head), with this name and lore. */
    static Icon look(Collection collection, String name, List<String> lore) {
        if (collection.boss()) {
            if (collection.texture() != null) return new Icon(Material.PLAYER_HEAD, name, lore, collection.texture());
            // Necron's is a wither skeleton skull (NEU's NECRON_BOSS, the wiki's Boss Collections UI); Kuudra's is UNKNOWN.
            return new Icon(collection.floor() > 0 ? Material.WITHER_SKELETON_SKULL : Material.PLAYER_HEAD, name, lore);
        }
        SkyBlockItem item = ItemRegistry.get(collection.item());
        if (item == null) return new Icon(Material.BARRIER, name, lore);
        return new Icon(item.material(), name, lore, item.material() == Material.PLAYER_HEAD ? MenuSlot.hash(item.skin()) : null);
    }

    // Collections

    /** The Collections item (the SkyBlock Menu's has "Click to view!" too): how many of the 83 are found. */
    public static Icon summary(Document profile, boolean click) {
        int[] found = Collections.foundOfAll(profile);
        return summary(found[0], found[1], click);
    }

    public static Icon summary(int found, int all, boolean click) {
        List<String> lore = new ArrayList<>(List.of("&7View all of the items available in", "&7SkyBlock. Collect more of an item to",
                "&7unlock rewards on your way to", "&7becoming a master of SkyBlock!", "",
                "&7Collections Unlocked: " + CollectionText.unlocked(fraction(new int[] {found, all})), CollectionText.bar(found, all), "",
                "&8Also accessible via /collection."));
        if (click) lore.addAll(List.of("", "&eClick to view!"));
        return new Icon(Material.PAINTING, "&aCollections", lore);
    }

    private static double fraction(int[] of) {
        return of[1] <= 0 ? 0 : (double) of[0] / of[1];
    }

    /** The Collections menu. {@code craftedMinions}: the unique minions the profile has crafted (none: minions come later). */
    public static Map<Integer, MenuSlot> collections(Document profile, int craftedMinions, int minionItems) {
        Map<Integer, MenuSlot> slots = glass(TOP, 20, 21, 22, 23, 24, BOSSES, BACK, CLOSE, CRAFTED_MINIONS, RANKINGS);
        slots.put(TOP, MenuSlot.of(summary(profile, false)));
        List<Category> categories = Collections.data().categories();
        for (int i = 0; i < categories.size() && i < CATEGORY_SLOTS.length; i++) {
            slots.put(CATEGORY_SLOTS[i], MenuSlot.of(categoryIcon(profile, categories.get(i), true)));
        }
        slots.put(BOSSES, MenuSlot.of(bossesIcon(profile, true)));
        slots.put(BACK, back("SkyBlock Menu"));
        slots.put(CLOSE, close());
        slots.put(CRAFTED_MINIONS, MenuSlot.of(craftedMinions(craftedMinions, minionItems)));
        slots.put(RANKINGS, MenuSlot.of(new Icon(Material.OAK_SIGN, "&aShow Collection Rankings", "&7Show the rankings display for your",
                "&7Collections.", "", "&eClick to show!")));
        return slots;
    }

    /** "&aCombat Collections": how many of the category's are found. */
    static Icon categoryIcon(Document profile, Category category, boolean click) {
        int found = Collections.found(profile, category);
        int all = category.collections().size();
        List<String> lore = new ArrayList<>(List.of("&7View your &a" + category.name() + " Collections&7!", "",
                "&7Collections Unlocked: " + CollectionText.unlocked(all == 0 ? 0 : (double) found / all), CollectionText.bar(found, all)));
        if (click) lore.addAll(List.of("", "&eClick to view!"));
        return new Icon(CATEGORY_ICONS.getOrDefault(category.id(), Material.PAINTING), "&a" + category.name() + " Collections", lore);
    }

    /** "&5Boss Collections": how many bosses have been killed once. */
    static Icon bossesIcon(Document profile, boolean click) {
        int[] found = Collections.bossesFound(profile);
        List<String> lore = new ArrayList<>(List.of("&7View your progress and claim", "&7rewards you have obtained from",
                "&7defeating SkyBlock bosses!", "", "&7Boss Collections Unlocked: " + CollectionText.unlocked(fraction(found)),
                CollectionText.bar(found[0], found[1])));
        if (click) lore.addAll(List.of("", "&eClick to view!"));
        return new Icon(Material.WITHER_SKELETON_SKULL, "&5Boss Collections", lore);
    }

    /**
     * Crafted Minions, as recorded, with the profile's numbers: the unique minions crafted of how many
     * there are, and the minion limit. Minions aren't in the plugin yet (LATER), so the limit is the five
     * everyone starts with and the next slot the wiki's first step (5 unique minions); the rest of the
     * wiki's table and the Community Shop's slots come with them.
     */
    static Icon craftedMinions(int crafted, int minionItems) {
        String texture = null;
        SkyBlockItem head = ItemRegistry.get(CRAFTED_MINIONS_ITEM);
        if (head != null) texture = MenuSlot.hash(head.skin());
        int limit = 5;
        int more = Math.max(0, 5 - crafted);
        return new Icon(Material.PLAYER_HEAD, "&aCrafted Minions", List.of("&7This menu shows all of the unique",
                "&7minions that you have crafted so far.", "", "&7Craft minions &ayou have never crafted",
                "&abefore &7to increase the minions limit on", "&7your Private Island! You can also unlock",
                "&75 bonus slots via the &bCommunity Shop &7in", "&7the Hub!", "", "&7Crafted minions: &e" + crafted + "&7/&6" + minionItems,
                "&7Minion limit: &e" + limit + "&7/&632", "", "&7Craft &b" + more + " &7more &aunique &7minions to unlock",
                "&7your &b" + ordinal(limit + 1) + " &7slot.", "", "&eClick to view!"), texture);
    }

    /** "6th", "21st", "22nd", "13th". */
    static String ordinal(int n) {
        int mod100 = n % 100;
        String suffix = mod100 >= 11 && mod100 <= 13 ? "th" : switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
        return n + suffix;
    }

    // A category's collections

    /** "Combat Collections": its collections, found or not, inside the border. */
    public static Map<Integer, MenuSlot> category(Document profile, Category category) {
        Map<Integer, MenuSlot> slots = border();
        slots.put(TOP, MenuSlot.of(categoryIcon(profile, category, false)));
        List<String> ids = category.collections();
        for (int i = 0; i < ids.size() && i < INSIDE.length; i++) {
            Collection collection = Collections.data().collection(ids.get(i));
            slots.put(INSIDE[i], MenuSlot.of(collectionIcon(profile, collection)));
        }
        slots.put(BACK, back("Collections"));
        slots.put(CLOSE, close());
        return slots;
    }

    /** "Boss Collections": each boss, killed once or not. */
    public static Map<Integer, MenuSlot> bosses(Document profile) {
        Map<Integer, MenuSlot> slots = border();
        slots.put(TOP, MenuSlot.of(bossesIcon(profile, false)));
        List<Collection> bosses = Collections.data().bosses();
        for (int i = 0; i < bosses.size() && i < INSIDE.length; i++) slots.put(INSIDE[i], MenuSlot.of(collectionIcon(profile, bosses.get(i))));
        slots.put(BACK, back("Collections"));
        slots.put(CLOSE, close());
        return slots;
    }

    /** "View all your &aBlaze Rod Collection" / "progress and rewards!" (a boss's name isn't green), wrapped. */
    static List<String> viewAll(Collection collection) {
        if (collection.boss()) return CollectionText.wrap("&7View all your " + collection.name() + " Collection progress and rewards!");
        return CollectionText.wrap("&7View all your &a" + collection.name() + " Collection &7progress and rewards!");
    }

    /**
     * A collection in its category's menu: its tier and the next one's progress and rewards, or maxed; a gray
     * dye while it's never been found (a boss never killed).
     */
    static Icon collectionIcon(Document profile, Collection collection) {
        long count = Collections.count(profile, collection.id());
        if (count <= 0) {
            if (collection.boss()) {
                List<String> lore = new ArrayList<>(List.of("&7Kill this boss once to view collection", "&7rewards!", ""));
                lore.addAll(CollectionText.wrap("&cKill " + collection.name() + " once to view this collection!"));
                return new Icon(Material.GRAY_DYE, "&c" + collection.name(), lore);
            }
            return new Icon(Material.GRAY_DYE, "&c" + collection.name(), "&7Find this item to add it to your",
                    "&7collection and unlock collection", "&7rewards!", "", "&cYou haven't found this item yet!");
        }
        int tier = Collections.tier(collection, count);
        List<String> lore = new ArrayList<>(viewAll(collection));
        lore.add("");
        boolean maxed = tier >= collection.maxTier();
        if (maxed) {
            // Recorded for an item collection; a boss's maxed lore is UNKNOWN, taken the same.
            lore.add("&a&lCOLLECTION MAXED OUT!");
            lore.add("&7Total collected: &e" + SkillText.number(count));
        } else {
            Tier next = collection.tier(tier + 1);
            String nextName = CollectionText.named(collection, tier + 1);
            lore.add("&7Progress to " + nextName + ": " + CollectionText.progress(CollectionText.fraction(count, next.amount())));
            lore.add(CollectionText.bar(count, next.amount()));
            lore.add("");
            // A boss's lists its rewards without the SkyBlock XP (recorded "Bonzo I Reward:" over Red Nose alone).
            List<String> rewards = CollectionText.rewardLines(next, !collection.boss());
            lore.add("&7" + nextName + " " + CollectionText.rewardsWord(rewards));
            lore.addAll(rewards);
        }
        lore.add("");
        lore.add("&eClick to view!");
        return look(collection, (maxed ? "&a" : "&e") + CollectionText.named(collection, tier), lore);
    }

    // A collection's tiers

    /** "Blaze Rod Collection": its item, and each tier, reached (lime), next (yellow) or not yet (red). */
    public static Map<Integer, MenuSlot> collection(Document profile, Collection collection) {
        int[] tierSlots = rowSlots(collection.maxTier());
        int[] except = new int[tierSlots.length + 3];
        System.arraycopy(tierSlots, 0, except, 0, tierSlots.length);
        except[tierSlots.length] = TOP;
        except[tierSlots.length + 1] = BACK;
        except[tierSlots.length + 2] = CLOSE;
        Map<Integer, MenuSlot> slots = glass(except);
        long count = Collections.count(profile, collection.id());
        int tier = Collections.tier(collection, count);
        slots.put(TOP, MenuSlot.of(look(collection, (tier >= collection.maxTier() ? "&a" : "&e") + CollectionText.named(collection, tier),
                topLore(collection, count))));
        for (int n = 1; n <= collection.maxTier() && n <= tierSlots.length; n++) {
            slots.put(tierSlots[n - 1], MenuSlot.of(tierPane(collection, n, count)).times(n));
        }
        slots.put(BACK, back(collection.boss() ? "Boss Collections" : Collections.data().category(collection.category()).name() + " Collections"));
        slots.put(CLOSE, close());
        return slots;
    }

    /** The top item's lore: how many in all ("Total Collected:"; a boss's "Total Collection:", with what completions count). */
    static List<String> topLore(Collection collection, long count) {
        List<String> lore = new ArrayList<>(viewAll(collection));
        lore.add("");
        if (!collection.boss()) {
            lore.add("&7Total Collected: &e" + SkillText.number(count));
            return lore;
        }
        lore.add("&7Total Collection: &e" + SkillText.number(count));
        if (collection.floor() > 0) {
            lore.addAll(List.of("", "&7Master Mode completions reward", "&7more Kill Count:", "&8 - &cBasic Dungeons: &e+1 Kill Count",
                    "&8 - &cMaster Mode: &e+2 Kill Count"));
        }
        return lore;
    }

    /** Where a count stands against tier {@code n}: 'a' reached, 'e' the next one, 'c' later. */
    static char state(Collection collection, int n, long count) {
        int tier = Collections.tier(collection, count);
        return n <= tier ? 'a' : n == tier + 1 ? 'e' : 'c';
    }

    /** A tier's pane: its progress and rewards, and "Click to view rewards!" when its Rewards menu has something to show. */
    static Icon tierPane(Collection collection, int n, long count) {
        char state = state(collection, n, count);
        Material pane = switch (state) {
            case 'a' -> Material.LIME_STAINED_GLASS_PANE;
            case 'e' -> Material.YELLOW_STAINED_GLASS_PANE;
            default -> Material.RED_STAINED_GLASS_PANE;
        };
        return new Icon(pane, "&" + state + CollectionText.named(collection, n), tierLore(collection, n, count, true));
    }

    /** "", "Progress:", the bar, "", "Rewards:" and the rewards; and the click line if asked for and there's something to see. */
    static List<String> tierLore(Collection collection, int n, long count, boolean click) {
        Tier tier = collection.tier(n);
        List<String> lore = new ArrayList<>(List.of("", "&7Progress: " + CollectionText.progress(CollectionText.fraction(count, tier.amount())),
                CollectionText.bar(count, tier.amount()), ""));
        List<String> rewards = CollectionText.rewardLines(tier, true);
        lore.add("&7" + CollectionText.rewardsWord(rewards));
        lore.addAll(rewards);
        if (click && hasRewardsMenu(tier)) {
            lore.add("");
            lore.add("&eClick to view rewards!");
        }
        return lore;
    }

    static boolean hasRewardsMenu(Tier tier) {
        for (Reward reward : tier.rewards()) if (reward.shownInRewards()) return true;
        return false;
    }

    // A tier's rewards

    /** What a boss reward item says under it: whether it's there to claim (UNKNOWN but the recorded "don't qualify"). */
    enum Claim { NOT_YET, CLAIMABLE, CLAIMED }

    /** "Blaze Rod I Rewards": the tier (its state's colour), and what it gives that has an item to show. */
    public static Map<Integer, MenuSlot> rewards(Document profile, Collection collection, int n) {
        Tier tier = collection.tier(n);
        List<Reward> shown = new ArrayList<>();
        for (Reward reward : tier.rewards()) if (reward.shownInRewards()) shown.add(reward);
        int[] rewardSlots = rowSlots(shown.size());
        int[] except = new int[rewardSlots.length + 3];
        System.arraycopy(rewardSlots, 0, except, 0, rewardSlots.length);
        except[rewardSlots.length] = TOP;
        except[rewardSlots.length + 1] = BACK;
        except[rewardSlots.length + 2] = CLOSE;
        Map<Integer, MenuSlot> slots = glass(except);
        long count = Collections.count(profile, collection.id());
        char state = state(collection, n, count);
        slots.put(TOP, MenuSlot.of(look(collection, "&" + state + CollectionText.named(collection, n), tierLore(collection, n, count, false))));
        List<Integer> claimed = collection.boss() ? Collections.claimed(profile, collection.id()) : List.of();
        for (int i = 0; i < shown.size() && i < rewardSlots.length; i++) {
            Claim claim = state != 'a' ? Claim.NOT_YET : claimed.contains(n) ? Claim.CLAIMED : Claim.CLAIMABLE;
            slots.put(rewardSlots[i], reward(shown.get(i), claim));
        }
        slots.put(BACK, back(collection.name() + " Collection"));
        slots.put(CLOSE, close());
        return slots;
    }

    /** Under a boss's reward: whether it can be claimed (UNKNOWN but the recorded "don't qualify"). */
    static String claimLine(Claim claim) {
        return switch (claim) {
            case NOT_YET -> "&cYou don't qualify for this reward!";
            // UNKNOWN: what it says once it can be claimed, and after.
            case CLAIMABLE -> "&eClick to claim!";
            case CLAIMED -> "&aYou have claimed this reward!";
        };
    }

    /** One reward's item in a Rewards menu, as the recording and the wiki's Collection UI show them. */
    static MenuSlot reward(Reward reward, Claim claim) {
        return switch (reward.type()) {
            case RECIPE -> reward.item() != null ? MenuSlot.item(reward.item(), 1, List.of("", "&eClick to view recipe!"))
                    : MenuSlot.of(new Icon(Material.PAPER, CollectionText.color(null, 'f') + reward.name(), "", "&eClick to view recipe!"));
            case MINION_RECIPES -> MenuSlot.of(minionRecipes(reward));
            // Pets aren't in the plugin yet: the head and name only (their tooltip is UNKNOWN here).
            case PET_RECIPE -> MenuSlot.of(new Icon(Material.PLAYER_HEAD, "&7[Lvl 1] &f" + reward.name(), List.of("", "&eClick to view recipe!"),
                    reward.texture()));
            case TRADE -> reward.item() != null ? MenuSlot.item(reward.item(), 1, List.of("", "&eClick to view trades!"))
                    : MenuSlot.of(new Icon(Material.PAPER, "&f" + reward.name(), "", "&eClick to view trades!"));
            case FORGE_RECIPE -> MenuSlot.of(new Icon(Material.PAPER, "&f" + reward.name(), List.of()));
            case ITEM -> {
                List<String> extra = List.of("", claimLine(claim));
                yield reward.item() != null ? MenuSlot.item(reward.item(), (int) Math.max(1, reward.amount()), extra)
                        : MenuSlot.of(new Icon(Material.PAPER, "&f" + reward.name(), extra));
            }
            // The wiki's Collection UI: the essence's head, "&dGold Essence&8 x250", and whether it can be claimed as an item's is.
            case ESSENCE -> MenuSlot.of(new Icon(Material.PLAYER_HEAD, "&d" + CollectionText.capitalized(reward.essence()) + " Essence"
                    + (reward.amount() > 0 ? "&8 x" + reward.amount() : ""), List.of("&7Essence can be used to convert",
                    "&7some items into Dungeon items", "&7and to repair Dungeon items!", "", claimLine(claim)), reward.texture()));
            default -> MenuSlot.FILLER;
        };
    }

    /**
     * "&9Blaze Minion Recipes": the tier I minion's head, and its text up to "Minions also work when you are
     * offline!" (the recorded Blaze Minion Recipes; the wiki's Collection UI cuts it there too).
     */
    static Icon minionRecipes(Reward reward) {
        SkyBlockItem minion = ItemRegistry.get(reward.item());
        List<String> lore = new ArrayList<>();
        if (minion != null) {
            String text = String.join("\n", minion.lore());
            int cut = text.indexOf("Minions also work");
            if (cut >= 0) text = text.substring(0, cut);
            for (String line : text.split("\n", -1)) lore.add(line);
            while (!lore.isEmpty() && lore.getLast().replaceAll("&[0-9a-fk-or]", "").isBlank()) lore.removeLast();
            if (!lore.isEmpty()) lore.set(lore.size() - 1, lore.getLast().stripTrailing());
        }
        lore.add("");
        lore.add("&eClick to view recipes!");
        return new Icon(Material.PLAYER_HEAD, CollectionText.color(reward.item(), '9') + reward.name() + " Minion Recipes", lore,
                minion == null ? null : MenuSlot.hash(minion.skin()));
    }
}
