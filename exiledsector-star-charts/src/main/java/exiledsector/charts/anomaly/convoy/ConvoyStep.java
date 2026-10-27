package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.Script;

public final class ConvoyStep implements Script {

    public enum Kind {
        ARRIVED,
        UNLOADED
    }

    private final ConvoyScript script;
    private final int convoyIndex;
    private final Kind kind;

    public ConvoyStep(ConvoyScript script, int convoyIndex, Kind kind) {
        this.script = script;
        this.convoyIndex = convoyIndex;
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void run() {
        if (script == null || script.isDone()) {
            return;
        }
        ConvoyState convoy = script.convoy(convoyIndex);
        if (convoy == null) {
            return;
        }
        if (kind == Kind.ARRIVED) {
            script.arrived(convoy);
        } else {
            script.unloaded(convoy);
        }
    }
}
