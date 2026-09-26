package net.icxd.dungeons.item.ability.abilities.scrolls;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.ability.AbilityType;
import org.bukkit.entity.Player;

public class WitherImpact extends Ability {
    public WitherImpact() {
        super("Wither Impact", AbilityType.ABILITY, AbilityActivation.RIGHT_CLICK, "&7Teleports &a10 blocks &7ahead of you. Then implode dealing &c13,961.2 &7damage to nearby enemies. Also applies the wither shield scroll ability reducing damage taken and granting an absorption shield for &e5 &7seconds.", 0, 300, 0);
    }

    @Override
    public void activate(Player player, SkyBlockItem item) {

    }
}
