package net.icxd.dungeons.item.requirement.slayer;

public enum SlayerBossType {
    ZOMBIE, SPIDER, WOLF, ENDERMAN, BLAZE;

    public String getName() {
        StringBuilder name = new StringBuilder();
        for (String s : name().split("_"))
            name.append(s.charAt(0)).append(s.substring(1).toLowerCase()).append(" ");
        return name.toString().trim();
    }
}
