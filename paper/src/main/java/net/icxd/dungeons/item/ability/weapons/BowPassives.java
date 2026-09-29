package net.icxd.dungeons.item.ability.weapons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.Debuffs;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.MobDebuffs;
import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.combat.Shots;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunItems;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.ItemCounters;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.ability.utility.Buffs;
import net.icxd.dungeons.item.behaviour.HeldStats;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.session.Mana;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * Bows' passives (ABILITIES_WEAPONS.md, "Weapon passives" and "Bows"), as their text says: the Runaan's Bow's Triple
 * Shot and the Hurricane Bow's Tempest (more arrows a shot), the Venom's Touch's volley and venom, the Crypt Bow's
 * skulls, the Sniper Bow's range, the Undead bows' bounce, Last Breath's Defense shred, the Explosive Bow's
 * Explosive Shot, and the abilities of bows: Arrow Infusion (ON_SHOOT), Extreme Focus (the End Stone Bow's left
 * click) and the Stinger Bow's Sting on teammates. What they need is heard through Shots' shot listeners, the hit
 * listeners (WeaponPassives) and arrows landing (WeaponEvents). Main thread.
 */
final class BowPassives {
    /** On arrows a bow's passive made (extra arrows, a bounce): they don't make more of their own. */
    static final String EXTRA = "skyblock_extra_arrow";
    /** On an arrow a Slime Bow's Arrow Infusion fed: "5x damage to Magma Cubes and Slimes". */
    static final String SLIME_INFUSED = "skyblock_slime_infused";
    /** Triple Shot's side arrows: "fires 2 additional arrows at 12.5° from the main arrow" (the wiki's Runaan's Bow). */
    static final double TRIPLE_SPREAD = 12.5;
    /** "These arrows home towards targets within 10{{Confirm}} blocks" (the wiki's Runaan's Bow). */
    static final double HOMING_RANGE = 10;
    /** Tempest's: "The additional arrows come out at a 7.5° and 15° offset from the main arrow" (the wiki's Hurricane Bow). */
    static final double TEMPEST_SPREAD = 7.5;
    /** UNKNOWN how the Venom's Touch's volley spreads: 5° apart. */
    static final double VOLLEY_SPREAD = 5;
    /** UNKNOWN how much further the Sniper Bow shoots ("much further", "fires arrows at a much faster rate"): half as fast again. */
    static final double SNIPER_SPEED = 1.5;
    /** UNKNOWN how far a bounce reaches (the wiki gives none): 8 blocks from what it hit. */
    static final double BOUNCE_RANGE = 8;
    /** An arrow's speed off a drawn bow (vanilla's 3 blocks a tick), for a bounce. */
    private static final float BOUNCE_SPEED = 2.5f;
    /** UNKNOWN how long Last Breath's shred lasts (its text gives none): until the mob dies, as far as a fight goes. */
    static final long SHRED_MILLIS = 10 * 60_000;
    /** "All enemies within 3 blocks of the arrow take Projectile damage" (the wiki's Explosive Bow). */
    static final double EXPLOSION_RADIUS = 3;
    /** Sting: "shooting your teammates grants them 20 speed and 30 strength for 30 seconds but deals 1 damage". */
    private static final Pattern STING = Pattern.compile("grants them (\\d+) speed and (\\d+) strength for (\\d+) seconds but deals (\\d+) damage");
    /** Arrow Infusion: "Consumes 1 Magma Cream from the Inventory or Quiver to double the damage per shot." */
    private static final Pattern DOUBLES = Pattern.compile("Consumes 1 (.+?) from the Inventory or Quiver to double the damage per shot");
    /** "Will consume Slimeballs in your Inventory or Quiver to deal 5x damage to Magma Cubes and Slimes." */
    private static final Pattern SLIMES = Pattern.compile("consume (.+?) in your Inventory or Quiver to deal ([\\d.]+)x damage to Magma Cubes and Slimes");

    /** Triple Shot's side arrows homing in. */
    private static final List<AbstractArrow> HOMING = new ArrayList<>();
    /** "Enemies within 2.5 blocks of the arrow take 360 + (3.6 x Strength) True Damage" (the wiki's Spider Queen's Stinger). */
    static final double AURA_RADIUS = 2.5;

