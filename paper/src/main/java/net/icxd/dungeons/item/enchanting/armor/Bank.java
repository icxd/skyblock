package net.icxd.dungeons.item.enchanting.armor;

import net.icxd.dungeons.economy.DeathCoins;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Bank, the armor ultimate: "Saves 50% of your coins on death. Additionally, enemies drop +2.5 coins when killed."
 * Its share of what a death takes from the purse is a {@link DeathCoins#addSaver} share: the pieces' add up ("It
 * decreases Coins lost on death by 50%, stacking additively", the wiki's Bank; "Wearing two pieces with Bank V saves
 * all of the player's coins", the wiki's Death), and DeathCoins keeps it to all of them at most. A kill of theirs
 * gives each piece's coins into their purse, as a kill's own coins go (KillCoins): "Gives 2.5 coins to the player
 * when a mob is killed by them" (the wiki's Bank). Main thread.
 */
final class Bank {
    static final String ID = "bank";

    private Bank() {
    }

    static void register() {
        DeathCoins.addSaver(player -> saved(WornEnchants.of(player)));
    }

    /** The share of a death's loss their pieces save (0.5 for one Bank V), added up. */
    static double saved(List<WornEnchants.Piece> pieces) {
        return sum(pieces, 0) / 100;
    }

    /** The coins each kill of theirs gives on top. UNKNOWN whether the pieces' add up, as their savings do: here they do. */
    static double killCoins(List<WornEnchants.Piece> pieces) {
        return sum(pieces, 1);
    }

    /** They killed a mob: its coins for their pieces, into their purse. */
    static void killed(Player killer) {
        User user = User.ifLoaded(killer.getUniqueId());
        if (user == null || user.isReleased()) return;
        double coins = killCoins(WornEnchants.of(killer));
        if (coins > 0) Purse.add(user, coins);
    }

    private static double sum(List<WornEnchants.Piece> pieces, int index) {
        double sum = 0;
        for (WornEnchants.Piece piece : pieces) {
            int level = piece.level(ID);
            if (level > 0) sum += EnchantNumbers.get(ID, level, index);
        }
        return sum;
    }
}
