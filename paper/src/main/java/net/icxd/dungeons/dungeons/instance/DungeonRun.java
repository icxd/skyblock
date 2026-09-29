package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
import net.icxd.dungeons.dungeons.instance.puzzle.RunPuzzles;
import net.icxd.dungeons.dungeons.paste.PastePlan;
import net.icxd.dungeons.leveling.SkyBlockLevels;
import net.icxd.dungeons.leveling.SkyBlockXp;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.SkillText;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;
import net.icxd.dungeons.utils.Text;
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
public final class DungeonRun implements ScoreCounts {
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
    private final RoomMobs roomMobs;
    private final DisplayCases cases;
    private final RunPuzzles puzzleRooms;
    private Watcher watcher;
    /** From the start ({@link #start}); the blessings once one is found. */
    private RunSecrets secrets;
    private RunBlessings blessings;
    private final Ghosts ghosts;
    private final RunClasses classes;
    private Fairies fairies;
    private boolean failed;
    private int ticks;
    private final MapView map;
    private Phase phase = Phase.WAITING;
    private final long closesAt;
    private int countdown;
    private long startedAt;
    private long endedAt;
    /** At the end: the chat's score, without the Blood Room, and the card's, with it (see {@link RunEnd}). */
    private Score chatScore;
    private Score cardScore;
    /** The Watcher let them pass; the Blood Room counts once the summary is out. */
    private boolean watcherPassed;
    /** At the end: the secrets' share, and the members who have had their summary (and rewards). */
    private double endSecretPercent;
    private final Set<UUID> summarized = new HashSet<>();
    private final SidebarScore sidebarScore = new SidebarScore();
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
        this.ghosts = new Ghosts(this, plugin);
        this.classes = new RunClasses(this);
        this.runMap = new RunMap(this, layout);
        this.roomMobs = new RoomMobs(this, world, layout, doors, floor, plugin.getLogger());
        PlacedRoom blood = layout.bloodRoom();
        this.cases = blood == null ? null : DisplayCases.place(world, layout.center(world, RunLayout.firstCell(blood)));
        PlacedRoom start = layout.roomAt(entrance);
        if (start != null) runMap.find(start);
        this.puzzleRooms = RunPuzzleHost.puzzles(this, plugin, layout, manager.puzzleData());
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
        if (member.arrived && phase == Phase.ENDED) backAfterTheEnd(player);
        ghosts.arrived(player);
        if (member.arrived) return;
        member.arrived = true;
        RunClasses.claimOrb(this, player);
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

    /** Whether their profile is here to tell their class (not for a member who isn't on this server yet). */
    boolean classKnown(UUID id) {
        User user = User.cached(id);
        return user != null && user.isLoaded();
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
        if (closed) return;
        if (phase != Phase.RUNNING) {
            // The rooms' mobs are spawned while the party waits.
            roomMobs.tickWaiting();
            return;
        }
        ticks++;
        List<Player> here = players();
        // Ghosts don't pick up keys, put out a room's secrets or start its puzzle: they only touch the
        // world through ghost abilities and fairies.
        List<Player> living = here.stream().filter(p -> !ghosts.isGhost(p.getUniqueId())).toList();
        doors.tick(living);
        if (secrets != null) secrets.tick(living);
        puzzleRooms.tick(living);
        if (ticks % FIND_ROOMS_EVERY == 0) {
            for (Player player : here) {
                PlacedRoom room = layout.roomAt(player.getLocation());
                if (room == null) continue;
                runMap.find(room);
                // Walking into a room opens it, for its mobs.
                roomMobs.open(room);
            }
        }
        roomMobs.tick(here);
        if (watcher != null) watcher.tick();
        ghosts.tick();
        classes.tick();
        if (fairies != null) fairies.tick();
    }

    // Doors, keys and the map

    /** A right-click on a block; true if it was a shut door (and taken care of). */
    boolean clickBlock(Player player, Block block) {
        return phase == Phase.RUNNING && !ghosts.isGhost(player.getUniqueId()) && doors.click(player, block);
    }

    boolean isShut(Door door) {
        return doors.isShut(door);
    }

    boolean isKey(org.bukkit.entity.Entity entity) {
        return doors.isKey(entity);
    }

