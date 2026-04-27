package net.icxd.dungeons.region;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.UUID;

@Getter
@Setter
public class Region {
    public static final HashMap<UUID, Region> regionCache = new HashMap<>();

    public RegionType type;
    public Region(RegionType type) { this.type = type; }

    @Override
    public String toString() {
        return type.getColor() + type.getName();
    }
}
