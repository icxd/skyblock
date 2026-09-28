package net.icxd.dungeons.leveling;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.leveling.LevelingData.Category;
import net.icxd.dungeons.leveling.LevelingData.Emblem;
import net.icxd.dungeons.leveling.LevelingData.Task;
import net.icxd.dungeons.profile.Profiles;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.StatsRunnable;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Text;

/**
 * SkyBlock Leveling on this server: the data (read off the main thread when the plugin starts), each player's
 * SkyBlock XP on the profile they play on, and what it shows and gives. The XP is worked out from the profile (see
 * {@link SkyBlockXp}) and kept per player and profile for their stats, the tab list and chat; it's worked out again
 * two seconds after something that gives XP ({@link #changed}), every ten seconds anyway, and on a profile switch.
 * A new level gives its stats (+5 Health a level, +1 Strength every fifth, through {@link PlayerStats#addModifier})
 * and the level-up message, once: the profile keeps the last level it was told of ({@code leveling.level}; a profile
 * that has none is told nothing the first time, so existing progress isn't announced). Main thread, but for what
 * chat reads.
 */
public final class SkyBlockLevels implements Listener {
    /**
     * XP is worked out this long after what gave it: Hypixel's level-up message comes 2 seconds after (real chat
     * logs: a skill's level-up at 23:18:59, the SkyBlock level's at 23:19:01; a Slayer reward at 12:30:27, 12:30:29).
     */
    static final long CHECK_TICKS = 40;
    /** And every ten seconds for everyone, for changes nothing says (UNKNOWN on Hypixel). */
    static final long SWEEP_TICKS = 200;
    /** How long the action bar shows the XP: as long as a skill's (research skills.md 2.1); UNKNOWN for SkyBlock XP. */
    static final long SHOWN_MILLIS = 2_000;
    /** The profile's last level they were told of. */
    static final String TOLD = "level";
    /** The account's "SkyBlock Levels in Chat", on unless it's off. */
    static final String LEVELS_IN_CHAT = "levelsInChat";

    private static Logger log = Logger.getLogger(SkyBlockLevels.class.getName());
    private static volatile LevelingData data = LevelingData.empty();
    private static volatile LevelingSources sources = new LevelingSources() {
    };
    /** Written on the main thread; chat reads it. */
    private static final Map<UUID, Snapshot> snapshots = new ConcurrentHashMap<>();
    /** Who has SkyBlock Levels in Chat off, for chat. */
    private static final Set<UUID> levelsHidden = ConcurrentHashMap.newKeySet();
    /** Who has a check coming. */
    private static final Set<UUID> pending = new HashSet<>();

    /** What's known of a player's SkyBlock XP on a profile: where it comes from, and the emblem they show ("&7♦"; null for none). */
    record Snapshot(String profileId, SkyBlockXp.Breakdown xp, String emblem) {
        int level() {
            return xp.level();
        }
    }

    /** Reads the data off the main thread, and gives the levels' stats and the checks their timer. Once, when the plugin starts. */
    public static void start(JavaPlugin plugin) {
        log = plugin.getLogger();
        Path folder = plugin.getDataFolder().toPath();
        CompletableFuture.supplyAsync(() -> LevelingData.load(folder)).thenAccept(loaded -> Bukkit.getScheduler().runTask(plugin, () -> {
            loaded.problems().forEach(p -> log.warning("Leveling: " + p));
            data = loaded;
            log.info("Leveling: " + loaded.categories().size() + " categories of tasks, " + loaded.rewards().size() + " rewards, "
                    + loaded.emblemCategories().stream().mapToInt(c -> c.emblems().size()).sum() + " emblems");
            // Everyone here is worked out again with the data (quietly: nothing they had is new).
            snapshots.clear();
            for (Player player : Bukkit.getOnlinePlayers()) check(player);
        }));
        PlayerStats.addModifier((player, stats) -> LevelRewards.add(stats, level(player)));
        Bukkit.getScheduler().runTaskTimer(plugin, SkyBlockLevels::sweep, SWEEP_TICKS, SWEEP_TICKS);
    }

    public static LevelingData data() {
        return data;
    }

    public static LevelingSources sources() {
        return sources;
    }

