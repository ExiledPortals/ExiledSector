package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import static exiledsector.skills.skilleffect.ScaledBonus.percent;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;

final class ArcStrikeSpeedListener extends ShipCombatListener implements DamageTakenModifier, AdvanceableListener {

    static final String SPEED_PERCENT_KEY = "exiledSector_empArcStrikeSpeedPercent";
    static final float BURST_SECONDS = 3f;
    private static final String MOD_ID_PREFIX = "exiledSector_empArcStrikeSpeed_";
    private static final ScaledBonus SPEED_BURST = new ScaledBonus(
            percent(liveStat(MutableShipStatsAPI::getMaxSpeed), SPEED_PERCENT_KEY),
            percent(Maneuverability.target(), SPEED_PERCENT_KEY));

    private final ScaledBonus.Tracker speedBurst = SPEED_BURST.tracker();
    private float burstSecondsLeft;

    ArcStrikeSpeedListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    boolean bursting() {
        return burstSecondsLeft > 0f;
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; the burst starts as a side effect instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof EmpArcEntityAPI && target == ownerShip && ownerIsAliveNotHulk()) {
            burstSecondsLeft = BURST_SECONDS;
            speedBurst.apply(ownerShip.getMutableStats(), modId, 1f);
        }
        return null;
    }

    @Override
    public void advance(float amount) {
        if (burstSecondsLeft <= 0f) {
            return;
        }
        burstSecondsLeft = ownerIsAliveNotHulk() ? burstSecondsLeft - amount : 0f;
        if (burstSecondsLeft <= 0f) {
            burstSecondsLeft = 0f;
            speedBurst.apply(ownerShip.getMutableStats(), modId, 0f);
        }
    }
}
