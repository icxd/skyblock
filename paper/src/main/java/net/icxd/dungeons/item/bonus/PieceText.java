package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.combat.VanillaDamage;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.ability.weapons.Hits;
import net.icxd.dungeons.item.ability.weapons.Magic;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What armor and equipment pieces' own text (not a bonus block) says they do while worn, as far as the plugin's
 * systems go (BONUSES.md, "Items' own text"): the hats (Creeper, Chicken, Zombie, Skeleton, Slime, Rabbit, Cow), the
 * Obsidian Chestplate, Rampart and Super Heavy Armor, the Catacombs boss heads, the Clover Helmet, the Farming-level
 * boots and helmet, and the equipment's (the DOJO belts, the Annihilation and Destruction Cloaks, the Balloon Snake,
 * the Demonlord Gauntlet, the Lava Shell Necklace, the Mithril and Titanium equipment's Mithril fortune and the
 * Dragonfuse Glove). Numbers are the text's.
 */
final class PieceText {
    private PieceText() {
    }

    static List<Bonus> all() {
        return List.of(new CreeperHat(), new ChickenHead(), new ZombieHat(), new SkeletonHat(), new SlimeHat(),
                new Jumps(), new CowHead(), new ObsidianChestplate(), new Rampart(), new SuperHeavy(), new BossHeads(), new CloverHelmet(),
                new FarmingLevels(), new DojoBelts(), new HealingCloaks(), new DemonlordGauntlet(), new LavaShellNecklace(),
                new MithrilFortune(), new DragonfuseGlove());
    }

    /** A bonus in the own text of the items it names. */
    abstract static class Own implements Bonus {
        private final Predicate<String> ids;

        Own(Predicate<String> ids) {
            this.ids = ids;
        }

        Own(String... ids) {
            this(Set.of(ids)::contains);
        }

        @Override
        public String kind() {
            return ITEM;
        }

        @Override
        public boolean item(String id) {
            return ids.test(id);
        }

        /** A worn piece's own text. */
        static List<String> lore(Worn.Piece piece) {
            return piece.item().lore();
        }

        /** The first worn piece's own text. */
        static List<String> lore(Active active) {
            return active.pieces().isEmpty() ? List.of() : lore(active.pieces().get(0));
        }
    }

    /** The Creeper Hat's "Grants immunity to explosion damage" ("only applies to vanilla Minecraft explosions", the wiki). */
    static final class CreeperHat extends Own {
        CreeperHat() {
            super("CREEPER_HAT");
        }

        @Override
        public String name() {
            return "Creeper Hat";
        }

        @Override
        public boolean immune(Player player, Active active, EntityDamageEvent.DamageCause cause) {
            return cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
        }
    }

    /** The Chicken Head's "Also reduces fall damage by 5%". Its eggs when sneaking are only a look (not here). */
    static final class ChickenHead extends Own {
        ChickenHead() {
            super("CHICKEN_HEAD");
        }

        @Override
        public String name() {
            return "Chicken Head";
        }

        @Override
        public double vanillaDamage(Player player, Active active, VanillaDamage.Cause cause) {
            return cause == VanillaDamage.Cause.FALL ? 1 - BonusText.after(lore(active), "fall damage by", 5) / 100 : 1;
        }
    }

    /**
     * The Zombie Hat's "Gives +10 ❈ Defense for each Zombie within 8 blocks": mobs that are vanilla zombies (as for
     * Training, UNKNOWN), counted each second.
     */
    static final class ZombieHat extends Own {
        private final Map<UUID, Integer> near = new HashMap<>();

        ZombieHat() {
            super("ZOMBIE_HAT");
        }

        @Override
        public String name() {
            return "Zombie Hat";
        }

        @Override
        public void second(Player player, Active active) {
            double range = BonusText.after(lore(active), "within", 8);
            int zombies = 0;
            for (LivingEntity mob : Bonuses.mobsNear(player, range)) if (mob.getType() == EntityType.ZOMBIE) zombies++;
            near.put(player.getUniqueId(), zombies);
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            Integer zombies = near.get(player.getUniqueId());
            if (zombies != null && zombies > 0) stats.add(Stat.DEFENSE, zombies * BonusText.after(lore(active), "Gives", 10));
        }

        @Override
        public void ended(Player player) {
            near.remove(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            near.remove(player);
        }
    }

    /**
     * The Skeleton Hat's "Your arrows have a 20% chance to explode on impact dealing 50 base Magic Damage to enemies
     * within 8 blocks": the hat's ability's damage (its scaling with Intelligence UNKNOWN: 1, the usual), to each mob
     * around the one the arrow hit.
     */
    static final class SkeletonHat extends Own {
        static final double SCALING = 1;

