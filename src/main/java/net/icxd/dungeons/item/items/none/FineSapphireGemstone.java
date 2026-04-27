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

public class FineSapphireGemstone implements SkyBlockItem {
    @Override public Material material() { return Material.SKULL_ITEM; }
    @Override public int durability() { return 3; }
    @Override public String skin() { return "ewogICJ0aW1lc3RhbXAiIDogMTYxODA4MzkwNDYxNSwKICAicHJvZmlsZUlkIiA6ICJmMjU5MTFiOTZkZDU0MjJhYTcwNzNiOTBmOGI4MTUyMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJmYXJsb3VjaDEwMCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zNjE2MWRhYTM1ODllYzljODE4NzQ1OWFjMzZmZDRkZDI2NDZjMDQwNjc4ZDNiZmFjYjcyYTIyMTBjNmM4MDFjIgogICAgfQogIH0KfQ"; }
    @Override public String name() { return "Fine Sapphire Gemstone"; }
    @Override public Rarity rarity() { return Rarity.RARE; }
    @Override public double npcSellPrice() { return 19200; }
    @Override public String id() { return "FINE_SAPPHIRE_GEM"; }
}
