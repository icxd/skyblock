package net.icxd.dungeons.mob;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.mob.mobs.Bladesoul;
import net.icxd.dungeons.mob.mobs.MagmaCube;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SkyBlock's mobs: which there are, the ones alive now, and how they fight. A mob's health is
 * SkyBlock health, kept here; hits on it do no vanilla damage (it still flinches and takes
 * knockback), and its hits on players do SkyBlock damage less their defense. Its name tag is a text
 * display riding it. Mobs aren't saved with the world: a restart clears them. Main thread.
 */
public final class Mobs implements Listener {
    /** On every spawned mob's entity: which mob it is. */
    public static final NamespacedKey TYPE = new NamespacedKey("skyblock", "mob");

    private static final Map<String, SkyBlockMob> REGISTRY = new LinkedHashMap<>();
    private static final Map<UUID, Live> LIVE = new HashMap<>();

    static {
        for (SkyBlockMob mob : List.of(new MagmaCube(), new Bladesoul())) REGISTRY.put(mob.getId(), mob);
    }

    /** A spawned mob: its entity, its health, its name tag and whatever rides it. */
    public static final class Live {
        private final SkyBlockMob type;
        private final LivingEntity entity;
        private double health;
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
    }

    public static SkyBlockMob get(String id) {
        return id == null ? null : REGISTRY.get(id.toUpperCase());
    }

    public static Map<String, SkyBlockMob> registry() {
        return java.util.Collections.unmodifiableMap(REGISTRY);
    }

    /** The live mob an entity is, or null. */
    public static Live of(Entity entity) {
        return entity == null ? null : LIVE.get(entity.getUniqueId());
    }

    public static Live spawn(SkyBlockMob type, Location location) {
        LivingEntity entity = (LivingEntity) location.getWorld().spawn(location, type.getEntityType().getEntityClass(), spawned -> {
            spawned.setPersistent(false);
            spawned.getPersistentDataContainer().set(TYPE, PersistentDataType.STRING, type.getId());
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

    /** "[Lv75] Magma Cube 1M/1M❤", and a boss's framed in ﴾ ﴿. */
    static String nameTag(SkyBlockMob type, double health) {
        String tag = "&8[&7Lv" + type.getLevel() + "&8] &c" + type.getName() + " &a" + Utils.formatNumber(Math.max(0, Math.ceil(health)))
                + "&f/&a" + Utils.formatNumber(type.getMaxHealth()) + "&c❤";
        return type.isBoss() ? "&e﴾ " + tag + " &e﴿" : tag;
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
        DungeonMobs.showDamage(live.entity, damage, critical);
        if (live.health <= 0) {
            event.setCancelled(true);
            die(live, player);
            return;
        }
        live.type.onDamaged(live.entity, player, damage);
        updateNameTag(live);
    }

    private static void die(Live live, Player killer) {
        live.type.onDeath(live.entity, killer);
        if (killer != null && !live.type.isBoss()) drop(live, killer);
        remove(live);
    }

    /** Each drop rolls on its own (magic find raises the chance); only what drops is announced. */
    private static void drop(Live live, Player killer) {
        double magicFind = net.icxd.dungeons.session.PlayerSession.of(killer).stats().get(net.icxd.dungeons.stats.Stat.MAGIC_FIND);
        for (MobDrop drop : live.type.getDrops()) {
            SkyBlockItem item = drop.item();
            if (item == null || Math.random() >= drop.chance() / 100 * (1 + magicFind / 100)) continue;
            ItemStack stack = ItemBuilder.build(item, Utils.random(drop.min(), drop.max()));
            live.entity.getWorld().dropItemNaturally(live.entity.getLocation(), stack);
            String kind = drop.type() == MobDropType.RNGESUS_INCARNATE ? "INSANE DROP! "
                    : (drop.type() == MobDropType.CRAZY_RARE ? "CRAZY " : "") + "RARE DROP! ";
            killer.sendMessage(Utils.color("§" + drop.type().getColor() + "§l" + kind + item.rarity().getColor() + item.name()
                    + " &b(+" + Utils.round(magicFind, 0) + "% ✯ Magic Find)"));
        }
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

    /** Hits on our mobs that PlayerListener didn't deal with (fists, other items) do fist damage; nothing else hurts them. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHurt(EntityDamageEvent event) {
        Live live = of(event.getEntity());
        if (live == null || event.getDamage() == 0) return;
        if (event instanceof EntityDamageByEntityEvent hit) {
            Player player = hit.getDamager() instanceof Player p ? p
                    : hit.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p ? p : null;
            if (player != null && !event.isCancelled()) {
                playerHit(hit, player, live, DungeonMobs.fistDamage(player), false);
                return;
            }
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.KILL) event.setCancelled(true);
    }

    /** Their hits (and their projectiles') on players do SkyBlock damage. */
    @EventHandler(priority = EventPriority.LOW)
    public void onAttack(EntityDamageByEntityEvent event) {
        Live live = attacker(event.getDamager());
        if (live == null || !(event.getEntity() instanceof Player player)) return;
        event.setCancelled(true);
        if (live.type.getDamage() > 0) DungeonMobs.hit(player, live.type.getDamage(), live.entity);
        live.type.onAttack(live.entity, player);
    }

    /** Killed some other way (/kill): gone, with no vanilla drops. */
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Live live = of(event.getEntity());
        if (live == null) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        remove(live);
    }

    /** Their wither skulls don't blow up the world. */
    @EventHandler
    public void onExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if (attacker(event.getEntity()) != null) event.blockList().clear();
    }

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        Live live = of(event.getEntity());
        if (live != null && event.getCause() != EntityRemoveEvent.Cause.PLUGIN) remove(live);
    }
}
