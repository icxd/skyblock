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

public class RoughAmethystGemstone implements SkyBlockItem {
    @Override public Material material() { return Material.SKULL_ITEM; }
    @Override public int durability() { return 3; }
    @Override public String skin() { return "ewogICJ0aW1lc3RhbXAiIDogMTYxODA4NTQ0OTk5NCwKICAicHJvZmlsZUlkIiA6ICIxYTc1ZTNiYmI1NTk0MTc2OTVjMmY4NTY1YzNlMDAzZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUZXJvZmFyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2U0OTNjNmY1NDBjNzAwMWZlZDk3YjA3ZjZiNGM4OTEyOGUzYTdjMzc1NjNhYTIyM2YwYWNjYTMxNGYxNzU1MTUiCiAgICB9CiAgfQp9"; }
    @Override public String name() { return "Rough Amethyst Gemstone"; }
    @Override public double npcSellPrice() { return 3; }
    @Override public String id() { return "ROUGH_AMETHYST_GEM"; }
}
