package net.icxd.dungeons.item.ability.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ItemCounters;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.ability.weapons.Hits;
import net.icxd.dungeons.item.behaviour.HeldStats;
import net.icxd.dungeons.item.bonus.Bonus;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.bonus.Worn;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.session.Mana;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Replacement;

/**
 * Worn armor's and equipment's own passive abilities that don't hit (ABILITIES_UTILITY.md, "Worn"): the ABILITY blocks
 * on a piece, each while a piece with it is worn (as set bonuses count what's worn: {@link SetBonuses#worn}), as
 * {@link Bonus}es of the kind "ABILITY" found by their block's name, and the shields they put on hits ({@link
 * #shield}, on {@link PlayerDamage}'s way through {@link Protection}). Each reads its numbers from its block's text.
 * Main thread.
 */
public final class WornPassives {
    /** The kind of the blocks these are found by. */
    public static final String KIND = "ABILITY";
    /** Bone Shield: "A Bone Shield will surround you" with 3 bones (the wiki's Skeleton's Helmet), "Bones regenerate every 30 seconds". */
    static final int BONES = 3;
    private static final long BONE_BACK_MILLIS = 30_000;
    /** UNKNOWN how strong Mithril's Protection's "Regeneration" is and how long it lasts: +100 Health Regen (twice the base's) for 5 seconds. */
    static final double REGENERATION = 100;
    static final long REGENERATION_MILLIS = 5_000;
    /** Double Jump's push: UNKNOWN (a jump's worth up, a little forward). */
    private static final double JUMP_UP = 0.9;
    private static final double JUMP_FORWARD = 0.3;

    private static final Pattern FULL_HEALTH_CAP = Pattern.compile("Any damage taken is max ([\\d.]+)% of the wearer's");
    private static final Pattern GROWTH_HEAL = Pattern.compile("Heals you for ([\\d.]+)% ❤ Health after killing a Monster");
    private static final Pattern GROWTH_MAX = Pattern.compile("\\(Max (\\d+)\\)");
    private static final Pattern RESTORES = Pattern.compile("Restores \\+([\\d.]+)❤ Health every second");
    private static final Pattern GLADIATOR = Pattern.compile("Gain \\+([\\d.]+)❈ Defense for each enemy within ([\\d.]+) blocks up to \\+([\\d.]+)❈ Defense");
    private static final Pattern GLADIATOR_TANK = Pattern.compile("Range increases to ([\\d.]+) blocks and the cap increases to \\+([\\d.]+)❈ Defense when you play as a Tank");

    /** Bone Shield's bones: how many they have, when the next comes back, and the bones shown around them. */
    private static final class Bones {
        int left = BONES;
        long nextBack;
        final List<ItemDisplay> shown = new ArrayList<>();
    }

    private static final Map<UUID, Bones> BONE_SHIELDS = new HashMap<>();
    /** Double Jump: who's jumped since they last stood on the ground. */
    private static final Set<UUID> JUMPED = new HashSet<>();

    private WornPassives() {
    }

    /** The bonuses these are, for SetBonuses (see WornAbilities). */
    public static List<Bonus> all() {
        return List.of(new BlockDamage(), new BoneShield(), new MithrilsProtection(), new Growth(), new DoubleJump(),
                new Coating("Depth Coating"), new Coating("Pressurized Coating"), new GladiatorsWill(), new BlazingRestoration(),
                new FarmersSpeedCap());
    }

    /** A worn piece's passive ABILITY block, by its name. */
    abstract static class Passive implements Bonus {
        private final String name;

        Passive(String name) {
            this.name = name;
        }

        @Override
        public String kind() {
            return KIND;
        }

        @Override
        public String name() {
            return name;
        }

        /** Its block on the first worn piece that has it; null for none. */
        ItemBlock block(Active active) {
            for (Worn.Piece piece : active.pieces()) {
                ItemBlock block = pieceBlock(piece, name);
                if (block != null) return block;
            }
            return null;
        }

        String text(Active active) {
            ItemBlock block = block(active);
            return block == null ? "" : AbilityText.plain(block.text());
        }
    }

    /** A piece's ABILITY block with this name; null for none. */
    static ItemBlock pieceBlock(Worn.Piece piece, String name) {
        for (ItemBlock block : piece.blocks()) if (block.isAbility() && name.equals(block.name())) return block;
        return null;
    }

