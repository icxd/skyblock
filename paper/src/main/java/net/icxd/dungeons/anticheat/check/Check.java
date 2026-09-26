package net.icxd.dungeons.anticheat.check;

import lombok.Getter;

@Getter
public abstract class Check {
    private final String name;
    private final CheckType type;
    private final boolean enabled;

    public Check(String name, CheckType type, boolean enabled) {
        this.name = name;
        this.type = type;
        this.enabled = enabled;
    }
}
