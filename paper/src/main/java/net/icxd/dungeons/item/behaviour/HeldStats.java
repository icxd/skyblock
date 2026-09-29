package net.icxd.dungeons.item.behaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.collection.CollectionData;
import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.instance.RunItems;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.item.ItemCounters;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;

/**
 * Weapons whose stats grow with something of their holder's, or with what the weapon has counted (ABILITIES_WEAPONS.md,
 * "Weapon passives"): each as its own text says, read from that text where it has the numbers, and the text's
 * count lines kept up to date where the weapon counts ({@link ItemCounters}). What holding one does is on the
 * holder's finished stats (see {@link ItemBehaviour#whileHeld(Player, NBTTagCompound, Stats)}), so the Stats menu
 * and every hit have it. A count line is only rewritten once there's a count, so a new item's text is its data's.
 */
public final class HeldStats {
    /** Counts kept on items (see {@link ItemCounters}), for what counts them (item/ability/weapons, utility). */
    public static final String FEL_SWORD_KILLS = "fel_sword_kills";
    public static final String HURRICANE_KILLS = "hurricane_kills";
    public static final String COMMANDER_WHIP_ZOMBIES = "commander_whip_zombies";
    /** The run the whip's zombies were killed in (see RunItems#runKey). */
    public static final String COMMANDER_WHIP_RUN = "commander_whip_run";
    public static final String PROMISING_PICKAXE_BLOCKS = "promising_pickaxe_blocks";
    public static final String GROWTH_HEALTH = "growth_health";
    /** Minutes a Training Weights has been in an inventory (see item/ability/utility's TrainingWeights). */
    public static final String TRAINING_WEIGHTS_MINUTES = "training_weights_minutes";

    private HeldStats() {
    }

    /** The Strength a Training Weights gives now, for the minutes it has been held (see {@link Weights}). */
    public static int trainingWeightsStrength(NBTTagCompound tag) {
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        return Weights.strength(ItemCounters.get(tag, TRAINING_WEIGHTS_MINUTES), item == null ? Weights.MAX : Weights.max(item.lore()));
    }

    /** How many arrows a Hurricane Bow's shot is with this many kills (see {@link Tempest}). */
    public static int tempestArrows(double kills) {
        return Tempest.arrows(kills);
    }

