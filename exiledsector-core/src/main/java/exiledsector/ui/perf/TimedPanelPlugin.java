package exiledsector.ui.perf;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.List;

public class TimedPanelPlugin implements CustomUIPanelPlugin {

    private final CustomUIPanelPlugin timedPlugin;
    private final ScreenPerformance performance;

    public TimedPanelPlugin(CustomUIPanelPlugin timedPlugin, ScreenPerformance performance) {
        this.timedPlugin = timedPlugin;
        this.performance = performance;
    }

    public CustomUIPanelPlugin timedPlugin() {
        return timedPlugin;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        timedPlugin.positionChanged(position);
    }

    @Override
    public void renderBelow(float alphaMult) {
        long startNanos = performance.now();
        timedPlugin.renderBelow(alphaMult);
        performance.rendered(startNanos);
    }

    @Override
    public void render(float alphaMult) {
        long startNanos = performance.now();
        timedPlugin.render(alphaMult);
        performance.rendered(startNanos);
    }

    @Override
    public void advance(float amount) {
        performance.frameStarted();
        long startNanos = performance.now();
        timedPlugin.advance(amount);
        performance.advanced(startNanos);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        long startNanos = performance.now();
        timedPlugin.processInput(events);
        performance.handledInput(startNanos);
    }

    @Override
    public void buttonPressed(Object buttonId) {
        timedPlugin.buttonPressed(buttonId);
    }
}
