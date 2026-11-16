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
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.breach.EventHorizon;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.effects.TechMiningItemListener;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.MaterialItems;
import exiledsector.loot.BattleEngagement;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.DropOrigin;
import exiledsector.loot.FleetDifficulties;
import exiledsector.loot.FleetLootRules;
import exiledsector.loot.Loot;
import exiledsector.loot.LootDelivery;
import exiledsector.loot.LootMultipliers;
import exiledsector.loot.LootSource;
import exiledsector.ui.cargo.MaterialDropAnnouncer;
import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class StarChartLootSource implements LootSource {

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
        for (LootShaper anomaly : losses.battleAnomalies) {
            try {
                anomaly.engagementRecorded(battle, engagement);
            } catch (RuntimeException e) {
                LOG.error("[ExiledSector] Recording an engagement failed in anomaly " + anomaly.id(), e);
            }
        }
        float spentDeploymentPoints = 0f;
        for (Map.Entry<CampaignFleetAPI, List<FleetMemberAPI>> fleetLosses : engagement.lostEnemiesBySourceFleet().entrySet()) {
            CampaignFleetAPI sourceFleet = fleetLosses.getKey();
            List<FleetMemberAPI> members = fleetLosses.getValue();
            float deploymentPoints = BattleEngagement.deploymentPoints(members);
            boolean spent = ChartFleetLedger.lootSpent(sourceFleet);
            if (ChartFleetLedger.hasLootLedger(sourceFleet) || adoptedAsDefender(sourceFleet)) {
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
                losses.highestDefeatedLevel = Math.max(losses.highestDefeatedLevel, FleetDifficulties.shipLevel(member));
            }
        }
        if (engagement.defeatedDeploymentPoints() > 0f) {
            losses.deploymentPointsDefeated += engagement.defeatedDeploymentPoints();
            losses.chartDpDefeated += Math.max(0f, engagement.defeatedDeploymentPoints() - spentDeploymentPoints)
                    * BattleLootRates.difficulty(engagement.difficultyMultiplier());
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
            return ChartFleetLedger.adoptDefender(sourceFleet, playerFleet.getContainingLocation());
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Checking whether a fleet defends a Star Chart site failed", e);
            return false;
        }
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        LootDelivery.toLootScreen(loot, battleLoot(battle), DropOrigin.BATTLE);
        try {
            losses(battle).shipSalvage.addTo(loot, battle.encounterSalvage(), battle.random());
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Adding Star Chart salvage from destroyed ships failed", e);
        }
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        LootDelivery.deliver(playerCargo, battleLoot(battle), DropOrigin.BATTLE);
    }

    private Loot battleLoot(BattleLoot battle) {
        Losses losses = losses(battle);
        SectorAPI sector = Global.getSector();
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (sector == null || playerFleet == null || !(losses.deploymentPointsDefeated > 0f)) {
            return Loot.NONE;
        }
        Loot chartLoot = Loot.NONE;
        if (battle.playerWon()) {
            try {
                ChartDrops.Rolled battleLoot = ChartDrops.forBattle(playerFleet.getContainingLocation(), losses.chartDpDefeated,
                        losses.highestDefeatedLevel, battle.independentRandom());
                chartLoot = chartLoot.plus(battleLoot.asLoot());
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
        recordWin(battle, playerFleet.getContainingLocation());
        return chartLoot;
    }

    static void recordWin(BattleLoot battle, LocationAPI location) {
        if (!battle.playerWon() || battle.unfought()) {
            return;
        }
        try {
            ChartAnomalies.battleWon(location);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Recording a won battle for the run's anomalies failed", e);
        }
    }

    @Override
    public FleetLootRules fleetLootRules(CampaignFleetAPI fleet) {
        try {
            ChartRun run = fleet == null ? null : ChartFleetLedger.liveRunOf(fleet);
            if (run == null) {
                return null;
            }
            if (ChartFleetLedger.lootSpent(fleet)) {
                return FleetLootRules.SPENT;
            }
            if (fleet.getMemoryWithoutUpdate().getBoolean(ChartIds.FEEDSTOCK_ONLY_MEMORY_KEY)) {
                return FleetLootRules.FEEDSTOCK_ONLY;
            }
            if (!ChartFleetLedger.hasLootLedger(fleet)) {
                return null;
            }
            ChartLootContext loot = ChartLootContext.of(run);
            List<LootShaper> battleAnomalies = anomaliesHoldingBattle();
            ChartLootContext fleetLoot = ChartFleetLoot.fleetLootContext(loot, battleAnomalies, ChartFleetLedger.isBoss(fleet, run));
            LootMultipliers multipliers = withBattleHullItems(BlackHoleHullItems.withBonusWherePlayerIs(fleetLoot.multipliers(1f)),
                    battleAnomalies, loot.effects());
            FleetLootRules rules = FleetLootRules.ownLedger(multipliers);
            return ChartFleetLoot.overchargesHullItems(fleet, run) ? rules.overchargingHullItems() : rules;
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Reading the loot rules of a defeated fleet failed", e);
            return null;
        }
    }

    static LootMultipliers withBattleHullItems(LootMultipliers multipliers, List<LootShaper> battleAnomalies, ChartGenContext context) {
        float percent = 0f;
        for (LootShaper anomaly : battleAnomalies) {
            percent += anomaly.battleHullItemPercent(context);
        }
        ItemChances chances = multipliers.chances();
        ItemChances boosted = BlackHoleHullItems.withExtraHullItemShare(chances, percent / ChartGenContext.PERCENT);
        return boosted == chances ? multipliers : multipliers.withChances(boosted);
    }

    @Override
    public LootMultipliers lootMultipliersAt(LocationAPI location) {
        LootMultipliers multipliers;
        try {
            multipliers = ChartLootContext.at(location).multipliers(1f);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Reading the Star Chart loot bonuses failed", e);
            multipliers = LootMultipliers.NONE;
        }
        return BlackHoleHullItems.withBonus(multipliers, location);
    }

    @Override
    public LootMultipliers lootMultipliersFor(SectorEntityToken site) {
        LootMultipliers multipliers = site == null ? null : lootMultipliersAt(site.getContainingLocation());
        MemoryAPI memory = site == null ? null : site.getMemoryWithoutUpdate();
        if (multipliers == null || memory == null) {
            return multipliers;
        }
        ItemChances chances = multipliers.chances();
        for (LootShaper anomaly : ChartAnomalies.with(LootShaper.class)) {
            chances = anomaly.siteChances(memory, chances);
        }
        return chances == multipliers.chances() ? multipliers : multipliers.withChances(chances);
    }

    @Override
    public void addSalvageLoot(SectorAPI sector, SectorEntityToken salvageEntity, CargoAPI loot) {
        ChartDrops.Rolled chartLoot = salvageLoot(sector, salvageEntity);
        chartLoot.charts().forEach(chart -> loot.addSpecial(chart.toSpecialItem(), 1f));
        chartLoot.materials().forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
        MaterialDropAnnouncer.announceOnLootScreen(chartLoot.ciphers());
    }

    public static ChartDrops.Rolled salvageLoot(SectorAPI sector, SectorEntityToken salvageEntity) {
        if (sector == null) {
            return ChartDrops.Rolled.NONE;
        }
        try {
            Random chartRandom = new Random(Misc.getSalvageSeed(salvageEntity) ^ SALVAGE_SEED_SALT);
            return ChartDrops.forSalvage(sector, salvageEntity.getContainingLocation(), chartRandom);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling Star Chart salvage drops failed", e);
            return ChartDrops.Rolled.NONE;
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
    public ItemData shape(ItemData item, DropOrigin origin, LocationAPI location) {
        return switch (origin.kind()) {
            case ANYWHERE -> shapedWhereFound(item, location);
            case BATTLE -> shapedAfterBattle(item, location);
            case SITE -> immutableAtSite(item, origin.site());
        };
    }

    private static ItemData shapedWhereFound(ItemData item, LocationAPI location) {
        SectorAPI sector = Global.getSector();
        return shapedByRunAnomalies(sector, withBiomeModifier(sector, item, location), location);
    }

    private static ItemData shapedAfterBattle(ItemData item, LocationAPI location) {
        SectorAPI sector = Global.getSector();
        ItemData shaped = withBiomeModifier(sector, item, location);
        for (LootShaper anomaly : anomaliesHoldingBattle()) {
            try {
                shaped = anomaly.shapeBattleDrop(sector, shaped, location);
            } catch (RuntimeException e) {
                LOG.error("[ExiledSector] Shaping a battle drop failed in anomaly " + anomaly.id(), e);
            }
        }
        return shapedByRunAnomalies(sector, shaped, location);
    }

    private static ItemData withBiomeModifier(SectorAPI sector, ItemData item, LocationAPI location) {
        try {
            return BiomeModifiers.onDrop(sector, item, location);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Adding a biome modifier to a dropped item failed", e);
            return item;
        }
    }

    private static ItemData shapedByRunAnomalies(SectorAPI sector, ItemData item, LocationAPI location) {
        List<LootShaper> anomalies;
        try {
            anomalies = ChartAnomalies.inRun(ChartRuns.liveIn(location), LootShaper.class);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Checking which anomalies shape a drop failed", e);
            return item;
        }
        ItemData shaped = item;
        for (LootShaper anomaly : anomalies) {
            try {
                shaped = anomaly.shapeDrop(sector, shaped, location);
            } catch (RuntimeException e) {
                LOG.error("[ExiledSector] Shaping a drop failed in anomaly " + anomaly.id(), e);
            }
        }
        return shaped;
    }

    private static ItemData immutableAtSite(ItemData item, SectorEntityToken site) {
        try {
            MemoryAPI memory = site.getMemoryWithoutUpdate();
            return ImmutableDrops.onSiteDrop(EventHorizon.onSiteDrop(item, memory), memory);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Making a modifier of a salvaged item immutable failed", e);
            return item;
        }
    }
}
