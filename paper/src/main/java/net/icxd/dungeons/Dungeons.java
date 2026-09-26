package net.icxd.dungeons;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import lombok.Getter;
import net.icxd.dungeons.anticheat.check.CheckHandler;
import net.icxd.dungeons.command.CommandLoader;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.database.ICollection;
import net.icxd.dungeons.database.collections.UserCollection;
import net.icxd.dungeons.database.mongo.Settings;
import net.icxd.dungeons.common.Runs;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.entity.EntityRegistry;
import net.icxd.dungeons.entity.EntityRunnable;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.network.ProxyLink;
import net.icxd.dungeons.rune.RuneRunnable;
import net.icxd.dungeons.scoreboard.ScoreboardRunnable;
import net.icxd.dungeons.stats.StatsRunnable;
import net.icxd.dungeons.tablist.TabList;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.user.UserStore;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;


public class Dungeons extends JavaPlugin {
    @Getter private static Dungeons instance;
    @Getter private static MongoClient mongoClient;
    @Getter private static ICollection userCollection;
    @Getter private static UserStore userStore;
    @Getter private static ProxyLink proxyLink;
    /** Dungeon runs, on DUNGEONS servers; null elsewhere. */
    @Getter private static RunManager runManager;

    @Getter
    public CommandMap commandMap;
    public CommandLoader cl;

    @Getter
    private static SkyBlockServer skyBlockServer;

    @Override
    public void onEnable() {
        instance = this;

        mongoClient = new MongoClient(new MongoClientURI(Settings.URI));
        userCollection = new UserCollection();

        getConfig().options().copyDefaults(true);
        saveConfig();

        skyBlockServer = new SkyBlockServer(getConfig());
        userStore = new UserStore(this, skyBlockServer.getName(), skyBlockServer.getServerType().name(), userCollection.get(),
                mongoClient.getDatabase(Settings.DATABASE).getCollection("servers"), userCollection::defaultDocument);
        userStore.start();
        proxyLink = new ProxyLink(this, userStore);
        if (skyBlockServer.getServerType() == ServerType.DUNGEONS) {
            runManager = new RunManager(this, skyBlockServer.getName(),
                    mongoClient.getDatabase(Settings.DATABASE).getCollection(Runs.COLLECTION), proxyLink);
            runManager.start();
        }

        new CheckHandler();
        new ItemRegistry();
        // new GUIRegistry();
        new EntityRegistry();

        this.commandMap = Bukkit.getCommandMap();

        cl = new CommandLoader();

        try {
            for (Class<?> listener : Utils.instantiableSubTypesOf(Listener.class)) {
                if (!skyBlockServer.runs(listener)) continue;
                getServer().getPluginManager().registerEvents((Listener) listener.newInstance(), this);
            }
            for (Class<?> command : Utils.instantiableSubTypesOf(SCommand.class)) {
                cl.register((SCommand) command.newInstance());
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        Bukkit.getScheduler().runTaskTimer(this, new StatsRunnable(), 0, 20);
        Bukkit.getScheduler().runTaskTimer(this, new ScoreboardRunnable(), 0, 20);
        Bukkit.getScheduler().runTaskTimer(this, new EntityRunnable(), 0, 1);
        Bukkit.getScheduler().runTaskTimer(this, new RuneRunnable(), 0, 1);
        if (getServer().getPluginManager().isPluginEnabled("packetevents")) {
            TabList.handle();
        } else {
            getLogger().warning("PacketEvents isn't installed, so there's no tab list");
        }

        // Players already online when the plugin (re)loads.
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                userStore.claimOnline(player);
            } catch (UserStore.HeldElsewhereException | InterruptedException | RuntimeException e) {
                getLogger().log(java.util.logging.Level.SEVERE, "Couldn't load " + player.getName() + "'s data", e);
                player.kick(net.kyori.adventure.text.Component.text("Couldn't load your profile, please rejoin."));
            }
        }
    }

    @Override
    public void onDisable() {
        if (runManager != null) runManager.stop();
        runManager = null;
        if (userStore != null) userStore.stop();
        if (mongoClient != null) mongoClient.close();
        instance = null;
    }
}