        SkeletonHat() {
            super("SKELETON_HAT");
        }

        @Override
        public String name() {
            return "Skeleton Hat";
        }

        @Override
        public void landed(Player player, Active active, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            if (landing.kind() != HitKind.ARROW) return;
            List<String> lore = lore(active);
            if (ThreadLocalRandom.current().nextDouble() * 100 >= BonusText.after(lore, "have a", 20)) return;
            Worn.Piece hat = active.pieces().get(0);
            Magic.Spell spell = new Magic.Spell(BonusText.after(lore, "dealing", 50), SCALING, true, false);
            LivingEntity at = landing.entity();
            at.getWorld().spawnParticle(Particle.EXPLOSION, at.getLocation().add(0, 1, 0), 1);
            double range = BonusText.after(lore, "within", 8);
            Hits.spell(player, hat.item(), hat.tag(), spell, Hits.near(at.getLocation(), range));
        }
    }

    /** The Slime Hat's "Grants immunity to knockback from mobs." */
    static final class SlimeHat extends Own {
        SlimeHat() {
            super("SLIME_HAT");
        }

        @Override
        public String name() {
            return "Slime Hat";
        }

        @Override
        public double knockbackResistance(Player player, Active active, Entity by) {
            return PlayerDamage.attacker(by) instanceof Player ? 0 : 1;
        }
    }

    /** The Rabbit Hat's "Grants Jump Boost IV while equipped" and the Balloon Snake's "Jump Boost II", given again every second. */
    static final class Jumps extends Own {
        private static final Pattern JUMP = Pattern.compile("Jump Boost ([IVX]+)");

        Jumps() {
            super("RABBIT_HAT", "BALLOON_SNAKE");
        }

        @Override
        public String name() {
            return "Jump Boost";
        }

        /** The highest Jump Boost the pieces' text gives, its level (IV: 4); 0 for none. */
        static int level(List<Worn.Piece> pieces) {
            int best = 0;
            for (Worn.Piece piece : pieces) {
                Matcher m = JUMP.matcher(AbilityText.plain(lore(piece)));
                if (m.find()) best = Math.max(best, roman(m.group(1)));
            }
            return best;
        }

        @Override
        public void second(Player player, Active active) {
            int level = level(active.pieces());
            if (level > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, level - 1, true, false, true));
        }

        @Override
        public void ended(Player player) {
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        }
    }

    /** I to X. */
    static int roman(String numeral) {
        int total = 0;
        for (int i = 0; i < numeral.length(); i++) {
            int value = switch (numeral.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                default -> 0;
            };
            int next = i + 1 < numeral.length() ? switch (numeral.charAt(i + 1)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                default -> 0;
            } : 0;
            total += value < next ? -value : value;
        }
        return total;
    }

    /** The Cow Head's "Grants immunity to de-buffs while equipped": no harmful potion effect is given them. */
    static final class CowHead extends Own {
        CowHead() {
            super("COW_HEAD");
        }

        @Override
        public String name() {
            return "Cow Head";
        }

        @Override
        public boolean blocksEffect(Player player, Active active, PotionEffectType type) {
            return type.getEffectCategory() == PotionEffectType.Category.HARMFUL;
        }
    }

    /**
     * The Obsidian Chestplate: "While worn, gain +1 ✦ Speed for every 20 pieces of Obsidian in your inventory!"
     * (Obsidian or Enchanted Obsidian, the wiki: not in storage or sacks), counted each second, and "Immune to Wither
     * effect." Its "Bonus Speed" line stays the data's (lore isn't built for a holder's inventory).
     */
    static final class ObsidianChestplate extends Own {
        private final Map<UUID, Integer> obsidian = new HashMap<>();

        ObsidianChestplate() {
            super("OBSIDIAN_CHESTPLATE");
        }

        @Override
        public String name() {
            return "Obsidian Chestplate";
        }

        /** How many pieces of Obsidian (the block, or the SkyBlock item Enchanted Obsidian) these stacks hold. */
        static int obsidian(ItemStack[] contents) {
            int count = 0;
            for (ItemStack stack : contents) {
                if (stack == null || stack.getType() != Material.OBSIDIAN) continue;
                NBTTagCompound tag = ItemNBT.read(stack);
                String id = tag == null ? null : tag.getString("id");
                if (id == null || id.isEmpty() || id.equalsIgnoreCase("OBSIDIAN") || id.equalsIgnoreCase("ENCHANTED_OBSIDIAN")) count += stack.getAmount();
            }
            return count;
        }

