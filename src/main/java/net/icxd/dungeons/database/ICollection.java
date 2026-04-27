package net.icxd.dungeons.database;

import com.mongodb.client.MongoCollection;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.database.mongo.Settings;
import org.bson.Document;

import java.util.ArrayList;

public interface ICollection {
    String name();
    Document defaultDocument();

    default MongoCollection<Document> get() {
        if (!Dungeons.getMongoClient().getDatabase(Settings.DATABASE).listCollectionNames().into(new ArrayList<>()).contains(name()))
            Dungeons.getMongoClient().getDatabase(Settings.DATABASE).createCollection(name());
        return Dungeons.getMongoClient().getDatabase(Settings.DATABASE).getCollection(name());
    }
}
