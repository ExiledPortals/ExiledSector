package exiledlib.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.ShipFacts;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.ListenerEffect;
import exiledlib.skills.skilleffect.SimpleStatEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.StatMode;
import exiledlib.skills.skilleffect.StatTarget;

import java.util.List;
import java.util.function.Function;

import static exiledlib.skills.skilleffect.StatMode.FLAT;
import static exiledlib.skills.skilleffect.StatMode.MULT;
import static exiledlib.skills.skilleffect.StatMode.PERCENT;
import static exiledlib.skills.skilleffect.StatTarget.bonus;
import static exiledlib.skills.skilleffect.StatTarget.dynamicStat;
import static exiledlib.skills.skilleffect.StatTarget.liveStat;
import static exiledlib.skills.skilleffect.StatTarget.stat;

public enum ShieldSkillEffect implements BackedSkillEffect {

    REMOVE_SHIELD {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            ship.setShield(ShieldAPI.ShieldType.NONE, 0f, 1f, 1f);
        }

        @Override
        public String blockAllocationReason(ShipFacts ship, ShieldAPI.ShieldType currentShieldType) {
            return noShieldsReason(currentShieldType);
        }
    },
    CREATE_FRONT_SHIELD_IF_NONE {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            if (ship.getShield() == null && !isPhaseHull(ship)) {
                ship.setShield(ShieldAPI.ShieldType.FRONT, MAKESHIFT_SHIELD_UPKEEP, MAKESHIFT_SHIELD_EFFICIENCY, MAKESHIFT_SHIELD_ARC);
            }
        }

        @Override
        public String blockAllocationReason(ShipFacts ship, ShieldAPI.ShieldType currentShieldType) {
            return phaseHullReason(currentShieldType);
        }
    },
    CONVERT_SHIELD_TO_FRONT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            convertShield(ship, ShieldAPI.ShieldType.FRONT);
        }

        @Override
        public String blockAllocationReason(ShipFacts ship, ShieldAPI.ShieldType currentShieldType) {
            return shieldConversionBlockReason(currentShieldType, ShieldAPI.ShieldType.FRONT, "node.block.hasFrontShield");
        }
    },
    CONVERT_SHIELD_TO_OMNI {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            convertShield(ship, ShieldAPI.ShieldType.OMNI);
        }

        @Override
        public String blockAllocationReason(ShipFacts ship, ShieldAPI.ShieldType currentShieldType) {
            return shieldConversionBlockReason(currentShieldType, ShieldAPI.ShieldType.OMNI, "node.block.hasOmniShield");
        }
    },
    SHIELD_ARC_PERCENT(PERCENT, bonus(MutableShipStatsAPI::getShieldArcBonus), StatNames.SHIELD_ARC, false),
    SHIELD_ARC_FLAT(FLAT, bonus(MutableShipStatsAPI::getShieldArcBonus), StatNames.SHIELD_ARC, false),
    SHIELD_ARC_MULT(MULT, bonus(MutableShipStatsAPI::getShieldArcBonus), StatNames.SHIELD_ARC, false),
    SHIELD_ABSORPTION_PERCENT(PERCENT, liveStat(MutableShipStatsAPI::getShieldAbsorptionMult), StatNames.DAMAGE_TAKEN_BY_SHIELDS, true),
    SHIELD_DAMAGE_TAKEN_MULT(MULT, liveStat(MutableShipStatsAPI::getShieldDamageTakenMult), StatNames.DAMAGE_TAKEN_BY_SHIELDS, true),
    SHIELD_PIERCE_CHANCE_PERCENT(PERCENT, dynamicStat(Stats.SHIELD_PIERCED_MULT), "stat.chanceForShieldsToBePiercedByEmpArcs", true),
    SHIELD_PIERCE_CHANCE_MULT(MULT, dynamicStat(Stats.SHIELD_PIERCED_MULT), "stat.chanceForShieldsToBePiercedByEmpArcs", true),
    SHIELD_UPKEEP_PERCENT(PERCENT, stat(MutableShipStatsAPI::getShieldUpkeepMult), "stat.shieldFluxUpkeep", true),
    SHIELD_UPKEEP_MULT(MULT, stat(MutableShipStatsAPI::getShieldUpkeepMult), "stat.shieldFluxUpkeep", true),
    SHIELD_TURN_RATE_PERCENT(PERCENT, stat(MutableShipStatsAPI::getShieldTurnRateMult), "stat.shieldTurnRate", false),
    SHIELD_RAISE_RATE_PERCENT(PERCENT, stat(MutableShipStatsAPI::getShieldUnfoldRateMult), "stat.shieldRaiseRate", false),
    SHIELD_SOFT_FLUX_CONVERSION_PERCENT(PERCENT, new StatTarget.PercentagePoints(MutableShipStatsAPI::getShieldSoftFluxConversion),
            "stat.shieldSoftFluxConversion", false) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.templated(this, magnitude);
        }
    },
    SHIELD_DAMAGE_SHARED_PERCENT(SharedShieldDamageListener.SHARED_PERCENT_KEY,
            SharedShieldDamageListener.class, SharedShieldDamageListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", Math.round(SharedShieldDamageListener.RANGE)).styled();
        }
    },
    SHIELD_ABSORB_RATE_OF_FIRE_PERCENT(AbsorbReserveListener.RATE_OF_FIRE_PERCENT_KEY,
            AbsorbReserveListener.class, AbsorbReserveListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("full", AbsorbReserveListener.FULL_RESERVE)
                    .arg("drain", AbsorbReserveListener.DRAIN_PER_SECOND).arg("max", AbsorbReserveListener.MAX_RESERVE).styled();
        }
    },
    DYNAMIC_GRID_ENERGY_WEAPON_BONUS_PERCENT(DynamicGridListener.ENERGY_BONUS_PERCENT_KEY, DynamicGridListener.class,
            DynamicGridListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return DynamicGridListener.SHIELDS_DOWN_SECONDS.withShipSizeArgs(EffectText.msg(this).arg("value", magnitude)).styled();
        }

        @Override
        public String blockAllocationReason(ShipFacts ship, ShieldAPI.ShieldType currentShieldType) {
            return noShieldsReason(currentShieldType);
        }
    };

    public static final float MAKESHIFT_SHIELD_UPKEEP = 0.5f;
    public static final float MAKESHIFT_SHIELD_EFFICIENCY = 1.2f;
    public static final float MAKESHIFT_SHIELD_ARC = 90f;

    private final EffectBacking backing;

    ShieldSkillEffect() {
        this((EffectBacking) null);
    }

    ShieldSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this(new SimpleStatEffect(mode, target, statKey, lowerIsBetter));
    }

    <T> ShieldSkillEffect(String magnitudeKey, Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        this(new ListenerEffect<>(magnitudeKey, listenerType, listenerFactory));
    }

    ShieldSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }

    public static ShieldAPI.ShieldType resolveDisplayShieldType(ShieldAPI.ShieldType baseType, List<SkillEffect> effectsInAllocationOrder) {
        ShieldAPI.ShieldType type = baseType;
        for (SkillEffect effect : effectsInAllocationOrder) {
            if (effect == REMOVE_SHIELD) {
                type = ShieldAPI.ShieldType.NONE;
            } else if (effect == CREATE_FRONT_SHIELD_IF_NONE && type == ShieldAPI.ShieldType.NONE) {
                type = ShieldAPI.ShieldType.FRONT;
            } else if (effect == CONVERT_SHIELD_TO_FRONT && hasShields(type)) {
                type = ShieldAPI.ShieldType.FRONT;
            } else if (effect == CONVERT_SHIELD_TO_OMNI && hasShields(type)) {
                type = ShieldAPI.ShieldType.OMNI;
            }
        }
        return type;
    }

    private static boolean hasShields(ShieldAPI.ShieldType type) {
        return type == ShieldAPI.ShieldType.FRONT || type == ShieldAPI.ShieldType.OMNI;
    }

    private static String noShieldsReason(ShieldAPI.ShieldType currentShieldType) {
        return hasShields(currentShieldType) ? null : Translation.text("node.block.noShields");
    }

    private static boolean isPhaseHull(ShipAPI ship) {
        return ship.getHullSpec().getShieldType() == ShieldAPI.ShieldType.PHASE;
    }

    private static String phaseHullReason(ShieldAPI.ShieldType currentShieldType) {
        return currentShieldType == ShieldAPI.ShieldType.PHASE ? Translation.text("node.requires.req_non_phase_hull") : null;
    }

    private static void convertShield(ShipAPI ship, ShieldAPI.ShieldType convertedType) {
        ShieldAPI shield = ship.getShield();
        if (shield != null) {
            shield.setType(convertedType);
        }
    }

    private static String shieldConversionBlockReason(ShieldAPI.ShieldType currentShieldType, ShieldAPI.ShieldType convertedType,
                                                      String alreadyConvertedReasonKey) {
        if (currentShieldType == convertedType) {
            return Translation.text(alreadyConvertedReasonKey);
        }
        return phaseHullReason(currentShieldType);
    }

    private static final class StatNames {
        static final String SHIELD_ARC = "stat.shieldArc";
        static final String DAMAGE_TAKEN_BY_SHIELDS = "stat.damageTakenByShields";

        private StatNames() {
        }
    }
}
