package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VanillaChartLayoutEngineTest {

    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
        globalMock.when(Global::getSector).thenReturn(mock(SectorAPI.class));
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
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
