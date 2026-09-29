package net.icxd.dungeons.item.ability.weapons;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import net.icxd.dungeons.dungeons.instance.RunItems;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.ability.Activations;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.Absorption;

/**
 * Dungeon ghosts' abilities (the wiki's Ghosts: "Players may only interact with the world ... through the use of
 * their respective Ghost Abilities depending on their chosen Class"). A ghost gets its items when it becomes one
 * (Haunt, and its class's: see RunItems), and "may only attack Dungeon Mobs ... with Ghost Abilities, with the
 * Spirit Sword, provided they had it in their inventory while alive, with the Spirit Shortbow" (the Spirit items'
 * "When turned into a ghost, this item becomes a Ghost Ability."). A run calls off everything a ghost clicks
 * before anything else sees it, so their clicks come here ({@link #clicked}) and are used as every ability is
 * ({@link Activations#use}: cooldowns and all). The Tank's Stun Potion and Absorption Potion are thrown and act
 * where they land; the Spirit items' Spirit Bomb flies. Haunt is the utility abilities' (it's a Spirit Leap).
 * Main thread.
 */
final class GhostAbilities {
    /** A Spirit item's line: it's a ghost ability for a ghost. */
    static final String SPIRIT_ITEM = "Spirit Item: When turned into a ghost, this item becomes a Ghost Ability.";
    static final String SPIRIT_BOMB = "Spirit Bomb";
    /** The Spirit Sword's 10 seconds, for a Spirit item whose block has no cooldown (the Spirit Shortbow's: UNKNOWN). */
    static final double SPIRIT_BOMB_COOLDOWN = 10;
    /** "temporarily stuns": how long is UNKNOWN (3 seconds). */
    static final int STUN_TICKS = 60;
    /** "10 block radius", if the text doesn't say. */
    private static final double RADIUS = 10;
    /** Absorption Potion: "+10 Absorption from Ghost Absorption Potion" a Tank level (the Fandom wiki's Tank: 700 at 50). */
    static final double ABSORPTION_PER_LEVEL = 10;
    /** A thrown potion: its speed, how fast it falls, how far it goes (UNKNOWN, a hand throw's). */
    private static final double THROW_SPEED = 0.8;
    private static final double THROW_GRAVITY = 0.05;
    private static final double THROW_RANGE = 24;
    /** The spirit's speed and range (UNKNOWN). */
    private static final double SPIRIT_SPEED = 1.2;
    private static final double SPIRIT_RANGE = 32;

    private static final Pattern RADIUS_TEXT = Pattern.compile("in a (\\d+) block radius");
    private static final Pattern ABSORPTION = Pattern.compile("([\\d,]+) HP worth of absorption for (\\d+) seconds");
    private static final Pattern SPIRIT = Pattern.compile("deals ([\\d,]+) damage on impact");

    private GhostAbilities() {
    }

    /** Whether the item's text says it's a Spirit item. */
    static boolean spiritItem(SkyBlockItem item) {
        return AbilityText.plain(item.lore()).contains(SPIRIT_ITEM);
    }

