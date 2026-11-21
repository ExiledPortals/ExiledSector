package exiledsector.harness;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

public final class HarnessCombatPlugin extends BaseEveryFrameCombatPlugin {

    static final int SETTLE_FRAMES = 30;

    private int titleFrames;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!Harness.launched()) {
            return;
        }
        GameState state = Global.getCurrentState();
        if (state == GameState.TITLE) {
            titleFrames++;
            if (titleFrames >= SETTLE_FRAMES) {
                Harness.atTitleScreen();
            }
        } else if (state == GameState.COMBAT) {
            Harness.advanceInCombat();
        }
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
        if (Harness.launched() && Global.getCurrentState() == GameState.COMBAT) {
            Harness.renderInCombat();
        }
    }
}
