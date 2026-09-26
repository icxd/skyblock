package net.icxd.dungeons.item.ability.abilities.scrolls;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.ability.AbilityType;
import org.bukkit.entity.Player;

public class ShadowWarp extends Ability {
    public ShadowWarp() {
        super("Shadow Warp", AbilityType.ABILITY, AbilityActivation.RIGHT_CLICK, "&7Created a special distortion &e10 &7blocks ahead of you that sucks all enemies around it. Using this ability again within &e5 &7seconds to detonate the warp and deal damage to enemies near it.", 10, 300, 0);
    }

    @Override
    public void activate(Player player, SkyBlockItem item) {

    }
}
