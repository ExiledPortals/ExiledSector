package exiledsector.charts;

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
    CHART_ITEM_DROP_CHANCE_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addItemChancePercent(value);
        }

        @Override
        public void afterGeneration(ChartGenerated generated, float value) {
            ChartGenContext.markSalvageChances(generated);
        }
    },
    CHART_VAULT_RICHNESS_PERCENT(false) {
        @Override
        public void beforeGeneration(ChartGenContext context, float value) {
            context.addVaultRichnessPercent(value);
        }

        @Override
        public void afterGeneration(ChartGenerated generated, float value) {
            ChartGenContext.enrichVault(generated, value);
            ChartGenContext.markSalvageChances(generated);
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

    ChartEffect(boolean counted) {
        this.counted = counted;
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

    public abstract void beforeGeneration(ChartGenContext context, float value);

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
