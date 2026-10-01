package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.i18n.StyledText;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.List;
import java.util.function.Function;

import static exiledsector.skills.skilleffect.StatMode.FLAT;
import static exiledsector.skills.skilleffect.StatMode.MULT;
import static exiledsector.skills.skilleffect.StatMode.PERCENT;
import static exiledsector.skills.skilleffect.StatTarget.bonus;
import static exiledsector.skills.skilleffect.StatTarget.dynamicStat;
import static exiledsector.skills.skilleffect.StatTarget.stat;

public enum ShieldSkillEffect implements BackedSkillEffect {

    BEAM_WEAPON_HARD_FLUX_PERCENT(BeamHardFluxListener.HARD_FLUX_PERCENT_KEY, BeamHardFluxListener.class, ship -> new BeamHardFluxListener()),
    REMOVE_SHIELD {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            ship.setShield(ShieldAPI.ShieldType.NONE, 0f, 1f, 1f);
        }

        @Override
        public boolean supportsTemporaryGating() {
            return false;
        }

        @Override
        public String blockAllocationReason(FleetMemberAPI member, float magnitude, List<SkillEffect> currentlyAllocatedEffects) {
            return shieldTypeBlockReason(resolveDisplayShieldType(member.getHullSpec().getShieldType(), currentlyAllocatedEffects));
        }

