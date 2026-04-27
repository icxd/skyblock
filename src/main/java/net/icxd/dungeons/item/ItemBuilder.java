package net.icxd.dungeons.item;

import net.icxd.dungeons.attributes.Attribute;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.enchanting.Enchantment;
import net.icxd.dungeons.item.enums.*;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.requirement.Requirement;
import net.icxd.dungeons.item.requirement.dungeontier.DungeonTierRequirement;
import net.icxd.dungeons.item.requirement.hotm.HeartOfTheMountainRequirement;
import net.icxd.dungeons.item.requirement.skill.SkillRequirement;
import net.icxd.dungeons.item.requirement.slayer.SlayerRequirement;
import net.icxd.dungeons.reforge.Reforge;
import net.icxd.dungeons.reforge.ReforgeStat;
import net.icxd.dungeons.rune.Rune;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Utils;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.*;

public class ItemBuilder {

    public static ItemStack build(SkyBlockItem item) {
        return build(item, null);
    }

    public static ItemStack build(SkyBlockItem item, int amount) {
        ItemStack itemStack = build(item);
        itemStack.setAmount(amount);
        return itemStack;
    }

    public static ItemStack build(SkyBlockItem item, NBTTagCompound tag, int amount) {
        ItemStack itemStack = build(item, tag);
        itemStack.setAmount(amount);
        return itemStack;
    }

