package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledlib.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VanillaChartLayoutEngineTest {

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        mockSector.global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    @Test
    void theSeededRandomIsSwappedInForTheBuildAndRestoredEvenWhenVanillaThrows() {
        Random original = StarSystemGenerator.random;
        Random seeded = new Random(5L);
        StarSystemAPI system = mock(StarSystemAPI.class);
        AtomicReference<Random> duringBuild = new AtomicReference<>();
        doAnswer(invocation -> {
            duringBuild.set(StarSystemGenerator.random);
            return null;
        }).when(system).setType(any());

        assertThrows(RuntimeException.class,
                () -> new VanillaChartLayoutEngine().build(system, "star", StarSystemType.SINGLE, StarAge.OLD, seeded));

        verify(system).setType(StarSystemType.SINGLE);
        assertSame(seeded, duringBuild.get());
        assertSame(original, StarSystemGenerator.random);
    }
}