    /** The ABILITY block of this name on the first worn piece that has it; null if none is worn (nothing made: every hit asks). */
    static ItemBlock worn(Player player, String name) {
        for (Worn.Piece piece : SetBonuses.worn(player).pieces()) {
            ItemBlock block = pieceBlock(piece, name);
            if (block != null) return block;
        }
        return null;
    }

    static double number(Pattern pattern, String plain, double otherwise) {
        Matcher m = pattern.matcher(plain);
        return m.find() ? Double.parseDouble(m.group(1).replace(",", "")) : otherwise;
    }

    /** Whether a passive's cooldown (its block's) is over; if it is, it starts again. Kept with the ability cooldowns. */
    static boolean ready(Player player, ItemBlock block) {
        PlayerSession session = PlayerSession.of(player);
        String key = "ability:" + block.name();
        if (session.cooldownLeft(key) > 0) return false;
        long millis = Abilities.cooldownMillis(block, player);
        if (millis > 0) session.startCooldown(key, millis);
        return true;
    }

    // ---------- shields ----------

    /**
     * What's left of a hit once the worn pieces' shields have had it, in this order: Block Damage ("If you are at full
     * ❤ Health, the first damage you take will be nullified", every 60 s, its cooldown), a bone of Bone Shield
     * ("nullifying damage you take but consuming a bone in the process"; not a boss's hits: "Bone Shields ability
     * won't work on Magma Boss and new bosses anymore", the wiki's history), Mithril's Protection's cap ("Any damage
     * taken is max 40% of the wearer's ❤ Health", their max health's: UNKNOWN whether it's their health now), and a
     * Gyrokinetic Wand's Aligned (see {@link CellsAlignment}).
     */
    static double shield(Player player, double taken, Entity by) {
        ItemBlock block = worn(player, "Block Damage");
        if (block != null && PlayerHealth.get(player) >= PlayerHealth.max(player) && ready(player, block)) return 0;
        if (worn(player, BoneShield.NAME) != null && !boss(by) && BoneShield.take(player)) return 0;
        block = worn(player, "Mithril's Protection");
        if (block != null) {
            double cap = number(FULL_HEALTH_CAP, AbilityText.plain(block.text()), 40) / 100 * PlayerHealth.max(player);
            if (taken > cap) {
                taken = cap;
                // "Gain Regeneration when this ability activates."
                PlayerSession.of(player).buff("Mithril's Protection", new Stats().set(Stat.HEALTH_REGEN, REGENERATION), REGENERATION_MILLIS);
            }
        }
        return CellsAlignment.aligned(player, taken);
    }

    /** Whether what hit them is a boss's (or its projectile). */
    private static boolean boss(Entity by) {
        Mobs.Live live = Mobs.of(PlayerDamage.attacker(by));
        return live != null && live.type().isBoss();
    }

    static final class BlockDamage extends Passive {
        BlockDamage() {
            super("Block Damage");
        }
    }

    /**
     * The Skeleton's Helmet's Bone Shield: 3 bones around them, each taking a whole hit, one back every 30 seconds.
     * They're full when it goes on (UNKNOWN). The bones circle their waist (UNKNOWN how Hypixel's move).
     */
    static final class BoneShield extends Passive {
        static final String NAME = "Bone Shield";

        BoneShield() {
            super(NAME);
        }

        /** A bone takes the hit; false if they have none left. */
        static boolean take(Player player) {
            Bones bones = BONE_SHIELDS.computeIfAbsent(player.getUniqueId(), id -> new Bones());
            if (bones.left <= 0) return false;
            if (bones.left == BONES) bones.nextBack = System.currentTimeMillis() + BONE_BACK_MILLIS;
            bones.left--;
            return true;
        }

        @Override
        public void second(Player player, Active active) {
            Bones bones = BONE_SHIELDS.computeIfAbsent(player.getUniqueId(), id -> new Bones());
            long now = System.currentTimeMillis();
            if (bones.left < BONES && now >= bones.nextBack) {
                bones.left++;
                bones.nextBack = now + BONE_BACK_MILLIS;
            }
        }

        @Override
        public void ended(Player player) {
            forgetBones(player.getUniqueId());
        }
    }

    /** Every tick: bones circle their wearers, as many as they have left. */
    static void tick() {
        if (BONE_SHIELDS.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Bones> entry : BONE_SHIELDS.entrySet()) {
            Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
            Bones bones = entry.getValue();
            if (player == null) continue;
            while (bones.shown.size() > bones.left) bones.shown.remove(bones.shown.size() - 1).remove();
            while (bones.shown.size() < bones.left) bones.shown.add(bone(player.getLocation()));
            for (int i = 0; i < bones.shown.size(); i++) {
                double angle = now / 400.0 + i * 2 * Math.PI / BONES;
                Location at = player.getLocation().add(Math.cos(angle) * 1.2, 1, Math.sin(angle) * 1.2);
                ItemDisplay bone = bones.shown.get(i);
                if (bone.isValid()) bone.teleport(at);
            }
        }
    }

