package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillEffectType;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatReadinessPlugin;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.CrewCompositionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.hullmods.ConvertedHangar;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WingRole;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class WingScrambleListener extends ShipCombatListener implements AdvanceableListener {

    static final String COPIES_KEY = "exiledSector_wingScrambleCopies";
    static final String INDEPENDENT_FIGHTER_HULL_MOD = "rat_exo_fighter_independence";
    static final List<String> BEHAVIOUR_TAGS = List.of(Tags.AUTOMATED_FIGHTER, Tags.WING_INDEPENDENT_OF_CARRIER, Tags.WING_STAY_IN_FRONT_OF_SHIP,
            Tags.WING_RAPID_REFORM, Tags.WING_LEADER_NO_SWARM, Tags.WING_WINGMEN_NO_SWARM, Tags.WING_MATCH_LEADER_FACING,
            Tags.WING_ATTACK_AT_AN_ANGLE, Tags.WING_KEEP_AT_RANGE, Tags.WING_TARGET_ESCORT_TARGET, Tags.SWARM_FIGHTER);
    static final float MAX_WAIT_FOR_LAUNCH_SECONDS = 4f;
    static final float ROUTING_INTERVAL_SECONDS = 0.25f;
    static final float LAUNCHED_WING_CREW = 100000f;
    static final float MIN_WING_MEMBER_READINESS = 0.01f;
    static final float LAUNCH_JITTER = 10f;
    static final String SHIP_FIGHTERS_EFFECT_INFIX = "_ship_fighters_";
    static final String ALL_FIGHTERS_EFFECT_INFIX = "_all_fighters_";
    static final String TAKEOFF_SOUND = "fighter_takeoff";
    private static final Logger LOG = Logger.getLogger(WingScrambleListener.class);
    private static final Set<String> REPORTED_UNLAUNCHABLE_TYPES = new HashSet<>();

    record DoctrineGroup(WingRole role, List<String> behaviourTags) {
    }

    private final Map<ShipAPI, String> typeByFighter = new IdentityHashMap<>();
    private final Set<String> unlaunchableTypes = new HashSet<>();
    private final Map<String, FleetMemberAPI> temporaryMemberByType = new HashMap<>();
    private List<List<FighterLaunchBayAPI>> bayGroups;
    private float secondsWaited;
    private float secondsSinceRouting;

    WingScrambleListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        if (bayGroups != null && bayGroups.isEmpty() || ownerShip.isFighter() || !ownerIsAliveNotHulk()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        int copiesPerType = Math.round(magnitude(COPIES_KEY));
        if (copiesPerType <= 0) {
            return;
        }
        if (bayGroups == null) {
            List<FighterLaunchBayAPI> bays = ownerShip.getLaunchBaysCopy();
            if (!everyBayHasAWing(bays)) {
                return;
            }
            secondsWaited += amount;
            if (!initialLaunchDone(bays) && secondsWaited < MAX_WAIT_FOR_LAUNCH_SECONDS) {
                return;
            }
            bayGroups = WingScramble.groups(bays, WingScrambleListener::doctrineGroupOf);
            secondsSinceRouting = ROUTING_INTERVAL_SECONDS;
        } else {
            secondsSinceRouting += amount;
        }
        if (secondsSinceRouting >= ROUTING_INTERVAL_SECONDS) {
            secondsSinceRouting = 0f;
            routeAll(engine, copiesPerType);
        }
    }

    static boolean everyBayHasAWing(List<FighterLaunchBayAPI> bays) {
        for (FighterLaunchBayAPI bay : bays) {
            if (bay.getWing() == null) {
                return false;
            }
        }
        return true;
    }

    static boolean initialLaunchDone(List<FighterLaunchBayAPI> bays) {
        for (FighterLaunchBayAPI bay : bays) {
            FighterWingAPI wing = bay.getWing();
            if (wing == null) {
                return false;
            }
            if (wing.getSpec() != null && wing.getWingMembers().size() < wing.getSpec().getNumFighters()) {
                return false;
            }
        }
        return true;
    }

    static DoctrineGroup doctrineGroupOf(FighterLaunchBayAPI bay) {
        FighterWingAPI wing = bay.getWing();
        FighterWingSpecAPI spec = wing == null ? null : wing.getSpec();
        if (spec == null || wing.getSource() != bay || !canBeScrambled(spec)) {
            return null;
        }
        List<String> behaviourTags = BEHAVIOUR_TAGS.stream().filter(spec::hasTag).toList();
        return new DoctrineGroup(FighterSkillEffect.effectiveRole(spec.getRole()), behaviourTags);
    }

    static boolean canBeScrambled(FighterWingSpecAPI spec) {
        ShipVariantAPI variant = spec.getVariant();
        return variant != null && variant.getHullSize() == ShipAPI.HullSize.FIGHTER && !variant.hasHullMod(INDEPENDENT_FIGHTER_HULL_MOD);
    }

    static boolean isPinned(ShipAPI fighter) {
        ShipVariantAPI variant = fighter.getVariant();
        return fighter.isLanding() || fighter.isFinishedLanding() || !fighter.isFighter()
                || variant != null && variant.hasHullMod(INDEPENDENT_FIGHTER_HULL_MOD);
    }

    private void routeAll(CombatEngineAPI engine, int copiesPerType) {
        typeByFighter.keySet().removeIf(fighter -> !fighter.isAlive() || !engine.isEntityInPlay(fighter));
        for (List<FighterLaunchBayAPI> group : bayGroups) {
            List<WingScramble.Host<ShipAPI, FighterLaunchBayAPI, String>> hosts = hostsOf(group);
            if (hosts.size() < 2) {
                continue;
            }
            WingScramble.Routing<ShipAPI, FighterLaunchBayAPI, String> routing = WingScramble.route(hosts, typeByFighter,
                    WingScrambleListener::isPinned, copiesPerType);
            for (WingScramble.Move<ShipAPI, FighterLaunchBayAPI> move : routing.moves()) {
                typeByFighter.put(move.fighter(), fighterTypeOf(move.fighter()));
                rehome(move.fighter(), move.toWing().getWing());
            }
            for (WingScramble.Conversion<ShipAPI, FighterLaunchBayAPI, String> conversion : routing.conversions()) {
                convert(engine, conversion.fighter(), conversion.hostWing(), conversion.fighterType());
            }
            for (ShipAPI surplus : routing.discards()) {
                discard(engine, surplus);
            }
        }
    }

    private static List<WingScramble.Host<ShipAPI, FighterLaunchBayAPI, String>> hostsOf(List<FighterLaunchBayAPI> group) {
        List<WingScramble.Host<ShipAPI, FighterLaunchBayAPI, String>> hosts = new ArrayList<>(group.size());
        for (FighterLaunchBayAPI bay : group) {
            FighterWingAPI wing = bay.getWing();
            if (wing == null || wing.getSource() != bay || wing.getSpec() == null) {
                continue;
            }
            int wingSize = wing.getSpec().getNumFighters();
            hosts.add(new WingScramble.Host<>(bay, typeOf(wing), wingSize, Math.max(0, bay.getExtraDeploymentLimit() - wingSize),
                    livingMembersOf(wing)));
        }
        return hosts;
    }

    static List<ShipAPI> livingMembersOf(FighterWingAPI wing) {
        List<ShipAPI> members = wing.getWingMembers();
        members.removeIf(member -> !member.isAlive());
        return members;
    }

    private String fighterTypeOf(ShipAPI fighter) {
        String fighterType = typeByFighter.get(fighter);
        return fighterType != null ? fighterType : typeOf(fighter.getWing());
    }

    private static String typeOf(FighterWingAPI wing) {
        return wing.getSpec().getId();
    }

    private static void rehome(ShipAPI fighter, FighterWingAPI toWing) {
        FighterWingAPI fromWing = fighter.getWing();
        if (fromWing == toWing) {
            return;
        }
        if (fromWing != null) {
            fromWing.removeMember(fighter);
        }
        fighter.setWing(toWing);
        toWing.addMember(fighter);
        fighter.setShipAI(Global.getSettings().pickShipAIPlugin(null, fighter));
    }

    private void convert(CombatEngineAPI engine, ShipAPI retrained, FighterLaunchBayAPI hostBay, String fighterType) {
        if (unlaunchableTypes.contains(fighterType)) {
            return;
        }
        ShipAPI copy;
        try {
            copy = launchCopy(engine, fighterType, hostBay);
        } catch (RuntimeException e) {
            unlaunchableTypes.add(fighterType);
            LOG.error("Couldn't add a " + fighterType + " to a wing of " + ownerShip.getName(), e);
            return;
        }
        if (copy == null) {
            unlaunchableTypes.add(fighterType);
            return;
        }
        typeByFighter.put(copy, fighterType);
        float retrainedTimeBeforeRefit = retrained.getFighterTimeBeforeRefit();
        if (retrainedTimeBeforeRefit < copy.getFighterTimeBeforeRefit()) {
            copy.setFighterTimeBeforeRefit(retrainedTimeBeforeRefit);
        }
        discard(engine, retrained);
    }

    private void discard(CombatEngineAPI engine, ShipAPI fighter) {
        typeByFighter.remove(fighter);
        if (fighter.getWing() != null) {
            fighter.getWing().removeMember(fighter);
        }
        engine.removeEntity(fighter);
    }

    ShipAPI launchCopy(CombatEngineAPI engine, String wingSpecId, FighterLaunchBayAPI hostBay) {
        CombatFleetManagerAPI fleetManager = engine.getFleetManager(ownerShip.getOwner());
        FleetMemberAPI wingMember = temporaryMemberFor(fleetManager, wingSpecId);
        wingMember.setOwner(ownerShip.getOwner());
        wingMember.setAlly(ownerShip.isAlly());
        CrewCompositionAPI crew = wingMember.getCrewComposition();
        if (crew != null) {
            crew.setCrew(LAUNCHED_WING_CREW);
        }
        wingMember.getRepairTracker().setCR(Math.max(MIN_WING_MEMBER_READINESS, ownerShip.getCRAtDeployment()));
        WeaponSlotAPI baySlot = hostBay.getWeaponSlot();
        Vector2f launchLocation = baySlot == null ? new Vector2f(ownerShip.getLocation()) : baySlot.computePosition(ownerShip);
        launchLocation.translate(jitter(), jitter());
        float launchFacing = (baySlot == null ? ownerShip.getFacing() : baySlot.computeMidArcAngle(ownerShip)) + jitter();
        ShipAPI fighter = spawnLoneFighter(engine, fleetManager, wingMember, wingSpecId, launchLocation, launchFacing);
        if (fighter != null) {
            prepareLikeABayLaunch(fighter, hostBay.getWing(), fleetManager, launchLocation, launchFacing);
        }
        return fighter;
    }

    private FleetMemberAPI temporaryMemberFor(CombatFleetManagerAPI fleetManager, String wingSpecId) {
        FleetMemberAPI cachedMember = temporaryMemberByType.get(wingSpecId);
        if (cachedMember != null && fleetManager.getShipFor(cachedMember) == null) {
            return cachedMember;
        }
        FleetMemberAPI wingMember = Global.getFactory().createFleetMember(FleetMemberType.FIGHTER_WING, wingSpecId);
        temporaryMemberByType.put(wingSpecId, wingMember);
        return wingMember;
    }

    private static float jitter() {
        return ((float) Math.random() - 0.5f) * 2f * LAUNCH_JITTER;
    }

    private static ShipAPI spawnLoneFighter(CombatEngineAPI engine, CombatFleetManagerAPI fleetManager, FleetMemberAPI wingMember, String wingSpecId,
                                            Vector2f launchLocation, float launchFacing) {
        FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(wingSpecId);
        int normalWingSize = spec.getNumFighters();
        boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
        fleetManager.setSuppressDeploymentMessages(true);
        spec.setNumFighters(1);
        ShipAPI spawned;
        try {
            spawned = fleetManager.spawnFleetMember(wingMember, new Vector2f(launchLocation), launchFacing, 0f);
        } finally {
            spec.setNumFighters(normalWingSize);
            fleetManager.setSuppressDeploymentMessages(wasSuppressed);
        }
        if (spawned == null) {
            return null;
        }
        FighterWingAPI spawnedWing = spawned.getWing();
        if (spawnedWing != null) {
            for (ShipAPI spawnedMember : spawnedWing.getWingMembers()) {
                spawnedWing.removeMember(spawnedMember);
                if (spawnedMember != spawned) {
                    engine.removeEntity(spawnedMember);
                }
            }
            fleetManager.removeDeployed(spawnedWing, false);
        }
        if (!spawned.isFighter()) {
            fleetManager.removeDeployed(spawned, false);
            engine.removeEntity(spawned);
            DeploymentRecordPruner.forget(fleetManager, wingMember);
            if (REPORTED_UNLAUNCHABLE_TYPES.add(wingSpecId)) {
                LOG.warn("Wing " + wingSpecId + " spawned a " + spawned.getHullSize() + " instead of a fighter; Coalition Flight Doctrine won't copy it");
            }
            return null;
        }
        if (spawnedWing != null) {
            DeploymentRecordPruner.forget(fleetManager, wingMember);
        }
        return spawned;
    }

    private void prepareLikeABayLaunch(ShipAPI fighter, FighterWingAPI hostWing, CombatFleetManagerAPI fleetManager, Vector2f launchLocation,
                                       float launchFacing) {
        CombatReadinessPlugin readiness = Global.getSettings().getCRPlugin();
        float carrierReadiness = ownerShip.getCurrentCR();
        readiness.applyCRToStats(carrierReadiness, fighter.getMutableStats(), fighter.getHullSize());
        readiness.applyCRToShip(carrierReadiness, fighter);
        fighter.setOwner(ownerShip.getOwner());
        fighter.setFacing(launchFacing);
        fighter.setWing(hostWing);
        fighter.setShipAI(Global.getSettings().pickShipAIPlugin(null, fighter));
        hostWing.addMember(fighter);
        PersonAPI captain = ownerShip.getCaptain();
        if (captain != null) {
            applyFighterSkillEffects(captain.getStats(), SkillEffectType.SHIP_FIGHTERS, SHIP_FIGHTERS_EFFECT_INFIX, fighter);
        }
        ConvertedHangar.setFighterSkin(fighter, ownerShip);
        applyCarrierHullMods(fighter);
        PersonAPI commander = fleetCommanderFor(fleetManager);
        if (commander != null) {
            applyFighterSkillEffects(commander.getStats(), SkillEffectType.ALL_FIGHTERS_IN_FLEET, ALL_FIGHTERS_EFFECT_INFIX, fighter);
        }
        fighter.setAnimatedLaunch();
        fighter.setLaunchingShip(ownerShip);
        fighter.setAlly(ownerShip.isAlly());
        fighter.getLocation().set(launchLocation);
        fighter.getVelocity().set(ownerShip.getVelocity());
        SoundPlayerAPI sounds = Global.getSoundPlayer();
        if (sounds != null) {
            sounds.playSound(TAKEOFF_SOUND, 1f, 1f, ownerShip.getLocation(), ownerShip.getVelocity());
        }
    }

    private PersonAPI fleetCommanderFor(CombatFleetManagerAPI fleetManager) {
        DeployedFleetMemberAPI deployedCarrier = fleetManager.getDeployedFleetMember(ownerShip);
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

    private void applyCarrierHullMods(ShipAPI fighter) {
        if (ownerShip.getVariant() == null) {
            return;
        }
        for (String hullModId : ownerShip.getVariant().getHullMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
            if (spec != null && spec.getEffect() != null) {
                spec.getEffect().applyEffectsToFighterSpawnedByShip(fighter, ownerShip, hullModId);
            }
        }
    }
}
