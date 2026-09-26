package net.icxd.dungeons.item.items.none;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.requirement.Requirements;
import net.icxd.dungeons.item.requirement.hotm.HeartOfTheMountainRequirement;
import org.bukkit.Material;

import java.util.List;

public class PerfectlyCutFuelTank implements SkyBlockItem {
    @Override public String id() { return "PERFECTLY_CUT_FUEL_TANK"; }
    @Override public String name() { return "Perfectly-Cut Fuel Tank"; }
    @Override public Material material() { return Material.PLAYER_HEAD; }
    @Override public Rarity rarity() { return Rarity.RARE; }
    @Override public String skin() { return "ewogICJ0aW1lc3RhbXAiIDogMTYyNTQ5MjcxMjIxMiwKICAicHJvZmlsZUlkIiA6ICI5ZDIyZGRhOTVmZGI0MjFmOGZhNjAzNTI1YThkZmE4ZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTYWZlRHJpZnQ0OCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mN2U1NDFkZmI0YmExZjdkYzI4YjU0OGUzNDdhYmJkYzk4N2ViZTBlNjFjNDlmYTg3MTExZWYxYjJkY2IyMjE4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0"; }
    @Override public List<String> categories() { return List.of("Drill Part"); }
    @Override public List<String> lore() {
        return List.of("&2100,000 Max Fuel Capacity", "&a-10% Pickaxe Ability Cooldown", "",
                "&7Put this item in the &6Fuel Tank &7slot of", "&7a Drill at a &2Drill Mechanic&7!");
    }
    @Override public Requirements requirements() { return new Requirements(new HeartOfTheMountainRequirement(8)); }
}
