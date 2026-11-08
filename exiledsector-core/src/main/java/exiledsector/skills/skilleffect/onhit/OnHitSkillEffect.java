package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import exiledsector.i18n.Message;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.motes.Motes;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static exiledsector.skills.skilleffect.Backings.listener;

public enum OnHitSkillEffect implements BackedSkillEffect {

    BEAM_WEAPON_HARD_FLUX_PERCENT(listener(BeamHardFluxListener.HARD_FLUX_PERCENT_KEY, BeamHardFluxListener.class, ship -> new BeamHardFluxListener())),
    BEAM_WEAPON_EMP_PERCENT(listener(BeamEmpListener.EMP_PERCENT_KEY, BeamEmpListener.class, BeamEmpListener::new)),
    BEAM_WEAPON_SHIELD_DISRUPTION_PERCENT(listener(BeamShieldDisruptionListener.SHIELD_DAMAGE_PERCENT_KEY, BeamShieldDisruptionListener.class,
            BeamShieldDisruptionListener::new)),
    BEAM_WEAPON_SPLIT_TARGETS_FLAT(listener(LionsGazeListener.TARGETS_KEY, LionsGazeListener.class, LionsGazeListener::new)) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).count(Math.round(magnitude)).arg("value", magnitude).styled();
        }
    },
    BEAM_WEAPON_FULL_DAMAGE_SPLIT_TARGETS_FLAT(listener(LionsGazeListener.FULL_DAMAGE_TARGETS_KEY, LionsGazeListener.class, LionsGazeListener::new)) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).count(Math.round(magnitude)).arg("value", magnitude).styled();
        }
    },
    NON_BEAM_ENERGY_WEAPON_REFRACTION_CHANCE_PERCENT(listener(RefractionListener.CHANCE_KEY, RefractionListener.class, RefractionListener::new)) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }
    },
    NON_BEAM_ENERGY_WEAPON_HULL_REFRACTION_CHANCE_PERCENT(listener(RefractionListener.HULL_CHANCE_KEY, RefractionListener.class,
            RefractionListener::new)) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }
    },
    NON_BEAM_ENERGY_WEAPON_REFRACTION_FALLOFF_MULT(listener(RefractionListener.FALLOFF_KEY,RefractionListener.class, RefractionListener::new)) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }
    },
    BALLISTIC_WEAPON_PIERCE_CHANCE_PERCENT(listener(BallisticPierceListener.CHANCE_KEY, BallisticPierceListener.class, BallisticPierceListener::new)),
    BALLISTIC_WEAPON_ADDITIONAL_PIERCES_FLAT(listener(BallisticPierceListener.ADDITIONAL_PIERCES_KEY, BallisticPierceListener.class,
            BallisticPierceListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).count(Math.round(magnitude)).arg("value", magnitude).styled();
        }
    },
    NON_MISSILE_PROJECTILE_ECHO_CHANCE_PERCENT(listener(ProjectileEchoListener.CHANCE_KEY, ProjectileEchoListener.class,
            ProjectileEchoListener::new)),
    ION_ARC_CHANCE_PERCENT(listener(IonArcListener.CHANCE_AT_FULL_HARD_FLUX_KEY, IonArcListener.class, IonArcListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("damage", IonArcListener.ARC_DAMAGE_FRACTION * 100f)
                    .arg("emp", IonArcListener.ARC_EMP_FRACTION * 100f).styled();
        }
    },
    RIFT_STRIKE_CHANCE_PERCENT(listener(RiftStrike.CHANCE_PERCENT_KEY, RiftStrikeListener.class, RiftStrikeListener::forShip)) {
        @Override
        public StyledText description(float magnitude) {
            return RiftStrike.describe(EffectText.msg(this).arg("value", magnitude));
        }
    },
    BEAM_RIFT_STRIKE_CHANCE_PERCENT(listener(RiftStrike.BEAM_CHANCE_PERCENT_KEY, RiftStrikeListener.class, RiftStrikeListener::forShip)) {
        @Override
        public StyledText description(float magnitude) {
            return RiftStrike.describe(EffectText.msg(this).arg("value", magnitude));
        }
    },
    MISSILE_HIT_RIFT_SERIES_CHANCE_PERCENT(listener(ResonatorPayloadListener.CHANCE_PERCENT_KEY, ResonatorPayloadListener.class,
            ResonatorPayloadListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return describeRiftSeries(EffectText.msg(this).arg("value", magnitude));
        }
    },
    MISSILE_HIT_RIFT_SERIES_EXPLOSIONS_FLAT(listener(ResonatorPayloadListener.ADDITIONAL_RIFTS_KEY, ResonatorPayloadListener.class,
            ResonatorPayloadListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).count(Math.round(magnitude)).arg("value", magnitude).styled();
        }
    },
    DISINTEGRATION_ARMOR_DAMAGE_PERCENT(listener(DisintegrationListener.ARMOR_DAMAGE_PERCENT_KEY,
            DisintegrationListener.class, DisintegrationListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("seconds", DisintegrationListener.STRIP_SECONDS).styled();
        }
    },
    BALLISTIC_DISINTEGRATION_ARMOR_DAMAGE_PERCENT(listener(DisintegrationListener.BALLISTIC_ARMOR_DAMAGE_PERCENT_KEY,
            DisintegrationListener.class, DisintegrationListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("seconds", DisintegrationListener.STRIP_SECONDS).styled();
        }
    },
    ARMOR_BURN_DAMAGE_PERCENT(listener(ArmorBurnListener.BURN_PERCENT_KEY, ArmorBurnListener.class, ArmorBurnListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("seconds", ArmorBurnListener.BURN_SECONDS).styled();
        }
    },
    MISSILE_SNARE_TOP_SPEED_MULT(listener(MissileSnareListener.TOP_SPEED_PERCENT_KEY, MissileSnareListener.class, MissileSnareListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("seconds", MissileSnareListener.SNARE_SECONDS).styled();
        }
    },
    KILL_FLUX_VENT_PERCENT(listener(KillFluxVentListener.MAX_FLUX_PERCENT_KEY, KillFluxVentListener.class, KillFluxVentListener::new)),
    MISSILE_KILL_FLAK_DAMAGE_FLAT(listener(FlakBurstListener.DAMAGE_KEY, FlakBurstListener.class, FlakBurstListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("range", FlakBurstListener.BURST_RADIUS).styled();
        }
    },
    MISSILE_KILL_FLAK_RADIUS_FLAT(listener(FlakBurstListener.RADIUS_BONUS_KEY, FlakBurstListener.class, FlakBurstListener::new)),
    LARGE_BALLISTIC_HIT_DISABLE_CHANCE_PERCENT(listener(ConcussionShellListener.CHANCE_PERCENT_KEY, ConcussionShellListener.class,
            ConcussionShellListener::new)),
    MEDIUM_BALLISTIC_HIT_DISABLE_CHANCE_PERCENT(listener(ConcussionShellListener.MEDIUM_CHANCE_PERCENT_KEY, ConcussionShellListener.class,
            ConcussionShellListener::new)),
    HULL_DAMAGE_FRAGMENT_REGROWTH_FLAT(listener(HullDamageRegrowthListener.FRAGMENTS_PER_STEP_KEY, HullDamageRegrowthListener.class,
            HullDamageRegrowthListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("damage", HullDamageRegrowthListener.HULL_DAMAGE_PER_STEP).styled();
        }
    },
    STARDUST_MOTE_DAMAGE_FLAT(listener(StardustMoteListener.MOTE_DAMAGE_KEY, StardustMoteListener.class, StardustMoteListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("seconds", StardustMoteListener.CONTACT_SECONDS_PER_MOTE)
                    .arg("max", Motes.MAX_LIVE_MOTES_PER_SOURCE).styled();
        }
    },
    STARDUST_MOTE_MAX_IN_FLIGHT_FLAT(listener(StardustMoteListener.EXTRA_MOTES_IN_FLIGHT_KEY, StardustMoteListener.class, StardustMoteListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.signed(this, magnitude).styled();
        }
    },
    DISABLED_PART_EMP_TO_HIGH_EXPLOSIVE_PERCENT(listener(IonCascadeListener.HIGH_EXPLOSIVE_SHARE_PERCENT_KEY, IonCascadeListener.class, IonCascadeListener::new)) {
        @Override
        public boolean readsFinalDealtDamage() {
            return true;
        }
    },
    SHIELD_EMP_TO_ENERGY_PERCENT(listener(IonSpillListener.ENERGY_SHARE_PERCENT_KEY, IonSpillListener.class, IonSpillListener::new)) {
        @Override
        public boolean readsFinalDealtDamage() {
            return true;
        }
    },
    ADAPTIVE_DAMAGE_TYPE_CHANCE_PERCENT(listener(DamageConversionListener.ADAPTIVE_CHANCE_PERCENT_KEY, DamageConversionListener.class,
            DamageConversionListener::new)) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }
    },
    ENERGY_HIT_INJUNCTION_PERCENT(listener(InjunctionListener.SLOWDOWN_PERCENT_KEY, InjunctionListener.class, InjunctionListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("seconds", Injunction.STACK_SECONDS).arg("stacks", Injunction.MAX_STACKS).styled()
                    .append("\n").append(Translation.msg("injunction.rechargeSlowdown").arg("value", magnitude).styled());
        }
    },
    ENERGY_HIT_INJUNCTION_VENT_PERCENT(listener(InjunctionListener.VENT_SLOWDOWN_PERCENT_KEY, InjunctionListener.class, InjunctionListener::new)),
    BALLISTIC_HARDPOINT_KINETIC_TO_HIGH_EXPLOSIVE(new DamageConversion(DamageConversion.KEY_PREFIX + "ballisticHardpointKineticToHighExplosive",
            WeaponType.BALLISTIC, null, DamageConversion.Mounting.HARDPOINT, false, DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE)),
    BEAM_ENERGY_TO_HIGH_EXPLOSIVE(new DamageConversion(DamageConversion.KEY_PREFIX + "beamEnergyToHighExplosive",
            null, null, DamageConversion.Mounting.ANY, true, DamageType.ENERGY, DamageType.HIGH_EXPLOSIVE)),
    SMALL_ENERGY_MOUNT_DAMAGE_TO_EMP(new DamageConversion(DamageConversion.KEY_PREFIX + "smallEnergyMountDamageToEmp",
            WeaponType.ENERGY, WeaponSize.SMALL, DamageConversion.Mounting.ANY, false, null, null));

    private static List<DamageConversion> damageConversions;

    public static StyledText describeRiftSeries(Message line) {
        return line.arg("count", ResonatorPayloadListener.RIFTS_PER_SERIES).arg("share", ResonatorPayloadListener.DAMAGE_SHARE_PERCENT)
                .arg("max", ResonatorPayloadListener.MAX_DAMAGE).styled();
    }

    private final EffectBacking backing;
    private final DamageConversion damageConversion;

    OnHitSkillEffect(EffectBacking backing) {
        this.backing = backing;
        this.damageConversion = null;
    }

    OnHitSkillEffect(DamageConversion damageConversion) {
        this.backing = listener(damageConversion.flagKey(), DamageConversionListener.class, DamageConversionListener::new);
        this.damageConversion = damageConversion;
    }

    DamageConversion damageConversion() {
        return damageConversion;
    }

    static List<DamageConversion> damageConversions() {
        if (damageConversions == null) {
            damageConversions = Arrays.stream(values()).map(OnHitSkillEffect::damageConversion).filter(Objects::nonNull).toList();
        }
        return damageConversions;
    }

    @Override
    public boolean reshapesDealtDamage() {
        return damageConversion != null;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
