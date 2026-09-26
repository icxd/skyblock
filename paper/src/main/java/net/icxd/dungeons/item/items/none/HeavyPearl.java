package net.icxd.dungeons.item.items.none;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import org.bukkit.Material;

import java.util.List;

public class HeavyPearl implements SkyBlockItem {
    @Override public String id() { return "HEAVY_PEARL"; }
    @Override public String name() { return "Heavy Pearl"; }
    @Override public Material material() { return Material.PLAYER_HEAD; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public String skin() { return "ewogICJ0aW1lc3RhbXAiIDogMTY0MjUxOTMyMjgwOSwKICAicHJvZmlsZUlkIiA6ICI1NjY3NWIyMjMyZjA0ZWUwODkxNzllOWM5MjA2Y2ZlOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGVJbmRyYSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yZGJkNGU1ZDNkOWMwNWEwMzZmYjYyZTZlNzBmYWY5ZTZmOThkMjk0ZjlkMDA2NzgxYzE0NGM5ZjE1Yjg3NzE1IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="; }
    @Override public List<String> lore() {
        return List.of("&7It can take years for Hellwisp to", "&7create a pearl, but the Matriarch", "&7secretes so much fluid that it can",
                "&7make multiple of those pearls every", "&7day.");
    }
}
