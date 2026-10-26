package net.icxd.dungeons;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.icxd.dungeons.anticheat.check.CheckListener;
import net.icxd.dungeons.collection.CollectionFiles;
import net.icxd.dungeons.collection.CollectionGains;
import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.command.commands.admin.SetCollectionCommand;
import net.icxd.dungeons.command.commands.admin.YoinkProfileCommand;
import net.icxd.dungeons.command.commands.user.CollectionCommand;
import net.icxd.dungeons.command.commands.user.CraftCommand;
import net.icxd.dungeons.command.commands.user.RecipesCommand;
import net.icxd.dungeons.command.commands.user.ViewBossCollectionCommand;
import net.icxd.dungeons.command.commands.user.ViewCollectionCommand;
import net.icxd.dungeons.command.commands.user.ViewRecipeCommand;
import net.icxd.dungeons.recipe.CraftingTable;
import net.icxd.dungeons.recipe.VanillaCrafting;
import net.icxd.dungeons.command.commands.admin.AddEnchantmentCommand;
import net.icxd.dungeons.command.commands.admin.DataCommand;
import net.icxd.dungeons.command.commands.admin.DungeonCommand;
import net.icxd.dungeons.command.commands.admin.GivePetCommand;
import net.icxd.dungeons.command.commands.admin.ItemCommand;
import net.icxd.dungeons.command.commands.admin.NBTCommand;
import net.icxd.dungeons.command.commands.admin.PlayerDataCommand;
import net.icxd.dungeons.command.commands.admin.RecombobulateCommand;
import net.icxd.dungeons.command.commands.admin.SpawnEntityCommand;
import net.icxd.dungeons.command.commands.admin.SpawnRewardChestCommand;
import net.icxd.dungeons.command.commands.admin.UnlockCommand;
import net.icxd.dungeons.command.commands.admin.UpgradeCommand;
import net.icxd.dungeons.command.commands.user.BackpackCommand;
import net.icxd.dungeons.command.commands.user.BagsCommand;
import net.icxd.dungeons.command.commands.user.CbCommand;
import net.icxd.dungeons.command.commands.user.EnderChestCommand;
import net.icxd.dungeons.command.commands.user.HexCommand;
import net.icxd.dungeons.command.commands.user.HotmCommand;
import net.icxd.dungeons.command.commands.user.IsCommand;
import net.icxd.dungeons.command.commands.user.QuestsCommand;
import net.icxd.dungeons.command.commands.user.ChaptersCommand;
import net.icxd.dungeons.command.commands.user.ClaimItemCommand;
import net.icxd.dungeons.command.commands.user.LevelsCommand;
import net.icxd.dungeons.command.commands.user.LoadoutsCommand;
import net.icxd.dungeons.command.commands.user.PetsCommand;
import net.icxd.dungeons.command.commands.user.PickupStashCommand;
import net.icxd.dungeons.command.commands.user.ProfilesCommand;
import net.icxd.dungeons.command.commands.user.SacksCommand;
import net.icxd.dungeons.command.commands.user.SbMenuCommand;
import net.icxd.dungeons.command.commands.user.ShowExtraStatsCommand;
import net.icxd.dungeons.command.commands.user.SkillsCommand;
import net.icxd.dungeons.command.commands.user.SkyBlockXpCommand;
import net.icxd.dungeons.command.commands.user.StatsCommand;
import net.icxd.dungeons.command.commands.user.StorageCommand;
import net.icxd.dungeons.command.commands.user.ToggleReadyUpCommand;
import net.icxd.dungeons.command.commands.user.ViewStashCommand;
import net.icxd.dungeons.combat.Deaths;
import net.icxd.dungeons.economy.DeathCoins;
import net.icxd.dungeons.economy.KillCoins;
import net.icxd.dungeons.gui.GUIListener;
import net.icxd.dungeons.leveling.LevelingSources;
import net.icxd.dungeons.leveling.SkyBlockLevels;
import net.icxd.dungeons.listeners.CombatListener;
import net.icxd.dungeons.listeners.HealthListener;
import net.icxd.dungeons.listeners.HubProtection;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.listeners.PlayerListener;
import net.icxd.dungeons.listeners.VanillaProgress;
import net.icxd.dungeons.listeners.WorldListener;
import net.icxd.dungeons.menu.SkyBlockMenuItem;
import net.icxd.dungeons.menu.SkyBlockMenuListener;
import net.icxd.dungeons.mining.BlockListener;
import net.icxd.dungeons.region.MovementListener;
import net.icxd.dungeons.command.Callbacks;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.database.collections.UserCollection;
import net.icxd.dungeons.database.mongo.Settings;
import net.icxd.dungeons.common.Runs;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.combat.Shots;
import net.icxd.dungeons.item.ability.Activations;
import net.icxd.dungeons.item.enchanting.weapon.WeaponEnchants;
import net.icxd.dungeons.reforge.CombatReforges;
import net.icxd.dungeons.item.ability.weapons.WeaponEvents;
import net.icxd.dungeons.item.ability.weapons.WeaponPassives;
import net.icxd.dungeons.mob.Essences;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.museum.Museum;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.ability.utility.UtilityListener;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.ability.utility.UtilityAbilities;
import net.icxd.dungeons.item.ability.weapons.WeaponAbilities;
import net.icxd.dungeons.item.enchanting.armor.ArmorEnchants;
import net.icxd.dungeons.item.upgrade.BookOfStats;
import net.icxd.dungeons.network.ProxyLink;
import net.icxd.dungeons.npc.Hologram;
import net.icxd.dungeons.npc.Npcs;
import net.icxd.dungeons.mob.DropEnchants;
import net.icxd.dungeons.economy.ExpBonuses;
import net.icxd.dungeons.npc.Ophelia;
import net.icxd.dungeons.pet.AutopetListener;
import net.icxd.dungeons.pet.George;
import net.icxd.dungeons.pet.HeldItemEffects;
import net.icxd.dungeons.pet.PetCopies;
import net.icxd.dungeons.pet.PetFollowers;
import net.icxd.dungeons.pet.PetItemUse;
import net.icxd.dungeons.pet.PetListener;
import net.icxd.dungeons.pet.Pets;
import net.icxd.dungeons.pet.ability.PetAbilities;
import net.icxd.dungeons.profile.SandboxDrops;
import net.icxd.dungeons.profile.SandboxStorage;
import net.icxd.dungeons.rune.RuneRunnable;
import net.icxd.dungeons.scoreboard.ScoreboardRunnable;
import net.icxd.dungeons.skill.SkillGains;
import net.icxd.dungeons.stats.StatsRunnable;
import net.icxd.dungeons.hex.Hex;
import net.icxd.dungeons.hex.HexListener;
import net.icxd.dungeons.fairysouls.SoulStands;
import net.icxd.dungeons.fairysouls.Tia;
import net.icxd.dungeons.hub.DungeonHubMap;
import net.icxd.dungeons.hub.FlowerGarden;
import net.icxd.dungeons.hub.Graveyard;
import net.icxd.dungeons.hub.HubMap;
import net.icxd.dungeons.hub.HubNpcs;
import net.icxd.dungeons.hub.MapStands;
import net.icxd.dungeons.island.IslandArrival;
import net.icxd.dungeons.island.IslandRules;
import net.icxd.dungeons.island.Islands;
import net.icxd.dungeons.island.Jerry;
import net.icxd.dungeons.quest.QuestListener;
import net.icxd.dungeons.storage.AccessoryBag;
import net.icxd.dungeons.storage.Bag;
import net.icxd.dungeons.storage.Storage;
import net.icxd.dungeons.storage.StorageListener;
import net.icxd.dungeons.tablist.TabList;
import net.icxd.dungeons.user.ItemStash;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.user.UserStore;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;


