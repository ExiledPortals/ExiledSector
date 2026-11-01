package exiledsector.charts.effect;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.data.EnumKeys;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenerated;
import exiledsector.charts.content.ChartPoi;
import exiledsector.i18n.Message;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.modifiers.ModifierEffect;
import exiledsector.charts.loot.DropType;

public enum ChartEffect implements ModifierEffect {

    CHART_FLEET_SIZE_PERCENT(false, ChartGenContext::addFleetSizePercent),
    CHART_BASE_FLEETS(true, (context, value) -> context.addBaseFleets(Math.round(value))),
    CHART_EXTRA_FLEETS(true, (context, value) -> context.addExtraFleets(Math.round(value))),
    CHART_OFFICER_LEVEL_BONUS(true, (context, value) -> context.addOfficerLevelBonus(Math.round(value))),
    CHART_EXTRA_OFFICERS(true, (context, value) -> context.addExtraOfficers(Math.round(value))),
    CHART_BOSS_SIZE_PERCENT(false, ChartGenContext::addBossSizePercent),
    CHART_FLEET_QUALITY_PERCENT(false, ChartGenContext::addFleetQualityPercent),
    CHART_SENSOR_RANGE_PERCENT(false, ChartGenContext::addSensorRangePercent) {
        @Override
        public StyledText description(float magnitude) {
            return Translation.msg(key()).arg("value", Math.abs(magnitude)).styled();
        }
    },
    CHART_FRAGMENTATION_DAMAGE_TAKEN_PERCENT(ChartFleetStat.FRAGMENTATION_DAMAGE_TAKEN),
    CHART_KINETIC_DAMAGE_TAKEN_PERCENT(ChartFleetStat.KINETIC_DAMAGE_TAKEN),
    CHART_HIGH_EXPLOSIVE_DAMAGE_TAKEN_PERCENT(ChartFleetStat.HIGH_EXPLOSIVE_DAMAGE_TAKEN),
    CHART_ENERGY_DAMAGE_TAKEN_PERCENT(ChartFleetStat.ENERGY_DAMAGE_TAKEN),
    CHART_SHIELD_DAMAGE_TAKEN_PERCENT(ChartFleetStat.SHIELD_DAMAGE_TAKEN),
    CHART_ARMOR_DAMAGE_TAKEN_PERCENT(ChartFleetStat.ARMOR_DAMAGE_TAKEN),
    CHART_TOP_SPEED_PERCENT(ChartFleetStat.TOP_SPEED),
    CHART_MANEUVERABILITY_PERCENT(ChartFleetStat.MANEUVERABILITY),
    CHART_SHIELD_ARC_PERCENT(ChartFleetStat.SHIELD_ARC),
    CHART_SHIELD_UPKEEP_PERCENT(ChartFleetStat.SHIELD_UPKEEP),
    CHART_SHIELD_RAISE_RATE_PERCENT(ChartFleetStat.SHIELD_RAISE_RATE),
    CHART_PHASE_ACTIVATION_COST_PERCENT(ChartFleetStat.PHASE_ACTIVATION_COST),
    CHART_FLUX_DISSIPATION_PERCENT(ChartFleetStat.FLUX_DISSIPATION),
    CHART_BALLISTIC_WEAPON_RANGE_PERCENT(ChartFleetStat.BALLISTIC_WEAPON_RANGE),
    CHART_ENERGY_WEAPON_RANGE_PERCENT(ChartFleetStat.ENERGY_WEAPON_RANGE),
    CHART_MISSILE_WEAPON_RANGE_PERCENT(ChartFleetStat.MISSILE_WEAPON_RANGE),
    CHART_DEPLOYMENT_COST_PERCENT(ChartFleetStat.DEPLOYMENT_COST),
    CHART_ENEMY_DEPLOYMENT_COST_PERCENT(ChartFleetStat.ENEMY_DEPLOYMENT_COST),
    CHART_COMMAND_POINT_RATE_PERCENT(ChartFleetStat.COMMAND_POINT_RATE),
    CHART_ENEMY_OBJECTIVE_CAPTURE_RATE_PERCENT(ChartFleetStat.ENEMY_OBJECTIVE_CAPTURE_RATE),
    CHART_ENEMY_KINETIC_DAMAGE_PERCENT(ChartFleetStat.ENEMY_KINETIC_DAMAGE_DEALT),
    CHART_ENEMY_HIGH_EXPLOSIVE_DAMAGE_PERCENT(ChartFleetStat.ENEMY_HIGH_EXPLOSIVE_DAMAGE_DEALT),
    CHART_ENEMY_ENERGY_DAMAGE_PERCENT(ChartFleetStat.ENEMY_ENERGY_DAMAGE_DEALT),
    CHART_ENEMY_FRAGMENTATION_DAMAGE_PERCENT(ChartFleetStat.ENEMY_FRAGMENTATION_DAMAGE_DEALT),
    CHART_SMALLER_BATTLES_FLAT(false, (context, value) -> context.addBattleSizeBonus(-value)),
    CHART_LARGER_BATTLES_FLAT(false, ChartGenContext::addBattleSizeBonus),
    CHART_ENEMY_SENSOR_RANGE_FLAT(false, ChartGenContext::addEnemySensorRangeBonus),
    CHART_QUANTITY_PERCENT(false, ChartGenContext::addQuantityPercent),
    CHART_POI_BUDGET_PERCENT(false, ChartGenContext::addAllSiteBudgetsPercent),
    CHART_CACHE_BUDGET_PERCENT(false, (context, value) -> context.addSiteBudgetPercent(ChartPoi.Kind.CACHE, value)),
    CHART_DOMAIN_BUDGET_PERCENT(false, (context, value) -> context.addSiteBudgetPercent(ChartPoi.Kind.DOMAIN, value)),
    CHART_STATION_BUDGET_PERCENT(false, (context, value) -> context.addSiteBudgetPercent(ChartPoi.Kind.STATION, value)),
    CHART_DERELICT_BUDGET_PERCENT(false, (context, value) -> context.addSiteBudgetPercent(ChartPoi.Kind.DERELICT, value)),
    CHART_EXTRA_POIS(true, (context, value) -> context.addExtraPois(Math.round(value))),
    CHART_ITEM_PERCENT(false, ChartGenContext::addItemPercent),
    CHART_ITEM_RARITY_PERCENT(false, ChartGenContext::addItemRarityPercent),
    CHART_BRIDGE_ITEM_PERCENT(DropType.BRIDGE),
    CHART_CREW_QUARTERS_ITEM_PERCENT(DropType.CREW_QUARTERS),
    CHART_ENGINE_ROOM_ITEM_PERCENT(DropType.ENGINE_ROOM),
    CHART_REACTOR_ITEM_PERCENT(DropType.REACTOR),
    CHART_ARMOR_PLATING_ITEM_PERCENT(DropType.ARMOR_PLATING),
    CHART_WEAPON_MOUNT_ITEM_PERCENT(DropType.WEAPON_MOUNT),
    CHART_SHIELD_GENERATOR_ITEM_PERCENT(DropType.SHIELD_GENERATOR),
    CHART_PHASE_COIL_ITEM_PERCENT(DropType.PHASE_COIL),
    CHART_FLIGHT_DECK_ITEM_PERCENT(DropType.FLIGHT_DECK),
    CHART_SUBROUTINE_PERCENT(DropType.SUBROUTINE),
    CHART_STAR_CHART_PERCENT(DropType.STAR_CHART),
    CHART_CURRENCY_PERCENT(DropType.CURRENCY),
    CHART_FEEDSTOCK_PERCENT(DropType.FEEDSTOCK),
    CHART_CATALYST_PERCENT(DropType.CATALYST),
    CHART_WEAPON_PERCENT(DropType.WEAPON),
    CHART_RESOURCE_PERCENT(DropType.RESOURCE),
    CHART_BLUEPRINT_PERCENT(DropType.BLUEPRINT),
    CHART_AI_CORE_PERCENT(DropType.AI_CORE),
    CHART_COLONY_ITEM_PERCENT(DropType.COLONY_ITEM),
    CHART_BOSS_SPOILS_PERCENT(false, ChartGenContext::addBossSpoilsPercent),
    CHART_REWARD_PERCENT(false, ChartGenContext::addRewardPercent),
    CHART_BREACH_GROWTH_PERCENT("spatial_breach.growth", AnomalyLine.PERCENT),
    CHART_BREACH_CACHE_PERCENT("spatial_breach.caches", AnomalyLine.PERCENT),
    CHART_BREACH_CACHE_ITEM_PERCENT("spatial_breach.cacheItems", AnomalyLine.PERCENT),
    CHART_RELAY_COST_PERCENT("relay_network.cost", AnomalyLine.PERCENT_REDUCED_WHEN_NEGATIVE),
    CHART_RELAY_CHAIN_STAGES("relay_network.stages", AnomalyLine.COUNT),
    CHART_CONVOY_CARGO_PERCENT("supply_convoy.cargo", AnomalyLine.PERCENT),
    CHART_CONVOY_ESCORT_PERCENT("supply_convoy.escort", AnomalyLine.PERCENT_REDUCED_WHEN_NEGATIVE),
    CHART_EXTRA_CONVOYS("supply_convoy.extra", AnomalyLine.COUNT),
    CHART_EXTRA_ANOMALOUS_DEBRIS("anomalous_debris.extra", AnomalyLine.COUNT),
    CHART_ANOMALOUS_DEBRIS_CURRENCY_QUANTITY_PERCENT("anomalous_debris.currency", AnomalyLine.PERCENT),
    CHART_EXTRA_EXCAVATION_SITES("excavation_sites.extra", AnomalyLine.COUNT),
    CHART_EXCAVATION_LOOT_QUANTITY_PERCENT("excavation_sites.loot", AnomalyLine.PERCENT),
    CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT("storm_front.loot", AnomalyLine.PERCENT),
    CHART_STORM_STRIKE_FREQUENCY_PERCENT("storm_front.strikes", AnomalyLine.PERCENT),
    CHART_STORM_FRONT_SIZE_PERCENT("storm_front.size", AnomalyLine.PERCENT),
    CHART_FORGE_PROGRAMS("derelict_forge.extraPrograms", AnomalyLine.COUNT),
    CHART_FORGE_RUNS("derelict_forge.extraRuns", AnomalyLine.COUNT),
    CHART_FORGE_COST_PERCENT("derelict_forge.cost", AnomalyLine.PERCENT_REDUCED_WHEN_NEGATIVE),
    CHART_LISTENING_POST_PICKETS("listening_post.pickets", AnomalyLine.COUNT),
    CHART_LISTENING_POST_LOOT_QUANTITY_PERCENT("listening_post.loot", AnomalyLine.PERCENT),
    CHART_LISTENING_POST_PURGE_PERCENT("listening_post.purge", AnomalyLine.PERCENT_INCREASED_WHEN_NEGATIVE);

