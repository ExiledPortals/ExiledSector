package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WingRole;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class WingScrambleListener extends ShipCombatListener implements AdvanceableListener {

    static final String COPIES_KEY = "exiledSector_wingScrambleCopies";
    static final String RANGE_MOD_ID = "exiledSector_wingScrambleRange";
    static final String INDEPENDENT_FIGHTER_HULL_MOD = "rat_exo_fighter_independence";
    static final List<String> BEHAVIOUR_TAGS = List.of(Tags.WING_INDEPENDENT_OF_CARRIER, Tags.WING_STAY_IN_FRONT_OF_SHIP,
            Tags.WING_RAPID_REFORM, Tags.WING_LEADER_NO_SWARM, Tags.WING_WINGMEN_NO_SWARM, Tags.WING_MATCH_LEADER_FACING,
            Tags.WING_ATTACK_AT_AN_ANGLE, Tags.WING_KEEP_AT_RANGE, Tags.WING_TARGET_ESCORT_TARGET, Tags.SWARM_FIGHTER);
    static final float MAX_WAIT_FOR_LAUNCH_SECONDS = 4f;
    static final float LAUNCH_JITTER = 10f;
    private static final String FEATURE_NAME = "Coalition Flight Doctrine";
    private static final Logger LOG = Logger.getLogger(WingScrambleListener.class);

    record DoctrineGroup(WingRole role, List<String> behaviourTags) {
    }

    private final Map<ShipAPI, String> typeByFighter = new IdentityHashMap<>();
    private final Set<String> unlaunchableTypes = new HashSet<>();
    private final Map<String, FleetMemberAPI> temporaryMemberByType = new HashMap<>();
    private final Set<ShipAPI> speedMatchedFighters = Collections.newSetFromMap(new IdentityHashMap<>());
    private List<List<FighterLaunchBayAPI>> bayGroups;
    private List<Float> groupBaseSpeeds;
    private float secondsWaited;
    private boolean routingNeeded;

    WingScrambleListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        if (bayGroups != null && (bayGroups.isEmpty() || !routingNeeded) || ownerShip.isFighter() || !ownerIsAliveNotHulk()) {
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
            groupBaseSpeeds = bayGroups.stream().map(WingScrambleListener::averageBaseSpeedOf).toList();
            Float longestBaseRange = bayGroups.isEmpty() ? null : longestBaseRangeOf(bays);
            if (longestBaseRange != null) {
                matchRangeTo(ownerShip.getMutableStats().getFighterWingRange(), longestBaseRange);
            }
        }
        routingNeeded = false;
        routeAll(engine, copiesPerType);
    }

    void fighterLaunched() {
        routingNeeded = true;
    }

    static Float longestBaseRangeOf(List<FighterLaunchBayAPI> bays) {
        float longest = Float.NEGATIVE_INFINITY;
        float shortest = Float.POSITIVE_INFINITY;
        for (FighterLaunchBayAPI bay : bays) {
            FighterWingSpecAPI spec = bay.getWing() == null ? null : bay.getWing().getSpec();
            if (spec != null) {
                longest = Math.max(longest, spec.getRange());
                shortest = Math.min(shortest, spec.getRange());
            }
        }
        return longest > shortest ? longest : null;
    }

    static void matchRangeTo(StatBonus wingRange, float baseRange) {
        float otherPercent = wingRange.getPercentMod();
        wingRange.modifyPercent(RANGE_MOD_ID, -100f - otherPercent);
        wingRange.modifyFlat(RANGE_MOD_ID, baseRange * (1f + otherPercent / 100f));
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

    static Float averageBaseSpeedOf(List<FighterLaunchBayAPI> group) {
        float speedTotal = 0f;
        int fighterCount = 0;
        for (FighterLaunchBayAPI bay : group) {
            FighterWingSpecAPI spec = bay.getWing() == null ? null : bay.getWing().getSpec();
            ShipVariantAPI variant = spec == null ? null : spec.getVariant();
            if (variant != null && variant.getHullSpec() != null && variant.getHullSpec().getEngineSpec() != null && spec.getNumFighters() > 0) {
                speedTotal += variant.getHullSpec().getEngineSpec().getMaxSpeed() * spec.getNumFighters();
                fighterCount += spec.getNumFighters();
            }
        }
        return fighterCount > 0 ? speedTotal / fighterCount : null;
    }

    static void matchSpeeds(List<WingScramble.Host<ShipAPI, FighterLaunchBayAPI, String>> hosts, Float baseSpeed, Set<ShipAPI> matchedFighters) {
        if (baseSpeed == null) {
            return;
        }
        for (WingScramble.Host<ShipAPI, FighterLaunchBayAPI, String> host : hosts) {
            for (ShipAPI fighter : host.members()) {
                if (matchedFighters.add(fighter)) {
                    fighter.getMutableStats().getMaxSpeed().setBaseValue(baseSpeed);
                }
            }
        }
    }

    private void routeAll(CombatEngineAPI engine, int copiesPerType) {
        typeByFighter.keySet().removeIf(fighter -> !fighter.isAlive() || !engine.isEntityInPlay(fighter));
        speedMatchedFighters.removeIf(fighter -> !fighter.isAlive() || !engine.isEntityInPlay(fighter));
        for (int groupIndex = 0; groupIndex < bayGroups.size(); groupIndex++) {
            List<WingScramble.Host<ShipAPI, FighterLaunchBayAPI, String>> hosts = hostsOf(bayGroups.get(groupIndex));
            matchSpeeds(hosts, groupBaseSpeeds.get(groupIndex), speedMatchedFighters);
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
        CarrierLaunch.readyTemporaryWingMember(wingMember, ownerShip);
        WeaponSlotAPI baySlot = hostBay.getWeaponSlot();
        Vector2f launchLocation = baySlot == null ? new Vector2f(ownerShip.getLocation()) : baySlot.computePosition(ownerShip);
        launchLocation.translate(jitter(), jitter());
        float launchFacing = (baySlot == null ? ownerShip.getFacing() : baySlot.computeMidArcAngle(ownerShip)) + jitter();
        ShipAPI fighter = CarrierLaunch.spawnLoneFighter(engine, fleetManager, wingMember, wingSpecId, launchLocation, launchFacing, false,
                FEATURE_NAME);
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

    private void prepareLikeABayLaunch(ShipAPI fighter, FighterWingAPI hostWing, CombatFleetManagerAPI fleetManager, Vector2f launchLocation,
                                       float launchFacing) {
        CarrierLaunch.applyCarrierReadiness(fighter, ownerShip);
        fighter.setFacing(launchFacing);
        fighter.setWing(hostWing);
        fighter.setShipAI(Global.getSettings().pickShipAIPlugin(null, fighter));
        hostWing.addMember(fighter);
        CarrierLaunch.applyCarrierBonuses(fighter, ownerShip, fleetManager);
        CarrierLaunch.finishLaunch(fighter, ownerShip, launchLocation);
    }
}
