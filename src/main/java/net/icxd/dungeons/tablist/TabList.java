package net.icxd.dungeons.tablist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.region.Region;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Hypixel-style tab list: 4 columns of 20 fake entries, sent with PacketEvents (the plugin must be
 * installed). Real players are unlisted and shown in the "Players" column instead.
 *
 * <p>Every viewer gets the same 80 entries (fixed ids, ordered by list order); each update only
 * sends the lines that changed. The 1.8 version made ~80 new server-side players and scoreboard
 * teams per viewer every 3 seconds and kept them all, so it got slower the longer the server ran.
 */
public class TabList {
    private static final int SLOTS = 80;
    private static final int PLAYER_SLOTS = 19;
    private static final UUID[] IDS = new UUID[SLOTS];
    private static final EnumSet<Action> ADD = EnumSet.of(Action.ADD_PLAYER, Action.UPDATE_LISTED, Action.UPDATE_LATENCY,
            Action.UPDATE_GAME_MODE, Action.UPDATE_DISPLAY_NAME, Action.UPDATE_LIST_ORDER, Action.UPDATE_HAT);
    private static final EnumSet<Action> CHANGE = EnumSet.of(Action.UPDATE_DISPLAY_NAME, Action.UPDATE_LATENCY);

    static {
        for (int i = 0; i < SLOTS; i++) IDS[i] = UUID.nameUUIDFromBytes(("dungeons-tab-" + i).getBytes(StandardCharsets.UTF_8));
    }

    private record Skin(String texture, String signature) {
    }

    private record Line(String text, Skin skin, int ping) {
        Line(String text, Skin skin) {
            this(text, skin, 0);
        }
    }

