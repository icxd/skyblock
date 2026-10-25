package net.icxd.dungeons.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.MobKinds;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.mob.SpawnOptions;
import net.icxd.dungeons.quest.QuestProgress;
import net.icxd.dungeons.quest.Quests;
import net.icxd.dungeons.utils.SkyBlockTime;

/**
 * The Hub's Graveyard, south-west of the Village: its Graveyard Zombies, and at night (8pm to 6am,
 * the wiki's) its Zombie Villagers, which Time to Strike has killed. As recorded (2026-10-25): they
 * appear at fixed spots (the ones below, where each recorded one was first seen), up to about 20
 * zombies and 10 villagers, one more every 2 seconds or so while there are fewer; a villager left at
 * 6am stays till it's killed (three were still there after it), and none come by day. Each kill of
 * either counts for "Kill Zombies".
 *
 * <p>Not recorded, so this plugin's: they appear only where the map is loaded (someone is about), so
 * an empty Hub has none, and a spot is picked at random. Main thread.
 */
@OnlyOn({ServerType.LOBBY})
public final class Graveyard implements Listener {
    static final double[][] ZOMBIE_SPOTS = {
            {-157.5, 73, -114.5}, {-154.5, 74, -118.5}, {-153.5, 75, -110.5}, {-151.5, 72, -116.5}, {-130.5, 75, -133.5},
            {-128.5, 76, -136.5}, {-128.5, 76, -134.5}, {-128.5, 77, -130.5}, {-127.5, 75, -135.5}, {-126.5, 75, -135.5},
            {-125.5, 76, -137.5}, {-124.5, 76, -138.5}, {-123.5, 74, -145.5}, {-123.5, 76, -135.5}, {-123.5, 76, -132.5},
            {-123.5, 76, -130.5}, {-122.5, 73, -147.5}, {-122.5, 74, -139.5}, {-122.5, 75, -164.5}, {-122.5, 75, -139.5},
            {-122.5, 75, -138.5}, {-122.5, 75, -134.5}, {-121.5, 74, -138.5}, {-119.5, 74, -174.5}, {-119.5, 74, -173.5},
            {-119.5, 74, -149.5}, {-119.5, 74, -145.5}, {-117.5, 73, -142.5}, {-115.5, 72, -108.5}, {-115.5, 73, -144.5},
            {-115.5, 74, -116.5}, {-114.5, 72, -109.5}, {-114.5, 74, -143.5}, {-114.5, 74, -141.5}, {-113.5, 72, -107.5},
            {-113.5, 73, -144.5}, {-113.5, 73, -117.5}, {-113.5, 73, -115.5}, {-113.5, 74, -117.5}, {-113.5, 75, -140.5},
            {-112.5, 72, -111.5}, {-111.5, 73, -107.5}, {-110.5, 72, -113.5}, {-110.5, 73, -114.5}, {-110.5, 74, -115.5},
            {-109.5, 72, -108.5}, {-109.5, 73, -116.5}, {-108.5, 73, -112.5}, {-108.5, 73, -111.5}, {-107.5, 72, -111.5},
            {-107.5, 73, -117.5}, {-107.5, 74, -117.5}, {-105.5, 72, -116.5}, {-100.5, 75, -167.5}, {-98.5, 76, -170.5},
            {-93.5, 75, -168.5}, {-85.5, 76, -152.5}, {-84.5, 77, -152.5}, {-81.5, 77, -147.5}};
    static final double[][] VILLAGER_SPOTS = {
            {-152.5, 74, -133.5}, {-151.5, 73, -136.5}, {-150.5, 73, -126.5}, {-136.5, 73, -158.5}, {-133.5, 74, -152.5},
            {-131.5, 73, -159.5}, {-107.5, 73, -130.5}, {-106.5, 74, -135.5}, {-105.5, 73, -138.5}, {-103.5, 73, -132.5},
            {-95.5, 75, -167.5}, {-88.5, 75, -165.5}};
    static final int ZOMBIES = 20;
    static final int VILLAGERS = 10;
    static final long SPAWN_TICKS = 40;

    private final List<Mobs.Live> zombies = new ArrayList<>();
    private final List<Mobs.Live> villagers = new ArrayList<>();

    /** Its mobs from now on, if this is the Hub's map. */
    public Graveyard(Plugin plugin) {
        if (HubMap.here()) Bukkit.getScheduler().runTaskTimer(plugin, this::spawn, SPAWN_TICKS, SPAWN_TICKS);
    }

    /** Whether Zombie Villagers come at this time: 8pm to 6am. */
    static boolean night(SkyBlockTime time) {
        return time.hour() >= 20 || time.hour() < 6;
    }

    private void spawn() {
        World world = Dungeons.getSkyBlockServer().getMainWorld();
        zombies.removeIf(live -> !live.entity().isValid());
        villagers.removeIf(live -> !live.entity().isValid());
        if (zombies.size() < ZOMBIES) spawn(world, MobKinds.GRAVEYARD_ZOMBIE, ZOMBIE_SPOTS, zombies);
        if (villagers.size() < VILLAGERS && night(SkyBlockTime.now())) spawn(world, MobKinds.ZOMBIE_VILLAGER, VILLAGER_SPOTS, villagers);
    }

    /** One more at a spot of these that's loaded, if any is. */
    private static void spawn(World world, MobKind kind, double[][] spots, List<Mobs.Live> alive) {
        List<double[]> loaded = new ArrayList<>();
        for (double[] spot : spots) {
            if (world.isChunkLoaded((int) Math.floor(spot[0]) >> 4, (int) Math.floor(spot[2]) >> 4)) loaded.add(spot);
        }
        if (loaded.isEmpty()) return;
        double[] spot = loaded.get(ThreadLocalRandom.current().nextInt(loaded.size()));
        Location at = new Location(world, spot[0], spot[1], spot[2], ThreadLocalRandom.current().nextFloat() * 360 - 180, 0);
        alive.add(Mobs.spawn(kind, null, SpawnOptions.NONE, at));
    }

    /** After the kill's Combat XP (a 10th kill's level up came before the objective, as recorded). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(SkyBlockMobDeathEvent event) {
        Player killer = event.killer();
        if (killer != null && counts(event.kind())) QuestProgress.count(killer, Quests.TIME_TO_STRIKE, Quests.KILL_ZOMBIES, 1);
    }

    /** "Kill Zombies" counts both kinds. */
    static boolean counts(MobKind kind) {
        return kind == MobKinds.GRAVEYARD_ZOMBIE || kind == MobKinds.ZOMBIE_VILLAGER;
    }
}
