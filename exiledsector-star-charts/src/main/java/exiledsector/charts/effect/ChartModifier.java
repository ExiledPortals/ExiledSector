package exiledsector.charts.effect;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.data.EnumKeys;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenerated;
import exiledsector.charts.content.ChartPoi;
import exiledsector.charts.anomaly.stormfront.StormRules;
import exiledsector.effects.GrantedNodes;
import exiledsector.i18n.Message;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.loot.DropType;
import exiledsector.modifiers.ModifierEffect;

import java.util.List;

public enum ChartModifier implements ModifierEffect {

    CHART_FLEET_SIZE_PERCENT(false, ChartGenContext::addFleetSizePercent),
    CHART_BASE_FLEETS(true, (context, value) -> context.addBaseFleets(Math.round(value))),
    CHART_EXTRA_FLEETS(true, (context, value) -> context.addExtraFleets(Math.round(value))),
    CHART_OFFICER_LEVEL_BONUS(true, (context, value) -> context.addOfficerLevelBonus(Math.round(value))),
    CHART_EXTRA_OFFICERS(true, (context, value) -> context.addExtraOfficers(Math.round(value))),
    CHART_BOSS_SIZE_PERCENT(false, ChartGenContext::addBossSizePercent),
    CHART_FLEET_QUALITY_PERCENT(false, ChartGenContext::addFleetQualityPercent),
    CHART_SENSOR_RANGE_PERCENT(false, ChartGenContext::addSensorRangePercent) {
        @Override
        public StyledText description(String baseKey, float magnitude) {
            return Translation.msg(baseKey).arg("value", Math.abs(magnitude)).styled();
        }
    },
    CHART_FRAGMENTATION_DAMAGE_TAKEN_PERCENT(ChartFleetStat.FRAGMENTATION_DAMAGE_TAKEN),
    CHART_KINETIC_DAMAGE_TAKEN_PERCENT(ChartFleetStat.KINETIC_DAMAGE_TAKEN),
    CHART_HIGH_EXPLOSIVE_DAMAGE_TAKEN_PERCENT(ChartFleetStat.HIGH_EXPLOSIVE_DAMAGE_TAKEN),
    CHART_ENERGY_DAMAGE_TAKEN_PERCENT(ChartFleetStat.ENERGY_DAMAGE_TAKEN),
    CHART_SHIELD_DAMAGE_TAKEN_PERCENT(ChartFleetStat.SHIELD_DAMAGE_TAKEN),
    CHART_ARMOR_DAMAGE_TAKEN_PERCENT(ChartFleetStat.ARMOR_DAMAGE_TAKEN),
    CHART_ARMOR_PERCENT(ChartFleetStat.ARMOR),
    CHART_TOP_SPEED_PERCENT(ChartFleetStat.TOP_SPEED),
    CHART_LARGE_SHIP_TOP_SPEED_PERCENT(ChartFleetStat.LARGE_SHIP_TOP_SPEED),
    CHART_MANEUVERABILITY_PERCENT(ChartFleetStat.MANEUVERABILITY),
    CHART_SHIELD_ARC_PERCENT(ChartFleetStat.SHIELD_ARC),
    CHART_SHIELD_UPKEEP_PERCENT(ChartFleetStat.SHIELD_UPKEEP),
    CHART_SHIELD_RAISE_RATE_PERCENT(ChartFleetStat.SHIELD_RAISE_RATE),
    CHART_PHASE_ACTIVATION_COST_PERCENT(ChartFleetStat.PHASE_ACTIVATION_COST),
    CHART_FLUX_DISSIPATION_PERCENT(ChartFleetStat.FLUX_DISSIPATION),
    CHART_WEAPON_AND_ENGINE_REPAIR_SPEED_PERCENT(ChartFleetStat.WEAPON_AND_ENGINE_REPAIR_SPEED),
    CHART_BALLISTIC_WEAPON_RANGE_PERCENT(ChartFleetStat.BALLISTIC_WEAPON_RANGE),
    CHART_ENERGY_WEAPON_RANGE_PERCENT(ChartFleetStat.ENERGY_WEAPON_RANGE),
    CHART_MISSILE_WEAPON_RANGE_PERCENT(ChartFleetStat.MISSILE_WEAPON_RANGE),
    CHART_MISSILE_AMMO_PERCENT(ChartFleetStat.MISSILE_AMMO),
    CHART_DMOD_EFFECT_PERCENT(ChartFleetStat.DMOD_EFFECT),
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
    CHART_TWINNED_BOSS(ChartGenContext::twinBoss),
    CHART_NO_COMBAT_CAPITAL_SHIPS(HullSize.CAPITAL_SHIP),
    CHART_NO_COMBAT_CRUISERS(HullSize.CRUISER),
    CHART_NO_COMBAT_DESTROYERS(HullSize.DESTROYER),
    CHART_NO_COMBAT_FRIGATES(HullSize.FRIGATE),
    CHART_UNSHIELDED_SHIELD_DAMAGE_TAKEN_PERCENT(ChartFleetStat.SHIELD_DAMAGE_TAKEN),
    CHART_PHASELOCKED_PHASE_ACTIVATION_COST_PERCENT(ChartFleetStat.PHASE_ACTIVATION_COST),
    CHART_NO_SHIP_SYSTEMS(ChartGenContext::disableShipSystems),
    CHART_SEVERED_COMMAND_POINT_RATE_PERCENT(ChartFleetStat.COMMAND_POINT_RATE),
    CHART_FORCED_AUTOPILOT(ChartGenContext::forceAutopilot),
    CHART_ARC_STRIKE_EMP_FLAT(false, ChartGenContext::addArcStrikeEmp) {
        @Override
        public StyledText description(String baseKey, float magnitude) {
            return Translation.msg(baseKey).arg("value", magnitude).arg("seconds", ChartArcStrikePlugin.INTERVAL_SECONDS).styled();
        }
    },
    CHART_WEAPON_MISFIRE_SECONDS(false, ChartGenContext::addWeaponMisfireSeconds) {
        @Override
        public StyledText description(String baseKey, float magnitude) {
            return Translation.msg(baseKey).arg("value", magnitude).arg("delay", ChartMisfirePlugin.GRACE_SECONDS).styled();
        }

        @Override
        public float withModifierEffect(float value, float effectMult) {
            return value / effectMult;
        }
    },
    CHART_DAMAGE_PER_SMOD_PERCENT(false, ChartGenContext::addDamagePercentPerSMod) {
        @Override
        public StyledText description(String baseKey, float magnitude) {
            return Translation.msg(baseKey).arg("value", Math.abs(magnitude)).styled();
        }
    },
    CHART_ENEMY_LIONS_GAZE("lions_gaze_1"),
    CHART_ENEMY_REFRACTING_PROJECTILES("refracting_projectiles_1"),
    CHART_ENEMY_HULLBREACHER_ROUNDS("hullbreacher_rounds_1"),
    CHART_ENEMY_FREEBOOTERS_CHARGE("freebooters_charge_1"),
    CHART_ENEMY_RISE_FROM_THE_ASHES("tania_rise_from_the_ashes_1"),
    CHART_ENEMY_TORPEDO_DOCTRINE("xiv_torpedo_doctrine_1"),
    CHART_ENEMY_SHARED_FATE("shared_fate"),
    CHART_ENEMY_TRIPAD_BACKDOOR("tripad_backdoor_1"),
    CHART_ENEMY_TERRIFYING_PRESENCE("terrifying_presence_1"),
    CHART_ENEMY_DISINTEGRATION("disintegration_1"),
    CHART_ENEMY_KNIGHTS_CHARGE("knights_charge_1"),
    CHART_ENEMY_CRITICAL_POINT_REFIT("nskr_criticalArmor_1"),
    CHART_ENEMY_PHASE_ENTANGLED_WINGS("phase_entangled_wings_1"),
    CHART_ENEMY_CORROSION_BEAM("corrosion_beam_1"),
    CHART_ENEMY_LUDDS_LIGHT("ludds_light_1"),
    CHART_ENEMY_AT_ANY_COST("at_any_cost_1"),
    CHART_QUANTITY_PERCENT(DropType.ITEM),
    CHART_POI_BUDGET_PERCENT(false, ChartGenContext::addAllSiteBudgetsPercent),
    CHART_CACHE_BUDGET_PERCENT(false, (context, value) -> context.addSiteBudgetPercent(ChartPoi.Kind.CACHE, value)),
    CHART_DOMAIN_BUDGET_PERCENT(false, (context, value) -> context.addSiteBudgetPercent(ChartPoi.Kind.DOMAIN, value)),
    CHART_STATION_BUDGET_PERCENT(false, (context, value) -> context.addSiteBudgetPercent(ChartPoi.Kind.STATION, value)),
    CHART_DERELICT_BUDGET_PERCENT(false, (context, value) -> context.addSiteBudgetPercent(ChartPoi.Kind.DERELICT, value)),
    CHART_EXTRA_POIS(true, (context, value) -> context.addExtraPois(Math.round(value))),
    CHART_ITEM_PERCENT(DropType.HULL_ITEM),
    CHART_ITEM_RARITY_PERCENT(false, (context, value) -> context.addRarityPercent(DropType.ITEM, value)),
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
    CHART_SCHEMATIC_PERCENT(DropType.SCHEMATIC),
    CHART_CRAFTING_ITEM_PERCENT(DropType.CRAFTING_ITEM),
    CHART_BASIC_CRAFTING_ITEM_PERCENT(DropType.BASIC_CRAFTING_ITEM),
    CHART_ENGRAM_PERCENT(DropType.ENGRAM),
    CHART_INHIBITOR_PERCENT(DropType.INHIBITOR),
    CHART_FEEDSTOCK_PERCENT(DropType.FEEDSTOCK),
    CHART_CATALYST_PERCENT(DropType.CATALYST),
    CHART_WEAPON_PERCENT(DropType.WEAPON),
    CHART_RESOURCE_PERCENT(DropType.RESOURCE),
    CHART_BLUEPRINT_PERCENT(DropType.BLUEPRINT),
    CHART_AI_CORE_PERCENT(DropType.AI_CORE),
    CHART_COLONY_ITEM_PERCENT(DropType.COLONY_ITEM),
    CHART_BOSS_SPOILS_PERCENT(false, ChartGenContext::addBossSpoilsPercent),
    CHART_BOSS_STAR_CHART_PERCENT(false, ChartGenContext::addBossStarChartPercent),
    CHART_MODIFIER_EFFECT_PERCENT(false, (context, value) -> { }),
    CHART_SPATIAL_BREACH_CHANCE_PERCENT(new OpeningChance("spatial_breach")),
    CHART_RELAY_NETWORK_CHANCE_PERCENT(new OpeningChance("relay_network")),
    CHART_SUPPLY_CONVOY_CHANCE_PERCENT(new OpeningChance("supply_convoy")),
    CHART_ANOMALOUS_DEBRIS_CHANCE_PERCENT(new OpeningChance("anomalous_debris")),
    CHART_EXCAVATION_SITES_CHANCE_PERCENT(new OpeningChance("excavation_sites")),
    CHART_LISTENING_POST_CHANCE_PERCENT(new OpeningChance("listening_post")),
    CHART_STELLAR_FLARES_CHANCE_PERCENT(new OpeningChance("stellar_flares")),
    CHART_BREACH_GROWTH_PERCENT("spatial_breach.growth", AnomalyLine.PERCENT),
    CHART_BREACH_CACHE_PERCENT("spatial_breach.caches", AnomalyLine.PERCENT),
    CHART_BREACH_CACHE_ITEM_PERCENT("spatial_breach.cacheItems", AnomalyLine.PERCENT),
    CHART_BREACH_WARPED_CHANCE_PERCENT("spatial_breach.warped", AnomalyLine.PERCENT),
    CHART_BREACH_HOLD_SECONDS("spatial_breach.hold", AnomalyLine.COUNT),
    CHART_BREACH_HELD_DRAIN_PERCENT("spatial_breach.heldDrain", AnomalyLine.PERCENT),
    CHART_BREACH_DRIFT("spatial_breach.drift", AnomalyLine.FLAG),
    CHART_BREACH_LIGHTNING("spatial_breach.lightning", AnomalyLine.FLAG),
    CHART_BREACH_BATTLE_HULL_ITEM_PERCENT("spatial_breach.battleHullItems", AnomalyLine.PERCENT),
    CHART_BREACH_FLEET_WARPED_CHANCE_PERCENT("spatial_breach.fleetWarped", AnomalyLine.PERCENT),
    CHART_BREACH_INCURSION("spatial_breach.incursion", AnomalyLine.FLAG),
    CHART_BREACH_WAKES_ANOMALOUS_FLEETS("spatial_breach.wakesAnomalous", AnomalyLine.FLAG),
    CHART_BREACH_ANOMALOUS_FLEETS_WARPED("spatial_breach.anomalousWarped", AnomalyLine.FLAG),
    CHART_BREACH_OFFICER_DUPLICATED_REWARDS("spatial_breach.officerDuplicates", AnomalyLine.COUNT),
    CHART_BREACH_OFFICER_EXTRA_OFFERS("spatial_breach.officerOffers", AnomalyLine.COUNT),
    CHART_BLACK_HOLE_EVENT_HORIZON("black_hole.eventHorizon", AnomalyLine.FLAG),
    CHART_BLACK_HOLE_WARPED_DERELICTS("black_hole.warpedDerelicts", AnomalyLine.FLAG),
    CHART_RELAY_COST_PERCENT("relay_network.cost", AnomalyLine.PERCENT_REDUCED_WHEN_NEGATIVE),
    CHART_RELAY_CHAIN_STAGES("relay_network.stages", AnomalyLine.COUNT),
    CHART_RELAY_REWARD_RARITY_PERCENT("relay_network.rewardRarity", AnomalyLine.PERCENT),
    CHART_RELAY_ONWARD_SYSTEMS("relay_network.onwardSystems", AnomalyLine.COUNT),
    CHART_RELAY_EXTRA_CHOICES("relay_network.extraChoices", AnomalyLine.COUNT),
    CHART_RELAY_EXTRA_REWARD_COPIES("relay_network.rewardCopies", AnomalyLine.COUNT),
    CHART_RELAY_EXTRA_SCARS("relay_network.extraScars", AnomalyLine.COUNT),
    CHART_RELAY_EXTRA_RELAYS("relay_network.extraRelays", AnomalyLine.COUNT),
    CHART_RELAY_SCAR_REWARDS("relay_network.scarRewards", AnomalyLine.COUNT),
    CHART_RELAY_STAGE_UNIQUE_CHANCE("relay_network.stageUnique", AnomalyLine.PERCENT),
    CHART_RELAY_SHARED_BROADCAST("relay_network.sharedBroadcast", AnomalyLine.FLAG),
    CHART_RELAY_FULL_ARRAY("relay_network.fullArray", AnomalyLine.FLAG),
    CHART_CONVOY_ESCORT_PERCENT("supply_convoy.escort", AnomalyLine.PERCENT_REDUCED_WHEN_NEGATIVE),
    CHART_EXTRA_CONVOYS("supply_convoy.extra", AnomalyLine.COUNT),
    CHART_EXTRA_CONVOY_CHANCE_PERCENT("supply_convoy.extraChance", AnomalyLine.PERCENT),
    CHART_CONVOY_THREAT_FEEDSTOCK_CHANCE_PERCENT("supply_convoy.threatChance", AnomalyLine.PERCENT_INCREASED_WHEN_NEGATIVE),
    CHART_CONVOY_SHROUD_FEEDSTOCK_CHANCE_PERCENT("supply_convoy.shroudChance", AnomalyLine.PERCENT_INCREASED_WHEN_NEGATIVE),
    CHART_CONVOY_PHASE_FEEDSTOCK_CHANCE_PERCENT("supply_convoy.phaseChance", AnomalyLine.PERCENT_INCREASED_WHEN_NEGATIVE),
    CHART_CONVOY_DOUBLE_FEEDSTOCK_CHANCE_PERCENT("supply_convoy.doubleFeedstock", AnomalyLine.PERCENT),
    CHART_CONVOY_BREACH_FEEDSTOCK_PERCENT("supply_convoy.breachFeedstock", AnomalyLine.PERCENT),
    CHART_CONVOY_OFFICER_FEEDSTOCK("supply_convoy.officerFeedstock", AnomalyLine.FLAG),
    CHART_CONVOY_EXCAVATION_DELIVERY("supply_convoy.excavationDelivery", AnomalyLine.FLAG),
    CHART_CONVOY_EXCAVATION_RAID_DIFFICULTY_PERCENT("supply_convoy.excavationRaidDifficulty", AnomalyLine.PERCENT),
    CHART_CONVOY_EXCAVATION_LOOT_PERCENT("supply_convoy.excavationLoot", AnomalyLine.PERCENT),
    CHART_HOSTILE_CONVOYS("supply_convoy.hostile", AnomalyLine.FLAG),
    CHART_CONVOY_FEEDSTOCK_PERCENT("supply_convoy.feedstock", AnomalyLine.PERCENT),
    CHART_CONVOY_MIXED_MANIFEST("supply_convoy.mixedManifest", AnomalyLine.FLAG),
    CHART_CONVOY_FLEET_SIZE_PERCENT("supply_convoy.fleetSize", AnomalyLine.PERCENT),
    CHART_EXTRA_ANOMALOUS_DEBRIS("anomalous_debris.extra", AnomalyLine.COUNT),
    CHART_ANOMALOUS_DEBRIS_CRAFTING_ITEM_QUANTITY_PERCENT("anomalous_debris.craftingItem", AnomalyLine.PERCENT),
    CHART_ANOMALOUS_DEBRIS_FLEET_SIZE_PERCENT("anomalous_debris.fleetSize", AnomalyLine.PERCENT),
    CHART_ANOMALOUS_DEBRIS_CORRUPTED_FLEET_SIZE_PERCENT("anomalous_debris.corruptedFleetSize", AnomalyLine.PERCENT),
    CHART_ANOMALOUS_DEBRIS_CORRUPTED_ENGRAMS("anomalous_debris.corruptedEngrams", AnomalyLine.COUNT),
    CHART_ANOMALOUS_DEBRIS_TWICE_CORRUPTED_ENGRAMS("anomalous_debris.twiceCorruptedEngrams", AnomalyLine.COUNT),
    CHART_ANOMALOUS_DEBRIS_SECOND_CORRUPTION("anomalous_debris.secondCorruption", AnomalyLine.FLAG),
    CHART_ANOMALOUS_DEBRIS_ALWAYS_TWICE_CORRUPTED("anomalous_debris.alwaysTwiceCorrupted", AnomalyLine.FLAG),
    CHART_ANOMALOUS_DEBRIS_SINGLE_FIELD("anomalous_debris.singleField", AnomalyLine.FLAG),
    CHART_ANOMALOUS_DEBRIS_BREACH_FIELD("anomalous_debris.breachField", AnomalyLine.FLAG),
    CHART_ANOMALOUS_DEBRIS_AWAKE("anomalous_debris.awake", AnomalyLine.FLAG),
    CHART_ANOMALOUS_DEBRIS_ROAMING("anomalous_debris.roaming", AnomalyLine.FLAG),
    CHART_ANOMALOUS_DEBRIS_REMNANT_SHIP_SHARE("anomalous_debris.remnantShips", AnomalyLine.PERCENT),
    CHART_ANOMALOUS_DEBRIS_THREAT_SHIP_SHARE("anomalous_debris.threatShips", AnomalyLine.PERCENT),
    CHART_ANOMALOUS_DEBRIS_DWELLER_SHIP_SHARE("anomalous_debris.dwellerShips", AnomalyLine.PERCENT),
    CHART_EXTRA_EXCAVATION_SITES("excavation_sites.extra", AnomalyLine.COUNT),
    CHART_EXCAVATION_LOOT_QUANTITY_PERCENT("excavation_sites.loot", AnomalyLine.PERCENT),
    CHART_EXCAVATION_OPEN_SHAFTS("excavation_sites.openShafts", AnomalyLine.FLAG),
    CHART_EXCAVATION_COLLAPSED_SHAFTS("excavation_sites.collapsedShafts", AnomalyLine.FLAG),
    CHART_EXCAVATION_TWIN_SHAFTS("excavation_sites.twinShafts", AnomalyLine.FLAG),
    CHART_EXCAVATION_GUARDIAN_INHIBITORS("excavation_sites.guardianInhibitors", AnomalyLine.COUNT),
    CHART_EXCAVATION_RAID_INHIBITORS("excavation_sites.raidInhibitors", AnomalyLine.COUNT),
    CHART_EXCAVATION_SECOND_CAVERN("excavation_sites.secondCavern", AnomalyLine.FLAG),
    CHART_EXCAVATION_SECOND_CAVERN_FLEET_SIZE_MULT("excavation_sites.secondCavernSize", AnomalyLine.PERCENT),
    CHART_EXCAVATION_SECOND_CAVERN_INHIBITORS("excavation_sites.secondCavernInhibitors", AnomalyLine.COUNT),
    CHART_EXCAVATION_FRIGATES_ONLY("excavation_sites.frigatesOnly", AnomalyLine.FLAG),
    CHART_EXCAVATION_CRUISERS_ALLOWED("excavation_sites.cruisersAllowed", AnomalyLine.FLAG),
    CHART_EXCAVATION_SIGHT_RADIUS_PERCENT("excavation_sites.sightRadius", AnomalyLine.PERCENT_REDUCED_WHEN_NEGATIVE),
    CHART_EXCAVATION_OPEN_SHAFT_INHIBITOR_PERCENT("excavation_sites.openShaftInhibitors", AnomalyLine.PERCENT),
    CHART_EXCAVATION_CORRUPTIBLE_GUARDIANS("excavation_sites.corruptibleGuardians", AnomalyLine.FLAG),
    CHART_EXCAVATION_CORRUPTED_GUARDIAN_INHIBITORS("excavation_sites.corruptedInhibitors", AnomalyLine.COUNT),
    CHART_EXCAVATION_INHIBITOR_PERCENT_PER_DUST("excavation_sites.inhibitorsPerDust", AnomalyLine.PERCENT),
    CHART_SETTLING_DUST_EFFECT_PERCENT("settling_dust.effect", AnomalyLine.PERCENT),
    CHART_SETTLING_DUST_MAX_STACKS("settling_dust.maxStacks", AnomalyLine.COUNT),
    CHART_SETTLING_DUST_QUANTITY_PERCENT("settling_dust.quantity", AnomalyLine.PERCENT),
    CHART_SETTLING_DUST_FLEET_SIZE_MULT("settling_dust.fleetSize", AnomalyLine.PERCENT),
    CHART_SETTLING_DUST_IONIZED("settling_dust.ionized", AnomalyLine.FLAG),
    CHART_MAGNETIC_STORM_CHANCE_PERCENT(new OpeningChance("magnetic_storm")),
    CHART_MAGNETIC_STORM_CRAFTING_ITEM_PERCENT(new AnomalyDrop("magnetic_storm", DropType.CRAFTING_ITEM)),
    CHART_MAGNETIC_STORM_FEEDSTOCK_PERCENT(new AnomalyDrop("magnetic_storm", DropType.FEEDSTOCK)),
    CHART_MAGNETIC_STORM_ION_FRONT("magnetic_storm.ionFront", AnomalyLine.FLAG),
    CHART_MAGNETIC_STORM_ION_FRONT_CRAFTING_ITEM_PERCENT("magnetic_storm.ionFrontCraftingItems", AnomalyLine.PERCENT),
    CHART_MAGNETIC_STORM_FLARE_AURORAS("magnetic_storm.flareAuroras", AnomalyLine.FLAG),
    CHART_MAGNETIC_STORM_LUCKY_MODIFIERS("magnetic_storm.luckyModifiers", AnomalyLine.LUCKY_COUNT),
    CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT("storm_front.loot", AnomalyLine.PERCENT),
    CHART_STORM_STRIKE_FREQUENCY_PERCENT("storm_front.strikes", AnomalyLine.PERCENT),
    CHART_STORM_FRONT_SIZE_PERCENT("storm_front.size", AnomalyLine.PERCENT),
    CHART_STORM_FRONT_CHANCE_PERCENT(new OpeningChance("storm_front")),
    CHART_STORM_FRONT_HELD_SPEED_PERCENT("storm_front.heldSpeed", AnomalyLine.PERCENT_INCREASED_WHEN_NEGATIVE),
    CHART_STORM_FRONT_REDACTED_FRIGATES("storm_front.redactedFrigates", AnomalyLine.COUNT) {
        @Override
        public StyledText description(String baseKey, float magnitude) {
            int most = Math.round(magnitude);
            return Translation.msg(baseKey).count(most).arg("fewest", StormRules.redactedFrigatesFewest(most)).arg("value", most).styled();
        }
    },
    CHART_STORM_FRONT_CAPTURED("storm_front.captured", AnomalyLine.FLAG),
    CHART_STORM_FRONT_CAPTURED_IMMUTABLE_CHANCE_PERCENT("storm_front.capturedImmutableChance", AnomalyLine.PERCENT),
    CHART_STORM_FRONT_LUCKY_BIOME_MODIFIERS("storm_front.luckyBiomeModifiers", AnomalyLine.LUCKY_FLAG),
    CHART_STORM_FRONT_OFFICER_EXTRA_REWARDS("storm_front.officerExtraRewards", AnomalyLine.COUNT),
    CHART_FORGE_RUNS("derelict_forge.extraRuns", AnomalyLine.COUNT),
    CHART_FORGE_COST_PERCENT("derelict_forge.cost", AnomalyLine.PERCENT_REDUCED_WHEN_NEGATIVE),
    CHART_LISTENING_POST_PICKETS("listening_post.pickets", AnomalyLine.COUNT),
    CHART_LISTENING_POST_LOOT_QUANTITY_PERCENT("listening_post.loot", AnomalyLine.PERCENT),
    CHART_LISTENING_POST_PURGE_PERCENT("listening_post.purge", AnomalyLine.PERCENT_INCREASED_WHEN_NEGATIVE),
    CHART_LISTENING_POST_GARRISON_PICKETS("listening_post.garrisonPickets", AnomalyLine.FLAG),
    CHART_LISTENING_POST_INSTANT_ALARM("listening_post.instantAlarm", AnomalyLine.FLAG),
    CHART_LISTENING_POST_UNSEEN_PROTOTYPES("listening_post.unseenPrototypes", AnomalyLine.COUNT),
    CHART_LISTENING_POST_RAID_ONLY("listening_post.raidOnly", AnomalyLine.FLAG),
    CHART_LISTENING_POST_PROTOTYPES("listening_post.prototypes", AnomalyLine.COUNT),
    CHART_LISTENING_POST_HUNTING_PICKETS("listening_post.huntingPickets", AnomalyLine.FLAG),
    CHART_LISTENING_POST_PICKET_PROTOTYPES("listening_post.picketPrototypes", AnomalyLine.FLAG),
    CHART_LISTENING_POST_ARMED_STATION("listening_post.armedStation", AnomalyLine.FLAG),
    CHART_LISTENING_POST_PURGE_HELD("listening_post.purgeHeld", AnomalyLine.FLAG),
    CHART_LISTENING_POST_OFFICER_LEDGER("listening_post.officerLedger", AnomalyLine.COUNT),
    CHART_LISTENING_POST_STOLEN_CHARTS("listening_post.stolenCharts", AnomalyLine.COUNT),
    CHART_STELLAR_FLARES_HULL_ITEM_PERCENT(new AnomalyDrop("stellar_flares", DropType.HULL_ITEM, ".reduced")),
    CHART_STELLAR_FLARES_LUCKY_MODIFIERS("stellar_flares.luckyModifiers", AnomalyLine.LUCKY_COUNT),
    CHART_STELLAR_FLARES_FLARE_FREQUENCY_MULT("stellar_flares.flareFrequency", AnomalyLine.PERCENT),
    CHART_STELLAR_FLARES_LUCKY_IMPLICITS("stellar_flares.luckyImplicits", AnomalyLine.LUCKY_FLAG),
    CHART_STELLAR_FLARES_LUCKY_ROLLS("stellar_flares.luckyRolls", AnomalyLine.COUNT),
    CHART_CONVOY_SUNSTRUCK("supply_convoy.sunstruck", AnomalyLine.FLAG),
    CHART_CONVOY_SUNSTRUCK_COMBAT_READINESS_PERCENT("supply_convoy.sunstruckCombatReadiness", AnomalyLine.PERCENT),
    CHART_STELLAR_FLARES_ALL_LUCKY("stellar_flares.allLucky", AnomalyLine.LUCKY_FLAG);

