package net.icxd.dungeons.item.ability.abilities.scrolls;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.ability.AbilityType;
import org.bukkit.entity.Player;

public class ShadowWarp extends Ability {
    public ShadowWarp() {
        super("Shadow Warp", AbilityType.ABILITY, AbilityActivation.RIGHT_CLICK,
                "&7Creates a spatial distortion &e10\n&7blocks ahead of you that sucks all\n&7enemies around it. Use this ability\n&7again within &e5 &7seconds to detonate\n&7the warp and deal &c13,961.2 &7damage\n&7to enemies near it.", 10, 300, 0);
    }

    @Override
    public void activate(Player player, SkyBlockItem item) {

    }
}
