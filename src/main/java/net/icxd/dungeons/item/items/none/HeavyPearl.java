package net.icxd.dungeons.item.items.none;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.Soulbound;
import org.bukkit.Material;

public class HeavyPearl implements SkyBlockItem {
    @Override public Material material() { return Material.SKULL_ITEM; }
    @Override public int durability() { return 3; }
    @Override public String skin() { return "ewogICJ0aW1lc3RhbXAiIDogMTY0MjUxOTMyMjgwOSwKICAicHJvZmlsZUlkIiA6ICI1NjY3NWIyMjMyZjA0ZWUwODkxNzllOWM5MjA2Y2ZlOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGVJbmRyYSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yZGJkNGU1ZDNkOWMwNWEwMzZmYjYyZTZlNzBmYWY5ZTZmOThkMjk0ZjlkMDA2NzgxYzE0NGM5ZjE1Yjg3NzE1IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="; }
    @Override public String name() { return "Heavy Pearl"; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public String description() { return "%%gray%%%%italic%%It can take years for Hellwisp to create a pearl, but the Matriarch secretes so much fluid that it can make multiple of those pearls every day."; }
    @Override public Soulbound soulbound() { return Soulbound.SOLO; }
}
