package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledlib.ProjectFiles;
import exiledsector.ui.cargo.AnchorLook;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;
import exiledsector.ui.cargo.DoctrineIconFx;
import exiledsector.ui.cargo.DoctrineLook;
import exiledsector.ui.cargo.DoctrineScreen;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DoctrineIconLookTest {

    private static final String HEGEMONY = "doctrine_hegemony_test";
    private static final String MODDED = "doctrine_modded_test";
    private static final String PLAIN_ICON = "doctrine_plain_icon_test";
    private static final String NO_ARCHIVE = "doctrine_no_archive_test";
    private static final Color HEGEMONY_UI = new Color(245, 150, 30);
    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() throws Exception {
        SectorAPI sector = mock(SectorAPI.class);
        FactionAPI hegemony = mock(FactionAPI.class);
        when(hegemony.getBaseUIColor()).thenReturn(HEGEMONY_UI);
        when(sector.getFaction("hegemony")).thenReturn(hegemony);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        ItemDefinitions.register(new JSONArray()
                .put(stillRow(HEGEMONY, "hegemony"))
                .put(stillRow(MODDED, "modded"))
                .put(ItemFixtures.subroutineRow(PLAIN_ICON, SubroutineKind.DOCTRINE))
                .put(stillRow(NO_ARCHIVE, "hegemony")));
        DoctrineArchives.register(new JSONArray()
                .put(archiveRow(HEGEMONY, "hegemony"))
                .put(archiveRow(MODDED, "some_modded_faction"))
                .put(archiveRow(PLAIN_ICON, "hegemony")));
    }

    @AfterEach
    void tearDown() {
        DoctrineArchives.clear();
        ItemDefinitions.clear();
        globalMock.close();
    }

    private static JSONObject stillRow(String id, String suffix) throws Exception {
        return ItemFixtures.subroutineRow(id, SubroutineKind.DOCTRINE).put("icon", DoctrineIconFx.STILL_PREFIX + suffix + ".png");
    }

    private static JSONObject archiveRow(String id, String factionId) throws Exception {
        return new JSONObject().put("id", id).put("behaviour", "TRANSFORM").put("radius", "large").put("faction", factionId)
                .put("notables", "some_notable");
    }

    private static Item doctrine(String definitionId) {
        return ItemData.rolled(ItemDefinitions.get(definitionId), 3L).create("socketable_3");
    }

    @Test
    void aDoctrineArchiveWithItsStillIconShowsItsFactionScreenInTheFactionUiColour() {
        CargoIconLook look = CargoIconLooks.forItem(doctrine(HEGEMONY));

        assertEquals(DoctrineLook.of(DoctrineScreen.SEAL, "hegemony"), look.art());
        assertEquals(HEGEMONY_UI, look.color());
        assertTrue(look.framed());
        assertTrue(look.ownArt());
        assertFalse(look.art() instanceof AnchorLook);
        assertEquals(look.art(), look.unframed().art());
        assertEquals(DoctrineIconFx.EMBLEM_PREFIX + "hegemony.png", ((DoctrineLook) look.art()).emblemPath());
    }

    @Test
    void aFactionWithNoScreenOfItsOwnGetsThePlainReadoutAndFallsBackToTheRarityColour() {
        Item modded = doctrine(MODDED);
        CargoIconLook look = CargoIconLooks.forItem(modded);

        assertEquals(DoctrineScreen.READOUT, ((DoctrineLook) look.art()).screen());
        assertEquals(modded.frameColor(), look.color());
    }

    @Test
    void aDoctrineWithAnotherIconOrNoArchiveRowKeepsTheRarityLook() {
        assertFalse(CargoIconLooks.forItem(doctrine(PLAIN_ICON)).art() instanceof DoctrineLook);
        assertFalse(CargoIconLooks.forItem(doctrine(NO_ARCHIVE)).art() instanceof DoctrineLook);
        assertFalse(CargoIconLooks.forItem(doctrine(PLAIN_ICON)).ownArt());
    }

    @Test
    void everyShippedDoctrineUsesItsBakedStillAndHasAnEmblemForItsFaction() throws Exception {
        JSONArray subroutines = ItemFixtures.shippedRows(SubroutineDefinitions.dataPath());
        JSONArray archives = ItemFixtures.shippedRows(DoctrineArchives.DATA_PATH);
        assertTrue(Files.isRegularFile(ProjectFiles.projectRoot().resolve(DoctrineIconFx.STILL_PREFIX + "frame.png")));
        int checked = 0;
        for (int index = 0; index < archives.length(); index++) {
            JSONObject archive = archives.getJSONObject(index);
            String id = archive.getString("id");
            String icon = null;
            for (int row = 0; row < subroutines.length(); row++) {
                if (subroutines.getJSONObject(row).getString("id").equals(id)) {
                    icon = subroutines.getJSONObject(row).getString("icon");
                }
            }
            String factionId = archive.getString("faction");
            assertEquals(DoctrineIconFx.STILL_PREFIX + id.substring("doctrine_archive_".length()) + ".png", icon, id);
            assertTrue(Files.isRegularFile(ProjectFiles.projectRoot().resolve(icon)), icon);
            assertTrue(Files.isRegularFile(ProjectFiles.projectRoot().resolve(DoctrineIconFx.EMBLEM_PREFIX + factionId + ".png")), factionId);
            assertTrue(DoctrineScreen.forFaction(factionId) != DoctrineScreen.READOUT, id + " has a screen of its own");
            checked++;
        }
        assertEquals(7, checked);
    }
}
