package net.icxd.dungeons.gui;

import io.papermc.paper.event.packet.UncheckedSignChangeEvent;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.utils.Text;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Text typed on a sign, the way Hypixel asks for it (the auction house's search): a sign only the
 * player sees, just above their head, opened for editing; what they write comes back when they're
 * done (Escape counts as done). The client only edits a sign it has, so it's sent one first, and the
 * block that's really there afterwards. Paper's {@link UncheckedSignChangeEvent} brings the lines,
 * as there's no sign on the server for {@code SignChangeEvent}.
 */
public final class SignInput implements Listener {
    private static final Map<UUID, Prompt> PROMPTS = new HashMap<>();
    private static boolean listening;

    private record Prompt(Location location, Consumer<List<String>> done) {
    }

    private SignInput() {
    }

    /** Opens the sign with these four lines (& colours); {@code done} gets the four lines as typed, on the main thread. */
    public static void open(Player player, List<String> lines, Consumer<List<String>> done) {
        if (!listening) {
            // From the first sign on; nothing else needs these events.
            Bukkit.getPluginManager().registerEvents(new SignInput(), Dungeons.getInstance());
            listening = true;
        }
        Prompt old = PROMPTS.remove(player.getUniqueId());
        if (old != null) restore(player, old.location());

        Location location = place(player);
        Sign sign = (Sign) Material.OAK_SIGN.createBlockData().createBlockState();
        SignSide front = sign.getSide(Side.FRONT);
        for (int i = 0; i < 4; i++) front.line(i, Text.line(i < lines.size() ? lines.get(i) : ""));
        player.sendBlockChange(location, sign.getBlockData());
        player.sendBlockUpdate(location, sign);
        player.openVirtualSign(Position.block(location), Side.FRONT);
        PROMPTS.put(player.getUniqueId(), new Prompt(location, done));
    }

    /**
     * The block above the player's head: close enough for the client to let them edit it (block
     * reach plus 4), out of sight, and not one they could be standing on.
     */
    private static Location place(Player player) {
        World world = player.getWorld();
        Block head = player.getEyeLocation().getBlock();
        int y = Math.clamp(head.getY() + 1L, world.getMinHeight(), world.getMaxHeight() - 1);
        return new Location(world, head.getX(), y, head.getZ());
    }

    /** What's really there, again. */
    private static void restore(Player player, Location location) {
        if (!player.getWorld().equals(location.getWorld())) return;
        Block block = location.getBlock();
        player.sendBlockChange(location, block.getBlockData());
        if (block.getState() instanceof TileState tile) player.sendBlockUpdate(location, tile);
    }

    @EventHandler
    public void onSignChange(UncheckedSignChangeEvent event) {
        Player player = event.getPlayer();
        Prompt prompt = PROMPTS.get(player.getUniqueId());
        if (prompt == null || !at(event.getEditedBlockPosition(), prompt.location())) return;
        PROMPTS.remove(player.getUniqueId());
        // Nothing to change on the server: there's no sign there.
        event.setCancelled(true);
        restore(player, prompt.location());
        prompt.done().accept(event.lines().stream().map(PlainTextComponentSerializer.plainText()::serialize).toList());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PROMPTS.remove(event.getPlayer().getUniqueId());
    }

    private static boolean at(BlockPosition position, Location location) {
        return position.blockX() == location.getBlockX() && position.blockY() == location.getBlockY()
                && position.blockZ() == location.getBlockZ();
    }
}
