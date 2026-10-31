package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;

@FunctionalInterface
public interface StatApplier {

    void apply(MutableShipStatsAPI stats, String modId, float magnitude);
}
