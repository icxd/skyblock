package net.icxd.dungeons.item.ability.utility;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import net.icxd.dungeons.dungeons.instance.RunItems;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.utils.Replacement;

/**
 * The Dungeonbreaker's Dungeon Breaker (DIG: a left click on a block): "While in The Catacombs, consume 1⸕ charge
 * to break a block. 20 blocks can be broken at a time, and re-appear after 10s. 2⸕ charges are regenerated each
 * second. Charges: 20/20⸕" (its text's numbers). Where it may break is the run's rule (see RunItems#mayBreak: not
 * in puzzle rooms, doors, crypts or the walls between rooms). The block goes at once, and comes back as it was 10
 * seconds later, once nobody stands in it (UNKNOWN: a player inside would suffocate). Nothing that holds something or
 * is used by a click (a chest, a lever, a skull), nor what can't be broken at all, is broken (UNKNOWN). The charges
 * are the player's, shown on the action bar when one is used (UNKNOWN where Hypixel shows them: the item's line stays
 * its data's). A click it can't break does nothing. Main thread.
 */
final class DungeonBreaker implements AbilityHandler {
    private static final Pattern CONSUME = Pattern.compile("consume (\\d+)⸕ charge");
    private static final Pattern AT_A_TIME = Pattern.compile("(\\d+) blocks can be broken at a time");
    private static final Pattern BACK = Pattern.compile("re-appear after (\\d+)s");
    private static final Pattern REGEN = Pattern.compile("(\\d+)⸕ charges are regenerated each second");
    private static final Pattern MAX = Pattern.compile("Charges: (\\d+)/(\\d+)⸕");

    /** Its numbers, from its text. */
    record Rules(int cost, int atATime, long backMillis, double regenPerSecond, int max) {
        static Rules of(ItemBlock block) {
            String plain = AbilityText.plain(block.text());
            return new Rules((int) number(CONSUME, plain, 1, 1), (int) number(AT_A_TIME, plain, 1, 20), (long) (number(BACK, plain, 1, 10) * 1000),
                    number(REGEN, plain, 1, 2), (int) number(MAX, plain, 2, 20));
        }
    }

    /** A player's charges: how many, as of when. */
    private static final class Charges {
        double left = -1;
        long at;
    }

    /** A broken block: what it was, whose it is, and when it comes back. */
    private record Broken(Block block, BlockData was, UUID by, long backAt) {
    }

    private static final Map<UUID, Charges> CHARGES = new HashMap<>();
    private static final Deque<Broken> BROKEN = new ArrayDeque<>();

    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block, Trigger trigger) {
        Block at = trigger.block();
        if (at == null || !breakable(at) || !RunItems.mayBreak(player, at)) return false;
        Rules rules = Rules.of(block);
        return charges(player.getUniqueId(), rules, System.currentTimeMillis()) >= rules.cost() && broken(player.getUniqueId()) < rules.atATime();
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block, boolean vitalityPaid, Trigger trigger) {
        Block at = trigger.block();
        if (at == null) return;
        Rules rules = Rules.of(block);
        long now = System.currentTimeMillis();
        double left = charges(player.getUniqueId(), rules, now) - rules.cost();
        Charges charges = CHARGES.get(player.getUniqueId());
        charges.left = left;
        charges.at = now;
        BROKEN.add(new Broken(at, at.getBlockData(), player.getUniqueId(), now + rules.backMillis()));
        at.getWorld().spawnParticle(Particle.BLOCK, at.getLocation().add(0.5, 0.5, 0.5), 12, 0.3, 0.3, 0.3, 0, at.getBlockData());
        at.getWorld().playSound(at.getLocation(), at.getBlockData().getSoundGroup().getBreakSound(), 1, 1);
        at.setType(Material.AIR, false);
        PlayerSession.of(player).setDefenseReplacement(Replacement.forMillis("§e" + (int) Math.floor(charges.left) + "/" + rules.max() + "⸕ Charges", 1000));
    }

    /** Whether a block is one it breaks: something solid there, holding nothing and not used by a click, and breakable at all. */
    static boolean breakable(Block block) {
        Material type = block.getType();
        if (type.isAir() || block.isLiquid() || type.getHardness() < 0 || type.isInteractable()) return false;
        return !(block.getState(false) instanceof TileState);
    }

    /** Their charges now: full at first, back by the regen a second up to the most. */
    private static double charges(UUID player, Rules rules, long now) {
        Charges charges = CHARGES.computeIfAbsent(player, id -> new Charges());
        if (charges.left < 0) {
            charges.left = rules.max();
            charges.at = now;
        }
        return regained(charges.left, now - charges.at, rules.regenPerSecond(), rules.max());
    }

    /** {@code left} charges {@code millis} ago, back by {@code perSecond}, up to {@code max}. */
    static double regained(double left, long millis, double perSecond, int max) {
        return Math.min(max, left + perSecond * Math.max(0, millis) / 1000.0);
    }

    /** How many of theirs are broken now. */
    private static int broken(UUID player) {
        int count = 0;
        for (Broken b : BROKEN) if (b.by().equals(player)) count++;
        return count;
    }

    /** Every tick: blocks whose time is up come back, where nothing has taken their place and nobody stands. */
    static void tick() {
        if (BROKEN.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Iterator<Broken> it = BROKEN.iterator(); it.hasNext(); ) {
            Broken b = it.next();
            if (now < b.backAt()) continue;
            if (!b.block().getWorld().isChunkLoaded(b.block().getX() >> 4, b.block().getZ() >> 4)) {
                it.remove();
                continue;
            }
            if (occupied(b.block())) continue;
            it.remove();
            if (b.block().getType().isAir()) b.block().setBlockData(b.was(), false);
        }
    }

    /** Whether a player stands in the block's space. */
    private static boolean occupied(Block block) {
        BoundingBox box = BoundingBox.of(block);
        for (Player player : block.getWorld().getPlayers()) if (player.getBoundingBox().overlaps(box)) return true;
        return false;
    }

    private static double number(Pattern pattern, String plain, int group, double otherwise) {
        Matcher m = pattern.matcher(plain);
        return m.find() ? Double.parseDouble(m.group(group)) : otherwise;
    }

    static void forget(UUID player) {
        CHARGES.remove(player);
    }

    /** The plugin is going: every broken block comes back now. */
    static void restoreAll() {
        for (Broken b : BROKEN) if (b.block().getType().isAir()) b.block().setBlockData(b.was(), false);
        BROKEN.clear();
    }
}
