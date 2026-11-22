package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.PlayerStatus;
import exiledsector.skills.skilleffect.ShipCombatListener;

import java.awt.Color;

final class UnstableShieldsListener extends ShipCombatListener implements AdvanceableListener {

    static final String DAMAGE_TAKEN_PERCENT_KEY = "exiledSector_unstableShieldDamageTakenPercent";
    static final String STEADY_FLAG_KEY = "exiledSector_unstableShieldSteady";
    static final float INSTABILITY_SWING_PERCENT = 60f;
    static final Timings UNSTEADY = new Timings(3f, 9f, 3f, 6f);
    static final Timings STEADY = new Timings(4f, 12f, 2f, 4f);
    private static final String MOD_ID_PREFIX = "exiledSector_unstableShields_";
    private static final String STATUS_ICON = "graphics/icons/hullsys/fortress_shield.png";
    private static final Color UNSTABLE_COLOR = new Color(254, 30, 30, 200);

    record Timings(float holdSeconds, float destabiliseSeconds, float recoveryDelaySeconds, float recoverySeconds) {
    }

    private final PlayerStatus status;
    private Color chargedColor;
    private float instability;
    private float raisedSeconds;
    private float recoveryDelayLeft;
    private float appliedMult = Float.NaN;
    private float shownMult = Float.NaN;

    UnstableShieldsListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
        status = new PlayerStatus(modId, STATUS_ICON, "combat.unstableShields");
    }

    float instability() {
        return instability;
    }

    @Override
    public void advance(float amount) {
        ShieldAPI shield = ownerShip.getShield();
        if (shield == null) {
            return;
        }
        if (!ownerIsAliveNotHulk()) {
            ownerShip.getMutableStats().getShieldDamageTakenMult().unmodify(modId);
            return;
        }
        if (chargedColor == null) {
            chargedColor = shield.getInnerColor();
        }
        updateInstability(shield, amount, magnitude(STEADY_FLAG_KEY) > 0f ? STEADY : UNSTEADY);
        float damageTakenMult = damageTakenMult(magnitude(DAMAGE_TAKEN_PERCENT_KEY), instability);
        if (damageTakenMult != appliedMult) {
            appliedMult = damageTakenMult;
            ownerShip.getMutableStats().getShieldDamageTakenMult().modifyMult(modId, damageTakenMult);
            shield.setInnerColor(blend(chargedColor, UNSTABLE_COLOR, instability));
        }
        showStatus(damageTakenMult);
    }

    static float damageTakenMult(float chargedPercent, float instability) {
        return Math.max(0f, 1f + (chargedPercent + INSTABILITY_SWING_PERCENT * instability) / 100f);
    }

    private void updateInstability(ShieldAPI shield, float amount, Timings timings) {
        if (shield.isOn()) {
            raisedSeconds += amount;
            recoveryDelayLeft = timings.recoveryDelaySeconds();
            if (raisedSeconds > timings.holdSeconds()) {
                instability = Math.min(1f, instability + amount / timings.destabiliseSeconds());
            }
        } else if (shield.isOff()) {
            raisedSeconds = 0f;
            if (recoveryDelayLeft > 0f) {
                recoveryDelayLeft -= amount;
            } else {
                instability = Math.max(0f, instability - amount / timings.recoverySeconds());
            }
        }
    }

    private static Color blend(Color from, Color to, float share) {
        if (from == null) {
            return to;
        }
        return new Color(mix(from.getRed(), to.getRed(), share), mix(from.getGreen(), to.getGreen(), share),
                mix(from.getBlue(), to.getBlue(), share), mix(from.getAlpha(), to.getAlpha(), share));
    }

    private static int mix(int from, int to, float share) {
        return Math.max(0, Math.min(255, Math.round(from + (to - from) * share)));
    }

    private void showStatus(float damageTakenMult) {
        float rounded = Math.round(damageTakenMult * 100f) / 100f;
        if (rounded != shownMult) {
            shownMult = rounded;
            status.invalidate();
        }
        status.showIfPlayer(ownerShip, rounded > 1f, () -> Translation.msg("combat.unstableShields.status").arg("value", rounded).text());
    }
}