    /** What else counts (collections): once, when the server starts. */
    public static void sources(LevelingSources counted) {
        sources = Objects.requireNonNull(counted);
        snapshots.clear();
    }

    // Levels

    /** Their SkyBlock XP on the profile they play on now, by task (worked out afresh, for menus). */
    public static SkyBlockXp.Breakdown breakdown(User user) {
        return SkyBlockXp.of(user.profile(), data, sources);
    }

    /** Their SkyBlock XP on the profile they play on; 0 while their data isn't here. */
    public static int xp(Player player) {
        return xp(User.ifLoaded(player.getUniqueId()));
    }

    /** The SkyBlock XP of the profile they play on; 0 for no one. What {@link User#getSkyBlockXp} reads. */
    public static int xp(User user) {
        Snapshot snapshot = snapshot(user);
        return snapshot == null ? 0 : snapshot.xp().total();
    }

    /** Their SkyBlock level on the profile they play on; 0 while their data isn't here. */
    public static int level(Player player) {
        Snapshot snapshot = snapshot(User.ifLoaded(player.getUniqueId()));
        return snapshot == null ? 0 : snapshot.level();
    }

    /**
     * What's known of them, worked out if nothing is or it was another profile's (quietly); kept only while
     * they're here (someone whose data this server holds for a moment, between servers, isn't kept).
     */
    private static Snapshot snapshot(User user) {
        if (user == null || !user.isLoaded()) return null;
        Snapshot snapshot = snapshots.get(user.getUuid());
        if (snapshot == null || !Objects.equals(snapshot.profileId(), user.profileId())) {
            snapshot = compute(user);
            if (Bukkit.getPlayer(user.getUuid()) != null) snapshots.put(user.getUuid(), snapshot);
        }
        return snapshot;
    }

    private static Snapshot compute(User user) {
        Document profile = user.profile();
        SkyBlockXp.Breakdown xp = SkyBlockXp.of(profile, data, sources);
        Emblem emblem = Emblems.shown(data, profile, xp.level(), sources);
        return new Snapshot(user.profileId(), xp, emblem == null ? null : emblem.symbol());
    }

    // Changes

