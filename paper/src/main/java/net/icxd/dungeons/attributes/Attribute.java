package net.icxd.dungeons.attributes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.icxd.dungeons.crimsonisle.kuudra.KuudraTier;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
public enum Attribute {
    // TODO: Arachno
    ATTACK_SPEED("Attack Speed", List.of(GenericItemType.WEAPON), Arrays.asList("§7Grants §e%s⚔ Bonus Attack", "§eSpeed§7."), KuudraTier.HOT, level -> new Stats().setAttackSpeed(level)),
    // TODO: Blazing
    // TODO: Combo
    // TODO: Elite
    // TODO: Ender
    // TODO: Ignition
    // TODO: Life Recovery
    // TODO: Mana Steal
    // TODO: Midas Touch
    // TODO: Undead
    // TODO: Warrior
    // TODO: Deadeye
    // TODO: Arachno Resistance
    // TODO: Blazing Resistance
    // TODO: Breeze
    // TODO: Dominance
    // TODO: Ender Resistance
    // TODO: Experience
    // TODO: Fortitude
    // TODO: Life Regeneration
    // TODO: Lifeline
    MAGIC_FIND("Magic Find", Arrays.asList(GenericItemType.ARMOR, GenericItemType.EQUIPMENT), List.of("§7Grants §b%s✯ Magic Find§7."), KuudraTier.INFERNAL, level -> new Stats().setMagicFind(0.5 * level)),
    MANA_POOL("Mana Pool", Arrays.asList(GenericItemType.ARMOR, GenericItemType.EQUIPMENT), List.of("§7Grants §b+%s✎ Intelligence§7."), KuudraTier.NONE, level -> new Stats().setIntelligence(20* level)),
    // TODO: Mana Regeneration
    MENDING("Mending", Arrays.asList(GenericItemType.ARMOR, GenericItemType.EQUIPMENT), List.of("§7Grants §a%s☄ Mending§7."), KuudraTier.BURNING, level -> new Stats().setMending(3 * level)),
    VITALITY("Vitality", Arrays.asList(GenericItemType.ARMOR, GenericItemType.EQUIPMENT), List.of("§7Grants §4%s♨ Vitality§7."), KuudraTier.BURNING, level -> new Stats().setVitality(3 * level)),
    SPEED("Speed", Arrays.asList(GenericItemType.ARMOR, GenericItemType.EQUIPMENT), List.of("§7Grants §f%s✦ Speed§7."), KuudraTier.NONE, level -> new Stats().setWalkSpeed(5 * level)),
    // TODO: Undead Resistance
    Veteran("Veteran", Arrays.asList(GenericItemType.ARMOR, GenericItemType.EQUIPMENT), List.of("§7Grants §3%s☯ Combat Wisdom§7."), KuudraTier.BURNING, level -> new Stats().setCombatWisdom(0.75 * level)),
    // TODO: Blazing Fortune
    // TODO: Fishing Experience
    // TODO: Infection
    // TODO: Double Hook
    // TODO: Fisherman
    // TODO: Fishing Speed
    // TODO: Hunter
    ;

    private final String name;
    private final List<GenericItemType> genericItemTypes;
    private final List<String> description;
    private final KuudraTier requiredCompletion;
    private final Function<Integer, Stats> statsFunction;
    private AttributeFunctionality attributeFunctionality = null;

    public Predicate<Player> requirement() {
        return player -> User.getUser(player.getUniqueId()).get("crimsonIsle.kuudra.highest", Integer.class) >= requiredCompletion.getTier();
    }
    public ArrayList<String> getLore(int level) {
        ArrayList<String> lore = new ArrayList<>();
        for (String s : description) {
            switch (this) {
                case ATTACK_SPEED -> s = String.format(s, level);
                case MAGIC_FIND -> s = String.format(s, 0.5 * level);
                case MANA_POOL -> s = String.format(s, 20 * level);
                case MENDING, VITALITY -> s = String.format(s, 3 * level);
                case SPEED -> s = String.format(s, 5 * level);
                case Veteran -> s = String.format(s, 0.75 * level);
            }
            lore.add(s);
        }
        return lore;
    }

    public static Attribute getRandomAttribute() {
        return Attribute.values()[new Random().nextInt(Attribute.values().length)];
    }
    public static Attribute getRandomAttributeButNot(Attribute attribute) {
        Attribute randomAttribute = Attribute.values()[new Random().nextInt(Attribute.values().length)];
        if (randomAttribute == attribute) {
            return getRandomAttributeButNot(attribute);
        }
        return randomAttribute;
    }

    static interface AttributeFunctionality {
        default void onEquip(Player player, SkyBlockItem skyBlockItem) {}
        default void onUnequip(Player player, SkyBlockItem skyBlockItem) {}
        default void onAttack(Player player, SkyBlockItem skyBlockItem, LivingEntity entity) {}
        default void onKill(Player player, SkyBlockItem skyBlockItem, LivingEntity entity) {}
    }
}