        @Override
        public void second(Player player, Active active) {
            obsidian.put(player.getUniqueId(), obsidian(player.getInventory().getStorageContents()));
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            Integer count = obsidian.get(player.getUniqueId());
            if (count == null) return;
            stats.add(Stat.SPEED, Math.floor(count / BonusText.after(lore(active), "every", 20)) * BonusText.after(lore(active), "gain", 1));
        }

        @Override
        public boolean blocksEffect(Player player, Active active, PotionEffectType type) {
            return type == PotionEffectType.WITHER;
        }

        @Override
        public void ended(Player player) {
            obsidian.remove(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            obsidian.remove(player);
        }
    }

    /** Each Rampart piece: "Grants +50❤ Health, +20❁ Strength, and +15☠ Crit Damage while on the Crimson Isle." */
    static final class Rampart extends Own {
        Rampart() {
            super(id -> DungeonSets.armorOf(id, "RAMPART"));
        }

        @Override
        public String name() {
            return "Rampart";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            if (!Bonuses.on(ServerType.CRIMSON_ISLE)) return;
            for (Worn.Piece piece : active.pieces()) stats.add(BonusText.stats(lore(piece)));
        }
    }

    /**
     * Each Super Heavy piece: "Each piece of this armor reduces the cooldown of Seismic Wave by 1s" (the Tank's
     * ability asks for it: {@link SetBonuses#seismicWaveCut}).
     */
    static final class SuperHeavy extends Own {
        SuperHeavy() {
            super(id -> DungeonSets.armorOf(id, "SUPER_HEAVY"));
        }

        @Override
        public String name() {
            return "Super Heavy";
        }

        long cutMillis(Active active) {
            return (long) (active.count() * BonusText.after(lore(active), "by", 1) * 1000);
        }
    }

    /**
     * The Catacombs boss heads (gold and diamond Bonzo to Necron): "Grants 2x stats on The Catacombs Floor I": the
     * head's stats (as they are in the run) once more on its floor, Master Mode's too (UNKNOWN).
     */
    static final class BossHeads extends Own {
        private static final Pattern FLOOR = Pattern.compile("Floor ([IVX]+)");

        BossHeads() {
            super(id -> (id.startsWith("GOLD_") || id.startsWith("DIAMOND_")) && id.endsWith("_HEAD"));
        }

        @Override
        public String name() {
            return "Boss heads";
        }

        private final Map<String, Integer> floors = new HashMap<>();

        /** The floor its text names; -1 for none. */
        static int floor(List<String> lore) {
            String plain = AbilityText.plain(lore);
            Matcher m = FLOOR.matcher(plain);
            return m.find() && plain.contains("2x stats") ? roman(m.group(1)) : -1;
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            DungeonRun run = RunManager.of(player);
            if (run == null) return;
            for (Worn.Piece piece : active.pieces()) {
                int floor = floors.computeIfAbsent(piece.id(), id -> floor(lore(piece)));
                if (floor == run.floorNumber()) stats.add(ItemStats.of(piece.stack(), player));
            }
        }
    }

    /**
     * The Clover Helmet: "Magic Find: +5% / All Other Stats: -5%", "reduces most other Stats by 5% (multiplicative)"
     * and "does not reduce Breaking Power" (the wiki): Magic Find x1.05, the rest x0.95 (UNKNOWN which "most").
     */
    static final class CloverHelmet extends Own {
        CloverHelmet() {
            super("CLOVER_HELMET");
        }

        @Override
        public String name() {
            return "Clover Helmet";
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            apply(stats, BonusText.after(lore(active), "Magic Find:", 5), BonusText.after(lore(active), "All Other Stats: -", 5));
        }

        static void apply(Stats stats, double more, double less) {
            for (Stat stat : Stat.values()) {
                if (!stats.has(stat) || stat == Stat.BREAKING_POWER) continue;
                stats.set(stat, stats.get(stat) * (stat == Stat.MAGIC_FIND ? 1 + more / 100 : 1 - less / 100));
            }
        }
    }

    /**
     * Stats by Farming level: the Farmer and Rancher's Boots' "+2❈ Defense and +4✦ Speed for every Farming Skill
     * level you have" and the Enchanted Jack o' Lantern's "+2 ❈ Defense / +4 ❤ Health" a level. Their Garden and
     * Farming Fortune parts wait for farming.
     */
    static final class FarmingLevels extends Own {
        FarmingLevels() {
            super("FARMER_BOOTS", "RANCHERS_BOOTS", "ENCHANTED_JACK_O_LANTERN");
        }

        @Override
        public String name() {
            return "Farming levels";
        }

        private final Map<String, Stats> perLevel = new HashMap<>();

