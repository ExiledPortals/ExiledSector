package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.items.custody.SavedCargo;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class RetiredItems {

    public static final String RETIRED_ID_TAG = "exiledSector_retired_id";
    private static final Logger LOG = Logger.getLogger(RetiredItems.class);

    public record RetiredIds(Set<String> specialItemIds, Set<String> commodityIds, Set<String> weaponIds) {

        public static final RetiredIds NONE = new RetiredIds(Set.of(), Set.of(), Set.of());

        public boolean isEmpty() {
            return specialItemIds.isEmpty() && commodityIds.isEmpty() && weaponIds.isEmpty();
        }

        boolean holds(CargoStackAPI stack) {
            SpecialItemData special = stack.getSpecialDataIfSpecial();
            if (special != null) {
                return specialItemIds.contains(special.getId());
            }
            if (stack.isCommodityStack()) {
                return commodityIds.contains(stack.getCommodityId());
            }
            WeaponSpecAPI weapon = stack.isWeaponStack() ? stack.getWeaponSpecIfWeapon() : null;
            return weapon != null && weaponIds.contains(weapon.getWeaponId());
        }
    }

    private RetiredItems() {
    }

    public static void removeFrom(SectorAPI sector, List<CargoAPI> savedCargoes) {
        try {
            RetiredIds retired = retiredIds(Global.getSettings());
            if (retired.isEmpty()) {
                return;
            }
            int removedCount = SavedCargo.removeStacks(SavedCargo.withFleets(sector, savedCargoes), retired::holds);
            if (removedCount > 0) {
                LOG.info("[ExiledSector] Removed " + removedCount + " retired items from saved cargo");
            }
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Removing retired items from saved cargo failed", e);
        }
    }

    static RetiredIds retiredIds(SettingsAPI settings) {
        return new RetiredIds(taggedIds(settings.getAllSpecialItemSpecs(), SpecialItemSpecAPI::getTags, SpecialItemSpecAPI::getId),
                taggedIds(settings.getAllCommoditySpecs(), CommoditySpecAPI::getTags, CommoditySpecAPI::getId),
                taggedIds(settings.getAllWeaponSpecs(), WeaponSpecAPI::getTags, WeaponSpecAPI::getWeaponId));
    }

    static int removeFrom(CargoAPI cargo, RetiredIds retired) {
        return SavedCargo.removeStacks(cargo, retired::holds);
    }

    private static <T> Set<String> taggedIds(List<T> specs, Function<T, Set<String>> tags, Function<T, String> id) {
        if (specs == null) {
            return Set.of();
        }
        return specs.stream().filter(spec -> {
            Set<String> specTags = tags.apply(spec);
            return specTags != null && specTags.contains(RETIRED_ID_TAG);
        }).map(id).collect(Collectors.toUnmodifiableSet());
    }
}
