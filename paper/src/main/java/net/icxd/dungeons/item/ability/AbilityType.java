package net.icxd.dungeons.item.ability;

public enum AbilityType {
    ABILITY,
    SHORTBOW,
    PIECE_BONUS,
    FULL_SET_BONUS;

    public String getName() {
        StringBuilder name = new StringBuilder();
        for (String s : name().split("_"))
            name.append(s.charAt(0)).append(s.substring(1).toLowerCase()).append(" ");
        return name.toString().trim();
    }
}
