package net.icxd.dungeons.item.ability.utility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.stats.StatsRunnable;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * Deployables' Deploy: Power Orbs ("Place an orb for 1m buffing up to 5 players within 18 blocks"), Flares
 * ("Shoot the flare up in the sky for 3m buffing up to 8 players within 40 blocks") and Lanterns, each with
 * the buff its item lists ("Orb Buff: Radiant": "Heal yourself for 30❤ per second.", "Heal others for 15❤ per
 * second.", "Gain +25❣ Health Regen.", "Grants +50% base mana regen.", stats; a Flare's "Heal 30❤ per second
 * while on the Crimson Isle and Kuudra"). Everything is the item's text, read once per item.
 *
 * <p>One of theirs at a time: deploying again takes the old one down with "§eYour previous §6Plasmaflux
 * Power Orb §ewas removed!" (the chat line SkyHanni filters). Each buffs the players nearest it within its
 * range, up to its number (whether its owner always counts among them is UNKNOWN: they come first here), and
 * "Only one deployable buff applies": a player gets the strongest of those buffing them, "the one of higher
 * rarity" (the wiki's Dwarven Lantern), the later one on a tie (UNKNOWN). The owner heals by the "yourself"
 * number, the rest by the "others" one times the owner's Mending (see {@link Heals}), each second from when it
 * was put down. What's on the stand is what the mods read: "§aRadiant §e57s", the seconds left (SkyHanni,
 * Skytils), with Plasmaflux pink and bold as they have it and other legendary ones bold (Skytils' and
 * SkyHanni's "§6§lWill-o'-wisp"); a Flare's name, where it hangs and when it goes are UNKNOWN but for its
 * owner going 80 blocks away (the wiki's Warning Flare): it hangs 8 blocks over where it was shot, and its
 * range is from there on the ground. An orb floats and turns where it was placed (its height and speed are
 * UNKNOWN). Not built: the Glacite Mineshaft ("Affects all players inside a Glacite Mineshaft"), Umberella's
 * rain, Kuudra's heal, the Mana Disintegrator, and the Totem of Corruption and Black Holes (fishing and hunting
 * aren't here): a deployable with nothing here to do isn't put down.
 */
final class Deployables implements AbilityHandler {
    /** "Grants +50% base mana regen". */
    private static final Pattern MANA_REGEN = Pattern.compile("\\+([\\d.]+)% base mana regen");
    /** "&aOrb Buff: Radiant", "&9Flare Buff: Alert Flare": its colour and name. */
    private static final Pattern BUFF = Pattern.compile("^(?:&[0-9a-fk-or])*&([0-9a-f])(?:&[0-9a-fk-or])*[A-Za-z]+ Buff: (.+)$");
    private static final double FLARE_HEIGHT = 8;
    /** "going 80 blocks away" takes a Flare down. */
    private static final double FLARE_LEASH = 80;
    private static final double ORB_HEIGHT = 1.2;

    /** What an item deploys, from its text. */
    record Kind(String itemId, String itemName, Rarity rarity, String label, boolean flare, long millis, double radius,
                int players, double healOwner, double healOthers, double islandHeal, double manaRegen, Stats stats) {
        /** Whether it does anything this server has. */
        boolean doesSomething() {
            return healOwner > 0 || healOthers > 0 || islandHeal > 0 || manaRegen > 0 || !stats.equals(new Stats());
        }
    }

    /** One that's out: whose, what, where (world and spot) and until when. */
    record Placed(UUID owner, Kind kind, UUID world, double x, double y, double z, long placedAt, long until) {
        double distanceSquared(double px, double py, double pz) {
            double dx = px - x, dy = py - y, dz = pz - z;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    /** A player who may be buffed, where they are. */
    record Near(UUID player, UUID world, double x, double y, double z) {
    }

    private static final class Out {
        final Placed placed;
        final ArmorStand stand;
        int ticks;

        Out(Placed placed, ArmorStand stand) {
            this.placed = placed;
            this.stand = stand;
        }
    }

    private static final Map<String, Kind> KINDS = new HashMap<>();
    /** Everyone's that are out, by owner. */
    private static final Map<UUID, Out> OUT = new LinkedHashMap<>();
    /** Who gets which buff this tick. */
    private static Map<UUID, Placed> buffed = Map.of();

    /** Its buff's stats and mana regen for whoever it buffs (once, when the server starts). */
    static void register() {
        PlayerStats.addModifier((player, stats) -> {
            Placed placed = buffed.get(player.getUniqueId());
            if (placed != null) stats.add(placed.kind().stats());
        });
        StatsRunnable.addManaRegenBonus(player -> {
            Placed placed = buffed.get(player.getUniqueId());
            return placed == null ? 0 : placed.kind().manaRegen();
        });
    }

    /** The item's deployable, from its ability's text and its own; null if it doesn't say enough. */
    static Kind kind(SkyBlockItem item, ItemBlock block) {
        return KINDS.computeIfAbsent(item.id(), id -> read(item, block));
    }

    static Kind read(SkyBlockItem item, ItemBlock block) {
        List<String> lines = new ArrayList<>(block.text());
        lines.addAll(item.lore());
        String ability = AbilityText.plain(block.text());
        String all = AbilityText.plain(lines);
        OptionalDouble millis = AbilityText.millis(ability);
        OptionalDouble radius = AbilityText.blocks(ability);
        String label = null;
        for (String line : lines) {
            Matcher m = BUFF.matcher(line);
            if (m.matches()) {
                label = label(m.group(1).charAt(0), m.group(2).trim(), item.rarity());
                break;
            }
        }
        if (millis.isEmpty() || radius.isEmpty() || label == null) return null;
        Matcher mana = MANA_REGEN.matcher(all);
        double manaRegen = mana.find() ? Double.parseDouble(mana.group(1)) / 100 : 0;
        boolean flare = ability.contains("Shoot the flare");
        double islandHeal = all.contains("while on the Crimson Isle") ? AbilityText.after(all, "Heal").orElse(0) : 0;
        return new Kind(item.id(), item.name(), item.rarity(), label, flare, (long) millis.getAsDouble(), radius.getAsDouble(),
                (int) AbilityText.players(ability).orElse(1), AbilityText.after(all, "Heal yourself for").orElse(0),
                AbilityText.after(all, "Heal others for").orElse(0), islandHeal, manaRegen, AbilityText.stats(all));
    }

    /** What its stand is called: "§aRadiant", legendary ones bold, Plasmaflux pink ("§d§lPlasmaflux", as the mods read it). */
    static String label(char color, String name, Rarity rarity) {
        if (name.equals("Plasmaflux")) return "§d§lPlasmaflux";
        return "§" + color + (rarity.ordinal() >= Rarity.LEGENDARY.ordinal() ? "§l" : "") + name;
    }

    /** "§aRadiant §e57s": seconds left, rounded up. */
    static String standName(String label, long millisLeft) {
        return label + " §e" + (long) Math.ceil(Math.max(0, millisLeft) / 1000.0) + "s";
    }

    /**
     * Who each deployable buffs: the players nearest it within its range (its owner first when they're in
     * range), up to its number; then each player gets the strongest of those that buff them (see {@link
     * #stronger}).
     */
    static Map<UUID, Placed> assign(List<Placed> placed, List<Near> players) {
        Map<UUID, Placed> out = new HashMap<>();
        for (Placed p : placed) {
            double range = p.kind().radius() * p.kind().radius();
            List<Near> inRange = new ArrayList<>();
            for (Near n : players) if (n.world().equals(p.world()) && p.distanceSquared(n.x(), n.y(), n.z()) <= range) inRange.add(n);
            inRange.sort(Comparator.<Near>comparingInt(n -> n.player().equals(p.owner()) ? 0 : 1)
                    .thenComparingDouble(n -> p.distanceSquared(n.x(), n.y(), n.z())));
            for (Near n : inRange.subList(0, Math.min(p.kind().players(), inRange.size()))) {
                out.merge(n.player(), p, (a, b) -> stronger(a, b) ? a : b);
            }
        }
        return out;
    }

    /** Whether {@code a} is the one that applies over {@code b}: higher rarity, else the later one. */
    static boolean stronger(Placed a, Placed b) {
        int rarity = Integer.compare(a.kind().rarity().ordinal(), b.kind().rarity().ordinal());
        return rarity != 0 ? rarity > 0 : a.placedAt() >= b.placedAt();
    }

    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Kind kind = kind(item, block);
        return kind != null && kind.doesSomething();
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Kind kind = kind(item, block);
        if (kind == null) return;
        Out previous = OUT.remove(player.getUniqueId());
        if (previous != null) {
            previous.stand.remove();
            SkyBlockItem old = ItemRegistry.get(previous.placed.kind().itemId());
            String name = old == null ? previous.placed.kind().itemName() : old.rarity().getColor() + old.name();
            player.sendMessage("§eYour previous §r" + name + " §r§ewas removed!");
        }
        Location at = player.getLocation();
        long now = System.currentTimeMillis();
        Placed placed = new Placed(player.getUniqueId(), kind, at.getWorld().getUID(), at.getX(), at.getY(), at.getZ(), now, now + kind.millis());
        Location standAt = at.clone().add(0, kind.flare() ? FLARE_HEIGHT : ORB_HEIGHT, 0);
        standAt.setPitch(0);
        ArmorStand stand = at.getWorld().spawn(standAt, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setInvulnerable(true);
            s.setMarker(true);
            s.setSmall(true);
            s.setPersistent(false);
            s.setCustomNameVisible(true);
            s.customName(Text.line(standName(kind.label(), kind.millis())));
            s.getEquipment().setHelmet(look(item));
            s.addScoreboardTag(UtilityListener.NOT_A_MOB);
        });
        OUT.put(player.getUniqueId(), new Out(placed, stand));
        if (kind.flare()) {
            at.getWorld().playSound(at, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1);
            at.getWorld().spawnParticle(Particle.FIREWORK, at.clone().add(0, 1, 0), 30, 0.1, FLARE_HEIGHT / 2, 0.1, 0.05);
        } else {
            at.getWorld().playSound(at, Sound.BLOCK_BEACON_ACTIVATE, 1, 1.5f);
        }
    }

    /** What the stand wears: the item's head, or a firework for a flare. */
    private static ItemStack look(SkyBlockItem item) {
        if (item.material() != Material.PLAYER_HEAD || item.skin() == null) return new ItemStack(item.material());
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        Utils.skull(head, item.skin());
        return head;
    }

    /** Every tick: stands turn and count down, old ones go, who gets what is worked out, and every second they heal. */
    static void tick() {
        long now = System.currentTimeMillis();
        List<Placed> placed = new ArrayList<>();
        for (Map.Entry<UUID, Out> entry : List.copyOf(OUT.entrySet())) {
            Out out = entry.getValue();
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (now >= out.placed.until() || owner == null || !out.stand.isValid() || leftBehind(owner, out.placed)) {
                out.stand.remove();
                OUT.remove(entry.getKey());
                continue;
            }
            placed.add(out.placed);
            out.ticks++;
            if (!out.placed.kind().flare()) {
                Location at = out.stand.getLocation();
                at.setYaw(at.getYaw() + 6);
                at.setY(out.placed.y() + ORB_HEIGHT + Math.sin(out.ticks / 10.0) * 0.1);
                out.stand.teleport(at);
            }
            if (out.ticks % 20 == 0) out.stand.customName(Text.line(standName(out.placed.kind().label(), out.placed.until() - now)));
        }
        List<Near> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead()) continue;
            DungeonRun run = RunManager.of(player);
            if (run != null && run.isGhost(player.getUniqueId())) continue;
            Location at = player.getLocation();
            players.add(new Near(player.getUniqueId(), at.getWorld().getUID(), at.getX(), at.getY(), at.getZ()));
        }
        buffed = placed.isEmpty() ? Map.of() : assign(placed, players);
        for (Map.Entry<UUID, Placed> entry : buffed.entrySet()) {
            Placed p = entry.getValue();
            Out out = OUT.get(p.owner());
            if (out == null || out.ticks % 20 != 0) continue;
            heal(Bukkit.getPlayer(p.owner()), Bukkit.getPlayer(entry.getKey()), p.kind());
        }
    }

    /** A Flare's owner 80 blocks from it, or in another world. */
    private static boolean leftBehind(Player owner, Placed placed) {
        if (!owner.getWorld().getUID().equals(placed.world())) return true;
        Location at = owner.getLocation();
        return placed.kind().flare() && placed.distanceSquared(at.getX(), at.getY(), at.getZ()) > FLARE_LEASH * FLARE_LEASH;
    }

    /** A second's heal from one: the owner's number for the owner, the others' for the rest, a Flare's on the Crimson Isle. */
    private static void heal(Player owner, Player player, Kind kind) {
        if (owner == null || player == null) return;
        double amount = player.equals(owner) ? kind.healOwner() : kind.healOthers();
        if (kind.islandHeal() > 0 && Dungeons.getSkyBlockServer().getServerType() == ServerType.CRIMSON_ISLE) amount += kind.islandHeal();
        if (amount > 0) Heals.give(owner, player, amount);
    }

    /** They've left: theirs goes. */
    static void forget(UUID owner) {
        Out out = OUT.remove(owner);
        if (out != null) out.stand.remove();
    }

    /** The plugin's stopping: every stand goes. */
    static void removeAll() {
        for (Out out : OUT.values()) out.stand.remove();
        OUT.clear();
        buffed = Map.of();
    }
}
