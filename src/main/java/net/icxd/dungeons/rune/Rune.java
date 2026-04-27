package net.icxd.dungeons.rune;

import lombok.Getter;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

@Getter
public enum Rune {
    GRAND_SEARING("Grand Searing", ChatColor.RED, Rarity.LEGENDARY, 0, List.of(SpecificItemType.CHESTPLATE), Arrays.asList("&7Special look on the Crimson", "&7Isle! Obtained from Fire Sales."), new RuneFunctionality() {
        @Override
        public void apply(Player player, int level, int ticks) {
            Location location = player.getLocation();

            double x = location.getX();
            double z = location.getZ();

            // should go in a circle around the player, and the particles should go up during it's duration and kind of go from the center to the outside
            for (int i = 0; i < 360; i += 360 / 2) {
                double angle = i * Math.PI / 180;
                angle += ticks / 20.0;
                double newX = x + 1 * Math.cos(angle);
                double newZ = z + 1 * Math.sin(angle);

                PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.FLAME, true, (float) newX, (float) location.getY() + (ticks % 20) / 20.0f / 2, (float) newZ, 0, 0, 0, 0, 1);
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
            }
        }
    }),
    ;

    private final String name;
    private final ChatColor color;
    private final Rarity rarity;
    private final int requiredLevel;
    private final List<GenericItemType> genericItemTypes;
    private final List<SpecificItemType> specificItemTypes;
    private final List<String> description;
    private final RuneFunctionality runeFunctionality;

    Rune(String name, ChatColor color, Rarity rarity, int requiredLevel, List<SpecificItemType> specificItemTypes, List<String> description, RuneFunctionality runeFunctionality) {
        this.name = name;
        this.color = color;
        this.rarity = rarity;
        this.requiredLevel = requiredLevel;
        this.genericItemTypes = null;
        this.specificItemTypes = specificItemTypes;
        this.description = description;
        this.runeFunctionality = runeFunctionality;
    }

    abstract static class RuneFunctionality {
        public abstract void apply(Player player, int level, int ticks);
    }
}
