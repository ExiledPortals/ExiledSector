package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.i18n.Translation;
import exiledsector.skills.ShipSkillData;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public enum MasterSchematic implements ShipCurrency {

    INSTANCE;

    public static final String ITEM_ID = "exiledSector_master_schematic";
    public static final String ICON_PATH = "graphics/icons/hull_upgrades/master_schematic.png";
    public static final Color COLOR = new Color(235, 245, 255);
    static final float SALVAGE_CHANCE_SHARE = 0.5f;
    private static final AtomicReference<BooleanSupplier> REQUIRED = new AtomicReference<>(() -> false);
    private static final List<Runnable> CHANGE_LISTENERS = new CopyOnWriteArrayList<>();

    public static void requiredWhen(BooleanSupplier required) {
        REQUIRED.set(required == null ? () -> false : required);
    }

    public static boolean required() {
        return REQUIRED.get().getAsBoolean();
    }

    public static void onChange(Runnable listener) {
        if (!CHANGE_LISTENERS.contains(listener)) {
            CHANGE_LISTENERS.add(listener);
        }
    }

    public static void changed() {
        CHANGE_LISTENERS.forEach(Runnable::run);
    }

    public static boolean treeOpen(ShipSkillData shipData) {
        if (shipData != null && (shipData.isNpcBuild() || shipData.isMasterSchematicd())) {
            return true;
        }
        return !required();
    }

    public static boolean is(SpecialItemData special) {
        return special != null && ITEM_ID.equals(special.getId());
    }

    public static Integer absorbInto(ItemStore store, CargoAPI cargo, CargoStackAPI stack) {
        if (!is(stack.getSpecialDataIfSpecial())) {
            return null;
        }
        int stackCount = Math.round(stack.getSize());
        if (stackCount < 1) {
            return 0;
        }
        store.addMasterSchematics(stackCount);
        cargo.removeStack(stack);
        return stackCount;
    }

    public static List<ShipCurrency> rollSalvage(float firstItemChance, Random random) {
        return required() && random.nextFloat() < firstItemChance * SALVAGE_CHANCE_SHARE ? List.of(INSTANCE) : List.of();
    }

    @Override
    public SpecialItemData toSpecialItem() {
        return new SpecialItemData(ITEM_ID, null);
    }

    @Override
    public String displayName() {
        return Translation.text("masterSchematic.name");
    }
}