    /** A Spider Queen's Stinger's arrow in flight: whose, what its aura deals, with what bow, and the mobs it has hurt. */
    private record Aura(AbstractArrow arrow, UUID by, double damage, NBTTagCompound bow, Set<UUID> hurt) {
    }

    private static final Map<UUID, Aura> AURAS = new HashMap<>();
    /** The Explosive Bow's arrows in flight: what their blast is worked out with (the shot's, as the arrow's own hit). */
    private static final Map<UUID, Damage.Attacker> EXPLOSIVE = new HashMap<>();
    /** Extreme Focus's damage waiting for their next hit, by player. */
    private static final Map<UUID, Double> FOCUS = new HashMap<>();
    /** A slime-fed arrow's factor on Magma Cubes and Slimes, by arrow. */
    private static final Map<UUID, Double> SLIME_FACTOR = new HashMap<>();
    private static final Map<String, String> IDS_BY_NAME = new HashMap<>();

    private BowPassives() {
    }

    static void register() {
        Shots.addShotListener(BowPassives::shot);
        Combat.addPlayerHitListener(BowPassives::hitPlayer);
        Bukkit.getScheduler().runTaskTimer(Dungeons.getInstance(), BowPassives::tick, 1, 1);
    }

    static void forget(UUID player) {
        FOCUS.remove(player);
    }

    // ---------- shots ----------

    /**
     * A drawn bow shot (a shortbow's arrows are its own): the Crypt Bow's skull in its arrow's place, the Sniper
     * Bow's faster arrow, and the extra arrows of the Runaan's Bow, the Hurricane Bow and the Venom's Touch. The extra
     * arrows are recorded as shots of the same bow (their Crit Chance rolled on their own: UNKNOWN whether Hypixel's
     * share the main arrow's), but aren't shots themselves, so they make none of their own.
     */
    private static void shot(Player player, Projectile projectile, NBTTagCompound bow, boolean fullyDrawn, boolean shortbow) {
        if (shortbow || !(projectile instanceof AbstractArrow arrow) || arrow.getScoreboardTags().contains(EXTRA)) return;
        WeaponPassives.Weapon weapon = WeaponPassives.weapon(bow);
        if (weapon == null) return;
        if (weapon.skulls) {
            skull(player, weapon.item, bow, arrow);
            return;
        }
        if (weapon.further) arrow.setVelocity(arrow.getVelocity().multiply(SNIPER_SPEED));
        if (weapon.lore.auraDamage > 0) {
            double strength = PlayerSession.of(player).stats().get(Stat.STRENGTH);
            AURAS.put(arrow.getUniqueId(), new Aura(arrow, player.getUniqueId(), auraDamage(weapon.lore.auraDamage, strength), bow, new HashSet<>()));
        }
        if (weapon.explosive) EXPLOSIVE.put(arrow.getUniqueId(), Hits.striker(player, bow, Hits.Strike.arrow(0, 1)));
        if (weapon.extraArrows > 0) {
            // Triple Shot: "Power and Piercing do not work on them" (the wiki's Runaan's Bow).
            NBTTagCompound without = without(bow, Set.of("power", "piercing"));
            for (double angle : sides(weapon.extraArrows, TRIPLE_SPREAD)) {
                AbstractArrow extra = extra(player, arrow, without, fullyDrawn, angle, weapon.extraShare);
                HOMING.add(extra);
            }
        }
        if (weapon.tempest) {
            int more = HeldStats.tempestArrows(ItemCounters.get(bow, HeldStats.HURRICANE_KILLS)) - 1;
            // "Unlike the Runaan's Bow, the extra arrows deal 100% of the main arrow's damage."
            for (double angle : sides(more, TEMPEST_SPREAD)) extra(player, arrow, bow, fullyDrawn, angle, 1);
        }
        if (weapon.lore.volley > 1) {
            for (double angle : sides(weapon.lore.volley - 1, VOLLEY_SPREAD)) extra(player, arrow, bow, fullyDrawn, angle, 1);
        }
    }