    // Coloured squares, named after their colour.
    private static final Skin GREEN = new Skin(
            "ewogICJ0aW1lc3RhbXAiIDogMTYxMjAyMTY4NjAwMCwKICAicHJvZmlsZUlkIiA6ICI3MzgyZGRmYmU0ODU0NTVjODI1ZjkwMGY4OGZkMzJmOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCdUlJZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDFlMTUxZTYzNmYzMWZjNGZhYjAyODUxZmZhNTMyMzdmNjBkZGY1YWU2YzU0ZDhkZDlhZTgzZmMzODg4MTZhZiIKICAgIH0KICB9Cn0=",
            "C6Uoga81Sl3GZ0bICjddbwHfnOEJfgHzMgCg9GdZ6OmS5fW0ilopjqElEGfrVIODsI8pJW8ycxOqupsRMR9xs3skWyQxrNGzVEhKkuoWB4EBiX4SLv1g1FnRHZjmiXvd+dam+jWHwQWmSoLnzipXfvNgKQGlCIDq3vFZm2pkW5pLoviTI5MkRx0k2PGk7fyF4wiHVoejekzwwPDbWxNHaqVqgP1ZoZiVYUaOZbgqtCGCfPKR+YIlecwoIsBBdlfc/Ndm3teDh7HJSACRAP+A4vQJa94t6GfPknzfc/TNi98UvlTHwAFlU/8FexoIUpz4JRRF8VsHCn/h+2WvhntdzbbisluHfvX9HQzmBc3nDpe5ncwGyhD0fbIjOu2kZssFRaXu0tV8Sso9puCV6q8Ad2jDoJcjni7ZpiAx8MwJcjzNqMfrwSoB27vYs2jNEKMiixCGMtMbWxpomHGA+91+LzhZPcxKM6eJ5ZI+azjdUTDzyYum+BQz661qtOb4TfLB9GBWOgfrlRDlbPN8iKQa5NkI8jLGm8zO5Ew/bMJ6b/k8sSglAq+owO/GGcj8PZMjaZBXq1VCe5qOVekXkT4SDGOdNn3kpdxuNrQhXSIB2GAHZl3O7IEAFxsLiEK/p3CgM9sfGUSiAO8ow2TuN/mC4dlPoRhSpMyjxb6TYDesT54=");
    private static final Skin DARK_AQUA = new Skin(
            "ewogICJ0aW1lc3RhbXAiIDogMTYzMDUyNzAxNDIwNywKICAicHJvZmlsZUlkIiA6ICJmZDQ3Y2I4YjgzNjQ0YmY3YWIyYmUxODZkYjI1ZmMwZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJDVUNGTDEyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzgyN2U0ODgyMmJjMGFmZmM4NWQzNWIzYzdiYjkwNTYyNDIxMjI5MzE3ODRiOWRkNTc3ZDZlYTk1ZDUxYWQ4OGUiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
            "f/Sb7m5nC/C11v10r4fokS5IsV/naKUB1j9zQueft0Cv1Ynr2ZbkL8zjr6pHLKLTjttIyVQBVlrRUUO2YOtzhRk07q59tJxEbH0S18PKyayIlOGRFPYmM7a6B0U9qioGj3Y10c61dX+2e9SdKP1JNrzmFfe2fMdQoGDtgZom8DwlX4uOy7S6D8+UDZh50Jsq33cKvgD6eeaVSY1gaxTULF1wDPYN+f0G/E9wuQWbNvTrShloJN4a3gC2/STqKcY6PAooj0cu5pY+z9ldIG1PqbVjPm6BEdkZ+r2dFfPw0d4Cq4KjTmmITJOU+rWu20TjQJfDh6ni9+2boN+5wu9P0f265m403QV3/8eyXlawsjPnf7gvBURRZyZaxpUBMtpC00e6qTUdEli3VcQZq65z4S4zdty/0kW8v8ap7wPstBNNLQdNm9vBhk/TU9WiXZzxZHuAxpka5SgLBQx92TsXRADQmnB4SnM6mYZuLL4bwcnaO8gbeBO2+xr0anQLcKLNYcgSHTIC2pNKcX7VYktWHfengmSXnahVAa14zLEuqy5N6dic1qU2aykpfFxGEnE8Jq/LR8MUhriDl9cFiNiKClBre+xt6YC+gZxyPkY3Vxy5pf3uuyU+/S6Xj5uDl5pPV4L8UDO69qatvnygqRqM+xwo0CRwa02F7J2RoRExNwU=");
    private static final Skin DARK_PURPLE = new Skin(
            "ewogICJ0aW1lc3RhbXAiIDogMTYxMjAyMTQ2NzY2NSwKICAicHJvZmlsZUlkIiA6ICIzM2ViZDMyYmIzMzk0YWQ5YWM2NzBjOTZjNTQ5YmE3ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYW5ub0JhbmFubm9YRCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hMjNiNjBiM2YxYjEwN2RiODQ4OWRkNGI3MjBiNTYwNjI2N2E3YzJiNjZhNWQwZGZmMjQwZDI1NjFkYjYwY2Q2IgogICAgfQogIH0KfQ==",
            "bLE+ZL+NKyqF30/lXJ6hoxigZUS0vPqFs+jcYiNtokbhPo2zb3ONel1pdT4C1Qm7ALCB9G+nnRU02ptriM2S6535d9fxBbwLaLzJ9C1CzbWmlDmgK+pb45fOBLUKHPNYCntU7GDYICtWUn+2Mz9cCsTf/lsyyke0Zy5tIACCV1C1kuOXsuo2+Y7mRmZoUlUdHqcYwSaA5eAl+5IT5k04lFxxIDj9zZPbh3rCH8mpdda+gkWQ5b2MJxPu+Cto1yrCbW5Q04JztcV7hKxU03X6T7dg4SiWC2lLNQc7BC7w69eLLK8gs92B3mIkSXaNHFJ9AAC7fLfBnzVKEeftshtNb1zSpjcdDAaq5QOFkbwD8ieQYQuH9dRsvJSmk+G8FpgT4qknm9XCoAWnhkGJuwZNMoLaaBkNOywcOHBdf4CbX1V/4TCi2sJvE+bE9B/XyMDi+e6N/7H1NFMfUlZVTTDeD2WK3n/AQaqcXNcWKGhf1L8ucuQYSshszRyXEWq+8Hyzc8naq7x31RKrMMMs7/uYTu4dq07lew6w1UXm1PJ3h7hh3cY6bxXhBoOvQYBnLmGBHqIdCIobmI57NzG2q2/VYFpkiKP68Hek7ZTHkJyuLHad4TDATckTVJOyJ2LNAGkEQIGLF84d4/lQAZIsNVoT7olcv/FCIYJanX+M8vJZQpo=");
    private static final Skin GRAY = new Skin(
            "ewogICJ0aW1lc3RhbXAiIDogMTYyMTU0MTc5NzE1NSwKICAicHJvZmlsZUlkIiA6ICI5OTdjZjFlMmY1NGQ0YzEyOWY2ZjU5ZTVlNjU1YjZmNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJpbzEyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzkxN2I0ZWQyNzFhNzk1MmRhNTA3ZWE2ZDk3NDZhNmI2MzYyMjQ5Y2NjNTY1OTIyZjRlNDdkMWY5ZGEyYWIxNGYiCiAgICB9CiAgfQp9",
            "fHEkWk+c3f7vae1iUQPlo3dEMuesaRMlTq3scb4o0VZwvnvywFD6CKnBiCVfC2FB1WpMaGbtWA6vhRYO9yVB9JS1IYCIwaI8XCprZMyrnjO32H9vtvdcr+WDRf2t1+1dm1iCH5o5U6rxzbYaZ5mgOp2/49vazNJbrsq2wgZ1mGuEXHubO9ldQrXp5YV+1V/2eGSO+lNn6fOUixKlDiDpUtLIxGqS3Mhr9M2dqYX/zz1GFUB5IGKUQKNfhbvPwOCCpubpvXBcqJ9liFf334KghrRQK2UugLfF2jsgOSlvsFIrEzUq/dykZDg7WCyRbUkn6J5PPqzzCIKuoIBdl6GQpchHbMv8Z5avpP4llVU0PrN6pefr3W6pcq24a6BmcFBTZI2F8IFrvn5BtPpv24Sq0BAfzbKOtk5enhQzkYg4Zhxr8MpqVYVRejt8LkrEaCeb+1uFbhrSaN9ZuB2voI2bWzU4WRztBNF6vxQWOD5w66mLC+v71ZHWjwBxg5YmpYXf9IL9AsTGdGQqkYh4S5ECQ9zdFdTNL0dM2luNMqLzm8OlU+pdGUMEWCzI7aIR4e7DuPi+IZBy8hJfgWJZWzQkZqPbidLAaij3EYXpPvq9at5mXW4pLnDqIdVOF/SLINhP8TL+pukp4Tq5R/GKhiLTyu66qajRIdjYZFN4OqMU2Ys=");
    private static final Skin GOLD = new Skin(
            "ewogICJ0aW1lc3RhbXAiIDogMTYwMjg1MDU1NTI0NCwKICAicHJvZmlsZUlkIiA6ICIwZjczMDA3NjEyNGU0NGM3YWYxMTE1NDY5YzQ5OTY3OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJPcmVfTWluZXIxMjMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTEzMmIxYzY5NzQxMzEwM2QxNjkyN2IyMTRkYTY5MmY3ZmZmZjg1MjdkYThkMzg2NDg1ZThkOGZkMmUyYTNiOSIKICAgIH0KICB9Cn0=",
            "dY6A5XFXhA5wF9D5hROJmUnyBt8eYTGIB2i6W1+ffLfT7uebBbFw8ucafgWM6nFEzZ1hhdNRm6oFe0D9DLAedvo8ZY6yQklxtZ56U3hQUyiWiyQLHQk62JyFN2024cDhgt7XIvPGWIlsWMjHXYXxV5xA04JoHb+OqyTM64QCZa0SM0E5Kxq3vXs3aMLOC14XPkWxMMFGsUOccIDgIujEx8c1tZDzBUuJH/+KVS5GKrx/YsqNTJk7q1qOVTFXOtCCnvoIG3A6YMn5i2x162PUIxXyQK9aDk+DJs9zig/QhEr9coaOyoRUR9eRuHREsh/A18Fba7wSK7XPp+9XCZy4pPYfUEOmVWWzsMVPyG9UYwh79fM8r2zAcZIZdY3PqfbqbGftluQDVEKNfWRq79ZXuxLKIHHLeLzMNJCx1b0ud6nK0lER/1fvcNy5DRYMmH+GP1iW3+xcT29MYSPT+QTn27V24nrgG4vljDA7HhhQ5zJVMN/k2kB3L8wbdj8WYg2YoDWR2y+DMDsxazhXVFPzMjz55DHRnKBsygn1TTZzqNnBWYeDnh638TyYGp1Z12ATyRvT0XzjTPoWknNQaG8azz/XW7SOH9B2bL/lsFZ0pwoY6HwaccvjWjJoMc7cKlsR1LkIUIjyFq17GYrXfFeSRwR7pYFobHT/IfcxT5IHmkY=");

