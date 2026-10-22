package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledlib.items.Item;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.socket.ItemStoragePanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocketPlacementRefusalTest {

    private static final String REFUSAL = "Second Bypass can't be installed";

    private final TreeAllocationSession treeSession = mock(TreeAllocationSession.class);
    private final ItemStoragePanel storagePanel = mock(ItemStoragePanel.class);
    private final AtomicReference<ItemStoragePanel.Listener> storageListener = new AtomicReference<>();
    private final SkillNode socket = new SkillNode("socket_2", new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build(),
            List.of(), 0f, 0f);
    private final Item secondBypass = mock(Item.class);
    private final Item volume = mock(Item.class);
    private MockedStatic<Global> globalMock;
    private MockedStatic<ItemStoragePanel> storagePanelMock;
    private SocketPlacement placement;

    @BeforeEach
    void setUp() {
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        storagePanelMock = Mockito.mockStatic(ItemStoragePanel.class);
        storagePanelMock.when(() -> ItemStoragePanel.openSubroutines(any(), any(), any(), any())).thenAnswer(invocation -> {
            storageListener.set(invocation.getArgument(3));
            return storagePanel;
        });
        when(treeSession.isAllocated(socket)).thenReturn(true);
        when(treeSession.isAllocatedSocket(socket)).thenReturn(true);
        when(treeSession.installInSocket(socket, secondBypass)).thenReturn(false);
        when(treeSession.socketRefusal(socket, secondBypass)).thenReturn(REFUSAL);
        when(treeSession.installInSocket(socket, volume)).thenReturn(true);
        placement = new SocketPlacement(mock(CustomPanelAPI.class), treeSession, mock(NodeSearch.class), mock(SkillTreeSearchBar.class));
        placement.handleSocketClick(socket, true, mock(PositionAPI.class), ScreenRect.NONE);
    }

    @AfterEach
    void tearDown() {
        storagePanelMock.close();
        globalMock.close();
    }

    @Test
    void aRefusalInTargetedSocketModeIsClearedOnceAnotherItemInstalls() {
        storageListener.get().selected(secondBypass);
        storageListener.get().selected(volume);

        InOrder notices = inOrder(storagePanel);
        notices.verify(storagePanel).showRefusal(REFUSAL);
        notices.verify(storagePanel).clearRefusal();
    }

    @Test
    void aRefusedItemIsNotPickedUpForPlacing() {
        storageListener.get().selected(secondBypass);

        verify(storagePanel).showRefusal(REFUSAL);
        verify(storagePanel, never()).setSelected(any(), any());
    }

    @Test
    void retargetingASocketClearsTheRefusalInsideThePanel() {
        storageListener.get().selected(secondBypass);
        placement.handleSocketClick(socket, true, mock(PositionAPI.class), ScreenRect.NONE);

        InOrder notices = inOrder(storagePanel);
        notices.verify(storagePanel).showRefusal(REFUSAL);
        notices.verify(storagePanel).setTargetingSocket(true);
    }
}
