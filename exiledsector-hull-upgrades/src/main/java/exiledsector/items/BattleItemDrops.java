package exiledsector.items;

import exiledlib.ModCsv;
import exiledlib.ModSettings;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public final class BattleItemDrops {

    public static final String DATA_PATH = "data/config/exiledSector/battle_item_drops.csv";
    public static final String DROP_PERCENT_FIELD_ID = "exiledSector_battleItemDropPercent";
    public static final int DEFAULT_DROP_PERCENT = 100;
    public static final int MAX_DROP_PERCENT = 1000;
    static final String DROP_COLUMN = "drop";
    static final String RATE_COLUMN = "per_100_power";
    static final float NODES_PER_DOUBLING = 30f;
    static final int MAX_ITEMS_PER_BATTLE = 25;
    private static final float POWER_PER_RATE_STEP = 100f;
    private static final Logger LOG = Logger.getLogger(BattleItemDrops.class);
    private static final AtomicReference<Map<Drop, Float>> RATES = new AtomicReference<>(Map.of());

    public enum Drop {
        MODS_1("mods_1", 1, false),
        MODS_2("mods_2", 2, false),
        REFIT_SCHEMATIC("refit_schematic", 0, false),
        MASTER_SCHEMATIC("master_schematic", 0, false),
        MODS_3("mods_3", 3, true),
        MODS_4("mods_4", 4, true),
        UNIQUE("unique", 0, true);

        private final String id;
        private final int effectCount;
        private final boolean rare;

        Drop(String id, int effectCount, boolean rare) {
            this.id = id;
            this.effectCount = effectCount;
            this.rare = rare;
        }

        public boolean rare() {
            return rare;
        }

        public String id() {
            return id;
        }

        public int effectCount() {
            return effectCount;
        }

        static Drop byIdOrNull(String id) {
            for (Drop drop : values()) {
                if (drop.id.equals(id)) {
                    return drop;
                }
            }
            return null;
        }
    }

    public record SourcePower(float power, ItemChances chances) {

        public SourcePower {
            power = Math.max(0f, power);
            chances = chances == null ? ItemChances.BASE : chances;
        }
    }

    public record Battle(List<SourcePower> sources, String uniqueFactionId) {

        public Battle {
            sources = sources == null ? List.of() : List.copyOf(sources);
        }

        public boolean isEmpty() {
            return sources.stream().noneMatch(source -> source.power() > 0f);
        }
    }


    record Planned(Drop drop, ItemKind kind) {
    }

    private BattleItemDrops() {
    }

    public static void load() {
        ModCsv.load(DROP_COLUMN, DATA_PATH, LOG, BattleItemDrops::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<Drop, Float> rates = new EnumMap<>(Drop.class);
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            String dropId = ModCsv.text(row, DROP_COLUMN);
            Drop drop = Drop.byIdOrNull(dropId);
            float rate = CsvRows.numberOrNaN(ModCsv.text(row, RATE_COLUMN));
            if (drop == null) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": unknown drop \"" + dropId + "\"");
            } else if (!(rate >= 0f) || Float.isInfinite(rate)) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + dropId + " needs a rate of 0 or more");
            } else {
                rates.put(drop, rate);
            }
        });
        RATES.set(Collections.unmodifiableMap(rates));
    }

    public static void clear() {
        RATES.set(Map.of());
    }

    public static float rate(Drop drop) {
        return RATES.get().getOrDefault(drop, 0f);
    }

    public static float settingMultiplier() {
        return Math.max(0, ModSettings.intOr(DROP_PERCENT_FIELD_ID, DEFAULT_DROP_PERCENT)) / 100f;
    }

    public static float shipPower(float deploymentPoints, int allocatedNodeCount) {
        return Math.max(0f, deploymentPoints) * (1f + Math.max(0, allocatedNodeCount) / NODES_PER_DOUBLING);
    }

    public static ItemDrops.SectionItemLoot roll(Battle battle, float dropMultiplier, Random random, Predicate<ItemDefinition> uniqueAllowed) {
        return roll(battle, dropMultiplier, random, uniqueAllowed, true);
    }

    public static ItemDrops.SectionItemLoot roll(Battle battle, float dropMultiplier, Random random, Predicate<ItemDefinition> uniqueAllowed,
                                                 boolean sectionsOpen) {
        if (battle == null || battle.isEmpty() || !(dropMultiplier > 0f)) {
            return ItemDrops.SectionItemLoot.NONE;
        }
        List<ItemData> items = new ArrayList<>();
        List<ShipCurrency> upgrades = new ArrayList<>();
        List<ItemKind> kindsWithBasics = sectionsOpen ? ItemDrops.sectionKindsWithBasics() : List.of();
        for (Planned planned : plan(battle.sources(), dropMultiplier, kindsWithBasics, random, sectionsOpen)) {
            if (planned.drop() == Drop.REFIT_SCHEMATIC) {
                upgrades.add(RefitSchematic.roll(random));
            } else if (planned.drop() == Drop.MASTER_SCHEMATIC) {
                upgrades.add(MasterSchematic.INSTANCE);
            } else if (planned.drop() == Drop.UNIQUE) {
                ItemDefinition definition = ItemDrops.pickUniqueFromFaction(battle.uniqueFactionId(), random,
                        candidate -> candidate.type() == ItemType.HULL_ITEM && uniqueAllowed.test(candidate));
                if (definition != null) {
                    items.add(ItemData.rolled(definition, random.nextLong()));
                }
            } else {
                int effectCount = planned.drop().effectCount();
                Predicate<ItemDefinition> holdsCount = candidate -> ItemRoller.canRollEffectCount(candidate, effectCount);
                ItemDefinition definition = planned.kind() == null ? ItemDrops.pickSectionItemBasic(random, holdsCount)
                        : ItemDrops.pickSectionItemBasic(planned.kind(), random, holdsCount);
                if (definition != null) {
                    items.add(ItemData.rolledWithEffectCount(definition, random.nextLong(), effectCount));
                }
            }
        }
        return new ItemDrops.SectionItemLoot(items, upgrades);
    }

    static List<Planned> plan(List<SourcePower> sources, float dropMultiplier, List<ItemKind> kindsWithBasics, Random random) {
        return plan(sources, dropMultiplier, kindsWithBasics, random, true);
    }

    static List<Planned> plan(List<SourcePower> sources, float dropMultiplier, List<ItemKind> kindsWithBasics, Random random,
                              boolean sectionsOpen) {
        List<Planned> planned = new ArrayList<>();
        for (Drop drop : Drop.values()) {
            if (!allowed(drop, sectionsOpen)) {
                continue;
            }
            float expected = 0f;
            for (SourcePower source : sources) {
                expected += expected(source, drop, dropMultiplier) * source.chances().general();
            }
            addPlanned(planned, drop, null, ItemDrops.wholeParts(expected, random));
        }
        for (ItemKind kind : kindsWithBasics) {
            for (Drop drop : Drop.values()) {
                if (drop.effectCount() == 0) {
                    continue;
                }
                float expected = 0f;
                for (SourcePower source : sources) {
                    expected += expected(source, drop, dropMultiplier) * source.chances().targeted(kind) / kindsWithBasics.size();
                }
                addPlanned(planned, drop, kind, ItemDrops.wholeParts(expected, random));
            }
        }
        return capped(planned);
    }

    static boolean allowed(Drop drop, boolean sectionsOpen) {
        return switch (drop) {
            case MASTER_SCHEMATIC -> MasterSchematic.required();
            case REFIT_SCHEMATIC -> sectionsOpen && RefitSchematic.dropsAllowed();
            default -> sectionsOpen;
        };
    }

    private static float expected(SourcePower source, Drop drop, float dropMultiplier) {
        float rarityMult = drop.rare() ? source.chances().rarity() : 1f;
        return source.power() / POWER_PER_RATE_STEP * rate(drop) * dropMultiplier * rarityMult;
    }

    private static void addPlanned(List<Planned> planned, Drop drop, ItemKind kind, int count) {
        for (int i = 0; i < count; i++) {
            planned.add(new Planned(drop, kind));
        }
    }

    static List<Planned> capped(List<Planned> planned) {
        if (planned.size() <= MAX_ITEMS_PER_BATTLE) {
            return planned;
        }
        List<Planned> byCutOrder = new ArrayList<>(planned);
        byCutOrder.sort(Comparator.comparingInt(entry -> entry.drop().ordinal()));
        return List.copyOf(byCutOrder.subList(byCutOrder.size() - MAX_ITEMS_PER_BATTLE, byCutOrder.size()));
    }
}
