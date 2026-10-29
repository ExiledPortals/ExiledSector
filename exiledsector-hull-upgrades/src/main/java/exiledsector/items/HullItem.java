package exiledsector.items;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.ExiledSector;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.tags.NodeRequirements;
import exiledsector.skills.tags.ShipProfile;
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
    public static final List<String> MECHANICS = List.of(ItemDefinition.ONE_OF_MECHANIC);
    static final String CREW_COLUMN = "crew";
    public static final String CREWED = "crewed";
    public static final String AUTOMATED = "automated";
    public static final List<String> CREW_VALUES = List.of(CREWED, AUTOMATED);

    private String targetSectionId;
    private transient List<List<RolledModifier>> targetCheckedModifiers;
    private transient boolean cachedTargetsAnotherItem;

    HullItem(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
    }

    HullItem(Item source) {
        super(source);
        copyTraitsFrom(source);
    }

    public static String definitionsPath() {
        return ExiledSector.configPath(DEFINITIONS_FILE);
    }

    static ItemDefinition parseDefinition(JSONObject row) {
        String sectionId = row.optString("section", "").trim();
        ItemKind kind = ItemKind.byIdOrNull(sectionId);
        if (kind == null || kind.type() != ItemType.HULL_ITEM) {
            throw new IllegalArgumentException("section \"" + sectionId + "\" is not a ship section");
        }
        String crew = row.optString(CREW_COLUMN, "").trim();
        if (!crew.isEmpty() && !CREW_VALUES.contains(crew)) {
            throw new IllegalArgumentException("crew \"" + crew + "\" is not one of " + String.join(", ", CREW_VALUES));
        }
        return ItemDefinition.parse(row, kind, UniqueSource.parseList(row.optString(UNIQUE_SOURCE_COLUMN, "")))
                .withMechanic(row.optString(MECHANIC_COLUMN, "")).withArea(row.optString(ItemAreas.AREA_COLUMN, "")).withCrew(crew);
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

    public boolean canSocketInto(ShipSection section, ShipProfile fit) {
        return canSocketInto(section) && unmetRequirement(fit) == null;
    }

    public String requirementTag() {
        ItemDefinition definition = definition();
        String crew = definition == null ? null : definition.crew();
        if (CREWED.equals(crew)) {
            return NodeRequirements.CREWED;
        }
        return AUTOMATED.equals(crew) ? NodeRequirements.AUTOMATED : null;
    }

    public String unmetRequirement(ShipProfile fit) {
        String requirementTag = requirementTag();
        if (requirementTag == null || fit == null) {
            return null;
        }
        return NodeRequirements.firstUnmet(List.of(requirementTag), fit);
    }

    @Override
    public StyledText limitLine() {
        String requirementTag = requirementTag();
        return requirementTag == null ? null : Translation.styled("hullItem.crew." + requirementTag);
    }

    public boolean targetsAnotherItem() {
        List<List<RolledModifier>> modifiers = List.of(explicits(), implicits());
        if (!modifiers.equals(targetCheckedModifiers)) {
            targetCheckedModifiers = List.of(List.copyOf(modifiers.get(0)), List.copyOf(modifiers.get(1)));
            cachedTargetsAnotherItem = skillEffects().stream().anyMatch(effect -> effect.effect().targetsSocketedItem());
        }
        return cachedTargetsAnotherItem;
    }

    public List<ShipSection> targetChoices() {
        ShipSection ownSection = section();
        return ShipSection.all().stream().filter(choice -> choice != ownSection).toList();
    }

    public ShipSection targetSection() {
        List<ShipSection> choices = targetsAnotherItem() ? targetChoices() : List.of();
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
    public boolean hasTunableTrait() {
        return targetsAnotherItem() && targetChoices().size() > 1;
    }

    @Override
    public boolean retuneTrait(Random random) {
        if (!hasTunableTrait()) {
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
        } else if (source instanceof UnresolvedItem unresolved && unresolved.targetId() != null) {
            targetSectionId = unresolved.targetId();
        }
    }

    @Override
    public void keepWhenUnresolved(UnresolvedItem unresolved) {
        unresolved.keepTarget(targetSectionId);
    }

    @Override
    public List<StyledText> implicitLines(boolean withRollRanges) {
        ShipSection target = targetSection();
        if (target == null) {
            return super.implicitLines(withRollRanges);
        }
        List<StyledText> lines = new ArrayList<>();
        lines.add(Translation.msg("hullItem.target.line").arg("section", target.displayName()).styled());
        if (withRollRanges) {
            String choices = targetChoices().stream().map(ShipSection::displayName).collect(Collectors.joining(", "));
            lines.add(Translation.msg("hullItem.target.choices").arg("sections", choices).styled());
        }
        lines.addAll(super.implicitLines(withRollRanges));
        return lines;
    }

    public static boolean fits(Item item, ShipSection section) {
        return item instanceof HullItem hullItem && hullItem.canSocketInto(section);
    }
}
