package exiledsector.items;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledlib.HostMod;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

public final class HullItem extends Item {

    public static final ItemTypeSupport SUPPORT = new BasicItemTypeSupport(ItemType.HULL_ITEM, HullItem.class, "exiledSector.HullItem",
            HullItem::new, HullItem::new) {
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
    static final String UNIQUE_SOURCE_COLUMN = "source";
    static final String MECHANIC_COLUMN = "mechanic";
    public static final String DOUBLER_MECHANIC = "doubler";

    private String targetSectionId;

    HullItem(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
    }

    HullItem(Item source) {
        super(source);
        copyTraitsFrom(source);
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
        return ItemDefinition.parse(row, kind, UniqueSource.parseList(row.optString(UNIQUE_SOURCE_COLUMN, "")))
                .withMechanic(row.optString(MECHANIC_COLUMN, ""));
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

    public boolean doubler() {
        ItemDefinition definition = definition();
        return definition != null && DOUBLER_MECHANIC.equals(definition.mechanic());
    }

    public List<ShipSection> targetChoices() {
        ShipSection ownSection = section();
        return ShipSection.all().stream().filter(choice -> choice != ownSection).toList();
    }

    public ShipSection targetSection() {
        List<ShipSection> choices = doubler() ? targetChoices() : List.of();
        if (choices.isEmpty()) {
            return null;
        }
        ShipSection target = ShipSection.byIdOrNull(targetSectionId);
        if (target == null || !choices.contains(target)) {
            target = choices.get(new SplittableRandom(seed()).nextInt(choices.size()));
            targetSectionId = target.id();
        }
        return target;
    }

    @Override
    public boolean hasRerollableTarget() {
        return doubler() && targetChoices().size() > 1;
    }

    @Override
    public boolean rerollTarget(Random random) {
        if (!hasRerollableTarget()) {
            return false;
        }
        ShipSection current = targetSection();
        List<ShipSection> others = targetChoices().stream().filter(choice -> choice != current).toList();
        targetSectionId = others.get(random.nextInt(others.size())).id();
        return true;
    }

    @Override
    public void copyTraitsFrom(Item source) {
        if (source instanceof HullItem hullItem) {
            targetSectionId = hullItem.targetSectionId;
        }
    }

    @Override
    public List<StyledText> implicitLines(boolean withRollRanges) {
        ShipSection target = targetSection();
        if (target == null) {
            return super.implicitLines(withRollRanges);
        }
        List<StyledText> lines = new ArrayList<>();
        lines.add(Translation.msg("hullItem.doubler.line").arg("section", target.displayName()).styled());
        if (withRollRanges) {
            String choices = targetChoices().stream().map(ShipSection::displayName).collect(Collectors.joining(", "));
            lines.add(Translation.msg("hullItem.doubler.choices").arg("sections", choices).styled());
        }
        lines.addAll(super.implicitLines(withRollRanges));
        return lines;
    }

    public static boolean fits(Item item, ShipSection section) {
        return item instanceof HullItem hullItem && hullItem.canSocketInto(section);
    }
}
