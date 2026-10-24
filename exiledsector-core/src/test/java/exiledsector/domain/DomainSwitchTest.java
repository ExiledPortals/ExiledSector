package exiledsector.domain;

import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainSwitchTest {

    private static final String FIELD_ID = "exiledSector_testDomainEnabled";

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private final AtomicInteger switches = new AtomicInteger();
    private final DomainSwitch domainSwitch = new DomainSwitch(FIELD_ID, true, switches::incrementAndGet);

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
    }

    private void configure(Boolean value) {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(Mockito.any(), Mockito.eq(FIELD_ID))).thenReturn(value);
    }

    @Test
    void itStartsAtItsDefaultAndKeepsItWhileTheSettingIsUnset() {
        assertTrue(domainSwitch.on());

        configure(null);
        domainSwitch.reload();

        assertTrue(domainSwitch.on());
        assertEquals(0, switches.get());
    }

    @Test
    void theCallbackRunsOnlyWhenTheSettingChanges() {
        configure(false);
        domainSwitch.reload();
        domainSwitch.reload();

        assertFalse(domainSwitch.on());
        assertEquals(1, switches.get());

        configure(true);
        domainSwitch.reload();

        assertTrue(domainSwitch.on());
        assertEquals(2, switches.get());
    }

    @Test
    void theTestSetterChangesItWithoutTheCallback() {
        domainSwitch.setForTests(false);

        assertFalse(domainSwitch.on());
        assertEquals(0, switches.get());
    }
}
