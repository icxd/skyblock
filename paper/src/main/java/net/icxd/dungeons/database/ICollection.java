package net.icxd.dungeons.database;

import com.mongodb.client.MongoCollection;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.database.mongo.Settings;
import org.bson.Document;

public interface ICollection {
    String name();
    Document defaultDocument();

    /** Mongo creates the collection on the first write. */
    default MongoCollection<Document> get() {
        return Dungeons.getMongoClient().getDatabase(Settings.DATABASE).getCollection(name());
    }
}
