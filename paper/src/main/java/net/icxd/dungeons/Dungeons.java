package net.icxd.dungeons;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.icxd.dungeons.anticheat.check.CheckListener;
import net.icxd.dungeons.command.commands.admin.AddEnchantmentCommand;
import net.icxd.dungeons.command.commands.admin.DataCommand;
import net.icxd.dungeons.command.commands.admin.DungeonCommand;
import net.icxd.dungeons.command.commands.admin.ItemCommand;
import net.icxd.dungeons.command.commands.admin.NBTCommand;
import net.icxd.dungeons.command.commands.admin.PlayerDataCommand;
import net.icxd.dungeons.command.commands.admin.RecombobulateCommand;
import net.icxd.dungeons.command.commands.admin.SpawnEntityCommand;
import net.icxd.dungeons.command.commands.admin.SpawnRewardChestCommand;
import net.icxd.dungeons.command.commands.admin.UnlockCommand;
import net.icxd.dungeons.command.commands.admin.UpgradeCommand;
import net.icxd.dungeons.command.commands.user.HotmCommand;
import net.icxd.dungeons.command.commands.user.ShowExtraStatsCommand;
import net.icxd.dungeons.command.commands.user.ToggleReadyUpCommand;
import net.icxd.dungeons.gui.GUIListener;
import net.icxd.dungeons.listeners.CombatListener;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.listeners.PlayerListener;
import net.icxd.dungeons.listeners.WorldListener;
import net.icxd.dungeons.mining.BlockListener;
import net.icxd.dungeons.region.MovementListener;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.database.ICollection;
import net.icxd.dungeons.database.collections.UserCollection;
import net.icxd.dungeons.database.mongo.Settings;
import net.icxd.dungeons.common.Runs;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.network.ProxyLink;
import net.icxd.dungeons.rune.RuneRunnable;
import net.icxd.dungeons.scoreboard.ScoreboardRunnable;
import net.icxd.dungeons.stats.StatsRunnable;
import net.icxd.dungeons.tablist.TabList;
import net.icxd.dungeons.user.UserStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;


public class Dungeons extends JavaPlugin {
    @Getter private static Dungeons instance;
    @Getter private static MongoClient mongoClient;
    @Getter private static ICollection userCollection;
    @Getter private static UserStore userStore;
    @Getter private static ProxyLink proxyLink;
    /** Dungeon runs, on DUNGEONS servers; null elsewhere. */
    @Getter private static RunManager runManager;

    @Getter
    private static SkyBlockServer skyBlockServer;

    @Override
    public void onEnable() {
        instance = this;

        mongoClient = MongoClients.create(Settings.URI);
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

        getLogger().info(ItemRegistry.getRegistry().size() + " SkyBlock items");

        // Each on its own, so one that fails doesn't take the rest with it.
        listen(PlayerListener.class, PlayerListener::new);
        listen(CombatListener.class, CombatListener::new);
        listen(InventorySyncListener.class, InventorySyncListener::new);
        listen(WorldListener.class, WorldListener::new);
        listen(GUIListener.class, GUIListener::new);
        listen(BlockListener.class, BlockListener::new);
        listen(MovementListener.class, MovementListener::new);
        listen(CheckListener.class, CheckListener::new);
        listen(Mobs.class, Mobs::new);

        List<SCommand> commands = List.of(new AddEnchantmentCommand(), new DataCommand(), new DungeonCommand(), new ItemCommand(),
                new NBTCommand(), new PlayerDataCommand(), new RecombobulateCommand(), new SpawnEntityCommand(),
                new SpawnRewardChestCommand(), new UnlockCommand(), new UpgradeCommand(), new HotmCommand(),
                new ShowExtraStatsCommand(), new ToggleReadyUpCommand());
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            for (SCommand command : commands) {
                try {
                    command.register(event.registrar());
                } catch (RuntimeException e) {
                    getLogger().log(java.util.logging.Level.SEVERE, "Couldn't register " + command.getClass().getSimpleName(), e);
                }
            }
        });

        Bukkit.getScheduler().runTaskTimer(this, new StatsRunnable(), 0, 20);
        Bukkit.getScheduler().runTaskTimer(this, new ScoreboardRunnable(), 0, 20);
        Mobs.start();
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

    /** Registers a listener, if it runs on this kind of server (see {@link OnlyOn}). */
    private <T extends Listener> void listen(Class<T> type, java.util.function.Supplier<T> listener) {
        if (!skyBlockServer.runs(type)) return;
        try {
            getServer().getPluginManager().registerEvents(listener.get(), this);
        } catch (RuntimeException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Couldn't register " + type.getSimpleName(), e);
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
