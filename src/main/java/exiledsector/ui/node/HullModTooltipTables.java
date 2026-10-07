package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.hullmods.BallisticRangefinder;
import com.fs.starfarer.api.impl.hullmods.MissileAutoloader;
import com.fs.starfarer.api.impl.hullmods.MissileAutoloader.ReloadCapacityData;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillType;
import exiledsector.ui.TooltipTable;
import exiledsector.ui.TooltipTable.Row;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class HullModTooltipTables {

    static final String BALLISTIC_RANGEFINDER = "ballistic_rangefinder";
    static final String MISSILE_AUTOLOADER = "missile_autoloader";
    private static final String NOT_APPLICABLE = "---";

    private HullModTooltipTables() {
    }

    static List<TooltipTable> forType(SkillType type, ShipHullSpecAPI hullSpec) {
        String hullModId = type == null ? null : type.getVanillaHullModId();
        if (BALLISTIC_RANGEFINDER.equals(hullModId)) {
            return rangefinderTables(largestBallisticSlot(hullSpec));
        }
        if (MISSILE_AUTOLOADER.equals(hullModId)) {
            return List.of(autoloaderTable(hullSpec));
        }
        return List.of();
    }

    static List<TooltipTable> rangefinderTables(WeaponSize largestSlotSize) {
        boolean smallOrMediumLargest = largestSlotSize == WeaponSize.SMALL || largestSlotSize == WeaponSize.MEDIUM;
        boolean largeLargest = largestSlotSize == WeaponSize.LARGE;
        String largestSlot = Translation.text("ui.tables.largestSlot");
        String rangeCap = Translation.text("ui.tables.rangeCap");
        String smallOrMedium = Translation.text("ui.tables.smallOrMedium");
        String large = Translation.text("ui.tables.large");

        TooltipTable ballistic = new TooltipTable(Translation.text("ui.tables.ballisticRange"),
                List.of(largestSlot, Translation.text("ui.tables.smallWeapon"), Translation.text("ui.tables.mediumWeapon"), rangeCap),
                List.of(Row.of(smallOrMediumLargest, smallOrMedium, bonus(BallisticRangefinder.BONUS_SMALL_1),
                                NOT_APPLICABLE, whole(BallisticRangefinder.BONUS_MAX_1)),
                        Row.of(largeLargest, large, bonus(BallisticRangefinder.BONUS_SMALL_3),
                                bonus(BallisticRangefinder.BONUS_MEDIUM_3), whole(BallisticRangefinder.BONUS_MAX_3))));

        float hybridMult = BallisticRangefinder.HYBRID_MULT;
        float hybridMin = BallisticRangefinder.HYBRID_BONUS_MIN;
        TooltipTable hybrid = new TooltipTable(Translation.text("ui.tables.hybridRange"),
                List.of(largestSlot, Translation.text("ui.tables.small"), Translation.text("ui.tables.medium"), large, rangeCap),
                List.of(Row.of(smallOrMediumLargest, smallOrMedium, bonus(BallisticRangefinder.BONUS_SMALL_1 * hybridMult),
                                bonus(hybridMin), bonus(hybridMin), whole(BallisticRangefinder.BONUS_MAX_1)),
                        Row.of(largeLargest, large, bonus(BallisticRangefinder.BONUS_SMALL_3 * hybridMult),
                                bonus(BallisticRangefinder.BONUS_MEDIUM_3 * hybridMult), bonus(hybridMin),
                                whole(BallisticRangefinder.BONUS_MAX_3))));
        return List.of(ballistic, hybrid);
    }

    static TooltipTable autoloaderTable(ShipHullSpecAPI hullSpec) {
        HullSize hullSize = hullSpec.getHullSize();
        ReloadCapacityData currentCapacity = capacityFor(hullSize, smallMissileSlotCount(hullSpec));
        List<ReloadCapacityData> sizeRows = new ArrayList<>();
        for (ReloadCapacityData data : MissileAutoloader.CAPACITY_DATA) {
            if (data.size == hullSize) {
                sizeRows.add(data);
            }
        }
        sizeRows.sort(Comparator.comparingInt(data -> data.capacity));

        List<Row> rows = new ArrayList<>();
        for (ReloadCapacityData data : sizeRows) {
            rows.add(Row.of(data == currentCapacity, hullSizeName(data.size), data.getWeaponsString(), String.valueOf(data.capacity)));
        }
        String reloadCapacity = Translation.text("ui.tables.reloadCapacity");
        return new TooltipTable(reloadCapacity, List.of(Translation.text("ui.tables.shipSize"), Translation.text("ui.tables.smallMissiles"), reloadCapacity), rows);
    }

    static WeaponSize largestBallisticSlot(ShipHullSpecAPI hullSpec) {
        WeaponSize largestSlotSize = null;
        for (WeaponSlotAPI slot : hullSpec.getAllWeaponSlotsCopy()) {
            boolean ballistic = !slot.isDecorative() && slot.getWeaponType() == WeaponType.BALLISTIC;
            if (ballistic && (largestSlotSize == null || largestSlotSize.ordinal() < slot.getSlotSize().ordinal())) {
                largestSlotSize = slot.getSlotSize();
            }
        }
        return largestSlotSize;
    }

    private static int smallMissileSlotCount(ShipHullSpecAPI hullSpec) {
        int count = 0;
        for (WeaponSlotAPI slot : hullSpec.getAllWeaponSlotsCopy()) {
            if (slot.getSlotSize() == WeaponSize.SMALL && slot.getWeaponType() == WeaponType.MISSILE) {
                count++;
            }
        }
        return count;
    }

    private static ReloadCapacityData capacityFor(HullSize hullSize, int smallMissileSlots) {
        for (ReloadCapacityData data : MissileAutoloader.CAPACITY_DATA) {
            boolean withinRange = smallMissileSlots >= data.minW && (data.maxW < 0 || smallMissileSlots <= data.maxW);
            if (data.size == hullSize && withinRange) {
                return data;
            }
        }
        return null;
    }

    private static String hullSizeName(HullSize size) {
        return Translation.data("hullSize." + size.name(), size.name());
    }

    private static String bonus(float value) {
        return "+" + (int) value;
    }

    private static String whole(float value) {
        return String.valueOf((int) value);
    }
}
