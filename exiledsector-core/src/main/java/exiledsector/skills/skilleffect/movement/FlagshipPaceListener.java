package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.i18n.NumberText;
import exiledsector.skills.skilleffect.Flagships;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;
import exiledsector.skills.skilleffect.TickTimer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class FlagshipPaceListener extends ShipCombatListener implements AdvanceableListener {

    static final String FLAGSHIP_SPEED_SHARE_PERCENT_KEY = "exiledSector_flagshipPaceSpeedSharePercent";
    static final float SCAN_INTERVAL_SECONDS = 1f;
    private static final String MOD_ID = "exiledSector_flagshipPace";
    private static final Map<HullSize, Float> SPEED_BONUS_CAPS = new EnumMap<>(HullSize.class);

    static {
        SPEED_BONUS_CAPS.put(HullSize.FRIGATE, 30f);
        SPEED_BONUS_CAPS.put(HullSize.DESTROYER, 25f);
        SPEED_BONUS_CAPS.put(HullSize.CRUISER, 20f);
        SPEED_BONUS_CAPS.put(HullSize.CAPITAL_SHIP, 15f);
    }

    private static final ScaledBonus FLAGSHIP_PACE = new ScaledBonus(
            ScaledBonus.fixed(StatTarget.liveStat(MutableShipStatsAPI::getMaxSpeed), StatMode.FLAT, 1f));

    private final TickTimer scanTimer = TickTimer.dueOnFirstAdvance(SCAN_INTERVAL_SECONDS);
    private final ScaledBonus.Tracker flagshipPace = FLAGSHIP_PACE.tracker();

    FlagshipPaceListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    static boolean capCovers(HullSize hullSize) {
        return hullSize != null && SPEED_BONUS_CAPS.containsKey(hullSize);
    }

    static String speedBonusCap(HullSize hullSize) {
        return NumberText.format(SPEED_BONUS_CAPS.get(hullSize));
    }

    static String speedBonusCapByHullSize() {
        List<String> caps = new ArrayList<>();
        for (float cap : SPEED_BONUS_CAPS.values()) {
            caps.add(NumberText.format(cap));
        }
        return String.join("/", caps);
    }

    @Override
    public void advance(float amount) {
        if (scanTimer.advance(amount)) {
            flagshipPace.apply(ownerShip.getMutableStats(), MOD_ID, ownerIsAliveNotHulk() ? speedBonus() : 0f);
        }
    }

    float speedBonus() {
        Float cap = SPEED_BONUS_CAPS.get(ownerShip.getHullSize());
        ShipAPI flagship = Flagships.deployedFlagshipOf(ownerShip);
        if (cap == null || flagship == null || flagship.getHullSpec() == null || flagship.getHullSpec().getEngineSpec() == null) {
            return 0f;
        }
        float share = flagship.getHullSpec().getEngineSpec().getMaxSpeed() * magnitude(FLAGSHIP_SPEED_SHARE_PERCENT_KEY) / 100f;
        return Math.min(share, cap);
    }
}
