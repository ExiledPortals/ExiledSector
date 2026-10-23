package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledlib.i18n.StyledText;
import exiledlib.motes.Motes;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.ListenerEffect;

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
    };

    private final EffectBacking backing;

    <T> OnHitSkillEffect(String magnitudeKey, Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        this.backing = new ListenerEffect<>(magnitudeKey, listenerType, listenerFactory);
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
