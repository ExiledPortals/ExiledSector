package exiledsector.items;

import exiledlib.ModCsv;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public final class HullItemDataRules implements ItemDataRules {

    public static final HullItemDataRules INSTANCE = new HullItemDataRules();

    private HullItemDataRules() {
    }

    @Override
    public ItemType type() {
        return ItemType.HULL_ITEM;
    }

    @Override
    public void checkDefinitionRow(ItemDataIntegrity integrity, String file, int line, String definitionId, JSONObject row) {
        String sourcesText = ModCsv.text(row, HullItem.UNIQUE_SOURCE_COLUMN);
        integrity.checkUniqueSources(file, line, definitionId, sourcesText);
        if (!sourcesText.isEmpty() && !"true".equalsIgnoreCase(ModCsv.text(row, "unique"))) {
            integrity.warning(file, line, definitionId + " names a source, but sources only apply to unique hull items");
        }
        String mechanic = ModCsv.text(row, HullItem.MECHANIC_COLUMN);
        if (!mechanic.isEmpty() && !HullItem.DOUBLER_MECHANIC.equals(mechanic)) {
            integrity.error(file, line, definitionId + " has an unknown mechanic \"" + mechanic + "\"; the only one is " + HullItem.DOUBLER_MECHANIC);
        } else if (!mechanic.isEmpty() && !"true".equalsIgnoreCase(ModCsv.text(row, "unique"))) {
            integrity.warning(file, line, definitionId + " names a mechanic, but mechanics are meant for unique hull items");
        }
    }

    @Override
    public void checkItemFiles(ItemDataIntegrity integrity) throws IOException, JSONException {
        integrity.checkDefinitions(ItemDataIntegrity.orEmpty(ModCsv.rows("id", HullItem.definitionsPath())), HullItem.definitionsPath(),
                ItemType.HULL_ITEM);
        integrity.checkKindImplicits(ItemDataIntegrity.orEmpty(ModCsv.rows("section", ItemImplicits.dataPath())), ItemImplicits.dataPath());
        integrity.checkCorruptionPool(ModCsv.rows(CorruptionImplicitPool.IMPLICIT_COLUMN, ItemImplicits.corruptionDataPath()),
                ItemImplicits.corruptionDataPath(), "the hull item corruption pool", ItemType.HULL_ITEM);
    }
}
