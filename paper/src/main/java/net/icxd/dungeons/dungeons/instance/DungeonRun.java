package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonLevels;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.paste.PastePlan;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * A run from the moment its floor is built until it closes, as on Hypixel (from recordings of
 * Entrance runs: messages, timings, sidebar and tab list):
 *
 * <ol>
 *   <li>Waiting: players stand at the back of the entrance room, its door shut, and ready up
 *       with Mort (the Ready Up menu), where they also pick a class. A run
 *       nobody starts closes after 2 minutes, with warnings for the last 30 seconds.</li>
 *   <li>Once everyone here is ready, it starts 4 seconds later (readying down stops that).</li>
 *   <li>Running: Mort hands out the map, the door opens, the clock runs. Keys open the wither
 *       doors and the Blood Door ({@link RunDoors}), the map fills in as rooms are walked into
 *       ({@link RunMap}), and the Blood Door starts the Watcher's fight ({@link Watcher}).</li>
 *   <li>Ended, when the Watcher lets them pass (no bosses yet) or with {@code /dungeon end}: the
 *       score and a re-queue link, and 20 seconds later everyone goes to the Dungeon Hub.</li>
 * </ol>
 *
 * <p>Main thread. {@link RunManager} ticks it every tick and every second.
 */
public final class DungeonRun {
    public enum Phase { WAITING, STARTING, RUNNING, ENDED }

    static final long AUTO_CLOSE_MILLIS = 120_000;
    private static final int COUNTDOWN_SECONDS = 4;
    private static final Set<Integer> CLOSE_WARNINGS = Set.of(30, 15, 10, 5, 4, 3, 2, 1);
    /** Ticks after the start until the entrance door opens (Hypixel's timing). */
    private static final long DOOR_OPENS = 4;
    /** How often the map checks which rooms people are in. */
    private static final int FIND_ROOMS_EVERY = 10;
    /** How long after arriving auto ready kicks in. */
    private static final long AUTO_READY_DELAY = 40;
    /** Ticks after the end: the re-queue link, the warning, and closing (Hypixel's +2.1, +10.1 and +20.2 seconds). */
    private static final long REQUEUE_MESSAGE = 42;
    private static final long CLOSE_WARNING = 202;
    private static final long CLOSE = 404;
    /**
     * From the room's centre: where players arrive, 4 blocks towards the back and at y 76.5 (they
     * drop onto the raised back of the room, as on Hypixel), and where Mort stands, 11 towards the door.
     */
    private static final int ARRIVAL_BACK = 4;
    private static final double ARRIVAL_Y = 76.5;
    private static final int MORT_FORWARD = 11;

    /** Someone in the run, and what they've done in it. */
    static final class Member {
        final UUID id;
        String name;
        String rankColor = "§7";
        String rankPrefix = "§7";
        boolean ready;
        boolean arrived;
        double damage;
        double healing;
        int kills;
        int deaths;
        int secrets;

        Member(UUID id) {
            this.id = id;
            // Until they get here; this server may not have seen them before.
            this.name = Objects.requireNonNullElse(Bukkit.getOfflinePlayer(id).getName(), "?");
        }

        /** "§b[MVP§6+§b] Name". */
        String display() {
            return rankPrefix + name;
        }
    }

    public record TabEntry(String text, UUID player) {
    }

    private final RunManager manager;
    private final Plugin plugin;
    final String id;
    final DungeonFloor floor;
    final World world;
    private final Map<UUID, Member> members = new LinkedHashMap<>();
    private final Location arrival;
    private final Location entrance;
    private final int rooms;
    private final int puzzles;
    private final Mort mort;
    private final RunLayout layout;
    private final RunDoors doors;
    private final RunMap runMap;
    private final DisplayCases cases;
    private Watcher watcher;
    private int ticks;
    private final MapView map;
    private Phase phase = Phase.WAITING;
    private final long closesAt;
    private int countdown;
    private long startedAt;
    private long endedAt;
    private Score finalScore;
    private boolean closed;

