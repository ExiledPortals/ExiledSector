package exiledsector.items;

import exiledsector.items.modifier.ItemModifiers;
import exiledsector.items.subroutine.Subroutine;
import exiledsector.items.subroutine.doctrine.DoctrineArchives;
import exiledsector.skills.ClearedSkillTreeExtension;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ClearedSkillTreeExtension.class)
class InfestedAnchorConversionTest {

    @BeforeEach
    void setUp() throws Exception {
        RealSkillData.load();
        ItemIdAliases.register(HullItemRows.shippedRows(ItemIdAliases.dataPath()));
        ItemFixtures.registerShippedItems();
        DoctrineArchives.register(HullItemRows.shippedRows(DoctrineArchives.DATA_PATH));
    }

    @AfterEach
    void tearDown() {
        DoctrineArchives.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
        ItemIdAliases.clear();
    }

    @ParameterizedTest
    @CsvSource({"wormhole_anchor_threat, doctrine_archive_threat", "wormhole_anchor_shrouded, doctrine_archive_shrouded"})
    void aSavedRetiredAnchorLoadsAsItsCorruptedInfestedArchiveWithTheSameSeed(String anchorId, String archiveId) {
        Object loaded = new UnresolvedItem("saved_anchor", anchorId, 4471L, List.of()).readResolve();

        Subroutine archive = assertInstanceOf(Subroutine.class, loaded);
        assertEquals(archiveId, archive.definitionId());
        assertEquals("saved_anchor", archive.id());
        assertEquals(4471L, archive.seed());
        assertTrue(archive.corrupted());
    }
}