    static final String KEY_PREFIX = "chart.modifier.";

    private interface Contribution {
        void addTo(ChartGenContext context, float value);
    }

    private interface Flag {
        void raise(ChartGenContext context);
    }

    private record OpeningChance(String anomalyId) {
    }

    private record AnomalyDrop(String anomalyId, DropType dropType, String negativeKeySuffix) {

        AnomalyDrop(String anomalyId, DropType dropType) {
            this(anomalyId, dropType, null);
        }
    }

    private enum AnomalyLine {
        PERCENT(false, null),
        PERCENT_REDUCED_WHEN_NEGATIVE(false, ".reduced"),
        PERCENT_INCREASED_WHEN_NEGATIVE(false, ".increased"),
        COUNT(true, null),
        FLAG(true, null, true, false),
        LUCKY_COUNT(true, null, false, true),
        LUCKY_FLAG(true, null, true, true);

        private final boolean counted;
        private final String negativeKeySuffix;
        private final boolean flag;
        private final boolean lucky;

        AnomalyLine(boolean counted, String negativeKeySuffix) {
            this(counted, negativeKeySuffix, false, false);
        }

        AnomalyLine(boolean counted, String negativeKeySuffix, boolean flag, boolean lucky) {
            this.counted = counted;
            this.negativeKeySuffix = negativeKeySuffix;
            this.flag = flag;
            this.lucky = lucky;
        }
    }

