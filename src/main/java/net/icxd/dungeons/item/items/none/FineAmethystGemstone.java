package net.icxd.dungeons.item.items.none;

import net.icxd.dungeons.item.*;
import net.icxd.dungeons.item.cost.*;
import net.icxd.dungeons.item.cost.coins.*;
import net.icxd.dungeons.item.cost.essence.*;
import net.icxd.dungeons.item.cost.item.*;
import net.icxd.dungeons.item.enums.*;
import net.icxd.dungeons.item.gemstone.*;
import net.icxd.dungeons.item.prestige.*;
import net.icxd.dungeons.item.requirement.*;
import net.icxd.dungeons.item.requirement.skill.*;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Material;

import java.awt.*;
import java.util.*;

public class FineAmethystGemstone implements SkyBlockItem {
    @Override public Material material() { return Material.SKULL_ITEM; }
    @Override public int durability() { return 3; }
    @Override public String skin() { return "ewogICJ0aW1lc3RhbXAiIDogMTYxODA4Mzg2Mjc2NCwKICAicHJvZmlsZUlkIiA6ICJmMGIzYmRkMjEwNDg0Y2VlYjZhNTQyYmZiOGEyNTdiMiIsCiAgInByb2ZpbGVOYW1lIiA6ICJBbm9uaW1ZVFQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2ExZWU1ZmZjZTA0ZWI3ZGE1OTJkNDI0MTRmZjM1ZTFiZjM4MTk0ZDZiODJlMzEwZGJjNjI2MWI0N2ZiOWM5MSIKICAgIH0KICB9Cn0"; }
    @Override public String name() { return "Fine Amethyst Gemstone"; }
    @Override public Rarity rarity() { return Rarity.RARE; }
    @Override public double npcSellPrice() { return 19200; }
    @Override public String id() { return "FINE_AMETHYST_GEM"; }
}
