package net.icxd.dungeons.dungeons.generation.secrets;

import org.bukkit.Location;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Secret {
    private final Location position;
    private final SecretType type;
}