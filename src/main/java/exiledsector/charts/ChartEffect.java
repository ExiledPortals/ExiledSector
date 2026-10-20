package exiledsector.charts;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.i18n.Message;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.modifiers.ModifierEffect;

public enum ChartEffect implements ModifierEffect {

    CHART_FLEET_SIZE_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addFleetSizePercent(value);
        }
    },
    CHART_BASE_FLEETS(true) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addBaseFleets(Math.round(value));
        }
    },
    CHART_EXTRA_FLEETS(true) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addExtraFleets(Math.round(value));
        }
    },
    CHART_OFFICER_LEVEL_BONUS(true) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addOfficerLevelBonus(Math.round(value));
        }
    },
    CHART_EXTRA_OFFICERS(true) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addExtraOfficers(Math.round(value));
        }
    },
    CHART_WARDEN_SIZE_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addWardenSizePercent(value);
        }
    },
    CHART_FLEET_QUALITY_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addFleetQualityPercent(value);
        }
    },
    CHART_SENSOR_RANGE_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addSensorRangePercent(value);
        }

        @Override
        public StyledText description(float magnitude) {
            return Translation.msg(key()).arg("value", Math.abs(magnitude)).styled();
        }
    },
    CHART_FRAGMENTATION_DAMAGE_TAKEN_PERCENT(ChartFleetStat.FRAGMENTATION_DAMAGE_TAKEN),
    CHART_KINETIC_DAMAGE_TAKEN_PERCENT(ChartFleetStat.KINETIC_DAMAGE_TAKEN),
    CHART_HIGH_EXPLOSIVE_DAMAGE_TAKEN_PERCENT(ChartFleetStat.HIGH_EXPLOSIVE_DAMAGE_TAKEN),
    CHART_ENERGY_DAMAGE_TAKEN_PERCENT(ChartFleetStat.ENERGY_DAMAGE_TAKEN),
    CHART_TOP_SPEED_PERCENT(ChartFleetStat.TOP_SPEED),
    CHART_MANEUVERABILITY_PERCENT(ChartFleetStat.MANEUVERABILITY),
    CHART_SHIELD_ARC_PERCENT(ChartFleetStat.SHIELD_ARC),
    CHART_PHASE_ACTIVATION_COST_PERCENT(ChartFleetStat.PHASE_ACTIVATION_COST),
    CHART_FLUX_DISSIPATION_PERCENT(ChartFleetStat.FLUX_DISSIPATION),
    CHART_WEAPON_RANGE_PERCENT(ChartFleetStat.WEAPON_RANGE),
    CHART_DEPLOYMENT_COST_PERCENT(ChartFleetStat.DEPLOYMENT_COST),
    CHART_ENEMY_DEPLOYMENT_COST_PERCENT(ChartFleetStat.ENEMY_DEPLOYMENT_COST),
    CHART_COMMAND_POINT_RATE_PERCENT(ChartFleetStat.COMMAND_POINT_RATE),
    CHART_ENEMY_OBJECTIVE_CAPTURE_RATE_PERCENT(ChartFleetStat.ENEMY_OBJECTIVE_CAPTURE_RATE),
    CHART_SMALLER_BATTLES_FLAT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addBattleSizeBonus(-value);
        }
    },
    CHART_LARGER_BATTLES_FLAT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addBattleSizeBonus(value);
        }
    },
    CHART_ENEMY_SENSOR_RANGE_FLAT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addEnemySensorRangeBonus(value);
        }
    },
    CHART_QUANTITY_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addQuantityPercent(value);
        }
    },
    CHART_POI_BUDGET_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addAllSiteBudgetsPercent(value);
        }
    },
    CHART_CACHE_BUDGET_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addSiteBudgetPercent(ChartPoi.Kind.CACHE, value);
        }
    },
    CHART_DOMAIN_BUDGET_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addSiteBudgetPercent(ChartPoi.Kind.DOMAIN, value);
        }
    },
    CHART_STATION_BUDGET_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addSiteBudgetPercent(ChartPoi.Kind.STATION, value);
        }
    },
    CHART_DERELICT_BUDGET_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addSiteBudgetPercent(ChartPoi.Kind.DERELICT, value);
        }
    },
    CHART_EXTRA_POIS(true) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addExtraPois(Math.round(value));
        }
    },
    CHART_ITEM_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addItemPercent(value);
        }
    },
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
    CHART_WARDEN_SPOILS_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addWardenSpoilsPercent(value);
        }
    },
    CHART_REWARD_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addRewardPercent(value);
        }
    };

    static final String KEY_PREFIX = "chart.effect.";

    private final boolean counted;
    private final DropType dropType;
    private final ChartFleetStat fleetStat;

    ChartEffect(boolean counted) {
        this.counted = counted;
        this.dropType = null;
        this.fleetStat = null;
    }

    ChartEffect(DropType dropType) {
        this.counted = false;
        this.dropType = dropType;
        this.fleetStat = null;
    }

    ChartEffect(ChartFleetStat fleetStat) {
        this.counted = false;
        this.dropType = null;
        this.fleetStat = fleetStat;
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
        Message message = Translation.msg(key());
        if (counted) {
            int count = Math.round(magnitude);
            return message.count(count).arg("value", count).styled();
        }
        return message.arg("value", fleetStat != null ? Math.abs(magnitude) : magnitude).styled();
    }

    public void beforeGeneration(ChartGenContext context, float value) {
        if (fleetStat != null) {
            context.addFleetStatPercent(fleetStat, value);
        } else {
            context.addDropPercent(dropType, value);
        }
    }

    public void afterGeneration(ChartGenerated generated, float value) {
    }

    public static ChartEffect byNameOrNull(String effectName) {
        if (effectName == null) {
            return null;
        }
        for (ChartEffect effect : values()) {
            if (effect.name().equals(effectName)) {
                return effect;
            }
        }
        return null;
    }
}
