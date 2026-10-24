package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.PlayerFleet;
import exiledlib.Seeds;
import exiledsector.charts.expansion.SpatialBreach;
import exiledsector.effects.ItemLootListener;
import exiledsector.effects.TechMiningItemListener;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDrops;
import exiledsector.items.MaterialItems;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.FleetLevelRange;
import exiledsector.loot.LootModifiers;
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
        private float deploymentPointsDefeated;
        private float chartStrengthDefeated;
        private int highestDefeatedLevel;
    }

    private StarChartLootSource() {
    }

    private Losses losses(BattleLoot battle) {
        return battle.state(this, Losses::new);
    }

    @Override
    public void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
        Losses losses = losses(battle);
        for (FleetMemberAPI member : engagement.lostEnemies()) {
            CampaignFleetAPI sourceFleet = member == null ? null : engagement.battle().getSourceFleet(member);
            if (ChartFleetLoot.isChartFleet(sourceFleet) || adoptedAsDefender(sourceFleet)) {
                losses.chartFleetDeploymentPoints.merge(sourceFleet, member.getDeploymentPointsCost(), Float::sum);
            }
            if (member != null) {
                losses.highestDefeatedLevel = Math.max(losses.highestDefeatedLevel, LootSources.shipLevel(member));
            }
        }
        if (engagement.defeatedDeploymentPoints() > 0f) {
            losses.deploymentPointsDefeated += engagement.defeatedDeploymentPoints();
            losses.chartStrengthDefeated += engagement.defeatedDeploymentPoints() * engagement.difficultyMultiplier();
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
        ItemLootListener.addSiteLoot(loot, battleLoot(battle));
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        ItemLootListener.deliverSiteLoot(playerCargo, battleLoot(battle));
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
                ChartDrops.Loot battleLoot = ChartDrops.forBattle(sector, playerFleet.getContainingLocation(), losses.chartStrengthDefeated,
                        losses.highestDefeatedLevel, battle.independentRandom());
                chartLoot = chartLoot.plus(battleLoot.charts(), List.of(), battleLoot.currencies());
            } catch (RuntimeException e) {
                LOG.error("[ExiledSector] Rolling Star Chart battle drops failed", e);
            }
        }
        for (Map.Entry<CampaignFleetAPI, Float> fleetLosses : losses.chartFleetDeploymentPoints.entrySet()) {
            try {
                chartLoot = chartLoot.plus(ChartFleetLoot.roll(sector, fleetLosses.getKey(), fleetLosses.getValue(), battle.independentRandom()));
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
    public LootModifiers modifiersAt(LocationAPI location) {
        ChartGenContext context;
        try {
            context = ChartDrops.liveContextAt(location);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Reading the Star Chart loot bonuses failed", e);
            context = ChartGenContext.none();
        }
        return new LootModifiers(context.itemChances(), context.dropMult(DropType.FEEDSTOCK), context.dropMult(DropType.CURRENCY));
    }

    @Override
    public LootModifiers modifiersFor(SectorEntityToken site) {
        LootModifiers modifiers = site == null ? null : modifiersAt(site.getContainingLocation());
        MemoryAPI memory = site == null ? null : site.getMemoryWithoutUpdate();
        if (modifiers == null || memory == null) {
            return modifiers;
        }
        ItemChances chances = modifiers.itemChances();
        if (memory.contains(SpatialBreach.ITEM_MULT_KEY)) {
            chances = chances.scaled(memory.getFloat(SpatialBreach.ITEM_MULT_KEY));
        }
        if (memory.contains(SpatialBreach.RARITY_KEY)) {
            chances = chances.withRarity(chances.rarity() * (1f + memory.getFloat(SpatialBreach.RARITY_KEY) / ChartGenContext.PERCENT));
        }
        return chances == modifiers.itemChances() ? modifiers : new LootModifiers(chances, modifiers.feedstockMult(), modifiers.currencyMult());
    }

    @Override
    public void addSalvageLoot(SectorAPI sector, SectorEntityToken salvageEntity, CargoAPI loot) {
        ChartDrops.Loot chartLoot = salvageLoot(sector, salvageEntity);
        chartLoot.charts().forEach(chart -> loot.addSpecial(chart.toSpecialItem(), 1f));
        chartLoot.currencies().forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
        MaterialDropAnnouncer.announceOnLootScreen(chartLoot.currencies());
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
    public FleetLevelRange levelRange(MemoryAPI fleetMemory) {
        return ChartTiers.fleetStrengthRangeFor(fleetMemory);
    }

    @Override
    public String levelSeed(MemoryAPI fleetMemory) {
        return ChartIds.fleetSeed(fleetMemory);
    }
}