    void doorOpened(Door door) {
        runMap.changed();
        // The room behind it opens, for its mobs.
        roomMobs.open(layout.room(door.child()));
    }

    /** The Watcher's fight starts. */
    void bloodDoorOpened() {
        PlacedRoom blood = layout.bloodRoom();
        if (blood != null && watcher == null) watcher = new Watcher(this, layout, blood, cases);
    }

    // Puzzles

    /** Puzzles failed, not solved yet, or never found (each costs 10 of the score). */
    public int puzzlesNotDone() {
        return puzzleRooms.notDone();
    }

    /** A puzzle room failed: its red cross on the map (it doesn't count as cleared). */
    void puzzleFailed(PlacedRoom room) {
        runMap.fail(room);
    }

    /**
     * "You have proven yourself. You may pass.": the Blood Room is done, though Hypixel only counts it
     * after the end-of-run summary (see {@link #end}).
     */
    void bloodRoomCleared() {
        watcherPassed = true;
    }

    // Room mobs (RoomMobs): clearing rooms, crypts, room loot

    /** A room is done (its starred mobs are all dead, or its puzzle solved): its tick on the map, the sidebar's Cleared, and a RoomClearedEvent. */
    void clearedRoom(PlacedRoom room) {
        runMap.complete(room);
        Bukkit.getPluginManager().callEvent(new RoomClearedEvent(this, room));
    }

    /** Whether a room's starred mobs are all dead (rooms with nothing to clear, like the fairy room, aren't counted here). */
    public boolean roomCleared(PlacedRoom room) {
        return roomMobs.isCleared(room);
    }

    /** How many crypts are done: blown up and their Crypt Undead killed (the bonus score counts at most 5). */
    public int cryptsBlown() {
        return roomMobs.cryptsBlown();
    }

