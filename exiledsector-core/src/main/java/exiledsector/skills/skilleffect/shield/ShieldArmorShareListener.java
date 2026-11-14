package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.StatMods;
import exiledsector.skills.skilleffect.TickTimer;

import java.util.Map;

final class ShieldArmorShareListener extends ShipCombatListener implements AdvanceableListener {

    static final String EFFECTIVENESS_PERCENT_KEY = "exiledSector_shieldDamageTakenArmorEffectivenessPercent";
    static final float CHECK_INTERVAL_SECONDS = 0.25f;
    private static final String MOD_ID_PREFIX = "exiledSector_shieldArmorShare_";

    private final TickTimer checkTimer = TickTimer.dueOnFirstAdvance(CHECK_INTERVAL_SECONDS);
    private float appliedMult = 1f;

    ShieldArmorShareListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    static float armorDamageTakenMult(float shieldDamageTakenMult, float effectivenessPercent) {
        return Math.max(0f, 1f - effectivenessPercent / 100f * (1f - shieldDamageTakenMult));
    }

    static float shieldOnlyDamageTakenMult(MutableStat shield, MutableStat armor) {
        float percent = 0f;
        for (Map.Entry<String, MutableStat.StatMod> mod : shield.getPercentMods().entrySet()) {
            percent += sharedWith(armor, mod.getKey()) ? 0f : mod.getValue().value;
        }
        float flat = 0f;
        for (Map.Entry<String, MutableStat.StatMod> mod : shield.getFlatMods().entrySet()) {
            flat += sharedWith(armor, mod.getKey()) ? 0f : mod.getValue().value;
        }
        float mult = 1f;
        for (Map.Entry<String, MutableStat.StatMod> mod : shield.getMultMods().entrySet()) {
            mult *= sharedWith(armor, mod.getKey()) ? 1f : mod.getValue().value;
        }
        float base = shield.getBaseValue();
        return (base + base * percent / 100f + flat) * mult;
    }

    private static boolean sharedWith(MutableStat armor, String modId) {
        return armor.getPercentMods().containsKey(modId) || armor.getFlatMods().containsKey(modId) || armor.getMultMods().containsKey(modId);
    }

    @Override
    public void advance(float amount) {
        if (!checkTimer.advance(amount)) {
            return;
        }
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        float shieldMult = shieldOnlyDamageTakenMult(stats.getShieldDamageTakenMult(), stats.getArmorDamageTakenMult());
        float mult = armorDamageTakenMult(shieldMult, magnitude(EFFECTIVENESS_PERCENT_KEY));
        if (mult == appliedMult) {
            return;
        }
        appliedMult = mult;
        StatMods.setMult(stats.getArmorDamageTakenMult(), modId, mult);
    }
}
