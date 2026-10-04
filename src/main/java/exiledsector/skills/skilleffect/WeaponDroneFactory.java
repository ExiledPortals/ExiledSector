package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponGroupType;
import org.lwjgl.util.vector.Vector2f;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

final class WeaponDroneFactory {

    static final String HULL_ID = "exiledSector_split_beam_drone";

    static final float SINGLE_SHOT_REFIRE_DELAY = 60f;

    private static final String INVULNERABLE_MOD_ID = "exiledSector_splitBeamDrone";
    private static final float MOTHERSHIP_FLAG_DURATION = 100000f;
    private static final Map<String, Boolean> BEAM_SUPPORT_BY_WEAPON_ID = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> PROJECTILE_SUPPORT_BY_WEAPON_ID = new ConcurrentHashMap<>();
    private static final Map<WeaponSize, String> SLOT_IDS = Map.of(
            WeaponSize.SMALL, "WS SMALL",
            WeaponSize.MEDIUM, "WS MEDIUM",
            WeaponSize.LARGE, "WS LARGE");

    private WeaponDroneFactory() {
    }

    static boolean supportsBeam(WeaponAPI weapon) {
        if (!(weapon.getSpec() instanceof BeamWeaponSpecAPI spec) || spec.getWeaponId() == null) {
            return false;
        }
        return BEAM_SUPPORT_BY_WEAPON_ID.computeIfAbsent(spec.getWeaponId(),
                id -> !isBlocklistedBeam(spec) && canMountOnDrone(weapon));
    }

    static boolean supportsProjectile(WeaponAPI weapon) {
        if (!(weapon.getSpec() instanceof ProjectileWeaponSpecAPI spec) || spec.getWeaponId() == null) {
            return false;
        }
        return PROJECTILE_SUPPORT_BY_WEAPON_ID.computeIfAbsent(spec.getWeaponId(),
                id -> spec.getProjectileSpec() instanceof ProjectileSpecAPI && canMountOnDrone(weapon));
    }

    static void markProjectileUnsupported(WeaponAPI weapon) {
        PROJECTILE_SUPPORT_BY_WEAPON_ID.put(weapon.getSpec().getWeaponId(), false);
    }

    private static boolean isBlocklistedBeam(BeamWeaponSpecAPI spec) {
        return spec.getBeamEffect() != null && CsvIdList.SPLIT_BEAM_EFFECTS.contains(spec.getBeamEffect().getClass().getName());
    }

    private static boolean canMountOnDrone(WeaponAPI weapon) {
        return SLOT_IDS.containsKey(weapon.getSize()) && Global.getSettings().getHullSpec(HULL_ID) != null;
    }

    static ShipAPI create(ShipAPI firingShip, WeaponAPI weapon, DamageDealtModifier firstListener) {
        ShipAPI drone = Global.getCombatEngine().createFXDrone(variantFor(weapon));
        setUp(firingShip, drone);
        drone.addListener(firstListener);
        shareDamageListeners(firingShip, drone);
        Global.getCombatEngine().addEntity(drone);
        return drone;
    }

    static <T> T createSingleShot(ShipAPI firingShip, WeaponAPI weapon, Function<ShipAPI, T> controllerFactory) {
        ShipVariantAPI variant = variantFor(weapon);
        ProjectileWeaponSpecAPI shared = (ProjectileWeaponSpecAPI) Global.getSettings().getWeaponSpec(weapon.getSpec().getWeaponId());
        float chargeTime = shared.getChargeTime();
        int burstSize = shared.getBurstSize();
        float refireDelay = shared.getRefireDelay();
        ShipAPI drone;
        try {
            shared.setChargeTime(0f);
            shared.setBurstSize(1);
            shared.setRefireDelay(SINGLE_SHOT_REFIRE_DELAY);
            drone = Global.getCombatEngine().createFXDrone(variant);
            drone.getAllWeapons().get(0).ensureClonedSpec();
        } finally {
            shared.setChargeTime(chargeTime);
            shared.setBurstSize(burstSize);
            shared.setRefireDelay(refireDelay);
        }
        setUp(firingShip, drone);
        T controller = controllerFactory.apply(drone);
        drone.addListener(controller);
        shareDamageListeners(firingShip, drone);
        Global.getCombatEngine().addEntity(drone);
        return controller;
    }

    private static ShipVariantAPI variantFor(WeaponAPI weapon) {
        ShipHullSpecAPI hull = Global.getSettings().getHullSpec(HULL_ID);
        ShipVariantAPI variant = Global.getSettings().createEmptyVariant(HULL_ID, hull);
        String slotId = SLOT_IDS.get(weapon.getSize());
        variant.addWeapon(slotId, weapon.getSpec().getWeaponId());
        WeaponGroupSpec group = new WeaponGroupSpec(WeaponGroupType.LINKED);
        group.addSlot(slotId);
        variant.addWeaponGroup(group);
        return variant;
    }

    private static void setUp(ShipAPI firingShip, ShipAPI drone) {
        drone.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        drone.setOwner(firingShip.getOwner());
        drone.setDrone(true);
        drone.getAIFlags().setFlag(AIFlags.DRONE_MOTHERSHIP, MOTHERSHIP_FLAG_DURATION, firingShip);
        drone.setCollisionClass(CollisionClass.NONE);
        drone.giveCommand(ShipCommand.SELECT_GROUP, null, 0);
        drone.getMutableStats().getHullDamageTakenMult().modifyMult(INVULNERABLE_MOD_ID, 0f);
        drone.setCaptain(officerCopy(firingShip.getCaptain()));
        WeaponDroneStats.mirror(firingShip.getMutableStats(), drone.getMutableStats());
    }

    static PersonAPI officerCopy(PersonAPI captain) {
        PersonAPI copy = Global.getFactory().createPerson();
        if (captain == null) {
            return copy;
        }
        copy.setName(captain.getName());
        copy.setPortraitSprite(captain.getPortraitSprite());
        copy.setPersonality(captain.getPersonalityAPI().getId());
        copy.setAICoreId(captain.getAICoreId());
        copy.getStats().setLevel(captain.getStats().getLevel());
        for (MutableCharacterStatsAPI.SkillLevelAPI skill : captain.getStats().getSkillsCopy()) {
            copy.getStats().setSkillLevel(skill.getSkill().getId(), skill.getLevel());
        }
        return copy;
    }

    static void shareDamageListeners(ShipAPI firingShip, ShipAPI drone) {
        for (DamageDealtModifier listener : firingShip.getListeners(DamageDealtModifier.class)) {
            if (listener instanceof DroneSpawner) {
                continue;
            }
            drone.removeListenerOfClass(listener.getClass());
            drone.addListener(listener instanceof AdvanceableListener ? new SharedDamageModifier(listener) : listener);
        }
    }

    static void mirrorMarkerHullMods(ShipAPI firingShip, ShipAPI drone) {
        Collection<String> droneMods = drone.getVariant().getHullMods();
        for (String hullModId : firingShip.getVariant().getHullMods()) {
            if (CsvIdList.DRONE_MARKER_HULLMODS.contains(hullModId) && !droneMods.contains(hullModId)) {
                droneMods.add(hullModId);
            }
        }
    }

    record SharedDamageModifier(DamageDealtModifier listener) implements DamageDealtModifier {

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            return listener.modifyDamageDealt(param, target, damage, point, shieldHit);
        }
    }
}
