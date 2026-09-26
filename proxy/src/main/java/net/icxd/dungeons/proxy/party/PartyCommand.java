package net.icxd.dungeons.proxy.party;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.icxd.dungeons.proxy.Chat;

/** {@code /party} ({@code /p}); {@code /party <player>...} invites. {@code /pc} and {@code /pl} are below. */
public final class PartyCommand implements SimpleCommand {
    private static final List<String> SUBCOMMANDS = List.of("accept", "chat", "demote", "disband", "help", "invite", "kick",
            "kickoffline", "leave", "list", "promote", "remove", "settings", "transfer", "warp");
    /** Subcommands that take a player name. */
    private static final List<String> WITH_PLAYER = List.of("accept", "demote", "invite", "kick", "promote", "remove", "transfer");

    private final PartyManager parties;
    private final ProxyServer proxy;

    public PartyCommand(PartyManager parties, ProxyServer proxy) {
        this.parties = parties;
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            Chat.send(invocation.source(), "§cOnly players can be in parties.");
            return;
        }
        String[] args = invocation.arguments();
        if (args.length == 0) {
            help(player);
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        if (WITH_PLAYER.contains(sub) && rest.length == 0) {
            Chat.send(player, "§cUsage: /party " + sub + " <player>");
            return;
        }
        switch (sub) {
            case "help" -> help(player);
            case "invite" -> {
                for (String name : rest) parties.invite(player, name);
            }
            case "accept" -> parties.accept(player, rest[0]);
            case "leave" -> parties.leave(player);
            case "list" -> parties.list(player);
            case "kick", "remove" -> parties.kick(player, rest[0]);
            case "kickoffline" -> parties.kickOffline(player);
            case "disband" -> parties.disband(player);
            case "transfer" -> parties.transfer(player, rest[0]);
            case "promote" -> parties.promote(player, rest[0]);
            case "demote" -> parties.demote(player, rest[0]);
            case "warp" -> parties.warp(player);
            case "chat" -> {
                if (rest.length == 0) Chat.send(player, "§cUsage: /party chat <message>");
                else parties.chat(player, String.join(" ", rest));
            }
            case "settings" -> {
                if (rest.length > 0 && rest[0].equalsIgnoreCase("allinvite")) parties.allInvite(player);
                else Chat.send(player, "§cUsage: /party settings allinvite");
            }
            // "/party Name Name2": invite them.
            default -> {
                for (String name : args) parties.invite(player, name);
            }
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String typed = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return Stream.concat(SUBCOMMANDS.stream(), players()).filter(s -> s.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && sub.equals("settings")) return List.of("allinvite");
        if (!WITH_PLAYER.contains(sub) && SUBCOMMANDS.contains(sub)) return List.of();
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return players().filter(n -> n.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    private Stream<String> players() {
        return proxy.getAllPlayers().stream().map(Player::getUsername);
    }

    private static void help(Player player) {
        player.sendMessage(Chat.framed(
                "§bParty Commands:",
                "§e/p accept <player> §8- §bAccept a party invite from a player",
                "§e/p chat <message> §8- §bSend a chat message to the entire party",
                "§e/p demote <player> §8- §bDemote a Party Moderator to Party Member",
                "§e/p disband §8- §bDisband the party",
                "§e/p invite <player> §8- §bInvite another player to your party",
                "§e/p kick <player> §8- §bRemove a player from your party",
                "§e/p kickoffline §8- §bRemove every offline player from your party",
                "§e/p leave §8- §bLeave the party",
                "§e/p list §8- §bList the players in your party",
                "§e/p promote <player> §8- §bPromote a player to Party Moderator, or a moderator to leader",
                "§e/p settings allinvite §8- §bLet every party member invite players",
                "§e/p transfer <player> §8- §bMake another player the party leader",
                "§e/p warp §8- §bBring your party to your server",
                "§e/pc <message> §8- §bShorthand for /p chat"));
    }

    /** {@code /pc <message>} ({@code /pchat}). */
    public static final class ChatCommand implements SimpleCommand {
        private final PartyManager parties;

        public ChatCommand(PartyManager parties) {
            this.parties = parties;
        }

        @Override
        public void execute(Invocation invocation) {
            if (!(invocation.source() instanceof Player player)) return;
            if (invocation.arguments().length == 0) Chat.send(player, "§cUsage: /pc <message>");
            else parties.chat(player, String.join(" ", invocation.arguments()));
        }
    }

    /** {@code /pl}: the party list. */
    public static final class ListCommand implements SimpleCommand {
        private final PartyManager parties;

        public ListCommand(PartyManager parties) {
            this.parties = parties;
        }

        @Override
        public void execute(Invocation invocation) {
            if (invocation.source() instanceof Player player) parties.list(player);
        }
    }
}
