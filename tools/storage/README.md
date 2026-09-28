# Storage data

The Accessory Bag's and the other bags' tables (see STORAGE.md) are Hypixel's, so they're kept in the
private data repository's `storage/`, which servermgr links into every server as
`plugins/dungeons/storage`. The script here makes two of them from public sources:

```
git clone --depth 1 https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO /tmp/neu
python3 tools/storage/build_storage.py --neu /tmp/neu                              # fetches the collections API
python3 tools/storage/build_storage.py --neu /tmp/neu --api collections.json --out DIR   # a saved response, elsewhere
```

- `accessories.json`: which accessories upgrade into which (NEU's `constants/misc.json`,
  `talisman_upgrades`), so only the best of a line counts.
- `bags.json`: each bag's size by its collection's tier (the collections API's "Potion Bag" and "+9
  Potion Bag Slots" unlocks), with the size a bag starts at, which the API doesn't say (the wiki's bag
  pages).

The third, `powers.json` (each Accessory Power's stats), comes from the wiki's Power Stone pages and a
recording, by a script kept with it in the private repository. By default the output goes to
`skyblock-dungeon-data/storage/` next to this repository's checkout. The plugin's storage tests read
the folder through `-Dstorage.dir` (else that checkout) and skip what needs it when it isn't there.
