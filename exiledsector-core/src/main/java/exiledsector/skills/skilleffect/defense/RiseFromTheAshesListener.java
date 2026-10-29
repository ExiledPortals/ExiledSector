package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import exiledsector.compat.CombatGlow;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.PlayerStatus;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.special.LethalHitArbiter;

import java.awt.Color;
import java.util.Map;

import static exiledsector.skills.skilleffect.StatTarget.all;
import static exiledsector.skills.skilleffect.StatTarget.stat;

final class RiseFromTheAshesListener extends ShipCombatListener implements LethalHitArbiter.Saver, AdvanceableListener {

    static final String HULL_PERCENT_KEY = "exiledLib_riseFromTheAshesHullPercent";
    static final String ARMOR_RESTORE_PERCENT_KEY = "exiledLib_riseFromTheAshesArmorRestorePercent";
    static final String DAMAGE_TAKEN_PERCENT_KEY = "exiledLib_riseFromTheAshesDamageTakenPercent";
    static final float DURATION_SECONDS = 6f;
    static final String USED_KEY_PREFIX = "exiledLib_riseFromTheAshesUsed_";
    private static final String MOD_ID_PREFIX = "exiledLib_riseFromTheAshes_";
    private static final String STATUS_ICON = "graphics/icons/hullsys/damper_field.png";
    private static final Color EMBER_COLOR = new Color(255, 140, 60);
    private static final Color JITTER_COLOR = new Color(255, 120, 40, 90);
    private static final CombatGlow.Burst RISE_GLOW = new CombatGlow.Burst(EMBER_COLOR, 3f, 1.2f, 1.5f, 2.5f, 0.25f, 1f, false);
    private static final ScaledBonus DAMAGE_TAKEN = new ScaledBonus(new ScaledBonus.Part(all(stat(MutableShipStatsAPI::getHullDamageTakenMult),
            stat(MutableShipStatsAPI::getArmorDamageTakenMult), stat(MutableShipStatsAPI::getShieldDamageTakenMult)),
            DAMAGE_TAKEN_PERCENT_KEY, StatMode.MULT, 1f));

    private final PlayerStatus status;
    private float remainingSeconds;

    RiseFromTheAshesListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
        status = new PlayerStatus(modId, STATUS_ICON, "combat.riseFromTheAshes");
        LethalHitArbiter.addSaver(ownerShip, LethalHitArbiter.RISE_FROM_THE_ASHES_PRIORITY, this);
    }

    boolean rising() {
        return remainingSeconds > 0f;
    }

    @Override
    public boolean saveFromLethalHit(ShipAPI ship, float damageAmount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Map<String, Object> customData = engine == null ? null : engine.getCustomData();
        String usedKey = usedKey(ownerShip);
        if (customData == null || customData.containsKey(usedKey)) {
            return false;
        }
        customData.put(usedKey, Boolean.TRUE);
        rise(engine);
        return true;
    }

    static String usedKey(ShipAPI ship) {
        String memberId = ship.getFleetMemberId();
        return USED_KEY_PREFIX + (memberId != null ? memberId : ship.getId());
    }

    private void rise(CombatEngineAPI engine) {
        ownerShip.setHitpoints(Math.max(1f, ownerShip.getMaxHitpoints() * magnitude(HULL_PERCENT_KEY) / 100f));
        restoreArmor(magnitude(ARMOR_RESTORE_PERCENT_KEY));
        DAMAGE_TAKEN.apply(ownerShip.getMutableStats(), modId, 1f);
        remainingSeconds = DURATION_SECONDS;
        RISE_GLOW.play(ownerShip.getLocation(), ownerShip, ownerShip.getVelocity(), ownerShip.getCollisionRadius());
        if (ownerShip.getFluxTracker() != null && ownerShip.getFluxTracker().showFloaty()) {
            engine.addFloatingTextAlways(ownerShip.getLocation(), Translation.gameText("combat.riseFromTheAshes"),
                    NeuralLinkScript.getFloatySize(ownerShip), EMBER_COLOR, ownerShip, 16f, 3.2f, 1f, 0f, 0f, 1f);
        }
    }

    private void restoreArmor(float percent) {
        ArmorGridAPI grid = ownerShip.getArmorGrid();
        if (percent <= 0f || grid == null) {
            return;
        }
        float max = grid.getMaxArmorInCell();
        float restored = max * percent / 100f;
        ArmorCells.update(grid, current -> Math.max(current, Math.min(max, current + restored)));
        ownerShip.syncWithArmorGridState();
    }

    @Override
    public void advance(float amount) {
        if (!rising()) {
            return;
        }
        remainingSeconds -= amount;
        if (!rising() || !ownerIsAliveNotHulk()) {
            remainingSeconds = 0f;
            DAMAGE_TAKEN.remove(ownerShip.getMutableStats(), modId);
            return;
        }
        ownerShip.setJitterUnder(this, JITTER_COLOR, remainingSeconds / DURATION_SECONDS, 6, 0f, ownerShip.getCollisionRadius() * 0.15f);
        status.showIfPlayer(ownerShip, false, () -> Translation.text("combat.riseFromTheAshes.status"));
    }
}
