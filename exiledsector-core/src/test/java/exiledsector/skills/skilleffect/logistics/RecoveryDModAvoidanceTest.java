package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.plugins.DModAdderPlugin.DModAdderParams;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecoveryDModAvoidanceTest {

    private static final class FixedRoll extends Random {
        private final float roll;

        FixedRoll(float roll) {
            this.roll = roll;
        }

        @Override
        public float nextFloat() {
            return roll;
        }
    }

    private static FleetMemberAPI member(float chancePercent, boolean mothballed) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getStats().getDynamic().getValue(RecoveryDModAvoidance.CHANCE_PERCENT_KEY, 0f)).thenReturn(chancePercent);
        return member;
    }

    private static CampaignFleetAPI fleet(FleetMemberAPI... members) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(List.of(members));
        return fleet;
    }

    private static DModAdderParams params(CampaignFleetAPI recoverer, float roll) {
        DModAdderParams params = new DModAdderParams();
        params.recoverer = recoverer;
        params.random = new FixedRoll(roll);
        return params;
    }

    @Test
    void activeShipsAddTheirChancesUpToTheFleetMaximum() {
        assertEquals(10f, RecoveryDModAvoidance.chancePercent(List.of(member(5f, false), member(5f, false), member(5f, true))));
        List<FleetMemberAPI> manySuites = List.of(member(5f, false), member(5f, false), member(5f, false), member(5f, false),
                member(5f, false), member(5f, false), member(5f, false), member(5f, false));
        assertEquals(RecoveryDModAvoidance.MAX_CHANCE_PERCENT, RecoveryDModAvoidance.chancePercent(manySuites));
    }

    @Test
    void aShipThePlayerRecoversSkipsItsDModsWhenTheRollLandsUnderTheChance() {
        CampaignFleetAPI playerFleet = fleet(member(5f, false), member(5f, false));

        assertTrue(RecoveryDModAvoidance.avoidsDMods(params(playerFleet, 0.09f), playerFleet));
        assertFalse(RecoveryDModAvoidance.avoidsDMods(params(playerFleet, 0.1f), playerFleet));
    }

    @Test
    void otherRecoverersAndFleetsWithoutTheEffectAddDModsAsNormal() {
        CampaignFleetAPI playerFleet = fleet(member(5f, false));

        assertFalse(RecoveryDModAvoidance.avoidsDMods(params(fleet(member(5f, false)), 0f), playerFleet));
        assertFalse(RecoveryDModAvoidance.avoidsDMods(params(null, 0f), playerFleet));
        assertFalse(RecoveryDModAvoidance.avoidsDMods(params(playerFleet, 0f), null));
        CampaignFleetAPI plainFleet = fleet(member(0f, false));
        assertFalse(RecoveryDModAvoidance.avoidsDMods(params(plainFleet, 0f), plainFleet));
    }

    @Test
    void theTransientPluginIsAddedOncePerSector() {
        SectorAPI sector = mock(SectorAPI.class);
        GenericPluginManagerAPI plugins = mock(GenericPluginManagerAPI.class);
        when(sector.getGenericPlugins()).thenReturn(plugins);
        when(plugins.hasPlugin(RecoveryDModAvoidance.class)).thenReturn(false, true);

        RecoveryDModAvoidance.install(sector);
        RecoveryDModAvoidance.install(sector);
        RecoveryDModAvoidance.install(null);

        verify(plugins).addPlugin(any(RecoveryDModAvoidance.class), eq(true));
        verify(plugins, times(1)).addPlugin(any(), anyBoolean());
    }
}