    /** What each viewer has been sent, per slot. Weak, so a player who rejoins starts over. */
    private static final Map<Player, Line[]> sent = new WeakHashMap<>();
    /** Players whose tab list failed to build, so it's logged once each. */
    private static final Set<UUID> failed = new HashSet<>();

    public static void handle() {
        Bukkit.getScheduler().runTaskTimer(Dungeons.getInstance(), TabList::update, 200, 60);
    }

    private static void update() {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        sent.keySet().removeIf(player -> !player.isOnline());
        for (Player viewer : online) {
            for (Player other : online) {
                if (viewer.isListed(other)) viewer.unlistPlayer(other);
            }
            try {
                send(viewer, lines(viewer, online));
            } catch (RuntimeException e) {
                // One player's broken data shouldn't stop everyone else's tab list.
                if (failed.add(viewer.getUniqueId())) {
                    Dungeons.getInstance().getLogger().log(Level.WARNING, "Couldn't build " + viewer.getName() + "'s tab list", e);
                }
            }
        }
    }

    private static Line[] lines(Player viewer, Collection<? extends Player> online) {
        User user = User.getUser(viewer.getUniqueId());
        boolean hub = viewer.getWorld().getName().equalsIgnoreCase("world");
        List<Line> lines = new ArrayList<>(SLOTS);

        // Column 1: players.
        lines.add(hub
                ? new Line("      §a§a§lPlayers §f(" + online.size() + ")      ", GREEN)
                : new Line("        §b§b§lIsland       ", DARK_AQUA));
        List<Player> players = new ArrayList<>(online);
        players.sort(Comparator.comparing((Player p) -> String.valueOf(User.getUser(p.getUniqueId()).getRank().getCharacter()))
                .thenComparing(Player::getName));
        for (int i = 0; i < PLAYER_SLOTS; i++) {
            if (i < players.size()) {
                Player p = players.get(i);
                lines.add(new Line("" + User.getUser(p.getUniqueId()).getRank().getColor() + p.getName(), skin(p), p.getPing()));
            } else {
                lines.add(new Line("§3 ", GRAY));
            }
        }

        // Column 2.
        lines.add(hub
                ? new Line("      §a§lPlayers §f(" + online.size() + ")      ", GREEN)
                : new Line("         §d§lGuest      ", DARK_PURPLE));
        for (int i = 0; i < 19; i++) lines.add(new Line("§4 ", GRAY));

        // Column 3: server info.
        lines.add(new Line("      §3§lServer Info     ", DARK_AQUA));
        if (hub) {
            Region region = Region.regionCache.get(viewer.getUniqueId());
            lines.add(new Line("§b§lArea: §7" + (region != null ? region.getType().getName() : "Village"), GRAY));
            lines.add(new Line("§f Server: §8" + Dungeons.getSkyBlockServer().getName(), GRAY));
            lines.add(new Line("§f Gems: §a" + user.getGems(), GRAY));
            lines.add(new Line("§3§l§6", GRAY));
            if (user.getFaction() == null) {
                lines.add(new Line("§b§lFaction: §7None yet", GRAY));
                for (int i = 0; i < 3; i++) lines.add(new Line("§8§a§b" + i + " ", GRAY));
            } else {
                lines.add(new Line("§b§l" + user.getFaction().getName() + " Reputation:", GRAY));
                lines.add(new Line(" §c" + user.getFactionReputation(), GRAY));
                lines.add(new Line(" §cadd progress bar here", GRAY));
                lines.add(new Line("§d" + user.getFactionTitle().getName() + "       "
                        + (user.getFactionTitle().next() == null ? "§a§lMAXED!" : user.getFactionTitle().next().getName()), GRAY));
            }
            for (int i = 0; i < 11; i++) lines.add(new Line("§8§a§a ", GRAY));
        } else {
            lines.add(new Line("§b§lArea: §7Private Island", GRAY));
            lines.add(new Line("§f Server: §8" + Dungeons.getSkyBlockServer().getName(), GRAY));
            lines.add(new Line("§f Crystals: §d0", GRAY));
            for (int i = 0; i < 16; i++) lines.add(new Line("§8§a§1§a ", GRAY));
        }

        // Column 4: account info.
        lines.add(new Line("§6§l      Account Info", GOLD));
        lines.add(new Line("§e§lProfile: §cN/A", GRAY));
        lines.add(new Line("§f Bank: §6" + Utils.formatNumber(user.getBankBalance()) + "/50M", GRAY));
        lines.add(new Line("§8§1 ", GRAY));
        lines.add(new Line("§e§lSkills: ", GRAY));
        while (lines.size() < SLOTS) lines.add(new Line("§9 ", GRAY));
        return lines.toArray(new Line[0]);
    }

