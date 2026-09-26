package net.icxd.dungeons.proxy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import net.icxd.dungeons.common.Runs;
import net.icxd.dungeons.proxy.dungeon.DungeonQueue;
import net.icxd.dungeons.proxy.dungeon.JoinInstanceCommand;
import net.icxd.dungeons.proxy.party.PartyCommand;
import net.icxd.dungeons.proxy.party.PartyManager;

/**
 * The Velocity side of the network: moving players between servers without losing their data,
 * parties, and sending parties to dungeon servers. Settings are in {@code config.properties}; the
 * server manager's remote console in {@code remote-console.properties}.
 */
@Plugin(id = "skyblock", name = "SkyBlock", version = "1.0-SNAPSHOT", authors = {"icxd"})
public final class SkyBlockProxy {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private RemoteConsole console;
    private MongoClient mongo;

    @Inject
    public SkyBlockProxy(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            console = RemoteConsole.load(proxy, logger, dataDirectory);
            if (console != null) console.start();
        } catch (IOException | RuntimeException e) {
            logger.error("Couldn't start the remote console", e);
        }

        ProxyConfig config;
        try {
            config = ProxyConfig.load(dataDirectory);
        } catch (IOException | RuntimeException e) {
            logger.error("Couldn't read {}; the SkyBlock features are off", ProxyConfig.FILE, e);
            return;
        }
        MongoDatabase database = null;
        try {
            mongo = MongoClients.create(MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(config.mongoUri()))
                    // Lookups give up quickly while it's down, instead of holding up logins.
                    .applyToClusterSettings(b -> b.serverSelectionTimeout(3, TimeUnit.SECONDS))
                    .build());
            database = mongo.getDatabase(config.mongoDatabase());
        } catch (RuntimeException e) {
            logger.error("Bad mongodb.uri in {}: server types, ranks and dungeons are off", ProxyConfig.FILE, e);
        }
        MongoCollection<Document> users = database == null ? null : database.getCollection("users");
        MongoCollection<Document> servers = database == null ? null : database.getCollection("servers");
        MongoCollection<Document> runs = database == null ? null : database.getCollection(Runs.COLLECTION);

        ServerDirectory directory = new ServerDirectory(proxy, logger, servers, runs);
        proxy.getScheduler().buildTask(this, directory::refresh).repeat(2, TimeUnit.SECONDS).schedule();
        Profiles profiles = new Profiles(proxy, logger, users);
        Transfers[] transfers = new Transfers[1];
        Messenger messenger = new Messenger(proxy, (from, destination) -> transfers[0].sendRequested(from, destination));
        transfers[0] = new Transfers(proxy, logger, messenger, directory, runs);
        PartyManager parties = new PartyManager(this, proxy, profiles, transfers[0], config.maxPartySize());
        DungeonQueue queue = new DungeonQueue(this, proxy, logger, directory, transfers[0], parties, profiles, runs, config.runsPerDungeonServer());

        for (Object listener : new Object[] {messenger, transfers[0], profiles, parties}) {
            proxy.getEventManager().register(this, listener);
        }
        register("hub", new HubCommand(transfers[0], directory), "lobby", "l");
        register("party", new PartyCommand(parties, proxy), "p");
        register("pc", new PartyCommand.ChatCommand(parties), "pchat");
        register("pl", new PartyCommand.ListCommand(parties));
        register("joininstance", new JoinInstanceCommand(queue), "joindungeon");
        logger.info("SkyBlock proxy plugin enabled with {} backend servers", proxy.getAllServers().size());
    }

    private void register(String name, SimpleCommand command, String... aliases) {
        CommandManager commands = proxy.getCommandManager();
        commands.register(commands.metaBuilder(name).aliases(aliases).plugin(this).build(), command);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (console != null) console.stop();
        if (mongo != null) mongo.close();
    }
}
