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

public class FlawedSapphireGemstone implements SkyBlockItem {
    @Override public Material material() { return Material.SKULL_ITEM; }
    @Override public int durability() { return 3; }
    @Override public String skin() { return "ewogICJ0aW1lc3RhbXAiIDogMTYxODA4NjIwMzYzNiwKICAicHJvZmlsZUlkIiA6ICI4OGU0YWNiYTQwOTc0YWZkYmE0ZDM1YjdlYzdmNmJmYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJKb2FvMDkxNSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84YTBhZjk5ZThkODcwMzE5NGE4NDdhNTUyNjhjZjVlZjRhYzRlYjNiMjRjMGVkODY1NTEzMzlkMTBiNjQ2NTI5IgogICAgfQogIH0KfQ"; }
    @Override public String name() { return "Flawed Sapphire Gemstone"; }
    @Override public Rarity rarity() { return Rarity.UNCOMMON; }
    @Override public double npcSellPrice() { return 240; }
    @Override public String id() { return "FLAWED_SAPPHIRE_GEM"; }
}