    public static ItemStack build(SkyBlockItem item, NBTTagCompound tag) {
        ItemStack stack = new ItemStack(item.material());
        stack.setDurability((short) item.durability());
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        if (tag == null) {
            tag = new NBTTagCompound();
            tag.setString("id", item.id());
            tag.setString("name", item.name());
            tag.setString("rarity", item.rarity().name());
            tag.setString("dungeon_star", DungeonStar.ZERO.name());
            tag.setString("specific_item_type", item.specificItemType().name());
            tag.setString("generic_item_type", item.genericItemType().name());
            tag.setString("soulbound", item.soulbound().name());
            tag.setBoolean("dungeon_item", item.dungeonItem());
            tag.setBoolean("can_have_attributes", item.canHaveAttributes());
            if (item.canHaveAttributes()) {
                Attribute attribute1 = Attribute.getRandomAttribute();
                Attribute attribute2 = Attribute.getRandomAttributeButNot(attribute1);
                tag.setString("attribute_1", attribute1.name());
                tag.setInt("attribute_1_level", Utils.random(1, 2));
                tag.setString("attribute_2", attribute2.name());
                tag.setInt("attribute_2_level", Utils.random(1, 2));
            }
            tag.setBoolean("recombobulated", false);
            tag.setInt("hot_potato_books", 0);
            tag.setBoolean("art_of_war", false);
            tag.setString("owner", "");
            tag.setString("reforge", "");
            tag.setString("rune", "");
            tag.setInt("rune_level", 0);
            tag.set("gemstone_slots", new NBTTagList());
            tag.set("enchantments", new NBTTagList());
            tag.setInt("upgrade_count", 0);
            if (item.nbt() != null) {
                for (String key : item.nbt().c()) {
                    tag.set(key, item.nbt().get(key));
                }
            }
            if (item.unstackable()) {
                tag.setString("uuid", UUID.randomUUID().toString());
            }
        }
        if (item.gemstoneSlots() != null && tag.getList("gemstone_slots", 10).isEmpty()) {
            NBTTagList gemstoneSlots = new NBTTagList();
            for (GemstoneSlot slot : item.gemstoneSlots().getSlots()) {
                NBTTagCompound gemstoneSlot = new NBTTagCompound();
                boolean locked = !slot.getCosts().isEmpty();
                gemstoneSlot.setBoolean("locked", locked);
                if (locked) {
                    NBTTagList costs = new NBTTagList();
                    for (Cost cost : slot.getCosts()) {
                        NBTTagCompound costTag = new NBTTagCompound();
                        if (cost instanceof CoinCost coinCost) {
                            costTag.setString("type", "coin");
                            costTag.setInt("amount", coinCost.getAmount());
                        } else if (cost instanceof ItemCost itemCost) {
                            costTag.setString("type", "item");
                            costTag.setString("item_id", itemCost.getItem().id());
                            costTag.setInt("amount", itemCost.getAmount());
                        } else if (cost instanceof EssenceCost essenceCost) {
                            costTag.setString("type", "essence");
                            costTag.setString("essence", essenceCost.getEssenceType().name());
                            costTag.setInt("amount", essenceCost.getAmount());
                        }
                        costs.add(costTag);
                    }
                    gemstoneSlot.set("costs", costs);
                }
                gemstoneSlots.add(gemstoneSlot);
            }
            tag.set("gemstone_slots", gemstoneSlots);
        }
        nmsStack.setTag(tag);
        stack = CraftItemStack.asBukkitCopy(nmsStack);

        if (item.color() != null) {
            LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
            meta.setColor(Color.fromRGB(item.color().getRed(), item.color().getGreen(), item.color().getBlue()));
            stack.setItemMeta(meta);
        } else if (item.material() == Material.SKULL_ITEM) {
            Utils.skull(stack, item.skin());
        }

        Rarity rarity = Rarity.valueOf(tag.getString("rarity"));

        ItemMeta meta = stack.getItemMeta();
        Reforge reforge = tag.getString("reforge").isEmpty() ? null : Reforge.valueOf(tag.getString("reforge"));
        meta.setDisplayName(rarity.getColor() + (reforge == null ? "" : reforge.getName() + " ") +
                item.name() + (switch (DungeonStar.valueOf(tag.getString("dungeon_star"))) {
            case ZERO -> "";
            case ONE -> ChatColor.GOLD + " ✪";
            case TWO -> ChatColor.GOLD + " ✪✪";
            case THREE -> ChatColor.GOLD + " ✪✪✪";
            case FOUR -> ChatColor.GOLD + " ✪✪✪✪";
            case FIVE -> ChatColor.GOLD + " ✪✪✪✪✪";
            case SIX -> ChatColor.GOLD + " ✪✪✪✪✪" + ChatColor.RED + "❶";
            case SEVEN -> ChatColor.GOLD + " ✪✪✪✪✪" + ChatColor.RED + "❷";
            case EIGHT -> ChatColor.GOLD + " ✪✪✪✪✪" + ChatColor.RED + "❸";
            case NINE -> ChatColor.GOLD + " ✪✪✪✪✪" + ChatColor.RED + "❹";
            case TEN -> ChatColor.GOLD + " ✪✪✪✪✪" + ChatColor.RED + "❺";
        }) + (switch (tag.getInt("upgrade_count")) {
            case 0 -> "";
            case 1 -> ChatColor.GOLD + " ✪";
            case 2 -> ChatColor.GOLD + " ✪✪";
            case 3 -> ChatColor.GOLD + " ✪✪✪";
            case 4 -> ChatColor.GOLD + " ✪✪✪✪";
            case 5 -> ChatColor.GOLD + " ✪✪✪✪✪";
            case 6 -> ChatColor.LIGHT_PURPLE + " ✪" + ChatColor.GOLD + "✪✪✪✪";
            case 7 -> ChatColor.LIGHT_PURPLE + " ✪✪" + ChatColor.GOLD + "✪✪✪";
            case 8 -> ChatColor.LIGHT_PURPLE + " ✪✪✪" + ChatColor.GOLD + "✪✪";
            case 9 -> ChatColor.LIGHT_PURPLE + " ✪✪✪✪" + ChatColor.GOLD + "✪";
            case 10 -> ChatColor.LIGHT_PURPLE + " ✪✪✪✪✪";
            case 11 -> ChatColor.AQUA + " ✪" + ChatColor.LIGHT_PURPLE + "✪✪✪✪";
            case 12 -> ChatColor.AQUA + " ✪✪" + ChatColor.LIGHT_PURPLE + "✪✪✪";
            case 13 -> ChatColor.AQUA + " ✪✪✪" + ChatColor.LIGHT_PURPLE + "✪✪";
            case 14 -> ChatColor.AQUA + " ✪✪✪✪" + ChatColor.LIGHT_PURPLE + "✪";
            case 15 -> ChatColor.AQUA + " ✪✪✪✪✪";
            default -> throw new IllegalStateException("Unexpected value: " + tag.getInt("upgrade_count"));
        }));

        ArrayList<String> lore = new ArrayList<>();

        Stats stats = item.stats();
        Stats tempStats = item.stats();
        if (reforge != null) {
            tempStats.addReforgeStat(tempStats, rarity, reforge.getStats());
        }

        if (tempStats.getBreakingPower() != 0) lore.add(ChatColor.DARK_GRAY + "Breaking Power " + (int) stats.getBreakingPower());
        double boost = DungeonStar.valueOf(tag.getString("dungeon_star")).getBoost();
        if (item.gearScore() >= 0) lore.add(ChatColor.GRAY + "Gear Score: " + ChatColor.LIGHT_PURPLE + item.gearScore() +
                ChatColor.DARK_GRAY + " (" + (int) (item.gearScore() + (item.gearScore() * boost)) + ")");
        if (tempStats.getDamage() != 0) lore.add(stat("Damage", ChatColor.RED, stats.getDamage(), ' ', tag,
                (item.genericItemType() == GenericItemType.WEAPON ? (tag.getInt("hot_potato_books") * 2) : 0)));
        if (tempStats.getStrength() != 0) lore.add(stat("Strength", ChatColor.RED, stats.getStrength(), ' ', tag,
                (item.genericItemType() == GenericItemType.WEAPON ? (tag.getInt("hot_potato_books") * 2) : 0), tag.getBoolean("art_of_war"),
                (reforge != null ? reforge.getStats().strength() : null)));
        if (tempStats.getCriticalChance() != 0) lore.add(stat("Crit Chance", ChatColor.RED, stats.getCriticalChance(), '%', tag, 0, false,
                (reforge != null ? reforge.getStats().critChance() : null)));
        if (tempStats.getCriticalDamage() != 0) lore.add(stat("Crit Damage", ChatColor.RED, stats.getCriticalDamage(), '%', tag, 0, false,
                (reforge != null ? reforge.getStats().critDamage() : null)));
        if (tempStats.getAttackSpeed() != 0) lore.add(stat("Bonus Attack Speed", ChatColor.RED, stats.getAttackSpeed(), '%', tag, 0, false,
                (reforge != null ? reforge.getStats().attackSpeed() : null)));
        if (tempStats.getWeaponAbilityDamage() != 0) lore.add(stat("Ability Damage", ChatColor.RED, stats.getWeaponAbilityDamage(), '%', tag));
        if (tempStats.getSeaCreatureChance() != 0) lore.add(stat("Sea Creature Chance", ChatColor.RED, stats.getSeaCreatureChance(), '%', tag, 0, false,
                (reforge != null ? reforge.getStats().seaCreatureChance() : null)));

        if (tempStats.getHealth() != 0) lore.add(stat("Health", ChatColor.GREEN, stats.getHealth(), ' ', tag,
                (item.genericItemType() == GenericItemType.ARMOR ? (tag.getInt("hot_potato_books") * 4) : 0), false,
                (reforge != null ? reforge.getStats().health() : null)));
        if (tempStats.getDefense() != 0) lore.add(stat("Defense", ChatColor.GREEN, stats.getDefense(), ' ', tag,
                (item.genericItemType() == GenericItemType.ARMOR ? (tag.getInt("hot_potato_books") * 2) : 0), false,
                (reforge != null ? reforge.getStats().defense() : null)));
        if (tempStats.getWalkSpeed() != 0) lore.add(stat("Speed", ChatColor.GREEN, stats.getWalkSpeed(), ' ', tag, 0, false,
                (reforge != null ? reforge.getStats().speed() : null)));
        if (tempStats.getIntelligence() != 0) lore.add(stat("Intelligence", ChatColor.GREEN, stats.getIntelligence(), ' ', tag, 0, false,
                (reforge != null ? reforge.getStats().intelligence() : null)));
        if (tempStats.getMagicFind() != 0) lore.add(stat("Magic Find", ChatColor.GREEN, stats.getMagicFind(), ' ', tag, 0, false,
                (reforge != null ? reforge.getStats().magicFind() : null)));
        if (tempStats.getPetLuck() != 0) lore.add(stat("Pet Luck", ChatColor.GREEN, stats.getPetLuck(), tag));
        if (tempStats.getTrueDefense() != 0) lore.add(stat("True Defense", ChatColor.GREEN, stats.getTrueDefense(), tag));
        if (tempStats.getFerocity() != 0) lore.add(stat("Ferocity", ChatColor.GREEN, stats.getFerocity(), ' ', tag, 0, false,
                (reforge != null ? reforge.getStats().ferocity() : null)));
        if (tempStats.getMiningSpeed() != 0) lore.add(stat("Mining Speed", ChatColor.GREEN, stats.getMiningSpeed(), ' ', tag, 0, false,
                (reforge != null ? reforge.getStats().miningSpeed() : null)));
        if (tempStats.getMiningFortune() != 0) lore.add(stat("Mining Fortune", ChatColor.GREEN, stats.getMiningFortune(), ' ', tag, 0, false,
                (reforge != null ? reforge.getStats().miningFortune() : null)));
        if (tempStats.getCombatWisdom() != 0) lore.add(stat("Combat Wisdom", ChatColor.GREEN, stats.getCombatWisdom(), tag));
        if (tempStats.getFarmingWisdom() != 0) lore.add(stat("Farming Wisdom", ChatColor.GREEN, stats.getFarmingWisdom(), tag));
        if (tempStats.getForagingWisdom() != 0) lore.add(stat("Foraging Wisdom", ChatColor.GREEN, stats.getFarmingWisdom(), tag));
        if (tempStats.getFishingWisdom() != 0) lore.add(stat("Fishing Wisdom", ChatColor.GREEN, stats.getFishingWisdom(), tag));
        if (tempStats.getFishingSpeed() != 0) lore.add(stat("Fishing Speed", ChatColor.GREEN, stats.getFishingSpeed(), tag));
        if (tempStats.getVitality() != 0) lore.add(stat("Vitality", ChatColor.GREEN, stats.getVitality(), tag));
        if (tempStats.getMending() != 0) lore.add(stat("Mending", ChatColor.GREEN, stats.getMending(), tag));

        if (tag.getList("gemstone_slots", 10) != null && !tag.getList("gemstone_slots", 10).isEmpty()) {
            NBTTagList slots = tag.getList("gemstone_slots", 10);
            StringBuilder gemstoneSlots = new StringBuilder();
            for (int i = 0; i < slots.size(); i++) {
                NBTTagCompound slot = slots.get(i);
                GemstoneSlot gemstoneSlot = item.gemstoneSlots().getSlots().get(i);
                gemstoneSlots.append(ChatColor.DARK_GRAY)
                        .append(" [")
                        .append(slot.getBoolean("locked") ? ChatColor.DARK_GRAY : ChatColor.GRAY)
                        .append(gemstoneSlot.getType().getIcon())
                        .append(ChatColor.DARK_GRAY)
                        .append("]");
            }
            lore.add(gemstoneSlots.toString());
            lore.add("");
        }

        if (tag.get("enchantments") != null && !tag.getList("enchantments", 10).isEmpty()) {
            int amount = tag.getList("enchantments", 10).size();
            NBTTagList enchantmentsn = tag.getList("enchantments", 10);
            List<Enchantment> enchantments = new ArrayList<>();
            for (int i = 0; i < amount; i++) {
                NBTTagCompound enchantment = enchantmentsn.get(i);
                String name = enchantment.getString("name");
                int level = enchantment.getInt("lvl");
                Enchantment enchant = Enchantment.getByIdentifiable(name + "." + level);
                enchantments.add(enchant);
            }
            enchantments.sort((o1, o2) -> Boolean.compare(o1.getType().isUltimate(), o2.getType().isUltimate()));
            List<String> stringEnchantments = new ArrayList<>();
            for (Enchantment enchantment : enchantments)
                stringEnchantments.add(enchantment.getDisplayName());
            Collections.sort(stringEnchantments);
            if (amount <= 5) {
                for (Enchantment enchantment : enchantments) {
                    lore.add(ChatColor.GRAY + enchantment.getDisplayName());
                    for (String line : Utils.splitByWordAndLength(enchantment.getDescription(), 30, "\\s"))
                        lore.add(ChatColor.GRAY + Utils.color(line));
                }
            } else if (amount <= 10) {
                for (Enchantment enchantment : enchantments)
                    lore.add(enchantment.getDisplayName());
            } else if (amount <= 25) {
                lore.addAll(Utils.combineElements(stringEnchantments, ", ", 2));
            } else {
                lore.addAll(Utils.combineElements(stringEnchantments, ", ", 3));
            }
            lore.add("");
        }

        if (tag.hasKey("attribute_1")) {
            Attribute attribute = Attribute.valueOf(tag.getString("attribute_1"));
            int level = tag.getInt("attribute_1_level");
            if (tag.hasKey("owner") && !tag.getString("owner").isEmpty()) {
                Player player = Bukkit.getPlayer(UUID.fromString(tag.getString("owner")));
                if (player != null && attribute.requirement().test(player)) {
                    lore.add("§b" + attribute.getName() + " " + Utils.getRomanNumeral(level));
                } else {
                    lore.add("§c" + attribute.getName() + " " + Utils.getRomanNumeral(level) + " ✖");
                }
            } else {
                lore.add("§c" + attribute.getName() + " " + Utils.getRomanNumeral(level) + " ✖");
            }
            lore.addAll(attribute.getLore(level));
            if (!tag.hasKey("attribute_2"))
                lore.add("");
        }
        if (tag.hasKey("attribute_2")) {
            Attribute attribute = Attribute.valueOf(tag.getString("attribute_2"));
            int level = tag.getInt("attribute_2_level");
            if (tag.hasKey("owner") && !tag.getString("owner").isEmpty()) {
                Player player = Bukkit.getPlayer(UUID.fromString(tag.getString("owner")));
                if (player != null && attribute.requirement().test(player)) {
                    lore.add("§b" + attribute.getName() + " " + Utils.getRomanNumeral(level));
                } else {
                    lore.add("§c" + attribute.getName() + " " + Utils.getRomanNumeral(level) + " ✖");
                }
            } else {
                lore.add("§c" + attribute.getName() + " " + Utils.getRomanNumeral(level) + " ✖");
            }
            lore.addAll(attribute.getLore(level));
            lore.add("");
        }

        if (item.description() != null) {
            String description = Utils.colorize(item.description());
            for (String line : Utils.splitByWordAndLength(description, 30, "\\s"))
                lore.add(ChatColor.GRAY + line);
            lore.add("");
        }

        if (!tag.getString("rune").isEmpty()) {
            Rune rune = Rune.valueOf(tag.getString("rune"));
            int runeLevel = tag.getInt("rune_level");
            lore.add(rune.getColor() + "◆ " + rune.getName() + " Rune " + runeLevel);
            lore.add("");
        }

        if (item.ability() != null) {
            buildAbility(item.ability(), lore);
            lore.add("");
        }

        if (item.nbtLore(tag) != null) {
            for (String line : item.nbtLore(tag))
                lore.add(ChatColor.GRAY + line);
            lore.add("");
        }

        if (tag.getString("soulbound") != null && !Soulbound.valueOf(tag.getString("soulbound")).equals(Soulbound.NONE)) {
            lore.add(ChatColor.DARK_GRAY + "* " + (Soulbound.valueOf(tag.getString("soulbound")).equals(Soulbound.COOP) ? "Co-op " : "") + "Soulbound *");
        }
        if (item.requirements() != null) {
            for (Requirement requirement : item.requirements().getRequirements()) {
                if (requirement.requirement() == null) continue;
                if (tag.getString("owner") == null || tag.getString("owner").isEmpty()) continue;
                Player player = Bukkit.getPlayer(UUID.fromString(tag.getString("owner")));
                if (requirement.requirement().test(player)) continue;

                if (requirement instanceof SkillRequirement skillRequirement) {
                    lore.add(
                            ChatColor.DARK_RED + "❣ " +
                            ChatColor.RED + "Requires " +
                            ChatColor.GREEN + skillRequirement.getSkill().getName() + " Skill " + skillRequirement.getLevel() + "."
                    );
                } else if (requirement instanceof DungeonTierRequirement dungeonTierRequirement) {
                    lore.add(
                            ChatColor.DARK_RED + "❣ " +
                            ChatColor.RED + "Requires " +
                            ChatColor.GREEN + dungeonTierRequirement.getDungeonType().getName() + " Floor " + dungeonTierRequirement.getTier() + " Completion."
                    );
                } else if (requirement instanceof SlayerRequirement slayerRequirement) {
                    lore.add(
                            ChatColor.DARK_RED + "☠ " +
                                    ChatColor.RED + "Requires " +
                                    ChatColor.DARK_PURPLE + slayerRequirement.getBossType().getName() + " Slayer " + slayerRequirement.getLevel() + "."
                    );
                } else if (requirement instanceof HeartOfTheMountainRequirement heartOfTheMountainRequirement) {
                    lore.add(
                            ChatColor.DARK_RED + "❣ " +
                                    ChatColor.RED + "Requires " +
                                    ChatColor.DARK_PURPLE + "Heart of the"
                    );
                    lore.add(
                            ChatColor.DARK_PURPLE + "Mountain Tier " + heartOfTheMountainRequirement.getLevel()
                    );
                }
            }
        }
        lore.add(
                rarity.getBoldedColor() +
                        (tag.getBoolean("recombobulated") ? rarity.getBoldedColor() + ChatColor.MAGIC + "A" + rarity.getBoldedColor() + " " : "") +
                        rarity.name() +
                        (item.specificItemType() != SpecificItemType.NONE ? (
                                (tag.getBoolean("dungeon_item") ? " DUNGEON" : "") +
                                " " + item.specificItemType().name().replace("_", " ")
                        ) : "") +
                        (tag.getBoolean("recombobulated") ? " " + rarity.getBoldedColor() + ChatColor.MAGIC + "A" + rarity.getBoldedColor() : "")
        );

        meta.spigot().setUnbreakable(true);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON);

