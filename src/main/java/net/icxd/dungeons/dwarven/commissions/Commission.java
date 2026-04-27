package net.icxd.dungeons.dwarven.commissions;

import net.icxd.dungeons.database.utility.MongoSerializable;
import org.bson.Document;

public enum Commission implements MongoSerializable {
    ;

    @Override
    public Document toDocument() {
        return null;
    }

    @Override
    public MongoSerializable fromDocument(Document document) {
        return null;
    }
}
