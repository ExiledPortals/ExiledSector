package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillEffectType;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatReadinessPlugin;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.CrewCompositionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.hullmods.ConvertedHangar;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.ModSounds;
import exiledsector.skills.skilleffect.DeploymentRecordPruner;
import exiledsector.skills.skilleffect.TemporaryCombatSpawn;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class CarrierLaunch {

    static final float LAUNCHED_WING_CREW = 100000f;
    static final float MIN_WING_MEMBER_READINESS = 0.01f;
    static final String SHIP_FIGHTERS_EFFECT_INFIX = "_ship_fighters_";
    static final String ALL_FIGHTERS_EFFECT_INFIX = "_all_fighters_";
    static final String TAKEOFF_SOUND = "fighter_takeoff";
    private static final Logger LOG = Logger.getLogger(CarrierLaunch.class);
    private static final Set<String> REPORTED_UNLAUNCHABLE_TYPES = new HashSet<>();
    private static final Set<String> REPORTED_SETTLING_ERROR_TYPES = new HashSet<>();

    private CarrierLaunch() {
    }

    static void readyTemporaryWingMember(FleetMemberAPI wingMember, ShipAPI carrier) {
        wingMember.setOwner(carrier.getOwner());
        wingMember.setAlly(carrier.isAlly());
        CrewCompositionAPI crew = wingMember.getCrewComposition();
        if (crew != null) {
            crew.setCrew(LAUNCHED_WING_CREW);
        }
        wingMember.getRepairTracker().setCR(Math.max(MIN_WING_MEMBER_READINESS, carrier.getCRAtDeployment()));
    }

    static ShipAPI spawnLoneFighter(CombatEngineAPI engine, CombatFleetManagerAPI fleetManager, FleetMemberAPI wingMember, String wingSpecId,
                                    Vector2f launchLocation, float launchFacing, boolean keepSpawnedWing, String featureName) {
        FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(wingSpecId);
        int normalWingSize = spec.getNumFighters();
        spec.setNumFighters(1);
        ShipAPI spawned;
        try {
            spawned = TemporaryCombatSpawn.spawnQuietly(fleetManager, wingMember, launchLocation, launchFacing, (error, settledShip) -> {
                if (settledShip == null) {
                    throw error;
                }
                if (REPORTED_SETTLING_ERROR_TYPES.add(wingSpecId)) {
                    LOG.warn("Wing " + wingSpecId + " threw while settling in after spawning; " + featureName + " launches it anyway", error);
                }
            });
        } finally {
            spec.setNumFighters(normalWingSize);
        }
        if (spawned == null) {
            return null;
        }
        FighterWingAPI spawnedWing = spawned.getWing();
        if (spawnedWing != null) {
            for (ShipAPI spawnedMember : spawnedWing.getWingMembers()) {
                if (spawnedMember != spawned) {
                    spawnedWing.removeMember(spawnedMember);
                    engine.removeEntity(spawnedMember);
                } else if (!keepSpawnedWing) {
                    spawnedWing.removeMember(spawnedMember);
                }
            }
            fleetManager.removeDeployed(spawnedWing, false);
        }
        if (!spawned.isFighter()) {
            TemporaryCombatSpawn.dismiss(engine, fleetManager, spawned, wingMember);
            if (REPORTED_UNLAUNCHABLE_TYPES.add(wingSpecId)) {
                LOG.warn("Wing " + wingSpecId + " spawned a " + spawned.getHullSize() + " instead of a fighter; " + featureName + " won't launch it");
            }
            return null;
        }
        if (spawnedWing != null) {
            DeploymentRecordPruner.forget(fleetManager, wingMember);
        }
        return spawned;
    }

    static void applyCarrierReadiness(ShipAPI fighter, ShipAPI carrier) {
        CombatReadinessPlugin readiness = Global.getSettings().getCRPlugin();
        float carrierReadiness = carrier.getCurrentCR();
        readiness.applyCRToStats(carrierReadiness, fighter.getMutableStats(), fighter.getHullSize());
        readiness.applyCRToShip(carrierReadiness, fighter);
        fighter.setOwner(carrier.getOwner());
    }

    static void applyCarrierBonuses(ShipAPI fighter, ShipAPI carrier, CombatFleetManagerAPI fleetManager) {
        PersonAPI captain = carrier.getCaptain();
        if (captain != null) {
            applyFighterSkillEffects(captain.getStats(), SkillEffectType.SHIP_FIGHTERS, SHIP_FIGHTERS_EFFECT_INFIX, fighter);
        }
        ConvertedHangar.setFighterSkin(fighter, carrier);
        applyCarrierHullMods(fighter, carrier);
        PersonAPI commander = fleetCommanderFor(carrier, fleetManager);
        if (commander != null) {
            applyFighterSkillEffects(commander.getStats(), SkillEffectType.ALL_FIGHTERS_IN_FLEET, ALL_FIGHTERS_EFFECT_INFIX, fighter);
        }
    }

    static void finishLaunch(ShipAPI fighter, ShipAPI carrier, Vector2f launchLocation) {
        fighter.setAnimatedLaunch();
        fighter.setLaunchingShip(carrier);
        fighter.setAlly(carrier.isAlly());
        fighter.getLocation().set(launchLocation);
        fighter.getVelocity().set(carrier.getVelocity());
        ModSounds.playAt(TAKEOFF_SOUND, 1f, carrier.getLocation(), carrier.getVelocity());
    }

    static void applyFighterSkillEffects(MutableCharacterStatsAPI skills, SkillEffectType effectType, String effectIdInfix, ShipAPI fighter) {
        if (skills == null) {
            return;
        }
        for (MutableCharacterStatsAPI.SkillLevelAPI skillLevel : skills.getSkillsCopy()) {
            SkillSpecAPI skill = skillLevel.getSkill();
            List<SkillSpecAPI.SkillEffectSpecAPI> effects = skill.getEffectsAPI();
            for (int index = 0; index < effects.size(); index++) {
                SkillSpecAPI.SkillEffectSpecAPI effect = effects.get(index);
                float governingLevel = skills.getSkillLevel(effect.getGoverningSkill());
                ShipSkillEffect shipEffect = effect.getType() == effectType && governingLevel >= effect.getRequiredSkillLevel()
                        ? effect.getAsShipEffect() : null;
                if (shipEffect != null) {
                    shipEffect.apply(fighter.getMutableStats(), fighter.getHullSize(), skill.getId() + effectIdInfix + index, governingLevel);
                }
            }
        }
    }

    private static PersonAPI fleetCommanderFor(ShipAPI carrier, CombatFleetManagerAPI fleetManager) {
        DeployedFleetMemberAPI deployedCarrier = fleetManager.getDeployedFleetMember(carrier);
        FleetMemberAPI carrierMember = deployedCarrier == null ? null : deployedCarrier.getMember();
        if (carrierMember == null) {
            return null;
        }
        if (carrierMember.getFleetCommanderForStats() != null) {
            return carrierMember.getFleetCommanderForStats();
        }
        if (carrierMember.getFleetData() != null && carrierMember.getFleetData().getCommander() != null) {
            return carrierMember.getFleetData().getCommander();
        }
        return fleetManager.getDefaultCommander();
    }

    private static void applyCarrierHullMods(ShipAPI fighter, ShipAPI carrier) {
        if (carrier.getVariant() == null) {
            return;
        }
        for (String hullModId : carrier.getVariant().getHullMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
            if (spec != null && spec.getEffect() != null) {
                spec.getEffect().applyEffectsToFighterSpawnedByShip(fighter, carrier, hullModId);
            }
        }
    }
}