    DungeonRun(RunManager manager, Plugin plugin, String id, DungeonFloor floor, List<UUID> members, World world,
               PastePlan.Block center, Direction door, RunLayout layout, int rooms, int puzzles, MapView map) {
        this.manager = manager;
        this.plugin = plugin;
        this.id = id;
        this.floor = floor;
        this.world = world;
        this.rooms = rooms;
        this.puzzles = puzzles;
        this.map = map;
        this.layout = layout;
        for (UUID member : members) this.members.put(member, new Member(member));
        this.closesAt = System.currentTimeMillis() + AUTO_CLOSE_MILLIS;

        int cx = center.x();
        int cz = center.z();
        this.arrival = new Location(world, cx + 0.5 - ARRIVAL_BACK * door.dx, ARRIVAL_Y, cz + 0.5 - ARRIVAL_BACK * door.dy,
                yaw(door.dx, door.dy), 0);
        this.entrance = RunManager.standingSpot(world, cx, center.y(), cz);
        this.mort = Mort.spawn(new Location(world, cx + 0.5 + MORT_FORWARD * door.dx, center.y(), cz + 0.5 + MORT_FORWARD * door.dy,
                yaw(-door.dx, -door.dy), 0));
        this.doors = new RunDoors(this, plugin, world, layout);
        this.runMap = new RunMap(this, layout);
        PlacedRoom blood = layout.bloodRoom();
        this.cases = blood == null ? null : DisplayCases.place(world, layout.center(world, RunLayout.firstCell(blood)));
        PlacedRoom start = layout.roomAt(entrance);
        if (start != null) runMap.find(start);
    }

