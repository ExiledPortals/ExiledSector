package exiledsector.skills.skilleffect.special;

import exiledsector.i18n.StyledText;
import exiledsector.skills.AffinityScaling;
import exiledsector.skills.CombatGate;
import exiledsector.skills.SocketedItem;
import exiledsector.skills.SocketedItemScaling;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.skills.skilleffect.fighter.FighterSkillEffect;
import exiledsector.skills.skilleffect.weapon.ScopedWeaponEffect;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

import static exiledsector.skills.skilleffect.Backings.listener;

public enum SpecialSkillEffect implements BackedSkillEffect {

    EXPLODE_ON_DEATH(listener(DeathExplosionListener.FUEL_DAMAGE_PERCENT_KEY, DeathExplosionListener.class, DeathExplosionListener::new)),
    DEATH_ON_COLLISION(listener(CollisionDeathListener.class, CollisionDeathListener::new)),
    RAM_BIGGEST_ENEMY(listener(RamPilotListener.class, RamPilotListener::new)),
    HEARTLESS_ON_NEARBY_DESTRUCTION_RANGE_FLAT(listener(NearbyDestructionHeartless.RANGE_KEY,
            NearbyDestructionHeartless.class, NearbyDestructionHeartless::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("stacks", HeartlessStacks.DEFAULT_MAX_STACKS).styled();
        }
    },
    HEARTLESS_MAX_STACKS_FLAT(listener(HeartlessStacks.MAX_STACKS_KEY, HeartlessStacks.class, HeartlessStacks::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.signed(this, magnitude).styled();
        }
    },
    HEARTLESS_ARMOR_RESTORE_PERCENT(listener(HeartlessStacks.ARMOR_RESTORE_PERCENT_KEY, HeartlessStacks.class, HeartlessStacks::new)),
    HEARTLESS_ARMOR_PERCENT_PER_STACK(listener(HeartlessStacks.ARMOR_PERCENT_PER_STACK_KEY, HeartlessStacks.class, HeartlessStacks::new)),
    HEARTLESS_MOBILITY_PENALTY_PERCENT_PER_STACK(listener(HeartlessStacks.MOBILITY_PENALTY_PERCENT_PER_STACK_KEY,
            HeartlessStacks.class, HeartlessStacks::new)),
    HEARTLESS_RADIATION_EMP_FLAT(listener(HeartlessStacks.RADIATION_EMP_KEY, HeartlessStacks.class, HeartlessStacks::new)),
    HEARTLESS_CREW_DEATH_CHANCE_PERCENT_PER_STACK(listener(LiveMunitionsListener.CREW_DEATH_CHANCE_PERCENT_PER_STACK_KEY,
            LiveMunitionsListener.class, LiveMunitionsListener::new)),
    HEARTLESS_MISSILE_DAMAGE_PERCENT_PER_STACK(listener(LiveMunitionsListener.MISSILE_DAMAGE_PERCENT_PER_STACK_KEY,
            LiveMunitionsListener.class, LiveMunitionsListener::new)),
    HEARTLESS_MISSILE_SPEED_PERCENT_PER_STACK(listener(LiveMunitionsListener.MISSILE_SPEED_PERCENT_PER_STACK_KEY,
            LiveMunitionsListener.class, LiveMunitionsListener::new)),
    SERAPHS_GRACE_MAX_STACKS_FLAT(listener(SeraphsGraceListener.MAX_STACKS_KEY, SeraphsGraceListener.class, SeraphsGraceListener::new)) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).arg("value", magnitude).arg("damage", SeraphsGraceListener.DAMAGE_PER_STACK)
                    .arg("duration", SeraphsGraceListener.STACK_DURATION_SECONDS).styled();
        }
    },
    SERAPHS_GRACE_PERCENT_PER_STACK(listener(SeraphsGraceListener.PERCENT_PER_STACK_KEY, SeraphsGraceListener.class, SeraphsGraceListener::new)),
    SERAPHIM_HEAL_PERCENT(listener(SeraphimListener.HEAL_PERCENT_KEY, SeraphimListener.class, SeraphimListener::new)),
    SERAPHIM_HEAL_CAP_PERCENT(listener(SeraphimListener.HEAL_CAP_PERCENT_KEY, SeraphimListener.class, SeraphimListener::new)),
    SERAPHIM_DAMAGE_BONUS_PERCENT(listener(SeraphimListener.DAMAGE_BONUS_PERCENT_KEY, SeraphimListener.class, SeraphimListener::new)),
    SERAPHIM_FULL_BONUS_DAMAGE_FLAT(listener(SeraphimListener.FULL_BONUS_DAMAGE_KEY, SeraphimListener.class, SeraphimListener::new)),
    LOW_TECH_AFFINITY_FLAT(new AffinityTally()),
    HIGH_TECH_AFFINITY_FLAT(new AffinityTally()),
    MIDLINE_AFFINITY_FLAT(new AffinityTally()),
    BALLISTIC_WEAPON_DAMAGE_PERCENT_PER_LOW_TECH_AFFINITY(new AffinityScaling(AffinityScaling.Filter.LOW_TECH, AffinityScaling.SMALL_STEP,
            ScopedWeaponEffect.find(WeaponStatFamily.DAMAGE, WeaponScope.BALLISTIC, StatMode.PERCENT))),
    ENERGY_WEAPON_DAMAGE_PERCENT_PER_HIGH_TECH_AFFINITY(new AffinityScaling(AffinityScaling.Filter.HIGH_TECH, AffinityScaling.SMALL_STEP,
            ScopedWeaponEffect.find(WeaponStatFamily.DAMAGE, WeaponScope.ENERGY, StatMode.PERCENT))),
    MISSILE_WEAPON_DAMAGE_PERCENT_PER_MIDLINE_AFFINITY(new AffinityScaling(AffinityScaling.Filter.MIDLINE, AffinityScaling.SMALL_STEP,
            ScopedWeaponEffect.find(WeaponStatFamily.DAMAGE, WeaponScope.MISSILE, StatMode.PERCENT))),
    ARMOR_PERCENT_PER_LOW_TECH_AFFINITY(new AffinityScaling(AffinityScaling.Filter.LOW_TECH, AffinityScaling.LARGE_STEP, DefenseSkillEffect.ARMOR_PERCENT)),
    WEAPON_RANGE_PERCENT_PER_AFFINITY(new AffinityScaling(AffinityScaling.Filter.ALL, AffinityScaling.LARGE_STEP,
            ScopedWeaponEffect.find(WeaponStatFamily.RANGE, WeaponScope.ALL, StatMode.PERCENT))),
    FIGHTER_WEAPON_DAMAGE_PERCENT_PER_AFFINITY(new AffinityScaling(AffinityScaling.Filter.ALL, AffinityScaling.LARGE_STEP,
            FighterSkillEffect.FIGHTER_WEAPON_DAMAGE_PERCENT)),
    FIGHTER_DAMAGE_TAKEN_MULT_PER_AFFINITY(new AffinityScaling(AffinityScaling.Filter.ALL, AffinityScaling.LARGEST_STEP,
            FighterSkillEffect.FIGHTER_DAMAGE_TAKEN_MULT)),
    HIGHEST_AFFINITY_FLAT_PER_S_MOD(new AffinityTally()),
    LOWEST_AFFINITY_AS_HIGHEST(new AffinityTally()) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.msg(this).styled();
        }
    },
    EVERY_AFFINITY_FLAT_PER_HOLLOW_NODE(new AffinityTally()),
    AFFINITY_EXCHANGE_TO_LOW_TECH_PERCENT(new AffinityTally()) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.templated(this, magnitude);
        }
    },
    AFFINITY_EXCHANGE_TO_HIGH_TECH_PERCENT(new AffinityTally()) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.templated(this, magnitude);
        }
    },
    AFFINITY_EXCHANGE_TO_MIDLINE_PERCENT(new AffinityTally()) {
        @Override
        public StyledText description(float magnitude) {
            return EffectText.templated(this, magnitude);
        }
    },
    GATES_RADIUS_BELOW_HALF_HULL(new RadiusGate(CombatGate.LOW_HULL)),
    GATES_RADIUS_WHILE_SYSTEM_ACTIVE(new RadiusGate(CombatGate.SYSTEM_ACTIVE)),
    GATES_RADIUS_WHILE_PHASED(new RadiusGate(CombatGate.PHASED)),
    SOCKETED_HULL_ITEM_EFFECT_PERCENT(new SocketedItemScaling(SocketedItemScaling.Scope.ALL, SocketedItem.HULL_ITEM)),
    TARGETED_HULL_ITEM_EFFECT_PERCENT(new SocketedItemScaling(SocketedItemScaling.Scope.TARGETED, SocketedItem.HULL_ITEM)),
    SOCKETED_HULL_ITEM_DRAWBACK_EFFECT_PERCENT(new SocketedItemScaling(SocketedItemScaling.Scope.DRAWBACKS, SocketedItem.HULL_ITEM)),
    SOCKETED_SUBROUTINE_EFFECT_PERCENT(new SocketedItemScaling(SocketedItemScaling.Scope.ALL, SocketedItem.SUBROUTINE)),
    SOCKETED_SUBROUTINE_DRAWBACK_EFFECT_PERCENT(new SocketedItemScaling(SocketedItemScaling.Scope.DRAWBACKS, SocketedItem.SUBROUTINE));

    private final EffectBacking backing;

    SpecialSkillEffect(EffectBacking backing) {
        this.backing = backing;
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
