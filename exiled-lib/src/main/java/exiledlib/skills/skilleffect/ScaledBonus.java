package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;

final class ScaledBonus {

    record Part(StatTarget target, String perUnitKey, StatMode mode, float fixedPerUnit) {
    }

    private final Part[] parts;

    ScaledBonus(Part... parts) {
        this.parts = parts.clone();
    }

    static Part percent(StatTarget target) {
        return new Part(target, null, StatMode.PERCENT, 1f);
    }

    static Part percent(StatTarget target, String perUnitKey) {
        return new Part(target, perUnitKey, StatMode.PERCENT, 1f);
    }

    static Part fixed(StatTarget target, StatMode mode, float perUnit) {
        return new Part(target, null, mode, perUnit);
    }

    void apply(MutableShipStatsAPI stats, String modId, float scale) {
        if (scale <= 0f) {
            remove(stats, modId);
            return;
        }
        for (Part part : parts) {
            float perUnit = part.perUnitKey() == null ? part.fixedPerUnit() : stats.getDynamic().getValue(part.perUnitKey(), 0f);
            part.target().apply(stats, modId, part.mode(), perUnit * scale);
        }
    }

    void remove(MutableShipStatsAPI stats, String modId) {
        for (Part part : parts) {
            part.target().remove(stats, modId);
        }
    }
}