    /** Minecraft's yaw for looking along (dx, dz): 0 is south (+z), 90 west. */
    private static float yaw(int dx, int dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    public Phase phase() {
        return phase;
    }

    List<Member> members() {
        return List.copyOf(members.values());
    }

    Member member(UUID id) {
        return members.get(id);
    }

    boolean isMort(org.bukkit.entity.Entity entity) {
        return mort.is(entity);
    }

    /** Members here, in this run (after a re-queue they're in the next one). */
    List<Player> players() {
        return members.keySet().stream().filter(m -> manager.belongs(m, this)).map(Bukkit::getPlayer)
                .filter(p -> p != null && p.getWorld().equals(world)).toList();
    }

    void tell(String message) {
        String colored = Utils.color(message);
        for (Player player : players()) player.sendMessage(colored);
    }

    private void tell(Component message) {
        for (Player player : players()) player.sendMessage(message);
    }

    // Arriving

    /** Puts a member where the run is at; the first time, also pauses their effects and maybe readies them up. */
    void arrive(Player player) {
        Member member = members.get(player.getUniqueId());
        if (member == null) return;
        User user = User.cached(player.getUniqueId());
        if (user != null && user.isLoaded()) {
            Rank rank = user.getRank();
            member.rankColor = rank.getColor();
            member.rankPrefix = rank.getPrefix();
        }
        member.name = player.getName();
        player.teleport(phase == Phase.WAITING || phase == Phase.STARTING ? arrival : entrance);
        if (member.arrived) return;
        member.arrived = true;
        // The score card of the run they re-queued from.
        takeRunItems(player);
        // Hypixel pauses potion effects in dungeons. Here they're per server anyway: the ones they had
        // elsewhere are waiting for them there.
        if (!player.getActivePotionEffects().isEmpty()) {
            for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
            player.sendMessage(Utils.color("&aYou are not allowed to use Potion Effects while in Dungeon, therefore all active effects "
                    + "have been paused and stored. They will be restored when you leave Dungeon!"));
        }
        if (user != null && user.isLoaded() && DungeonProfile.autoReadyUp(user)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!closed && player.isOnline() && phase == Phase.WAITING && !member.ready) setReady(player, true);
            }, AUTO_READY_DELAY);
        }
    }

    // Ready Up

    void setReady(Player player, boolean ready) {
        Member member = members.get(player.getUniqueId());
        if (member == null || member.ready == ready || !(phase == Phase.WAITING || phase == Phase.STARTING)) return;
        member.ready = ready;
        tell(member.rankColor + member.name + (ready ? "&a is now ready!" : "&c is no longer ready!"));
        if (!ready && phase == Phase.STARTING) phase = Phase.WAITING;
    }

    void selectClass(Player player, DungeonClass dungeonClass) {
        Member member = members.get(player.getUniqueId());
        User user = User.cached(player.getUniqueId());
        if (member == null || user == null || !user.isLoaded() || DungeonProfile.selectedClass(user) == dungeonClass) return;
        DungeonProfile.selectClass(user, dungeonClass);
        tell(member.rankColor + member.name + "&a selected the " + dungeonClass.getDisplayName() + " Class!");
    }

    DungeonClass classOf(UUID id) {
        User user = User.cached(id);
        return user == null || !user.isLoaded() ? DungeonClass.HEALER : DungeonProfile.selectedClass(user);
    }

    int classLevel(UUID id) {
        User user = User.cached(id);
        return user == null || !user.isLoaded() ? 0 : DungeonProfile.classLevel(user, classOf(id));
    }

    // Every tick

    void tick() {
        if (closed || phase != Phase.RUNNING) return;
        ticks++;
        List<Player> here = players();
        doors.tick(here);
        if (ticks % FIND_ROOMS_EVERY == 0) {
            for (Player player : here) {
                PlacedRoom room = layout.roomAt(player.getLocation());
                if (room != null) runMap.find(room);
            }
        }
        if (watcher != null) watcher.tick();
    }

    // Doors, keys and the map

    /** A right-click on a block; true if it was a shut door (and taken care of). */
    boolean clickBlock(Player player, Block block) {
        return phase == Phase.RUNNING && doors.click(player, block);
    }

    boolean isShut(Door door) {
        return doors.isShut(door);
    }

    boolean isKey(org.bukkit.entity.Entity entity) {
        return doors.isKey(entity);
    }

    void doorOpened(Door door) {
        runMap.changed();
    }

    /** The Watcher's fight starts. */
    void bloodDoorOpened() {
        PlacedRoom blood = layout.bloodRoom();
        if (blood != null && watcher == null) watcher = new Watcher(this, layout, blood, cases);
    }

    /** "You have proven yourself. You may pass.": the Blood Room is done. */
    void bloodRoomCleared() {
        PlacedRoom blood = layout.bloodRoom();
        if (blood != null) runMap.complete(blood);
    }

    // Every second

    void second() {
        if (closed) return;
        long now = System.currentTimeMillis();
        switch (phase) {
            case WAITING -> {
                long left = closesAt - now;
                int seconds = (int) Math.ceil(left / 1000.0);
                if (left <= 0) {
                    close();
                    return;
                }
                if (CLOSE_WARNINGS.contains(seconds)) {
                    tell("&cWarning! &eThis instance will &cclose &ein &a" + seconds + " &e" + (seconds == 1 ? "second" : "seconds") + " if it isn't started!");
                }
                List<Player> here = players();
                if (!here.isEmpty() && here.stream().allMatch(p -> members.get(p.getUniqueId()).ready)) {
                    phase = Phase.STARTING;
                    countdown = COUNTDOWN_SECONDS;
                    tell("&aStarting in " + countdown + " seconds.");
                }
            }
            case STARTING -> {
                countdown--;
                if (countdown <= 0) start();
                else tell("&aStarting in " + countdown + (countdown == 1 ? " second." : " seconds."));
            }
            case RUNNING, ENDED -> {
            }
        }
    }

    // Start

    private void start() {
        phase = Phase.RUNNING;
        startedAt = System.currentTimeMillis();
        mortSays("Here, I found this map when I first entered the dungeon.");
        runMap.show(map);
        for (Player player : players()) giveMap(player, "&bMagical Map", List.of("&7Shows the layout of the Dungeon as", "&7it is explored and completed."));
        later(DOOR_OPENS, () -> {
            Door entranceDoor = doors.entranceDoor();
            if (entranceDoor != null) doors.open(entranceDoor);
        });
        later(40, () -> mortSays("You should find it useful if you get lost."));
        later(70, () -> mortSays("Good luck."));
    }

    void later(long ticks, Runnable task) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!closed) task.run();
        }, ticks);
    }

    private void mortSays(String line) {
        tell("&e[NPC] &bMort&f: " + line);
    }

    /**
     * Hotbar slot 9, where Hypixel's SkyBlock Menu is; whatever was there moves to a free slot, and
     * with no free slot they go without. Never saved to their stored inventory.
     */
    private void giveMap(Player player, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) item.getItemMeta();
        meta.setMapView(map);
        meta.setDisplayName(Utils.color(name));
        meta.setLore(Utils.colorList(lore));
        item.setItemMeta(meta);
        StoredInventory.markNotSaved(item);
        PlayerInventory inventory = player.getInventory();
        ItemStack old = inventory.getItem(8);
        if (old != null && !old.isEmpty() && !StoredInventory.isNotSaved(old)) {
            int free = inventory.firstEmpty();
            if (free < 0) return;
            inventory.setItem(free, old);
        }
        inventory.setItem(8, item);
    }

    /** Takes run items (the map) off a player who's leaving. */
    static void takeRunItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (StoredInventory.isNotSaved(inventory.getItem(slot))) inventory.setItem(slot, null);
        }
    }

    // End

    /**
     * The run is over: the score, EXTRA STATS, and off to the Dungeon Hub 20 seconds later. For
     * now only {@code /dungeon end} does it (the bosses come later).
     *
     * @return false if it isn't running
     */
    public boolean end() {
        if (phase != Phase.RUNNING) return false;
        phase = Phase.ENDED;
        endedAt = System.currentTimeMillis();
        finalScore = score(endedAt);
        String grade = gradeColor(finalScore.grade()) + finalScore.grade();
        tell(RunText.RULE);
        tell(RunText.centered("&c" + floor.getDungeonName() + " &8- &e" + floor.getTierName()));
        tell("");
        tell(RunText.centered("Team Score: &a" + finalScore.total() + " &f(" + grade + "&f)"));
        tell(RunText.centered("&c☠ &eDefeated &c" + floor.getBossName() + " &ein &a" + RunText.elapsed(endedAt - startedAt)));
        tell(legacy(RunText.centered("&6> &e&lEXTRA STATS &6<"))
                .clickEvent(ClickEvent.runCommand("/showextrastats"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to view extra stats!", NamedTextColor.YELLOW))));
        tell(RunText.RULE);
        ScoreCard.draw(map, floor, finalScore);
        for (Player player : players()) giveMap(player, "&a&lYour Score Summary", List.of());
        later(REQUEUE_MESSAGE, this::requeueMessage);
        later(CLOSE_WARNING, () -> tell("&cWarning! &eThe instance will &cclose &ein &a10s&e."));
        later(CLOSE, this::close);
        return true;
    }

    private void requeueMessage() {
        tell("");
        tell(RunText.RULE);
        tell(legacy("      &7Click &e&lHERE &7to re-queue into &a" + floor.getDungeonName() + "&7!")
                .clickEvent(ClickEvent.runCommand("/instancerequeue"))
                .hoverEvent(HoverEvent.showText(legacy("&7Instance: &a" + floor.getDungeonName() + "\n&7Tier: &b" + floor.getTierName()))));
        tell(RunText.RULE);
        tell("");
    }

    /** {@code /showextrastats}: after the run, how everyone did. */
    public void showExtraStats(Player player) {
        if (phase != Phase.ENDED) {
            player.sendMessage(Utils.color("&cYou can only view extra stats once the dungeon is over!"));
            return;
        }
        Member me = members.get(player.getUniqueId());
        int deaths = members.values().stream().mapToInt(m -> m.deaths).sum();
        int secrets = members.values().stream().mapToInt(m -> m.secrets).sum();
        int kills = members.values().stream().mapToInt(m -> m.kills).sum();
        double healing = members.values().stream().mapToDouble(m -> m.healing).sum();
        List<String> lines = List.of(
                RunText.RULE,
                RunText.centered("&c" + floor.getDungeonName() + " &8- &e" + floor.getTierName() + " Stats"),
                "",
                RunText.centered("Team Score: &a" + finalScore.total() + " &f(" + gradeColor(finalScore.grade()) + finalScore.grade() + "&f)"),
                "",
                RunText.centered("Total Damage as " + classOf(player.getUniqueId()).getDisplayName() + ": &a" + Utils.getFormattedNumber((int) me.damage)),
                RunText.centered("Ally Healing: &a" + Utils.getFormattedNumber((int) healing)),
                RunText.centered("Enemies Killed: &a" + Utils.getFormattedNumber(kills)),
                RunText.centered("Deaths: &c" + deaths),
                RunText.centered("Secrets Found: &b" + secrets),
                "",
                RunText.RULE);
        for (String line : lines) player.sendMessage(Utils.color(line));
    }

    /** Hypixel's colours for B and C (from end-of-run messages); the others are a guess until we see them. */
    static String gradeColor(String grade) {
        return switch (grade) {
            case "S+" -> "&6";
            case "S" -> "&e";
            case "A" -> "&5";
            case "B" -> "&e";
            case "C" -> "&6";
            default -> "&c";
        };
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(Utils.color(text));
    }

    // Closing

    /** Everyone to the Dungeon Hub, and the run is done. */
    private void close() {
        if (closed) return;
        for (Player player : players()) {
            player.closeInventory();
            manager.leave(player);
        }
        manager.finish(this);
    }

    /** Takes everything this run put in its world away again. */
    void dispose() {
        closed = true;
        mort.remove();
        doors.dispose();
        if (watcher != null) watcher.dispose();
        if (cases != null) cases.dispose();
        for (Player player : players()) takeRunItems(player);
        ScoreCard.blank(map);
    }

    // Stats

    public boolean isStarted() {
        return phase == Phase.RUNNING || phase == Phase.ENDED;
    }

    void damageDealt(UUID member, double damage) {
        Member m = members.get(member);
        if (m != null && phase == Phase.RUNNING) m.damage += damage;
    }

    void killed(UUID member) {
        Member m = members.get(member);
        if (m != null && phase == Phase.RUNNING) m.kills++;
    }

    void died(UUID member) {
        Member m = members.get(member);
        if (m != null && phase == Phase.RUNNING) m.deaths++;
    }

    Score score(long now) {
        int deaths = members.values().stream().mapToInt(m -> m.deaths).sum();
        int secrets = members.values().stream().mapToInt(m -> m.secrets).sum();
        double seconds = startedAt == 0 ? 0 : (now - startedAt) / 1000.0;
        // Secrets, puzzles, crypts and the mimic come with clearing.
        return Score.of(floor, new Score.Inputs(runMap.completedRooms(), rooms, secrets, 0, deaths, false, 0, 0, false, false, seconds));
    }

    // Sidebar and tab list

    /** The lines under the date: Hypixel's dungeon sidebar at each stage. */
    public List<String> sidebar(Player viewer, String dateLine, String season, String time) {
        List<String> lines = new ArrayList<>();
        String where = "&7 ⏣ &c" + floor.getDungeonName().replace("Master Mode ", "") + " &7(" + floor.getShortName() + ")";
        if (phase == Phase.WAITING || phase == Phase.STARTING) {
            lines.add("");
            lines.add("");
            lines.add(dateLine);
            lines.add("");
            lines.add(season);
            lines.add(time);
            lines.add(where);
            lines.add("");
            for (Member m : members.values()) {
                lines.add((m.ready ? "&a[" : "&c[") + classOf(m.id).getLetter() + "] " + m.rankColor + m.name + " &7[Lv" + classLevel(m.id) + "]");
            }
            lines.add("");
            lines.add(phase == Phase.STARTING ? "&fStarting in: &a0:" + String.format("%02d", countdown)
                    : "&fAuto-closing in: &c" + RunText.clock(closesAt - System.currentTimeMillis()));
        } else {
            long now = phase == Phase.ENDED ? endedAt : System.currentTimeMillis();
            Score score = phase == Phase.ENDED ? finalScore : score(now);
            lines.add(dateLine);
            lines.add("");
            lines.add(season);
            lines.add(time);
            lines.add(where);
            lines.add("");
            lines.add("&fKeys: &c■ " + (doors.hasBloodKey() ? "&a✓" : "&c✗") + " &8■ &a" + doors.witherKeys() + "x");
            lines.add("&fTime Elapsed: &a" + RunText.elapsed(now - startedAt));
            lines.add("&fCleared: &c" + cleared() + "% &8(" + score.total() + ")");
            lines.add("");
            List<Member> others = members.values().stream().filter(m -> !m.id.equals(viewer.getUniqueId())).toList();
            if (others.isEmpty()) lines.add("&3&lSolo");
            for (Member m : others) {
                Player p = Bukkit.getPlayer(m.id);
                int health = p == null ? 0 : (int) p.getHealth();
                lines.add("&e[" + classOf(m.id).getLetter() + "] " + m.rankColor + m.name + " &a" + Utils.getFormattedNumber(health) + "&c❤");
            }
        }
        lines.add("");
        lines.add("&ewww.hypixel.net");
        return lines;
    }

    /** Hypixel's dungeon tab list, column by column (80 entries). */
    public List<TabEntry> tab(Player viewer) {
        List<TabEntry> out = new ArrayList<>(80);
        boolean started = isStarted();

        column(out, "         &b&lParty &f(" + members.size() + ")", () -> {
            List<TabEntry> party = new ArrayList<>();
            for (Member m : members.values()) {
                String what = started ? "&d" + classOf(m.id).getDisplayName() + " " + DungeonLevels.roman(classLevel(m.id)) : "&7EMPTY";
                party.add(new TabEntry("&8[" + skyBlockLevel(m.id) + "&8] " + m.rankColor + m.name + " &f(" + what + "&f)", m.id));
                party.add(new TabEntry(" Ultimate: " + (started ? "&aReady" : "&cN/A"), null));
                party.add(new TabEntry(" Revive Stones: &c0", null));
                party.add(new TabEntry("", null));
            }
            return party;
        });

        int deaths = members.values().stream().mapToInt(m -> m.deaths).sum();
        double damage = members.values().stream().mapToDouble(m -> m.damage).sum();
        double healing = members.values().stream().mapToDouble(m -> m.healing).sum();
        int secrets = members.values().stream().mapToInt(m -> m.secrets).sum();
        column(out, "       &2&lPlayer Stats", () -> texts(
                "&a&lDowned: &7NONE",
                " Time: &eN/A",
                " Revive: &cN/A",
                "",
                "&a&lTeam Deaths: &f" + deaths,
                " Team Damage Dealt: &c" + RunText.compact(damage) + "❤",
                " Team Healing Done: &c" + RunText.compact(healing) + "❤",
                " Your Milestone: &e?",
                "",
                "&a&lDiscoveries: &f" + secrets,
                " Secrets Found: &b" + secrets,
                " Crypts: &60"));

        long now = phase == Phase.ENDED ? endedAt : System.currentTimeMillis();
        column(out, "       &3&lDungeon Stats", () -> {
            List<TabEntry> stats = texts(
                    "&b&lDungeon: &7Catacombs",
                    " Opened Rooms: &5" + runMap.foundRooms(),
                    " Completed Rooms: &d" + runMap.completedRooms(),
                    " Secrets Found: &e0%",
                    " Time: &6" + (started ? RunText.elapsed(now - startedAt) : "Soon!"),
                    "",
                    "&b&lPuzzles: &f(" + puzzles + ")");
            // Unknown until someone finds them.
            for (int i = 0; i < puzzles; i++) stats.add(new TabEntry(" ???: &7[&6&l✦&7]", null));
            return stats;
        });

        User user = User.cached(viewer.getUniqueId());
        Stats stats = PlayerSession.of(viewer).stats();
        column(out, "       &6&lAccount Info", () -> texts(
                "&e&lProfile: &cN/A",
                " Bank: &6" + (user != null && user.isLoaded() ? Utils.formatNumber(user.getBankBalance()) : "0"),
                "",
                "&e&lSkills:",
                " Speed: &f" + (stats == null ? 0 : (int) stats.get(Stat.SPEED)),
                " Strength: &c" + (stats == null ? 0 : (int) stats.get(Stat.STRENGTH)),
                " Crit Chance: &9" + (stats == null ? 0 : (int) stats.get(Stat.CRIT_CHANCE)),
                " Crit Damage: &9" + (stats == null ? 0 : (int) stats.get(Stat.CRIT_DAMAGE)),
                " Attack Speed: &e" + (stats == null ? 0 : (int) stats.get(Stat.ATTACK_SPEED))));
        return out;
    }

    /** How much of the floor is done, by rooms. */
    private int cleared() {
        return rooms == 0 ? 0 : runMap.completedRooms() * 100 / rooms;
    }

    private static List<TabEntry> texts(String... lines) {
        List<TabEntry> out = new ArrayList<>();
        for (String line : lines) out.add(new TabEntry(line, null));
        return out;
    }

    private static void column(List<TabEntry> out, String header, Supplier<List<TabEntry>> body) {
        out.add(new TabEntry(header, null));
        List<TabEntry> lines = body.get();
        for (int i = 0; i < 19; i++) out.add(i < lines.size() ? lines.get(i) : new TabEntry("", null));
    }

    /** SkyBlock levels are coloured by the 40 they're in (as in Hypixel's tab list). */
    private static final String[] LEVEL_COLORS = {"&7", "&f", "&e", "&a", "&2", "&b", "&3", "&9", "&d", "&5", "&6", "&c", "&4"};

    /** "&e88": the SkyBlock level in its colour. */
    private static String skyBlockLevel(UUID id) {
        User user = User.cached(id);
        int level = user == null ? 0 : user.getSkyBlockXp() / 100;
        return LEVEL_COLORS[Math.min(level / 40, LEVEL_COLORS.length - 1)] + level;
    }
}
