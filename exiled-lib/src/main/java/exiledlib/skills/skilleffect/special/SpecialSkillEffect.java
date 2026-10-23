package exiledlib.skills.skilleffect.special;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledlib.i18n.StyledText;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.ListenerEffect;

import java.util.function.Function;

public enum SpecialSkillEffect implements BackedSkillEffect {

    EXPLODE_ON_DEATH(DeathExplosionListener.FUEL_DAMAGE_PERCENT_KEY, DeathExplosionListener.class, DeathExplosionListener::new),
    DEATH_ON_COLLISION(CollisionDeathListener.class, CollisionDeathListener::new),
    RAM_BIGGEST_ENEMY(RamPilotListener.class, RamPilotListener::new),
    HEARTLESS_ON_NEARBY_DESTRUCTION_RANGE_FLAT(NearbyDestructionHeartless.RANGE_KEY,
            NearbyDestructionHeartless.class, NearbyDestructionHeartless::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("stacks", HeartlessStacks.DEFAULT_MAX_STACKS).styled();
        }
    },
    HEARTLESS_MAX_STACKS_FLAT(HeartlessStacks.MAX_STACKS_KEY, HeartlessStacks.class, HeartlessStacks::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.signed(this, magnitude).styled();
        }
    },
    HEARTLESS_ARMOR_RESTORE_PERCENT(HeartlessStacks.ARMOR_RESTORE_PERCENT_KEY, HeartlessStacks.class, HeartlessStacks::new),
    HEARTLESS_ARMOR_PERCENT_PER_STACK(HeartlessStacks.ARMOR_PERCENT_PER_STACK_KEY, HeartlessStacks.class, HeartlessStacks::new),
    HEARTLESS_MOBILITY_PENALTY_PERCENT_PER_STACK(HeartlessStacks.MOBILITY_PENALTY_PERCENT_PER_STACK_KEY,
            HeartlessStacks.class, HeartlessStacks::new),
    HEARTLESS_RADIATION_EMP_FLAT(HeartlessStacks.RADIATION_EMP_KEY, HeartlessStacks.class, HeartlessStacks::new),
    HEARTLESS_CREW_DEATH_CHANCE_PERCENT_PER_STACK(LiveMunitionsListener.CREW_DEATH_CHANCE_PERCENT_PER_STACK_KEY,
            LiveMunitionsListener.class, LiveMunitionsListener::new),
    HEARTLESS_MISSILE_DAMAGE_PERCENT_PER_STACK(LiveMunitionsListener.MISSILE_DAMAGE_PERCENT_PER_STACK_KEY,
            LiveMunitionsListener.class, LiveMunitionsListener::new),
    HEARTLESS_MISSILE_SPEED_PERCENT_PER_STACK(LiveMunitionsListener.MISSILE_SPEED_PERCENT_PER_STACK_KEY,
            LiveMunitionsListener.class, LiveMunitionsListener::new),
    SERAPHS_GRACE_MAX_STACKS_FLAT(SeraphsGraceListener.MAX_STACKS_KEY, SeraphsGraceListener.class, SeraphsGraceListener::new) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("damage", SeraphsGraceListener.DAMAGE_PER_STACK)
                    .arg("duration", SeraphsGraceListener.STACK_DURATION_SECONDS).styled();
        }
    },
    SERAPHS_GRACE_PERCENT_PER_STACK(SeraphsGraceListener.PERCENT_PER_STACK_KEY, SeraphsGraceListener.class, SeraphsGraceListener::new),
    SERAPHIM_HEAL_PERCENT(SeraphimListener.HEAL_PERCENT_KEY, SeraphimListener.class, SeraphimListener::new),
    SERAPHIM_HEAL_CAP_PERCENT(SeraphimListener.HEAL_CAP_PERCENT_KEY, SeraphimListener.class, SeraphimListener::new),
    SERAPHIM_DAMAGE_BONUS_PERCENT(SeraphimListener.DAMAGE_BONUS_PERCENT_KEY, SeraphimListener.class, SeraphimListener::new),
    SERAPHIM_FULL_BONUS_DAMAGE_FLAT(SeraphimListener.FULL_BONUS_DAMAGE_KEY, SeraphimListener.class, SeraphimListener::new);

    private final EffectBacking backing;

    <T> SpecialSkillEffect(Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        this(null, listenerType, listenerFactory);
    }

    <T> SpecialSkillEffect(String magnitudeKey, Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        this.backing = new ListenerEffect<>(magnitudeKey, listenerType, listenerFactory);
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
