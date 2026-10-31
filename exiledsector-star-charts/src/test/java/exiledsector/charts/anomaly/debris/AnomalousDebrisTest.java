package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.StatBonus;
import exiledsector.MockSector;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnomalousDebrisTest {

    private MockSector mockSector;
    private StarSystemAPI system;
    private final List<CampaignFleetAPI> fleets = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        system = mock(StarSystemAPI.class);
        when(system.getFleets()).thenReturn(fleets);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private AnomalyContentSite site(List<SectorEntityToken> pois) {
        return new AnomalyContentSite(system, "run1", 4, "pirates", ChartGenContext.none(), pois, new Vector2f(), spot -> true,
                salt -> new Random(salt));
    }

    @Test
    void theAnomalyIsBuiltIn() {
        assertSame(AnomalousDebris.INSTANCE, ChartAnomalies.byId(AnomalousDebris.ANOMALY_ID));
    }

    @Test
    void fieldsKeepClearOfTheEntryAndOfEachOther() {
        SectorEntityToken poi = mock(SectorEntityToken.class);
        when(poi.getLocation()).thenReturn(new Vector2f(5000f, 0f));
        when(poi.getRadius()).thenReturn(50f);
        for (long seed = 0; seed < 20; seed++) {
            List<AnomalousDebris.Field> fields = AnomalousDebris.pickFields(site(List.of(poi)), 4, new Random(seed));
            assertFalse(fields.isEmpty());
            for (int i = 0; i < fields.size(); i++) {
                AnomalousDebris.Field field = fields.get(i);
                assertTrue(field.centre().length() >= field.radius() + AnomalousDebris.ENTRY_CLEARANCE);
                for (int j = i + 1; j < fields.size(); j++) {
                    AnomalousDebris.Field other = fields.get(j);
                    float apart = Vector2f.sub(field.centre(), other.centre(), new Vector2f()).length();
                    assertTrue(apart >= field.radius() + other.radius() + AnomalousDebris.FIELD_GAP);
                }
            }
        }
    }

    @Test
    void theFieldRevealsItsFleetOnceWhenThePlayerComesClose() {
        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(entity.getLocation()).thenReturn(new Vector2f());
        when(entity.getContainingLocation()).thenReturn(system);
        CampaignFleetAPI dormant = mock(CampaignFleetAPI.class);
        StatBonus detected = new StatBonus();
        when(dormant.getId()).thenReturn("dormant1");
        when(dormant.getDetectedRangeMod()).thenReturn(detected);
        fleets.add(dormant);
        DormantFleets.hide(dormant);
        AnomalousDebrisTerrainPlugin field = new AnomalousDebrisTerrainPlugin();
        field.init(AnomalousDebris.TERRAIN_ID, entity, new AnomalousDebrisTerrainPlugin.Params("run1", "dormant1", 400f, 9L));
        CampaignFleetAPI player = mockSector.playerFleet();
        Vector2f playerAt = new Vector2f(5000f, 0f);
        when(player.getLocation()).thenReturn(playerAt);
        when(player.getContainingLocation()).thenReturn(system);

        field.advance(1f);
        assertTrue(DormantFleets.isHidden(dormant));
        assertFalse(field.isRevealed());

        playerAt.set(900f, 0f);
        field.advance(1f);
        assertFalse(DormantFleets.isHidden(dormant));
        assertTrue(field.isRevealed());
        assertEquals(0f, detected.getFlatBonus());
    }

    @Test
    void piecesMoveSmoothlyForHoursWithoutJumping() {
        List<AnomalousDebrisTerrainPlugin.Piece> pieces = AnomalousDebrisTerrainPlugin.pieces(400f, 4L);
        float[] angles = new float[pieces.size()];
        float[] facings = new float[pieces.size()];
        float[] pulses = new float[pieces.size()];
        float frame = 1f / 60f;
        for (int step = 0; step < 4 * 3600 * 60; step++) {
            float before = angles[0];
            float beforeFacing = facings[0];
            AnomalousDebrisTerrainPlugin.advancePhases(pieces, angles, facings, pulses, frame);
            float moved = AnomalousDebrisTerrainPlugin.wrap(angles[0] - before + 180f, 360f) - 180f;
            float spun = AnomalousDebrisTerrainPlugin.wrap(facings[0] - beforeFacing + 180f, 360f) - 180f;
            assertEquals(pieces.get(0).driftDegreesPerSecond() * frame, moved, 0.01f);
            assertEquals(pieces.get(0).spinDegreesPerSecond() * frame, spun, 0.01f);
        }
        assertTrue(angles[0] >= 0f && angles[0] < 360f);
        assertTrue(pulses[0] >= 0f && pulses[0] < (float) Math.PI * 2f);
    }

    @Test
    void piecesAreTheSameForTheSameSeedAndStayInsideTheField() {
        List<AnomalousDebrisTerrainPlugin.Piece> pieces = AnomalousDebrisTerrainPlugin.pieces(400f, 4L);
        assertEquals(pieces, AnomalousDebrisTerrainPlugin.pieces(400f, 4L));
        assertTrue(pieces.size() > 10);
        assertTrue(pieces.stream().allMatch(piece -> piece.orbitRadius() <= 400f));
    }
}
