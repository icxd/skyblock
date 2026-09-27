package net.icxd.dungeons.command.commands.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.profile.Profiles;

class PlayerDataCommandTest {
    /** A profile's fields are the selected profile's, however they're written; the account's stay where they are. */
    @Test
    void pathsIntoTheSelectedProfile() {
        Document doc = new Document("rank", "STAFF").append("gems", 3);
        Profiles.create(doc, "abc", "Kiwi", ProfileMode.SANDBOX, new Document("coins", 5), 0);
        doc.put(Profiles.SELECTED, "abc");

        assertEquals("profiles.abc.coins", PlayerDataCommand.resolve(doc, "coins"));
        assertEquals("profiles.abc.dungeons.floors.highest", PlayerDataCommand.resolve(doc, "dungeons.floors.highest"));
        assertEquals("profiles.abc.coins", PlayerDataCommand.resolve(doc, "profile.coins"));
        assertEquals("profiles.abc", PlayerDataCommand.resolve(doc, "profile"));
        assertEquals("profiles.abc.coins", PlayerDataCommand.resolve(doc, "profiles.abc.coins"));
        assertEquals("rank", PlayerDataCommand.resolve(doc, "rank"));
        assertEquals("settings.autoReadyUp", PlayerDataCommand.resolve(doc, "settings.autoReadyUp"));
        assertEquals("", PlayerDataCommand.resolve(doc, ""));
        // One from before profiles still has them at the top.
        assertEquals("coins", PlayerDataCommand.resolve(new Document("coins", 1), "coins"));
    }
}