    /** Hands each behaviour to {@code to}, by item id. */
    static void register(BiConsumer<String, ItemBehaviour> to) {
        ClassAdaptive adaptive = new ClassAdaptive();
        to.accept("STONE_BLADE", adaptive);
        to.accept("STARRED_STONE_BLADE", adaptive);
        to.accept("SHAMAN_SWORD", new PerMaxHealth());
        to.accept("GREAT_SPOOK_SWORD", new PerFear());
        to.accept("VOID_SWORD", new PerEnderPiece());
        to.accept("TACTICIAN_SWORD", new PerCombatCollection());
        to.accept("EMERALD_BLADE", new FromPurse());
        to.accept("FEL_SWORD", new FelSword());
        to.accept("ZOMBIE_COMMANDER_WHIP", new CommanderWhip());
        to.accept("HURRICANE_BOW", new Tempest());
        to.accept("PROMISING_PICKAXE", new StoredPotential());
        for (String piece : new String[] {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"}) to.accept("GROWTH_" + piece, new Growth());
        to.accept("TRAINING_WEIGHTS", new Weights());
    }

    /** The text's lines with the first that matches {@code line} replaced by {@code with} (given its match); as they were if none does. */
    static List<String> replace(List<String> lines, Pattern line, Function<Matcher, String> with) {
        List<String> out = null;
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = line.matcher(lines.get(i));
            if (!m.matches()) continue;
            out = new ArrayList<>(lines);
            out.set(i, with.apply(m));
            break;
        }
        return out == null ? lines : out;
    }

    /** The blocks with the named ABILITY block's text changed by {@code text}; as they were if it has none. */
    static List<ItemBlock> replaceText(List<ItemBlock> blocks, String name, UnaryOperator<List<String>> text) {
        List<ItemBlock> out = new ArrayList<>(blocks);
        for (int i = 0; i < out.size(); i++) {
            ItemBlock b = out.get(i);
            if (!b.isAbility() || !name.equals(b.name())) continue;
            List<String> changed = text.apply(b.text());
            if (changed != b.text()) out.set(i, new ItemBlock(b.kind(), b.name(), b.header(), b.activation(), changed, b.mana(),
                    b.manaPercent(), b.cooldown(), b.soulflow(), b.healthCost(), b.vitality(), b.pieces()));
        }
        return out;
    }

    /** A whole count as the text shows it ("1,234"). */
    static String count(double value) {
        return String.format(Locale.US, "%,d", (long) Math.floor(value));
    }

    /**
     * The Stone Blade: "As a weapon created by Scarf, it automatically adapts to its user inside Dungeons.
     * Berserk: +10❁ Damage, +75❁ Strength, +10✦ Speed / ... / Tank: +100❈ Defense, +5♨ Vitality, +5❂ True Defense":
     * in a run, the stats its text gives the class they play. The Healer's tether, the Mage's "melee attacks restore
     * 25% additional mana" and the Archer's arrows' bonus aren't stats (see WeaponPassives).
     */
    static final class ClassAdaptive implements ItemBehaviour {
        @Override
        public void whileHeld(Player holder, NBTTagCompound tag, Stats stats) {
            DungeonClass dungeonClass = playing(holder);
            if (dungeonClass == null) return;
            SkyBlockItem item = ItemRegistry.get(tag.getString("id"));
            if (item != null) stats.add(classStats(item.lore(), dungeonClass));
        }

        /** The class they play in the run they're in; null outside one (or while their data isn't here). */
        static DungeonClass playing(Player holder) {
            if (holder == null || !RunManager.inRun(holder)) return null;
            User user = User.ifLoaded(holder.getUniqueId());
            return user == null ? null : DungeonProfile.selectedClass(user);
        }

        /** The stats the text gives a class: those after "Berserk:" up to the next class's name. */
        static Stats classStats(List<String> lore, DungeonClass dungeonClass) {
            String plain = AbilityText.plain(lore);
            String start = dungeonClass.getDisplayName() + ":";
            int from = plain.indexOf(start);
            if (from < 0) return new Stats();
            int to = plain.length();
            for (DungeonClass other : DungeonClass.values()) {
                int at = plain.indexOf(other.getDisplayName() + ":", from + start.length());
                if (at >= 0) to = Math.min(to, at);
            }
            return AbilityText.stats(plain.substring(from + start.length(), to));
        }
    }

    /** The Shaman Sword's "Deal +1 Damage per 50 max ❤": a whole Damage for each whole 50 of their max health. */
    static final class PerMaxHealth implements ItemBehaviour {
        private static final Pattern PER = Pattern.compile("Deal \\+([\\d.]+) Damage per ([\\d,]+) max ❤");

        @Override
        public void whileHeld(Player holder, NBTTagCompound tag, Stats stats) {
            SkyBlockItem item = ItemRegistry.get(tag.getString("id"));
            Matcher m = item == null ? null : PER.matcher(AbilityText.plain(item.lore()));
            if (m == null || !m.find()) return;
            stats.add(Stat.DAMAGE, perWhole(stats.get(Stat.HEALTH), Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2).replace(",", ""))));
        }
    }

    /** {@code each} for every whole {@code per} of {@code amount}. */
    static double perWhole(double amount, double each, double per) {
        return per <= 0 ? 0 : each * Math.floor(Math.max(0, amount) / per + 1e-9);
    }

    /** The Great Spook Sword's "Gains +1❁ Damage and +1❁ Strength for every Fear you have". */
    static final class PerFear implements ItemBehaviour {
        @Override
        public void whileHeld(Player holder, NBTTagCompound tag, Stats stats) {
            double fear = Math.max(0, stats.get(Stat.FEAR));
            stats.add(Stat.DAMAGE, fear);
            stats.add(Stat.STRENGTH, fear);
        }
    }

    /**
     * The Void Sword's "Gain +20❁ Strength per piece of Ender Armor worn" (its text's number). Its "Current Bonus"
     * line stays its data's: an item's text isn't built for who holds it.
     */
    static final class PerEnderPiece implements ItemBehaviour {
        private static final Pattern PER = Pattern.compile("Gain \\+([\\d.]+)❁ Strength per piece of Ender Armor");
        /** Ender Armor's pieces ("Ender Helmet" is END_HELMET in the data). */
        private static final Set<String> ENDER_ARMOR = Set.of("END_HELMET", "END_CHESTPLATE", "END_LEGGINGS", "END_BOOTS");

        @Override
        public void whileHeld(Player holder, NBTTagCompound tag, Stats stats) {
            SkyBlockItem item = ItemRegistry.get(tag.getString("id"));
            Matcher m = item == null || holder == null ? null : PER.matcher(AbilityText.plain(item.lore()));
            if (m == null || !m.find()) return;
            int pieces = 0;
            for (ItemStack armor : holder.getInventory().getArmorContents()) {
                NBTTagCompound worn = ItemNBT.read(armor);
                if (worn != null && ENDER_ARMOR.contains(worn.getString("id"))) pieces++;
            }
            stats.add(Stat.STRENGTH, pieces * Double.parseDouble(m.group(1)));
        }
    }

    /**
     * The Tactician's Sword's "Gains +15 Damage for each Combat collection at Tier VII or higher" (its text's
     * numbers), from their collections; nothing while they aren't known.
     */
    static final class PerCombatCollection implements ItemBehaviour {
        private static final Pattern PER = Pattern.compile("Gains \\+([\\d.]+) Damage for each Combat collection at Tier ([IVXL]+) or higher");

        @Override
        public void whileHeld(Player holder, NBTTagCompound tag, Stats stats) {
            SkyBlockItem item = ItemRegistry.get(tag.getString("id"));
            Matcher m = item == null || holder == null ? null : PER.matcher(AbilityText.plain(item.lore()));
            User user = holder == null ? null : User.ifLoaded(holder.getUniqueId());
            CollectionData data = Collections.data();
            if (m == null || !m.find() || user == null || data == null) return;
            CollectionData.Category combat = data.category("COMBAT");
            if (combat == null) return;
            int tier = roman(m.group(2));
            int count = 0;
            for (String collection : combat.collections()) if (Collections.tier(user.profile(), collection) >= tier) count++;
            stats.add(Stat.DAMAGE, count * Double.parseDouble(m.group(1)));
        }
    }

    /** A Roman numeral's value (I to L). */
    static int roman(String numeral) {
        int total = 0;
        int previous = 0;
        for (int i = numeral.length() - 1; i >= 0; i--) {
            int value = switch (numeral.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                case 'L' -> 50;
                default -> 0;
            };
            total += value < previous ? -value : value;
            previous = Math.max(previous, value);
        }
        return total;
    }

    /**
     * The Emerald Blade: "This blade becomes stronger as you carry more coins in your purse (Capped at 2B coins)":
     * the wiki's "DMG = 130 + 2.5 x Coins^(1/4)" (its own 130 is its data's Damage), up to 2B coins. Its "Current
     * Damage Bonus" line stays its data's (an item's text isn't built for who holds it), and the Curse of Greed does
     * nothing: there's no coin loss on death here.
     */
    static final class FromPurse implements ItemBehaviour {
        static final double CAP = 2_000_000_000;

        @Override
        public void whileHeld(Player holder, NBTTagCompound tag, Stats stats) {
            User user = holder == null ? null : User.ifLoaded(holder.getUniqueId());
            if (user != null) stats.add(Stat.DAMAGE, bonus(Purse.coins(user)));
        }

        /** 2.5 x coins^(1/4), coins up to 2B. */
        static double bonus(double coins) {
            return 2.5 * Math.pow(Math.max(0, Math.min(coins, CAP)), 0.25);
        }
    }

    /**
     * The Fel Sword: "Every 100 Kills with this sword grants +1 Weapon Damage, up to 100 extra. Current Kills: 0 (+0
     * Damage)": kills counted on it (see WeaponPassives) under {@link #KILLS}.
     */
    static final class FelSword implements ItemBehaviour {
        static final String KILLS = FEL_SWORD_KILLS;
        private static final Pattern EVERY = Pattern.compile("Every (\\d+) Kills with this sword grants \\+(\\d+) Weapon Damage, up to (\\d+) extra");
        private static final Pattern LINE = Pattern.compile("(&7Current Kills:&a )[\\d,]+( &8\\(&c\\+)[\\d,]+( Damage&8\\))");

        @Override
        public void whileHeld(Player holder, NBTTagCompound tag, Stats stats) {
            SkyBlockItem item = ItemRegistry.get(tag.getString("id"));
            if (item != null) stats.add(Stat.DAMAGE, damage(item.lore(), ItemCounters.get(tag, KILLS)));
        }

        /** The Damage this many kills give, as the text says. */
        static double damage(List<String> lore, double kills) {
            Matcher m = EVERY.matcher(AbilityText.plain(lore));
            if (!m.find()) return 0;
            return Math.min(Double.parseDouble(m.group(3)), perWhole(kills, Double.parseDouble(m.group(2)), Double.parseDouble(m.group(1))));
        }

        @Override
        public List<String> lore(SkyBlockItem item, NBTTagCompound tag, List<String> lore) {
            if (!tag.hasKey(KILLS)) return lore;
            double kills = ItemCounters.get(tag, KILLS);
            return replace(lore, LINE, m -> m.group(1) + count(kills) + m.group(2) + count(damage(lore, kills)) + m.group(3));
        }
    }

    /**
     * The Zombie Commander Whip's Commander Whip: "Every 1 Zombies killed during a dungeon run by this weapon gives
     * the wielder +1 ❁ Strength": the kills are counted on it for the run they're in (see WeaponPassives), so a new
     * run starts from none; its "Zombies Killed" and "Bonus Strength" lines show the last run's.
     */
    static final class CommanderWhip implements ItemBehaviour {
        static final String KILLS = COMMANDER_WHIP_ZOMBIES;
        static final String RUN = COMMANDER_WHIP_RUN;
        private static final Pattern EVERY = Pattern.compile("Every (\\d+) Zombies killed during a dungeon run by this weapon gives the wielder \\+(\\d+) ❁ Strength");
        private static final Pattern KILLED = Pattern.compile("(&7Zombies Killed: &a)[\\d,]+");
        private static final Pattern BONUS = Pattern.compile("(&7Bonus Strength: &a)[\\d,]+");

        @Override
        public void whileHeld(Player holder, NBTTagCompound tag, Stats stats) {
            String run = holder == null ? null : RunItems.runKey(holder);
            if (run == null || !run.equals(tag.getString(RUN))) return;
            SkyBlockItem item = ItemRegistry.get(tag.getString("id"));
            if (item != null) stats.add(Stat.STRENGTH, strength(blocks(item), ItemCounters.get(tag, KILLS)));
        }

        private static List<String> blocks(SkyBlockItem item) {
            for (ItemBlock block : item.blocks()) if ("Commander Whip".equals(block.name())) return block.text();
            return List.of();
        }

        /** The Strength this many kills give, as the text says. */
        static double strength(List<String> text, double kills) {
            Matcher m = EVERY.matcher(AbilityText.plain(text));
            return m.find() ? perWhole(kills, Double.parseDouble(m.group(2)), Double.parseDouble(m.group(1))) : 0;
        }

        @Override
        public List<ItemBlock> blocks(SkyBlockItem item, NBTTagCompound tag, List<ItemBlock> blocks) {
            if (!tag.hasKey(KILLS)) return blocks;
            double kills = ItemCounters.get(tag, KILLS);
            return replaceText(blocks, "Commander Whip", text -> {
                List<String> out = replace(text, KILLED, m -> m.group(1) + count(kills));
                return replace(out, BONUS, m -> m.group(1) + count(strength(text, kills)));
            });
        }
    }

    /**
     * The Hurricane Bow's Tempest: "The more kills you get using this bow the more powerful it becomes! Reach 250
     * kills to unlock its full potential. Next Upgrade: Double Shot (0/20)": its kills under {@link #KILLS} (see
     * WeaponPassives), and how many arrows they make it shoot ({@link #arrows}).
     */
    static final class Tempest implements ItemBehaviour {
        static final String KILLS = HURRICANE_KILLS;
        /**
         * Kills for 2, 3, 4 and 5 arrows. 20 is its text's ("Double Shot (0/20)") and 250 "its full potential", 5 arrows
         * (the wiki's Hurricane Bow); 50 and 100 between them are UNKNOWN (no source gives them).
         */
        static final int[] STEPS = {20, 50, 100, 250};
        private static final String[] NAMES = {"Double Shot", "Triple Shot", "Quadruple Shot", "Penta Shot"};
        private static final Pattern NEXT = Pattern.compile("(&7Next Upgrade: &e)[^&]+( &8\\(&a)[\\d,]+(&7/&c)[\\d,]+(&8\\))");
        private static final Pattern KILLS_LINE = Pattern.compile("(&7Kills: &b)[\\d,]+");

        /** How many arrows a shot is with this many kills: 1, then one more at each step. */
        static int arrows(double kills) {
            int arrows = 1;
            for (int step : STEPS) if (kills >= step) arrows++;
            return arrows;
        }

        @Override
        public List<ItemBlock> blocks(SkyBlockItem item, NBTTagCompound tag, List<ItemBlock> blocks) {
            if (!tag.hasKey(KILLS)) return blocks;
            double kills = ItemCounters.get(tag, KILLS);
            int next = arrows(kills) - 1;
            return replaceText(blocks, "Tempest", text -> {
                List<String> out = replace(text, KILLS_LINE, m -> m.group(1) + count(kills));
                // UNKNOWN what the line says once it's all unlocked: it goes.
                if (next >= STEPS.length) {
                    List<String> without = new ArrayList<>(out);
                    without.removeIf(line -> NEXT.matcher(line).matches());
                    return without;
                }
                return replace(out, NEXT, m -> m.group(1) + NAMES[next] + m.group(2) + count(kills) + m.group(3) + count(STEPS[next]) + m.group(4));
            });
        }
    }

    /**
     * The Promising Pickaxe's Stored Potential: "Grants +10⸕ Mining Speed for every 100 blocks mined. (Max +250⸕
     * Mining Speed)": the blocks it has mined under {@link #BLOCKS} (see WeaponPassives), and its two count lines.
     */
    static final class StoredPotential implements ItemBehaviour {
        static final String BLOCKS = PROMISING_PICKAXE_BLOCKS;
        private static final Pattern GRANTS = Pattern.compile("Grants \\+([\\d.]+)⸕ Mining Speed for every ([\\d,]+) blocks mined\\. \\(Max \\+([\\d,]+)⸕ Mining Speed\\)");
        private static final Pattern MINED = Pattern.compile("(&7Blocks Mined: &a)[\\d,]+");
        private static final Pattern SPEED = Pattern.compile("(&7Mining Speed: &a)[\\d,]+");

        @Override
        public void whileHeld(Player holder, NBTTagCompound tag, Stats stats) {
            SkyBlockItem item = ItemRegistry.get(tag.getString("id"));
            if (item != null) stats.add(Stat.MINING_SPEED, speed(text(item), ItemCounters.get(tag, BLOCKS)));
        }

        private static List<String> text(SkyBlockItem item) {
            for (ItemBlock block : item.blocks()) if ("Stored Potential".equals(block.name())) return block.text();
            return List.of();
        }

        /** The Mining Speed this many blocks give, as the text says. */
        static double speed(List<String> text, double blocks) {
            Matcher m = GRANTS.matcher(AbilityText.plain(text));
            if (!m.find()) return 0;
            double per = Double.parseDouble(m.group(2).replace(",", ""));
            return Math.min(Double.parseDouble(m.group(3).replace(",", "")), perWhole(blocks, Double.parseDouble(m.group(1)), per));
        }

        @Override
        public List<ItemBlock> blocks(SkyBlockItem item, NBTTagCompound tag, List<ItemBlock> blocks) {
            if (!tag.hasKey(BLOCKS)) return blocks;
            double mined = ItemCounters.get(tag, BLOCKS);
            return replaceText(blocks, "Stored Potential", text -> {
                List<String> out = replace(text, MINED, m -> m.group(1) + count(mined));
                return replace(out, SPEED, m -> m.group(1) + count(speed(text, mined)));
            });
        }
    }

    /**
     * The Growth armor's pieces: "increases the ❤ Health bonus of a piece of the armor by 1 (Max 100)": each piece's
     * own count under {@link #HEALTH} (see the Growth bonus, item/ability/utility), shown on its "Bonus HP: 0/100" line.
     * The Health itself is the bonus's, while it's worn.
     */
    static final class Growth implements ItemBehaviour {
        static final String HEALTH = GROWTH_HEALTH;
        private static final Pattern LINE = Pattern.compile("(&7Bonus HP: &a)[\\d,]+(/[\\d,]+)");

        @Override
        public List<String> lore(SkyBlockItem item, NBTTagCompound tag, List<String> lore) {
            if (!tag.hasKey(HEALTH)) return lore;
            return replace(lore, LINE, m -> m.group(1) + count(ItemCounters.get(tag, HEALTH)) + m.group(2));
        }
    }

    /**
     * Training Weights: "The longer you hold this in your inventory, the stronger you'll become for 2 minutes after
     * removing it from your inventory! Max +50. Time Held: 0 Minutes Strength Gain: +1❁": the minutes it has been held
     * under {@link #TRAINING_WEIGHTS_MINUTES} (item/ability/utility counts them, and shatters it), shown on its two
     * lines.
     */
    static final class Weights implements ItemBehaviour {
        static final int MAX = 50;
        private static final Pattern MAX_TEXT = Pattern.compile("Max \\+(\\d+)");
        private static final Pattern HELD = Pattern.compile("(&fTime Held: &a)[\\d,]+( Minutes?)");
        private static final Pattern GAIN = Pattern.compile("(&fStrength Gain: &c\\+)[\\d,]+(❁)");

        /** Its "Max +50" (50 if it doesn't say). */
        static int max(List<String> lore) {
            Matcher m = MAX_TEXT.matcher(AbilityText.plain(lore));
            return m.find() ? Integer.parseInt(m.group(1)) : MAX;
        }

        /**
         * The Strength after {@code minutes} held, at most {@code max}: +1 from the start, and each +1 after takes 20
         * minutes more for each five before it (+2 at 40 minutes, then 20 more each to +5 at 100, 40 more each to +10
         * at 300, 60 more each to +15 at 600, ... +50 at 5,500): the wiki's Training Weights table follows this.
         */
        static int strength(double minutes, int max) {
            int strength = 1;
            double needed = 20;
            while (strength < max) {
                needed += 20 * (strength / 5 + 1);
                if (minutes < needed) break;
                strength++;
            }
            return strength;
        }

        @Override
        public List<String> lore(SkyBlockItem item, NBTTagCompound tag, List<String> lore) {
            if (!tag.hasKey(TRAINING_WEIGHTS_MINUTES)) return lore;
            double minutes = ItemCounters.get(tag, TRAINING_WEIGHTS_MINUTES);
            List<String> out = replace(lore, HELD, m -> m.group(1) + count(minutes) + m.group(2));
            return replace(out, GAIN, m -> m.group(1) + strength(minutes, max(lore)) + m.group(2));
        }
    }
}
