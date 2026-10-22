package exiledsector.skills.npc;

import exiledsector.ExiledSector;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcTreeSizeTest {

    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
    }

    @Test
    void theDefaultsAreEightyToOneHundredAndTwentyPercent() {
        assertEquals(new NpcTreeSize.PercentRange(80, 120), NpcTreeSize.percentRange());
    }

    @Test
    void everyRollMirrorsOneOfThePlayersShipsWithinThePercentRange() {
        List<Integer> playerShipPowers = List.of(10, 50);
        Random random = new Random(7L);
        boolean sawSmall = false;
        boolean sawLarge = false;
        for (int roll = 0; roll < 500; roll++) {
            int nodeCount = NpcTreeSize.roll(playerShipPowers, random);
            boolean mirrorsSmall = nodeCount >= 8 && nodeCount <= 12;
            boolean mirrorsLarge = nodeCount >= 40 && nodeCount <= 60;
            assertTrue(mirrorsSmall || mirrorsLarge, "unexpected node count " + nodeCount);
            sawSmall |= mirrorsSmall;
            sawLarge |= mirrorsLarge;
        }
        assertTrue(sawSmall && sawLarge);
    }

    @Test
    void theSameSeedRollsTheSameTree() {
        List<Integer> playerShipPowers = List.of(12, 20, 33);
        assertEquals(NpcTreeSize.roll(playerShipPowers, new Random(42L)), NpcTreeSize.roll(playerShipPowers, new Random(42L)));
    }

    @Test
    void aMinimumSetAboveTheMaximumIsSwapped() {
        lunaSettingsMock.when(() -> LunaSettings.getInt(ExiledSector.MOD_ID, NpcTreeSize.MIN_PERCENT_FIELD_ID)).thenReturn(150);
        lunaSettingsMock.when(() -> LunaSettings.getInt(ExiledSector.MOD_ID, NpcTreeSize.MAX_PERCENT_FIELD_ID)).thenReturn(90);

        assertEquals(new NpcTreeSize.PercentRange(90, 150), NpcTreeSize.percentRange());
    }

    @Test
    void percentagesOutsideTheAllowedRangeAreClamped() {
        lunaSettingsMock.when(() -> LunaSettings.getInt(ExiledSector.MOD_ID, NpcTreeSize.MIN_PERCENT_FIELD_ID)).thenReturn(-20);
        lunaSettingsMock.when(() -> LunaSettings.getInt(ExiledSector.MOD_ID, NpcTreeSize.MAX_PERCENT_FIELD_ID)).thenReturn(900);

        assertEquals(new NpcTreeSize.PercentRange(NpcTreeSize.LOWEST_PERCENT, NpcTreeSize.HIGHEST_PERCENT), NpcTreeSize.percentRange());
    }

    @Test
    void nodeCountsRoundToTheNearestNode() {
        assertEquals(12, NpcTreeSize.nodesFor(15, 80));
        assertEquals(18, NpcTreeSize.nodesFor(15, 120));
        assertEquals(0, NpcTreeSize.roll(List.of(), new Random(1L)));
    }
}
