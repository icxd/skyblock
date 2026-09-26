package net.icxd.dungeons.attributes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.crimsonisle.kuudra.KuudraTier;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

@AllArgsConstructor
@Getter
public enum Attribute {
    // TODO: Arachno
    ATTACK_SPEED("Attack Speed", List.of(GenericItemType.WEAPON), List.of("§7Grants §e+%s⚔ Attack Speed§7."), KuudraTier.HOT, level -> new Stats().set(Stat.ATTACK_SPEED, level)),
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
    MAGIC_FIND("Magic Find", Arrays.asList(GenericItemType.ARMOR, GenericItemType.EQUIPMENT), List.of("§7Grants §b+%s✯ Magic Find§7."), KuudraTier.INFERNAL, level -> new Stats().set(Stat.MAGIC_FIND, 0.5 * level)),
    MANA_POOL("Mana Pool", Arrays.asList(GenericItemType.ARMOR, GenericItemType.EQUIPMENT), List.of("§7Grants §b+%s✎ Intelligence§7."), KuudraTier.NONE, level -> new Stats().set(Stat.INTELLIGENCE, 20* level)),
    // TODO: Mana Regeneration
    MENDING("Mending", Arrays.asList(GenericItemType.ARMOR, GenericItemType.EQUIPMENT), List.of("§7Grants §a+%s☄ Mending§7."), KuudraTier.BURNING, level -> new Stats().set(Stat.MENDING, 3 * level)),
    VITALITY("Vitality", Arrays.asList(GenericItemType.ARMOR, GenericItemType.EQUIPMENT), List.of("§7Grants §4+%s♨ Vitality§7."), KuudraTier.BURNING, level -> new Stats().set(Stat.VITALITY, 3 * level)),
    SPEED("Speed", Arrays.asList(GenericItemType.ARMOR, GenericItemType.EQUIPMENT), List.of("§7Grants §f+%s✦ Speed§7."), KuudraTier.NONE, level -> new Stats().set(Stat.SPEED, 5 * level)),
    // TODO: Undead Resistance
    VETERAN("Veteran", Arrays.asList(GenericItemType.ARMOR, GenericItemType.EQUIPMENT), List.of("§7Grants §3+%s☯ Combat Wisdom§7."), KuudraTier.BURNING, level -> new Stats().set(Stat.COMBAT_WISDOM, 0.75 * level)),
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

    /** By stored name; items made before the rename say "Veteran". */
    public static Attribute of(String name) {
        return "Veteran".equals(name) ? VETERAN : valueOf(name);
    }

    public Predicate<Player> requirement() {
        return player -> {
            User user = User.ifLoaded(player.getUniqueId());
            Integer highest = user == null ? null : user.get("crimsonIsle.kuudra.highest", Integer.class);
            return highest != null && highest >= requiredCompletion.getTier();
        };
    }
    /** Its description at a level, with the stat it grants filled in. */
    public List<String> getLore(int level) {
        Stats stats = statsFunction.apply(level);
        double value = Arrays.stream(Stat.values()).mapToDouble(stats::get).filter(v -> v != 0).findFirst().orElse(0);
        return description.stream().map(line -> String.format(line, Text.number(value))).toList();
    }

    /** One that can roll on this kind of item, other than {@code not} (null for any). */
    public static Attribute random(GenericItemType type, Attribute not) {
        List<Attribute> fitting = Arrays.stream(values())
                .filter(a -> a != not && (type == null || a.genericItemTypes.contains(type)))
                .toList();
        if (fitting.isEmpty()) fitting = Arrays.stream(values()).filter(a -> a != not).toList();
        return fitting.get(ThreadLocalRandom.current().nextInt(fitting.size()));
    }

}
