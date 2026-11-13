package exiledsector.charts.loot;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.Seeds;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartIds;
import exiledsector.charts.effect.ChartEnemyShipEffects;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.effects.ItemLootListener;
import exiledsector.effects.TechMiningItemListener;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDrops;
import exiledsector.items.MaterialItems;
import exiledsector.loot.BattleEngagement;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.FleetLevelRange;
import exiledsector.loot.FleetLootRules;
import exiledsector.loot.LootMultipliers;
import exiledsector.loot.LootSource;
import exiledsector.loot.LootSources;
import exiledsector.ui.cargo.MaterialDropAnnouncer;
import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class StarChartLootSource implements LootSource, FleetDifficulty {

    public static final StarChartLootSource INSTANCE = new StarChartLootSource();
    static final long SALVAGE_SEED_SALT = 0x63686172746470L;
    static final String TECH_MINING_SEED_SUFFIX = "|starChart";
    private static final Logger LOG = Logger.getLogger(StarChartLootSource.class);

    private static final class Losses {
        private final Map<CampaignFleetAPI, Float> chartFleetDeploymentPoints = new LinkedHashMap<>();
        private final Map<CampaignFleetAPI, Float> chartFleetDifficultyWeighted = new LinkedHashMap<>();
        private final ChartShipSalvage shipSalvage = new ChartShipSalvage();
        private float deploymentPointsDefeated;
        private float chartDpDefeated;
        private int highestDefeatedLevel;
        private List<LootShaper> battleAnomalies;
    }

    private StarChartLootSource() {
    }

    private Losses losses(BattleLoot battle) {
        return battle.state(this, Losses::new);
    }

    @Override
    public void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
        Losses losses = losses(battle);
        if (losses.battleAnomalies == null) {
            losses.battleAnomalies = anomaliesHoldingBattle();
        }
        float spentDeploymentPoints = 0f;
        for (Map.Entry<CampaignFleetAPI, List<FleetMemberAPI>> fleetLosses : engagement.lostEnemiesBySourceFleet().entrySet()) {
            CampaignFleetAPI sourceFleet = fleetLosses.getKey();
            List<FleetMemberAPI> members = fleetLosses.getValue();
            float deploymentPoints = BattleEngagement.deploymentPoints(members);
            boolean spent = ChartFleetLoot.lootSpent(sourceFleet);
            if (ChartFleetLoot.isChartFleet(sourceFleet) || adoptedAsDefender(sourceFleet)) {
                losses.shipSalvage.record(sourceFleet, members);
                if (!spent) {
                    losses.chartFleetDeploymentPoints.merge(sourceFleet, deploymentPoints, Float::sum);
                    losses.chartFleetDifficultyWeighted.merge(sourceFleet,
                            deploymentPoints * BattleLootRates.difficulty(engagement.difficultyMultiplier()), Float::sum);
                }
            }
            if (spent) {
                spentDeploymentPoints += deploymentPoints;
                continue;
            }
            for (FleetMemberAPI member : members) {
                losses.highestDefeatedLevel = Math.max(losses.highestDefeatedLevel, LootSources.shipLevel(member));
            }
        }
        if (engagement.defeatedDeploymentPoints() > 0f) {
            losses.deploymentPointsDefeated += engagement.defeatedDeploymentPoints();
            losses.chartDpDefeated += Math.max(0f, engagement.defeatedDeploymentPoints() - spentDeploymentPoints) * engagement.difficultyMultiplier();
        }
    }

    private static List<LootShaper> anomaliesHoldingBattle() {
        try {
            return ChartAnomalies.holdingPlayerBattle();
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Checking which anomalies hold a battle failed", e);
            return List.of();
        }
    }

    private static boolean adoptedAsDefender(CampaignFleetAPI sourceFleet) {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (sourceFleet == null || playerFleet == null) {
            return false;
        }
        try {
            return ChartFleetLoot.adoptDefender(sourceFleet, playerFleet.getContainingLocation());
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Checking whether a fleet defends a Star Chart site failed", e);
            return false;
        }
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        ItemLootListener.addBattleSiteLoot(loot, battleLoot(battle));
        try {
            losses(battle).shipSalvage.addTo(loot, battle.encounterSalvage(), battle.random());
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Adding Star Chart salvage from destroyed ships failed", e);
        }
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        ItemLootListener.deliverBattleSiteLoot(playerCargo, battleLoot(battle));
    }

    private ItemDrops.SiteLoot battleLoot(BattleLoot battle) {
        Losses losses = losses(battle);
        SectorAPI sector = Global.getSector();
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (sector == null || playerFleet == null || !(losses.deploymentPointsDefeated > 0f)) {
            return ItemDrops.SiteLoot.NONE;
        }
        ItemDrops.SiteLoot chartLoot = ItemDrops.SiteLoot.NONE;
        if (battle.playerWon()) {
            try {
                ChartDrops.Loot battleLoot = ChartDrops.forBattle(playerFleet.getContainingLocation(), losses.chartDpDefeated,
                        losses.highestDefeatedLevel, battle.independentRandom());
                chartLoot = chartLoot.plus(battleLoot.charts(), List.of(), battleLoot.materials());
            } catch (RuntimeException e) {
                LOG.error("[ExiledSector] Rolling Star Chart battle drops failed", e);
            }
        }
        for (Map.Entry<CampaignFleetAPI, Float> fleetLosses : losses.chartFleetDeploymentPoints.entrySet()) {
            try {
                float deploymentPoints = fleetLosses.getValue();
                float difficulty = deploymentPoints > 0f ? losses.chartFleetDifficultyWeighted.getOrDefault(fleetLosses.getKey(), 0f) / deploymentPoints : 1f;
                ChartFleetLoot.Battle fleetBattle = new ChartFleetLoot.Battle(losses.deploymentPointsDefeated, difficulty,
                        losses.battleAnomalies == null ? List.of() : losses.battleAnomalies);
                chartLoot = chartLoot.plus(ChartFleetLoot.roll(sector, fleetLosses.getKey(), deploymentPoints, fleetBattle, battle.independentRandom()));
            } catch (RuntimeException e) {
                LOG.error("[ExiledSector] Rolling Star Chart fleet loot failed for fleet " + fleetLosses.getKey().getId(), e);
            }
        }
        return chartLoot;
    }

    @Override
    public ItemChances itemChancesFor(CampaignFleetAPI fleet) {
        try {
            return LootContexts.forFleet(fleet).itemChances();
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Reading the item chances of a defeated fleet failed", e);
            return LootContexts.global().itemChances();
        }
    }

    @Override
    public FleetLootRules fleetLootRules(CampaignFleetAPI fleet) {
        try {
            ChartRun run = fleet == null ? null : ChartFleetLoot.liveRunOf(fleet);
            if (run == null) {
                return null;
            }
            if (ChartFleetLoot.lootSpent(fleet)) {
                return FleetLootRules.NONE;
            }
            if (fleet.getMemoryWithoutUpdate().getBoolean(ChartIds.FEEDSTOCK_ONLY_MEMORY_KEY)) {
                return FleetLootRules.FEEDSTOCK_ONLY;
            }
            if (!ChartFleetLoot.isChartFleet(fleet)) {
                return null;
            }
            ChartGenContext context = ChartRunEffects.contextOf(run);
            float battleLootMult = ChartFleetLoot.battleLootMult(anomaliesHoldingBattle(), context);
            LootMultipliers multipliers = BlackHoleHullItems.withBonusWherePlayerIs(context.lootMultipliers(battleLootMult));
            FleetLootRules rules = new FleetLootRules(FleetLootRules.Rates.FULL, true, multipliers,
                    ChartFleetLoot.isBoss(fleet, run) ? context.bossSpoilsMult() : ChartFleetLoot.anomalyFleetLoot(fleet, run));
            return ChartFleetLoot.overchargesHullItems(fleet, run) ? rules.overchargingHullItems() : rules;
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Reading the loot rules of a defeated fleet failed", e);
            return null;
        }
    }

    @Override
    public LootMultipliers modifiersAt(LocationAPI location) {
        ChartGenContext context;
        try {
            context = ChartDrops.liveContextAt(location);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Reading the Star Chart loot bonuses failed", e);
            context = ChartGenContext.none();
        }
        return BlackHoleHullItems.withBonus(context.lootMultipliers(1f), location);
    }

    @Override
    public LootMultipliers modifiersFor(SectorEntityToken site) {
        LootMultipliers modifiers = site == null ? null : modifiersAt(site.getContainingLocation());
        MemoryAPI memory = site == null ? null : site.getMemoryWithoutUpdate();
        if (modifiers == null || memory == null) {
            return modifiers;
        }
        ItemChances chances = modifiers.itemChances();
        for (LootShaper anomaly : ChartAnomalies.with(LootShaper.class)) {
            chances = anomaly.siteChances(memory, chances);
        }
        return chances == modifiers.itemChances() ? modifiers : new LootMultipliers(chances, modifiers.feedstockMult(), modifiers.craftingItemMult());
    }

    @Override
    public void addSalvageLoot(SectorAPI sector, SectorEntityToken salvageEntity, CargoAPI loot) {
        ChartDrops.Loot chartLoot = salvageLoot(sector, salvageEntity);
        chartLoot.charts().forEach(chart -> loot.addSpecial(chart.toSpecialItem(), 1f));
        chartLoot.materials().forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
        MaterialDropAnnouncer.announceOnLootScreen(chartLoot.craftingItems());
    }

    public static ChartDrops.Loot salvageLoot(SectorAPI sector, SectorEntityToken salvageEntity) {
        if (sector == null) {
            return ChartDrops.Loot.NONE;
        }
        try {
            Random chartRandom = new Random(Misc.getSalvageSeed(salvageEntity) ^ SALVAGE_SEED_SALT);
            return ChartDrops.forSalvage(sector, salvageEntity.getContainingLocation(), chartRandom);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling Star Chart salvage drops failed", e);
            return ChartDrops.Loot.NONE;
        }
    }

    @Override
    public void addTechMiningFinds(TechMiningItemListener.Find find) {
        find.items().addAll(techMiningCharts(find.sector(), find.market().getId(), find.minedMonths(), find.chanceMult()));
    }

    public static List<ItemData> techMiningCharts(SectorAPI sector, String marketId, int minedMonths, float chanceMult) {
        try {
            Random chartRandom = Seeds.random(sector.getSeedString() + "|" + marketId + "|" + minedMonths + TECH_MINING_SEED_SUFFIX);
            return ChartDrops.forTechMining(sector, minedMonths == 0, chanceMult, chartRandom).charts();
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling Star Chart tech mining finds failed", e);
            return List.of();
        }
    }

    @Override
    public ItemData shapeDrop(ItemData item, LocationAPI location) {
        try {
            return BiomeModifiers.onDrop(Global.getSector(), item, location);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Adding a biome modifier to a dropped item failed", e);
            return item;
        }
    }

    @Override
    public ItemData shapeBattleDrop(ItemData item, LocationAPI location) {
        ItemData shaped = shapeDrop(item, location);
        SectorAPI sector = Global.getSector();
        for (LootShaper anomaly : anomaliesHoldingBattle()) {
            try {
                shaped = anomaly.shapeBattleDrop(sector, shaped, location);
            } catch (RuntimeException e) {
                LOG.error("[ExiledSector] Shaping a battle drop failed in anomaly " + anomaly.id(), e);
            }
        }
        return shaped;
    }

    @Override
    public ItemData shapeSiteDrop(ItemData item, SectorEntityToken site) {
        try {
            return ImmutableDrops.onSiteDrop(item, site.getMemoryWithoutUpdate());
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Making a modifier of a salvaged item immutable failed", e);
            return item;
        }
    }

    @Override
    public FleetLevelRange levelRange(MemoryAPI fleetMemory) {
        return ChartTiers.fleetStrengthRangeFor(fleetMemory);
    }

    @Override
    public String levelSeed(MemoryAPI fleetMemory) {
        return ChartIds.fleetSeed(fleetMemory);
    }

    @Override
    public String memberLevelSeed(MemoryAPI fleetMemory, FleetMemberAPI member) {
        return OfficerFleets.flagshipTreeSeed(fleetMemory, member);
    }

    @Override
    public int memberExtraLevels(MemoryAPI fleetMemory, FleetMemberAPI member) {
        return OfficerFleets.flagshipExtraLevels(fleetMemory, member);
    }

    @Override
    public List<String> grantedNodeIds(MemoryAPI fleetMemory) {
        List<String> nodeIds = ChartEnemyShipEffects.grantedNodeIds(fleetMemory);
        return nodeIds.isEmpty() ? null : nodeIds;
    }
}
