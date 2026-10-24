package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;
import lunalib.lunaRefit.BaseRefitButton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SectionEditorTest {

    private MockedStatic<Global> globalMock;
    private FleetMemberAPI member;
    private BaseRefitButton refitButton;
    private SectionEditor editor;

    @BeforeEach
    void setUp() {
        globalMock = Mockito.mockStatic(Global.class);
        SectorAPI sector = mock(SectorAPI.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship");
        refitButton = mock(BaseRefitButton.class);
        editor = new SectionEditor(member, mock(ShipVariantAPI.class), refitButton);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Test
    void aChangeToTheShipsDataFromOutsideTheScreenRefreshesTheShip() {
        ShipSkillDataManager.get("ship").markChanged();

        editor.syncOutsideChanges();

        assertEquals(1, editor.revision());
        verify(refitButton).refreshVariant();
        verify(member).updateStats();
    }

    @Test
    void withoutAnOutsideChangeNothingIsRefreshed() {
        editor.syncOutsideChanges();
        editor.syncOutsideChanges();

        assertEquals(0, editor.revision());
        verify(refitButton, never()).refreshVariant();
    }
}
