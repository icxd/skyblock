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

public class FlawedJasperGemstone implements SkyBlockItem {
    @Override public Material material() { return Material.SKULL_ITEM; }
    @Override public int durability() { return 3; }
    @Override public String skin() { return "ewogICJ0aW1lc3RhbXAiIDogMTYxODA4NjAwMzU1NywKICAicHJvZmlsZUlkIiA6ICJmZDYwZjM2ZjU4NjE0ZjEyYjNjZDQ3YzJkODU1Mjk5YSIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWFkIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2E3MzUxMWU1MDRjMzE2YjEzOWVkYjM1ZmViZTczZWY1OTFjMGY0NTVlOGNhZjllZTM1M2JjMTJiNmMxNGE5MjIiCiAgICB9CiAgfQp9"; }
    @Override public String name() { return "Flawed Jasper Gemstone"; }
    @Override public Rarity rarity() { return Rarity.UNCOMMON; }
    @Override public double npcSellPrice() { return 240; }
    @Override public String id() { return "FLAWED_JASPER_GEM"; }
}
