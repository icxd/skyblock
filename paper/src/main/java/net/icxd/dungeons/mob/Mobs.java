package net.icxd.dungeons.mob;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SkyBlock's mobs: the ones alive now, and how they fight. Every kind ({@link MobKinds}) is spawned
 * here, as a {@link DataMob}, and shares this one path: hits, damage numbers, drops, death (a
 * {@link SkyBlockMobDeathEvent} after the drops) and removal. A mob's health is SkyBlock health,
 * kept here; hits on it do no vanilla damage (it still flinches and takes knockback), and its hits on
 * players do SkyBlock damage less their defense. Its name tag is a text display riding it. Mobs
 * aren't saved with the world: a restart clears them. Main thread.
 */
public final class Mobs implements Listener {
    /** On every spawned mob's entity: which mob it is. */
    public static final NamespacedKey TYPE = new NamespacedKey("skyblock", "mob");

    private static final Map<UUID, Live> LIVE = new HashMap<>();

    /** A spawned mob: its entity, its health, its name tag and whatever rides it. */
    public static final class Live {
        private final SkyBlockMob type;
        private final LivingEntity entity;
        private double health;
        /** Hits it has taken from players (First Strike and Triple-Strike count them). */
        private int hits;
        private TextDisplay nameTag;
        private LivingEntity passenger;
        private String shownName;

        private Live(SkyBlockMob type, LivingEntity entity) {
            this.type = type;
            this.entity = entity;
            this.health = type.getMaxHealth();
        }

        public SkyBlockMob type() {
            return type;
        }

        public LivingEntity entity() {
            return entity;
        }

        public double health() {
            return health;
        }

        /** What a hit on it is worked out against. */
        public Damage.Target target() {
            return new Damage.Target(health, type.getMaxHealth(), type.getDefense(), type.getMagicResistance(), type.getTypes(), hits);
        }
    }

    /** The kind with this id ("zombie_grunt" too); null for none. */
    public static MobKind kind(String id) {
        return MobKinds.get(id);
    }

    /** A mob of this kind as it first spawns (the Entrance's lowest level, for a dungeon kind); null for no such kind. */
    public static DataMob get(String id) {
        MobKind kind = kind(id);
        return kind == null ? null : new DataMob(kind, kind.firstVariant(), SpawnOptions.NONE);
    }

    /** Every kind, by id. */
    public static Map<String, MobKind> registry() {
        return MobKinds.all();
    }

    /** The live mob an entity is, or null. */
    public static Live of(Entity entity) {
        return entity == null ? null : LIVE.get(entity.getUniqueId());
    }

    /**
     * A mob of this kind on this floor (null outside the dungeons), at the level {@code options} asks for
     * (the floor's first if none), starred or with a modifier as it says, times the room's multiplier.
     *
     * @throws IllegalArgumentException if the kind has no such variant there
     */
    public static Live spawn(MobKind kind, DungeonFloor floor, SpawnOptions options, Location location) {
        MobKind.Variant variant = kind.variant(floor, options.level());
        if (variant == null) {
            throw new IllegalArgumentException(kind.id() + " has no " + (options.level() == null ? "" : "Lv" + options.level() + " ")
                    + "variant " + (floor == null ? "outside the dungeons" : "on " + floor.getName()));
        }
        return spawn(new DataMob(kind, variant, options), location);
    }

