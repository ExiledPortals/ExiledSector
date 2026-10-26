package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.abilities.FractureJumpAbility;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.loading.AbilitySpecAPI;
import exiledlib.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartTransverseJumpTest {

    private static final String RUN_ID = "abc";
    private static final float FUEL_COST = 7f;

    private MockSector mockSector;
    private SectorAPI sector;
    private CampaignFleetAPI playerFleet;
    private StarSystemAPI chartSystem;
    private LocationAPI hyperspace;
    private CargoAPI cargo;
    private RepairTrackerAPI repairs;
    private final Map<String, AbilityPlugin> abilities = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        sector = mockSector.sector;
        playerFleet = mockSector.playerFleet();
        cargo = mockSector.cargo();
        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        when(settings.getAbilitySpec(Abilities.TRANSVERSE_JUMP)).thenReturn(mock(AbilitySpecAPI.class));
        chartSystem = mock(StarSystemAPI.class);
        hyperspace = mock(LocationAPI.class);
        when(chartSystem.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(true);
        when(sector.getStarSystem(ChartIds.systemId(RUN_ID))).thenReturn(chartSystem);
        when(sector.getStarSystems()).thenReturn(List.of(chartSystem));
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.getCampaignUI()).thenReturn(mock(CampaignUIAPI.class));
        when(playerFleet.getAbilities()).thenReturn(abilities);
        when(playerFleet.getAbility(anyString())).thenAnswer(call -> abilities.get(call.<String>getArgument(0)));
        when(playerFleet.getMemoryWithoutUpdate()).thenReturn(mock(MemoryAPI.class));
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        repairs = mock(RepairTrackerAPI.class);
        when(member.getDeployCost()).thenReturn(0.5f);
        when(member.getRepairTracker()).thenReturn(repairs);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));
        when(playerFleet.getFleetData()).thenReturn(fleetData);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private ChartRun runWithOpenOuterWormhole() {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 2);
        run.recordArrival();
        ChartRuns.store(run);
        CustomCampaignEntityAPI outer = mock(CustomCampaignEntityAPI.class);
        when(sector.getEntityById(run.outerWormholeId())).thenReturn(outer);
        when(outer.getContainingLocation()).thenReturn(hyperspace);
        when(outer.getLocation()).thenReturn(new Vector2f(100f, 100f));
        SectorEntityToken arrival = mock(SectorEntityToken.class);
        when(arrival.getContainingLocation()).thenReturn(hyperspace);
        when(hyperspace.createToken(100f + ChartSystemFactory.ENTRY_OFFSET, 100f)).thenReturn(arrival);
        return run;
    }

    private ChartTransverseJumpAbility primedJump(float crCostMult) {
        ChartTransverseJumpAbility jump = new ChartTransverseJumpAbility() {
            {
                primed = true;
            }

            @Override
            protected float getCRCostMult(CampaignFleetAPI fleet) {
                return crCostMult;
            }

            @Override
            protected List<FleetMemberAPI> getNonReadyShips() {
                return List.of();
            }

            @Override
            protected float computeFuelCost() {
                return FUEL_COST;
            }
        };
        jump.init(Abilities.TRANSVERSE_JUMP, playerFleet);
        return jump;
    }

    private FractureJumpAbility vanillaJump(float cooldownLeft) {
        FractureJumpAbility vanilla = new FractureJumpAbility();
        vanilla.init(Abilities.TRANSVERSE_JUMP, playerFleet);
        vanilla.setCooldownLeft(cooldownLeft);
        abilities.put(Abilities.TRANSVERSE_JUMP, vanilla);
        return vanilla;
    }

    @Test
    void jumpingInAChartSystemReturnsThroughTheRunAndChargesVanillasCosts() {
        ChartRun run = runWithOpenOuterWormhole();
        when(playerFleet.getContainingLocation()).thenReturn(chartSystem);

        primedJump(1f).applyEffect(0f, 1f);

        assertTrue(run.isReturnRequested());
        verify(sector).doHyperspaceTransition(eq(playerFleet), eq(playerFleet), any(JumpDestination.class));
        verify(repairs).applyCREvent(eq(-0.5f * FractureJumpAbility.CR_COST_MULT), anyString());
        verify(cargo).removeFuel(FUEL_COST);
    }

    @Test
    void aJumpThatCannotReachTheReturnPointCostsNothing() {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 2);
        run.recordArrival();
        ChartRuns.store(run);
        when(playerFleet.getContainingLocation()).thenReturn(chartSystem);
        when(playerFleet.isInHyperspaceTransition()).thenReturn(true);

        primedJump(1f).applyEffect(0f, 1f);

        verify(sector, never()).doHyperspaceTransition(any(), any(), any());
        verify(cargo, never()).removeFuel(anyFloat());
        verify(repairs, never()).applyCREvent(anyFloat(), anyString());
    }

    @Test
    void outsideAChartSystemTheJumpGoesToHyperspaceAsInVanilla() {
        ChartRun run = runWithOpenOuterWormhole();
        StarSystemAPI ordinary = mock(StarSystemAPI.class);
        SectorEntityToken centre = mock(SectorEntityToken.class);
        when(centre.getLocation()).thenReturn(new Vector2f());
        when(ordinary.getCenter()).thenReturn(centre);
        when(ordinary.getLocation()).thenReturn(new Vector2f(5000f, 0f));
        when(playerFleet.getContainingLocation()).thenReturn(ordinary);
        when(playerFleet.getLocation()).thenReturn(new Vector2f(1000f, 0f));
        SectorEntityToken hyperspaceSpot = mock(SectorEntityToken.class);
        when(hyperspace.createToken(anyFloat(), anyFloat())).thenReturn(hyperspaceSpot);

        primedJump(0f).applyEffect(0f, 1f);

        assertFalse(run.isReturnRequested());
        ArgumentCaptor<JumpDestination> destination = ArgumentCaptor.forClass(JumpDestination.class);
        verify(sector).doHyperspaceTransition(eq(playerFleet), eq(playerFleet), destination.capture());
        assertSame(hyperspaceSpot, destination.getValue().getDestination());
        verify(cargo).removeFuel(FUEL_COST);
    }

    @Test
    void enteringAChartSwapsInTheChartJumpWithTheSameCooldown() {
        FractureJumpAbility vanilla = vanillaJump(0.3f);

        assertTrue(ChartTransverseJump.install(playerFleet));

        AbilityPlugin installed = abilities.get(Abilities.TRANSVERSE_JUMP);
        assertInstanceOf(ChartTransverseJumpAbility.class, installed);
        assertEquals(0.3f, installed.getCooldownLeft());
        assertSame(playerFleet, ((ChartTransverseJumpAbility) installed).getEntity());
        verify(playerFleet).removeScript(vanilla);
        verify(playerFleet).addScript((ChartTransverseJumpAbility) installed);
    }

    @Test
    void puttingVanillaBackAlsoHandsItTheFleetsScriptSlot() {
        vanillaJump(0f);
        ChartTransverseJump.install(playerFleet);
        AbilityPlugin chartJump = abilities.get(Abilities.TRANSVERSE_JUMP);

        ChartTransverseJump.restoreBeforeSave(playerFleet);

        AbilityPlugin restored = abilities.get(Abilities.TRANSVERSE_JUMP);
        verify(playerFleet).removeScript((ChartTransverseJumpAbility) chartJump);
        verify(playerFleet).addScript((FractureJumpAbility) restored);
    }

    @Test
    void anotherModsTransverseJumpIsLeftAloneAndStaysBlocked() {
        FractureJumpAbility otherMod = new FractureJumpAbility() {
        };
        abilities.put(Abilities.TRANSVERSE_JUMP, otherMod);

        assertFalse(ChartTransverseJump.install(playerFleet));
        assertSame(otherMod, abilities.get(Abilities.TRANSVERSE_JUMP));
    }

    @Test
    void leavingPutsVanillaBackOnlyOnceTheJumpHasFinished() {
        ChartTransverseJumpAbility busy = new ChartTransverseJumpAbility() {
            @Override
            public boolean isActiveOrInProgress() {
                return true;
            }
        };
        busy.init(Abilities.TRANSVERSE_JUMP, playerFleet);
        abilities.put(Abilities.TRANSVERSE_JUMP, busy);

        assertFalse(ChartTransverseJump.restoreWhenIdle(playerFleet));
        assertSame(busy, abilities.get(Abilities.TRANSVERSE_JUMP));

        vanillaJump(0.2f);
        ChartTransverseJump.install(playerFleet);
        assertTrue(ChartTransverseJump.restoreWhenIdle(playerFleet));
        AbilityPlugin restored = abilities.get(Abilities.TRANSVERSE_JUMP);
        assertEquals(FractureJumpAbility.class, restored.getClass());
        assertEquals(0.2f, restored.getCooldownLeft());
    }

    @Test
    void savingAlwaysPutsVanillaBackSoTheSaveNeverHoldsTheChartJump() {
        ChartTransverseJumpAbility busy = new ChartTransverseJumpAbility() {
            @Override
            public boolean isActiveOrInProgress() {
                return true;
            }
        };
        busy.init(Abilities.TRANSVERSE_JUMP, playerFleet);
        abilities.put(Abilities.TRANSVERSE_JUMP, busy);

        ChartTransverseJump.restoreBeforeSave(playerFleet);

        assertEquals(FractureJumpAbility.class, abilities.get(Abilities.TRANSVERSE_JUMP).getClass());
    }

    @Test
    void theChartJumpIsNotBlockedButOtherEscapeAbilitiesAre() {
        ChartTransverseJumpAbility chartJump = spy(new ChartTransverseJumpAbility());
        chartJump.init(Abilities.TRANSVERSE_JUMP, playerFleet);
        abilities.put(Abilities.TRANSVERSE_JUMP, chartJump);

        ChartRunScript.blockEscapeAbilities(playerFleet);

        verify(chartJump, never()).forceDisable();
    }

    @Test
    void theWatchdogSwapsTheJumpInWhileInsideAndBackOutAfterLeaving() {
        runWithOpenOuterWormhole();
        vanillaJump(0f);
        ChartRunScript watchdog = new ChartRunScript();
        when(playerFleet.getContainingLocation()).thenReturn(chartSystem);

        watchdog.watch(sector, ChartRuns.currentOrNull());

        assertInstanceOf(ChartTransverseJumpAbility.class, abilities.get(Abilities.TRANSVERSE_JUMP));

        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
        watchdog.watch(sector, ChartRuns.currentOrNull());

        assertEquals(FractureJumpAbility.class, abilities.get(Abilities.TRANSVERSE_JUMP).getClass());
    }
}