public class Dungeons extends JavaPlugin {
    @Getter private static Dungeons instance;
    @Getter private static MongoClient mongoClient;
    @Getter private static UserCollection userCollection;
    @Getter private static UserStore userStore;
    @Getter private static ProxyLink proxyLink;
    /** Dungeon runs, on DUNGEONS servers; null elsewhere. */
    @Getter private static RunManager runManager;
    /** Players' islands, on ISLAND (and NONE) servers; null elsewhere. */
    @Getter private static Islands islands;
    /** The Hub's flower beds, whose picked flowers grow back as it stops; null off the Hub. */
    private static FlowerGarden flowerGarden;

    @Getter
    private static SkyBlockServer skyBlockServer;

    @Override
    public void onEnable() {
        instance = this;

        mongoClient = MongoClients.create(Settings.URI);
        userCollection = new UserCollection();

        getConfig().options().copyDefaults(true);
        saveConfig();

        // Hypixel's items, as data kept out of this repository (servermgr links it in); without it, there are no items.
        ItemRegistry.loadData(getDataFolder().toPath().resolve("items/items.json"));
        // Storage, bags and Loadouts; their tables are data kept out of it too (storage/), read off the main thread.
        Storage.start(this);
        // Collections and recipes, the same way (collections/), read off the main thread.
        CollectionFiles.load(this);
        // The Hex: its item is saved with the inventory, its tables (hex/) are read off the main thread.
        Hex.start(this);
        // What the Museum takes (museum/), read off the main thread.
        Museum.start(this);
        // The pets (pets/), read off the main thread; summoned ones follow their owners.
        Pets.start(this);
        // What counts collections: the bags' sizes and unlocks, and the set bonuses that count collected items.
        Bag.setCollections(Collections::tier);
        // Aurora Armor's Arcane Energy stacks for Homing Missiles, and Hollow Armor's Spirit stacks for the Hollow Wand.
        WeaponAbilities.arcaneEnergy(SetBonuses::arcaneEnergy, SetBonuses::spendArcaneEnergy);
        UtilityAbilities.hollowSpirit(SetBonuses::spirit, SetBonuses::spendSpirit);
        SetBonuses.setCollections((player, item) -> {
            User user = User.ifLoaded(player.getUniqueId());
            return user == null ? 0 : Collections.count(user.profile(), item);
        });
        // SkyBlock Leveling's Collections task and the guide's collection tasks (whether boss collections' tiers
        // count as collection milestones there is UNKNOWN: they don't), its Boss Collections task (their tiers'
        // own SkyBlock XP), its Accessory Bag task and its Museum Donations task.
        SkyBlockLevels.sources(new LevelingSources() {
            @Override
            public int collectionTiers(Document profile) {
                int tiers = 0;
                for (Map.Entry<String, Integer> e : Collections.unlockedTiers(profile).entrySet()) {
                    if (!Collections.data().collection(e.getKey()).boss()) tiers += e.getValue();
                }
                return tiers;
            }

            @Override
            public int collectionTier(Document profile, String collection) {
                return Collections.tier(profile, collection);
            }

            @Override
            public int bossCollectionXp(Document profile) {
                return Collections.skyBlockXp(profile, true);
            }

            @Override
            public int accessoryPower(Document profile) {
                return AccessoryBag.highestPower(profile);
            }

            @Override
            public int museumXp(Document profile) {
                return Museum.xp(profile);
            }

            @Override
            public int petScore(Document profile) {
                return Pets.bestScore(profile);
            }

            @Override
            public int tamingCapIncreases(Document profile) {
                return George.sacrificed(profile).size();
            }
        });
        // SkyBlock Leveling's tasks and rewards, from the same data (leveling/leveling.json), read off the main thread.
        SkyBlockLevels.start(this);

        skyBlockServer = new SkyBlockServer(getConfig());
        // Nobody drops their items by dying in SkyBlock (a death costs coins: DeathCoins); islands and runs set
        // it on their own worlds too. On the Hub, the Graveyard's zombies can kill. Deaths are called off (Deaths):
        // one that isn't respawns at once, without vanilla's death screen or its line.
        skyBlockServer.getMainWorld().setGameRule(GameRules.KEEP_INVENTORY, true);
        skyBlockServer.getMainWorld().setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        skyBlockServer.getMainWorld().setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
        userStore = new UserStore(this, skyBlockServer.getName(), skyBlockServer.getServerType().name(), userCollection.get(),
                mongoClient.getDatabase(Settings.DATABASE).getCollection("servers"), userCollection::defaultDocument,
                userCollection::profileDefaults);
        userStore.start();
        proxyLink = new ProxyLink(this, userStore);
        if (Islands.hostedOn(skyBlockServer.getServerType())) {
            // The starter island comes from the data checkout's captured worlds (servermgr links them in as worlds/).
            islands = Islands.start(this, skyBlockServer.getName(), mongoClient.getDatabase(Settings.DATABASE),
                    getDataFolder().toPath().resolve("worlds"));
            userStore.beforeRelease(islands::beforeRelease);
            Islands.feature(new Jerry(this));
            Islands.feature(new IslandArrival());
        }
        if (skyBlockServer.getServerType() == ServerType.DUNGEONS) {
            runManager = new RunManager(this, skyBlockServer.getName(),
                    mongoClient.getDatabase(Settings.DATABASE).getCollection(Runs.COLLECTION), proxyLink);
            runManager.start();
        }

        // Each on its own, so one that fails doesn't take the rest with it.
        listen(PlayerListener.class, PlayerListener::new);
        listen(CombatListener.class, CombatListener::new);
        listen(HealthListener.class, HealthListener::new);
        listen(InventorySyncListener.class, InventorySyncListener::new);
        listen(WorldListener.class, WorldListener::new);
        listen(VanillaProgress.class, VanillaProgress::new);
        listen(HubProtection.class, HubProtection::new);
        listen(HubMap.class, () -> new HubMap(this));
        listen(DungeonHubMap.class, DungeonHubMap::new);
        listen(MapStands.class, MapStands::new);
        // The Fairy Souls the Hub's and the Dungeon Hub's maps have.
        listen(SoulStands.class, () -> new SoulStands(this));
        listen(IslandRules.class, IslandRules::new);
        listen(QuestListener.class, QuestListener::new);
        flowerGarden = listen(FlowerGarden.class, () -> new FlowerGarden(this));
        listen(Graveyard.class, () -> new Graveyard(this));
        listen(GUIListener.class, GUIListener::new);
        listen(BlockListener.class, BlockListener::new);
        listen(MovementListener.class, MovementListener::new);
        listen(CheckListener.class, CheckListener::new);
        listen(Mobs.class, Mobs::new);
        listen(SandboxDrops.class, SandboxDrops::new);
        listen(SandboxStorage.class, SandboxStorage::new);
        listen(Shots.class, Shots::new);
        listen(WeaponEvents.class, WeaponEvents::new);
        listen(WeaponPassives.class, WeaponPassives::new);
        listen(SkillGains.class, SkillGains::new);
        listen(KillCoins.class, KillCoins::new);
        listen(DeathCoins.class, DeathCoins::new);
        listen(Deaths.class, () -> new Deaths(this));
        listen(BookOfStats.class, BookOfStats::new);
        listen(Essences.class, Essences::new);
        listen(Npcs.class, Npcs::new);
        listen(SkyBlockMenuListener.class, SkyBlockMenuListener::new);
        listen(PetListener.class, PetListener::new);
        listen(PetItemUse.class, PetItemUse::new);
        listen(AutopetListener.class, AutopetListener::new);
        listen(Callbacks.class, Callbacks::new);
        listen(UtilityListener.class, UtilityListener::new);
        listen(Activations.class, Activations::new);
        // What weapons' enchantments and reforges do in a fight (ENCHANTS_WEAPONS.md).
        listen(WeaponEnchants.class, WeaponEnchants::new);
        listen(CombatReforges.class, CombatReforges::new);
        try {
            SetBonuses.enable(this);
        } catch (RuntimeException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Couldn't enable set bonuses", e);
        }
        // After the set bonuses: armor enchantments' stats (Legion's share more) come after theirs.
        try {
            ArmorEnchants.enable(this);
        } catch (RuntimeException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Couldn't enable armor enchantments", e);
        }
        // After the armor's: a pet ability's share of a stat ("Increases all Combat stats by 10%") counts what the armor gives.
        try {
            PetAbilities.enable(this);
            HeldItemEffects.enable(this);
            PetCopies.enable();
        } catch (RuntimeException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Couldn't enable pet abilities", e);
        }
        listen(StorageListener.class, StorageListener::new);
        listen(CollectionGains.class, CollectionGains::new);
        listen(CraftingTable.Drags.class, CraftingTable.Drags::new);
        listen(VanillaCrafting.class, VanillaCrafting::new);
        listen(HexListener.class, HexListener::new);
        listen(SkyBlockLevels.class, SkyBlockLevels::new);
        // What changes a kill's drops and a grant's experience orbs (STATS_EFFECTS.md): their order doesn't matter.
        listen(DropEnchants.class, DropEnchants::new);
        listen(ExpBonuses.class, ExpBonuses::new);
        if (skyBlockServer.runs(Ophelia.class)) {
            try {
                Ophelia.spawn(this, skyBlockServer.getMainWorld(), getConfig().getConfigurationSection("npcs.ophelia"));
            } catch (RuntimeException e) {
                getLogger().log(java.util.logging.Level.SEVERE, "Couldn't spawn Ophelia", e);
            }
        }
        if (skyBlockServer.runs(HubNpcs.class)) {
            try {
                HubNpcs.spawn(this);
            } catch (RuntimeException e) {
                getLogger().log(java.util.logging.Level.SEVERE, "Couldn't spawn the Hub's NPCs", e);
            }
        }
        if (skyBlockServer.runs(Tia.class) && HubMap.here()) {
            try {
                Tia.spawn(this, skyBlockServer.getMainWorld());
            } catch (RuntimeException e) {
                getLogger().log(java.util.logging.Level.SEVERE, "Couldn't spawn Tia the Fairy", e);
            }
        }

        List<SCommand> commands = List.of(new AddEnchantmentCommand(), new DataCommand(), new DungeonCommand(), new ItemCommand(),
                new NBTCommand(), new PlayerDataCommand(), new RecombobulateCommand(), new SpawnEntityCommand(),
                new SpawnRewardChestCommand(), new UnlockCommand(), new UpgradeCommand(), new HotmCommand(),
                new ShowExtraStatsCommand(), new ToggleReadyUpCommand(), new ProfilesCommand(), new SkillsCommand(),
                new PickupStashCommand(), new ViewStashCommand(), new SbMenuCommand(), new StatsCommand(), new StorageCommand(),
                new EnderChestCommand(), new BackpackCommand(), new BagsCommand(), new SacksCommand(), new LoadoutsCommand(),
                new CollectionCommand(), new ViewCollectionCommand(), new ViewBossCollectionCommand(), new RecipesCommand(),
                new ViewRecipeCommand(), new CraftCommand(), new SetCollectionCommand(), new LevelsCommand(), new SkyBlockXpCommand(),
                new HexCommand(), new IsCommand(), new QuestsCommand(), new ChaptersCommand(), new ClaimItemCommand(),
                new YoinkProfileCommand(), new PetsCommand(), new GivePetCommand(), new CbCommand());
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            for (SCommand command : commands) {
                try {
                    command.register(event.registrar());
                } catch (RuntimeException e) {
                    getLogger().log(java.util.logging.Level.SEVERE, "Couldn't register " + command.getClass().getSimpleName(), e);
                }
            }
        });

        Bukkit.getScheduler().runTaskTimer(this, new StatsRunnable(), 0, 20);
        if (SkyBlockMenuItem.givenHere()) Bukkit.getScheduler().runTaskTimer(this, SkyBlockMenuItem::sweep, 20, 20);
        Bukkit.getScheduler().runTaskTimer(this, new ScoreboardRunnable(), 0, 20);
        Bukkit.getScheduler().runTaskTimer(this, ItemStash::remindAll, ItemStash.REMIND_TICKS, ItemStash.REMIND_TICKS);
        Mobs.start();
        Bukkit.getScheduler().runTaskTimer(this, new RuneRunnable(), 0, 1);
        if (getServer().getPluginManager().isPluginEnabled("packetevents")) {
            TabList.handle();
        } else {
            getLogger().warning("PacketEvents isn't installed, so there's no tab list");
        }

        // Players already online when the plugin (re)loads.
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                userStore.claimOnline(player);
            } catch (UserStore.HeldElsewhereException | InterruptedException | RuntimeException e) {
                getLogger().log(java.util.logging.Level.SEVERE, "Couldn't load " + player.getName() + "'s data", e);
                player.kick(net.kyori.adventure.text.Component.text("Couldn't load your profile, please rejoin."));
            }
        }
    }

    /** Registers a listener, if it runs on this kind of server (see {@link OnlyOn}); it, or null if it doesn't or failed. */
    private <T extends Listener> T listen(Class<T> type, java.util.function.Supplier<T> listener) {
        if (!skyBlockServer.runs(type)) return null;
        try {
            T made = listener.get();
            getServer().getPluginManager().registerEvents(made, this);
            return made;
        } catch (RuntimeException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Couldn't register " + type.getSimpleName(), e);
            return null;
        }
    }

    @Override
    public void onDisable() {
        if (flowerGarden != null) flowerGarden.regrowAll();
        flowerGarden = null;
        Npcs.removeAll();
        Hologram.removeAll();
        PetFollowers.removeAll();
        if (runManager != null) runManager.stop();
        runManager = null;
        // Before the players' data is released: each release waits for its island's save.
        if (islands != null) islands.stop();
        islands = null;
        if (userStore != null) userStore.stop();
        if (mongoClient != null) mongoClient.close();
        instance = null;
    }
}
