package net.icxd.dungeons.database.utility;

import org.bson.Document;

public interface MongoSerializable {
    Document toDocument();
    MongoSerializable fromDocument(Document document);
}
