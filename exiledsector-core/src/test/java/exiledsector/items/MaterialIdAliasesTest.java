package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.ProjectFiles;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaterialIdAliasesTest {

    private static final String OLD_ID = "exiledSector_old_swarm";
    private static final String NEW_ID = Nanoswarm.AUGMENTATION.itemId();
    private static final String RENAMED_ID_TAG = "exiledSector_renamed_id";

    @AfterEach
    void tearDown() {
        MaterialIdAliases.clear();
    }

    private static JSONArray csvRows(String dataPath) throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

    private static void registerAlias() throws Exception {
        MaterialIdAliases.register(new JSONArray().put(new JSONObject().put("alias", OLD_ID).put("id", NEW_ID)));
    }

    @Test
    void stacksAndLegacyCommoditiesUnderAnOldIdBecomeTheCurrentItem() throws Exception {
        registerAlias();
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(OLD_ID))).thenReturn(3f);
        when(cargo.getCommodityQuantity(OLD_ID)).thenReturn(2f);

        assertEquals(5, MaterialMigration.convert(cargo));

        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(OLD_ID), 3);
        verify(cargo).addSpecial(MaterialItems.data(NEW_ID), 3);
        verify(cargo).removeCommodity(OLD_ID, 2);
        verify(cargo).addSpecial(MaterialItems.data(NEW_ID), 2);
    }

    @Test
    void storedCountsUnderAnOldIdJoinTheCurrentId() throws Exception {
        registerAlias();
        Map<String, Integer> stored = new HashMap<>(Map.of(OLD_ID, 4, NEW_ID, 1));

        assertEquals(4, CraftingMaterials.renameStoredIds(stored, MaterialIdAliases.currentIdsByAlias()));

        assertEquals(Map.of(NEW_ID, 5), stored);
        assertEquals(0, CraftingMaterials.renameStoredIds(null, MaterialIdAliases.currentIdsByAlias()));
    }

    @Test
    void oldIdsReadAsTheCurrentIdAndOtherIdsPassThrough() throws Exception {
        registerAlias();

        assertEquals(NEW_ID, MaterialIdAliases.currentId(OLD_ID));
        assertEquals(NEW_ID, MaterialIdAliases.currentId(NEW_ID));
        assertNull(MaterialIdAliases.currentId(null));
    }

    @Test
    void everyAliasPointsAtAShippedMaterial() throws Exception {
        Map<String, JSONObject> specialItems = specialItemRows();
        JSONArray aliases = csvRows(MaterialIdAliases.dataPath());
        assertTrue(aliases.length() > 0);
        for (int index = 0; index < aliases.length(); index++) {
            String alias = aliases.getJSONObject(index).getString("alias");
            String currentId = aliases.getJSONObject(index).getString("id");
            JSONObject current = specialItems.get(currentId);
            assertNotNull(current, alias + " points at " + currentId + ", which is not in special_items.csv");
            assertTrue(current.optString("plugin").endsWith("MaterialItemPlugin"), currentId + " is not a material");
            assertFalse(tags(current).contains(RENAMED_ID_TAG), currentId + " is itself marked as a renamed id");
        }
    }

    @Test
    void everyRenamedIdKeepsAHiddenRowSoOldSavesLoad() throws Exception {
        Map<String, JSONObject> specialItems = specialItemRows();
        Map<String, String> currentIdsByOldId = new HashMap<>();
        JSONArray aliases = csvRows(MaterialIdAliases.dataPath());
        for (int index = 0; index < aliases.length(); index++) {
            currentIdsByOldId.put(aliases.getJSONObject(index).getString("alias"), aliases.getJSONObject(index).getString("id"));
        }
        currentIdsByOldId.put(RefitSchematic.LEGACY_ITEM_ID, RefitSchematic.ITEM_ID);
        currentIdsByOldId.forEach((oldId, currentId) -> {
            JSONObject legacy = specialItems.get(oldId);
            assertNotNull(legacy, oldId + " has no row in special_items.csv, so saves holding it fail to load");
            assertTrue(tags(legacy).containsAll(List.of("hide_in_codex", RENAMED_ID_TAG)), oldId + " must be tagged hide_in_codex and " + RENAMED_ID_TAG);
            assertEquals(specialItems.get(currentId).optString("plugin"), legacy.optString("plugin"), oldId + " must use the plugin of " + currentId);
        });
        specialItems.forEach((id, row) -> assertTrue(!tags(row).contains(RENAMED_ID_TAG) || currentIdsByOldId.containsKey(id),
                id + " is tagged " + RENAMED_ID_TAG + " but no alias maps it to a current id"));
    }

    private static Map<String, JSONObject> specialItemRows() throws Exception {
        JSONArray rows = csvRows("data/campaign/special_items.csv");
        Map<String, JSONObject> rowsById = new HashMap<>();
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.getJSONObject(index);
            rowsById.put(row.optString("id"), row);
        }
        return rowsById;
    }

    private static Set<String> tags(JSONObject row) {
        Set<String> tags = new HashSet<>();
        for (String tag : row.optString("tags").split(",")) {
            tags.add(tag.trim());
        }
        return tags;
    }
}
