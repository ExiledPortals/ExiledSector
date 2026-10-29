package exiledsector.ui;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ui.refit.RefitFleetList;
import lunalib.backend.ui.refit.RefitButtonAdder;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

final class RefitShipSwitch implements EveryFrameScript {

    static final int MAX_WAIT_FRAMES = 30;

    private final FleetMemberAPI target;
    private final BooleanSupplier variantSyncPending;
    private final Function<FleetMemberAPI, RefitFleetList.Shown> selector;
    private final Consumer<RefitFleetList.Shown> switched;
    private int waitedFrames;
    private boolean done;

    RefitShipSwitch(FleetMemberAPI target, BooleanSupplier variantSyncPending, Function<FleetMemberAPI, RefitFleetList.Shown> selector,
                    Consumer<RefitFleetList.Shown> switched) {
        this.target = target;
        this.variantSyncPending = variantSyncPending;
        this.selector = selector;
        this.switched = switched;
    }

    static RefitShipSwitch to(FleetMemberAPI target, Consumer<RefitFleetList.Shown> switched) {
        return new RefitShipSwitch(target, RefitButtonAdder::getRequiresVariantUpdate, RefitFleetList::select, switched);
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
        if (variantSyncPending.getAsBoolean() && waitedFrames++ < MAX_WAIT_FRAMES) {
            return;
        }
        done = true;
        switched.accept(selector.apply(target));
    }
}