    /**
     * A mob of this type, as it is: without vanilla's spawn randomness (a zombie's baby or chicken jockey,
     * gear, bonus follow range).
     */
    public static Live spawn(SkyBlockMob type, Location location) {
        LivingEntity entity = (LivingEntity) location.getWorld().spawn(location, type.getEntityType().getEntityClass(), false, spawned -> {
            spawned.setPersistent(false);
            spawned.getPersistentDataContainer().set(TYPE, PersistentDataType.STRING, type.getId());
            if (spawned instanceof LivingEntity living) type.beforeSpawn(living);
        });
        entity.setRemoveWhenFarAway(false);
        entity.setCanPickupItems(false);
        entity.setInvisible(type.isInvisible());
        if (type.isUpsideDown()) {
            entity.customName(Text.line("Dinnerbone"));
            entity.setCustomNameVisible(false);
        }
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) {
            equipment.setItemInMainHand(type.getItemInHand());
            equipment.setHelmet(type.getHelmet());
            equipment.setChestplate(type.getChestplate());
            equipment.setLeggings(type.getLeggings());
            equipment.setBoots(type.getBoots());
            equipment.setItemInMainHandDropChance(0);
            equipment.setHelmetDropChance(0);
            equipment.setChestplateDropChance(0);
            equipment.setLeggingsDropChance(0);
            equipment.setBootsDropChance(0);
        }
        Live live = new Live(type, entity);
        LIVE.put(entity.getUniqueId(), live);
        if (type.getPassenger() != null) live.passenger = spawn(type.getPassenger(), location).entity;
        // The name tag rides first: a mob's first passenger steers it if it's a mob itself (as a
        // skeleton does a spider), and a text display can't.
        if (type.hasNameTag()) {
            live.nameTag = location.getWorld().spawn(location, TextDisplay.class, display -> {
                display.setPersistent(false);
                display.setBillboard(Display.Billboard.CENTER);
                display.setShadowed(true);
                display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
                // Above the head (or the rider's), where name tags go.
                float up = (float) (live.passenger != null ? live.passenger.getHeight() : 0) + 0.35f;
                display.setTransformation(new Transformation(new Vector3f(0, up, 0), new org.joml.Quaternionf(),
                        new Vector3f(1, 1, 1), new org.joml.Quaternionf()));
            });
            entity.addPassenger(live.nameTag);
            updateNameTag(live);
        }
        if (live.passenger != null) entity.addPassenger(live.passenger);
        type.onSpawn(entity);
        return live;
    }

    /** "[Lv75] Magma Cube 1M/1M❤" in the Hub, "༕ ✯ Zombie Grunt 7,000❤" in a dungeon (see {@link NameTags}). */
    static String nameTag(SkyBlockMob type, double health) {
        return type.nameTag(health);
    }

    /** It gets health back, never more than its max. */
    public static void heal(Live live, double amount) {
        if (amount <= 0 || live.health <= 0) return;
        live.health = Math.min(live.type.getMaxHealth(), live.health + amount);
        updateNameTag(live);
    }

    private static void updateNameTag(Live live) {
        if (live.nameTag == null) return;
        String name = nameTag(live.type, live.health);
        if (name.equals(live.shownName)) return;
        live.shownName = name;
        live.nameTag.text(Text.line(name));
    }

    /**
     * A player hit a mob for this much SkyBlock damage: it loses that much health (or dies, dropping
     * what it drops for them), and the hit itself goes through with no vanilla damage.
     */
    public static void playerHit(EntityDamageByEntityEvent event, Player player, Live live, double damage, boolean critical) {
        if (live.type.isInvulnerable()) {
            event.setCancelled(true);
            return;
        }
        event.setDamage(0);
        live.health -= damage;
        live.hits++;
        DamageIndicators.show(live.entity, damage, critical);
        DungeonRun run = RunManager.of(player);
        if (run != null) run.damageDealt(player.getUniqueId(), damage);
        if (live.health <= 0) {
            event.setCancelled(true);
            die(live, player);
            return;
        }
        live.type.onDamaged(live.entity, player, damage);
        updateNameTag(live);
    }

    private static void die(Live live, Player killer) {
        DungeonRun run = killer == null ? null : RunManager.of(killer);
        if (run != null) run.killed(killer.getUniqueId());
        live.type.onDeath(live.entity, killer);
        if (killer != null && !live.type.isBoss()) drop(live, killer);
        Location at = live.entity.getLocation();
        remove(live);
        died(live, killer, at);
    }

    /** Tells the rest of the plugin (Combat XP, coins, the room's starred mobs), once its drops are out. */
    private static void died(Live live, Player killer, Location at) {
        if (live.type instanceof DataMob mob) Bukkit.getPluginManager().callEvent(new SkyBlockMobDeathEvent(killer, mob, at));
    }

    /**
     * Each drop rolls on its own (magic find raises the chance). A dungeon mob's go straight into the
     * killer's inventory, as recorded on Hypixel (on the ground where it died if there's no room), and
     * only the rare ones are announced (the recorded 5% armor drops had no chat line); other mobs' land
     * on the ground, and each is announced.
     */
    private static void drop(Live live, Player killer) {
        double magicFind = PlayerSession.of(killer).stats().get(Stat.MAGIC_FIND);
        boolean toInventory = live.type.dropsToInventory() && !InventorySyncListener.frozen(killer);
        for (MobDrop drop : live.type.getDrops()) {
            SkyBlockItem item = drop.item();
            if (item == null || Math.random() >= drop.chance() / 100 * (1 + magicFind / 100)) continue;
            ItemStack stack = ItemBuilder.build(item, Utils.random(drop.min(), drop.max()));
            Location at = live.entity.getLocation();
            if (toInventory) {
                for (ItemStack left : killer.getInventory().addItem(stack).values()) at.getWorld().dropItemNaturally(at, left);
            } else {
                at.getWorld().dropItemNaturally(at, stack);
            }
            if (!announced(live.type, drop.type())) continue;
            String kind = drop.type() == MobDropType.RNGESUS_INCARNATE ? "INSANE DROP! "
                    : (drop.type() == MobDropType.CRAZY_RARE ? "CRAZY " : "") + "RARE DROP! ";
            killer.sendMessage(Utils.color("§" + drop.type().getColor() + "§l" + kind + item.rarity().getColor() + item.name()
                    + " &b(+" + Utils.round(magicFind, 0) + "% ✯ Magic Find)"));
        }
    }

    /** Whether a drop this rare from this mob gets a "RARE DROP!" line: a dungeon mob's only past Occasional. */
    static boolean announced(SkyBlockMob type, MobDropType drop) {
        return !type.dropsToInventory() || drop.ordinal() > MobDropType.OCCASIONAL.ordinal();
    }

    /** It, its name tag and its passenger, gone. */
    public static void remove(Live live) {
        LIVE.remove(live.entity.getUniqueId());
        if (live.nameTag != null) live.nameTag.remove();
        if (live.passenger != null) {
            Live passenger = LIVE.remove(live.passenger.getUniqueId());
            if (passenger != null && passenger.nameTag != null) passenger.nameTag.remove();
            live.passenger.remove();
        }
        live.entity.remove();
    }

    /** Every tick: each mob's own behaviour; mobs whose entity has gone are forgotten. */
    public static void tick() {
        for (Live live : new ArrayList<>(LIVE.values())) {
            if (!live.entity.isValid()) {
                if (live.entity.isDead() || !live.entity.getLocation().isChunkLoaded()) remove(live);
                continue;
            }
            live.type.onTick(live.entity);
        }
    }

    public static void start() {
        Bukkit.getScheduler().runTaskTimer(Dungeons.getInstance(), Mobs::tick, 1, 1);
    }

    /** The mob behind a hit: the attacker itself, or whoever shot the projectile. */
    private static Live attacker(Entity damager) {
        Live live = of(damager);
        if (live == null && damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) live = of(shooter);
        return live;
    }

    /** Only players hurt our mobs (their hits are {@link Combat}'s, through PlayerListener), and /kill. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHurt(EntityDamageEvent event) {
        if (of(event.getEntity()) == null) return;
        if (event instanceof EntityDamageByEntityEvent hit && Combat.playerBehind(hit.getDamager()) != null) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.KILL) event.setCancelled(true);
    }

    /**
     * A mob's hit on a player, by itself or with a projectile ({@code by}): its damage, less their
     * Defense, and whatever its hits do (a Flaming mob's melee sets them on fire).
     */
    public static void mobHit(Live live, Player player, Entity by) {
        if (live.type.getDamage() > 0) PlayerDamage.hit(player, live.type.getDamage(), PlayerDamage.Kind.NORMAL, by, live.type.getKnockback());
        live.type.onHit(live.entity, player, !(by instanceof Projectile));
    }

    /**
     * Their hits (and their projectiles') on players do SkyBlock damage. The vanilla hit is cancelled, so
     * an arrow would bounce off and lie there for a minute: it goes instead.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onAttack(EntityDamageByEntityEvent event) {
        Live live = attacker(event.getDamager());
        if (live == null || !(event.getEntity() instanceof Player player)) return;
        event.setCancelled(true);
        if (event.getDamager() instanceof AbstractArrow arrow) arrow.remove();
        // A thrown bone (a snowball) hits in onProjectileHit: snowballs don't always get this far.
        if (event.getDamager() instanceof Snowball) return;
        mobHit(live, player, event.getDamager());
    }

    /**
     * Their arrows that miss go when they land, rather than a minute later: a Skeleton Grunt shoots
     * twice a second, and a fight would leave hundreds lying in the room.
     */
    @EventHandler
    public void onArrowLand(ProjectileHitEvent event) {
        if (event.getHitBlock() != null && event.getEntity() instanceof AbstractArrow arrow && attacker(arrow) != null) arrow.remove();
    }

    /** A Crypt Lurker's bone (a snowball that looks like one) hits whoever it lands on. */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball bone) || !(event.getHitEntity() instanceof Player player)) return;
        Live live = attacker(bone);
        if (live != null) mobHit(live, player, bone);
    }

    /** Their wither skulls hit, and don't blow up as well (the blast would hit again). */
    @EventHandler
    public void onPrime(org.bukkit.event.entity.ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof Projectile && attacker(event.getEntity()) != null) event.setCancelled(true);
    }

    /** Killed some other way (/kill): gone, with no vanilla drops. */
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Live live = of(event.getEntity());
        if (live == null) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        remove(live);
        died(live, null, event.getEntity().getLocation());
    }

    /** Their wither skulls don't blow up the world. */
    @EventHandler
    public void onExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if (attacker(event.getEntity()) != null) event.blockList().clear();
    }

    /** Vanilla doesn't turn them into anything else (a zombie drowning into a Drowned, a skeleton freezing into a Stray). */
    @EventHandler
    public void onTransform(EntityTransformEvent event) {
        if (of(event.getEntity()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        Live live = of(event.getEntity());
        if (live != null && event.getCause() != EntityRemoveEvent.Cause.PLUGIN) remove(live);
    }
}
