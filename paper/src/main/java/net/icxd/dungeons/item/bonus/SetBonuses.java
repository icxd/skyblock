package net.icxd.dungeons.item.bonus;

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.ability.WornAbilities;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.stats.PlayerAttributes;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;

/**
 * Armor set, tiered, piece and extra bonuses (BONUSES.md): what a player wears ({@link Worn}), which
 * {@link Bonus}es that makes count, and what those do, through the plugin's hooks: stats
 * ({@link PlayerStats#addModifier}), their hits ({@link Combat#addHitBuffs}), hits on them
 * ({@link PlayerDamage}), their Speed cap and their abilities' mana costs; and kills, teleports,
 * sneaking and a tick a second here. What they wear is worked out at most once a tick. Bonuses that
 * are only text (their systems aren't here yet) still show how many of their set is worn (see
 * {@link SetBonusLore}). Main thread.
 */
public final class SetBonuses implements Listener {
    /** Bonuses by their blocks' kind and name (not the ones in items' own text, found by item id). */
    private static final Map<String, Bonus> BY_BLOCK = new HashMap<>();
    private static final List<Bonus> ALL = new ArrayList<>();
    private static final Map<UUID, Now> NOW = new HashMap<>();
    /** What counted at the last second's tick, so a bonus that stops counting can end what it started (see {@link Bonus#ended}). */
    private static final Map<UUID, Set<Bonus>> LAST_SECOND = new HashMap<>();
    private static final Set<UUID> REFRESHING = new HashSet<>();
    private static final List<Consumer<Player>> TELEPORTED = new ArrayList<>();
    private static Function<Player, List<ItemStack>> equipment = player -> List.of();
    private static ToDoubleBiFunction<Player, String> collections = (player, item) -> 0;
    private static Plugin plugin;

    /** What a player wears and what counts of it, at a tick. */
    private record Now(int tick, Worn worn, List<Bonus.Active> active) {
    }

    static {
        for (Bonus bonus : DungeonSets.all()) add(bonus);
        for (Bonus bonus : DragonSets.all()) add(bonus);
        for (Bonus bonus : SlayerSets.all()) add(bonus);
        for (Bonus bonus : OtherSets.all()) add(bonus);
        for (Bonus bonus : TieredSets.all()) add(bonus);
        // Worn pieces' own passive abilities (item/ability, ABILITIES_UTILITY.md and ABILITIES_WEAPONS.md).
        for (Bonus bonus : WornAbilities.all()) add(bonus);
    }

    private static void add(Bonus bonus) {
        ALL.add(bonus);
        if (!Bonus.ITEM.equals(bonus.kind()) && BY_BLOCK.putIfAbsent(key(bonus.kind(), bonus.name()), bonus) != null) {
            throw new IllegalStateException("Two bonuses for " + bonus.kind() + " " + bonus.name());
        }
    }

    private static String key(String kind, String name) {
        return kind + ":" + name;
    }

    /** The bonus that does what blocks of this kind and name say; null if they're only text. */
    static Bonus bonus(String kind, String name) {
        return BY_BLOCK.get(key(kind, name));
    }

    /** Every bonus that does something. */
    static List<Bonus> all() {
        return List.copyOf(ALL);
    }

    /** How many pieces of this set it takes to count: its bonus's say, a full set's all, a tiered one's 1 (UNKNOWN where the wiki doesn't say). */
    static int needs(Bonus bonus, SetKey set) {
        if (bonus != null) return bonus.needs(set);
        return set.tiered() || set.pieces() <= 0 ? 1 : set.pieces();
    }