    private static ItemDisplay bone(Location at) {
        return at.getWorld().spawn(at, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.BONE));
            d.setPersistent(false);
            d.setTeleportDuration(1);
            d.addScoreboardTag(UtilityListener.NOT_A_MOB);
        });
    }

    private static void forgetBones(UUID player) {
        Bones bones = BONE_SHIELDS.remove(player);
        if (bones != null) for (ItemDisplay bone : bones.shown) bone.remove();
    }

    static final class MithrilsProtection extends Passive {
        MithrilsProtection() {
            super("Mithril's Protection");
        }
    }

    // ---------- kills, stats, seconds ----------

    /**
     * The Growth armor's Growth: "Heals you for 1% ❤ Health after killing a Monster, and also increases the ❤ Health
     * bonus of a piece of the armor by 1 (Max 100)", its 4 second cooldown the block's. Each piece has its own
     * block, so each worn piece gains its 1 (its count on it, see HeldStats), and the heal is once a kill (UNKNOWN
     * whether it's once a piece). A "Monster" is any mob but an Animal or Critter one (UNKNOWN). The Health is the
     * pieces' counts while they're worn.
     */
    static final class Growth extends Passive {
        Growth() {
            super("Growth");
        }

        @Override
        public void killed(Player player, Active active, SkyBlockMobDeathEvent event) {
            if (event.kind().types().contains(MobType.ANIMAL) || event.kind().types().contains(MobType.CRITTER)) return;
            ItemBlock block = block(active);
            if (block == null || !ready(player, block)) return;
            String plain = AbilityText.plain(block.text());
            Heals.give(player, player, number(GROWTH_HEAL, plain, 1) / 100 * PlayerHealth.max(player));
            double max = number(GROWTH_MAX, plain, 100);
            for (Worn.Piece piece : active.pieces()) {
                EquipmentSlot slot = slot(piece);
                if (slot != null && ItemCounters.get(piece.tag(), HeldStats.GROWTH_HEALTH) < max) ItemCounters.addWorn(player, slot, HeldStats.GROWTH_HEALTH, 1);
            }
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            double max = number(GROWTH_MAX, text(active), 100);
            for (Worn.Piece piece : active.pieces()) stats.add(Stat.HEALTH, Math.min(max, ItemCounters.get(piece.tag(), HeldStats.GROWTH_HEALTH)));
        }
    }

    /** The armor slot a worn piece is in; null for equipment. */
    static EquipmentSlot slot(Worn.Piece piece) {
        SpecificItemType type = piece.item().specificItemType();
        return switch (type) {
            case HELMET -> EquipmentSlot.HEAD;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    /**
     * The Spider's, Tarantula and Primordial Boots' Double Jump: "Allows you to double jump by sneaking mid air!", for
     * the block's mana (50, 40, 20): once each time they leave the ground. How far it throws them is UNKNOWN.
     */
    static final class DoubleJump extends Passive {
        DoubleJump() {
            super("Double Jump");
        }

        @Override
        public void sneaked(Player player, Active active, boolean sneaking) {
            if (!sneaking || player.isOnGround() || player.isFlying() || JUMPED.contains(player.getUniqueId())) return;
            ItemBlock block = block(active);
            if (block == null) return;
            Worn.Piece piece = active.pieces().get(0);
            int cost = Abilities.manaCost(block, PlayerSession.of(player).maxMana(), player, piece.tag());
            if (Mana.get(player) < cost) {
                PlayerSession.of(player).setManaReplacement(Replacement.forMillis("§c§lNOT ENOUGH MANA", 2000));
                return;
            }
            if (cost > 0) {
                Mana.spend(player, cost, block.name());
                PlayerSession.of(player).setDefenseReplacement(Replacement.forMillis("§b-" + cost + " Mana (§6" + block.name() + "§b)", 400));
            }
            JUMPED.add(player.getUniqueId());
            Vector forward = player.getLocation().getDirection().setY(0);
            if (forward.lengthSquared() > 0) forward.normalize().multiply(JUMP_FORWARD);
            player.setVelocity(player.getVelocity().setX(forward.getX()).setZ(forward.getZ()).setY(JUMP_UP));
            player.setFallDistance(0);
        }
    }

    /** Every tick: who's back on the ground can double jump again. */
    static void landed() {
        if (JUMPED.isEmpty()) return;
        JUMPED.removeIf(id -> {
            Player player = org.bukkit.Bukkit.getPlayer(id);
            return player == null || player.isOnGround();
        });
    }

    /**
     * The Tank Miner Armor's Depth Coating ("Doubles this piece's ❈ Defense while on Mining Islands.") and the Heat
     * Armor's Pressurized Coating ("Triple this piece's ❈ Defense and ❁ Strength while on Mining Islands."): each
     * worn piece's own stats of those, times its factor, on the Dwarven Mines (the only Mining Island here).
     */
    static final class Coating extends Passive {
        private static final Pattern FACTOR = Pattern.compile("(Doubles|Triple)s? this piece's (.+?) while on Mining Islands");

        Coating(String name) {
            super(name);
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            if (Dungeons.getSkyBlockServer() == null || Dungeons.getSkyBlockServer().getServerType() != ServerType.DWARVEN_MINES) return;
            for (Worn.Piece piece : active.pieces()) {
                ItemBlock block = pieceBlock(piece, name());
                Matcher m = block == null ? null : FACTOR.matcher(AbilityText.plain(block.text()));
                if (m == null || !m.find() || piece.stack() == null) continue;
                double more = (m.group(1).startsWith("Double") ? 2 : 3) - 1;
                Stats own = ItemStats.of(piece.stack(), player);
                for (Stat stat : new Stat[] {Stat.DEFENSE, Stat.STRENGTH}) {
                    if (m.group(2).contains(stat.getDisplayName())) stats.add(stat, own.get(stat) * more);
                }
            }
        }
    }

    /**
     * The Bone Necklace's Gladiator's Will: "Gain +3❈ Defense for each enemy within 10 blocks up to +30❈ Defense. Range
     * increases to 30 blocks and the cap increases to +60❈ Defense when you play as a Tank in dungeons." (its text's
     * numbers). An enemy is one of SkyBlock's mobs that can be hurt. Its "Increases the range of your Diversion passive
     * by 15 blocks" is LATER: the Tank's Diversion isn't built.
     */
    static final class GladiatorsWill extends Passive {
        GladiatorsWill() {
            super("Gladiator's Will");
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            String plain = text(active);
            Matcher m = GLADIATOR.matcher(plain);
            if (!m.find()) return;
            double each = Double.parseDouble(m.group(1));
            double range = Double.parseDouble(m.group(2));
            double cap = Double.parseDouble(m.group(3));
            Matcher tank = GLADIATOR_TANK.matcher(plain);
            if (tank.find() && playingTank(player)) {
                range = Double.parseDouble(tank.group(1));
                cap = Double.parseDouble(tank.group(2));
            }
            stats.add(Stat.DEFENSE, Math.min(cap, each * Hits.near(player.getLocation(), range).size()));
        }

        private static boolean playingTank(Player player) {
            if (!RunManager.inRun(player)) return false;
            User user = User.ifLoaded(player.getUniqueId());
            return user != null && DungeonProfile.selectedClass(user) == DungeonClass.TANK;
        }
    }

    /** The Ghast Cloaks' Blazing Restoration: "Restores +5❤ Health every second while worn." (the Vanquished one's +15). */
    static final class BlazingRestoration extends Passive {
        BlazingRestoration() {
            super("Blazing Restoration");
        }

        @Override
        public void second(Player player, Active active) {
            Heals.give(player, player, number(RESTORES, text(active), 0));
        }
    }

    /** The Rancher's Boots' Farmer's Speed: while worn, their Speed is at most what's been set on them (see {@link FarmersSpeed}). */
    static final class FarmersSpeedCap extends Passive {
        FarmersSpeedCap() {
            super("Farmer's Speed");
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            int cap = FarmersSpeed.NONE;
            for (Worn.Piece piece : active.pieces()) cap = Math.min(cap, FarmersSpeed.cap(piece.tag()));
            if (cap < FarmersSpeed.NONE && stats.get(Stat.SPEED) > cap) stats.set(Stat.SPEED, cap);
        }
    }

    /** They've left: what's kept about them goes. */
    static void forget(UUID player) {
        forgetBones(player);
        JUMPED.remove(player);
    }

    /** The plugin is going: the bones shown go. */
    static void removeAll() {
        for (UUID player : List.copyOf(BONE_SHIELDS.keySet())) forgetBones(player);
    }
}
