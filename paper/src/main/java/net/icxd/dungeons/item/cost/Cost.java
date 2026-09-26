package net.icxd.dungeons.item.cost;

import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;

/** Something an upgrade (or a gemstone slot) costs. Checking and paying are separate, so nothing is taken unless all of it can be. */
public abstract class Cost {
    /** Whether the player has it. */
    public abstract boolean canPay(Player player, User user);

    /** Takes it; only after {@link #canPay} said yes. */
    public abstract void pay(Player player, User user);
}