        stack.setItemMeta(meta);
        return stack;
    }

    public static void buildAbility(Ability ability, ArrayList<String> lore) {
        lore.add(ChatColor.GOLD + ability.getType().getName() + ": " + ability.getName() + " " +
                ChatColor.YELLOW + ChatColor.BOLD + ability.getActivation().name().replace("_", " "));
        for (String line : Utils.splitByWordAndLength(ability.getDescription(), 30, "\\s"))
            lore.add(ChatColor.GRAY + Utils.color(line));
        if (ability.getManaCost() != 0)
            lore.add(ChatColor.DARK_GRAY + "Mana Cost: " + ChatColor.DARK_AQUA + ability.getManaCost());
        if (ability.getCooldown() != 0)
            lore.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.GREEN + ability.getCooldown() + "s");
    }

    private static String stat(String name, ChatColor color, double value, char ending, NBTTagCompound tag, int hotPotatoBooks, boolean artOfWar, ReforgeStat reforgeStat) {
        Rarity rarity = Rarity.valueOf(tag.getString("rarity"));
        double rsv = reforgeStat == null ? 0 : switch (rarity) {
            case COMMON -> reforgeStat.getCommon();
            case UNCOMMON -> reforgeStat.getUncommon();
            case RARE -> reforgeStat.getRare();
            case EPIC -> reforgeStat.getEpic();
            case LEGENDARY -> reforgeStat.getLegendary();
            case MYTHIC -> reforgeStat.getMythic();
            default -> 0;
        };
        double boost = DungeonStar.valueOf(tag.getString("dungeon_star")).getBoost();
        double val = value + hotPotatoBooks + (artOfWar ? 5 : 0) + rsv;
        double wb = val + (val * boost);
        return Utils.color(
                "&7" + name + ": " +
                        color + (val < 0 ? (int) val : "+" + (int) val) + (ending != ' ' ? ending : "") + " " +
                        (hotPotatoBooks > 0 ? "&e(+" + hotPotatoBooks + ") " : "") +
                        (artOfWar ? "&6[+5] " : "") +
                        (rsv != 0 ? ("&9(" + (rsv < 0 ? (int) rsv : "+" + (int) rsv) + (ending != ' ' ? ""+ending : "") + ") ") : "") +
                        (tag.getBoolean("dungeon_item") ? "&8(+" + wb + (ending != ' ' ? ending : "") + ")" : "")
                );
    }

    private static String stat(String name, ChatColor color, double value, char ending, NBTTagCompound tag, int hotPotatoBooks, boolean artOfWar) {
        return stat(name, color, value, ending, tag, hotPotatoBooks, artOfWar, null);
    }

    private static String stat(String name, ChatColor color, double value, char ending, NBTTagCompound tag, int hotPotatoBooks) {
        return stat(name, color, value, ending, tag, hotPotatoBooks, false);
    }

    private static String stat(String name, ChatColor color, double value, char ending, NBTTagCompound tag) {
        return stat(name, color, value, ending, tag, 0, false);
    }

    private static String stat(String name, ChatColor color, double value, NBTTagCompound tag) {
        return stat(name, color, value, ' ', tag, 0, false);
    }

    public static NBTTagCompound serializeEnchantments(List<Enchantment> enchantments) {
        NBTTagCompound tag = new NBTTagCompound();
        for (Enchantment enchantment : enchantments) {
            NBTTagCompound enchantmentTag = new NBTTagCompound();
            enchantmentTag.setString("name", enchantment.getType().getNamespace());
            enchantmentTag.setInt("level", enchantment.getLevel());
            tag.set(enchantment.getType().getNamespace(), enchantmentTag);
        }
        return tag;
    }

}
