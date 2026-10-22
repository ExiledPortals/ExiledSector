package exiledsector.items;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledlib.HostMod;
import exiledlib.items.Item;
import exiledlib.items.ItemDefinition;
import exiledlib.items.ItemKind;
import exiledlib.items.ItemType;
import exiledlib.items.ItemTypeSupport;
import exiledlib.items.RolledModifier;
import org.json.JSONObject;

import java.util.List;

public final class HullItem extends Item {

    public static final ItemTypeSupport SUPPORT = new ItemTypeSupport() {
        @Override
        public ItemType type() {
            return ItemType.HULL_ITEM;
        }

        @Override
        public Class<? extends Item> itemClass() {
            return HullItem.class;
        }

        @Override
        public String saveAlias() {
            return "exiledSector.HullItem";
        }

        @Override
        public Item create(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
            return new HullItem(id, definitionId, seed, explicitModifiers);
        }

        @Override
        public Item convert(Item source) {
            return new HullItem(source);
        }

        @Override
        public String definitionsPath() {
            return HullItem.definitionsPath();
        }

        @Override
        public ItemDefinition parseDefinition(JSONObject row) {
            return HullItem.parseDefinition(row);
        }

        @Override
        public boolean available(SectorAPI sector) {
            return HullUpgradesSetting.sectionsOpen(sector);
        }

        @Override
        public int uniqueMinPlayerLevel() {
            return HullUpgradesSetting.unlockLevel();
        }
    };

    static final String DEFINITIONS_FILE = "hull_items.csv";

    HullItem(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
    }

    HullItem(Item source) {
        super(source);
    }

    public static String definitionsPath() {
        return HostMod.configPath(DEFINITIONS_FILE);
    }

    static ItemDefinition parseDefinition(JSONObject row) {
        String sectionId = row.optString("section", "").trim();
        ItemKind kind = ItemKind.byIdOrNull(sectionId);
        if (kind == null || kind.type() != ItemType.HULL_ITEM) {
            throw new IllegalArgumentException("section \"" + sectionId + "\" is not a ship section");
        }
        return ItemDefinition.parse(row, kind);
    }

    @Override
    public ItemType type() {
        return ItemType.HULL_ITEM;
    }

    public ShipSection section() {
        return ShipSection.of(kind());
    }

    public boolean canSocketInto(ShipSection section) {
        return section != null && section == section();
    }

    public static boolean fits(Item item, ShipSection section) {
        return item instanceof HullItem hullItem && hullItem.canSocketInto(section);
    }
}
