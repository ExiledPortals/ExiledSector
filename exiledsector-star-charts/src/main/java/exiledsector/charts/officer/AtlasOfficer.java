package exiledsector.charts.officer;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.officer.reward.CarriedItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record AtlasOfficer(int recordId, String factionId, long nameSeed, long portraitSeed, String rankId, AtlasCell commandCentre,
                           String traitId, String flagshipHullId, String flagshipVariantId, long shipNameSeed, long treeSeed,
                           List<String> dmodIds, Map<String, Integer> scars, List<String> carriedItems, List<String> pendingChoice,
                           int defeats, boolean lastChance, Status status, boolean heldCommand, List<String> history, int renown,
                           boolean identified, int seat, String portraitPath) {

    public static final String RUN_DATA_KEY = "atlas_officer";
    public static final int HISTORY_LENGTH = 3;
    public static final int UNSEATED = -1;
    static final String FORMAT = "o3";
    static final String PORTRAITLESS_FORMAT = "o2";
    static final String SEATLESS_FORMAT = "o1";
    static final String FIELD_SEPARATOR = "|";
    static final String LIST_SEPARATOR = ",";
    static final String COUNT_SEPARATOR = "=";
    private static final int FIELD_COUNT = 24;
    private static final int PORTRAITLESS_FIELD_COUNT = 23;
    private static final int SEATLESS_FIELD_COUNT = 22;

    public enum Status {
        ACTIVE,
        WAITING
    }

    public AtlasOfficer {
        dmodIds = List.copyOf(dmodIds);
        scars = Map.copyOf(scars);
        carriedItems = List.copyOf(carriedItems);
        history = List.copyOf(history.subList(Math.max(0, history.size() - HISTORY_LENGTH), history.size()));
        pendingChoice = List.copyOf(pendingChoice);
        portraitPath = portraitPath == null || portraitPath.contains(FIELD_SEPARATOR) ? "" : portraitPath;
    }

    public static AtlasOfficer waiting(int recordId, String factionId, String rankId, String traitId, OfficerSeeds seeds,
                                       FlagshipCatalogue.Flagship flagship) {
        return new AtlasOfficer(recordId, factionId, seeds.nameSeed(), seeds.portraitSeed(), rankId, null, traitId,
                flagship == null ? "" : flagship.hullId(), flagship == null ? "" : flagship.variantId(), seeds.shipNameSeed(), seeds.treeSeed(),
                List.of(), Map.of(), List.of(), List.of(), 0, false, Status.WAITING, false, List.of(), 0, false, UNSEATED, "");
    }

    public record OfficerSeeds(long nameSeed, long portraitSeed, long shipNameSeed, long treeSeed) {
    }

    public boolean isActive() {
        return status == Status.ACTIVE && commandCentre != null;
    }

    public boolean hasFled() {
        return status == Status.WAITING && heldCommand;
    }

    public boolean neverPlaced() {
        return status == Status.WAITING && !heldCommand;
    }

    public boolean isSeated() {
        return seat >= 0;
    }

    public boolean hasFlagship() {
        return !flagshipVariantId.isEmpty();
    }

    public AtlasOfficer takingCommand(AtlasCell centre, String historyEntry) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, centre, traitId, flagshipHullId, flagshipVariantId,
                shipNameSeed, treeSeed, dmodIds, scars, carriedItems, pendingChoice, defeats, lastChance, Status.ACTIVE, true,
                appended(historyEntry), renown, identified, seat, portraitPath);
    }

    public AtlasOfficer takingOverFrom(AtlasOfficer predecessor, String historyEntry) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, predecessor.rankId, predecessor.commandCentre, traitId, flagshipHullId,
                flagshipVariantId, shipNameSeed, treeSeed, dmodIds, scars, predecessor.carriedItems, predecessor.pendingChoice, defeats, lastChance,
                Status.ACTIVE, true, appended(historyEntry), predecessor.renown, predecessor.identified, predecessor.seat, portraitPath);
    }

    public AtlasOfficer waitingInPlaceOf(AtlasOfficer predecessor) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, predecessor.rankId, null, traitId, flagshipHullId, flagshipVariantId,
                shipNameSeed, treeSeed, dmodIds, scars, carriedItems, predecessor.pendingChoice, defeats, lastChance, Status.WAITING, false, history,
                predecessor.renown, predecessor.identified, predecessor.seat, portraitPath);
    }

    public AtlasOfficer losingCommand(String historyEntry) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, null, traitId, flagshipHullId, flagshipVariantId,
                shipNameSeed, treeSeed, dmodIds, scars, carriedItems, pendingChoice, defeats, lastChance, Status.WAITING, heldCommand,
                appended(historyEntry), renown, identified, seat, portraitPath);
    }

    public AtlasOfficer seatedAt(String newRankId, int newSeat) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, newRankId, commandCentre, traitId, flagshipHullId,
                flagshipVariantId, shipNameSeed, treeSeed, dmodIds, scars, carriedItems, pendingChoice, defeats, lastChance, status,
                heldCommand, history, renown, identified, newSeat, portraitPath);
    }

    public AtlasOfficer identifiedNow() {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, commandCentre, traitId, flagshipHullId,
                flagshipVariantId, shipNameSeed, treeSeed, dmodIds, scars, carriedItems, pendingChoice, defeats, lastChance, status,
                heldCommand, history, renown, true, seat, portraitPath);
    }

    public AtlasOfficer withRenown(int newRenown) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, commandCentre, traitId, flagshipHullId,
                flagshipVariantId, shipNameSeed, treeSeed, dmodIds, scars, carriedItems, pendingChoice, defeats, lastChance, status,
                heldCommand, history, Math.max(0, newRenown), identified, seat, portraitPath);
    }

    public AtlasOfficer withLastChance(boolean marked) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, commandCentre, traitId, flagshipHullId,
                flagshipVariantId, shipNameSeed, treeSeed, dmodIds, scars, carriedItems, pendingChoice, defeats, marked, status,
                heldCommand, history, renown, identified, seat, portraitPath);
    }

    public AtlasOfficer withDefeats(int defeatCount) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, commandCentre, traitId, flagshipHullId,
                flagshipVariantId, shipNameSeed, treeSeed, dmodIds, scars, carriedItems, pendingChoice, defeatCount, lastChance, status,
                heldCommand, history, renown, identified, seat, portraitPath);
    }

    public AtlasOfficer withHistory(String historyEntry) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, commandCentre, traitId, flagshipHullId,
                flagshipVariantId, shipNameSeed, treeSeed, dmodIds, scars, carriedItems, pendingChoice, defeats, lastChance, status,
                heldCommand, appended(historyEntry), renown, identified, seat, portraitPath);
    }

    public AtlasOfficer withScars(Map<String, Integer> newScars) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, commandCentre, traitId, flagshipHullId,
                flagshipVariantId, shipNameSeed, treeSeed, dmodIds, newScars, carriedItems, pendingChoice, defeats, lastChance, status,
                heldCommand, history, renown, identified, seat, portraitPath);
    }

    public AtlasOfficer withDModIds(List<String> newDModIds) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, commandCentre, traitId, flagshipHullId,
                flagshipVariantId, shipNameSeed, treeSeed, newDModIds, scars, carriedItems, pendingChoice, defeats, lastChance, status,
                heldCommand, history, renown, identified, seat, portraitPath);
    }

    public AtlasOfficer flying(FlagshipCatalogue.Flagship flagship, List<String> newDModIds) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, commandCentre, traitId, flagship.hullId(),
                flagship.variantId(), shipNameSeed, treeSeed, newDModIds, scars, carriedItems, pendingChoice, defeats, lastChance, status,
                heldCommand, history, renown, identified, seat, portraitPath);
    }

    public AtlasOfficer withCarried(List<CarriedItem> newCarried) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, commandCentre, traitId, flagshipHullId,
                flagshipVariantId, shipNameSeed, treeSeed, dmodIds, scars, tokens(newCarried), pendingChoice, defeats, lastChance, status,
                heldCommand, history, renown, identified, seat, portraitPath);
    }

    public boolean hasPortrait() {
        return !portraitPath.isEmpty();
    }

    public AtlasOfficer withPortrait(String newPortraitPath) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, commandCentre, traitId, flagshipHullId,
                flagshipVariantId, shipNameSeed, treeSeed, dmodIds, scars, carriedItems, pendingChoice, defeats, lastChance, status,
                heldCommand, history, renown, identified, seat, newPortraitPath);
    }

    public AtlasOfficer withPendingOffers(List<CarriedItem> newOffers) {
        return new AtlasOfficer(recordId, factionId, nameSeed, portraitSeed, rankId, commandCentre, traitId, flagshipHullId,
                flagshipVariantId, shipNameSeed, treeSeed, dmodIds, scars, carriedItems, tokens(newOffers), defeats, lastChance, status,
                heldCommand, history, renown, identified, seat, portraitPath);
    }

    public List<CarriedItem> carried() {
        return items(carriedItems);
    }

    public List<CarriedItem> pendingOffers() {
        return items(pendingChoice);
    }

    public boolean hasPendingChoice() {
        return !pendingOffers().isEmpty();
    }

    private static List<String> tokens(List<CarriedItem> items) {
        List<String> tokens = new ArrayList<>();
        for (CarriedItem item : items) {
            tokens.add(item.toToken());
        }
        return tokens;
    }

    private static List<CarriedItem> items(List<String> tokens) {
        List<CarriedItem> items = new ArrayList<>();
        for (String token : tokens) {
            CarriedItem item = CarriedItem.fromToken(token);
            if (item != null) {
                items.add(item);
            }
        }
        return List.copyOf(items);
    }

    private List<String> appended(String historyEntry) {
        List<String> entries = new ArrayList<>(history);
        if (historyEntry != null && !historyEntry.isEmpty()) {
            entries.add(historyEntry);
        }
        return entries;
    }

    public String encode() {
        return String.join(FIELD_SEPARATOR, FORMAT, factionId, seed(nameSeed), seed(portraitSeed), rankId,
                commandCentre == null ? "" : Long.toString(commandCentre.pack()), traitId, flagshipHullId, flagshipVariantId,
                seed(shipNameSeed), seed(treeSeed), String.join(LIST_SEPARATOR, dmodIds), counts(scars), String.join(LIST_SEPARATOR, carriedItems),
                String.join(LIST_SEPARATOR, pendingChoice),
                Integer.toString(defeats), flag(lastChance), status.name(), flag(heldCommand), String.join(LIST_SEPARATOR, history),
                Integer.toString(renown), flag(identified), Integer.toString(seat), portraitPath);
    }

    public static AtlasOfficer decode(int recordId, String text) {
        if (text == null) {
            return null;
        }
        String[] fields = text.split("\\" + FIELD_SEPARATOR, -1);
        boolean current = fields.length == FIELD_COUNT && FORMAT.equals(fields[0]);
        boolean portraitless = fields.length == PORTRAITLESS_FIELD_COUNT && PORTRAITLESS_FORMAT.equals(fields[0]);
        boolean seatless = fields.length == SEATLESS_FIELD_COUNT && SEATLESS_FORMAT.equals(fields[0]);
        if (!current && !portraitless && !seatless || fields[1].isEmpty()) {
            return null;
        }
        try {
            return new AtlasOfficer(recordId, fields[1], unseed(fields[2]), unseed(fields[3]), fields[4],
                    fields[5].isEmpty() ? null : AtlasCell.unpack(Long.parseLong(fields[5])), fields[6], fields[7], fields[8], unseed(fields[9]),
                    unseed(fields[10]), list(fields[11]), uncounts(fields[12]), list(fields[13]), list(fields[14]), Integer.parseInt(fields[15]),
                    unflag(fields[16]), Status.valueOf(fields[17]), unflag(fields[18]), list(fields[19]), Integer.parseInt(fields[20]),
                    unflag(fields[21]), seatless ? UNSEATED : Integer.parseInt(fields[22]), current ? fields[23] : "");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String seed(long value) {
        return Long.toString(value, Character.MAX_RADIX);
    }

    private static long unseed(String text) {
        return Long.parseLong(text, Character.MAX_RADIX);
    }

    private static String flag(boolean value) {
        return value ? "1" : "0";
    }

    private static boolean unflag(String text) {
        return "1".equals(text);
    }

    private static String counts(Map<String, Integer> counts) {
        List<String> entries = new ArrayList<>();
        new TreeMap<>(counts).forEach((id, count) -> entries.add(id + COUNT_SEPARATOR + count));
        return String.join(LIST_SEPARATOR, entries);
    }

    private static Map<String, Integer> uncounts(String text) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String entry : list(text)) {
            int split = entry.lastIndexOf(COUNT_SEPARATOR);
            if (split <= 0) {
                throw new IllegalArgumentException("Count entry without a count: " + entry);
            }
            counts.put(entry.substring(0, split), Integer.parseInt(entry.substring(split + 1)));
        }
        return counts;
    }

    private static List<String> list(String text) {
        return text.isEmpty() ? List.of() : List.of(text.split(LIST_SEPARATOR, -1));
    }
}