    /** A player's own skin, or gray if the server has none (offline mode). */
    private static Skin skin(Player player) {
        for (ProfileProperty property : player.getPlayerProfile().getProperties()) {
            if (property.getName().equals("textures") && property.getSignature() != null) {
                return new Skin(property.getValue(), property.getSignature());
            }
        }
        return GRAY;
    }

    private static void send(Player viewer, Line[] lines) {
        Line[] before = sent.get(viewer);
        List<UUID> removed = new ArrayList<>();
        List<PlayerInfo> added = new ArrayList<>();
        List<PlayerInfo> changed = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            Line line = lines[i];
            Line old = before == null ? null : before[i];
            if (line.equals(old)) continue;
            // A new skin needs the entry to be added again.
            if (old == null || !Objects.equals(old.skin(), line.skin())) {
                if (old != null) removed.add(IDS[i]);
                added.add(info(i, line));
            } else {
                changed.add(info(i, line));
            }
        }
        var players = PacketEvents.getAPI().getPlayerManager();
        if (!removed.isEmpty()) players.sendPacket(viewer, new WrapperPlayServerPlayerInfoRemove(removed));
        if (!added.isEmpty()) players.sendPacket(viewer, new WrapperPlayServerPlayerInfoUpdate(ADD, added));
        if (!changed.isEmpty()) players.sendPacket(viewer, new WrapperPlayServerPlayerInfoUpdate(CHANGE, changed));
        sent.put(viewer, lines);
    }

    private static PlayerInfo info(int slot, Line line) {
        UserProfile profile = new UserProfile(IDS[slot], String.format("!tab%02d", slot),
                List.of(new TextureProperty("textures", line.skin().texture(), line.skin().signature())));
        // Higher list order comes first, so slot 0 is the top of the first column.
        return new PlayerInfo(profile, true, line.ping(), GameMode.SURVIVAL,
                LegacyComponentSerializer.legacySection().deserialize(line.text()), null, SLOTS - slot, true);
    }
}