    /**
     * A ghost's right click: with a ghost ability item, its ability; with a Spirit item, its Spirit Bomb. Nothing
     * for anyone who isn't a ghost in a running run (their clicks are the usual ones).
     */
    static void clicked(Player player) {
        if (!RunItems.ghost(player)) return;
        ItemStack held = player.getInventory().getItemInMainHand();
        NBTTagCompound tag = ItemNBT.read(held);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null) return;
        List<ItemBlock> blocks = ItemBehaviours.of(item).blocks(item, tag, item.blocks());
        AbilityHandler.Trigger click = AbilityHandler.Trigger.click(AbilityActivation.RIGHT_CLICK, true, null);
        if (RunItems.ghostItem(item.id())) {
            for (ItemBlock block : blocks) {
                if (block.isAbility() && AbilityActivation.of(block.activation()) == AbilityActivation.RIGHT_CLICK && Abilities.get(block.name()) != null) {
                    Activations.use(player, item, tag, block, click);
                    return;
                }
            }
        } else if (spiritItem(item)) {
            for (ItemBlock block : blocks) {
                if (block.isAbility() && SPIRIT_BOMB.equals(block.name())) {
                    Activations.use(player, item, tag, withCooldown(block), click);
                    return;
                }
            }
        }
    }

    /** The block with the Spirit Sword's cooldown where it has none. */
    static ItemBlock withCooldown(ItemBlock block) {
        if (block.cooldown() > 0) return block;
        return new ItemBlock(block.kind(), block.name(), block.header(), block.activation(), block.text(), block.mana(), block.manaPercent(),
                SPIRIT_BOMB_COOLDOWN, block.soulflow(), block.healthCost(), block.vitality(), block.pieces());
    }

    /** "in a 10 block radius": its text's number. */
    static double radius(ItemBlock block) {
        Matcher m = RADIUS_TEXT.matcher(AbilityText.plain(block.text()));
        return m.find() ? Double.parseDouble(m.group(1)) : RADIUS;
    }

    /** A ghost ability that only a ghost in a running run uses. */
    abstract static class GhostOnly implements AbilityHandler {
        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            return RunItems.ghost(player);
        }
    }

    /** A potion thrown in an arc from their eyes, which acts where it breaks (on a block, a mob or the end of its flight). */
    private static void throwPotion(Player player, Color colour, Landed landed) {
        Location eye = player.getEyeLocation();
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setColor(colour);
        potion.setItemMeta(meta);
        new Missile(player, eye, eye.getDirection().multiply(THROW_SPEED))
                .byGhost()
                .gravity(THROW_GRAVITY)
                .range(THROW_RANGE)
                .look(Missile.display(eye, potion, 0.5f, 0))
                .onEnd((missile, where, impact) -> {
                    where.getWorld().spawnParticle(Particle.ENTITY_EFFECT, where, 40, 1.5, 0.5, 1.5, 1, colour);
                    where.getWorld().playSound(where, Sound.ENTITY_SPLASH_POTION_BREAK, 1, 1);
                    landed.at(where);
                })
                .launch();
        player.getWorld().playSound(eye, Sound.ENTITY_SPLASH_POTION_THROW, 1, 1);
    }

    @FunctionalInterface
    private interface Landed {
        void at(Location where);
    }

    /**
     * The Tank's Stun Potion: "Throw a potion which temporarily stuns all monsters in a 10 block radius." Every one
     * of SkyBlock's mobs in reach of where it breaks is stunned ({@link Hits#stun}: rooted, its hits do nothing), but
     * bosses (the Archer's Stun Bow "does not affect bosses"; UNKNOWN for this one); how long is UNKNOWN (3 s).
     */
    static final class StunPotion extends GhostOnly {
        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            double radius = radius(block);
            throwPotion(player, Color.GRAY, where -> {
                for (LivingEntity mob : Hits.near(where, radius)) {
                    Mobs.Live live = Mobs.of(mob);
                    if (live != null && live.type().isBoss()) continue;
                    Hits.stun(mob, STUN_TICKS);
                }
            });
        }
    }

    /**
     * The Tank's Absorption Potion: "Throw a potion which gives all teammates in a 10 block radius 200 HP worth of
     * absorption for 3 seconds." The wiki's Ghosts has it "multiplied by your Tank level * 10", which the Fandom
     * wiki's Tank puts as "+10 Absorption from Ghost Absorption Potion" a level (700 at 50): 200 + 10 x their Tank
     * level, to each living teammate in reach of where it breaks.
     */
    static final class AbsorptionPotion extends GhostOnly {
        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Matcher m = ABSORPTION.matcher(AbilityText.plain(block.text()));
            boolean said = m.find();
            double base = said ? Double.parseDouble(m.group(1).replace(",", "")) : 200;
            long millis = said ? Long.parseLong(m.group(2)) * 1000 : 3000;
            double amount = absorption(base, RunItems.classLevel(player));
            double radius = radius(block);
            throwPotion(player, Color.YELLOW, where -> {
                for (Player teammate : RunItems.aliveTeammatesNear(player, where, radius)) Absorption.give(teammate, block.name(), amount, millis);
            });
        }
    }

    /** "200 HP worth", and 10 more a Tank level. */
    static double absorption(double base, int tankLevel) {
        return base + ABSORPTION_PER_LEVEL * Math.max(0, tankLevel);
    }

    /**
     * The Spirit Sword's and Spirit Shortbows' Spirit Bomb, as a ghost: "Shoots a spirit that deals 8000 damage on
     * impact." A spirit flies from their eyes, and the first mob it touches takes its text's damage as magic damage
     * with no Intelligence scaling (UNKNOWN, as its look, speed and whether it's a blast are: here only what it hits).
     */
    static final class SpiritBomb extends GhostOnly {
        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Matcher m = SPIRIT.matcher(AbilityText.plain(block.text()));
            Magic.Spell spell = new Magic.Spell(m.find() ? Double.parseDouble(m.group(1).replace(",", "")) : 8000, 0);
            Location eye = player.getEyeLocation();
            new Missile(player, eye, eye.getDirection().multiply(SPIRIT_SPEED))
                    .byGhost()
                    .range(SPIRIT_RANGE)
                    .trail(at -> at.getWorld().spawnParticle(Particle.SOUL, at, 2, 0.1, 0.1, 0.1, 0.01))
                    .onHit((missile, mob) -> {
                        Hits.spell(missile.caster(), item, tag, spell, List.of(mob));
                        mob.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, mob.getLocation().add(0, mob.getHeight() / 2, 0), 20, 0.3, 0.3, 0.3, 0.05);
                        return false;
                    })
                    .launch();
            player.getWorld().playSound(eye, Sound.PARTICLE_SOUL_ESCAPE, 1, 1);
        }
    }
}
