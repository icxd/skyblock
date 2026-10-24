package net.icxd.dungeons.island;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Sorts.descending;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

/**
 * Islands in MongoDB, so any island server can load any of them: GridFS's {@code islands} bucket,
 * one file per island (named by its id, the profile's) holding its zipped region files (see
 * {@link IslandFiles}). A save uploads a new file and then drops the older ones, so a save that
 * fails halfway leaves the last one whole. Blocking: off the main thread.
 */
final class IslandStore {
    static final String BUCKET = "islands";

    private final GridFSBucket bucket;

    IslandStore(MongoDatabase database) {
        this.bucket = GridFSBuckets.create(database, BUCKET);
    }

    /** The island's last save; null if it has never been saved. */
    byte[] load(String id) throws IOException {
        GridFSFile file = bucket.find(eq("filename", id)).sort(descending("uploadDate")).first();
        if (file == null) return null;
        if (file.getLength() > IslandFiles.MAX_BYTES) throw new IOException("island " + id + " is " + file.getLength() + " bytes");
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) file.getLength());
        bucket.downloadToStream(file.getObjectId(), out);
        return out.toByteArray();
    }

    /** Saves the island as it is now, by the server that has it loaded. */
    void save(String id, byte[] zip, String server) {
        GridFSUploadOptions options = new GridFSUploadOptions().metadata(new Document("server", server).append("savedAt", new Date()));
        ObjectId saved = bucket.uploadFromStream(id, new ByteArrayInputStream(zip), options);
        for (GridFSFile older : bucket.find(and(eq("filename", id), ne("_id", saved)))) bucket.delete(older.getObjectId());
    }
}
