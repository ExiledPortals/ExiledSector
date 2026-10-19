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
    CHART_QUANTITY_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addQuantityPercent(value);
        }
    },
    CHART_LOOT_BUDGET_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addLootBudgetPercent(value);
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
    CHART_REWARD_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addRewardPercent(value);
        }
    };

    static final String KEY_PREFIX = "chart.effect.";

    private final boolean counted;
    private final DropType dropType;

    ChartEffect(boolean counted) {
        this.counted = counted;
        this.dropType = null;
    }

    ChartEffect(DropType dropType) {
        this.counted = false;
        this.dropType = dropType;
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
        return message.arg("value", magnitude).styled();
    }

    public void beforeGeneration(ChartGenContext context, float value) {
        context.addDropPercent(dropType, value);
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
