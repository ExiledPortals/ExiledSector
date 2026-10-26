package exiledsector.charts;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.expansion.SpatialBreach;
import exiledsector.charts.phenomenon.AsteroidStorm;
import exiledsector.charts.phenomenon.MagneticStorm;
import exiledsector.charts.phenomenon.StellarFlares;
import exiledlib.i18n.Message;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.modifiers.ModifierEffect;

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
    CHART_WEAPON_PERCENT(DropType.WEAPON),
    CHART_RESOURCE_PERCENT(DropType.RESOURCE),
    CHART_BLUEPRINT_PERCENT(DropType.BLUEPRINT),
    CHART_AI_CORE_PERCENT(DropType.AI_CORE),
    CHART_COLONY_ITEM_PERCENT(DropType.COLONY_ITEM),
    CHART_BOSS_SPOILS_PERCENT(false, ChartGenContext::addBossSpoilsPercent),
    CHART_REWARD_PERCENT(false, ChartGenContext::addRewardPercent),
    CHART_BREACH_CHANCE(new Chance((context, value) -> context.addExpansionChance(SpatialBreach.EXPANSION_ID, value))),
    CHART_BREACH_GROWTH_PERCENT(false, (context, value) -> context.addExpansionPercent(SpatialBreach.GROWTH_KEY, value)),
    CHART_BREACH_CACHE_PERCENT(false, (context, value) -> context.addExpansionPercent(SpatialBreach.CACHE_KEY, value)),
    CHART_BREACH_CORE_ITEM_PERCENT(false, (context, value) -> context.addExpansionPercent(SpatialBreach.CORE_ITEM_KEY, value)),
    CHART_ASTEROID_STORM_CHANCE(new Chance((context, value) -> context.addPhenomenonChance(AsteroidStorm.PHENOMENON_ID, value))),
    CHART_MAGNETIC_STORM_CHANCE(new Chance((context, value) -> context.addPhenomenonChance(MagneticStorm.PHENOMENON_ID, value))),
    CHART_STELLAR_FLARES_CHANCE(new Chance((context, value) -> context.addPhenomenonChance(StellarFlares.PHENOMENON_ID, value)));

    static final String KEY_PREFIX = "chart.effect.";
    static final String CHANCE_SUFFIX = ".chance";

    private interface Contribution {
        void addTo(ChartGenContext context, float value);
    }

    private record Chance(Contribution contribution) {
    }

    private final boolean counted;
    private final DropType dropType;
    private final ChartFleetStat fleetStat;
    private final Contribution contribution;
    private final boolean chance;

    ChartEffect(boolean counted, Contribution contribution) {
        this(counted, null, null, contribution, false);
    }

    ChartEffect(Chance chance) {
        this(false, null, null, chance.contribution(), true);
    }

    ChartEffect(DropType dropType) {
        this(false, dropType, null, (context, value) -> context.addDropPercent(dropType, value), false);
    }

    ChartEffect(ChartFleetStat fleetStat) {
        this(false, null, fleetStat, (context, value) -> context.addFleetStatPercent(fleetStat, value), false);
    }

    ChartEffect(boolean counted, DropType dropType, ChartFleetStat fleetStat, Contribution contribution, boolean chance) {
        this.counted = counted;
        this.dropType = dropType;
        this.fleetStat = fleetStat;
        this.contribution = contribution;
        this.chance = chance;
    }

    public ChartFleetStat fleetStat() {
        return fleetStat;
    }

    public DropType dropType() {
        return dropType;
    }

    public boolean canRoll(SectorAPI sector) {
        return dropType == null || dropType.canDrop(sector);
    }

    String key() {
        return KEY_PREFIX + name();
    }

    @Override
    public StyledText description(float magnitude) {
        if (chance && magnitude < ChartGenContext.PERCENT) {
            return Translation.msg(key() + CHANCE_SUFFIX).arg("value", magnitude).styled();
        }
        Message message = Translation.msg(key());
        if (chance) {
            return message.styled();
        }
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
        if (effectName == null) {
            return null;
        }
        for (ChartEffect effect : values()) {
            if (effect.name().equalsIgnoreCase(effectName.trim())) {
                return effect;
            }
        }
        return null;
    }
}