    /**
     * The angles of {@code extras} arrows beside a shot's: one each side at {@code spread}, then at twice it, and so
     * on, the first to the right (the Hurricane Bow's 2 arrows: the main one and one at +7.5°).
     */
    static List<Double> sides(int extras, double spread) {
        List<Double> angles = new ArrayList<>();
        for (int i = 0; i < extras; i++) angles.add((i % 2 == 0 ? 1 : -1) * spread * (i / 2 + 1));
        return angles;
    }

    /** An arrow like {@code main}, turned {@code degrees} about the vertical, recorded as a shot of {@code bow} doing {@code share} of its damage. */
    private static AbstractArrow extra(Player player, AbstractArrow main, NBTTagCompound bow, boolean fullyDrawn, double degrees, double share) {
        Vector velocity = main.getVelocity().clone().rotateAroundY(Math.toRadians(-degrees));
        Arrow extra = main.getWorld().spawnArrow(main.getLocation(), velocity, (float) velocity.length(), 0);
        extra.setShooter(player);
        extra.setCritical(main.isCritical());
        extra.setFireTicks(main.getFireTicks());
        extra.addScoreboardTag(EXTRA);
        Shots.record(extra, player, bow, fullyDrawn, share);
        return extra;
    }

    /** The bow's data without these enchantments (for arrows they don't count on). */
    static NBTTagCompound without(NBTTagCompound bow, Set<String> enchantments) {
        NBTTagCompound copy = bow.copy();
        NBTTagList list = copy.getList("enchantments", 10);
        NBTTagList kept = new NBTTagList();
        for (int i = 0; i < list.size(); i++) {
            if (!enchantments.contains(list.get(i).getString("name").toLowerCase(Locale.ROOT))) kept.add(list.get(i));
        }
        copy.set("enchantments", kept);
        return copy;
    }

    /**
     * The Crypt Bow: "Replaces the arrows that you shoot with exploding wither skulls!": its arrow goes, and a skull
     * flies in its place as the Crypt swords' do, its blast theirs (the Dreadlord's 500 base, 0.3 scaling: UNKNOWN,
     * no page gives the bow's; the bow has Intelligence and no Damage, as a caster would), under the bow's name.
     */
    private static void skull(Player player, SkyBlockItem bow, NBTTagCompound tag, AbstractArrow arrow) {
        Vector direction = arrow.getVelocity();
        arrow.remove();
        if (direction.lengthSquared() == 0) return;
        Location eye = player.getEyeLocation();
        Skulls.shoot(player, bow, tag, Hits.spellOf(bow, Skulls.DREADLORD), bow.name(), eye, direction);
        player.getWorld().playSound(eye, Sound.ENTITY_WITHER_SHOOT, 0.6f, 1);
    }

    /**
     * The Spider Queen's Stinger: "Arrows shot using this bow have an aura around them that deals 360❁ Damage to nearby
     * enemies instead of dealing impact damage. Arrows travel through enemies." The wiki's: "Enemies within 2.5 blocks
     * of the arrow take 360 + (3.6 x Strength) True Damage", its text's number and the shooter's Strength when it was
     * shot, "not affected by Hot Potato Book buffs, Enchantments, Crit Damage". Each mob once an arrow (UNKNOWN), as an
     * effect's damage with no Defense; the arrow goes through mobs (see {@link #passesThrough}).
     */
    static double auraDamage(double base, double strength) {
        return Math.max(0, base) * (1 + Math.max(0, strength) / 100);
    }

    /** Whether this arrow goes through the mobs it meets (a Spider Queen's Stinger's): its hits are called off. */
    static boolean passesThrough(Entity arrow) {
        return !AURAS.isEmpty() && AURAS.containsKey(arrow.getUniqueId());
    }

    private static void tickAuras() {
        for (Iterator<Aura> it = AURAS.values().iterator(); it.hasNext(); ) {
            Aura aura = it.next();
            Player by = Bukkit.getPlayer(aura.by());
            if (by == null || !aura.arrow().isValid() || aura.arrow().isInBlock() || aura.arrow().isOnGround() || !Hits.canStillHit(by)) {
                it.remove();
                continue;
            }
            for (LivingEntity mob : Hits.near(aura.arrow().getLocation(), AURA_RADIUS)) {
                if (aura.hurt().add(mob.getUniqueId())) MobHits.deal(by, mob, aura.damage(), DamageIndicators.Look.NORMAL, HitKind.OTHER, aura.bow());
            }
        }
    }