    /** Registers the hooks, the listener and the second's tick, once, as the plugin starts. */
    public static void enable(Plugin plugin) {
        SetBonuses.plugin = plugin;
        PlayerStats.addModifier(SetBonuses::stats);
        Combat.addHitBuffs((player, attacker, target) -> {
            double additive = 0;
            double multiplier = 1;
            double added = 0;
            for (Bonus.Active a : active(player)) {
                Combat.HitBuff buff = a.bonus().hit(player, a, attacker, target);
                if (buff == null) continue;
                additive += buff.additive();
                multiplier *= buff.multiplier();
                added += buff.added();
            }
            return new Combat.HitBuff(additive, multiplier, added);
        });
        PlayerDamage.addDefenseAgainst((player, by) -> {
            double defense = 0;
            for (Bonus.Active a : active(player)) defense += a.bonus().defenseAgainst(player, a, by);
            return defense;
        });
        PlayerDamage.addTakenFrom((player, by) -> {
            double factor = 1;
            for (Bonus.Active a : active(player)) factor *= a.bonus().takenFrom(player, a, by);
            return factor;
        });
        // Every hit, a trap's (nothing hit them) too: "players within 10 blocks of you take 5% less damage".
        PlayerDamage.addTakenMultiplier(SetBonuses::takenNear);
        PlayerDamage.addKnockbackResistance((player, by) -> {
            double share = 0;
            for (Bonus.Active a : active(player)) share += a.bonus().knockbackResistance(player, a, by);
            return share;
        });
        PlayerDamage.addHurtListener((player, by, kind, taken) -> {
            for (Bonus.Active a : active(player)) a.bonus().hurt(player, a, by, taken);
        });
        PlayerAttributes.addSpeedCap(player -> {
            double raise = 0;
            for (Bonus.Active a : active(player)) raise = Math.max(raise, a.bonus().speedCap(player, a));
            return raise;
        });
        Abilities.addManaCostFactor(player -> {
            double factor = 1;
            for (Bonus.Active a : active(player)) factor *= a.bonus().manaCost(player, a);
            return factor;
        });
        plugin.getServer().getPluginManager().registerEvents(new SetBonuses(), plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, SetBonuses::second, 20, 20);
    }

    /**
     * Their bonuses' stats: each one's own, then what's worked out from the rest, then what nearby wearers'
     * auras give; with the other modifiers, before a run's blessings (UNKNOWN, as PlayerStats has it).
     */
    private static void stats(Player player, Stats stats) {
        List<Bonus.Active> active = active(player);
        for (Bonus.Active a : active) a.bonus().stats(player, a, stats);
        for (Bonus.Active a : active) a.bonus().derivedStats(player, a, stats);
        for (Bonus bonus : ALL) {
            if (bonus.auraRange() > 0 && auraReaches(bonus, player)) bonus.aura(player, stats);
        }
    }

    /**
     * Whether their bonuses act: not the dead's, or dungeon ghosts' (who are invulnerable). The wiki's
     * Wither Armor has "Fixed the Witherborn full set bonus working as a ghost in Dungeons".
     */
    static boolean inPlay(Player player) {
        return !player.isDead() && !player.isInvulnerable();
    }

    /** The least factor on a hit on them that wearers' bonuses give (see {@link Bonus#takenNear}); not the dead's or ghosts'. */
    private static double takenNear(Player hurt) {
        double least = 1;
        for (Player wearer : hurt.getWorld().getPlayers()) {
            if (!inPlay(wearer)) continue;
            for (Bonus.Active a : active(wearer)) least = Math.min(least, a.bonus().takenNear(wearer, a, hurt));
        }
        return least;
    }

    /** Whether they, or someone near enough to them, wears this aura's bonus (not the dead or ghosts). */
    private static boolean auraReaches(Bonus bonus, Player player) {
        double range = bonus.auraRange();
        for (Player other : player.getWorld().getPlayers()) {
            if (!inPlay(other) || other.getLocation().distanceSquared(player.getLocation()) > range * range) continue;
            for (Bonus.Active a : active(other)) if (a.bonus() == bonus) return true;
        }
        return false;
    }

    // ---------- what's worn ----------

    /**
     * Where their equipment is (necklace, cloak, belt, gloves), once there are equipment slots: sets with
     * equipment in them (Arachne's, Mithril, the Shadow Assassin Cloak's bonus) count from there too, and
     * a change to it calls {@link #refreshLore}. Until then there's none.
     */
    public static void setEquipment(Function<Player, List<ItemStack>> source) {
        equipment = source;
    }

    /**
     * How many of an item they have in their collections (Emerald Armor and the Blaze sets count them),
     * once there are collections: 0 until then.
     */
    public static void setCollections(ToDoubleBiFunction<Player, String> amount) {
        collections = amount;
    }

