package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SplitBeamDronesTest {

    private static final class FakeSplitter implements DamageDealtModifier, DroneSpawner {
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            return null;
        }
    }

    private static ShipAPI shieldedShipAtOrigin(float shieldRadius) {
        ShipAPI ship = mock(ShipAPI.class);
        ShieldAPI shield = mock(ShieldAPI.class);
        when(ship.getShieldCenterEvenIfNoShield()).thenReturn(new Vector2f(0f, 0f));
        when(ship.getShield()).thenReturn(shield);
        when(shield.isOn()).thenReturn(true);
        when(shield.getRadius()).thenReturn(shieldRadius);
        return ship;
    }

    private static ShipAPI liveFiringShip(float timeMult) {
        ShipAPI firingShip = mock(ShipAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(firingShip.isAlive()).thenReturn(true);
        when(firingShip.getMutableStats().getTimeMult().getModifiedValue()).thenReturn(timeMult);
        return firingShip;
    }

    private static ShipAPI liveTarget(Vector2f location) {
        ShipAPI target = mock(ShipAPI.class);
        when(target.isAlive()).thenReturn(true);
        when(target.getLocation()).thenReturn(location);
        return target;
    }

    private static ShipAPI droneWith(WeaponAPI droneWeapon) {
        ShipAPI drone = mock(ShipAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(drone.getLocation()).thenReturn(new Vector2f());
        when(drone.getAllWeapons()).thenReturn(List.of(droneWeapon));
        return drone;
    }

    private static AdvanceableListener selfTickingListenerOn(ShipAPI drone) {
        ArgumentCaptor<Object> added = ArgumentCaptor.forClass(Object.class);
        verify(drone, Mockito.atLeastOnce()).addListener(added.capture());
        return added.getAllValues().stream().filter(AdvanceableListener.class::isInstance)
                .map(AdvanceableListener.class::cast).findFirst().orElseThrow();
    }

    @Test
    void drawsTheVisibleBeamFromThePrimaryImpactPointWhileTheDroneFiresFromBeyondThePrimary() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        ShipAPI firingShip = liveFiringShip(1f);
        ShipAPI primary = shieldedShipAtOrigin(100f);
        ShipAPI splitTarget = liveTarget(new Vector2f(-500f, 0f));
        WeaponAPI droneWeapon = mock(WeaponAPI.class);
        ShipAPI drone = droneWith(droneWeapon);
        BeamAPI droneBeam = mock(BeamAPI.class);
        when(droneWeapon.getBeams()).thenReturn(List.of(droneBeam));
        when(droneBeam.getBrightness()).thenReturn(1f);
        when(droneBeam.getWidth()).thenReturn(12f);
        when(droneBeam.getCoreColor()).thenReturn(java.awt.Color.WHITE);
        when(droneBeam.getFringeColor()).thenReturn(java.awt.Color.CYAN);
        WeaponAPI primaryWeapon = mock(WeaponAPI.class);

        try (MockedStatic<WeaponDroneFactory> factory = Mockito.mockStatic(WeaponDroneFactory.class);
             MockedStatic<MagicFakeBeamPlugin> fakeBeams = Mockito.mockStatic(MagicFakeBeamPlugin.class);
             MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            factory.when(() -> WeaponDroneFactory.create(Mockito.eq(firingShip), Mockito.eq(primaryWeapon), any())).thenReturn(drone);
            global.when(Global::getCombatEngine).thenReturn(engine);
            new SplitBeamDrones(firingShip).refresh(primaryWeapon, primary, splitTarget, new Vector2f(100f, 0f), 0.5f);

            selfTickingListenerOn(drone).advance(0.016f);

            assertEquals(-110f, drone.getLocation().x, 0.01f);
            fakeBeams.verify(() -> MagicFakeBeamPlugin.addBeam(
                    Mockito.eq(0f), Mockito.eq(0f), Mockito.eq(12f), Mockito.eq(new Vector2f(100f, 0f)),
                    Mockito.anyFloat(), Mockito.anyFloat(), any(), any()));
        }
    }

    @Test
    void keepsFiringForAtLeastOneSecondAfterTheSplitStartsEvenWithoutFurtherHits() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        ShipAPI firingShip = liveFiringShip(1f);
        WeaponAPI droneWeapon = mock(WeaponAPI.class);
        ShipAPI drone = droneWith(droneWeapon);
        WeaponAPI primaryWeapon = mock(WeaponAPI.class);

        try (MockedStatic<WeaponDroneFactory> factory = Mockito.mockStatic(WeaponDroneFactory.class);
             MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            factory.when(() -> WeaponDroneFactory.create(Mockito.eq(firingShip), Mockito.eq(primaryWeapon), any())).thenReturn(drone);
            global.when(Global::getCombatEngine).thenReturn(engine);
            new SplitBeamDrones(firingShip).refresh(primaryWeapon, shieldedShipAtOrigin(100f), liveTarget(new Vector2f(500f, 0f)),
                    new Vector2f(100f, 0f), 0.5f);
            AdvanceableListener split = selfTickingListenerOn(drone);

            split.advance(0.8f);
            verify(droneWeapon, Mockito.atLeastOnce()).setForceFireOneFrame(true);
            verify(engine, never()).removeEntity(drone);

            split.advance(0.3f);
            verify(engine).removeEntity(drone);
        }
    }

    private static float shareDealtBy(DamageDealtModifier shareListener) {
        DamageAPI beamHit = mock(DamageAPI.class);
        MutableStat modifier = new MutableStat(1f);
        when(beamHit.getModifier()).thenReturn(modifier);
        shareListener.modifyDamageDealt(mock(BeamAPI.class), mock(CombatEntityAPI.class), beamHit, new Vector2f(), true);
        return modifier.getModifiedValue();
    }

    private static List<DamageDealtModifier> shareListenersCreatedFor(MockedStatic<WeaponDroneFactory> factory, ShipAPI firingShip,
                                                                      WeaponAPI primaryWeapon) {
        ArgumentCaptor<DamageDealtModifier> shareListeners = ArgumentCaptor.forClass(DamageDealtModifier.class);
        factory.verify(() -> WeaponDroneFactory.create(Mockito.eq(firingShip), Mockito.eq(primaryWeapon), shareListeners.capture()),
                Mockito.atLeastOnce());
        return shareListeners.getAllValues();
    }

    @Test
    void aDroneKeptFiringOnlyForItsMinimumWindowDealsNoDamageOnceTheSplitStopsRefreshing() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        ShipAPI firingShip = liveFiringShip(1f);
        WeaponAPI droneWeapon = mock(WeaponAPI.class);
        ShipAPI drone = droneWith(droneWeapon);
        WeaponAPI primaryWeapon = mock(WeaponAPI.class);

        try (MockedStatic<WeaponDroneFactory> factory = Mockito.mockStatic(WeaponDroneFactory.class);
             MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            factory.when(() -> WeaponDroneFactory.create(Mockito.eq(firingShip), Mockito.eq(primaryWeapon), any())).thenReturn(drone);
            global.when(Global::getCombatEngine).thenReturn(engine);
            new SplitBeamDrones(firingShip).refresh(primaryWeapon, shieldedShipAtOrigin(100f), liveTarget(new Vector2f(500f, 0f)),
                    new Vector2f(100f, 0f), 0.5f);
            DamageDealtModifier shareListener = shareListenersCreatedFor(factory, firingShip, primaryWeapon).get(0);
            AdvanceableListener split = selfTickingListenerOn(drone);

            split.advance(0.2f);
            assertEquals(0.5f, shareDealtBy(shareListener), 1e-6f);

            split.advance(0.2f);
            verify(droneWeapon, Mockito.times(2)).setForceFireOneFrame(true);
            assertEquals(0f, shareDealtBy(shareListener), 1e-6f);
        }
    }

    @Test
    void aHitThatNoLongerSplitsToATargetStopsThatDronesDamageAtOnceWhileTheOthersKeepTheirShare() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        ShipAPI firingShip = liveFiringShip(1f);
        ShipAPI firstDrone = droneWith(mock(WeaponAPI.class));
        ShipAPI secondDrone = droneWith(mock(WeaponAPI.class));
        WeaponAPI primaryWeapon = mock(WeaponAPI.class);
        ShipAPI keptTarget = liveTarget(new Vector2f(500f, 0f));
        ShipAPI droppedTarget = liveTarget(new Vector2f(-500f, 0f));

        try (MockedStatic<WeaponDroneFactory> factory = Mockito.mockStatic(WeaponDroneFactory.class);
             MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            factory.when(() -> WeaponDroneFactory.create(Mockito.eq(firingShip), Mockito.eq(primaryWeapon), any()))
                    .thenReturn(firstDrone, secondDrone);
            global.when(Global::getCombatEngine).thenReturn(engine);
            SplitBeamDrones drones = new SplitBeamDrones(firingShip);
            drones.refresh(primaryWeapon, shieldedShipAtOrigin(100f), keptTarget, new Vector2f(100f, 0f), 1f / 3f);
            drones.refresh(primaryWeapon, shieldedShipAtOrigin(100f), droppedTarget, new Vector2f(100f, 0f), 1f / 3f);
            List<DamageDealtModifier> shareListeners = shareListenersCreatedFor(factory, firingShip, primaryWeapon);

            drones.claim(beamOf(mock(WeaponAPI.class)), List.of());
            assertEquals(1f / 3f, shareDealtBy(shareListeners.get(1)), 1e-6f);

            drones.claim(beamOf(primaryWeapon), List.of(keptTarget));

            assertEquals(1f / 3f, shareDealtBy(shareListeners.get(0)), 1e-6f);
            assertEquals(0f, shareDealtBy(shareListeners.get(1)), 1e-6f);
        }
    }

    @Test
    void anotherBeamOfTheSameWeaponMissingDoesNotStopADroneAFreshBeamStillSplitsTo() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        ShipAPI firingShip = liveFiringShip(1f);
        ShipAPI drone = droneWith(mock(WeaponAPI.class));
        WeaponAPI paladin = mock(WeaponAPI.class);
        ShipAPI splitTarget = liveTarget(new Vector2f(500f, 0f));
        BeamAPI firstRay = beamOf(paladin);
        BeamAPI secondRay = beamOf(paladin);

        try (MockedStatic<WeaponDroneFactory> factory = Mockito.mockStatic(WeaponDroneFactory.class);
             MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            factory.when(() -> WeaponDroneFactory.create(Mockito.eq(firingShip), Mockito.eq(paladin), any())).thenReturn(drone);
            global.when(Global::getCombatEngine).thenReturn(engine);
            SplitBeamDrones drones = new SplitBeamDrones(firingShip);
            drones.claim(firstRay, List.of(splitTarget));
            drones.refresh(paladin, shieldedShipAtOrigin(100f), splitTarget, new Vector2f(100f, 0f), 0.5f);
            DamageDealtModifier shareListener = shareListenersCreatedFor(factory, firingShip, paladin).get(0);

            drones.claim(secondRay, List.of());
            assertEquals(0.5f, shareDealtBy(shareListener), 1e-6f);

            when(engine.getTotalElapsedTime(false)).thenReturn(1f);
            drones.claim(secondRay, List.of());
            assertEquals(0f, shareDealtBy(shareListener), 1e-6f);
        }
    }

    private static BeamAPI beamOf(WeaponAPI weapon) {
        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getWeapon()).thenReturn(weapon);
        return beam;
    }

    private interface SplitScenario {
        void run(SplitBeamDrones drones, CombatEngineAPI engine);
    }

    private static void withDrones(ShipAPI firingShip, WeaponAPI primaryWeapon, SplitScenario scenario, ShipAPI... drones) {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        try (MockedStatic<WeaponDroneFactory> factory = Mockito.mockStatic(WeaponDroneFactory.class);
             MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            factory.when(() -> WeaponDroneFactory.create(Mockito.eq(firingShip), Mockito.eq(primaryWeapon), any()))
                    .thenReturn(drones[0], java.util.Arrays.copyOfRange(drones, 1, drones.length));
            global.when(Global::getCombatEngine).thenReturn(engine);
            scenario.run(new SplitBeamDrones(firingShip), engine);
            factory.verify(() -> WeaponDroneFactory.create(Mockito.eq(firingShip), Mockito.eq(primaryWeapon), any()),
                    Mockito.times(drones.length));
        }
    }

    private static void split(SplitBeamDrones drones, WeaponAPI primaryWeapon, ShipAPI splitTarget) {
        drones.refresh(primaryWeapon, shieldedShipAtOrigin(100f), splitTarget, new Vector2f(100f, 0f), 0.5f);
    }

    @Test
    void aDroneStopsAndRemovesItselfOnceItsFiringShipIsGoneEvenThoughThatShipNoLongerTicks() {
        ShipAPI firingShip = liveFiringShip(1f);
        WeaponAPI droneWeapon = mock(WeaponAPI.class);
        ShipAPI drone = droneWith(droneWeapon);
        WeaponAPI primaryWeapon = mock(WeaponAPI.class);

        withDrones(firingShip, primaryWeapon, (drones, engine) -> {
            split(drones, primaryWeapon, liveTarget(new Vector2f(500f, 0f)));
            when(firingShip.isAlive()).thenReturn(false);

            selfTickingListenerOn(drone).advance(0.016f);

            verify(droneWeapon, never()).setForceFireOneFrame(true);
            verify(engine).removeEntity(drone);
        }, drone);
    }

    @Test
    void aDroneCopiesTheFiringShipsMarkerHullModsOnceOnItsFirstTick() {
        ShipAPI firingShip = liveFiringShip(1f);
        ShipAPI drone = droneWith(mock(WeaponAPI.class));
        WeaponAPI primaryWeapon = mock(WeaponAPI.class);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);

        try (MockedStatic<WeaponDroneFactory> factory = Mockito.mockStatic(WeaponDroneFactory.class);
             MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            factory.when(() -> WeaponDroneFactory.create(Mockito.eq(firingShip), Mockito.eq(primaryWeapon), any())).thenReturn(drone);
            global.when(Global::getCombatEngine).thenReturn(engine);
            new SplitBeamDrones(firingShip).refresh(primaryWeapon, shieldedShipAtOrigin(100f), liveTarget(new Vector2f(500f, 0f)),
                    new Vector2f(100f, 0f), 0.5f);
            factory.verify(() -> WeaponDroneFactory.mirrorMarkerHullMods(any(), any()), never());
            AdvanceableListener tick = selfTickingListenerOn(drone);

            tick.advance(0.016f);
            tick.advance(0.016f);

            factory.verify(() -> WeaponDroneFactory.mirrorMarkerHullMods(firingShip, drone), Mockito.times(1));
        }
    }

    @Test
    void aDroneWaitsForItsBeamToFinishFadingBeforeRemovingItself() {
        ShipAPI firingShip = liveFiringShip(1f);
        WeaponAPI droneWeapon = mock(WeaponAPI.class);
        ShipAPI drone = droneWith(droneWeapon);
        WeaponAPI primaryWeapon = mock(WeaponAPI.class);
        when(droneWeapon.isFiring()).thenReturn(true);

        withDrones(firingShip, primaryWeapon, (drones, engine) -> {
            split(drones, primaryWeapon, liveTarget(new Vector2f(500f, 0f)));
            AdvanceableListener tick = selfTickingListenerOn(drone);

            tick.advance(2f);
            verify(engine, never()).removeEntity(drone);
            verify(droneWeapon).setForceFireOneFrame(false);

            when(droneWeapon.isFiring()).thenReturn(false);
            tick.advance(0.016f);
            tick.advance(0.016f);
            verify(engine, Mockito.times(1)).removeEntity(drone);
        }, drone);
    }

    @Test
    void theDroneRunsAtTheFiringShipsTimeRateAndFollowsItsChanges() {
        ShipAPI firingShip = liveFiringShip(1f);
        MutableStat firingTimeMult = new MutableStat(1f);
        firingTimeMult.modifyMult("temporal_shell", 3f);
        when(firingShip.getMutableStats().getTimeMult()).thenReturn(firingTimeMult);
        WeaponAPI droneWeapon = mock(WeaponAPI.class);
        ShipAPI drone = droneWith(droneWeapon);
        MutableStat droneTimeMult = new MutableStat(1f);
        when(drone.getMutableStats().getTimeMult()).thenReturn(droneTimeMult);
        WeaponAPI primaryWeapon = mock(WeaponAPI.class);

        withDrones(firingShip, primaryWeapon, (drones, engine) -> {
            split(drones, primaryWeapon, liveTarget(new Vector2f(500f, 0f)));
            assertEquals(3f, droneTimeMult.getModifiedValue(), 1e-6f);

            firingTimeMult.unmodify("temporal_shell");
            selfTickingListenerOn(drone).advance(0.05f);

            assertEquals(1f, droneTimeMult.getModifiedValue(), 1e-6f);
        }, drone);
    }

    @Test
    void theSplitWindowCountsTheDronesOwnTimeBecauseTheEngineAlreadyRunsItOnTheFiringShipsClock() {
        ShipAPI firingShip = liveFiringShip(0.5f);
        WeaponAPI droneWeapon = mock(WeaponAPI.class);
        ShipAPI drone = droneWith(droneWeapon);
        WeaponAPI primaryWeapon = mock(WeaponAPI.class);

        withDrones(firingShip, primaryWeapon, (drones, engine) -> {
            split(drones, primaryWeapon, liveTarget(new Vector2f(500f, 0f)));

            selfTickingListenerOn(drone).advance(0.75f);

            verify(engine, never()).removeEntity(drone);
            verify(droneWeapon).setForceFireOneFrame(true);
        }, drone);
    }

    @Test
    void aDroneStillInPlayIsReusedForTheSameWeaponAndTarget() {
        ShipAPI firingShip = liveFiringShip(1f);
        ShipAPI drone = droneWith(mock(WeaponAPI.class));
        WeaponAPI primaryWeapon = mock(WeaponAPI.class);
        ShipAPI splitTarget = liveTarget(new Vector2f(500f, 0f));

        withDrones(firingShip, primaryWeapon, (drones, engine) -> {
            when(engine.isEntityInPlay(drone)).thenReturn(true);
            split(drones, primaryWeapon, splitTarget);
            split(drones, primaryWeapon, splitTarget);
        }, drone);
    }

    @Test
    void aDroneThatRemovedItselfIsReplacedByAFreshOneOnTheNextSplit() {
        ShipAPI firingShip = liveFiringShip(1f);
        ShipAPI first = droneWith(mock(WeaponAPI.class));
        ShipAPI second = droneWith(mock(WeaponAPI.class));
        WeaponAPI primaryWeapon = mock(WeaponAPI.class);
        ShipAPI splitTarget = liveTarget(new Vector2f(500f, 0f));

        withDrones(firingShip, primaryWeapon, (drones, engine) -> {
            when(engine.isEntityInPlay(first)).thenReturn(true);
            split(drones, primaryWeapon, splitTarget);
            selfTickingListenerOn(first).advance(2f);
            verify(engine).removeEntity(first);

            split(drones, primaryWeapon, splitTarget);

            selfTickingListenerOn(second);
        }, first, second);
    }

    @Test
    void aDroneRemovedBySomethingElseIsReplacedInsteadOfBlockingThatSplitForTheRestOfTheBattle() {
        ShipAPI firingShip = liveFiringShip(1f);
        ShipAPI first = droneWith(mock(WeaponAPI.class));
        ShipAPI second = droneWith(mock(WeaponAPI.class));
        WeaponAPI primaryWeapon = mock(WeaponAPI.class);
        ShipAPI splitTarget = liveTarget(new Vector2f(500f, 0f));

        withDrones(firingShip, primaryWeapon, (drones, engine) -> {
            split(drones, primaryWeapon, splitTarget);
            when(engine.isEntityInPlay(first)).thenReturn(false);

            split(drones, primaryWeapon, splitTarget);

            selfTickingListenerOn(second);
        }, first, second);
    }

    @Test
    void officerCopyCarriesTheCaptainsIdentityPersonalityAndEverySkillLevel() {
        PersonAPI captain = mock(PersonAPI.class, Answers.RETURNS_DEEP_STUBS);
        FullName name = mock(FullName.class);
        PersonalityAPI personality = mock(PersonalityAPI.class);
        MutableCharacterStatsAPI.SkillLevelAPI skill = mock(MutableCharacterStatsAPI.SkillLevelAPI.class);
        SkillSpecAPI skillSpec = mock(SkillSpecAPI.class);
        when(captain.getName()).thenReturn(name);
        when(captain.getPortraitSprite()).thenReturn("portrait.png");
        when(captain.getPersonalityAPI()).thenReturn(personality);
        when(personality.getId()).thenReturn("aggressive");
        when(captain.getStats().getLevel()).thenReturn(5);
        when(captain.getStats().getSkillsCopy()).thenReturn(List.of(skill));
        when(skill.getSkill()).thenReturn(skillSpec);
        when(skillSpec.getId()).thenReturn("energy_weapon_mastery");
        when(skill.getLevel()).thenReturn(2f);
        FactoryAPI factory = mock(FactoryAPI.class);
        PersonAPI copy = mock(PersonAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(factory.createPerson()).thenReturn(copy);

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getFactory).thenReturn(factory);

            WeaponDroneFactory.officerCopy(captain);
        }

        verify(copy).setName(name);
        verify(copy).setPortraitSprite("portrait.png");
        verify(copy).setPersonality("aggressive");
        verify(copy.getStats()).setLevel(5);
        verify(copy.getStats()).setSkillLevel("energy_weapon_mastery", 2f);
    }

    @Test
    void aCaptainSkillThatFailsOnADroneIsLeftOutAndTheOtherSkillsKept() {
        PersonAPI captain = mock(PersonAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(captain.getPersonalityAPI()).thenReturn(mock(PersonalityAPI.class));
        List<MutableCharacterStatsAPI.SkillLevelAPI> skills = new ArrayList<>();
        for (String skillId : List.of("gunnery_implants", "drone_test_fulldive_officer")) {
            MutableCharacterStatsAPI.SkillLevelAPI skill = mock(MutableCharacterStatsAPI.SkillLevelAPI.class);
            SkillSpecAPI skillSpec = mock(SkillSpecAPI.class);
            when(skillSpec.getId()).thenReturn(skillId);
            when(skill.getSkill()).thenReturn(skillSpec);
            when(skill.getLevel()).thenReturn(1f);
            skills.add(skill);
        }
        when(captain.getStats().getSkillsCopy()).thenReturn(skills);
        Map<PersonAPI, Set<String>> skillsByCopy = new HashMap<>();
        FactoryAPI factory = mock(FactoryAPI.class);
        when(factory.createPerson()).thenAnswer(invocation -> {
            PersonAPI copy = mock(PersonAPI.class, Answers.RETURNS_DEEP_STUBS);
            Set<String> copiedSkills = new HashSet<>();
            skillsByCopy.put(copy, copiedSkills);
            MutableCharacterStatsAPI copyStats = copy.getStats();
            Mockito.doAnswer(set -> copiedSkills.add(set.getArgument(0))).when(copyStats).setSkillLevel(Mockito.anyString(), Mockito.anyFloat());
            return copy;
        });
        ShipAPI drone = mock(ShipAPI.class);
        List<PersonAPI> captainsSet = new ArrayList<>();
        Mockito.doAnswer(set -> {
            PersonAPI officer = set.getArgument(0);
            if (skillsByCopy.get(officer).contains("drone_test_fulldive_officer")) {
                throw new NullPointerException("member is null");
            }
            captainsSet.add(officer);
            return null;
        }).when(drone).setCaptain(Mockito.any());

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getFactory).thenReturn(factory);

            WeaponDroneFactory.assignCaptain(drone, captain);
            WeaponDroneFactory.assignCaptain(drone, captain);
        }

        assertEquals(Set.of("gunnery_implants"), skillsByCopy.get(captainsSet.get(captainsSet.size() - 1)));
        assertEquals(Set.of("gunnery_implants"), skillsByCopy.get(captainsSet.get(captainsSet.size() - 2)));
    }

    @Test
    void sharesTickingDamageListenersThroughAPassThroughSoTheyStillTickOnlyOnTheFiringShip() {
        ShipAPI firingShip = mock(ShipAPI.class);
        ShipAPI drone = mock(ShipAPI.class);
        DamageDealtModifier plain = mock(DamageDealtModifier.class);
        DamageDealtModifier ticking = mock(DamageDealtModifier.class, Mockito.withSettings().extraInterfaces(AdvanceableListener.class));
        when(firingShip.getListeners(DamageDealtModifier.class)).thenReturn(List.of(plain, ticking));

        WeaponDroneFactory.shareDamageListeners(firingShip, drone);

        verify(drone).addListener(plain);
        verify(drone, never()).addListener(ticking);
        ArgumentCaptor<Object> added = ArgumentCaptor.forClass(Object.class);
        verify(drone, Mockito.times(2)).addListener(added.capture());
        Object passThrough = added.getAllValues().get(1);
        assertFalse(passThrough instanceof AdvanceableListener);
        BeamAPI beam = mock(BeamAPI.class);
        CombatEntityAPI target = mock(CombatEntityAPI.class);
        DamageAPI damage = mock(DamageAPI.class);
        when(ticking.modifyDamageDealt(beam, target, damage, new Vector2f(), true)).thenReturn("ewm_dam_mod");

        String result = ((DamageDealtModifier) passThrough).modifyDamageDealt(beam, target, damage, new Vector2f(), true);

        assertEquals("ewm_dam_mod", result);
    }

    @Test
    void theDronesOwnCopyOfATickingListenerIsSwappedForTheFiringShipsSoBonusesReadTheFiringShipsFlux() {
        ShipAPI firingShip = mock(ShipAPI.class);
        ShipAPI drone = mock(ShipAPI.class);
        DamageDealtModifier ticking = mock(DamageDealtModifier.class, Mockito.withSettings().extraInterfaces(AdvanceableListener.class));
        when(firingShip.getListeners(DamageDealtModifier.class)).thenReturn(List.of(ticking));

        WeaponDroneFactory.shareDamageListeners(firingShip, drone);

        org.mockito.InOrder order = Mockito.inOrder(drone);
        order.verify(drone).removeListenerOfClass(ticking.getClass());
        order.verify(drone).addListener(any(WeaponDroneFactory.SharedDamageModifier.class));
    }

    @Test
    void replacesTheOfficerCopysOwnInstanceOfAListenerWithTheFiringShipsSoItReadsTheRealShip() {
        ShipAPI firingShip = mock(ShipAPI.class);
        ShipAPI drone = mock(ShipAPI.class);
        DamageDealtModifier listener = mock(DamageDealtModifier.class);
        when(firingShip.getListeners(DamageDealtModifier.class)).thenReturn(List.of(listener));

        WeaponDroneFactory.shareDamageListeners(firingShip, drone);

        org.mockito.InOrder order = Mockito.inOrder(drone);
        order.verify(drone).removeListenerOfClass(listener.getClass());
        order.verify(drone).addListener(listener);
    }

    @Test
    void neverSharesABeamSplitterSoDronesCannotSplitAgain() {
        ShipAPI firingShip = mock(ShipAPI.class);
        ShipAPI drone = mock(ShipAPI.class);
        DamageDealtModifier splitter = new FakeSplitter();
        when(firingShip.getListeners(DamageDealtModifier.class)).thenReturn(List.of(splitter));

        WeaponDroneFactory.shareDamageListeners(firingShip, drone);

        verify(drone, never()).addListener(any());
    }

    @Test
    void neverSharesTheRefractionListenerSoDroneHitsAreNeverReducedOrRefractedTwice() {
        ShipAPI firingShip = mock(ShipAPI.class);
        ShipAPI drone = mock(ShipAPI.class);
        DamageDealtModifier refraction = new EnergyChainListener(firingShip, mock(RefractionDrones.class));
        when(firingShip.getListeners(DamageDealtModifier.class)).thenReturn(List.of(refraction));

        WeaponDroneFactory.shareDamageListeners(firingShip, drone);

        verify(drone, never()).addListener(any());
    }

    @Test
    void mirroringCarriesProjectileSpeedAndSubsystemDamageSoRefractedShotsMatchTheOriginal() {
        MutableShipStatsAPI source = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        MutableShipStatsAPI drone = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        MutableStat sourceSpeed = new MutableStat(1f);
        MutableStat droneSpeed = new MutableStat(1f);
        MutableStat sourceEngines = new MutableStat(1f);
        MutableStat droneEngines = new MutableStat(1f);
        sourceSpeed.modifyPercent("node", 25f);
        sourceEngines.modifyMult("skill", 1.5f);
        when(source.getEnergyProjectileSpeedMult()).thenReturn(sourceSpeed);
        when(drone.getEnergyProjectileSpeedMult()).thenReturn(droneSpeed);
        when(source.getDamageToTargetEnginesMult()).thenReturn(sourceEngines);
        when(drone.getDamageToTargetEnginesMult()).thenReturn(droneEngines);

        WeaponDroneStats.mirror(source, drone);

        assertEquals(1.25f, droneSpeed.getModifiedValue(), 0.0001f);
        assertEquals(1.5f, droneEngines.getModifiedValue(), 0.0001f);
    }

    @Test
    void mirroringReplacesTheDronesModifiersWithTheFiringShipsCurrentOnes() {
        MutableShipStatsAPI source = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        MutableShipStatsAPI drone = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        MutableStat droneEnergyDamage = new MutableStat(1f);
        MutableStat sourceEnergyDamage = new MutableStat(1f);
        droneEnergyDamage.modifyMult("expired_system", 2f);
        sourceEnergyDamage.modifyMult("high_energy_focus", 1.5f);
        when(drone.getEnergyWeaponDamageMult()).thenReturn(droneEnergyDamage);
        when(source.getEnergyWeaponDamageMult()).thenReturn(sourceEnergyDamage);
        StatBonus droneBeamRange = new StatBonus();
        StatBonus sourceBeamRange = new StatBonus();
        sourceBeamRange.modifyPercent("advanced_optics", 20f);
        when(drone.getBeamWeaponRangeBonus()).thenReturn(droneBeamRange);
        when(source.getBeamWeaponRangeBonus()).thenReturn(sourceBeamRange);

        WeaponDroneStats.mirror(source, drone);

        assertEquals(1.5f, droneEnergyDamage.getModifiedValue(), 0.0001f);
        assertEquals(120f, droneBeamRange.computeEffective(100f), 0.0001f);
    }

    @Test
    void mirroringCarriesTheBeamHardFluxBonusSoSplitBeamsFromDronesKeepIt() {
        MutableShipStatsAPI source = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        MutableShipStatsAPI drone = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        String key = BeamHardFluxListener.HARD_FLUX_PERCENT_KEY;
        StatBonus sourceHardFlux = new StatBonus();
        StatBonus droneHardFlux = new StatBonus();
        sourceHardFlux.modifyFlat("exiledLib_skill_beam_1", 50f);
        droneHardFlux.modifyFlat("stale", 10f);
        when(source.getDynamic().getMod(key)).thenReturn(sourceHardFlux);
        when(drone.getDynamic().getMod(key)).thenReturn(droneHardFlux);

        WeaponDroneStats.mirror(source, drone);

        assertEquals(50f, droneHardFlux.getFlatBonus(), 0.0001f);
    }

    @Test
    void droneShareListenerScalesOnlyBeamHitsAndHandsItsModifierBackToTheEngine() {
        DamageDealtModifier shareListener = new SplitBeamDrones.ShareListener();
        DamageAPI beamHit = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);
        DamageAPI projectileHit = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);

        String beamResult = shareListener.modifyDamageDealt(mock(com.fs.starfarer.api.combat.BeamAPI.class),
                mock(CombatEntityAPI.class), beamHit, new Vector2f(), true);
        String projectileResult = shareListener.modifyDamageDealt(new Object(), mock(CombatEntityAPI.class),
                projectileHit, new Vector2f(), true);

        assertEquals("exiledLib_splitBeamDroneShare", beamResult);
        verify(beamHit.getModifier()).modifyMult("exiledLib_splitBeamDroneShare", 1f);
        assertEquals(null, projectileResult);
    }
}