    /** A Superboom TNT went off at this block. */
    void superboom(Block at) {
        roomMobs.superboom(at);
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
            case RUNNING -> {
                sidebarScore.tick(now, this::sidebarScore);
                int here = 0;
                int ghostsHere = 0;
                for (Player player : players()) {
                    here++;
                    if (ghosts.isGhost(player.getUniqueId())) ghostsHere++;
                }
                if (DeathRules.failed(here, ghostsHere, DeathRules.autoReviveSeconds(floor) >= 0, now - startedAt)) fail();
            }
            case ENDED -> sidebarScore.tick(now, this::sidebarScore);
        }
    }

    // Start

    private void start() {
        phase = Phase.RUNNING;
        startedAt = System.currentTimeMillis();
        sidebarScore.start(startedAt);
        classes.start(members());
        fairies = Fairies.spawn(this, layout, world);
        mortSays("Here, I found this map when I first entered the dungeon.");
        runMap.show(map);
        for (Player player : players()) giveMap(player, "&bMagical Map", List.of("&7Shows the layout of the Dungeon as", "&7it is explored and completed."));
        startSecrets();
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
        meta.displayName(Text.line(name));
        meta.lore(Text.lines(lore));
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

    /** Takes run items (the map, Revive Stones) off a player who's leaving. */
    static void takeRunItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (StoredInventory.isNotSaved(inventory.getItem(slot))) inventory.setItem(slot, null);
        }
        ReviveStones.takeAll(player);
    }

    // Secrets and blessings (RunSecrets, RunBlessings)

    /** The run starts: its secrets, from the rooms' data (RunManager has it), and the levers reset. */
    private void startSecrets() {
        secrets = new RunSecrets(this, plugin, world, layout, floor, manager.secretData());
        secrets.start();
    }

    /** Null before the start. */
    RunSecrets secrets() {
        return secrets;
    }

    /** Whether it's a Master Mode floor (where master stars count: ItemStats). */
    public boolean masterMode() {
        return floor.isMasterMode();
    }

    /** Its floor's number: 0 for the Entrance, 1 to 7 (the same for Master Mode's; the boss heads' "on The Catacombs Floor I"). */
    public int floorNumber() {
        return floor.getNumber();
    }

    /** Every secret on the floor, as Hypixel counts them room by room (for the score and the tab list). */
    public int totalSecrets() {
        return secrets == null ? 0 : secrets.total();
    }

    /** How many of them the team has found. */
    public int secretsFound() {
        return secrets == null ? 0 : secrets.found();
    }

    /** Whether all of a room's secrets are found (a room without any: yes), for its tick's colour on the map. */
    boolean allSecretsFound(PlacedRoom room) {
        return secrets != null && secrets.allFound(room);
    }

    /** "          &72/5 Secrets", after the mana on the action bar, in a room with secrets. */
    public String secretsActionBar(Player player) {
        return phase == Phase.RUNNING && secrets != null ? secrets.actionBar(player) : "";
    }

    /** A member's stats with the team's blessings, from the start until they leave (PlayerStats). */
    public void applyBlessings(Stats stats) {
        if (isStarted() && blessings != null) blessings.apply(stats);
    }

    /**
     * Someone picked up a blessing a room dropped where its last starred mob died ("Name has obtained Blessing of
     * Wisdom!" has been said): the team has it at this level, with "DUNGEON BUFF!" and the run's time.
     * {@code blessing} is its kind ("Wisdom").
     */
    void blessingFound(Player player, String blessing, int level) {
        Blessing kind = Blessing.named(blessing);
        if (kind != null) blessingFound(player, kind, level, Blessing.elapsed(System.currentTimeMillis() - startedAt));
    }

    /**
     * {@code finder} found a blessing: the whole team has it for the rest of the run, with Hypixel's lines
     * ("DUNGEON BUFF! You found a Blessing of Stone I!" and what it grants). {@code elapsed}, the run's time,
     * for one picked up from a room; null for one out of a chest or a bat.
     */
    void blessingFound(Player finder, Blessing blessing, int level, String elapsed) {
        if (phase != Phase.RUNNING || level <= 0) return;
        if (blessings == null) blessings = new RunBlessings(floor);
        blessings.add(blessing, level);
        Member member = members.get(finder.getUniqueId());
        String who = member != null ? member.display() : finder.getName();
        List<String> granted = blessing.granted(level, blessings.strength());
        for (Player player : players()) {
            player.sendMessage(Utils.color(blessing.found(player.equals(finder) ? null : who, level, elapsed)));
            for (String line : granted) player.sendMessage(Utils.color(line));
        }
    }

    /**
     * A chest with a blessing of this level opened (a puzzle's reward chest): it stays open, harp notes, its
     * name over it and the "DUNGEON BUFF!" lines, as a secret chest's. Its kind is picked at random (which
     * one Hypixel gives is UNKNOWN).
     */
    void blessingChest(Player player, Block chest, int level) {
        if (secrets != null) secrets.blessingChest(player, chest, Blessing.random(ThreadLocalRandom.current()), level);
    }

    /** The tab list footer while the run's on: its Dungeon Buffs (recorded from the start); null before. */
    public List<String> tabFooter() {
        if (!isStarted()) return null;
        return blessings == null ? new RunBlessings(floor).footer() : blessings.footer();
    }

    // End

    /**
     * The run is over (the Watcher let them pass, or {@code /dungeon end}): each member's summary with
     * their experience and Bits, saved to the profile they play on; then the Blood Room counts and the
     * map becomes the score card; and off to the Dungeon Hub 20 seconds later (see {@link RunEnd}).
     *
     * @return false if it isn't running
     */
    public boolean end() {
        if (phase != Phase.RUNNING) return false;
        phase = Phase.ENDED;
        endedAt = System.currentTimeMillis();
        // Before the Blood Room counts, as Hypixel's chat has it.
        chatScore = score(endedAt);
        rewardAndSummarize();
        later(RunEnd.CARD, this::showScoreCard);
        later(RunEnd.CARD_ITEM, () -> {
            for (Player player : players()) giveMap(player, "&a&lYour Score Summary", List.of());
        });
        later(RunEnd.REQUEUE_MESSAGE, this::requeueMessage);
        later(RunEnd.CLOSE_WARNING, () -> tell("&cWarning! &eThe instance will &cclose &ein &a10s&e."));
        later(RunEnd.CLOSE, this::close);
        return true;
    }

    /** Each member here gets their experience and Bits, saved to the profile they play on, and their summary. */
    private void rewardAndSummarize() {
        endSecretPercent = scoreInputs(endedAt).secretPercent();
        List<Player> here = players();
        for (Player player : here) rewardAndSummarize(player, here);
    }

    /**
     * One member's experience and Bits (their teammates are the others {@code here}), saved to the
     * profile they play on, and their summary; once a run.
     */
    private void rewardAndSummarize(Player player, List<Player> here) {
        if (!summarized.add(player.getUniqueId())) return;
        long millis = endedAt - startedAt;
        User user = User.ifLoaded(player.getUniqueId());
        RunEnd.Outcome outcome = null;
        // Handed off (a warp, or a transfer waiting to be reclaimed): nothing given here would be saved.
        if (user != null && !user.isReleased()) {
            List<DungeonClass> teammates = here.stream().filter(p -> !p.equals(player)).map(p -> classOf(p.getUniqueId())).toList();
            outcome = failed
                    ? RunEnd.awardFailed(user.profile(), floor, chatScore, endSecretPercent, classOf(player.getUniqueId()), teammates)
                    : RunEnd.award(user.profile(), floor, chatScore, millis, endSecretPercent, classOf(player.getUniqueId()), teammates,
                    RunEnd.today());
            user.save();
            // A first completion, Catacombs and class levels: SkyBlock XP.
            SkyBlockLevels.changed(player);
        }
        for (String line : RunEnd.summary(floor, chatScore, millis, outcome, failed)) {
            if (!line.equals(RunEnd.EXTRA_STATS)) player.sendMessage(Utils.color(line));
            else player.sendMessage(legacy(line).clickEvent(ClickEvent.runCommand("/showextrastats"))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to view extra stats!", NamedTextColor.YELLOW))));
        }
    }

    /**
     * A member back (from a disconnect) after the end, before the close: their summary and rewards
     * if they missed them, and the score card once it's out.
     */
    private void backAfterTheEnd(Player player) {
        rewardAndSummarize(player, players());
        if (cardScore != null) giveMap(player, "&a&lYour Score Summary", List.of());
    }

    /** Half a second after the summary: the Blood Room counts, and the map shows the score with it. */
    private void showScoreCard() {
        PlacedRoom blood = layout.bloodRoom();
        if (watcherPassed && blood != null) runMap.complete(blood);
        cardScore = score(endedAt);
        ScoreCard.draw(map, floor, cardScore);
    }

    /**
     * Everyone left is a ghost, or it has gone on for an hour (see {@link DeathRules#failed}): it ends
     * as a failed run, its score cut by 30%.
     */
    private void fail() {
        failed = true;
        end();
    }

    /** Whether it ended without the boss beaten. */
    public boolean failed() {
        return failed;
    }

    private void requeueMessage() {
        tell("");
        tell(RunText.RULE);
        // "§c§a" as recorded.
        tell(legacy("      &7Click &e&lHERE &7to re-queue into &c&a" + floor.getDungeonName() + "&7!")
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
                RunText.centered("Team Score: &a" + chatScore.total() + " &f(" + Score.gradeColor(chatScore.grade()) + chatScore.grade() + "&f)"),
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
        roomMobs.dispose();
        ghosts.dispose();
        classes.dispose();
        if (fairies != null) fairies.dispose();
        if (watcher != null) watcher.dispose();
        if (secrets != null) secrets.dispose();
        if (cases != null) cases.dispose();
        puzzleRooms.dispose();
        for (Player player : players()) takeRunItems(player);
        ScoreCard.blank(map);
    }

    // Stats

    public boolean isStarted() {
        return phase == Phase.RUNNING || phase == Phase.ENDED;
    }

    /** How long it has been going, as its Time Elapsed shows it: 0 before the start, up to its end once it's over. */
    public long elapsedMillis() {
        if (!isStarted()) return 0;
        return (phase == Phase.ENDED ? endedAt : System.currentTimeMillis()) - startedAt;
    }

    /** For EXTRA STATS and the tab list: how much damage a member has dealt, and how many kills. */
    public void damageDealt(UUID member, double damage) {
        Member m = members.get(member);
        if (m == null || phase != Phase.RUNNING) return;
        m.damage += damage;
        classes.hit(member, damage);
    }

    public void killed(UUID member) {
        Member m = members.get(member);
        if (m == null || phase != Phase.RUNNING) return;
        m.kills++;
        classes.kill(member);
    }

    /** For the tab list's "Team Healing Done" and EXTRA STATS' "Ally Healing": a member healed others for this much. */
    public void healedAllies(UUID member, double healing) {
        Member m = members.get(member);
        if (m != null && phase == Phase.RUNNING && healing > 0) m.healing += healing;
    }

    // For items' abilities (item/ability/utility)

    /** A member as a menu of teammates shows them: their name, "§b[MVP§6+§b] Name", and whether they're a ghost now. */
    public record Teammate(UUID id, String name, String display, boolean ghost) {
    }

    /** Everyone in the run but {@code viewer}, in the order they joined it. */
    public List<Teammate> teammates(UUID viewer) {
        List<Teammate> out = new ArrayList<>();
        for (Member m : members.values()) {
            if (!m.id.equals(viewer)) out.add(new Teammate(m.id, m.name, m.display(), ghosts.isGhost(m.id)));
        }
        return out;
    }

    /** Whether this member is a ghost now: dead in the run, and not to be healed, buffed or leapt to. */
    public boolean isGhost(UUID member) {
        return ghosts.isGhost(member);
    }

    /** Whether this player is a member here now, in this run's world (a Spirit Leap goes to them). */
    public boolean isHere(Player player) {
        return players().contains(player);
    }

    // Ghosts, fairies and classes

    Ghosts ghosts() {
        return ghosts;
    }

    RunClasses classes() {
        return classes;
    }

    boolean isFairy(org.bukkit.entity.Entity entity) {
        return fairies != null && fairies.is(entity);
    }

    void fairyKilled(org.bukkit.entity.Entity entity, Player killer) {
        if (fairies != null) fairies.killed(entity, killer);
    }

    void died(UUID member) {
        Member m = members.get(member);
        if (m != null && phase == Phase.RUNNING) m.deaths++;
    }

    /** Deaths so far, everyone's (for the score and the tab list). */
    public int deaths() {
        return members.values().stream().mapToInt(m -> m.deaths).sum();
    }

    /** The score as it stands (rooms done, secrets, puzzles, crypts, deaths and time). */
    Score score(long now) {
        Score score = Score.of(floor, scoreInputs(now));
        return failed ? DeathRules.failedScore(score, floor, runMap.completedCells(), runMap.totalCells()) : score;
    }

    /**
     * What the score is worked out from, now: rooms are cells (the Entrance room's not counted), and
     * the rest is {@link ScoreCounts}'. No pets (the Spirit pet's death), Mimic (Floor VI and up) or
     * mayors (Paul) here.
     */
    private Score.Inputs scoreInputs(long now) {
        double seconds = startedAt == 0 ? 0 : (now - startedAt) / 1000.0;
        return new Score.Inputs(runMap.completedCells(), runMap.totalCells(), secretsFound(), totalSecrets(), deaths(), false,
                puzzlesNotDone(), cryptsBlown(), false, false, seconds);
    }

    /** The sidebar's "(N)" when it next updates: the in-run indicator, or the score card's total after the end. */
    private int sidebarScore() {
        if (phase == Phase.ENDED) return (cardScore != null ? cardScore : chatScore).total();
        return Score.indicator(floor, scoreInputs(System.currentTimeMillis()), watcher == null ? 0 : watcher.killed());
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
            lines.add(dateLine);
            lines.add("");
            lines.add(season);
            lines.add(time);
            lines.add(where);
            lines.add("");
            lines.add("&fKeys: &c■ " + (doors.hasBloodKey() ? "&a✓" : "&c✗") + " &8■ &a" + doors.witherKeys() + "x");
            lines.add("&fTime Elapsed: &a" + RunText.elapsed(now - startedAt));
            int cleared = cleared();
            lines.add("&fCleared: " + Score.clearedColor(cleared) + cleared + "% &8(" + sidebarScore.shown() + ")");
            lines.add("");
            List<Member> others = members.values().stream().filter(m -> !m.id.equals(viewer.getUniqueId())).toList();
            if (others.isEmpty()) lines.add("&3&lSolo");
            for (Member m : others) {
                Player p = Bukkit.getPlayer(m.id);
                int health = p == null ? 0 : (int) PlayerHealth.get(p);
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
                // A ghost's is "DEAD" (Skytils' tab pattern).
                String what = !started ? "&7EMPTY" : ghosts.isGhost(m.id) ? "&cDEAD"
                        : "&d" + classOf(m.id).getDisplayName() + " " + DungeonLevels.roman(classLevel(m.id));
                party.add(new TabEntry("&8[" + skyBlockLevel(m.id) + "&8] " + m.rankColor + m.name + " &f(" + what + "&f)", m.id));
                party.add(new TabEntry(" Ultimate: " + (started ? classes.ultimateTab(m.id) : "&cN/A"), null));
                Player online = Bukkit.getPlayer(m.id);
                party.add(new TabEntry(" Revive Stones: &c" + (online == null ? 0 : ReviveStones.count(online)), null));
                party.add(new TabEntry("", null));
            }
            return party;
        });

        int deaths = members.values().stream().mapToInt(m -> m.deaths).sum();
        double damage = members.values().stream().mapToDouble(m -> m.damage).sum();
        double healing = members.values().stream().mapToDouble(m -> m.healing).sum();
        int secrets = members.values().stream().mapToInt(m -> m.secrets).sum();
        List<String> downed = ghosts.tabLines();
        column(out, "       &2&lPlayer Stats", () -> texts(
                downed.get(0),
                downed.get(1),
                downed.get(2),
                "",
                "&a&lTeam Deaths: &f" + deaths,
                " Team Damage Dealt: &c" + RunText.compact(damage) + "❤",
                " Team Healing Done: &c" + RunText.compact(healing) + "❤",
                " Your Milestone: &e?",
                "",
                "&a&lDiscoveries: &f" + (secrets + cryptsBlown()),
                " Secrets Found: &b" + secrets,
                " Crypts: &6" + cryptsBlown()));

        long now = phase == Phase.ENDED ? endedAt : System.currentTimeMillis();
        column(out, "       &3&lDungeon Stats", () -> {
            List<TabEntry> stats = texts(
                    "&b&lDungeon: &7Catacombs",
                    " Opened Rooms: &5" + runMap.openedCells(doors::isShut),
                    " Completed Rooms: &d" + runMap.completedCells(),
                    " Secrets Found: &e" + SecretText.percent(secretsFound(), totalSecrets()) + "%",
                    " Time: &6" + (started ? RunText.elapsed(now - startedAt) : "Soon!"),
                    "",
                    "&b&lPuzzles: &f(" + puzzles + ")");
            for (String row : puzzleRooms.tabRows()) stats.add(new TabEntry(row, null));
            return stats;
        });

        User user = User.cached(viewer.getUniqueId());
        PlayerSession session = PlayerSession.of(viewer);
        Stats stats = session.stats();
        // The skill that last gained XP (Combat before any has).
        Skill skill = session.getLastSkill() == null ? Skill.COMBAT : session.getLastSkill();
        column(out, "       &6&lAccount Info", () -> texts(
                "&e&lProfile: &a" + (user != null && user.isLoaded() ? user.profileName() : "?"),
                " Bank: &6" + (user != null && user.isLoaded() ? Utils.formatNumber(user.getBankBalance()) : "0"),
                "",
                SkillText.dungeonTab(skill, user != null && user.isLoaded() ? Skills.xp(user.profile(), skill) : 0),
                " Speed: &f" + (stats == null ? 0 : (int) stats.get(Stat.SPEED)),
                " Strength: &c" + (stats == null ? 0 : (int) stats.get(Stat.STRENGTH)),
                " Crit Chance: &9" + (stats == null ? 0 : (int) stats.get(Stat.CRIT_CHANCE)),
                " Crit Damage: &9" + (stats == null ? 0 : (int) stats.get(Stat.CRIT_DAMAGE)),
                " Attack Speed: &e" + (stats == null ? 0 : (int) stats.get(Stat.ATTACK_SPEED))));
        return out;
    }

    /** How much of the floor is done, in percent of its cells (the Entrance room's not counted). */
    private int cleared() {
        return Score.cleared(runMap.completedCells(), runMap.totalCells());
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

    /** "&e88": the SkyBlock level in its colour (see {@link SkyBlockXp}). */
    private static String skyBlockLevel(UUID id) {
        User user = User.cached(id);
        int level = user == null ? 0 : SkyBlockXp.level(user.getSkyBlockXp());
        return SkyBlockXp.color(level) + level;
    }
}
