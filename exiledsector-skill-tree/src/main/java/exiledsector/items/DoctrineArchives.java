package exiledsector.items;

import exiledlib.ModCsv;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinition;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.ItemIdAliases;
import exiledlib.items.ItemModifiers;
import exiledlib.items.ItemType;
import exiledlib.items.ModifierScope;
import exiledlib.items.PoolEntry;
import exiledlib.modifiers.ModifierSlot;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.radius.RadiusSize;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class DoctrineArchives {

    public static final String DATA_PATH = "data/config/exiledSector/doctrine_archives.csv";
    static final String ID_COLUMN = "id";
    static final String BEHAVIOUR_COLUMN = "behaviour";
    static final String RADIUS_COLUMN = "radius";
    static final String FACTION_COLUMN = "faction";
    static final String NOTABLES_COLUMN = "notables";
    static final String KEYSTONES_COLUMN = "keystones";
    static final String SMALL_NODES_COLUMN = "small_nodes";
    static final String MODIFIERS_COLUMN = "modifiers";
    static final String MIN_LINES_COLUMN = "min_lines";
    static final String MAX_LINES_COLUMN = "max_lines";
    static final int DEFAULT_LINES = 1;
    public static final int WARDEN_MIN_TIER = 14;
    static final float WARDEN_CHANCE = 0.5f;
    private static final String ENTRY_SEPARATOR = ";";
    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,4}");
    private static final Logger LOG = Logger.getLogger(DoctrineArchives.class);
    private static final AtomicReference<Map<String, DoctrineArchive>> BY_ID = new AtomicReference<>(Map.of());
    private static final AtomicInteger GENERATION = new AtomicInteger();

    private DoctrineArchives() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, DATA_PATH, LOG, DoctrineArchives::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, DoctrineArchive> loaded = new LinkedHashMap<>();
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            try {
                DoctrineArchive archive = parse(row);
                if (loaded.putIfAbsent(archive.definitionId(), archive) != null) {
                    throw new IllegalArgumentException(archive.definitionId() + " is listed more than once");
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.SUBROUTINE)) {
            if (SubroutineKind.of(definition) == SubroutineKind.DOCTRINE && !loaded.containsKey(definition.id())) {
                LOG.error(definition.id() + " is a " + SubroutineKind.DOCTRINE.id() + " subroutine with no row in " + DATA_PATH
                        + ", so it rewrites nothing");
            }
        }
        BY_ID.set(Collections.unmodifiableMap(loaded));
        GENERATION.incrementAndGet();
    }

    static DoctrineArchive parse(JSONObject row) {
        String definitionId = ModCsv.text(row, ID_COLUMN);
        if (definitionId.isEmpty()) {
            throw new IllegalArgumentException("a row has no id");
        }
        ItemDefinition definition = ItemDefinitions.get(definitionId);
        if (definition == null || SubroutineKind.of(definition) != SubroutineKind.DOCTRINE) {
            throw new IllegalArgumentException(definitionId + " is not a " + SubroutineKind.DOCTRINE.id() + " subroutine in "
                    + SubroutineDefinitions.dataPath());
        }
        String behaviourText = ModCsv.text(row, BEHAVIOUR_COLUMN);
        DoctrineBehaviour behaviour = DoctrineBehaviour.byIdOrNull(behaviourText);
        if (behaviour == null) {
            throw new IllegalArgumentException(definitionId + " has behaviour \"" + behaviourText + "\"; expected one of " + DoctrineBehaviour.ids());
        }
        String radiusText = ModCsv.text(row, RADIUS_COLUMN);
        RadiusSize radiusSize = RadiusSize.byDataKeyOrNull(radiusText);
        if (radiusSize == null) {
            throw new IllegalArgumentException(definitionId + " has radius \"" + radiusText + "\"; expected one of " + radiusKeys());
        }
        String factionText = ModCsv.text(row, FACTION_COLUMN);
        String factionId = factionText.isEmpty() ? null : factionText;
        List<String> notableTypeIds = typeIds(ModCsv.text(row, NOTABLES_COLUMN));
        List<String> keystoneTypeIds = typeIds(ModCsv.text(row, KEYSTONES_COLUMN));
        List<String> smallTypeIds = typeIds(ModCsv.text(row, SMALL_NODES_COLUMN));
        String modifiersText = ModCsv.text(row, MODIFIERS_COLUMN);
        Integer minLines = lineCount(ModCsv.text(row, MIN_LINES_COLUMN));
        Integer maxLines = lineCount(ModCsv.text(row, MAX_LINES_COLUMN));
        if (minLines == null || maxLines == null || minLines < 1 || maxLines < minLines) {
            throw new IllegalArgumentException(definitionId + " needs min_lines of 1 or more and max_lines of at least min_lines");
        }
        boolean listsTypes = !notableTypeIds.isEmpty() || !keystoneTypeIds.isEmpty() || !smallTypeIds.isEmpty();
        if (behaviour == DoctrineBehaviour.TRANSFORM) {
            if (!listsTypes) {
                throw new IllegalArgumentException(definitionId + " transforms nodes but lists no replacement notables, keystones or small nodes");
            }
            if (!modifiersText.isEmpty()) {
                throw new IllegalArgumentException(definitionId + " transforms nodes, so it lists no modifiers");
            }
            return new DoctrineArchive(definitionId, behaviour, radiusSize, notableTypeIds, keystoneTypeIds, smallTypeIds, List.of(), minLines,
                    maxLines, factionId);
        }
        if (listsTypes) {
            throw new IllegalArgumentException(definitionId + " augments nodes, so it lists no replacement nodes");
        }
        List<PoolEntry> modifierPool = modifierPool(definitionId, modifiersText);
        if (modifierPool.isEmpty()) {
            throw new IllegalArgumentException(definitionId + " augments nodes but lists no modifiers");
        }
        return new DoctrineArchive(definitionId, behaviour, radiusSize, List.of(), List.of(), List.of(), modifierPool, minLines, maxLines,
                factionId);
    }

    private static String radiusKeys() {
        return String.join(", ", Arrays.stream(RadiusSize.values()).map(RadiusSize::dataKey).toList());
    }

    static List<String> typeIds(String poolText) {
        List<String> typeIds = new ArrayList<>();
        for (String entryText : poolText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (!trimmed.isEmpty() && !typeIds.contains(trimmed)) {
                typeIds.add(trimmed);
            }
        }
        return typeIds;
    }

    static Integer lineCount(String lineCountText) {
        if (lineCountText.isEmpty()) {
            return DEFAULT_LINES;
        }
        return WHOLE_NUMBER.matcher(lineCountText).matches() ? Integer.valueOf(lineCountText) : null;
    }

    private static List<PoolEntry> modifierPool(String definitionId, String modifiersText) {
        List<PoolEntry> pool = new ArrayList<>();
        for (String entryText : modifiersText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!ItemModifiers.isReference(trimmed)) {
                throw new IllegalArgumentException(definitionId + " lists \"" + trimmed + "\" inline; reference a modifier as @ID or @ID:weight");
            }
            try {
                pool.add(PoolEntry.parse(trimmed, ModifierSlot.PREFIX, ModifierScope.DOCTRINE));
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping modifier " + trimmed + " of " + definitionId + " in " + DATA_PATH + ": " + e.getMessage());
            }
        }
        return pool;
    }

    public static DoctrineArchive get(String definitionId) {
        return definitionId == null ? null : BY_ID.get().get(ItemIdAliases.currentId(definitionId));
    }

    public static Collection<DoctrineArchive> all() {
        return BY_ID.get().values();
    }

    public static ItemData rollForWarden(int chartTier, String wardenFactionId, float chanceMult, Random random,
                                         Predicate<ItemDefinition> allowed) {
        float chance = Math.min(1f, WARDEN_CHANCE * Math.max(0f, chanceMult));
        if (chartTier < WARDEN_MIN_TIER || !(random.nextFloat() < chance)) {
            return null;
        }
        ItemDefinition definition = pickFor(wardenFactionId, random, allowed);
        return definition == null ? null : ItemData.rolled(definition, random.nextLong());
    }

    static ItemDefinition pickFor(String factionId, Random random, Predicate<ItemDefinition> allowed) {
        List<ItemDefinition> candidates = new ArrayList<>();
        List<ItemDefinition> factionMatches = new ArrayList<>();
        for (DoctrineArchive archive : all()) {
            ItemDefinition definition = ItemDefinitions.get(archive.definitionId());
            if (definition == null || !allowed.test(definition)) {
                continue;
            }
            candidates.add(definition);
            if (sameFaction(archive.factionId(), factionId)) {
                factionMatches.add(definition);
            }
        }
        List<ItemDefinition> pool = factionMatches.isEmpty() ? candidates : factionMatches;
        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
    }

    private static boolean sameFaction(String archiveFactionId, String factionId) {
        if (archiveFactionId == null || factionId == null) {
            return false;
        }
        String region = NpcFactionVolumes.regionFor(factionId);
        return archiveFactionId.equals(factionId) || (region != null && region.equals(NpcFactionVolumes.regionFor(archiveFactionId)));
    }

    public static int generation() {
        return GENERATION.get();
    }

    public static void clear() {
        BY_ID.set(Map.of());
        GENERATION.incrementAndGet();
    }
}
