package exiledsector.items;

import exiledsector.ModCsv;
import org.json.JSONException;

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
    public void checkItemFiles(ItemDataIntegrity integrity) throws IOException, JSONException {
        integrity.checkDefinitions(ItemDataIntegrity.orEmpty(ModCsv.rows("id", HullItem.definitionsPath())), HullItem.definitionsPath(),
                ItemType.HULL_ITEM);
        integrity.checkKindImplicits(ItemDataIntegrity.orEmpty(ModCsv.rows("section", ItemImplicits.dataPath())), ItemImplicits.dataPath());
        integrity.checkCorruptionPool(ModCsv.rows(CorruptionImplicitPool.IMPLICIT_COLUMN, ItemImplicits.corruptionDataPath()),
                ItemImplicits.corruptionDataPath(), "the hull item corruption pool", ItemType.HULL_ITEM);
    }
}
