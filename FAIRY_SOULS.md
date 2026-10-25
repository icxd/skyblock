# Fairy Souls

The Hub's 80 and the Dungeon Hub's 7, Tia the Fairy, and the Quest Log's count and guide, as on Hypixel. The code
is in `paper/src/main/java/net/icxd/dungeons/fairysouls`; the Quest Log's parts are in `.../quest/QuestMenus.java`.

## The data

- **Where they are**: `npcs/fairy_souls.json` in the data checkout, which is NEU's `constants/fairy_souls.json` as
  it was on 2026-10-25 (each island's souls as "x,y,z", the block the soul's head is in). servermgr links `npcs/`
  into every server; without the file there are no souls, and the log says so. Checked against the recordings:
  every soul stand that the four Hub recordings and the Dungeon Hub's passed (58 of the Hub's 80, all 7 of the
  Dungeon Hub's) stands where NEU has it, 1.46875 below that block, and no recorded soul is anywhere else. The
  Hub's other 22 each sit on the captured map's ground. Each island's count is the recorded Fairy Souls Guide's
  (a test checks).
- **The souls are the maps' own.** The captures have Hypixel's stands for every one of them, all 80 of the Hub's
  and all 7 of the Dungeon Hub's: invisible, silent armor stands wearing the Fairy Soul's head, signed as
  Hypixel's. So the plugin spawns none. The capture kept 12 of the Hub's two to four times over (Hypixel sent them
  again as it went on, each facing a new way). All but one go as their chunk's entities load (`SoulStands`).

## Done

- **Finding one**: a left or right click on a soul. The first time it says "&d&lSOUL! &fYou found a &dFairy
  Soul&r&f!", and after that "&dYou have already found that Fairy Soul!" (the wiki's Game Messages). Found ones
  still show. The profile keeps them in `fairySouls.found` ("hub:-233,86,84").
- **Tia the Fairy** stands at the Fairy Pond (119.5 65 147.5, the wiki's and NEU's spot; she was never recorded)
  in NEU's skin, which has no signature, under "&dTia the Fairy" and CLICK.
  - The first click: the wiki's four First Interaction lines.
  - After that, her menu, "Fairy" (the wiki's UI). The exchange in the middle shows how many souls they have to
    give, the next reward (+10 SkyBlock XP, and the backpack slot that exchange unlocks on Hypixel), and "You
    don't have enough Fairy Souls!" with fewer than five. With five or more, a click gives her five
    (`fairySouls.exchanges`).
- **SkyBlock XP**: the Fairy Souls task, 10 an exchange (570 at most). The action bar shows it as "+10 SkyBlock XP
  (Fairy Souls)".
- **The Quest Log**: "Find all Fairy Souls" counts them: " &c✖ &eFound: &d246&7/&d289" with 246, and "&70" with
  none, both as recorded. It opens the Fairy Souls Guide (recorded 2026-09-27): each island's head with "Fairy
  Souls: 1/80", the miscellaneous souls' kinds, and Go Back to the Quest Log.
- **/yoinkprofile**'s Fairy Souls part brings how many were found and exchanged (`fairy_soul.total_collected`,
  `fairy_exchanges`). The API doesn't say which, so those souls are on no island here, and the Hub's can be found
  again (never more than 289 in all).

## Approximated (UNKNOWN)

- The sound on finding one: none.
- Tia: which way she faces (north, at the pond), her lines' timing (a second apart, as Elizabeth's were) and voice
  (a villager's), and what she says on an exchange (nothing; the SkyBlock XP shows on the action bar). Her menu
  only opens from the click after the first talk.
- Tia's menu: its size (six rows, the wiki's default), the count with more than five ("Fairy Souls: 12/5"), and
  "Click to exchange!" with enough.
- The Quest Log's tick once every one is found.

## Not done

- **Backpack slots.** On Hypixel exchanges unlock them (slot 2 at the first, slot 18 at the 44th), and Tia's menu
  says which. Here every slot stays open, the stand-in from STORAGE.md: with only 87 souls to find (17
  exchanges, 9 slots), locking them would take most away.
- The other islands' souls, the Rift's, the 8 in dungeon rooms, fishing's, the Garden's and the Placeable Fairy
  Soul.
- "New players will be prompted to talk to Tia after they find their first few fairy souls" (the wiki's history):
  never recorded.