    static double collection(Player player, String item) {
        return collections.applyAsDouble(player, item);
    }

    /** What they wear now: their armor, helmet first, then their equipment. */
    public static Worn worn(Player player) {
        return now(player).worn();
    }

    /** The bonuses that count for them now. */
    static List<Bonus.Active> active(Player player) {
        return now(player).active();
    }

    /**
     * Whether the bonus with this name counts for them now: for what other parts do with it (Superior
     * Blood's "Aspect of the Dragons ability deals 50% more damage", Trolling The Reaper's healing wands).
     */
    public static boolean active(Player player, String name) {
        for (Bonus.Active a : active(player)) if (a.bonus().name().equals(name)) return true;
        return false;
    }

    /**
     * Their Fervor stacks now (Fervor Armor's tiered bonus; 0 without 2 of its pieces on), for the Fervor
     * Chestplate's Ground Pound: "At 10 stacks, sneak to reset your stacks and perform a Ground Pound",
     * which then calls {@link #spendFervor}.
     */
    public static int fervor(Player player) {
        for (Bonus.Active a : active(player)) {
            if (a.bonus() instanceof TieredSets.Fervor fervor) return fervor.stacks(player.getUniqueId(), System.currentTimeMillis(), a.count());
        }
        return 0;
    }

    /** Their Fervor stacks are spent (reset to none). */
    public static void spendFervor(Player player) {
        for (Bonus bonus : ALL) if (bonus instanceof TieredSets.Fervor fervor) fervor.spend(player.getUniqueId());
    }

    private static Now now(Player player) {
        int tick = Bukkit.getCurrentTick();
        Now now = NOW.get(player.getUniqueId());
        if (now != null && now.tick() == tick) return now;
        Worn worn = wornNow(player);
        now = new Now(tick, worn, active(worn, ALL));
        if (player.isOnline()) NOW.put(player.getUniqueId(), now);
        return now;
    }

    /** Their armor (what's in an armor slot that is armor) and their equipment. */
    private static Worn wornNow(Player player) {
        List<Worn.Piece> pieces = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();
        for (ItemStack armor : new ItemStack[] {inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots()}) {
            Worn.Piece piece = piece(armor);
            // A head that isn't armor (an equipment piece) can sit on the head without being worn as armor.
            if (piece != null && piece.item().genericItemType() == GenericItemType.ARMOR) pieces.add(piece);
        }
        for (ItemStack stack : equipment.apply(player)) {
            Worn.Piece piece = piece(stack);
            if (piece != null) pieces.add(piece);
        }
        return pieces.isEmpty() ? Worn.NOTHING : new Worn(pieces);
    }

