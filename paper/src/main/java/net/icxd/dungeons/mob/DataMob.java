package net.icxd.dungeons.mob;

import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.dungeons.instance.DungeonTextures;
import net.icxd.dungeons.session.PlayerHealth;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * One mob of a {@link MobKind}, at one of its {@link MobKind.Variant}s, spawned with some
 * {@link SpawnOptions}: its health and damage are the variant's times its modifier's and the room's
 * multipliers, it wears the kind's gear, and it fights as the kind's behaviour says. Spawn it with
 * {@link Mobs#spawn(MobKind, net.icxd.dungeons.common.DungeonFloor, SpawnOptions, org.bukkit.Location)}.
 */
public final class DataMob implements SkyBlockMob {
    /** Healing mobs "slowly regenerate health at all times" (the wiki): how fast is UNKNOWN; 1 a tick was recorded near one. */
    static final double HEALING_PER_SECOND = 20;
    /**
     * Stormy mobs "strike lightning on nearby players, dealing True Damage" (the wiki); how often, how near
     * and how much is UNKNOWN, so as the Unstable Lost Adventurer's lightning: 10% of max health as true
     * damage, here every 5 seconds to players within 5 blocks.
     */
    static final int STORM_EVERY = 100;
    static final double STORM_RANGE = 5;
    static final double STORM_SHARE = 0.1;
    /** Flaming mobs' hits set you on fire: for 4 seconds, as the recorded burn lasted (research damage.md 1.5). */
    static final int BURN_TICKS = 80;

    private final MobKind kind;
    private final MobKind.Variant variant;
    private SpawnOptions options;
    private final MobBehaviour behaviour;
    private double maxHealth;
    private double damage;
    private int age;
    /** Waiting for its room to open: it neither moves nor fights (see {@link #setDormant}). */
    private boolean dormant;

    public DataMob(MobKind kind, MobKind.Variant variant, SpawnOptions options) {
        this.kind = kind;
        this.variant = variant;
        this.options = options;
        this.behaviour = kind.behaviour().get();
        this.maxHealth = maxHealth(kind, variant, options);
        this.damage = damage(kind, variant, options);
    }

    /**
     * The variant's health times its modifier's (Healthy: x1.6) and the room's multiplier, rounded (the
     * recorded ones are whole: 7,000 x 1.05 = 7,350; 9,000 x 1.6 x 1.05 = 15,120).
     */
    public static double maxHealth(MobKind kind, MobKind.Variant variant, SpawnOptions options) {
        double modifier = options.modifier() == null ? 1 : options.modifier().healthMultiplier();
        return Math.round(variant.health() * modifier * room(kind, options));
    }

    /** The variant's damage times the room's multiplier (the staff quote says damage scales too; not seen in the recordings). */
    public static double damage(MobKind kind, MobKind.Variant variant, SpawnOptions options) {
        return variant.damage() * room(kind, options);
    }

    /** The room's multiplier, for a kind it applies to (1 for the rest). */
    private static double room(MobKind kind, SpawnOptions options) {
        return kind.roomScaled() ? options.roomMultiplier() : 1;
    }

    public MobKind kind() {
        return kind;
    }

    /** The room's health and damage multiplier it has now (1 until its room opens). */
    public double roomMultiplier() {
        return options.roomMultiplier();
    }

    /**
     * Its room opened: its health and damage are the variant's times this multiplier from now on (for a kind
     * it applies to). {@link Mobs#setRoomMultiplier} keeps its health's share of the max.
     */
    void roomMultiplier(double multiplier) {
        options = options.roomMultiplier(multiplier);
        maxHealth = maxHealth(kind, variant, options);
        damage = damage(kind, variant, options);
    }

    public boolean dormant() {
        return dormant;
    }

    /**
     * A room's mobs are there from the start of a run and wait for their room to open (research mobs.md
     * 1.2): until then it doesn't move, target or fight (whether Hypixel's go for players through the walls
     * before their room opens is UNKNOWN; they only wandered a few blocks), and doesn't heal or strike.
     */
    public void setDormant(LivingEntity entity, boolean dormant) {
        this.dormant = dormant;
        if (entity instanceof org.bukkit.entity.Mob mob) {
            mob.setAware(!dormant);
            if (dormant) mob.setTarget(null);
        }
    }

    public MobKind.Variant variant() {
        return variant;
    }

    public boolean starred() {
        return options.starred();
    }

    public Modifier modifier() {
        return options.modifier();
    }

    /** Its movement speed attribute: the kind's (NaN for vanilla's), and Speedy's +0.12. */
    public double speed() {
        double bonus = options.modifier() == null ? 0 : options.modifier().speedBonus();
        return Double.isNaN(kind.speed()) ? kind.speed() : kind.speed() + bonus;
    }

    /** Tank Zombies and Fortified mobs aren't knocked back. */
    public boolean knockbackImmune() {
        return behaviour.knockbackImmune() || (options.modifier() != null && options.modifier().knockbackImmune());
    }

    @Override public String getId() { return kind.id(); }
    @Override public EntityType getEntityType() { return kind.entityType(); }
    @Override public String getName() { return kind.name(); }
    @Override public int getLevel() { return variant.level(); }
    @Override public double getMaxHealth() { return maxHealth; }
    @Override public double getDamage() { return damage; }
    @Override public double getDefense() { return variant.defense(); }
    @Override public double getMagicResistance() { return kind.magicResistance(); }
    @Override public Set<MobType> getTypes() { return Set.copyOf(kind.types()); }
    @Override public double getKnockback() { return behaviour.knockback(); }
    @Override public boolean isBoss() { return kind.style() == MobKind.NameStyle.BOSS; }
    @Override public List<MobDrop> getDrops() { return variant.drops(); }
    @Override public int getOrbs() { return variant.orbs(); }
    @Override public SkyBlockMob getPassenger() { return behaviour.passenger(); }
    @Override public boolean dropsToInventory() { return kind.dungeon(); }

    @Override
    public String nameTag(double health) {
        return switch (kind.style()) {
            case HUB, BOSS -> NameTags.hub(variant.level(), kind.name(), health, maxHealth, isBoss());
            default -> NameTags.dungeon(kind.style(), kind.types(), kind.name(), variant.level(), options.starred(), options.modifier(), health,
                    maxHealth);
        };
    }

    private MobKind.Gear gear() {
        return variant.gear() != null ? variant.gear() : kind.gear();
    }

    private static ItemStack stack(MobKind.Piece piece) {
        return piece == null ? null : piece.stack();
    }

    @Override public ItemStack getItemInHand() { return stack(gear().hand()); }
    @Override public ItemStack getHelmet() { return stack(gear().helmet()); }
    @Override public ItemStack getChestplate() { return stack(gear().chestplate()); }
    @Override public ItemStack getLeggings() { return stack(gear().leggings()); }
    @Override public ItemStack getBoots() { return stack(gear().boots()); }

    /** A player-shaped mob has its skin before anyone sees it. */
    @Override
    public void beforeSpawn(LivingEntity entity) {
        if (entity instanceof Mannequin mannequin) {
            if (kind.skin() != null) mannequin.setProfile(DungeonTextures.profile(kind.skin()));
            mannequin.setDescription(null);
        }
    }

    @Override
    public void onSpawn(LivingEntity entity) {
        // Daylight, reinforcements and doors are vanilla's business, not a SkyBlock mob's.
        if (entity instanceof Zombie zombie) {
            zombie.setShouldBurnInDay(false);
            zombie.setAdult();
            zombie.setCanBreakDoors(false);
            set(entity, Attribute.SPAWN_REINFORCEMENTS, 0);
        }
        if (entity instanceof AbstractSkeleton skeleton) skeleton.setShouldBurnInDay(false);
        if (!Double.isNaN(speed())) set(entity, Attribute.MOVEMENT_SPEED, speed());
        if (knockbackImmune()) set(entity, Attribute.KNOCKBACK_RESISTANCE, 1);
        behaviour.spawned(this, entity);
    }

    private static void set(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (dormant) return;
        age++;
        Modifier modifier = options.modifier();
        if (modifier == Modifier.HEALING && age % 20 == 0) {
            Mobs.Live live = Mobs.of(entity);
            if (live != null) Mobs.heal(live, HEALING_PER_SECOND);
        }
        if (modifier == Modifier.STORMY && age % STORM_EVERY == 0) storm(entity);
        behaviour.tick(this, entity);
    }

    private void storm(LivingEntity entity) {
        for (Player player : entity.getWorld().getPlayers()) {
            boolean fair = player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
            if (!fair || player.isDead() || player.getLocation().distanceSquared(entity.getLocation()) > STORM_RANGE * STORM_RANGE) continue;
            entity.getWorld().strikeLightningEffect(player.getLocation());
            PlayerDamage.hit(player, STORM_SHARE * PlayerHealth.max(player), PlayerDamage.Kind.TRUE, null, 0);
        }
    }

    @Override
    public void onDamaged(LivingEntity entity, Player by, double damage) {
        behaviour.damaged(this, entity, by, damage);
    }

    /** Flaming mobs' melee hits set you on fire (not their arrows or skulls: "melee attacks", the wiki). */
    @Override
    public void onHit(LivingEntity entity, Player target, boolean melee) {
        if (melee && options.modifier() == Modifier.FLAMING) target.setFireTicks(Math.max(target.getFireTicks(), BURN_TICKS));
        behaviour.attacked(this, entity, target);
    }
}
