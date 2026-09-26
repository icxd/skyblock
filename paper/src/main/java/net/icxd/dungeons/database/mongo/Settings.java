package net.icxd.dungeons.database.mongo;

import net.icxd.dungeons.Dungeons;

public class Settings {
    public static final String URI = Dungeons.getInstance().getConfig().getString("mongodb.uri");
    public static final String DATABASE = Dungeons.getInstance().getConfig().getString("mongodb.database");
}
