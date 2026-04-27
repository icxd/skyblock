package net.icxd.dungeons.bank;

import lombok.Getter;
import lombok.Setter;
import net.icxd.dungeons.database.utility.MongoSerializable;
import org.bson.Document;

import java.util.ArrayList;

@Getter
@Setter
public class Bank implements MongoSerializable {
    private double balance;
    private ArrayList<BankTransaction> transactions;

    public Bank(double balance, ArrayList<BankTransaction> transactions) {
        this.balance = balance;
        this.transactions = transactions;
    }

    public Bank(Document document) {
        this.balance = document.getDouble("balance");
        this.transactions = new ArrayList<>();
        for (Document transaction : document.getList("transactions", Document.class))
            this.transactions.add(new BankTransaction(transaction));
    }

    @Override
    public Document toDocument() {
        return new Document()
                .append("balance", balance)
                .append("transactions", transactions);
    }

    @Override
    public MongoSerializable fromDocument(Document document) {
        return new Bank(document);
    }
}
