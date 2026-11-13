package exiledsector.charts.anomaly.listeningpost;

import exiledsector.ProjectFiles;
import exiledsector.items.HullItem;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemImplicits;
import exiledsector.items.ItemModifiers;
import exiledsector.items.PrototypeImplicits;
import exiledsector.items.SubroutineDefinitions;
import exiledsector.skills.skilleffect.EffectAliases;
import org.json.CDL;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VaultPrototypesTest {

    @BeforeEach
    void loadRealData() throws Exception {
        EffectAliases.register(csvRows(EffectAliases.dataPath()));
        ItemModifiers.register(csvRows(ItemModifiers.dataPath()));
        ItemDefinitions.register(csvRows(SubroutineDefinitions.dataPath()), csvRows(HullItem.definitionsPath()));
        ItemImplicits.register(csvRows(ItemImplicits.dataPath()));
        PrototypeImplicits.register(csvRows(PrototypeImplicits.dataPath()));
    }

    @AfterEach
    void clear() {
        ItemImplicits.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    @Test
    void theVaultOffersThreePrototypesOfDifferentBases() {
        List<ItemData> offered = VaultPrototypes.roll(VaultPrototypes.PICKS, 42L);

        assertEquals(VaultPrototypes.PICKS, offered.size());
        assertEquals(VaultPrototypes.PICKS, offered.stream().map(ItemData::definitionId).distinct().count());
        offered.forEach(prototype -> assertNotNull(PrototypeImplicits.prototypeImplicit(prototype.preview()), prototype.definitionId()));
    }

    @Test
    void theSameVaultOffersTheSamePrototypes() {
        assertEquals(encoded(VaultPrototypes.roll(VaultPrototypes.PICKS, 42L)), encoded(VaultPrototypes.roll(VaultPrototypes.PICKS, 42L)));
    }

    @Test
    void theOfferStopsWhenTheBasesRunOut() {
        int baseCount = PrototypeImplicits.bases().size();

        assertEquals(baseCount, VaultPrototypes.roll(baseCount + 5, 7L).size());
    }

    @Test
    void basesThatCannotDropAreNeverOffered() {
        String blocked = PrototypeImplicits.bases().get(0);
        List<ItemData> offered = VaultPrototypes.roll(PrototypeImplicits.bases().size(), 42L, definition -> !definition.id().equals(blocked));

        assertEquals(PrototypeImplicits.bases().size() - 1, offered.size());
        offered.forEach(prototype -> assertNotEquals(blocked, prototype.definitionId()));
    }

    private static List<String> encoded(List<ItemData> prototypes) {
        return prototypes.stream().map(ItemData::encode).toList();
    }

    private static JSONArray csvRows(String dataPath) throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }
}
