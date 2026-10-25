package net.icxd.dungeons.fairysouls;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.fairysouls.FairySoulTable.Soul;
import net.icxd.dungeons.hub.DungeonHubMap;
import net.icxd.dungeons.hub.HubMap;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.npc.NpcTable;
import net.icxd.dungeons.npc.Npcs;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * The Fairy Souls of the map this server is (the Hub's, the Dungeon Hub's). The captured maps have Hypixel's own
 * stands for every one (all 80 of the Hub's, all 7 of the Dungeon Hub's): invisible, silent armor stands of the
 * usual size wearing the Fairy Soul's head, 1.46875 under the block the table names (66.53125 for a soul in block
 * 68), so none are spawned. The capture kept some two to four times over (Hypixel sent them again as it went on,
 * each facing a new way), so all but one go as their chunk's entities load. A left or right click on one finds
 * it, with the wiki's lines (Fairy Souls' Game Messages); its sound, if it has one, is UNKNOWN, so there's none.
 * Found ones still show, as Hypixel's do. Main thread.
 */
@OnlyOn({ServerType.LOBBY, ServerType.DUNGEON_HUB})
public final class SoulStands implements Listener {
    static final double BELOW = 1.46875;
    static final String FOUND = "&d&lSOUL! &fYou found a &dFairy Soul&r&f!";
    static final String ALREADY = "&dYou have already found that Fairy Soul!";

    /** This map's souls by the block their heads are in ("-233,86,84"); none off the maps. */
    private final Map<String, Soul> souls = new HashMap<>();

    public SoulStands(Plugin plugin) {
        String island = HubMap.here() ? FairySoulTable.HUB : DungeonHubMap.here() ? FairySoulTable.DUNGEON_HUB : null;
        if (island == null) return;
        Logger log = plugin.getLogger();
        FairySoulTable table = FairySoulTable.load(plugin.getDataFolder().toPath().resolve(NpcTable.FOLDER));
        for (String problem : table.problems()) log.warning("Fairy Souls: " + problem);
        for (Soul soul : table.on(island)) souls.put(soul.x() + "," + soul.y() + "," + soul.z(), soul);
        if (souls.isEmpty()) return;
        log.info("Fairy Souls: " + souls.size() + " (" + island + ")");
        oncePerSoul(mainWorld().getEntities());
    }

    /** A right click on an armor stand: the game sends it as a click at a spot on it, never as a plain one. */
    @EventHandler
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        Soul soul = soul(event.getRightClicked());
        if (soul == null) return;
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) click(event.getPlayer(), soul);
    }

    @EventHandler
    public void onAttack(PrePlayerAttackEntityEvent event) {
        Soul soul = soul(event.getAttacked());
        if (soul == null) return;
        event.setCancelled(true);
        click(event.getPlayer(), soul);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        if (!souls.isEmpty() && event.getWorld().equals(mainWorld())) oncePerSoul(event.getEntities());
    }

    /** The soul this entity is the stand of: an armor stand wearing a head where one of the table's is. */
    Soul soul(Entity entity) {
        if (souls.isEmpty() || !(entity instanceof ArmorStand stand) || !stand.getWorld().equals(mainWorld())) return null;
        ItemStack head = stand.getEquipment().getHelmet();
        if (head == null || head.getType() != Material.PLAYER_HEAD) return null;
        return souls.get(spot(stand.getX(), stand.getY(), stand.getZ()));
    }

    /** The block a stand here has its soul's head in. */
    static String spot(double x, double y, double z) {
        return (int) Math.floor(x) + "," + (int) Math.floor(y + BELOW + 1e-6) + "," + (int) Math.floor(z);
    }

    /** Takes all but one of each soul's stands among these. */
    private void oncePerSoul(Collection<? extends Entity> entities) {
        Set<Soul> seen = new HashSet<>();
        for (Entity entity : List.copyOf(entities)) {
            Soul soul = soul(entity);
            if (soul != null && !seen.add(soul)) entity.remove();
        }
    }

    static void click(Player player, Soul soul) {
        Npcs.clicked(player);
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player)) return;
        player.sendMessage(Text.line(FairySouls.find(user.profile(), soul) ? FOUND : ALREADY));
    }

    private static World mainWorld() {
        return Dungeons.getSkyBlockServer().getMainWorld();
    }
}
