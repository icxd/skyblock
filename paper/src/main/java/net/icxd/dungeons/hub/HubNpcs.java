package net.icxd.dungeons.hub;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.destroystokyo.paper.profile.ProfileProperty;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.npc.Npc;
import net.icxd.dungeons.npc.NpcTable;

/**
 * The Hub's NPCs where Hypixel has them, on its map: the data checkout's {@code npcs/hub.json} (see
 * {@link NpcTable}), each in its skin or as its kind, under its name tags, holding what it held. Those
 * whose words were recorded say them ({@link HubTalk}), the Banker his and then the bank ({@link
 * Banker}), the Farm Merchant his and then his shop ({@link FarmMerchant}), Marco and the Bartender
 * theirs for their quests ({@link Marco}, {@link Bartender}), Jacobus his and then his Accessory Bag
 * Upgrades ({@link Jacobus}), Madame Goldsworth hers and then the Museum ({@link MadameGoldsworth}), Bea her shop
 * ({@link Bea}); the rest stand there for now. Their
 * chunks are kept loaded, since an NPC goes with its chunk. Main thread.
 */
@OnlyOn({ServerType.LOBBY})
public final class HubNpcs {
    /** The replay tool's names for the equipment slots. */
    private static final Map<String, EquipmentSlot> SLOTS = Map.of("mainhand", EquipmentSlot.HAND, "offhand", EquipmentSlot.OFF_HAND,
            "feet", EquipmentSlot.FEET, "legs", EquipmentSlot.LEGS, "chest", EquipmentSlot.CHEST, "head", EquipmentSlot.HEAD);

    private HubNpcs() {
    }

    /** Spawns them, if this server's main world is the Hub's map; says what's wrong with the table. */
    public static void spawn(Plugin plugin) {
        if (!HubMap.here()) return;
        Logger log = plugin.getLogger();
        NpcTable table = NpcTable.load(plugin.getDataFolder().toPath().resolve(NpcTable.FOLDER), HubMap.NAME);
        for (String problem : table.problems()) log.warning("Hub NPCs: " + problem);
        World world = Dungeons.getSkyBlockServer().getMainWorld();
        Set<Long> chunks = new HashSet<>();
        int spawned = 0;
        for (NpcTable.Entry npc : table.npcs()) {
            Location at = new Location(world, npc.x(), npc.y(), npc.z(), npc.yaw(), npc.pitch());
            if (chunks.add(((long) at.getBlockX() >> 4) << 32 | ((at.getBlockZ() >> 4) & 0xffffffffL))) at.getChunk().addPluginChunkTicket(plugin);
            Consumer<Player> click = switch (npc.id()) {
                case Banker.ID -> player -> Banker.click(plugin, player);
                case FarmMerchant.ID -> player -> FarmMerchant.click(plugin, player);
                case Marco.ID -> player -> Marco.click(plugin, player);
                case Bartender.ID -> player -> Bartender.click(plugin, player);
                case Jacobus.ID -> player -> Jacobus.click(plugin, player);
                case MadameGoldsworth.ID -> player -> MadameGoldsworth.click(plugin, player);
                case Bea.ID -> Bea::click;
                default -> HubTalk.of(npc).<Consumer<Player>>map(talk -> player -> talk.talk(plugin, player)).orElse(player -> {
                });
            };
            try {
                spawn(npc, at, click);
                spawned++;
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Couldn't spawn the Hub's " + npc.name() + " at " + npc.x() + " " + npc.y() + " " + npc.z(), e);
            }
        }
        if (!table.npcs().isEmpty()) log.info("Hub: " + spawned + " NPCs from " + NpcTable.FOLDER + "/" + HubMap.NAME + ".json");
    }

    private static void spawn(NpcTable.Entry npc, Location at, Consumer<Player> click) {
        String id = npc.id();
        switch (npc.type()) {
            case "player", "mannequin" -> {
                if (npc.skin() == null) throw new IllegalArgumentException("a player-shaped NPC with no skin");
                Npc.player(id, at, new ProfileProperty("textures", npc.skin(), npc.signature()), npc.lines(), npc.heights(), equipment(npc), click);
            }
            case "villager" -> Npc.villager(id, at, registered(Registry.VILLAGER_TYPE, npc.villagerType(), Villager.Type.PLAINS),
                    registered(Registry.VILLAGER_PROFESSION, npc.villagerProfession(), Villager.Profession.NONE), npc.lines(), npc.heights(), click);
            default -> {
                EntityType type = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(npc.type()));
                if (type == null) throw new IllegalArgumentException("no entity type " + npc.type());
                Npc.mob(id, at, type, npc.lines(), npc.heights(), click);
            }
        }
    }

    /** What it held, as plain items of those materials (their names don't show in a hand). */
    private static Map<EquipmentSlot, ItemStack> equipment(NpcTable.Entry npc) {
        Map<EquipmentSlot, ItemStack> out = new EnumMap<>(EquipmentSlot.class);
        npc.equipment().forEach((slot, item) -> {
            Material material = Material.matchMaterial(item);
            EquipmentSlot to = SLOTS.get(slot.toLowerCase(Locale.ROOT));
            if (material != null && to != null) out.put(to, new ItemStack(material));
        });
        return out;
    }

    /** A villager's kind by its name ("plains", "farmer"); {@code fallback} for none or one 26.2 doesn't have. */
    private static <T extends org.bukkit.Keyed> T registered(Registry<T> registry, String name, T fallback) {
        if (name == null) return fallback;
        T found = registry.get(NamespacedKey.minecraft(name));
        return found == null ? fallback : found;
    }
}
