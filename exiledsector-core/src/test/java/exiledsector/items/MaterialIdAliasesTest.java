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
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaterialIdAliasesTest {

    private static final String OLD_ID = "exiledSector_old_swarm";
    private static final String NEW_ID = CraftingCurrency.AUGMENTATION.itemId();

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
    void everyAliasPointsAtAShippedMaterialAndNoAliasIsStillShipped() throws Exception {
        JSONArray specialItems = csvRows("data/campaign/special_items.csv");
        Set<String> materialIds = new HashSet<>();
        for (int index = 0; index < specialItems.length(); index++) {
            JSONObject row = specialItems.getJSONObject(index);
            if (row.optString("plugin").endsWith("MaterialItemPlugin")) {
                materialIds.add(row.optString("id"));
            }
        }
        JSONArray aliases = csvRows(MaterialIdAliases.dataPath());
        assertTrue(aliases.length() > 0);
        for (int index = 0; index < aliases.length(); index++) {
            String alias = aliases.getJSONObject(index).getString("alias");
            String currentId = aliases.getJSONObject(index).getString("id");
            assertTrue(materialIds.contains(currentId), alias + " points at " + currentId + ", which is not a material in special_items.csv");
            assertFalse(materialIds.contains(alias), alias + " is still a material in special_items.csv");
        }
    }
}
