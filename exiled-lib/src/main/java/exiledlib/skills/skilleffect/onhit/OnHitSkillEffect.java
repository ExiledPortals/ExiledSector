package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import exiledlib.i18n.StyledText;
import exiledlib.motes.Motes;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.ListenerEffect;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public enum OnHitSkillEffect implements BackedSkillEffect {

    BEAM_WEAPON_HARD_FLUX_PERCENT(BeamHardFluxListener.HARD_FLUX_PERCENT_KEY, BeamHardFluxListener.class, ship -> new BeamHardFluxListener()),
    BEAM_WEAPON_SPLIT_TARGETS_FLAT(BeamSplitListener.TARGETS_KEY, BeamSplitListener.class, BeamSplitListener::new) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }

        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).count(Math.round(magnitude)).arg("value", magnitude).styled();
        }
    },
    NON_BEAM_ENERGY_WEAPON_CHAIN_CHANCE_PERCENT(EnergyChainListener.CHANCE_KEY, EnergyChainListener.class, EnergyChainListener::new) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }
    },
    NON_BEAM_ENERGY_WEAPON_CHAIN_FALLOFF_PERCENT(EnergyChainListener.FALLOFF_KEY, EnergyChainListener.class, EnergyChainListener::new) {
        @Override
        public boolean reshapesDealtDamage() {
            return true;
        }
    },
    BALLISTIC_WEAPON_PIERCE_CHANCE_PERCENT(BallisticPierceListener.CHANCE_KEY, BallisticPierceListener.class, BallisticPierceListener::new),
    NON_MISSILE_PROJECTILE_ECHO_CHANCE_PERCENT(ProjectileEchoListener.CHANCE_KEY, ProjectileEchoListener.class, ProjectileEchoListener::new),
    ION_ARC_CHANCE_PERCENT(IonArcListener.CHANCE_AT_FULL_HARD_FLUX_KEY, IonArcListener.class, IonArcListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("damage", IonArcListener.ARC_DAMAGE_FRACTION * 100f)
                    .arg("emp", IonArcListener.ARC_EMP_FRACTION * 100f).styled();
        }
    },
    DISINTEGRATION_ARMOR_DAMAGE_PERCENT(DisintegrationListener.ARMOR_DAMAGE_PERCENT_KEY,
            DisintegrationListener.class, DisintegrationListener::new),
    CORROSION_MOTE_DAMAGE_FLAT(CorrosionMoteListener.MOTE_DAMAGE_KEY, CorrosionMoteListener.class, CorrosionMoteListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("seconds", CorrosionMoteListener.CONTACT_SECONDS_PER_MOTE)
                    .arg("max", Motes.MAX_LIVE_MOTES_PER_SOURCE).styled();
        }
    },
    BALLISTIC_HARDPOINT_KINETIC_TO_HIGH_EXPLOSIVE(new DamageConversion(DamageConversion.KEY_PREFIX + "ballisticHardpointKineticToHighExplosive",
            WeaponType.BALLISTIC, null, DamageConversion.Mounting.HARDPOINT, false, DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE)),
    ENERGY_TURRET_BEAM_ENERGY_TO_HIGH_EXPLOSIVE(new DamageConversion(DamageConversion.KEY_PREFIX + "energyTurretBeamEnergyToHighExplosive",
            WeaponType.ENERGY, null, DamageConversion.Mounting.TURRET, true, DamageType.ENERGY, DamageType.HIGH_EXPLOSIVE)),
    SMALL_ENERGY_MOUNT_DAMAGE_TO_EMP(new DamageConversion(DamageConversion.KEY_PREFIX + "smallEnergyMountDamageToEmp",
            WeaponType.ENERGY, WeaponSize.SMALL, DamageConversion.Mounting.ANY, false, null, null));

    private static List<DamageConversion> damageConversions;

    private final EffectBacking backing;
    private final DamageConversion damageConversion;

    <T> OnHitSkillEffect(String magnitudeKey, Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        this.backing = new ListenerEffect<>(magnitudeKey, listenerType, listenerFactory);
        this.damageConversion = null;
    }

    OnHitSkillEffect(DamageConversion damageConversion) {
        this.backing = new ListenerEffect<>(damageConversion.flagKey(), DamageConversionListener.class, DamageConversionListener::new);
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
