package net.icxd.dungeons.hex.enchant;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.HexPage;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.utils.Text;

/**
 * "Bottles of Enchanting" (the wiki's General tab), from Experience Bottles on the Enchant Item pages: the four
 * Experience Bottles in 21, 22, 24 and 25, each saying how many experience orbs it gives them (more with their
 * Enchanting level) and what their Exp level would come to, and a click drinks one of theirs (their inventory, then
 * their storage; see HexCosts) for its orbs. Hypixel buys one from the Bazaar when they have none; there's no Bazaar
 * here, so they need their own (on a Sandbox profile, nothing: it's free). Go Back is "To Enchant Item", to the page
 * it came from. Main thread.
 */
public final class BottlesPage extends HexPage {
    public static final String TITLE = "Bottles of Enchanting";
    /** UNKNOWN: Hypixel's action (its bottles say "Click to buy on the Bazaar!"); ours. */
    static final String CONSUME = "&eClick to consume!";

    /**
     * A bottle: its item, its name as the wiki's screen colours it, and the orbs it gives at Enchanting 0 (the wiki's
     * bottles: 8, 1,500, 250,000 and 500,000; four times that at Enchanting 60).
     */
    record Bottle(String id, String name, int base) {
    }

    static final List<Bottle> BOTTLES = List.of(new Bottle("EXP_BOTTLE", "&fExperience Bottle", 8),
            new Bottle("GRAND_EXP_BOTTLE", "&aGrand Experience Bottle", 1_500),
            new Bottle("TITANIC_EXP_BOTTLE", "&9Titanic Experience Bottle", 250_000),
            new Bottle("COLOSSAL_EXP_BOTTLE", "&5Colossal Experience Bottle", 500_000));

    private final EnchantItemPage.Shown list;
    /** The level page it was opened from; null for the list. */
    private final EnchantmentData.Entry enchantment;

    BottlesPage(HexSession session, EnchantItemPage.Shown list, EnchantmentData.Entry enchantment) {
        super(session, TITLE);
        this.list = list;
        this.enchantment = enchantment;
    }

    /** The wiki's header, less the Bazaar (ours: "or purchase them from the Bazaar"). */
    @Override
    protected Icon header() {
        return new Icon(Material.ENCHANTING_TABLE, "&aConsume Experience Bottles", "&7Consume &3Experience Bottles",
                "&7from your inventories directly", "&7to increase your exp level.");
    }

    @Override
    protected Placement placement() {
        return Placement.CENTRED;
    }

    @Override
    protected List<Entry> entries() {
        Player player = session.player();
        int enchanting = Skills.level(player, Skill.ENCHANTING);
        List<Entry> entries = new ArrayList<>();
        for (Bottle bottle : BOTTLES) {
            int orbs = orbs(bottle.base(), enchanting);
            HexCosts cost = HexCosts.of(new HexCosts.Items(bottle.id(), 1));
            int after = levelAfter(player.getLevel(), player.getExp(), orbs);
            entries.add(Entry.of(icon(bottle, orbs, player.getLevel(), after, cost.lore(session, CONSUME)), () -> drink(cost, orbs)));
        }
        return entries;
    }

    @Override
    protected String backTo() {
        return "&7To Enchant Item";
    }

    @Override
    protected void back() {
        session.open(enchantment == null ? list.open(session) : new EnchantLevelPage(session, enchantment, list));
    }

    /** One of theirs drunk (nothing taken on a Sandbox profile): its orbs as vanilla experience. */
    private void drink(HexCosts cost, int orbs) {
        if (!cost.pay(session)) {
            say("&cYou don't have that in your inventories!");
            return;
        }
        session.player().giveExp(orbs);
        redraw();
    }

    // What it shows

    /**
     * A bottle: "&7Grants &3<orbs> &7experience orbs." wrapped (the wiki's; its "Buying this directly will instantly
     * consume it!" is the Bazaar's, left out), their Exp level now and after it, then the Cost block.
     */
    static Icon icon(Bottle bottle, int orbs, int level, int after, List<String> block) {
        List<String> lore = new ArrayList<>(Text.wrap("&7Grants &3" + Text.number(orbs) + " &7experience orbs.", Text.LORE_WIDTH));
        lore.add("");
        lore.add("&7Your Exp Level: &3" + level);
        lore.add("&7Level When Applied: &3" + after);
        lore.add("");
        lore.addAll(block);
        return new Icon(Material.EXPERIENCE_BOTTLE, bottle.name(), lore);
    }

    /** The orbs a bottle gives at this Enchanting level: 5% more a level, rounded down (the wiki's Experience Bottle table). */
    static int orbs(int base, int enchanting) {
        return (int) ((long) base * (20 + Math.max(0, enchanting)) / 20);
    }

    /** The points from one vanilla Exp level to the next (Minecraft's). */
    static int toNext(int level) {
        return level >= 30 ? 112 + (level - 30) * 9 : level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }

    /** The Exp level they'd have with {@code orbs} more points, at {@code level} and {@code progress} (0-1) of the way to the next. */
    static int levelAfter(int level, float progress, long orbs) {
        long points = Math.round(progress * toNext(level)) + orbs;
        while (points >= toNext(level)) {
            points -= toNext(level);
            level++;
        }
        return level;
    }
}
