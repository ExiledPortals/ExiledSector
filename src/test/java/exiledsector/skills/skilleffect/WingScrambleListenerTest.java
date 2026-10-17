package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillEffectType;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatReadinessPlugin;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.CrewCompositionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WingRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WingScrambleListenerTest {

    private static final String GUN_PLATFORM = "nsp_gun_platform";
    private static final int GUN_PLATFORM_WING_SIZE = 3;

    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;
    private FactoryAPI factory;
    private CombatEngineAPI engine;
    private CombatFleetManagerAPI fleetManager;
    private CombatReadinessPlugin readiness;
    private FighterWingSpecAPI gunPlatformSpec;
    private FleetMemberAPI wingMember;
    private ShipAPI carrier;
    private FighterLaunchBayAPI hostBay;
    private FighterWingAPI hostWing;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        factory = mock(FactoryAPI.class);
        readiness = mock(CombatReadinessPlugin.class);
        when(settings.getCRPlugin()).thenReturn(readiness);
        gunPlatformSpec = mock(FighterWingSpecAPI.class);
        when(gunPlatformSpec.getNumFighters()).thenReturn(GUN_PLATFORM_WING_SIZE);
        when(settings.getFighterWingSpec(GUN_PLATFORM)).thenReturn(gunPlatformSpec);
        wingMember = mock(FleetMemberAPI.class);
        when(wingMember.getCrewComposition()).thenReturn(mock(CrewCompositionAPI.class));
        when(wingMember.getRepairTracker()).thenReturn(mock(RepairTrackerAPI.class));
        when(factory.createFleetMember(FleetMemberType.FIGHTER_WING, GUN_PLATFORM)).thenReturn(wingMember);
        engine = mock(CombatEngineAPI.class);
        fleetManager = mock(CombatFleetManagerAPI.class);
        when(engine.getFleetManager(0)).thenReturn(fleetManager);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getFactory).thenReturn(factory);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
        carrier = mock(ShipAPI.class);
        when(carrier.getOwner()).thenReturn(0);
        when(carrier.getCurrentCR()).thenReturn(0.6f);
        when(carrier.getCRAtDeployment()).thenReturn(0.7f);
        when(carrier.getLocation()).thenReturn(new Vector2f(100f, 200f));
        when(carrier.getVelocity()).thenReturn(new Vector2f(5f, 0f));
        when(carrier.getHullStyleId()).thenReturn("MIDLINE");
        hostWing = mock(FighterWingAPI.class);
        hostBay = mock(FighterLaunchBayAPI.class);
        when(hostBay.getWing()).thenReturn(hostWing);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private ShipAPI fighter() {
        ShipAPI fighter = mock(ShipAPI.class);
        when(fighter.isFighter()).thenReturn(true);
        when(fighter.getLocation()).thenReturn(new Vector2f());
        when(fighter.getVelocity()).thenReturn(new Vector2f());
        when(fighter.getHullStyleId()).thenReturn("MIDLINE");
        return fighter;
    }

    private FighterWingAPI spawnedWingOf(ShipAPI... members) {
        FighterWingAPI spawnedWing = mock(FighterWingAPI.class);
        when(spawnedWing.getWingMembers()).thenReturn(new ArrayList<>(List.of(members)));
        for (ShipAPI member : members) {
            when(member.getWing()).thenReturn(spawnedWing);
        }
        return spawnedWing;
    }

    @Test
    void aWingWhoseIdDoesNotEndInWingIsSpawnedAsAFighterWingMemberAndOnlyOneFighterJoinsTheHost() {
        ShipAPI leader = fighter();
        ShipAPI spare = fighter();
        FighterWingAPI spawnedWing = spawnedWingOf(leader, spare);
        when(fleetManager.spawnFleetMember(eq(wingMember), any(Vector2f.class), anyFloat(), eq(0f))).thenReturn(leader);

        ShipAPI copy = new WingScrambleListener(carrier).launchCopy(engine, GUN_PLATFORM, hostBay);

        assertSame(leader, copy);
        verify(factory).createFleetMember(FleetMemberType.FIGHTER_WING, GUN_PLATFORM);
        verify(fleetManager, never()).spawnShipOrWing(anyString(), any(Vector2f.class), anyFloat());
        verify(fleetManager, never()).spawnShipOrWing(anyString(), any(Vector2f.class), anyFloat(), anyFloat());
        verify(wingMember).setOwner(0);
        verify(wingMember.getCrewComposition()).setCrew(WingScrambleListener.LAUNCHED_WING_CREW);
        verify(wingMember.getRepairTracker()).setCR(0.7f);
        InOrder specSize = inOrder(gunPlatformSpec, fleetManager);
        specSize.verify(gunPlatformSpec).setNumFighters(1);
        specSize.verify(fleetManager).spawnFleetMember(eq(wingMember), any(Vector2f.class), anyFloat(), eq(0f));
        specSize.verify(gunPlatformSpec).setNumFighters(GUN_PLATFORM_WING_SIZE);
        verify(spawnedWing).removeMember(spare);
        verify(engine).removeEntity(spare);
        verify(engine, never()).removeEntity(leader);
        verify(fleetManager).removeDeployed(spawnedWing, false);
        verify(leader).setWing(hostWing);
        verify(hostWing).addMember(leader);
        verify(readiness).applyCRToStats(eq(0.6f), any(), any());
        verify(readiness).applyCRToShip(0.6f, leader);
        verify(leader).setLaunchingShip(carrier);
        assertTrue(Vector2f.sub(leader.getLocation(), new Vector2f(100f, 200f), null).length() <= WingScrambleListener.LAUNCH_JITTER * 1.5f);
        assertEquals(new Vector2f(5f, 0f), leader.getVelocity());
    }

    @Test
    void aSpawnThatIsNotAFighterIsRemovedAndNeverJoinsTheWing() {
        ShipAPI freighter = mock(ShipAPI.class);
        when(freighter.isFighter()).thenReturn(false);
        when(freighter.getHullSize()).thenReturn(ShipAPI.HullSize.FRIGATE);
        when(fleetManager.spawnFleetMember(eq(wingMember), any(Vector2f.class), anyFloat(), eq(0f))).thenReturn(freighter);

        ShipAPI copy = new WingScrambleListener(carrier).launchCopy(engine, GUN_PLATFORM, hostBay);

        assertNull(copy);
        verify(hostWing, never()).addMember(any());
        verify(freighter, never()).setWing(any());
        verify(fleetManager).removeDeployed(freighter, false);
        verify(engine).removeEntity(freighter);
        verify(gunPlatformSpec).setNumFighters(GUN_PLATFORM_WING_SIZE);
    }

    @Test
    void theWingSizeIsRestoredEvenWhenTheSpawnThrows() {
        when(fleetManager.spawnFleetMember(eq(wingMember), any(Vector2f.class), anyFloat(), eq(0f))).thenThrow(new IllegalStateException("broken"));
        WingScrambleListener listener = new WingScrambleListener(carrier);

        assertThrows(IllegalStateException.class, () -> listener.launchCopy(engine, GUN_PLATFORM, hostBay));

        verify(gunPlatformSpec).setNumFighters(GUN_PLATFORM_WING_SIZE);
        verify(fleetManager).setSuppressDeploymentMessages(false);
    }

    @Test
    void launchSkillsUseTheEngineEffectIdsAndOnlyTheRequestedScope() {
        ShipSkillEffect shipFightersEffect = mock(ShipSkillEffect.class);
        ShipSkillEffect shipOnlyEffect = mock(ShipSkillEffect.class);
        SkillSpecAPI.SkillEffectSpecAPI shipOnly = effect(SkillEffectType.SHIP, shipOnlyEffect, 1f);
        SkillSpecAPI.SkillEffectSpecAPI shipFighters = effect(SkillEffectType.SHIP_FIGHTERS, shipFightersEffect, 1f);
        SkillSpecAPI skill = mock(SkillSpecAPI.class);
        when(skill.getId()).thenReturn("carrier_group");
        when(skill.getEffectsAPI()).thenReturn(List.of(shipOnly, shipFighters));
        MutableCharacterStatsAPI.SkillLevelAPI skillLevel = mock(MutableCharacterStatsAPI.SkillLevelAPI.class);
        when(skillLevel.getSkill()).thenReturn(skill);
        MutableCharacterStatsAPI captainSkills = mock(MutableCharacterStatsAPI.class);
        when(captainSkills.getSkillsCopy()).thenReturn(List.of(skillLevel));
        when(captainSkills.getSkillLevel("carrier_group")).thenReturn(2f);
        ShipAPI fighter = fighter();
        MutableShipStatsAPI fighterStats = mock(MutableShipStatsAPI.class);
        when(fighter.getMutableStats()).thenReturn(fighterStats);
        when(fighter.getHullSize()).thenReturn(ShipAPI.HullSize.FIGHTER);

        WingScrambleListener.applyFighterSkillEffects(captainSkills, SkillEffectType.SHIP_FIGHTERS, WingScrambleListener.SHIP_FIGHTERS_EFFECT_INFIX,
                fighter);

        verify(shipFightersEffect).apply(fighterStats, ShipAPI.HullSize.FIGHTER, "carrier_group_ship_fighters_1", 2f);
        verify(shipOnlyEffect, never()).apply(any(), any(), anyString(), anyFloat());
    }

    private static SkillSpecAPI.SkillEffectSpecAPI effect(SkillEffectType type, ShipSkillEffect shipEffect, float requiredLevel) {
        SkillSpecAPI.SkillEffectSpecAPI effect = mock(SkillSpecAPI.SkillEffectSpecAPI.class);
        when(effect.getType()).thenReturn(type);
        when(effect.getAsShipEffect()).thenReturn(shipEffect);
        when(effect.getGoverningSkill()).thenReturn("carrier_group");
        when(effect.getRequiredSkillLevel()).thenReturn(requiredLevel);
        return effect;
    }

    private static FighterLaunchBayAPI bayWith(WingRole role, Set<String> tags, boolean independentFighters, boolean ownSource) {
        FighterLaunchBayAPI bay = mock(FighterLaunchBayAPI.class);
        FighterWingAPI wing = mock(FighterWingAPI.class);
        FighterWingSpecAPI spec = mock(FighterWingSpecAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullSize()).thenReturn(ShipAPI.HullSize.FIGHTER);
        when(variant.hasHullMod(WingScrambleListener.INDEPENDENT_FIGHTER_HULL_MOD)).thenReturn(independentFighters);
        when(spec.getVariant()).thenReturn(variant);
        when(spec.getRole()).thenReturn(role);
        when(spec.hasTag(anyString())).thenAnswer(invocation -> tags.contains(invocation.<String>getArgument(0)));
        when(wing.getSpec()).thenReturn(spec);
        when(wing.getSource()).thenReturn(ownSource ? bay : null);
        when(bay.getWing()).thenReturn(wing);
        return bay;
    }

    @Test
    void assaultWingsGroupWithFightersButDronesAndIndependentWingsStayApart() {
        Object fighters = WingScrambleListener.doctrineGroupOf(bayWith(WingRole.FIGHTER, Set.of(), false, true));
        Object assault = WingScrambleListener.doctrineGroupOf(bayWith(WingRole.ASSAULT, Set.of(), false, true));
        Object drones = WingScrambleListener.doctrineGroupOf(bayWith(WingRole.FIGHTER, Set.of(Tags.AUTOMATED_FIGHTER), false, true));
        Object independent = WingScrambleListener.doctrineGroupOf(bayWith(WingRole.FIGHTER, Set.of(Tags.WING_INDEPENDENT_OF_CARRIER), false, true));
        Object escorts = WingScrambleListener.doctrineGroupOf(bayWith(WingRole.FIGHTER, Set.of(Tags.WING_STAY_IN_FRONT_OF_SHIP), false, true));
        Object bombers = WingScrambleListener.doctrineGroupOf(bayWith(WingRole.BOMBER, Set.of(), false, true));

        assertEquals(fighters, assault);
        assertNotEquals(fighters, drones);
        assertNotEquals(fighters, independent);
        assertNotEquals(fighters, escorts);
        assertNotEquals(fighters, bombers);
    }

    @Test
    void independentFightersAndWingsWithoutTheirBayAreNeverScrambled() {
        assertNull(WingScrambleListener.doctrineGroupOf(bayWith(WingRole.FIGHTER, Set.of(), true, true)));
        assertNull(WingScrambleListener.doctrineGroupOf(bayWith(WingRole.FIGHTER, Set.of(), false, false)));
    }

    @Test
    void wingsWhoseFighterIsNotFighterSizedAreNeverScrambled() {
        FighterLaunchBayAPI bay = bayWith(WingRole.FIGHTER, Set.of(), false, true);
        when(bay.getWing().getSpec().getVariant().getHullSize()).thenReturn(ShipAPI.HullSize.FRIGATE);

        assertNull(WingScrambleListener.doctrineGroupOf(bay));
    }

    @Test
    void independentFightersAndDockedFightersArePinned() {
        ShipAPI independent = fighter();
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasHullMod(WingScrambleListener.INDEPENDENT_FIGHTER_HULL_MOD)).thenReturn(true);
        when(independent.getVariant()).thenReturn(variant);
        ShipAPI landing = fighter();
        when(landing.isLanding()).thenReturn(true);

        assertTrue(WingScrambleListener.isPinned(independent));
        assertTrue(WingScrambleListener.isPinned(landing));
        assertFalse(WingScrambleListener.isPinned(fighter()));
    }

    @Test
    void theFirstScrambleWaitsOnlyForBaysThatExistAndTheirWingsToFillUp() {
        FighterLaunchBayAPI full = bayWith(WingRole.FIGHTER, Set.of(), false, true);
        when(full.getWing().getSpec().getNumFighters()).thenReturn(2);
        List<ShipAPI> fullMembers = List.of(fighter(), fighter());
        when(full.getWing().getWingMembers()).thenReturn(fullMembers);
        FighterLaunchBayAPI filling = bayWith(WingRole.FIGHTER, Set.of(), false, true);
        when(filling.getWing().getSpec().getNumFighters()).thenReturn(2);
        List<ShipAPI> fillingMembers = List.of(fighter());
        when(filling.getWing().getWingMembers()).thenReturn(fillingMembers);
        FighterLaunchBayAPI notLaunched = mock(FighterLaunchBayAPI.class);

        assertTrue(WingScrambleListener.initialLaunchDone(List.of(full)));
        assertFalse(WingScrambleListener.initialLaunchDone(List.of(full, filling)));
        assertFalse(WingScrambleListener.initialLaunchDone(List.of(full, notLaunched)));
        assertTrue(WingScrambleListener.everyBayHasAWing(List.of(full, filling)));
        assertFalse(WingScrambleListener.everyBayHasAWing(List.of(full, notLaunched)), "bays can't launch during the deployment burn, so the timeout must not start yet");
    }

    @Test
    void fightersKilledSinceTheEngineLastPrunedTheWingAreNotRouted() {
        ShipAPI living = fighter();
        when(living.isAlive()).thenReturn(true);
        ShipAPI killed = fighter();
        FighterWingAPI wing = spawnedWingOf(living, killed);

        assertEquals(List.of(living), WingScrambleListener.livingMembersOf(wing));
    }
}