    private static Worn.Piece piece(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null) return null;
        return new Worn.Piece(item, tag, ItemBehaviours.of(item).blocks(item, tag, item.blocks()), stack);
    }

    /**
     * Of these bonuses, the ones that count for what's worn: a set's when enough of its set is (see
     * {@link Bonus#needs}), a piece's when a piece with it is, an item's when that item is; in the order
     * the bonuses are given.
     */
    static List<Bonus.Active> active(Worn worn, Collection<Bonus> bonuses) {
        List<Bonus.Active> active = new ArrayList<>();
        if (worn.pieces().isEmpty()) return active;
        for (Bonus bonus : bonuses) {
            switch (bonus.kind()) {
                case SetKey.FULL_SET, SetKey.TIERED -> {
                    for (SetKey set : worn.sets()) {
                        if (!set.kind().equals(bonus.kind()) || !set.name().equals(bonus.name())) continue;
                        int count = worn.count(set);
                        if (count >= bonus.needs(set)) active.add(new Bonus.Active(bonus, set, count, worn.in(set)));
                    }
                }
                case Bonus.ITEM -> {
                    List<Worn.Piece> pieces = worn.matching(bonus::item);
                    if (!pieces.isEmpty()) active.add(new Bonus.Active(bonus, null, pieces.size(), pieces));
                }
                default -> {
                    List<Worn.Piece> pieces = worn.with(bonus.kind(), bonus.name());
                    if (!pieces.isEmpty()) active.add(new Bonus.Active(bonus, null, pieces.size(), pieces));
                }
            }
        }
        return active;
    }

    /** The mob types of what hit them or what they hit (a projectile's shooter's): none for anything but SkyBlock's mobs. */
    static Set<MobType> types(Entity entity) {
        Entity mob = PlayerDamage.attacker(entity);
        Mobs.Live live = Mobs.of(mob);
        if (live != null) return live.type().getTypes();
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(mob);
        return dungeonMob == null ? Set.of() : dungeonMob.types();
    }

    /** The SkyBlock id of what they hold (upper case); null for nothing, or anything else. */
    static String held(Player player) {
        NBTTagCompound tag = ItemNBT.read(player.getInventory().getItemInMainHand());
        return tag == null ? null : tag.getString("id").toUpperCase(Locale.ROOT);
    }

    // ---------- what happens ----------

    /**
     * They teleported: an ability's teleport or an ender pearl (Shadow Assassin Armor's pieces act on
     * "Ender Pearls, ..., the Aspect of the End and Aspect of the Void, the Etherwarp Conduit, ... and the
     * Shadow Warp ability", the wiki's Shadow Assassin Armor). Abilities that teleport call it once
     * they have.
     */
    public static void teleported(Player player) {
        for (Bonus.Active a : active(player)) a.bonus().teleported(player, a);
        for (Consumer<Player> listener : TELEPORTED) listener.accept(player);
    }

    /** Adds what else happens when they teleport (see {@link #teleported}): the Hyper reforge's Speed (CombatReforges). */
    public static void addTeleportListener(Consumer<Player> listener) {
        TELEPORTED.add(listener);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPearl(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) teleported(event.getPlayer());
    }

    @EventHandler
    public void onKill(SkyBlockMobDeathEvent event) {
        Player killer = event.killer();
        if (killer == null || !killer.isOnline()) return;
        for (Bonus.Active a : active(killer)) a.bonus().killed(killer, a, event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        for (Bonus.Active a : active(event.getPlayer())) a.bonus().sneaked(event.getPlayer(), a, event.isSneaking());
    }

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        for (Bonus.Active a : active(player)) a.bonus().shot(player, a, event);
    }

    /** Before damage becomes SkyBlock health (see HealthListener), so what they're immune to never takes any. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHurt(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        for (Bonus.Active a : active(player)) {
            if (!a.bonus().immune(player, a, event.getCause())) continue;
            event.setCancelled(true);
            player.setFireTicks(0);
            return;
        }
    }

    /** Their armor changed: their items' set bonus counts are refreshed. */
    @EventHandler
    public void onEquipment(EntityEquipmentChangedEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        for (EquipmentSlot slot : event.getEquipmentChanges().keySet()) {
            if (slot.isArmor()) {
                refreshLore(player);
                return;
            }
        }
    }

    /**
     * What they wear changed: their items' set bonus counts are refreshed, once, when the tick's changes
     * are done (armor changes do it themselves; equipment slots, once there are some, call it).
     */
    public static void refreshLore(Player player) {
        if (plugin == null || !REFRESHING.add(player.getUniqueId())) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            REFRESHING.remove(player.getUniqueId());
            if (player.isOnline()) SetBonusLore.refresh(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        NOW.remove(id);
        LAST_SECOND.remove(id);
        REFRESHING.remove(id);
        for (Bonus bonus : ALL) bonus.forget(id);
    }

    /**
     * Every second: what counts does its second's work, and what stopped counting ends. The dead and
     * dungeon ghosts do no second's work, and what theirs started ends (see {@link #inPlay}).
     */
    private static void second() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Set<Bonus> counting = new LinkedHashSet<>();
            if (inPlay(player)) {
                for (Bonus.Active a : active(player)) {
                    counting.add(a.bonus());
                    a.bonus().second(player, a);
                }
            }
            Set<Bonus> before = LAST_SECOND.put(player.getUniqueId(), counting);
            if (before != null) for (Bonus bonus : before) if (!counting.contains(bonus)) bonus.ended(player);
        }
    }
}
