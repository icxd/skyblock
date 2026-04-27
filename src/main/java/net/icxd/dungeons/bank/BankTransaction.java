package net.icxd.dungeons.bank;

import lombok.Getter;
import net.icxd.dungeons.database.utility.MongoSerializable;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Getter
public class BankTransaction implements MongoSerializable {
    private final Player player;
    private final long timestamp;
    private final double amount;
    private final TransactionType type;

    public BankTransaction(Player player, double amount, TransactionType type) {
        this.player = player;
        this.timestamp = System.currentTimeMillis();
        this.amount = amount;
        this.type = type;
    }

    public BankTransaction(Document document) {
        this.player = Bukkit.getPlayer(document.getString("player"));
        this.timestamp = document.getLong("timestamp");
        this.amount = document.getDouble("amount");
        this.type = TransactionType.valueOf(document.getString("type"));
    }

    @Override
    public Document toDocument() {
        return new Document()
                .append("player", player.getUniqueId().toString())
                .append("timestamp", timestamp)
                .append("amount", amount)
                .append("type", type.name());
    }

    @Override
    public MongoSerializable fromDocument(Document document) {
        return new BankTransaction(document);
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAW
    }
}
