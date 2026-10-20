package exiledsector.skills.unlock;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.compat.AbyssSingularity;
import exiledsector.compat.NightcrossCompat;
import exiledsector.compat.NightcrossUnlocks;
import exiledsector.compat.RandomAssortmentCompat;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public enum UnlockConditionType {
    BLUEPRINT(UnlockConditionOverrides.DISABLE_BLUEPRINT_FIELD_ID, null),
    CHARACTER_STAT(UnlockConditionOverrides.DISABLE_CHARACTER_STAT_FIELD_ID, null),
    MIN_SHIP_LEVEL(UnlockConditionOverrides.DISABLE_MIN_SHIP_LEVEL_FIELD_ID, null),
    MEMORY_FLAG(UnlockConditionOverrides.DISABLE_MEMORY_FLAG_FIELD_ID, null),
    RAT_PLAYER_EXOSHIP(new ModGate(RandomAssortmentCompat::isModEnabled, sector -> RandomAssortmentCompat.exoshipConfirmedRecovered())),
    RAT_SINGULARITY_DEFEATED(new ModGate(RandomAssortmentCompat::isModEnabled, AbyssSingularity::isGateOpen)),
    NIGHTCROSS_PASCAL_VISITED(new ModGate(NightcrossCompat::isModEnabled, NightcrossUnlocks::isPascalVisited)),
    NIGHTCROSS_LUNAR_FLEET_DEFEATED(new ModGate(NightcrossCompat::isModEnabled, NightcrossUnlocks::isLunarFleetDefeated)),
    NIGHTCROSS_ANCIENT_PROTOTYPE_FOUND(new ModGate(NightcrossCompat::isModEnabled, NightcrossUnlocks::isAncientPrototypeFound)),
    THREAT_FOUGHT(new ModGate(() -> true, ThreatEncounter::isFought));

    record ModGate(BooleanSupplier modEnabled, Predicate<SectorAPI> passed) {

        boolean isOpen(SectorAPI sector) {
            return !modEnabled.getAsBoolean() || passed.test(sector);
        }
    }

    private final String overrideFieldId;
    private final ModGate modGate;

    UnlockConditionType(ModGate modGate) {
        this(UnlockConditionOverrides.DISABLE_MEMORY_FLAG_FIELD_ID, modGate);
    }

    UnlockConditionType(String overrideFieldId, ModGate modGate) {
        this.overrideFieldId = overrideFieldId;
        this.modGate = modGate;
    }

    public String overrideFieldId() {
        return overrideFieldId;
    }

    public boolean isModGate() {
        return modGate != null;
    }

    public boolean failsOpen() {
        return isModGate();
    }

    ModGate modGate() {
        return modGate;
    }
}
