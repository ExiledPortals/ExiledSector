package exiledsector.charts.officer;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.reward.OfficerRewardDrop;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.Scars;
import exiledsector.charts.phasing.DisplacementTheorem;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemDrops;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class OfficerDefeatTheoremTest {

    private static final int BOSS_RECORD_ID = 7;
    private static final int SKIRMISH_RECORD_ID = 8;
    private static final Map<String, Integer> THEOREM = Map.of(DisplacementTheorem.ITEM_ID, 1);

    private final SectorAPI sector = mock(SectorAPI.class);
    private final ChartRun run = mock(ChartRun.class);
    private final AtlasState state = mock(AtlasState.class);
    private final AtlasOfficer boss = admiral(BOSS_RECORD_ID);
    private final AtlasOfficer skirmisher = admiral(SKIRMISH_RECORD_ID);
    private MockedStatic<AtlasState> stateMock;
    private MockedStatic<OfficerFleets> fleetsMock;
    private MockedStatic<Scars> scarsMock;
    private MockedStatic<OfficerRewardDrop> dropMock;
    private MockedStatic<OfficerRewardPool> poolMock;
    private MockedStatic<RewardContext> contextMock;
    private MockedStatic<DisplacementTheorem> theoremMock;

    private static AtlasOfficer admiral(int recordId) {
        return AtlasOfficer.waiting(recordId, "hegemony", "admiral", "steady", new AtlasOfficer.OfficerSeeds(1L, 2L, 3L, 4L), null);
    }

    @BeforeEach
    void setUp() {
        when(state.officer(BOSS_RECORD_ID)).thenReturn(boss);
        when(state.officer(SKIRMISH_RECORD_ID)).thenReturn(skirmisher);
        stateMock = Mockito.mockStatic(AtlasState.class);
        stateMock.when(() -> AtlasState.getIfCreated(sector)).thenReturn(state);
        fleetsMock = Mockito.mockStatic(OfficerFleets.class);
        fleetsMock.when(() -> OfficerFleets.officerOf(nullable(AtlasRunNode.class))).thenReturn(boss);
        fleetsMock.when(() -> OfficerFleets.fightingRecordIdsOf(nullable(AtlasRunNode.class))).thenReturn(Set.of());
        scarsMock = Mockito.mockStatic(Scars.class, Mockito.CALLS_REAL_METHODS);
        scarsMock.when(() -> Scars.pickAfterBattle(nullable(String.class), any(), nullable(HullFacts.class))).thenReturn(null);
        dropMock = Mockito.mockStatic(OfficerRewardDrop.class);
        dropMock.when(() -> OfficerRewardDrop.roll(anyList(), any(ChartRun.class), any(Random.class))).thenReturn(ItemDrops.SiteLoot.NONE);
        contextMock = Mockito.mockStatic(RewardContext.class);
        poolMock = Mockito.mockStatic(OfficerRewardPool.class);
        poolMock.when(() -> OfficerRewardPool.offers(nullable(RewardContext.class), any(Random.class))).thenReturn(List.of());
        theoremMock = Mockito.mockStatic(DisplacementTheorem.class);
        theoremMock.when(() -> DisplacementTheorem.bossDrop(any(ChartRun.class), anyString(), anyInt())).thenReturn(THEOREM);
    }

    @AfterEach
    void tearDown() {
        theoremMock.close();
        poolMock.close();
        contextMock.close();
        dropMock.close();
        scarsMock.close();
        fleetsMock.close();
        stateMock.close();
    }

    @Test
    void aChartBossRollsTheTheoremOnceFromItsRankBeforeTheDefeat() {
        ItemDrops.SiteLoot loot = OfficerDefeat.bossDefeated(sector, run);

        assertEquals(THEOREM, loot.materials());
        theoremMock.verify(() -> DisplacementTheorem.bossDrop(run, "admiral", BOSS_RECORD_ID), times(1));
    }

    @Test
    void aFactionSkirmishOfficerRollsTheTheoremOnceFromItsRankBeforeTheDefeat() {
        ItemDrops.SiteLoot loot = OfficerDefeat.officerBeaten(sector, run, SKIRMISH_RECORD_ID, "flagship");

        assertEquals(THEOREM, loot.materials());
        theoremMock.verify(() -> DisplacementTheorem.bossDrop(run, "admiral", SKIRMISH_RECORD_ID), times(1));
        theoremMock.verify(() -> DisplacementTheorem.bossDrop(any(ChartRun.class), anyString(), anyInt()), times(1));
    }
}