    private static final float WHOLE_NUMBER_TOLERANCE = 1e-4f;

    private final boolean counted;
    private final DropType dropType;
    private final ChartFleetStat fleetStat;
    private final String anomalyTuningKey;
    private final String negativeKeySuffix;
    private final Contribution contribution;
    private final boolean flag;
    private String grantedNodeId;
    private String openingChanceAnomalyId;
    private boolean makesLucky;
    private AnomalyDrop anomalyDrop;

    ChartModifier(boolean counted, Contribution contribution) {
        this(counted, null, null, null, null, contribution, false);
    }

    ChartModifier(Flag flag) {
        this(false, null, null, null, null, (context, value) -> flag.raise(context), true);
    }

    ChartModifier(DropType dropType) {
        this(false, dropType, null, null, null, (context, value) -> context.addDropPercent(dropType, value), false);
    }

    ChartModifier(String grantedNodeId) {
        this(context -> context.grantEnemyNode(grantedNodeId));
        this.grantedNodeId = grantedNodeId;
    }

    ChartModifier(OpeningChance openingChance) {
        this(false, (context, value) -> { });
        this.openingChanceAnomalyId = openingChance.anomalyId();
    }

    ChartModifier(AnomalyDrop anomalyDrop) {
        this(false, null, null, null, anomalyDrop.negativeKeySuffix(), (context, value) -> { }, false);
        this.anomalyDrop = anomalyDrop;
    }