        @Override
        public String shieldTypeBlockReason(ShieldAPI.ShieldType resolvedShieldType) {
            boolean hasShields = resolvedShieldType == ShieldAPI.ShieldType.FRONT || resolvedShieldType == ShieldAPI.ShieldType.OMNI;
            return hasShields ? null : "Ship has no shields.";
        }
    },
    CREATE_FRONT_SHIELD_IF_NONE {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            if (ship.getShield() == null) {
                ship.setShield(ShieldAPI.ShieldType.FRONT, MAKESHIFT_SHIELD_EFFICIENCY, MAKESHIFT_SHIELD_TURN_RATE_MULT, MAKESHIFT_SHIELD_ARC);
            }
        }

        @Override
        public boolean supportsTemporaryGating() {
            return false;
        }
    },
    CONVERT_SHIELD_TO_FRONT {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            ShieldAPI shield = ship.getShield();
            if (shield != null) {
                shield.setType(ShieldAPI.ShieldType.FRONT);
            }
        }

        @Override
        public boolean supportsTemporaryGating() {
            return false;
        }

        @Override
        public String blockAllocationReason(FleetMemberAPI member, float magnitude, List<SkillEffect> currentlyAllocatedEffects) {
            return shieldTypeBlockReason(resolveDisplayShieldType(member.getHullSpec().getShieldType(), currentlyAllocatedEffects));
        }

        @Override
        public String shieldTypeBlockReason(ShieldAPI.ShieldType resolvedShieldType) {
            return resolvedShieldType == ShieldAPI.ShieldType.FRONT ? "Ship already has front shields." : null;
        }
    },
    CONVERT_SHIELD_TO_OMNI {
        @Override
        public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        }

        @Override
        public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
            ShieldAPI shield = ship.getShield();
            if (shield != null) {
                shield.setType(ShieldAPI.ShieldType.OMNI);
            }
        }

        @Override
        public boolean supportsTemporaryGating() {
            return false;
        }

        @Override
        public String blockAllocationReason(FleetMemberAPI member, float magnitude, List<SkillEffect> currentlyAllocatedEffects) {
            return shieldTypeBlockReason(resolveDisplayShieldType(member.getHullSpec().getShieldType(), currentlyAllocatedEffects));
        }

        @Override
        public String shieldTypeBlockReason(ShieldAPI.ShieldType resolvedShieldType) {
            return resolvedShieldType == ShieldAPI.ShieldType.OMNI ? "Ship already has omni-directional shields." : null;
        }
    },
    SHIELD_ARC_PERCENT(PERCENT, bonus(MutableShipStatsAPI::getShieldArcBonus), StatNames.SHIELD_ARC, false),
    SHIELD_ARC_FLAT(FLAT, bonus(MutableShipStatsAPI::getShieldArcBonus), StatNames.SHIELD_ARC, false),
    SHIELD_ARC_MULT(MULT, bonus(MutableShipStatsAPI::getShieldArcBonus), StatNames.SHIELD_ARC, false),
    SHIELD_PIERCE_CHANCE_PERCENT(PERCENT, dynamicStat(Stats.SHIELD_PIERCED_MULT), "stat.chanceForShieldsToBePiercedByEmpArcs", true),
    SHIELD_PIERCE_CHANCE_MULT(MULT, dynamicStat(Stats.SHIELD_PIERCED_MULT), "stat.chanceForShieldsToBePiercedByEmpArcs", true),
    SHIELD_UPKEEP_PERCENT(PERCENT, stat(MutableShipStatsAPI::getShieldUpkeepMult), "stat.shieldFluxUpkeep", true),
    SHIELD_UPKEEP_MULT(MULT, stat(MutableShipStatsAPI::getShieldUpkeepMult), "stat.shieldFluxUpkeep", true),
    SHIELD_TURN_RATE_PERCENT(PERCENT, stat(MutableShipStatsAPI::getShieldTurnRateMult), "stat.shieldTurnRate", false),
    SHIELD_RAISE_RATE_PERCENT(PERCENT, stat(MutableShipStatsAPI::getShieldUnfoldRateMult), "stat.shieldRaiseRate", false),
    SHIELD_DAMAGE_SHARED_PERCENT(SharedShieldDamageListener.SHARED_PERCENT_KEY,
            SharedShieldDamageListener.class, SharedShieldDamageListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", Math.round(SHARED_SHIELD_DAMAGE_RANGE)).styled();
        }
    };

    private static final float SHARED_SHIELD_DAMAGE_RANGE = 1000f;

    public static final float MAKESHIFT_SHIELD_EFFICIENCY = 0.5f;
    public static final float MAKESHIFT_SHIELD_TURN_RATE_MULT = 1.2f;
    public static final float MAKESHIFT_SHIELD_ARC = 90f;

    private final EffectBacking backing;

    ShieldSkillEffect() {
        this((EffectBacking) null);
    }

    ShieldSkillEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        this(new SimpleStatEffect(mode, target, statKey, lowerIsBetter));
    }

    <T> ShieldSkillEffect(String magnitudeKey, Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        this(new ListenerEffect(magnitudeKey, listenerType, listenerFactory));
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
            } else if (effect == CONVERT_SHIELD_TO_FRONT && type != ShieldAPI.ShieldType.NONE) {
                type = ShieldAPI.ShieldType.FRONT;
            } else if (effect == CONVERT_SHIELD_TO_OMNI && type != ShieldAPI.ShieldType.NONE) {
                type = ShieldAPI.ShieldType.OMNI;
            }
        }
        return type;
    }

    static final class BeamHardFluxListener implements DamageDealtModifier {

        static final String HARD_FLUX_PERCENT_KEY = "exiledSector_beamDamageHardFluxPercent";

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (shieldHit && param instanceof BeamAPI && target instanceof ShipAPI targetShip) {
                SkillEffectSupport.ensureListener(targetShip, BeamHardFluxConverter.class, BeamHardFluxConverter::new);
            }
            return null;
        }
    }

    private static final class BeamHardFluxConverter implements DamageListener {

        private final ShipAPI ship;

        private BeamHardFluxConverter(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            float shieldFlux = result.getDamageToShields();
            if (shieldFlux <= 0f || BeamSplitListener.isApplyingSimulatedHit()
                    || !(ship.getParamAboutToApplyDamage() instanceof BeamAPI beam) || beam.getSource() == null
                    || beam.getDamage().isForceHardFlux()) return;

            convertToHardFlux(ship.getFluxTracker(), shieldFlux,
                    beam.getSource().getMutableStats().getDynamic().getValue(BeamHardFluxListener.HARD_FLUX_PERCENT_KEY, 0f));
        }
    }

    static void convertToHardFlux(FluxTrackerAPI flux, float shieldFlux, float percent) {
        float converted = shieldFlux * Math.min(percent, 100f) / 100f;
        if (converted <= 0f) return;
        flux.setHardFlux(Math.min(flux.getCurrFlux(), flux.getHardFlux() + converted));
    }

    private static final class SharedShieldDamageListener implements DamageTakenModifier, DamageListener {

        private static final String SHARED_PERCENT_KEY = "exiledSector_shieldDamageSharedPercent";
        private static final String SHARE_MOD_ID = "exiledSector_shieldDamageShared";
        private static final float MAX_SHARED_PERCENT = 90f;

        private final ShipAPI ship;
        private List<ShipAPI> allies = List.of();
        private float alliesFoundAt = -1f;
        private boolean sharePending;
        private Object pendingSource;
        private float pendingShare;

        private SharedShieldDamageListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            sharePending = false;
            if (!shieldHit) return null;

            float share = Math.min(ship.getMutableStats().getDynamic().getValue(SHARED_PERCENT_KEY, 0f), MAX_SHARED_PERCENT) / 100f;
            if (share <= 0f || nearbyAllies().isEmpty()) return null;

            damage.getModifier().modifyMult(SHARE_MOD_ID, 1f - share);
            sharePending = true;
            pendingSource = param;
            pendingShare = share;
            return SHARE_MOD_ID;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (!sharePending || ship.getParamAboutToApplyDamage() != pendingSource) return;
            sharePending = false;

            float moved = result.getDamageToShields() * pendingShare / (1f - pendingShare);
            if (moved <= 0f) return;

            float perAlly = moved / allies.size();
            for (ShipAPI ally : allies) {
                ally.getFluxTracker().increaseFlux(perAlly, true);
            }
        }

        private List<ShipAPI> nearbyAllies() {
            float now = Global.getCombatEngine().getTotalElapsedTime(false);
            if (now != alliesFoundAt) {
                alliesFoundAt = now;
                allies = CombatQueries.shipsNear(ship.getLocation(), SHARED_SHIELD_DAMAGE_RANGE, this::sharesShieldDamageWith);
            }
            return allies;
        }

        private boolean sharesShieldDamageWith(ShipAPI other) {
            return other != ship && other.getOwner() == ship.getOwner() && other.isAlive() && !other.isHulk()
                    && !other.isFighter()
                    && CombatQueries.withinRadius(other.getLocation(), ship.getLocation(), SHARED_SHIELD_DAMAGE_RANGE)
                    && !other.getFluxTracker().isOverloadedOrVenting();
        }
    }

    private static final class StatNames {
        static final String SHIELD_ARC = "stat.shieldArc";

        private StatNames() {
        }
    }
}