        /** What one of these pieces gives a Farming level: every "+N Stat" in its text before its Garden part, not Farming Fortune. */
        static Stats perLevel(List<String> lore) {
            String plain = AbilityText.plain(lore);
            int garden = plain.indexOf("Garden");
            Stats stats = AbilityText.stats(garden >= 0 ? plain.substring(0, garden) : plain);
            stats.set(Stat.FARMING_FORTUNE, 0);
            return stats;
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            int level = Skills.level(player, Skill.FARMING);
            if (level <= 0) return;
            for (Worn.Piece piece : active.pieces()) {
                Stats each = perLevel.computeIfAbsent(piece.id(), id -> perLevel(lore(piece)));
                for (Stat stat : Stat.values()) if (each.get(stat) != 0) stats.add(stat, each.get(stat) * level);
            }
        }
    }

    /** The DOJO belts' "Reduces damage taken by 6%" (0.5% to 6%), on every hit. */
    static final class DojoBelts extends Own {
        DojoBelts() {
            super(id -> id.startsWith("DOJO_") && id.endsWith("_BELT"));
        }

        @Override
        public String name() {
            return "Dojo belts";
        }

        @Override
        public double taken(Player player, Active active) {
            double factor = 1;
            for (Worn.Piece piece : active.pieces()) factor *= 1 - BonusText.after(lore(piece), "taken by", 0) / 100;
            return factor;
        }
    }

    /** The Annihilation Cloak's "Heal 25❤ every 1s while worn" and the Destruction Cloak's 15. */
    static final class HealingCloaks extends Own {
        HealingCloaks() {
            super("ANNIHILATION_CLOAK", "DESTRUCTION_CLOAK");
        }

        @Override
        public String name() {
            return "Healing cloaks";
        }

        @Override
        public void second(Player player, Active active) {
            for (Worn.Piece piece : active.pieces()) PlayerHealth.heal(player, BonusText.after(lore(piece), "Heal", 0));
        }
    }

    /** The Demonlord Gauntlet's "Deal 1.15x damage against ♨ Infernal Mobs", a multiplicative factor. */
    static final class DemonlordGauntlet extends Own {
        DemonlordGauntlet() {
            super("DEMONLORD_GAUNTLET");
        }

        @Override
        public String name() {
            return "Demonlord Gauntlet";
        }

        @Override
        public Combat.HitBuff hit(Player player, Active active, boolean ranged, Damage.Target target) {
            return target.types().contains(MobType.INFERNAL) ? new Combat.HitBuff(0, BonusText.after(lore(active), "Deal", 1)) : null;
        }
    }

    /**
     * The Lava Shell Necklace's "Sets your ❣ Health Regen and ☄ Mending to 0": both, after the rest. Its "you can
     * no longer receive healing" would need every heal to ask (UNKNOWN which heals Hypixel stops): not here.
     */
    static final class LavaShellNecklace extends Own {
        LavaShellNecklace() {
            super("LAVA_SHELL_NECKLACE");
        }

        @Override
        public String name() {
            return "Lava Shell Necklace";
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            stats.set(Stat.HEALTH_REGEN, 0).set(Stat.MENDING, 0);
        }
    }

    /**
     * The Mithril equipment's "Grants +5☘ Mining Fortune while mining Mithril" and the Titanium's "+10 ... while
     * mining Mithril and Titanium": Dwarven Metal Fortune, which is Mining Fortune on Mithril (the only one of them
     * here).
     */
    static final class MithrilFortune extends Own {
        MithrilFortune() {
            super("MITHRIL_NECKLACE", "MITHRIL_CLOAK", "MITHRIL_BELT", "MITHRIL_GAUNTLET", "TITANIUM_NECKLACE", "TITANIUM_CLOAK", "TITANIUM_BELT",
                    "TITANIUM_GAUNTLET");
        }

        @Override
        public String name() {
            return "Mithril fortune";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            for (Worn.Piece piece : active.pieces()) stats.add(Stat.DWARVEN_METAL_FORTUNE, BonusText.after(lore(piece), "Grants", 0));
        }
    }

    /**
     * The Dragonfuse Glove: "While equipped, upgrades the Aspect of the Dragons with: +35❁ Damage +50❁ Strength": while
     * the Aspect of the Dragons is held. Its "Very reduced ability knockback" is the ability's (not here).
     */
    static final class DragonfuseGlove extends Own {
        static final String SWORD = "ASPECT_OF_THE_DRAGON";

        DragonfuseGlove() {
            super("DRAGONFUSE_GLOVE");
        }

        @Override
        public String name() {
            return "Dragonfuse Glove";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            if (!SWORD.equals(SetBonuses.held(player))) return;
            Stats more = BonusText.stats(lore(active));
            stats.add(Stat.DAMAGE, more.get(Stat.DAMAGE)).add(Stat.STRENGTH, more.get(Stat.STRENGTH));
        }
    }
}
