package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import exiledsector.i18n.StyledText;
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
    BEAM_WEAPON_SHIELD_DISRUPTION_PERCENT(listener(BeamShieldDisruptionListener.SHIELD_DAMAGE_PERCENT_KEY, BeamShieldDisruptionListener.class,
            BeamShieldDisruptionListener::new)),
    BEAM_WEAPON_SPLIT_TARGETS_FLAT(listener(BeamSplitListener.TARGETS_KEY, BeamSplitListener.class, BeamSplitListener::new)) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).count(Math.round(magnitude)).arg("value", magnitude).styled();
        }
    },
    NON_BEAM_ENERGY_WEAPON_CHAIN_CHANCE_PERCENT(listener(EnergyChainListener.CHANCE_KEY, EnergyChainListener.class, EnergyChainListener::new)) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }
    },
    NON_BEAM_ENERGY_WEAPON_CHAIN_FALLOFF_PERCENT(listener(EnergyChainListener.FALLOFF_KEY, EnergyChainListener.class, EnergyChainListener::new)) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }
    },
    BALLISTIC_WEAPON_PIERCE_CHANCE_PERCENT(listener(BallisticPierceListener.CHANCE_KEY, BallisticPierceListener.class, BallisticPierceListener::new)),
    NON_MISSILE_PROJECTILE_ECHO_CHANCE_PERCENT(listener(ProjectileEchoListener.CHANCE_KEY, ProjectileEchoListener.class,
            ProjectileEchoListener::new)),
    ION_ARC_CHANCE_PERCENT(listener(IonArcListener.CHANCE_AT_FULL_HARD_FLUX_KEY, IonArcListener.class, IonArcListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("damage", IonArcListener.ARC_DAMAGE_FRACTION * 100f)
                    .arg("emp", IonArcListener.ARC_EMP_FRACTION * 100f).styled();
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
    MISSILE_SNARE_TOP_SPEED_PERCENT(listener(MissileSnareListener.TOP_SPEED_PERCENT_KEY, MissileSnareListener.class, MissileSnareListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("seconds", MissileSnareListener.SNARE_SECONDS).styled();
        }
    },
    KILL_FLUX_VENT_PERCENT(listener(KillFluxVentListener.MAX_FLUX_PERCENT_KEY, KillFluxVentListener.class, KillFluxVentListener::new)),
    CORROSION_MOTE_DAMAGE_FLAT(listener(CorrosionMoteListener.MOTE_DAMAGE_KEY, CorrosionMoteListener.class, CorrosionMoteListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("seconds", CorrosionMoteListener.CONTACT_SECONDS_PER_MOTE)
                    .arg("max", Motes.MAX_LIVE_MOTES_PER_SOURCE).styled();
        }
    },
    BALLISTIC_HARDPOINT_KINETIC_TO_HIGH_EXPLOSIVE(new DamageConversion(DamageConversion.KEY_PREFIX + "ballisticHardpointKineticToHighExplosive",
            WeaponType.BALLISTIC, null, DamageConversion.Mounting.HARDPOINT, false, DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE)),
    BEAM_ENERGY_TO_HIGH_EXPLOSIVE(new DamageConversion(DamageConversion.KEY_PREFIX + "beamEnergyToHighExplosive",
            null, null, DamageConversion.Mounting.ANY, true, DamageType.ENERGY, DamageType.HIGH_EXPLOSIVE)),
    SMALL_ENERGY_MOUNT_DAMAGE_TO_EMP(new DamageConversion(DamageConversion.KEY_PREFIX + "smallEnergyMountDamageToEmp",
            WeaponType.ENERGY, WeaponSize.SMALL, DamageConversion.Mounting.ANY, false, null, null));

    private static List<DamageConversion> damageConversions;

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