    ChartModifier(HullSize bannedCombatHullSize) {
        this(context -> context.banCombatHullSize(bannedCombatHullSize));
    }

    ChartModifier(ChartFleetStat fleetStat) {
        this(false, null, fleetStat, null, null, (context, value) -> context.addFleetStatPercent(fleetStat, value), false);
    }

    ChartModifier(String anomalyTuningKey, AnomalyLine line) {
        this(line.counted, null, null, anomalyTuningKey, line.negativeKeySuffix,
                (context, value) -> context.addAnomalyPercent(anomalyTuningKey, line.counted ? Math.round(value) : value), line.flag);
        this.makesLucky = line.lucky;
    }

    ChartModifier(boolean counted, DropType dropType, ChartFleetStat fleetStat, String anomalyTuningKey, String negativeKeySuffix,
                Contribution contribution, boolean flag) {
        this.counted = counted;
        this.dropType = dropType;
        this.fleetStat = fleetStat;
        this.anomalyTuningKey = anomalyTuningKey;
        this.negativeKeySuffix = negativeKeySuffix;
        this.contribution = contribution;
        this.flag = flag;
    }

    public boolean isFlag() {
        return flag;
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

    public String openingChanceAnomalyId() {
        return openingChanceAnomalyId;
    }

    public boolean makesLucky() {
        return makesLucky;
    }

    public ChartGenContext.AppliedEffect dropLineWith(List<String> anomalyIds, float value) {
        if (anomalyDrop == null || anomalyIds == null || !anomalyIds.contains(anomalyDrop.anomalyId())) {
            return null;
        }
        return new ChartGenContext.AppliedEffect(forDropType(anomalyDrop.dropType()), value);
    }

    public boolean canRoll(SectorAPI sector) {
        if (grantedNodeId != null && !GrantedNodes.exists(grantedNodeId)) {
            return false;
        }
        return dropType == null || dropType.canDrop(sector);
    }

    public String key() {
        return KEY_PREFIX + name();
    }

    @Override
    public StyledText description(float magnitude) {
        return description(key(), magnitude);
    }

    public StyledText description(String baseKey, float magnitude) {
        if (negativeKeySuffix != null) {
            return Translation.msg(messageKey(baseKey, magnitude)).arg("value", Math.abs(magnitude)).styled();
        }
        Message message = Translation.msg(baseKey);
        if (flag) {
            return message.styled();
        }
        if (counted) {
            int count = Math.round(magnitude);
            return message.count(count).arg("value", count).styled();
        }
        return message.arg("value", fleetStat != null ? Math.abs(magnitude) : magnitude).styled();
    }

    public String messageKey(String baseKey, float magnitude) {
        return negativeKeySuffix != null && magnitude < 0f ? baseKey + negativeKeySuffix : baseKey;
    }

    @Override
    public String detailTitle() {
        return grantedNodeId == null ? null : GrantedNodes.displayName(grantedNodeId);
    }

    @Override
    public List<StyledText> detailLines() {
        return grantedNodeId == null ? List.of() : GrantedNodes.descriptionLines(grantedNodeId).stream().map(StyledText::inverted).toList();
    }

    public float withModifierEffect(float value, float effectMult) {
        if (!counted) {
            return value * effectMult;
        }
        float scaled = Math.round(value) * effectMult;
        return Math.signum(scaled) * (float) Math.floor(Math.abs(scaled) + WHOLE_NUMBER_TOLERANCE);
    }

    public void beforeGeneration(ChartGenContext context, float value) {
        contribution.addTo(context, value);
    }

    public void afterGeneration(ChartGenerated generated, float value) {
    }

    public static ChartModifier byNameOrNull(String effectName) {
        return EnumKeys.parse(ChartModifier.class, effectName);
    }

    public static ChartModifier forDropType(DropType type) {
        for (ChartModifier effect : values()) {
            if (type != null && effect.dropType == type) {
                return effect;
            }
        }
        return null;
    }
}
