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
        Document dungeons = user.profile().get("dungeons", Document.class);
        Document essence = dungeons.get("essence", Document.class);
        if (essence == null) {
            essence = new Document();
            dungeons.put("essence", essence);
        }
        return essence;
    }

    // An essence added since the player's data was made (Forest) isn't in it yet: they have none of it.
    @Override
    public boolean canPay(Player player, User user) {
        return essence(user).getInteger(essenceType.name().toLowerCase(), 0) >= amount;
    }

    @Override
    public void pay(Player player, User user) {
        Document essence = essence(user);
        String key = essenceType.name().toLowerCase();
        essence.append(key, essence.getInteger(key, 0) - amount);
    }
}
