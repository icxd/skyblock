package net.icxd.dungeons.proxy.party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.icxd.dungeons.proxy.Chat;
import net.icxd.dungeons.proxy.Profiles;
import net.icxd.dungeons.proxy.Transfers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * Parties, as on Hypixel: {@code /party <player>} starts one, invites last 60 seconds, the leader
 * can promote moderators who may kick and invite, and members who disconnect have 5 minutes to
 * come back (the party is disbanded if that's the leader). Parties live on the proxy, so they
 * follow players between servers; they're gone when it restarts.
 *
 * <p>Everything runs under this object's lock; messages are sent from inside it, which only
 * queues them.
 */
public final class PartyManager {
    private static final long INVITE_SECONDS = 60;
    private static final long DISCONNECT_MINUTES = 5;

    private final Object plugin;
    private final ProxyServer proxy;
    private final Profiles profiles;
    private final Transfers transfers;
    private final int maxSize;
    private final Map<UUID, Party> byMember = new HashMap<>();

    public PartyManager(Object plugin, ProxyServer proxy, Profiles profiles, Transfers transfers, int maxSize) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.profiles = profiles;
        this.transfers = transfers;
        this.maxSize = maxSize;
    }

    public synchronized Optional<Party.Snapshot> partyOf(UUID player) {
        return Optional.ofNullable(byMember.get(player)).map(Party::snapshot);
    }

    // Invites

    public synchronized void invite(Player inviter, String name) {
        Optional<Player> found = profiles.online(name);
        if (found.isEmpty()) {
            Chat.send(inviter, "§cCouldn't find a player with that name!");
            return;
        }
        Player target = found.get();
        UUID from = inviter.getUniqueId();
        UUID to = target.getUniqueId();
        if (to.equals(from)) {
            Chat.send(inviter, "§cYou cannot party yourself!");
            return;
        }
        Party party = byMember.get(from);
        if (party != null) {
            if (party.role(from) == Party.Role.MEMBER && !party.allInvite) {
                Chat.send(inviter, "§cYou are not allowed to invite players.");
                return;
            }
            if (party.members.contains(to)) {
                Chat.send(inviter, "§c" + profiles.display(to) + " §cis already in the party.");
                return;
            }
            if (party.invites.containsKey(to)) {
                Chat.send(inviter, "§cYou have already invited that player! Wait for them to accept or for the invite to expire.");
                return;
            }
            if (party.members.size() + party.invites.size() >= maxSize) {
                Chat.send(inviter, "§cYour party is full!");
                return;
            }
        } else {
            party = new Party(from);
            byMember.put(from, party);
        }
        Party invitedTo = party;
        party.invites.put(to, proxy.getScheduler().buildTask(plugin, () -> expire(invitedTo, to)).delay(INVITE_SECONDS, TimeUnit.SECONDS).schedule());

        tell(party, Chat.framed(profiles.display(from) + " §einvited " + profiles.display(to) + " §eto the party! They have §c"
                + INVITE_SECONDS + " §eseconds to accept."));
        String accept = "/party accept " + inviter.getUsername();
        Component join = Chat.text("§eYou have §c" + INVITE_SECONDS + " §eseconds to accept. ")
                .append(Chat.text("§6Click here to join!")
                        .clickEvent(ClickEvent.runCommand(accept))
                        .hoverEvent(HoverEvent.showText(Chat.text("§eClick to run\n" + accept))));
        target.sendMessage(Chat.framed(Chat.text(profiles.display(from) + " §ehas invited you to join their party!"), join));
    }

    private synchronized void expire(Party party, UUID invited) {
        if (party.disbanded || party.invites.remove(invited) == null) return;
        tell(party, Chat.framed("§eThe party invite to " + profiles.display(invited) + " §ehas expired"));
        proxy.getPlayer(invited).ifPresent(p -> p.sendMessage(Chat.framed("§eThe party invite from " + profiles.display(party.leader) + " §ehas expired")));
        disbandIfEmpty(party);
    }

    public synchronized void accept(Player player, String name) {
        UUID uuid = player.getUniqueId();
        Party party = null;
        for (Party p : new ArrayList<>(byMember.values())) {
            if (p.invites.containsKey(uuid) && p.members.stream().anyMatch(m -> profiles.get(m).name().equalsIgnoreCase(name))) {
                party = p;
                break;
            }
        }
        if (party == null) {
            boolean inParty = byMember.values().stream().anyMatch(p -> p.members.stream().anyMatch(m -> profiles.get(m).name().equalsIgnoreCase(name)));
            Chat.send(player, inParty ? "§cYou don't have an invite to that player's party." : "§cThat party has been disbanded.");
            return;
        }
        Party current = byMember.get(uuid);
        if (current != null && current.members.size() > 1) {
            Chat.send(player, "§cYou are already in a party! Leave it to join another one.");
            return;
        }
        if (party.members.size() >= maxSize) {
            Chat.send(player, "§cThat party is full!");
            return;
        }
        // Just themselves and some invites: dropped for the party they're joining.
        if (current != null) disband(current);
        party.invites.remove(uuid).cancel();
        tell(party, Chat.framed(profiles.display(uuid) + " §ejoined the party."));
        party.members.add(uuid);
        party.version++;
        byMember.put(uuid, party);
        player.sendMessage(Chat.framed("§eYou have joined " + profiles.display(party.leader) + "'s §eparty!"));
    }

    // Leaving

    public synchronized void leave(Player player) {
        Party party = partyFor(player);
        if (party == null) return;
        UUID uuid = player.getUniqueId();
        boolean wasLeader = uuid.equals(party.leader);
        remove(party, uuid);
        player.sendMessage(Chat.framed("§eYou left the party."));
        if (party.disbanded) return;
        String who = profiles.display(uuid);
        tell(party, Chat.framed(wasLeader
                ? "§eThe party was transferred to " + profiles.display(party.leader) + " §ebecause " + who + " §eleft"
                : who + " §ehas left the party."));
        disbandIfEmpty(party);
    }

    public synchronized void kick(Player player, String name) {
        Party party = partyFor(player);
        if (party == null) return;
        Party.Role role = party.role(player.getUniqueId());
        UUID target = member(party, name);
        if (target == null) {
            Chat.send(player, "§cThat player is not in your party!");
            return;
        }
        if (target.equals(player.getUniqueId())) {
            Chat.send(player, "§cYou cannot kick yourself! Use /party leave instead.");
            return;
        }
        if (role == Party.Role.MEMBER || (role == Party.Role.MODERATOR && party.role(target) != Party.Role.MEMBER)) {
            Chat.send(player, "§cYou are not allowed to kick that player.");
            return;
        }
        remove(party, target);
        tell(party, Chat.framed(profiles.display(target) + " §ehas been removed from the party."));
        proxy.getPlayer(target).ifPresent(p -> p.sendMessage(Chat.framed("§eYou have been kicked from the party by " + profiles.display(player.getUniqueId()))));
        disbandIfEmpty(party);
    }

    public synchronized void kickOffline(Player player) {
        Party party = leaderParty(player);
        if (party == null) return;
        if (party.disconnected.isEmpty()) {
            Chat.send(player, "§cThere are no offline players in your party.");
            return;
        }
        List<String> names = new ArrayList<>();
        for (UUID offline : List.copyOf(party.disconnected.keySet())) {
            names.add(profiles.display(offline));
            remove(party, offline);
        }
        tell(party, Chat.framed("§eKicked " + String.join("§e, ", names) + " §ebecause they were offline."));
        disbandIfEmpty(party);
    }

    public synchronized void disband(Player player) {
        Party party = leaderParty(player);
        if (party == null) return;
        tell(party, Chat.framed(profiles.display(party.leader) + " §ehas disbanded the party!"));
        disband(party);
    }

    // Roles

    public synchronized void transfer(Player player, String name) {
        Party party = leaderParty(player);
        if (party == null) return;
        UUID target = member(party, name);
        if (target == null || target.equals(party.leader)) {
            Chat.send(player, target == null ? "§cThat player is not in your party!" : "§cYou are already the party leader!");
            return;
        }
        makeLeader(party, target);
        tell(party, Chat.framed("§eThe party was transferred to " + profiles.display(target) + " §eby " + profiles.display(player.getUniqueId())));
    }

    public synchronized void promote(Player player, String name) {
        Party party = leaderParty(player);
        if (party == null) return;
        UUID target = member(party, name);
        if (target == null || target.equals(party.leader)) {
            Chat.send(player, target == null ? "§cThat player is not in your party!" : "§cYou are already the party leader!");
            return;
        }
        String by = profiles.display(player.getUniqueId());
        if (party.moderators.contains(target)) {
            makeLeader(party, target);
            tell(party, Chat.framed(by + " §ehas promoted " + profiles.display(target) + " §eto Party Leader"));
        } else {
            party.moderators.add(target);
            tell(party, Chat.framed(by + " §ehas promoted " + profiles.display(target) + " §eto Party Moderator"));
        }
    }

    public synchronized void demote(Player player, String name) {
        Party party = leaderParty(player);
        if (party == null) return;
        UUID target = member(party, name);
        if (target == null || !party.moderators.remove(target)) {
            Chat.send(player, target == null ? "§cThat player is not in your party!" : "§c" + profiles.display(target) + " §cis already a Party Member.");
            return;
        }
        tell(party, Chat.framed(profiles.display(player.getUniqueId()) + " §ehas demoted " + profiles.display(target) + " §eto Party Member"));
    }

    public synchronized void allInvite(Player player) {
        Party party = leaderParty(player);
        if (party == null) return;
        party.allInvite = !party.allInvite;
        tell(party, Chat.framed(profiles.display(player.getUniqueId()) + (party.allInvite ? " §aenabled All Invite" : " §cdisabled All Invite")));
    }

    // Everyone

    public synchronized void list(Player player) {
        Party party = partyFor(player);
        if (party == null) return;
        List<Component> lines = new ArrayList<>();
        lines.add(Chat.text("§6Party Members (" + party.members.size() + ")"));
        lines.add(Component.empty());
        lines.add(Chat.text("§eParty Leader: " + entry(party, party.leader)));
        List<String> mods = new ArrayList<>();
        List<String> members = new ArrayList<>();
        for (UUID member : party.members) {
            if (member.equals(party.leader)) continue;
            (party.moderators.contains(member) ? mods : members).add(entry(party, member));
        }
        if (!mods.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Chat.text("§eParty Moderators: " + String.join(" ", mods)));
        }
        if (!members.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Chat.text("§eParty Members: " + String.join(" ", members)));
        }
        player.sendMessage(Chat.framed(lines.toArray(Component[]::new)));
    }

    private String entry(Party party, UUID member) {
        return profiles.display(member) + (party.disconnected.containsKey(member) ? " §c●" : " §a●");
    }

    public synchronized void chat(Player player, String message) {
        Party party = partyFor(player);
        if (party == null) return;
        // What they typed stays plain text: no colour codes from players.
        tell(party, Chat.text("§9Party §8> " + profiles.display(player.getUniqueId()) + "§f: ").append(Component.text(message)));
    }

    /** Brings every member to the leader's server. */
    public synchronized void warp(Player player) {
        Party party = leaderParty(player);
        if (party == null) return;
        Optional<RegisteredServer> here = player.getCurrentServer().map(ServerConnection::getServer);
        if (here.isEmpty()) return;
        int summoned = 0;
        for (UUID member : party.members) {
            Optional<Player> online = proxy.getPlayer(member);
            if (member.equals(party.leader) || online.isEmpty()) continue;
            if (online.get().getCurrentServer().map(c -> c.getServer().equals(here.get())).orElse(false)) continue;
            summoned++;
            online.get().sendMessage(Chat.framed("§eParty Leader, " + profiles.display(party.leader) + "§e, summoned you to their server."));
            String display = profiles.display(member);
            transfers.connect(online.get(), here.get()).thenAccept(ok -> {
                if (ok) Chat.send(player, "§eYou summoned " + display + " §eto your server.");
                else Chat.send(player, "§c" + display + " §ccouldn't be warped to your server.");
            });
        }
        if (summoned == 0) Chat.send(player, "§cEveryone in your party is already on your server.");
    }

    // Connections

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        // Not a second login refused while they're still on.
        if (event.getLoginStatus() == DisconnectEvent.LoginStatus.CONFLICTING_LOGIN) return;
        disconnected(event.getPlayer().getUniqueId());
    }

    private synchronized void disconnected(UUID uuid) {
        Party party = byMember.get(uuid);
        if (party == null || party.disconnected.containsKey(uuid)) return;
        if (party.members.size() == 1) {
            // Just them and some invites.
            disband(party);
            return;
        }
        String display = profiles.display(uuid);
        boolean leader = uuid.equals(party.leader);
        party.disconnected.put(uuid, proxy.getScheduler().buildTask(plugin, () -> timedOut(party, uuid)).delay(DISCONNECT_MINUTES, TimeUnit.MINUTES).schedule());
        tell(party, Chat.framed(leader
                ? "§eThe party leader, " + display + " §ehas disconnected, they have §c" + DISCONNECT_MINUTES + " §eminutes to rejoin before the party is disbanded."
                : display + " §ehas disconnected, they have §c" + DISCONNECT_MINUTES + " §eminutes to rejoin before they are removed from the party."));
    }

    private synchronized void timedOut(Party party, UUID uuid) {
        if (party.disbanded || party.disconnected.remove(uuid) == null) return;
        if (uuid.equals(party.leader)) {
            tell(party, Chat.framed("§cThe party was disbanded because the party leader disconnected."));
            disband(party);
            return;
        }
        remove(party, uuid);
        tell(party, Chat.framed(profiles.display(uuid) + " §ewas removed from your party because they disconnected."));
        disbandIfEmpty(party);
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        rejoined(event.getPlayer().getUniqueId());
    }

    private synchronized void rejoined(UUID uuid) {
        Party party = byMember.get(uuid);
        if (party == null) return;
        var task = party.disconnected.remove(uuid);
        if (task == null) return;
        task.cancel();
        String display = profiles.display(uuid);
        tell(party, Chat.framed(uuid.equals(party.leader) ? "§eThe party leader " + display + " §ehas rejoined." : display + " §ehas rejoined."));
    }

    // Helpers (lock held)

    /** Their party, or null after telling them they aren't in one. */
    private Party partyFor(Player player) {
        Party party = byMember.get(player.getUniqueId());
        if (party == null) Chat.send(player, "§cYou are not in a party right now.");
        return party;
    }

    /** Their party if they lead it, or null after telling them why not. */
    private Party leaderParty(Player player) {
        Party party = partyFor(player);
        if (party != null && !party.leader.equals(player.getUniqueId())) {
            Chat.send(player, "§cYou are not this party's leader!");
            return null;
        }
        return party;
    }

    private UUID member(Party party, String name) {
        for (UUID member : party.members) {
            if (profiles.get(member).name().equalsIgnoreCase(name)) return member;
        }
        return null;
    }

    private void makeLeader(Party party, UUID target) {
        party.moderators.remove(target);
        party.moderators.add(party.leader);
        party.leader = target;
    }

    /** Takes someone out; if they led it, the next moderator (or member) takes over. */
    private void remove(Party party, UUID uuid) {
        party.members.remove(uuid);
        party.moderators.remove(uuid);
        party.version++;
        byMember.remove(uuid, party);
        var task = party.disconnected.remove(uuid);
        if (task != null) task.cancel();
        if (uuid.equals(party.leader) && !party.members.isEmpty()) {
            UUID next = party.moderators.isEmpty() ? party.members.iterator().next() : party.moderators.iterator().next();
            party.moderators.remove(next);
            party.leader = next;
        } else if (party.members.isEmpty()) {
            disband(party);
        }
    }

    private void disbandIfEmpty(Party party) {
        if (party.disbanded || party.members.size() > 1 || !party.invites.isEmpty()) return;
        tell(party, Chat.framed("§cThe party was disbanded because all invites expired and the party was empty."));
        disband(party);
    }

    private void disband(Party party) {
        party.disbanded = true;
        party.invites.values().forEach(t -> t.cancel());
        party.disconnected.values().forEach(t -> t.cancel());
        for (UUID member : party.members) byMember.remove(member, party);
    }

    /** To every member who's online. */
    private void tell(Party party, Component message) {
        for (UUID member : party.members) proxy.getPlayer(member).ifPresent(p -> p.sendMessage(message));
    }
}
