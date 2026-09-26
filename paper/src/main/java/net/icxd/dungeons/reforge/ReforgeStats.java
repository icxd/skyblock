package net.icxd.dungeons.reforge;


public record ReforgeStats(ReforgeStat health, ReforgeStat defense, ReforgeStat intelligence, ReforgeStat speed,
                           ReforgeStat strength, ReforgeStat critChance, ReforgeStat critDamage,
                           ReforgeStat attackSpeed, ReforgeStat seaCreatureChance, ReforgeStat magicFind,
                           ReforgeStat ferocity, ReforgeStat miningSpeed, ReforgeStat miningFortune,
                           ReforgeStat farmingFortune, ReforgeStat foragingFortune) {
}
