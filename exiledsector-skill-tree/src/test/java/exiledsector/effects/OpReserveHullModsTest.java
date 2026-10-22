package exiledsector.effects;

import exiledsector.persistence.RetiredReserveIds;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpReserveHullModsTest {

    private static final Path HULL_MODS = Path.of("data/hullmods/hull_mods.csv");

    @Test
    void spentOpIsPaidWithOneCoinPerNonZeroDigit() {
        assertEquals(List.of(), OpReserveHullMods.reserveHullModsFor(0));
        assertEquals(List.of("exiledSector_opReserve_7"), OpReserveHullMods.reserveHullModsFor(7));
        assertEquals(List.of("exiledSector_opReserve_100", "exiledSector_opReserve_30", "exiledSector_opReserve_7"),
                OpReserveHullMods.reserveHullModsFor(137));
        assertEquals(List.of("exiledSector_opReserve_1000", "exiledSector_opReserve_5"), OpReserveHullMods.reserveHullModsFor(1005));
    }

    @Test
    void spentOpBeyondTheCoinsIsCappedRatherThanDropped() {
        assertEquals(List.of("exiledSector_opReserve_9000", "exiledSector_opReserve_900", "exiledSector_opReserve_90",
                "exiledSector_opReserve_9"), OpReserveHullMods.reserveHullModsFor(25000));
    }

    @Test
    void bothCoinsAndRetiredSlotsCountAsReserves() {
        assertTrue(OpReserveHullMods.isReserveHullMod("exiledSector_opReserve_40"));
        assertTrue(OpReserveHullMods.isReserveHullMod("exiledSector_opSpent_12"));
        assertFalse(OpReserveHullMods.isReserveHullMod("heavyarmor"));
    }

    @Test
    void everyCoinIsDefinedWithItsValueAsTheCostAtEveryHullSize() throws Exception {
        Map<String, JSONObject> rowsById = hullModRows();
        for (int op = 1; op <= OpReserveHullMods.MAX_RESERVED_OP; op++) {
            for (String coinId : OpReserveHullMods.reserveHullModsFor(op)) {
                JSONObject coin = rowsById.get(coinId);
                assertNotNull(coin, coinId + " is missing from " + HULL_MODS);
                String value = coinId.substring(OpReserveHullMods.ID_PREFIX.length());
                for (String costColumn : List.of("cost_frigate", "cost_dest", "cost_cruiser", "cost_capital")) {
                    assertEquals(value, coin.getString(costColumn), coinId + " " + costColumn);
                }
            }
        }
    }

    @Test
    void theOneRetiredReserveOldSavesLoadAsStaysDefinedAndCostsNothing() throws Exception {
        Map<String, JSONObject> rowsById = hullModRows();
        JSONObject retired = rowsById.get(RetiredReserveIds.RETIRED_ID);
        assertNotNull(retired, RetiredReserveIds.RETIRED_ID + " must stay in " + HULL_MODS + " so older saves load");
        for (String costColumn : List.of("cost_frigate", "cost_dest", "cost_cruiser", "cost_capital")) {
            assertEquals("0", retired.getString(costColumn));
        }
        long retiredRows = rowsById.keySet().stream().filter(id -> id.startsWith(RetiredReserveIds.LEGACY_PREFIX)).count();
        assertEquals(1, retiredRows);
    }

    private static Map<String, JSONObject> hullModRows() throws Exception {
        JSONArray rows = CDL.toJSONArray(Files.readString(HULL_MODS, StandardCharsets.UTF_8).replace("\r\n", "\n"));
        Map<String, JSONObject> rowsById = new HashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            rowsById.put(rows.getJSONObject(i).getString("id"), rows.getJSONObject(i));
        }
        return rowsById;
    }
}
