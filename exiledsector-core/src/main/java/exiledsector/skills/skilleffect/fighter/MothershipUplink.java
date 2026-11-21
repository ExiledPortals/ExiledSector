package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;

import java.util.List;
import java.util.function.Function;

final class MothershipUplink {

    private static final List<Function<MutableShipStatsAPI, MutableStat>> SHARED_STATS = List.of(
            MutableShipStatsAPI::getBallisticWeaponDamageMult,
            MutableShipStatsAPI::getEnergyWeaponDamageMult,
            MutableShipStatsAPI::getMissileWeaponDamageMult,
            MutableShipStatsAPI::getBeamWeaponDamageMult,
            MutableShipStatsAPI::getBallisticRoFMult,
            MutableShipStatsAPI::getEnergyRoFMult,
            MutableShipStatsAPI::getMissileRoFMult);

    private MothershipUplink() {
    }

    static void share(MutableShipStatsAPI carrierStats, MutableShipStatsAPI fighterStats, String modId, float sharePercent) {
        for (Function<MutableShipStatsAPI, MutableStat> sharedStat : SHARED_STATS) {
            float carrierPercent = sharedStat.apply(carrierStats).getPercentMod();
            MutableStat fighterStat = sharedStat.apply(fighterStats);
            if (carrierPercent > 0f && sharePercent > 0f) {
                fighterStat.modifyPercent(modId, carrierPercent * sharePercent / 100f);
            } else {
                fighterStat.unmodifyPercent(modId);
            }
        }
    }
}