    static final String KEY_PREFIX = "chart.effect.";

    private interface Contribution {
        void addTo(ChartGenContext context, float value);
    }

    private enum AnomalyLine {
        PERCENT(false, null),
        PERCENT_REDUCED_WHEN_NEGATIVE(false, ".reduced"),
        PERCENT_INCREASED_WHEN_NEGATIVE(false, ".increased"),
        COUNT(true, null);

        private final boolean counted;
        private final String negativeKeySuffix;

        AnomalyLine(boolean counted, String negativeKeySuffix) {
            this.counted = counted;
            this.negativeKeySuffix = negativeKeySuffix;
        }
    }

    private final boolean counted;
    private final DropType dropType;
    private final ChartFleetStat fleetStat;
    private final String anomalyTuningKey;
    private final String negativeKeySuffix;
    private final Contribution contribution;

    ChartEffect(boolean counted, Contribution contribution) {
        this(counted, null, null, null, null, contribution);
    }

    ChartEffect(DropType dropType) {
        this(false, dropType, null, null, null, (context, value) -> context.addDropPercent(dropType, value));
    }

    ChartEffect(ChartFleetStat fleetStat) {
        this(false, null, fleetStat, null, null, (context, value) -> context.addFleetStatPercent(fleetStat, value));
    }

