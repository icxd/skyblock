package net.icxd.dungeons.stats;

import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.dwarven.Perk;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** A player's stats. Use {@link net.icxd.dungeons.session.PlayerSession#stats()}, which keeps them for the tick. */
public final class PlayerStats {
    /** What else changes a player's stats (a dungeon class's bonuses), after everything else, in order. */
    private static final List<BiConsumer<Player, Stats>> MODIFIERS = new ArrayList<>();

    private PlayerStats() {
    }

    /** Adds something that changes players' stats: it gets their stats once the rest are in, to add to or scale. */
    public static void addModifier(BiConsumer<Player, Stats> modifier) {
        MODIFIERS.add(modifier);
    }

    /**
     * The base, the armor they wear, what they hold (unless its stats only count when worn or
     * equipped, see {@link SkyBlockItem#statsWhenHeld()}), their skill levels' bonuses (see
     * {@link Skills#stats}), their Heart of the Mountain perks, and then what the modifiers (see
     * {@link #addModifier}) make of them; in a dungeon run, its blessings on top of all that (they
     * multiply what the rest adds up to, the class's stats included: the order is UNKNOWN).
     */
    public static Stats of(Player player) {
        Stats stats = Stats.base();
        PlayerInventory inventory = player.getInventory();
        ItemStack hand = inventory.getItemInMainHand();
        NBTTagCompound handTag = ItemNBT.read(hand);
        SkyBlockItem held = handTag == null ? null : ItemRegistry.get(handTag.getString("id"));
        if (held == null || held.statsWhenHeld()) stats.add(ItemStats.of(hand, player));
        for (ItemStack armor : inventory.getArmorContents()) stats.add(ItemStats.of(armor, player));
        User user = User.ifLoaded(player.getUniqueId());
        if (user != null) {
            stats.add(Skills.stats(user.profile()));
            for (Perk perk : Perk.values()) {
                Integer level = user.profileValue("dwarvenMines.hotm.tree." + perk.name(), Integer.class);
                if (level != null && level > 0) stats.add(perk.getStats().apply(level));
            }
        }
        for (BiConsumer<Player, Stats> modifier : MODIFIERS) modifier.accept(player, stats);
        DungeonRun run = RunManager.of(player);
        if (run != null) run.applyBlessings(stats);
        // Last: the Terminator divides Crit Chance, whatever it came from.
        if (held != null) ItemBehaviours.of(held).whileHeld(player, handTag, stats);
        return stats;
    }
}