    /**
     * Something SkyBlock XP counts changed for them (a skill's level, a run's rewards): it's worked out again
     * {@link #CHECK_TICKS} later, with what it shows.
     */
    public static void changed(Player player) {
        if (!pending.add(player.getUniqueId())) return;
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
            pending.remove(player.getUniqueId());
            if (player.isOnline()) check(player);
        }, CHECK_TICKS);
    }

    private static void sweep() {
        for (Player player : Bukkit.getOnlinePlayers()) if (!pending.contains(player.getUniqueId())) check(player);
    }

    /**
     * Works out their XP again: the action bar shows what a task gave since the last time (the most, if several
     * did: UNKNOWN how Hypixel shows two at once), and a level they haven't been told of gets its message.
     */
    static void check(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null) return;
        Snapshot before = snapshots.get(player.getUniqueId());
        Snapshot now = compute(user);
        snapshots.put(player.getUniqueId(), now);
        if (before != null && Objects.equals(before.profileId(), now.profileId())) showGain(player, before.xp(), now.xp());
        if (before == null || before.level() != now.level()) PlayerSession.of(player).invalidateStats();
        if (!user.isReleased()) announce(player, user, now.level());
    }

    /** The action bar's "+20 SkyBlock XP (Skill Level Up) (34/100)" for the task that gave the most since before. */
    private static void showGain(Player player, SkyBlockXp.Breakdown before, SkyBlockXp.Breakdown now) {
        Task best = null;
        int most = 0;
        for (Category category : data.categories()) {
            for (Task task : category.tasks()) {
                int gained = now.of(task.id()) - before.of(task.id());
                if (gained > most) {
                    most = gained;
                    best = task;
                }
            }
        }
        if (best == null) return;
        PlayerSession session = PlayerSession.of(player);
        Replacement shown = Replacement.forMillis(LevelingText.actionBar(most, best.name(), now.total()), SHOWN_MILLIS);
        session.setDefenseReplacement(shown);
        StatsRunnable.sendActionBar(player);
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
            if (!player.isOnline() || session.getDefenseReplacement() != shown) return;
            session.setDefenseReplacement(null);
            StatsRunnable.sendActionBar(player);
        }, SHOWN_MILLIS / 50);
    }

    /** The level-up message for levels past the last one the profile was told of (a first time tells nothing). */
    private static void announce(Player player, User user, int level) {
        // Without the data, everyone is at 0: nothing to tell, and nothing to remember.
        if (data.categories().isEmpty()) return;
        Document part = Emblems.part(user.profile());
        if (!(part.get(TOLD) instanceof Number told)) {
            part.put(TOLD, level);
            return;
        }
        int from = told.intValue();
        if (level <= from) return;
        for (String line : LevelingText.levelUp(from, level, LevelRewards.gained(data, from, level))) player.sendMessage(Text.line(line));
        // UNKNOWN on Hypixel: a skill's level-up sound.
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        part.put(TOLD, level);
        user.save();
    }

    // Chat and the tab list

    /** "&8[&e88&8] " and their emblem ("&7♦ ") before their name in chat; nothing while it isn't known. Any thread. */
    public static String chatPrefix(UUID player) {
        Snapshot snapshot = snapshots.get(player);
        if (snapshot == null) return "";
        return SkyBlockXp.bracket(snapshot.level()) + " " + (snapshot.emblem() == null ? "" : snapshot.emblem() + " ");
    }

    /** Whether they see SkyBlock levels and emblems in chat ("SkyBlock Levels in Chat"). Any thread. */
    public static boolean levelsInChat(UUID viewer) {
        return !levelsHidden.contains(viewer);
    }

    /** "&e88": their level in its colour, as the tab lists put it in brackets; "&70" while it isn't known. */
    public static String levelText(UUID player) {
        Snapshot snapshot = snapshots.get(player);
        int level = snapshot == null ? 0 : snapshot.level();
        return SkyBlockXp.color(level) + level;
    }

    /** " &7♦" after their name in the tab list; "" for none. */
    public static String emblemSuffix(UUID player) {
        Snapshot snapshot = snapshots.get(player);
        return snapshot == null || snapshot.emblem() == null ? "" : " " + snapshot.emblem();
    }

    /**
     * "&8[&e88&8] &bICoding": a player in the tab list's Players column (recorded on a private island, the SkyBlock
     * Menu tour), with their emblem after the name (" &7♦": UNKNOWN, none was chosen there).
     */
    public static String tabName(Player player, Rank rank) {
        snapshot(User.ifLoaded(player.getUniqueId()));
        return "&8[" + levelText(player.getUniqueId()) + "&8] " + rank.getColor() + player.getName() + emblemSuffix(player.getUniqueId());
    }

    /** The Info column's " SB Level: [88] 34/100 XP" (recorded under Profile, in every tab list). */
    public static String tabLine(Player viewer) {
        int xp = xp(viewer);
        return "&f SB Level&f: " + SkyBlockXp.bracket(SkyBlockXp.level(xp)) + " &b" + SkyBlockXp.intoLevel(xp) + "&3/&b" + SkyBlockXp.PER_LEVEL + " XP";
    }

    /** Turns SkyBlock Levels in Chat off, or back on, for their account. */
    static void toggleLevelsInChat(Player player, User user) {
        Document settings = settings(user);
        boolean on = !levelsInChat(settings);
        settings.put(LEVELS_IN_CHAT, on);
        if (on) levelsHidden.remove(player.getUniqueId());
        else levelsHidden.add(player.getUniqueId());
        user.save();
    }

    static boolean levelsInChat(Document settings) {
        return !Boolean.FALSE.equals(settings.getBoolean(LEVELS_IN_CHAT));
    }

    private static Document settings(User user) {
        Document doc = user.getDocument();
        if (doc.get(Profiles.SETTINGS) instanceof Document settings) return settings;
        Document settings = new Document();
        doc.put(Profiles.SETTINGS, settings);
        return settings;
    }

    /** A chosen emblem shows at once. */
    static void emblemChanged(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user != null) snapshots.put(player.getUniqueId(), compute(user));
    }

    // Joining and leaving

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null) return;
        if (levelsInChat(settings(user))) levelsHidden.remove(player.getUniqueId());
        else levelsHidden.add(player.getUniqueId());
        snapshot(user);
        // A level reached where nothing told them (another server stopped before it could).
        changed(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        snapshots.remove(id);
        levelsHidden.remove(id);
    }
}