    ChartEffect(String anomalyTuningKey, AnomalyLine line) {
        this(line.counted, null, null, anomalyTuningKey, line.negativeKeySuffix,
                (context, value) -> context.addAnomalyPercent(anomalyTuningKey, line.counted ? Math.round(value) : value));
    }

    ChartEffect(boolean counted, DropType dropType, ChartFleetStat fleetStat, String anomalyTuningKey, String negativeKeySuffix,
                Contribution contribution) {
        this.counted = counted;
        this.dropType = dropType;
        this.fleetStat = fleetStat;
        this.anomalyTuningKey = anomalyTuningKey;
        this.negativeKeySuffix = negativeKeySuffix;
        this.contribution = contribution;
    }

    public ChartFleetStat fleetStat() {
        return fleetStat;
    }

    public DropType dropType() {
        return dropType;
    }

    public String anomalyTuningKey() {
        return anomalyTuningKey;
    }

    public boolean canRoll(SectorAPI sector) {
        return dropType == null || dropType.canDrop(sector);
    }

    public String key() {
        return KEY_PREFIX + name();
    }

    @Override
    public StyledText description(float magnitude) {
        if (negativeKeySuffix != null) {
            return Translation.msg(magnitude < 0f ? key() + negativeKeySuffix : key()).arg("value", Math.abs(magnitude)).styled();
        }
        Message message = Translation.msg(key());
        if (counted) {
            int count = Math.round(magnitude);
            return message.count(count).arg("value", count).styled();
        }
        return message.arg("value", fleetStat != null ? Math.abs(magnitude) : magnitude).styled();
    }

    public void beforeGeneration(ChartGenContext context, float value) {
        contribution.addTo(context, value);
    }

    public void afterGeneration(ChartGenerated generated, float value) {
    }

    public static ChartEffect byNameOrNull(String effectName) {
        return EnumKeys.parse(ChartEffect.class, effectName);
    }

    public static ChartEffect forDropType(DropType type) {
        for (ChartEffect effect : values()) {
            if (type != null && effect.dropType == type) {
                return effect;
            }
        }
        return null;
    }
}
