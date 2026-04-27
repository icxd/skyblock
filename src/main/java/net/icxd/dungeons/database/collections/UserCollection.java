package net.icxd.dungeons.database.collections;

import net.icxd.dungeons.database.ICollection;
import net.icxd.dungeons.dwarven.Perk;
import net.icxd.dungeons.dwarven.PowderType;
import net.icxd.dungeons.user.Rank;
import org.bson.Document;

import java.util.ArrayList;

public class UserCollection implements ICollection {

  @Override
  public String name() {
    return "users";
  }

  @Override
  public Document defaultDocument() {
    return new Document()
        .append("uuid", "")
        .append("username", "")
        .append("ip", "")
        .append("rank", Rank.DEFAULT.name())
        .append("coins", 0)
        .append("bits", 0)
        .append("gems", 0)
        .append("skills", new Document()
            .append("combat", 0))
        .append("dungeons", new Document()
            .append("essence", new Document()
                .append("wither", 0)
                .append("spider", 0)
                .append("undead", 0)
                .append("dragon", 0)
                .append("gold", 0)
                .append("diamond", 0)
                .append("ice", 0)
                .append("crimson", 0))
            .append("floors", new Document()
                .append("highest", 0))
            .append("classExp", new Document()
                .append("HEALER", 0)
                .append("MAGE", 0)
                .append("BERSERK", 0)
                .append("ARCHER", 0)
                .append("TANK", 0))
            .append("selectedClass", "HEALER"))
        .append("crimsonIsle", new Document()
            .append("factions", new Document()
                .append("barbarian", new Document()
                    .append("reputation", 0))
                .append("mage", new Document()
                    .append("reputation", 0)))
            .append("kuudra", new Document()
                .append("highest", 0))
            .append("selectedFaction", null))
        .append("dwarvenMines", new Document()
            .append("powder", new Document()
                .append(PowderType.MITHRIL.name(), 0)
                .append(PowderType.GEMSTONE.name(), 0)
                .append(PowderType.GLACITE.name(), 0))
            .append("commissions", new Document()
                .append("completed", 0)
                .append("slot1", new Document())
                .append("slot2", new Document())
                .append("slot3", new Document())
                .append("slot4", new Document()))
            .append("hotm", new Document()
                .append("tokens", 0)
                .append("xp", 0)
                .append("tree", new Document()
                    .append(Perk.MINING_SPEED.name(), 0)
                    .append(Perk.MINING_FORTUNE.name(), 0)
                    .append(Perk.TITANIUM_INSANIUM.name(), 0)
                    .append(Perk.QUICK_FORGE.name(), 0))))
        .append("bank", new Document()
            .append("balance", 0)
            .append("transactions", new ArrayList<Document>()))
        .append("storage", new Document()
            .append("inventory", null)
            .append("armor", null))
        .append("minions", new Document()
            .append("craftedMinions", new ArrayList<Document>())
            .append("minions", new ArrayList<Document>()))
        .append("firstLogin", System.currentTimeMillis())
        .append("lastLogin", System.currentTimeMillis());
  }
}
