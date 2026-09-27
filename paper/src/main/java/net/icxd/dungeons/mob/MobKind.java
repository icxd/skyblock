package net.icxd.dungeons.mob;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.instance.DungeonTextures;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A kind of SkyBlock mob, as data (the table is {@link MobKinds}): what it's called, what entity it
 * is and what it wears, its mob types, how it fights, and its stats wherever it spawns: one
 * {@link Variant} per level it comes at on a floor, or one for outside the dungeons. {@link DataMob}
 * spawns one through {@link Mobs#spawn}.
 *
 * @param id              what it's registered as ("ZOMBIE_GRUNT"), Hypixel's id without the level
 * @param name            as it's shown ("Zombie Grunt"; the Hub's may have {@code &} colours)
 * @param skin            for a player-shaped mob, the key of its skin in dungeons/textures.json; else null
 * @param speed           its movement speed attribute (vanilla units, as Hypixel sends it); NaN for vanilla's
 * @param magicResistance the share of magic damage it resists (0.1 for 10%)
 * @param roomScaled      whether a room's health and damage multiplier applies to it (not to Undead Skeletons and
 *                        Crypt Undead, research mobs.md 1.3)
 * @param behaviour       makes the (per mob) behaviour: its goals and anything else it does
 */
public record MobKind(String id, String name, EntityType entityType, List<MobType> types, NameStyle style, Gear gear, String skin,
                      double speed, double magicResistance, boolean roomScaled, Supplier<? extends MobBehaviour> behaviour,
                      List<Variant> variants) {
    public MobKind {
        Objects.requireNonNull(id);
        types = List.copyOf(types);
        variants = List.copyOf(variants);
        if (variants.isEmpty()) throw new IllegalArgumentException(id + " has no variant");
    }

    /** How its name tag looks (research mobs.md 1.6 for the dungeon ones). */
    public enum NameStyle {
        /** The Hub's: "[Lv75] Magma Cube 1M/1M❤". */
        HUB,
        /** The Hub's, framed: "﴾ [Lv200] Bladesoul 50M/50M❤ ﴿". */
        BOSS,
        /** A dungeon mob's: its types, a star if starred, its name and its health, with no level or max. */
        DUNGEON,
        /** A dungeon mob's with its level and max health, as the Undead Skeleton's is. */
        LEVELED,
        /** A miniboss's: its name in bold light purple, big health in short (130k). */
        MINIBOSS
    }

    /**
     * Its stats at one level where it spawns: on a floor, or outside the dungeons ({@code floor} null).
     * {@code gear} is what it wears at this level if that differs from the kind's; null otherwise.
     */
    public record Variant(DungeonFloor floor, int level, double health, double damage, double defense, double combatXp, double coins,
                          List<MobDrop> drops, Gear gear) {
        public Variant {
            drops = List.copyOf(drops);
        }
    }

    /** Whether it's a dungeon mob (it spawns on floors, not outside). */
    public boolean dungeon() {
        return variants.get(0).floor() != null;
    }

    /** Its variants on this floor (outside the dungeons: null), lowest level first. */
    public List<Variant> variants(DungeonFloor floor) {
        return variants.stream().filter(v -> v.floor() == floor).toList();
    }

    /**
     * Its variant on this floor (outside the dungeons: null) at this level, or the floor's first (lowest)
     * for a null level; null if it has none there.
     */
    public Variant variant(DungeonFloor floor, Integer level) {
        for (Variant variant : variants(floor)) {
            if (level == null || variant.level() == level) return variant;
        }
        return null;
    }

    /** Where it spawns first: the Entrance's lowest level for a dungeon mob, its only one otherwise. */
    public Variant firstVariant() {
        return variants.get(0);
    }

    /** One piece of what it wears or holds: an item, dyed, as a head with a skin, with a glint. */
    public record Piece(Material material, Integer dye, String skin, boolean glint) {
        public static Piece of(Material material) {
            return new Piece(material, null, null, false);
        }

        public static Piece dyed(Material material, int rgb) {
            return new Piece(material, rgb, null, false);
        }

        /** A player head with the skin dungeons/textures.json has under this key. */
        public static Piece head(String skin) {
            return new Piece(Material.PLAYER_HEAD, null, skin, false);
        }

        public Piece glinted() {
            return new Piece(material, dye, skin, true);
        }

        /** The item (server only: it needs the item registry). */
        public ItemStack stack() {
            ItemStack stack = skin != null ? DungeonTextures.head(skin) : new ItemStack(material);
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return stack;
            if (dye != null && meta instanceof LeatherArmorMeta leather) leather.setColor(Color.fromRGB(dye));
            if (glint) meta.setEnchantmentGlintOverride(true);
            stack.setItemMeta(meta);
            return stack;
        }
    }

    /** What it holds and wears (null for nothing). */
    public record Gear(Piece hand, Piece helmet, Piece chestplate, Piece leggings, Piece boots) {
        public static final Gear NONE = new Gear(null, null, null, null, null);

        public static Gear holding(Material hand) {
            return new Gear(Piece.of(hand), null, null, null, null);
        }

        /** Leather armor in these colours, holding {@code hand} (null for nothing). */
        public static Gear leather(Piece hand, int helmet, int chestplate, int leggings, int boots) {
            return new Gear(hand, Piece.dyed(Material.LEATHER_HELMET, helmet), Piece.dyed(Material.LEATHER_CHESTPLATE, chestplate),
                    Piece.dyed(Material.LEATHER_LEGGINGS, leggings), Piece.dyed(Material.LEATHER_BOOTS, boots));
        }
    }

    public static Builder builder(String id, String name, EntityType entityType) {
        return new Builder(id, name, entityType);
    }

    /** For the table: everything but the id, name and entity type has a default. */
    public static final class Builder {
        private final String id;
        private final String name;
        private final EntityType entityType;
        private List<MobType> types = List.of();
        private NameStyle style = NameStyle.DUNGEON;
        private Gear gear = Gear.NONE;
        private String skin;
        private double speed = Double.NaN;
        private double magicResistance;
        private boolean roomScaled = true;
        private Supplier<? extends MobBehaviour> behaviour = () -> new MobBehaviour() {
        };
        private final List<Variant> variants = new ArrayList<>();

        private Builder(String id, String name, EntityType entityType) {
            this.id = id;
            this.name = name;
            this.entityType = entityType;
        }

        public Builder types(MobType... types) {
            this.types = List.of(types);
            return this;
        }

        public Builder style(NameStyle style) {
            this.style = style;
            return this;
        }

        public Builder gear(Gear gear) {
            this.gear = gear;
            return this;
        }

        public Builder skin(String skin) {
            this.skin = skin;
            return this;
        }

        public Builder speed(double speed) {
            this.speed = speed;
            return this;
        }

        public Builder magicResistance(double magicResistance) {
            this.magicResistance = magicResistance;
            return this;
        }

        /** A room's multiplier leaves its health and damage alone. */
        public Builder notRoomScaled() {
            this.roomScaled = false;
            return this;
        }

        public Builder behaviour(Supplier<? extends MobBehaviour> behaviour) {
            this.behaviour = behaviour;
            return this;
        }

        public Builder variant(DungeonFloor floor, int level, double health, double damage, double defense, double combatXp, double coins,
                               MobDrop... drops) {
            return variant(floor, level, health, damage, defense, combatXp, coins, null, drops);
        }

        public Builder variant(DungeonFloor floor, int level, double health, double damage, double defense, double combatXp, double coins,
                               Gear gear, MobDrop... drops) {
            variants.add(new Variant(floor, level, health, damage, defense, combatXp, coins, List.of(drops), gear));
            return this;
        }

        public MobKind build() {
            return new MobKind(id, name, entityType, types, style, gear, skin, speed, magicResistance, roomScaled, behaviour, variants);
        }
    }
}
