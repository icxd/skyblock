package net.icxd.dungeonscanner.legacy;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * 1.8 blocks that stored part of their state in a tile entity, and the tile entities themselves.
 * Only the kinds that show up in dungeon builds get real data (skulls, signs, flower pots, banners,
 * note blocks); containers get an empty tile entity of the right type (their contents aren't sent
 * to the client anyway).
 */
public final class LegacyTileEntities {
  private LegacyTileEntities() {
  }

  private static final int SKULL = 144;
  private static final int FLOWER_POT = 140;
  private static final int STANDING_BANNER = 176;
  private static final int WALL_BANNER = 177;
  private static final int DOUBLE_PLANT = 175;
  private static final int NOTE_BLOCK = 25;

  /** potted_x -> 1.8 flower pot contents (item id, data). */
  private static final Map<Block, Object[]> POTTED = Map.ofEntries(
      Map.entry(Blocks.POTTED_POPPY, new Object[]{"minecraft:red_flower", 0}),
      Map.entry(Blocks.POTTED_BLUE_ORCHID, new Object[]{"minecraft:red_flower", 1}),
      Map.entry(Blocks.POTTED_ALLIUM, new Object[]{"minecraft:red_flower", 2}),
      Map.entry(Blocks.POTTED_AZURE_BLUET, new Object[]{"minecraft:red_flower", 3}),
      Map.entry(Blocks.POTTED_RED_TULIP, new Object[]{"minecraft:red_flower", 4}),
      Map.entry(Blocks.POTTED_ORANGE_TULIP, new Object[]{"minecraft:red_flower", 5}),
      Map.entry(Blocks.POTTED_WHITE_TULIP, new Object[]{"minecraft:red_flower", 6}),
      Map.entry(Blocks.POTTED_PINK_TULIP, new Object[]{"minecraft:red_flower", 7}),
      Map.entry(Blocks.POTTED_OXEYE_DAISY, new Object[]{"minecraft:red_flower", 8}),
      Map.entry(Blocks.POTTED_DANDELION, new Object[]{"minecraft:yellow_flower", 0}),
      Map.entry(Blocks.POTTED_OAK_SAPLING, new Object[]{"minecraft:sapling", 0}),
      Map.entry(Blocks.POTTED_SPRUCE_SAPLING, new Object[]{"minecraft:sapling", 1}),
      Map.entry(Blocks.POTTED_BIRCH_SAPLING, new Object[]{"minecraft:sapling", 2}),
      Map.entry(Blocks.POTTED_JUNGLE_SAPLING, new Object[]{"minecraft:sapling", 3}),
      Map.entry(Blocks.POTTED_ACACIA_SAPLING, new Object[]{"minecraft:sapling", 4}),
      Map.entry(Blocks.POTTED_DARK_OAK_SAPLING, new Object[]{"minecraft:sapling", 5}),
      Map.entry(Blocks.POTTED_RED_MUSHROOM, new Object[]{"minecraft:red_mushroom", 0}),
      Map.entry(Blocks.POTTED_BROWN_MUSHROOM, new Object[]{"minecraft:brown_mushroom", 0}),
      Map.entry(Blocks.POTTED_CACTUS, new Object[]{"minecraft:cactus", 0}),
      Map.entry(Blocks.POTTED_DEAD_BUSH, new Object[]{"minecraft:deadbush", 0}),
      Map.entry(Blocks.POTTED_FERN, new Object[]{"minecraft:tallgrass", 2})
  );

  /** Modern block entity id -> 1.8 tile entity id, for the ones without special data. */
  private static final Map<String, String> PLAIN = Map.ofEntries(
      Map.entry("minecraft:chest", "Chest"),
      Map.entry("minecraft:trapped_chest", "Chest"),
      Map.entry("minecraft:ender_chest", "EnderChest"),
      Map.entry("minecraft:furnace", "Furnace"),
      Map.entry("minecraft:dispenser", "Trap"),
      Map.entry("minecraft:dropper", "Dropper"),
      Map.entry("minecraft:hopper", "Hopper"),
      Map.entry("minecraft:brewing_stand", "Cauldron"),
      Map.entry("minecraft:enchanting_table", "EnchantTable"),
      Map.entry("minecraft:beacon", "Beacon"),
      Map.entry("minecraft:mob_spawner", "MobSpawner"),
      Map.entry("minecraft:jukebox", "RecordPlayer"),
      Map.entry("minecraft:daylight_detector", "DLDetector"),
      Map.entry("minecraft:comparator", "Comparator"),
      Map.entry("minecraft:command_block", "Control"),
      Map.entry("minecraft:end_portal", "Airportal")
  );