    /** Every tick: Triple Shot's side arrows turn towards the nearest mob within reach, keeping their speed; auras hurt. */
    private static void tick() {
        if (!AURAS.isEmpty()) tickAuras();
        for (Iterator<AbstractArrow> it = HOMING.iterator(); it.hasNext(); ) {
            AbstractArrow arrow = it.next();
            if (!arrow.isValid() || arrow.isInBlock() || arrow.isOnGround()) {
                it.remove();
                continue;
            }
            LivingEntity target = nearest(arrow.getLocation(), HOMING_RANGE);
            if (target == null) continue;
            Vector velocity = arrow.getVelocity();
            double speed = velocity.length();
            Vector to = target.getBoundingBox().getCenter().subtract(arrow.getLocation().toVector());
            if (to.lengthSquared() > 0 && speed > 0) arrow.setVelocity(to.normalize().multiply(speed));
        }
    }

    private static LivingEntity nearest(Location at, double range) {
        List<LivingEntity> near = Hits.near(at, range);
        return near.isEmpty() ? null : near.get(0);
    }

    // ---------- hits ----------

    /**
     * An arrow's buff as it lands (see WeaponPassives): a slime-fed arrow's 5x on Cubic mobs (the Hub's Magma Cube;
     * UNKNOWN which mobs Hypixel counts: its text's "Magma Cubes and Slimes").
     */
    static double factor(Combat.Landing landing, Damage.Target target) {
        if (landing.projectile() == null) return 1;
        Double factor = SLIME_FACTOR.get(landing.projectile().getUniqueId());
        return factor != null && target.types().contains(MobType.CUBIC) ? factor : 1;
    }

    /** Extreme Focus's damage for their next hit, taken; 0 for none. */
    static double focus(Player player) {
        Double added = FOCUS.remove(player.getUniqueId());
        return added == null ? 0 : added;
    }

    /**
     * An arrow of a bow's has done its damage (see WeaponPassives): Last Breath's shred, the Venom's Touch's venom and
     * the Undead bows' bounce.
     */
    static void landed(Player player, WeaponPassives.Weapon weapon, Combat.Landing landing, double damage) {
        WeaponLore lore = weapon.lore;
        LivingEntity mob = landing.entity();
        if (lore.shredStacks > 0) {
            // One shred per mob for everyone's arrows (UNKNOWN how Hypixel's stack across players).
            MobDebuffs.add(mob, new Debuffs.Spec("Last Breath", Debuffs.Kind.DEFENSE, lore.shredShare, lore.shredStacks, SHRED_MILLIS), player);
        }
        if (lore.venomSeconds > 0 && lore.venomDamage > 0) {
            MobDebuffs.dot(mob, "Venom's Touch", player, lore.venomDamage, 20, lore.venomSeconds, DamageIndicators.Look.POISON);
        }
        if (lore.bounceChance > 0 && landing.projectile() instanceof AbstractArrow arrow && !arrow.getScoreboardTags().contains(EXTRA)
                && ThreadLocalRandom.current().nextDouble() < lore.bounceChance) {
            bounce(player, arrow, mob, landing.weapon());
        }
    }

    /**
     * "Your arrows have a 50% chance to bounce to another target after it hits something": a new arrow of the same
     * bow from what it hit to the nearest other mob within reach, hitting it as an arrow of the bow does (its
     * damage worked out anew). Only off a mob (UNKNOWN whether "something" is a block too), and a bounced arrow
     * doesn't bounce again (UNKNOWN).
     */
    private static void bounce(Player player, AbstractArrow arrow, LivingEntity from, NBTTagCompound bow) {
        LivingEntity to = null;
        for (LivingEntity mob : Hits.near(from.getLocation(), BOUNCE_RANGE)) {
            if (!mob.equals(from)) {
                to = mob;
                break;
            }
        }
        if (to == null) return;
        Vector start = from.getBoundingBox().getCenter();
        Vector direction = to.getBoundingBox().getCenter().subtract(start);
        if (direction.lengthSquared() == 0) return;
        direction.normalize();
        // Out of the hit mob's box, so it doesn't hit it again on its way out.
        Location at = start.add(direction.clone().multiply(from.getWidth() / 2 + 0.6)).toLocation(from.getWorld());
        Arrow bounced = from.getWorld().spawnArrow(at, direction, BOUNCE_SPEED, 0);
        bounced.setShooter(player);
        bounced.addScoreboardTag(EXTRA);
        Shots.record(bounced, player, bow, true);
    }

