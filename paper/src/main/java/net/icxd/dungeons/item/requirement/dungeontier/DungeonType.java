package net.icxd.dungeons.item.requirement.dungeontier;

public enum DungeonType {
    CATACOMBS,
    MASTER_CATACOMBS;

    public String getName() {
        StringBuilder name = new StringBuilder();
        for (String s : name().split("_"))
            name.append(s.charAt(0)).append(s.substring(1).toLowerCase()).append(" ");
        return name.toString().trim();
    }
}