  /** Modern banner pattern id -> 1.8 code. Patterns added after 1.8 have no entry. */
  private static final Map<String, String> PATTERNS = Map.ofEntries(
      Map.entry("minecraft:stripe_bottom", "bs"), Map.entry("minecraft:stripe_top", "ts"),
      Map.entry("minecraft:stripe_left", "ls"), Map.entry("minecraft:stripe_right", "rs"),
      Map.entry("minecraft:stripe_center", "cs"), Map.entry("minecraft:stripe_middle", "ms"),
      Map.entry("minecraft:stripe_downright", "drs"), Map.entry("minecraft:stripe_downleft", "dls"),
      Map.entry("minecraft:small_stripes", "ss"), Map.entry("minecraft:cross", "cr"),
      Map.entry("minecraft:straight_cross", "sc"), Map.entry("minecraft:square_bottom_left", "bl"),
      Map.entry("minecraft:square_bottom_right", "br"), Map.entry("minecraft:square_top_left", "tl"),
      Map.entry("minecraft:square_top_right", "tr"), Map.entry("minecraft:triangle_bottom", "bt"),
      Map.entry("minecraft:triangle_top", "tt"), Map.entry("minecraft:triangles_bottom", "bts"),
      Map.entry("minecraft:triangles_top", "tts"), Map.entry("minecraft:circle", "mc"),
      Map.entry("minecraft:rhombus", "mr"), Map.entry("minecraft:half_vertical", "vh"),
      Map.entry("minecraft:half_horizontal", "hh"), Map.entry("minecraft:border", "bo"),
      Map.entry("minecraft:curly_border", "cbo"), Map.entry("minecraft:gradient", "gra"),
      Map.entry("minecraft:creeper", "cre"), Map.entry("minecraft:skull", "sku"),
      Map.entry("minecraft:flower", "flo"), Map.entry("minecraft:mojang", "moj"),
      Map.entry("minecraft:bricks", "bri")
  );

  /** Blocks {@link LegacyMapper}'s table gets wrong or doesn't have. */
  static Optional<LegacyBlock> specialBlock(BlockState state) {
    Block block = state.getBlock();
    if (block instanceof AbstractSkullBlock) {
      if (block instanceof WallSkullBlock) {
        return Optional.of(new LegacyBlock(SKULL, facingData(state.getValue(WallSkullBlock.FACING))));
      }
      return Optional.of(new LegacyBlock(SKULL, 1));
    }
    if (POTTED.containsKey(block)) return Optional.of(new LegacyBlock(FLOWER_POT, 0));
    if (block instanceof AbstractBannerBlock) {
      if (block instanceof WallBannerBlock) {
        return Optional.of(new LegacyBlock(WALL_BANNER, facingData(state.getValue(WallBannerBlock.FACING))));
      }
      return Optional.of(new LegacyBlock(STANDING_BANNER, state.getValue(BlockStateProperties.ROTATION_16)));
    }
    // 1.8 only stored the plant type in the lower half; every upper half is 175:8.
    if (block instanceof DoublePlantBlock && state.hasProperty(DoublePlantBlock.HALF)
        && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
      return Optional.of(new LegacyBlock(DOUBLE_PLANT, 8));
    }
    return Optional.empty();
  }

  /**
   * The 1.8 tile entity for a block, or empty if it doesn't have one.
   *
   * @param modern the modern block entity NBT (may be null, e.g. potted plants have none)
   */
  public static Optional<CompoundTag> convert(BlockState state, CompoundTag modern, int x, int y, int z) {
    Block block = state.getBlock();
    CompoundTag out = new CompoundTag();
    if (block instanceof AbstractSkullBlock) {
      out.putString("id", "Skull");
      out.putByte("SkullType", skullType(block));
      out.putByte("Rot", (byte) (block instanceof WallSkullBlock ? 0 : state.getValue(SkullBlock.ROTATION)));
      if (modern != null) owner(modern).ifPresent(o -> out.put("Owner", o));
    } else if (POTTED.containsKey(block)) {
      Object[] plant = POTTED.get(block);
      out.putString("id", "FlowerPot");
      out.putString("Item", (String) plant[0]);
      out.putInt("Data", (Integer) plant[1]);
    } else if (block == Blocks.FLOWER_POT) {
      out.putString("id", "FlowerPot");
      out.putString("Item", "minecraft:air");
      out.putInt("Data", 0);
    } else if (block instanceof AbstractBannerBlock banner) {
      out.putString("id", "Banner");
      out.putInt("Base", dye(banner.getColor()));
      ListTag patterns = new ListTag();
      if (modern != null) {
        for (Tag t : modern.getListOrEmpty("patterns")) {
          if (!(t instanceof CompoundTag p)) continue;
          String code = PATTERNS.get(p.getStringOr("pattern", ""));
          DyeColor color = DyeColor.byName(p.getStringOr("color", "white"), DyeColor.WHITE);
          if (code == null) continue;
          CompoundTag lp = new CompoundTag();
          lp.putString("Pattern", code);
          lp.putInt("Color", dye(color));
          patterns.add(lp);
        }
      }
      out.put("Patterns", patterns);
    } else if (block == Blocks.NOTE_BLOCK) {
      out.putString("id", "Music");
      out.putByte("note", (byte) (int) state.getValue(BlockStateProperties.NOTE));
    } else if (modern != null && modern.getStringOr("id", "").endsWith("sign")) {
      out.putString("id", "Sign");
      ListTag messages = modern.getCompoundOrEmpty("front_text").getListOrEmpty("messages");
      for (int i = 0; i < 4; i++) {
        String text = i < messages.size() ? legacyText(messages.get(i)) : "";
        out.putString("Text" + (i + 1), "{\"text\":" + jsonString(text) + "}");
      }
    } else if (modern != null && PLAIN.containsKey(modern.getStringOr("id", ""))) {
      out.putString("id", PLAIN.get(modern.getStringOr("id", "")));
    } else {
      return Optional.empty();
    }
    out.putInt("x", x);
    out.putInt("y", y);
    out.putInt("z", z);
    return Optional.of(out);
  }

