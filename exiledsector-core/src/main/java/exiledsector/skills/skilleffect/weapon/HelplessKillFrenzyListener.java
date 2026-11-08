package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.KillWatch;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class HelplessKillFrenzyListener extends ShipCombatListener implements DamageDealtModifier, KillWatch.KillReaction {

    static final String STACKS_KEY = "exiledSector_helplessKillPirateFrenzyStacks";

    private final PirateFrenzy frenzy;

    HelplessKillFrenzyListener(ShipAPI ownerShip) {
        super(ownerShip);
        this.frenzy = PirateFrenzy.of(ownerShip);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (target instanceof ShipAPI targetShip && HelplessShipDamageListener.isHelpless(targetShip)) {
            KillWatch.watch(ownerShip, targetShip);
        }
        return null;
    }

    @Override
    public void onKill(ShipAPI victim) {
        if (!HelplessShipDamageListener.isHelpless(victim)) {
            return;
        }
        int stacks = Math.round(magnitude(STACKS_KEY));
        for (int i = 0; i < stacks; i++) {
            frenzy.gain();
        }
    }
}
