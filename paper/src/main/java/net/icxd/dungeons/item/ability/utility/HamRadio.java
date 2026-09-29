package net.icxd.dungeons.item.ability.utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.behaviour.HeldStats;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Utils;

/**
 * The Blazetekk™ Ham Radio. Telecommunications: "If exactly two players within 110 blocks are on the same channel,
 * both gain: +60❈ Defense +40❂ True Defense +5☯ Combat Wisdom +10♨ Vitality. Earn half stats while alone!" (its text's
 * stats and range); Tuning 4 Dummies (CLICK): "Switches to the next radio channel." The wiki's Blazetekk™ Ham Radio
 * has the channels and the lines: "Your radio is weak. Find another enjoyer to boost it." alone, "Your radio signal is
 * strong!" with one other, and "Your radio lost signal. There's too many enjoyers on this channel." with more (then
 * no stats: the text's "exactly two"). A radio anywhere in their inventory counts (UNKNOWN where Hypixel's must be),
 * its channel kept on it ({@link HeldStats#HAM_RADIO_CHANNEL}, its lore line follows). Once a second each carrier's
 * signal is worked out, with a line when it changes; the stats are that second's. Main thread.
 */
final class HamRadio implements AbilityHandler {
    static final String ID = "BLAZETEKK_HAM_RADIO";
    /** Telecommunications' range, if its text doesn't say. */
    private static final double RANGE = 110;

    /** How strong a carrier's signal is: alone (half), with exactly one other (all), or lost (none). */
    enum Signal {
        WEAK(0.5, "&7Your radio is weak. Find another enjoyer to boost it."),
        STRONG(1, "&7Your radio signal is strong!"),
        LOST(0, "&7Your radio lost signal. There's too many enjoyers on this channel.");

        final double share;
        final String line;

        Signal(double share, String line) {
            this.share = share;
            this.line = line;
        }

        /** With this many carriers on the channel in reach, them included. */
        static Signal of(int onChannel) {
            return onChannel <= 1 ? WEAK : onChannel == 2 ? STRONG : LOST;
        }
    }

    /** A carrier's radio this second: its channel and signal, and the stats that gives. */
    private record Tuned(int channel, Signal signal, Stats stats) {
    }

    private static final Map<UUID, Tuned> TUNED = new HashMap<>();

    /** The next channel. */
    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        if (InventorySyncListener.frozen(player)) return;
        int channel = (HeldStats.hamRadioChannel(tag) + 1) % HeldStats.RADIO_CHANNELS.size();
        tag.setInt(HeldStats.HAM_RADIO_CHANNEL, channel);
        ItemStack held = player.getInventory().getItemInMainHand();
        player.getInventory().setItemInMainHand(ItemBuilder.build(item, tag, held.getAmount(), player));
        player.sendMessage(Utils.color("&7Your channel: " + HeldStats.RADIO_CHANNELS.get(channel)));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1.5f);
        TUNED.remove(player.getUniqueId());
        second();
    }

    /** Their radio's stats this second (none without one). */
    static void stats(Player player, Stats stats) {
        Tuned tuned = TUNED.get(player.getUniqueId());
        if (tuned != null) stats.add(tuned.stats());
    }

    /** Once a second: each carrier's channel and signal, and a line to those whose signal changed. */
    static void second() {
        Map<UUID, Integer> channels = new HashMap<>();
        Map<UUID, ItemBlock> blocks = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            NBTTagCompound tag = radio(player);
            if (tag == null) continue;
            channels.put(player.getUniqueId(), HeldStats.hamRadioChannel(tag));
            SkyBlockItem item = ItemRegistry.get(ID);
            ItemBlock block = item == null ? null : telecommunications(item.blocks());
            if (block != null) blocks.put(player.getUniqueId(), block);
        }
        TUNED.keySet().removeIf(id -> !channels.containsKey(id));
        for (Map.Entry<UUID, Integer> entry : channels.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            ItemBlock block = blocks.get(entry.getKey());
            if (player == null || block == null) continue;
            String plain = AbilityText.plain(block.text());
            double range = AbilityText.after(plain, "If exactly two players within").orElse(RANGE);
            int onChannel = 0;
            for (Map.Entry<UUID, Integer> other : channels.entrySet()) {
                Player them = Bukkit.getPlayer(other.getKey());
                if (them == null || !other.getValue().equals(entry.getValue()) || !them.getWorld().equals(player.getWorld())) continue;
                if (them.getLocation().distanceSquared(player.getLocation()) <= range * range) onChannel++;
            }
            Signal signal = Signal.of(onChannel);
            Tuned before = TUNED.put(player.getUniqueId(), new Tuned(entry.getValue(), signal, times(AbilityText.stats(plain), signal.share)));
            if (before == null || before.signal() != signal) player.sendMessage(Utils.color(signal.line));
        }
    }

    /** The stats times a share. */
    static Stats times(Stats stats, double share) {
        Stats out = new Stats();
        for (Stat stat : Stat.values()) if (stats.has(stat)) out.set(stat, stats.get(stat) * share);
        return out;
    }

    /** Their radio's data: the one they hold, else the first in their inventory; null for none. */
    private static NBTTagCompound radio(Player player) {
        NBTTagCompound held = ItemNBT.read(player.getInventory().getItemInMainHand());
        if (held != null && ID.equals(held.getString("id"))) return held;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            NBTTagCompound tag = ItemNBT.read(stack);
            if (tag != null && ID.equals(tag.getString("id"))) return tag;
        }
        return null;
    }

    private static ItemBlock telecommunications(List<ItemBlock> blocks) {
        for (ItemBlock block : blocks) if (block.isAbility() && "Telecommunications".equals(block.name())) return block;
        return null;
    }

    static void forget(UUID player) {
        TUNED.remove(player);
    }
}