  /** 1.8 facing data: 2 north, 3 south, 4 west, 5 east. */
  private static int facingData(Direction d) {
    return switch (d) {
      case NORTH -> 2;
      case SOUTH -> 3;
      case WEST -> 4;
      case EAST -> 5;
      default -> 1;
    };
  }

  private static byte skullType(Block block) {
    String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
    if (id.startsWith("wither_skeleton")) return 1;
    if (id.startsWith("skeleton")) return 0;
    if (id.startsWith("zombie")) return 2;
    if (id.startsWith("creeper")) return 4;
    return 3; // player heads (and anything newer: dragon, piglin)
  }

  /** 1.8 dye damage values run black..white, modern ids white..black. */
  private static int dye(DyeColor color) {
    return 15 - color.getId();
  }

  /**
   * Modern {@code profile} (name string, or {name, id: int[4], properties: [{name, value,
   * signature}]}) to the 1.8 {@code Owner} compound.
   */
  private static Optional<CompoundTag> owner(CompoundTag be) {
    Tag profile = be.get("profile");
    if (profile == null) return Optional.empty();
    CompoundTag owner = new CompoundTag();
    if (profile instanceof CompoundTag p) {
      String name = p.getStringOr("name", "");
      int[] id = p.getIntArray("id").orElse(null);
      UUID uuid = id != null && id.length == 4
          ? new UUID((long) id[0] << 32 | (id[1] & 0xFFFFFFFFL), (long) id[2] << 32 | (id[3] & 0xFFFFFFFFL))
          : UUID.nameUUIDFromBytes(p.toString().getBytes());
      owner.putString("Id", uuid.toString());
      if (!name.isEmpty()) owner.putString("Name", name);
      ListTag textures = new ListTag();
      for (Tag t : p.getListOrEmpty("properties")) {
        if (!(t instanceof CompoundTag prop) || !"textures".equals(prop.getStringOr("name", ""))) continue;
        CompoundTag tex = new CompoundTag();
        tex.putString("Value", prop.getStringOr("value", ""));
        prop.getString("signature").ifPresent(s -> tex.putString("Signature", s));
        textures.add(tex);
      }
      if (!textures.isEmpty()) {
        CompoundTag properties = new CompoundTag();
        properties.put("textures", textures);
        owner.put("Properties", properties);
      }
    } else {
      String name = profile.asString().orElse("");
      if (name.isEmpty()) return Optional.empty();
      owner.putString("Name", name);
      owner.putString("Id", UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()).toString());
    }
    return Optional.of(owner);
  }

  /** A sign line (text component NBT) as a string with 1.8 formatting codes. */
  static String legacyText(Tag tag) {
    Component component = ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(Component.empty());
    StringBuilder sb = new StringBuilder();
    component.visit((style, text) -> {
      sb.append(codes(style)).append(text);
      return Optional.empty();
    }, Style.EMPTY);
    return sb.toString();
  }

  private static String codes(Style style) {
    StringBuilder sb = new StringBuilder();
    TextColor color = style.getColor();
    if (color != null) {
      // Only the 16 named colours exist in 1.8; RGB colours are dropped.
      for (ChatFormatting f : ChatFormatting.values()) {
        TextColor named = TextColor.fromLegacyFormat(f);
        if (named != null && named.getValue() == color.getValue()) {
          sb.append(f);
          break;
        }
      }
    }
    if (style.isBold()) sb.append(ChatFormatting.BOLD);
    if (style.isItalic()) sb.append(ChatFormatting.ITALIC);
    if (style.isUnderlined()) sb.append(ChatFormatting.UNDERLINE);
    if (style.isStrikethrough()) sb.append(ChatFormatting.STRIKETHROUGH);
    if (style.isObfuscated()) sb.append(ChatFormatting.OBFUSCATED);
    return sb.toString();
  }

  private static String jsonString(String s) {
    StringBuilder sb = new StringBuilder("\"");
    for (char c : s.toCharArray()) {
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        default -> {
          if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
          else sb.append(c);
        }
      }
    }
    return sb.append('"').toString();
  }
}
