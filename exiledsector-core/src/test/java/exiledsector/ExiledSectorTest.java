package exiledsector;

import exiledlib.TestHost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExiledSectorTest {

    @Test
    void testsRegisterTheSameHostModAsTheGame() {
        assertEquals(ExiledSector.HOST, TestHost.EXILED_SECTOR);
    }
}
