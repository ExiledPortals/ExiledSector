package exiledsector.skills.unlock;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.compat.AbyssSingularity;
import exiledlib.compat.LostSectorCompat;
import exiledsector.compat.LostSectorUnlocks;
import exiledsector.compat.NightcrossCompat;
import exiledsector.compat.NightcrossUnlocks;
import exiledsector.compat.RandomAssortmentCompat;
import exiledsector.compat.TaniaCompat;
import exiledsector.compat.TaniaUnlocks;
import exiledsector.skills.tags.AreaToggles;

import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public enum UnlockConditionType {
    BLUEPRINT(UnlockConditionOverrides.DISABLE_BLUEPRINT_FIELD_ID, null),
    CHARACTER_STAT(UnlockConditionOverrides.DISABLE_CHARACTER_STAT_FIELD_ID, null),
    MIN_SHIP_LEVEL(UnlockConditionOverrides.DISABLE_MIN_SHIP_LEVEL_FIELD_ID, null),
    MEMORY_FLAG(UnlockConditionOverrides.DISABLE_MEMORY_FLAG_FIELD_ID, null),
    RAT_PLAYER_EXOSHIP(ModGate.of(RandomAssortmentCompat::isModEnabled, sector -> RandomAssortmentCompat.exoshipConfirmedRecovered())),
    RAT_SINGULARITY_DEFEATED(ModGate.of(RandomAssortmentCompat::isModEnabled, AbyssSingularity::isGateOpen)),
    ANCHOR_FOUND(new ModGate(AreaToggles::modInstalledFor, AnchorFinds::isFound), true),
    NIGHTCROSS_EXISTENTIAL_THREAT_DEFEATED(ModGate.of(NightcrossCompat::isModEnabled, NightcrossUnlocks::isExistentialThreatDefeated)),
    NIGHTCROSS_ANCIENT_PROTOTYPE_FOUND(ModGate.of(NightcrossCompat::isModEnabled, NightcrossUnlocks::isAncientPrototypeFound)),
    LOST_SECTOR_FROZEN_HEART_VISITED(ModGate.of(LostSectorCompat::isModEnabled, LostSectorUnlocks::isFrozenHeartSystemVisited)),
    TANIA_BLOOD_TRAIL_DEFEATED(ModGate.of(TaniaCompat::isModEnabled, TaniaUnlocks::isBloodTrailDefeated)),
    TANIA_FROST_FIRE_DEFEATED(ModGate.of(TaniaCompat::isModEnabled, TaniaUnlocks::isFrostFireDefeated)),
    TANIA_LAST_STRIVE_COMPLETED(ModGate.of(TaniaCompat::isModEnabled, TaniaUnlocks::isLastStriveCompleted)),
    THREAT_FOUGHT(ModGate.of(() -> true, ThreatEncounter::isFought)),
    REMNANT_FOUGHT(ModGate.of(() -> true, RemnantEncounter::isFought));

    record ModGate(Predicate<String> modEnabled, BiPredicate<SectorAPI, String> passed) {

        static ModGate of(BooleanSupplier modEnabled, Predicate<SectorAPI> passed) {
            return new ModGate(volume -> modEnabled.getAsBoolean(), (sector, volume) -> passed.test(sector));
        }

        boolean isOpen(SectorAPI sector, String volume, boolean relocked) {
            return !modEnabled.test(volume) || !relocked && passed.test(sector, volume);
        }
    }

    private final String overrideFieldId;
    private final ModGate modGate;
    private final boolean takesVolume;

    UnlockConditionType(ModGate modGate) {
        this(modGate, false);
    }

    UnlockConditionType(ModGate modGate, boolean takesVolume) {
        this.overrideFieldId = UnlockConditionOverrides.DISABLE_MEMORY_FLAG_FIELD_ID;
        this.modGate = modGate;
        this.takesVolume = takesVolume;
    }

    UnlockConditionType(String overrideFieldId, ModGate modGate) {
        this.overrideFieldId = overrideFieldId;
        this.modGate = modGate;
        this.takesVolume = false;
    }

    public String overrideFieldId() {
        return overrideFieldId;
    }

    public boolean isModGate() {
        return modGate != null;
    }

    public boolean takesVolume() {
        return takesVolume;
    }

    public boolean metWithModInstalled(SectorAPI sector, String volume) {
        return modGate != null && modGate.modEnabled().test(volume) && modGate.passed().test(sector, volume);
    }

    public boolean failsOpen() {
        return isModGate();
    }

    ModGate modGate() {
        return modGate;
    }
}
