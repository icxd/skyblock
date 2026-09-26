package net.icxd.dungeons.proxy.party;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.velocitypowered.api.scheduler.ScheduledTask;

/** A party. Only touched while holding the {@link PartyManager}'s lock. */
public final class Party {
    public enum Role { LEADER, MODERATOR, MEMBER }

    UUID leader;
    /** Everyone in it, the leader included, in the order they joined. */
    final Set<UUID> members = new LinkedHashSet<>();
    final Set<UUID> moderators = new LinkedHashSet<>();
    /** Invited players, with the task that expires the invite. */
    final Map<UUID, ScheduledTask> invites = new HashMap<>();
    /** Members who disconnected, with the task that removes them if they don't come back. */
    final Map<UUID, ScheduledTask> disconnected = new HashMap<>();
    boolean allInvite;
    boolean disbanded;
    /** Goes up whenever someone joins or leaves, so a queued dungeon run can tell the party changed. */
    int version;

    Party(UUID leader) {
        this.leader = leader;
        members.add(leader);
    }

    Role role(UUID player) {
        if (player.equals(leader)) return Role.LEADER;
        return moderators.contains(player) ? Role.MODERATOR : Role.MEMBER;
    }

    /** What the rest of the proxy sees: a copy, safe to keep. */
    public record Snapshot(UUID leader, List<UUID> members, Set<UUID> offline, int version) {
    }

    Snapshot snapshot() {
        return new Snapshot(leader, List.copyOf(members), Set.copyOf(disconnected.keySet()), version);
    }
}
