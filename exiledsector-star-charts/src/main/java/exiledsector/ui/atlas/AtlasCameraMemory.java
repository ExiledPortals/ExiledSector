package exiledsector.ui.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;

import java.lang.ref.WeakReference;

final class AtlasCameraMemory {

    private static volatile Centre remembered = Centre.NONE;

    record Centre(WeakReference<SectorAPI> sectorReference, float treeX, float treeY) {
        static final Centre NONE = new Centre(new WeakReference<>(null), 0f, 0f);

        boolean isFor(SectorAPI sector) {
            return sector != null && sectorReference.get() == sector;
        }
    }

    private AtlasCameraMemory() {
    }

    static void remember(SectorAPI sector, float treeX, float treeY) {
        remembered = new Centre(new WeakReference<>(sector), treeX, treeY);
    }

    static Centre recall(SectorAPI sector) {
        Centre centre = remembered;
        return centre.isFor(sector) ? centre : null;
    }
}
