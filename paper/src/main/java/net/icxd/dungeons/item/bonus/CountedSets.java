package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.economy.KillCoins;
import net.icxd.dungeons.item.ItemCounters;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Text;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bonuses that grow with what their wearer does, counted on the piece that shows it (the Book of Stats' way,
 * see {@link ItemCounters}; its lore lines follow the count, see {@link BonusCounters}): Zombie Commander
 * Armor's Training, the Bulwarks (Revenant's and Reaper's Zombie, Tarantula's and Primordial's Spider, Final
 * Destination's Enderman), Armor of Magma's Absorb (with Sponge's, in {@link OtherSets.Absorb}) and Eleanor's
 * Riches. Only armor counts: counting on equipment waits for a way to put a piece back into its slot.
 *
 * <p>What a "Zombie" is: a mob that is a vanilla zombie (the Entrance's Zombie Grunt, Tank Zombie and Crypt
 * Lurker), a question for the owner (UNKNOWN: the wiki links the Private Island Zombie only); Spiders and
 * Endermen the same way (none here yet), Magma Cubes as the Hub's is.
 */
final class CountedSets {
    /** Where the counts are kept on the pieces. */
    static final String ZOMBIE_KILLS = "zombie_kills";
    static final String SPIDER_KILLS = "spider_kills";
    static final String ENDERMAN_KILLS = "enderman_kills";
    static final String MAGMA_CUBE_KILLS = "magma_cube_kills";
    static final String SCAVENGER_COINS = "scavenger_coins";
    /** What follows a colour code's "&" (see {@link #startsWith}). */
    private static final String CODES = "0123456789abcdefklmnor";
    /** What a "Zombie" is (see the class). */
    static final Set<EntityType> ZOMBIES = Set.of(EntityType.ZOMBIE);

    private CountedSets() {
    }

    static List<Bonus> all() {
        return List.of(new Training(), new Bulwark("Zombie Bulwark", ZOMBIE_KILLS, ZOMBIES),
                new Bulwark("Spider Bulwark", SPIDER_KILLS, Set.of(EntityType.SPIDER, EntityType.CAVE_SPIDER)),
                new Bulwark("Enderman Bulwark", ENDERMAN_KILLS, Set.of(EntityType.ENDERMAN)), new Riches());
    }

    /** Whether the killed mob is one of these (a kind of their vanilla entity). */
    static boolean killedOne(SkyBlockMobDeathEvent event, Set<EntityType> types) {
        return event.kind() != null && types.contains(event.kind().entityType());
    }

    /** The armor slot a worn armor piece is in; null for equipment (whose counts can't be kept yet). */
    static EquipmentSlot slot(Worn.Piece piece) {
        SpecificItemType type = piece.item().specificItemType();
        return type == null ? null : switch (type) {
            case HELMET -> EquipmentSlot.HEAD;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    /** Adds to the count kept on a worn armor piece, and rebuilds it for them (its lines follow). */
    static void count(Player player, Worn.Piece piece, String key, double amount) {
        EquipmentSlot slot = slot(piece);
        if (slot != null && amount != 0) ItemCounters.addWorn(player, slot, key, amount);
    }

    /** The block with other text. */
    static ItemBlock withText(ItemBlock block, List<String> text) {
        return new ItemBlock(block.kind(), block.name(), block.header(), block.activation(), text, block.mana(), block.manaPercent(),
                block.cooldown(), block.soulflow(), block.healthCost(), block.vitality(), block.pieces());
    }

    /**
     * The lines with the one that starts {@code start} (after its colour codes) set to {@code line}; as they are without
     * one. A worn piece's blocks are worked out each tick, so the lines are compared as they are, with no copies made.
     */
    static List<String> line(List<String> text, String start, String line) {
        for (int i = 0; i < text.size(); i++) {
            if (!startsWith(text.get(i), start)) continue;
            List<String> out = new ArrayList<>(text);
            out.set(i, line);
            return out;
        }
        return text;
    }

    /** Whether the line, its colour codes ("&7", "§a") left out, starts with {@code start}. */
    static boolean startsWith(String line, String start) {
        int j = 0;
        for (int i = 0; i < line.length() && j < start.length(); i++) {
            char c = line.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < line.length() && CODES.indexOf(line.charAt(i + 1)) >= 0) {
                i++;
                continue;
            }
            if (c != start.charAt(j++)) return false;
        }
        return j == start.length();
    }

    /** Whole numbers grouped by thousands: "1,000". */
    static String whole(double value) {
        return Text.number(Math.floor(value));
    }

    /** A bonus whose lines show a count kept on its piece. */
    interface Counted {
        /** The name its blocks carry. */
        String name();

        /** Its block's text with {@code count} counted on this piece (as it is for a piece that's counted nothing). */
        List<String> counted(List<String> text, double count, SkyBlockItem item);

        /** Where it keeps its count. */
        String key();
    }

    /**
     * Zombie Commander Armor's Training: "Every 50 Zombies killed gives the wearer +5 ❤ Health while wearing the
     * set. Max 500." Kills while the set is worn, kept on the chestplate (its "Zombies Killed" and "Bonus HP"
     * lines), whole fifties.
     */
    static final class Training implements Bonus, Counted {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Training";
        }

        @Override
        public String key() {
            return ZOMBIE_KILLS;
        }

        /** The Health for this many kills, with the text's numbers: every {@code per}, {@code gain}, at most {@code most}. */
        static double health(double kills, double per, double gain, double most) {
            return per <= 0 ? 0 : Math.min(most, Math.floor(Math.max(0, kills) / per) * gain);
        }

        private static double health(ItemBlock block, double kills) {
            return health(kills, BonusText.after(block, "Every", 50), BonusText.after(block, "wearer", 5), BonusText.after(block, "Max", 500));
        }

        private static Worn.Piece keeper(Active active) {
            for (Worn.Piece piece : active.pieces()) if (piece.item().specificItemType() == SpecificItemType.CHESTPLATE) return piece;
            return null;
        }

        @Override
        public void killed(Player player, Active active, SkyBlockMobDeathEvent event) {
            Worn.Piece keeper = keeper(active);
            if (keeper != null && killedOne(event, ZOMBIES)) count(player, keeper, ZOMBIE_KILLS, 1);
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            Worn.Piece keeper = keeper(active);
            if (keeper == null) return;
            stats.add(Stat.HEALTH, health(BonusText.block(keeper, kind(), name()), ItemCounters.get(keeper.tag(), ZOMBIE_KILLS)));
        }

        @Override
        public List<String> counted(List<String> text, double count, SkyBlockItem item) {
            ItemBlock block = new ItemBlock(kind(), name(), null, null, text, 0, 0, 0, 0, 0, 0, 0);
            text = line(text, "Zombies Killed:", "&7Zombies Killed: &a" + whole(count));
            return line(text, "Bonus HP:", "&7Bonus HP: &a" + whole(health(block, count)));
        }
    }

    /**
     * A Bulwark (PIECE): "Kill Zombies to accumulate defense against them": each piece with it counts its
     * wearer's kills of its kind, and gives Defense against that kind by its kills, the private tables' steps
     * (see {@link BonusTables}; the wiki's, 14 of them for Zombies: +20 at 50 kills, ..., +315 at 500,000). Without
     * the tables, only the step its text shows ("Next Upgrade: +20❈ (0/50)"). Its "Piece Bonus" and "Next Upgrade"
     * lines follow; past the last step "Next Upgrade" says so (UNKNOWN how Hypixel words it).
     */
    static final class Bulwark implements Bonus, Counted {
        private final String name;
        private final String key;
        private final Set<EntityType> of;

        Bulwark(String name, String key, Set<EntityType> of) {
            this.name = name;
            this.key = key;
            this.of = of;
        }

        @Override
        public String kind() {
            return "PIECE";
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String key() {
            return key;
        }

        /** Its steps: the tables', or the one in the item's own text. */
        List<BonusTables.Step> steps(SkyBlockItem item) {
            List<BonusTables.Step> steps = BonusTables.get().killSteps(name);
            if (!steps.isEmpty()) return steps;
            ItemBlock own = null;
            for (ItemBlock block : item.blocks()) if (name.equals(block.name())) own = block;
            if (own == null) return List.of();
            return List.of(new BonusTables.Step(BonusText.after(own, "/", 50), BonusText.after(own, "Next Upgrade:", 20)));
        }

        @Override
        public void killed(Player player, Active active, SkyBlockMobDeathEvent event) {
            if (!killedOne(event, of)) return;
            for (Worn.Piece piece : active.pieces()) count(player, piece, key, 1);
        }

        @Override
        public double defenseAgainst(Player player, Active active, Entity by) {
            Entity mob = PlayerDamage.attacker(by);
            if (mob == null || !of.contains(mob.getType())) return 0;
            double defense = 0;
            for (Worn.Piece piece : active.pieces()) defense += BonusTables.reached(steps(piece.item()), ItemCounters.get(piece.tag(), key));
            return defense;
        }

        @Override
        public List<String> counted(List<String> text, double count, SkyBlockItem item) {
            List<BonusTables.Step> steps = steps(item);
            text = line(text, "Piece Bonus:", "&7Piece Bonus: &a+" + whole(BonusTables.reached(steps, count)) + "❈");
            BonusTables.Step next = BonusTables.next(steps, count);
            return line(text, "Next Upgrade:", next == null ? "&7Next Upgrade: &aMaxed!"
                    : "&7Next Upgrade: &a+" + whole(next.value()) + "❈ &8(&a" + whole(count) + "&7/&c" + whole(next.at()) + "&8)");
        }
    }

    /**
     * Eleanor's Armor's Riches (tiered, 2+): "Gain 1.5x Scavenger Coins", 2x with 3 pieces and 3x with 4 (the
     * wiki's Eleanor's Armor): the Scavenger coins of a kill, the enchantment's and the accessory's ("Scavenger
     * Coins are obtained from: Scavenger Enchantment and Scavenger Artifact accessory line"), times it (see
     * {@link KillCoins#addScavengerFactor}). Each worn piece's "Scavenger Coins Gained" counts what they made with
     * it on (UNKNOWN: all of them, not only Riches' extra).
     */
    static final class Riches implements Bonus, Counted {
        static final Tiers FACTOR = new Tiers(2, 1.5, 2, 3);

        @Override
        public String kind() {
            return SetKey.TIERED;
        }

        @Override
        public String name() {
            return "Riches";
        }

        @Override
        public int needs(SetKey set) {
            return 2;
        }

        @Override
        public String key() {
            return SCAVENGER_COINS;
        }

        @Override
        public void killed(Player player, Active active, SkyBlockMobDeathEvent event) {
            double coins = KillCoins.scavengerCoins(player, event.variant().level());
            for (Worn.Piece piece : active.pieces()) count(player, piece, SCAVENGER_COINS, coins);
        }

        @Override
        public List<String> text(List<String> text, int count) {
            return FACTOR.text(text, "&a", count, 2);
        }

        @Override
        public List<String> counted(List<String> text, double count, SkyBlockItem item) {
            return line(text, "Scavenger Coins Gained:", "&7Scavenger Coins Gained: &6" + whole(count));
        }
    }

    /** The factor Riches puts on their Scavenger coins now: 1 without 2 pieces of it on. */
    static double richesFactor(List<Bonus.Active> active) {
        for (Bonus.Active a : active) if (a.bonus() instanceof Riches) return Riches.FACTOR.at(a.count());
        return 1;
    }

    /** The counted bonuses' lines on a piece, by the pieces' ids: what {@link BonusCounters} rewrites. */
    static Map<String, Counted> byName() {
        Map<String, Counted> map = new java.util.HashMap<>();
        for (Bonus bonus : SetBonuses.all()) if (bonus instanceof Counted counted) map.put(counted.name(), counted);
        return map;
    }

    /** The count this piece keeps for this bonus. */
    static double count(NBTTagCompound tag, Counted counted) {
        return ItemCounters.get(tag, counted.key());
    }
}
