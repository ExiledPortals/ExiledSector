package exiledsector.ui.refit;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import exiledsector.skills.PhantomHullModStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class PhantomHullModRefitHiderTest {

    public static class Core {
        public final Tab tab = new Tab();
        public final List<Object> children = new ArrayList<>();

        public Tab getCurrentTab() {
            return tab;
        }

        public List<Object> getChildrenNonCopy() {
            return children;
        }
    }

    public static class Dialog {
        public final List<Object> children = new ArrayList<>();

        public List<Object> getChildrenNonCopy() {
            return children;
        }
    }

    public static class Tab {
        public final RefitPanel refitPanel = new RefitPanel();

        public RefitPanel getRefitPanel() {
            return refitPanel;
        }
    }

    public static class RefitPanel {
        public final ModDisplay modDisplay = new ModDisplay();
        public final ShipDisplay shipDisplay = new ShipDisplay();

        public ModDisplay getModDisplay() {
            return modDisplay;
        }

        public ShipDisplay getShipDisplay() {
            return shipDisplay;
        }
    }

    public static class ShipDisplay {
        public ShipVariantAPI variant;

        public ShipVariantAPI getCurrentVariant() {
            return variant;
        }
    }

    public static class ModDisplay {
        public final ModWidget mods = new ModWidget();

        public ModWidget getMods() {
            return mods;
        }
    }

    public static class ModWidget {
        public final RowList list = newList();
        public final List<Object> children = new ArrayList<>(List.of(new Object(), list));
        public int packs;

        public List<Object> getChildrenNonCopy() {
            return children;
        }

        public void pack() {
            packs++;
        }
    }

    public abstract static class RowList implements UIComponentAPI {
        public final List<Object> items = new ArrayList<>();
        public int collapses;

        public List<Object> getItems() {
            return items;
        }

        public void removeItem(Object row) {
            items.remove(row);
        }

        public void collapseEmptySlots() {
            collapses++;
        }

        public void collapseEmptySlots(boolean keepScroll) {
            collapses++;
        }

        public float getItemHeight() {
            return 34f;
        }

        public float getItemPad() {
            return 3f;
        }
    }

    public abstract static class CampaignState implements CampaignUIAPI {
        public Core core;

        public Core getCore() {
            return core;
        }
    }

    public static class Row {
        private final String label;
        private final HullModSpecAPI spec;

        Row(HullModSpecAPI spec) {
            this.label = "row";
            this.spec = spec;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static RowList newList() {
        RowList list = mock(RowList.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        PositionAPI position = mock(PositionAPI.class);
        when(position.getWidth()).thenReturn(384f);
        doReturn(position).when(list).getPosition();
        return list;
    }

    private Core core;
    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private CampaignState campaignState;

    @BeforeEach
    void setUp() {
        PhantomHullModRefitHider.resetForTests();
        PhantomHullModStatus.clear();
        PhantomHullModStatus.markActive("safetyoverrides");
        PhantomHullModStatus.markActive("heavyarmor");
        core = new Core();
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasTag("exiledSector_installed_safetyoverrides")).thenReturn(true);
        when(variant.hasTag("exiledSector_installed_inactive_phantom")).thenReturn(true);
        core.tab.refitPanel.shipDisplay.variant = variant;

        campaignState = mock(CampaignState.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        campaignState.core = core;
        doReturn(null).when(campaignState).getCurrentInteractionDialog();
        sector = mock(SectorAPI.class);
        when(sector.getCampaignUI()).thenReturn(campaignState);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        PhantomHullModRefitHider.resetForTests();
        PhantomHullModStatus.clear();
    }

    private static Row row(String id) {
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getId()).thenReturn(id);
        return new Row(spec);
    }

    private ModWidget refitMods() {
        return core.tab.refitPanel.modDisplay.mods;
    }

    @Test
    void onlyTheRowsOfPhantomsTheTreePlacedOnThisShipAreRemovedAndTheListShrinksToFit() throws Throwable {
        Row phantom = row("safetyoverrides");
        Row realHeavyArmor = row("heavyarmor");
        Row taggedButNotAnActivePhantom = row("inactive_phantom");
        Row ordinary = row("eccm");
        refitMods().list.items.addAll(List.of(realHeavyArmor, phantom, taggedButNotAnActivePhantom, ordinary));

        PhantomHullModRefitHider.hidePhantomRows(core);

        assertEquals(List.of(realHeavyArmor, taggedButNotAnActivePhantom, ordinary), refitMods().list.items);
        assertEquals(1, refitMods().list.collapses);
        verify(refitMods().list.getPosition()).setSize(384f, 3 * 37f);
        assertEquals(1, refitMods().packs);
    }

    @Test
    void theListNeverGrowsTallerThanVanillasTenRows() throws Throwable {
        refitMods().list.items.add(row("safetyoverrides"));
        for (int i = 0; i < 12; i++) {
            refitMods().list.items.add(row("mod_" + i));
        }

        PhantomHullModRefitHider.hidePhantomRows(core);

        verify(refitMods().list.getPosition()).setSize(384f, PhantomHullModRefitHider.MAX_VISIBLE_ROWS * 37f);
    }

    @Test
    void aListWithNothingToHideIsLeftUntouched() throws Throwable {
        refitMods().list.items.add(row("eccm"));

        PhantomHullModRefitHider.hidePhantomRows(core);

        assertEquals(0, refitMods().list.collapses);
        assertEquals(0, refitMods().packs);
    }

    @Test
    void aScreenThatIsNotTheRefitPanelIsIgnored() {
        Object notARefitScreen = new Object();

        assertDoesNotThrow(() -> PhantomHullModRefitHider.hidePhantomRows(notARefitScreen));
    }

    @Test
    void everyRefitSyncHidesTheRowsItJustRebuilt() {
        doReturn(CoreUITabId.REFIT).when(campaignState).getCurrentCoreTab();
        refitMods().list.items.add(row("safetyoverrides"));

        new PhantomHullModRefitHider().reportRefreshedCharacterStatEffects();

        assertTrue(refitMods().list.items.isEmpty());
    }

    @Test
    void statRefreshesOutsideTheRefitScreenLeaveEverythingAlone() {
        doReturn(CoreUITabId.CARGO).when(campaignState).getCurrentCoreTab();
        refitMods().list.items.add(row("safetyoverrides"));

        new PhantomHullModRefitHider().reportRefreshedCharacterStatEffects();

        assertEquals(1, refitMods().list.items.size());
    }

    private EveryFrameScript scheduledHide() {
        ArgumentCaptor<EveryFrameScript> script = ArgumentCaptor.forClass(EveryFrameScript.class);
        verify(sector).addTransientScript(script.capture());
        return script.getValue();
    }

    @Test
    void aListRebuiltWithAPhantomInItIsHiddenByOneScheduledPassWhateverRebuiltIt() {
        globalMock.when(Global::getCurrentState).thenReturn(GameState.CAMPAIGN);
        refitMods().list.items.add(row("safetyoverrides"));

        PhantomHullModRefitHider.requestHide();
        EveryFrameScript hide = scheduledHide();
        when(sector.hasTransientScript(any())).thenReturn(true);
        PhantomHullModRefitHider.requestHide();

        assertTrue(hide.runWhilePaused());
        assertFalse(hide.isDone());
        hide.advance(0f);

        assertTrue(hide.isDone());
        assertTrue(refitMods().list.items.isEmpty());
    }

    @Test
    void aRebuildAfterThePassRanSchedulesAnotherOne() {
        globalMock.when(Global::getCurrentState).thenReturn(GameState.CAMPAIGN);
        when(sector.hasTransientScript(any())).thenReturn(true);
        PhantomHullModRefitHider.requestHide();
        scheduledHide().advance(0f);

        PhantomHullModRefitHider.requestHide();

        verify(sector, times(2)).addTransientScript(any());
    }

    @Test
    void aPassLostWithAReloadedSaveIsScheduledAgain() {
        globalMock.when(Global::getCurrentState).thenReturn(GameState.CAMPAIGN);
        PhantomHullModRefitHider.requestHide();

        PhantomHullModRefitHider.requestHide();

        verify(sector, times(2)).addTransientScript(any());
    }

    @Test
    void nothingIsScheduledOutsideTheCampaign() {
        globalMock.when(Global::getCurrentState).thenReturn(GameState.COMBAT);

        PhantomHullModRefitHider.requestHide();

        verify(sector, never()).addTransientScript(any());
    }

    @Test
    void theCopyOfTheListInsideTheAddAndBuildInDialogIsHiddenToo() throws Throwable {
        ModDisplay dialogCopy = new ModDisplay();
        dialogCopy.mods.list.items.add(row("safetyoverrides"));
        Dialog dialog = new Dialog();
        dialog.children.add(dialogCopy);
        core.children.add(dialog);

        PhantomHullModRefitHider.hidePhantomRows(core);

        assertEquals(List.of(), dialogCopy.mods.list.items);
    }
}
