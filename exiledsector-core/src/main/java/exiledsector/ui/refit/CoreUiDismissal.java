package exiledsector.ui.refit;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class CoreUiDismissal {

    private static final Logger LOG = Logger.getLogger(CoreUiDismissal.class);
    private static final MethodType DISMISS_TYPE = MethodType.methodType(void.class, int.class);
    private static final int DISMISS_CODE = 0;

    private CoreUiDismissal() {
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost the automatic close, never the wormhole
    @SuppressWarnings("java:S1181")
    public static boolean dismiss(CampaignUIAPI campaignUI) {
        if (campaignUI == null) {
            return false;
        }
        try {
            return dismissCore(UiReflection.coreUI(campaignUI));
        } catch (Throwable e) {
            LOG.warn("[ExiledSector] Could not close the refit screen after opening a Star Chart wormhole; close it by hand", e);
            return false;
        }
    }

    static boolean dismissCore(Object coreUi) throws Throwable {
        if (coreUi == null) {
            return false;
        }
        MethodHandle dismiss = MethodHandles.publicLookup().findVirtual(coreUi.getClass(), "dismiss", DISMISS_TYPE);
        dismiss.invoke(coreUi, DISMISS_CODE);
        return true;
    }
}
