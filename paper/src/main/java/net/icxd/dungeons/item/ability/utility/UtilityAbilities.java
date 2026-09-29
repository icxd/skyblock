package net.icxd.dungeons.item.ability.utility;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import net.icxd.dungeons.item.ability.AbilityHandler;

/**
 * Abilities that don't hit: movement, healing, summons, tools and the rest, by ability name. What each does,
 * and where Hypixel's is only approximated, is in ABILITIES_UTILITY.md. What they need besides a click (every
 * tick, shields on hits, what's worn) is {@link UtilityListener}'s.
 */
public final class UtilityAbilities {
    /** The names registered here (see {@link #has}). */
    private static final Set<String> NAMES = new HashSet<>();

    private UtilityAbilities() {
    }

    /** Hands each of them to {@code registry}, by ability name as items' ABILITY blocks have it. */
    public static void register(BiConsumer<String, AbilityHandler> registry) {
        BiConsumer<String, AbilityHandler> to = (name, handler) -> {
            NAMES.add(name);
            registry.accept(name, handler);
        };
        // Dungeons
        to.accept(SpiritLeap.NAME, new SpiritLeap());
        to.accept("Aspiring Leap", new Refusal("&cYou can only use this item on your private island!"));
        to.accept(CreeperVeil.NAME, new CreeperVeil());
        to.accept(Shadowstep.NAME, new Shadowstep());
        to.accept("Haunt", new SpiritLeap.Haunt());
        to.accept("Echolocation", new SecretTracker());
        to.accept("Dungeon Breaker", new DungeonBreaker());
        to.accept("Tuning 4 Dummies", new HamRadio());
        to.accept("Try Your Luck", new ArchfiendDice());
        // Healing
        WandHeal wand = new WandHeal();
        for (String name : new String[] {"Small Heal", "Medium Heal", "Big Heal", "Huge Heal"}) to.accept(name, wand);
        InstantHeal instant = new InstantHeal();
        to.accept("Instant Heal", instant);
        to.accept("Extreme Measures", instant);
        // Buffs, deployables and taunts
        to.accept("Speed Boost", new TimedBuff(TimedBuff.Who.SELF, "entity.bat.takeoff"));
        to.accept("Howl", new TimedBuff(TimedBuff.Who.YOU_AND_NEARBY, "block.note_block.didgeridoo"));
        to.accept("Life Blood", new TimedBuff(TimedBuff.Who.ALLIES_FOR_HEALTH, "entity.player.hurt"));
        to.accept("Deploy", new Deployables());
        to.accept(Soulward.NAME, new Soulward());
        to.accept("Enrage", new Taunt("entity.ravager.roar"));
        to.accept("Jingle Bells", new Taunt("block.note_block.bell"));
        // Movement
        to.accept("Ether Transmission", new EtherTransmission());
        to.accept(Movement.WaterBurst.NAME, new Movement.WaterBurst());
        to.accept("To the Moon!", new Movement.ToTheMoon());
        // Shields
        to.accept(CellsAlignment.NAME, new CellsAlignment());
    }

    /** Whether the ability with this name is one of these (once they're registered). */
    static boolean has(String name) {
        return NAMES.contains(name);
    }
}
