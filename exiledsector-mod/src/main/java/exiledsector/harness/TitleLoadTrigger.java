package exiledsector.harness;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;

final class TitleLoadTrigger extends BaseCustomUIPanelPlugin {

    private final Runnable load;
    private boolean triggered;

    TitleLoadTrigger(Runnable load) {
        this.load = load;
    }

    @Override
    public void advance(float amount) {
        if (triggered) {
            return;
        }
        triggered = true;
        load.run();
    }
}