    /**
     * One of their arrows landed (see WeaponEvents), before its hit is worked out: the Explosive Bow's "Creates an
     * explosion on impact! Every Monster caught in this explosion takes the full damage of the weapon! Acts as
     * Superboom TNT!": every mob within 3 blocks but the one it hit (whose is the arrow's own hit) takes the arrow's
     * damage (worked out with the shot's stats, each its own crit roll: UNKNOWN), as an ability's hit, times
     * Consolidated; and in a run it's Superboom TNT where it landed.
     */
    static void arrowLanded(AbstractArrow arrow, Player shooter, Entity hit, Block block) {
        Damage.Attacker shot = EXPLOSIVE.remove(arrow.getUniqueId());
        // A slime-fed arrow's factor is for its hit, which is worked out after this: it goes with the arrow (arrowGone),
        // or here when it lands on a block.
        if (hit == null) SLIME_FACTOR.remove(arrow.getUniqueId());
        if (shot == null || !Hits.canStillHit(shooter)) return;
        Location at = arrow.getLocation();
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 1);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f);
        NBTTagCompound bow = Shots.bow(arrow);
        Hits.Strike strike = new Hits.Strike(true, 0, Explosions.factor(shooter), false, false);
        for (LivingEntity mob : Hits.near(at, EXPLOSION_RADIUS)) {
            if (!mob.equals(hit)) Hits.weaponHit(shooter, bow, shot, mob, strike);
        }
        Block superboom = block != null ? block : at.getBlock();
        RunItems.superboom(shooter, superboom);
    }

    static void arrowGone(UUID arrow) {
        EXPLOSIVE.remove(arrow);
        AURAS.remove(arrow);
        SLIME_FACTOR.remove(arrow);
    }

    // ---------- teammates ----------

    /**
     * The Stinger Bow's Sting: "While in Dungeons, shooting your teammates grants them 20 speed and 30 strength for 30
     * seconds but deals 1 damage", for its block's 100 mana (with what makes it cheaper): an arrow of the bow on a
     * living teammate of their run. Without the mana, nothing (UNKNOWN). The 1 damage is taken from their health as it
     * is (UNKNOWN whether Defense counts).
     */
    private static void hitPlayer(Player attacker, Player target, HitKind kind, NBTTagCompound weapon, Projectile projectile) {
        if (kind != HitKind.ARROW) return;
        WeaponPassives.Weapon bow = WeaponPassives.weapon(weapon);
        if (bow == null || bow.sting == null || !RunItems.alive(attacker) || !RunItems.alive(target)) return;
        DungeonRun run = RunManager.of(attacker);
        if (run == null || run != RunManager.of(target)) return;
        Matcher m = STING.matcher(AbilityText.plain(bow.sting.text()));
        if (!m.find()) return;
        int cost = Abilities.manaCost(bow.sting, PlayerSession.of(attacker).maxMana(), attacker, weapon);
        if (!Hits.enoughMana(attacker, cost)) return;
        Hits.takeMana(attacker, cost, bow.sting.name());
        Stats stats = new Stats().set(Stat.SPEED, Double.parseDouble(m.group(1))).set(Stat.STRENGTH, Double.parseDouble(m.group(2)));
        Buffs.give(target, bow.sting.name(), stats, (long) (Double.parseDouble(m.group(3)) * 1000));
        PlayerHealth.damage(target, Double.parseDouble(m.group(4)));
    }

    // ---------- abilities ----------

    /**
     * Arrow Infusion (ON_SHOOT): "Consumes 1 Magma Cream from the Inventory or Quiver to double the damage per shot"
     * (the Magma Bow; the Sulphur Bow's Sulphur), "Will consume Slimeballs in your Inventory or Quiver to deal 5x damage
     * to Magma Cubes and Slimes" (the Slime Bow; one a shot, UNKNOWN). With none in their inventory, the shot is as
     * ever. The Quiver's (LATER: there's no Quiver) and the Prismarine Bow's (sea creatures, squids and guardians:
     * none here) aren't. The item the text names is found by its name in the data ("Sulphur" is SULPHUR_ORE's).
     */
    static final class ArrowInfusion implements AbilityHandler {
        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block, Trigger trigger) {
            Infusion infusion = infusion(block);
            return infusion != null && trigger.projectile() != null && slot(player, infusion.itemId()) >= 0;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block, boolean vitalityPaid, Trigger trigger) {
            Infusion infusion = infusion(block);
            int slot = infusion == null ? -1 : slot(player, infusion.itemId());
            if (slot < 0 || trigger.projectile() == null) return;
            ItemStack stack = player.getInventory().getItem(slot);
            stack.setAmount(stack.getAmount() - 1);
            player.getInventory().setItem(slot, stack.getAmount() <= 0 ? null : stack);
            if (infusion.slimes()) SLIME_FACTOR.put(trigger.projectile().getUniqueId(), infusion.factor());
            else Shots.scale(trigger.projectile(), infusion.factor());
        }
    }

    /** What an Arrow Infusion feeds on (its id in the data), its factor, and whether it's only on Magma Cubes and Slimes. */
    record Infusion(String itemId, double factor, boolean slimes) {
    }

    /** What its text says it feeds on; null for one that isn't built (the Prismarine Bow's). */
    static Infusion infusion(ItemBlock block) {
        String plain = AbilityText.plain(block.text());
        Matcher m = DOUBLES.matcher(plain);
        if (m.find()) return byName(m.group(1), 2, false);
        m = SLIMES.matcher(plain);
        if (m.find()) return byName(m.group(1), Double.parseDouble(m.group(2)), true);
        return null;
    }

    /** "Magma Cream", "Sulphur", "Slimeballs": the item with that name (singular) in the data; null for none. */
    private static Infusion byName(String name, double factor, boolean slimes) {
        String id = idByName(name.endsWith("s") ? name.substring(0, name.length() - 1) : name);
        if (id == null) id = idByName(name);
        return id == null ? null : new Infusion(id, factor, slimes);
    }

    /** The id of the item with this name ("Slimeball": SLIME_BALL); null for none. Looked up once each. */
    private static String idByName(String name) {
        if (IDS_BY_NAME.containsKey(name)) return IDS_BY_NAME.get(name);
        String found = null;
        for (SkyBlockItem item : ItemRegistry.getRegistry().values()) {
            if (name.equalsIgnoreCase(item.name().replaceAll("[&§].", ""))) {
                found = item.id();
                break;
            }
        }
        IDS_BY_NAME.put(name, found);
        return found;
    }

    /** The first inventory slot holding the item with this id; -1 for none. */
    private static int slot(Player player, String id) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            NBTTagCompound tag = contents[i] == null || contents[i].isEmpty() ? null : ItemNBT.read(contents[i]);
            if (tag != null && id.equals(tag.getString("id"))) return i;
        }
        return -1;
    }

    /**
     * The End Stone Bow's Extreme Focus: "Consumes all your mana, and your next hit will deal that much more damage!":
     * the mana they had is added to their next hit (a melee hit or an arrow) as damage that only a crit multiplies
     * (the wiki's "Add Damage": EFFECTS.md). A new focus takes the old one's place. The End Stone Sword's ("While in
     * the The End") does nothing: there's no End here (LATER).
     */
    static final class ExtremeFocus implements AbilityHandler {
        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            return !AbilityText.plain(block.text()).contains("While in the The End") && Mana.get(player) > 0;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            int mana = Mana.get(player);
            int spent = Mana.spend(player, mana, block.name());
            if (spent > 0) FOCUS.put(player.getUniqueId(), (double) spent);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1, 1.2f);
        }
    }
}
