package net.icxd.dungeons.item.ability.abilities;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.ability.AbilityType;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class InstantlyShoots extends Ability {
    private final int arrows;
    public InstantlyShoots(int arrowAmount) {
        super("Instantly shoots!", AbilityType.SHORTBOW, AbilityActivation.LEFT_CLICK, "&7Shoots &b"+arrowAmount+" &7arrows at once. Can damage endermen.", 0, 0, 0);
        this.arrows = arrowAmount;
        setShowManaCost(false);
    }

    @Override
    public void activate(Player player, SkyBlockItem item) {
        Location location = player.getEyeLocation();
        for (int i = 0; i < arrows; i++) {
            Location l = location.clone();
            l.setYaw(location.getYaw() + (i * 10) - (arrows * 5) + 5);
            Arrow a = player.getWorld().spawnArrow(l.clone().add(l.getDirection().multiply(0.7)), l.getDirection(), 5, 1);
            a.setShooter(player);
            a.setMetadata("dmgEndermen", new FixedMetadataValue(Dungeons.getInstance(), 1));
        }
    }
}
