package exiledsector.effects;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorAPI;

public final class TransientScripts {

    private TransientScripts() {
    }

    public static void nextFrame(SectorAPI sector, Runnable action) {
        sector.addTransientScript(new NextFrame(action));
    }

    private static final class NextFrame implements EveryFrameScript {

        private final Runnable action;
        private boolean done;

        private NextFrame(Runnable action) {
            this.action = action;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }

        @Override
        public void advance(float amount) {
            if (done) {
                return;
            }
            done = true;
            action.run();
        }
    }
}
