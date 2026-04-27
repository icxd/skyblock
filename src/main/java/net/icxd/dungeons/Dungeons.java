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
import net.icxd.dungeons.entity.EntityRegistry;
import net.icxd.dungeons.entity.EntityRunnable;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.rune.RuneRunnable;
import net.icxd.dungeons.scoreboard.ScoreboardRunnable;
import net.icxd.dungeons.stats.StatsRunnable;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;

import java.lang.reflect.Field;

public class Dungeons extends JavaPlugin {
    @Getter private static Dungeons instance;
    @Getter private static MongoClient mongoClient;
    @Getter private static ICollection userCollection;

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

        new CheckHandler();
        new ItemRegistry();
        // new GUIRegistry();
        new EntityRegistry();

        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            this.commandMap = (CommandMap) f.get(Bukkit.getServer());
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        cl = new CommandLoader();

        try {
            for (Class<?> listener : new Reflections().getSubTypesOf(Listener.class)) {
                getServer().getPluginManager().registerEvents((Listener) listener.newInstance(), this);
            }
            for (Class<?> command : new Reflections().getSubTypesOf(SCommand.class)) {
                cl.register((SCommand) command.newInstance());
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        Bukkit.getScheduler().runTaskTimer(this, new StatsRunnable(), 0, 20);
        Bukkit.getScheduler().runTaskTimer(this, new ScoreboardRunnable(), 0, 20);
        Bukkit.getScheduler().runTaskTimer(this, new EntityRunnable(), 0, 1);
        Bukkit.getScheduler().runTaskTimer(this, new RuneRunnable(), 0, 1);

        for (Player player : Bukkit.getOnlinePlayers()) {
            long cms = System.currentTimeMillis();

            Utils.async(() -> {
                User user = User.getUser(player.getUniqueId());
                user.load();

                player.sendMessage(Utils.color("&aSuccessfully loaded player data. &8(took " + (System.currentTimeMillis() - cms) + "ms)"));
            });
        }
    }

    @Override
    public void onDisable() {
        instance = null;
    }
}
