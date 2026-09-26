package net.icxd.dungeons.item.cost.essence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.user.User;
import org.bson.Document;
import org.bukkit.entity.Player;

@Getter
@AllArgsConstructor
public class EssenceCost extends Cost {
    private final EssenceType essenceType;
    private final int amount;

    private Document essence(User user) {
        return user.getDocument().get("dungeons", Document.class).get("essence", Document.class);
    }

    @Override
    public boolean canPay(Player player, User user) {
        return essence(user).getInteger(essenceType.name().toLowerCase()) >= amount;
    }

    @Override
    public void pay(Player player, User user) {
        Document essence = essence(user);
        String key = essenceType.name().toLowerCase();
        essence.append(key, essence.getInteger(key) - amount);
    }
}
