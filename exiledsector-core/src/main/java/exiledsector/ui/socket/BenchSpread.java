package exiledsector.ui.socket;

record BenchSpread(float artWidth, float socketX, float columnLeft, float controlsLeft, float trayLeft, float trayWidth, float artShift) {

    record Contents(boolean orbit, boolean column, boolean trays) {
    }

    static BenchSpread full(BenchMode mode, float scale, Contents contents) {
        float artWidth = BenchScene.ART_WIDTH * scale;
        float tileSize = BenchScene.tileSize(scale);
        float gap = BenchScene.COLUMN_GAP * scale;
        float controlsLeft = artWidth - tileSize;
        float columnLeft = contents.trays() ? controlsLeft - gap - tileSize : artWidth - BenchScene.COLUMN_RIGHT_MARGIN * scale - tileSize;
        return new BenchSpread(artWidth, mode.socketX() * scale, columnLeft, controlsLeft, controlsLeft - gap, 0f, 0f);
    }

    static BenchSpread contracted(BenchMode mode, float scale, Contents contents) {
        float tileSize = BenchScene.tileSize(scale);
        float gap = BenchScene.COLUMN_GAP * scale;
        float sideReach = sideReach(mode, scale, contents);
        float socketX = gap + sideReach;
        float columnLeft = socketX + sideReach + gap;
        float contentRight = contents.column() ? columnLeft + tileSize : socketX + sideReach;
        float right = contents.trays() ? contentRight + gap + tileSize : contentRight + gap;
        float artWidth = Math.min(BenchScene.ART_WIDTH * scale, right);
        float controlsLeft = contents.trays() ? artWidth - tileSize : contentRight + gap;
        return new BenchSpread(artWidth, socketX, columnLeft, controlsLeft, controlsLeft - gap, 0f, socketX - mode.socketX() * scale);
    }

    static BenchSpread trayOpen(BenchMode mode, float scale, Contents contents, float wantedTrayWidth) {
        BenchSpread full = full(mode, scale, contents);
        float tileSize = BenchScene.tileSize(scale);
        float gap = BenchScene.COLUMN_GAP * scale;
        float sideReach = sideReach(mode, scale, contents);
        float trayRight = full.controlsLeft() - gap;
        float trayWidth = Math.max(0f, Math.min(wantedTrayWidth, maxTrayWidth(mode, scale, contents)));
        float trayLeft = trayRight - trayWidth;
        float contentRight = trayLeft - gap;
        float socketRight = contents.column() ? contentRight - tileSize - gap : contentRight;
        float socketX = Math.min(full.socketX(), socketRight - sideReach);
        float columnLeft = contents.column() ? contentRight - tileSize : socketX + sideReach + gap;
        return new BenchSpread(full.artWidth(), socketX, columnLeft, full.controlsLeft(), trayLeft, trayWidth, 0f);
    }

    static float maxTrayWidth(BenchMode mode, float scale, Contents contents) {
        float tileSize = BenchScene.tileSize(scale);
        float gap = BenchScene.COLUMN_GAP * scale;
        float sideReach = sideReach(mode, scale, contents);
        float packedSocketRight = gap + sideReach * 2f;
        float packedContentRight = contents.column() ? packedSocketRight + gap + tileSize : packedSocketRight;
        return Math.max(0f, full(mode, scale, contents).controlsLeft() - gap - packedContentRight - gap);
    }

    private static float sideReach(BenchMode mode, float scale, Contents contents) {
        float tileSize = BenchScene.tileSize(scale);
        float plateHalf = BenchScene.DETAILS_WIDTH / 2f * scale + CraftingBenchPanel.PLATE_PAD;
        float orbitHalf = contents.orbit()
                ? mode.orbitRadiusX() * scale * (float) Math.cos(BenchScene.ORBIT_DROP_RADIANS) + tileSize / 2f
                : BenchScene.SLOT_RADIUS * BenchScene.SOCKET_GLOW_SHARE * scale;
        return Math.max(plateHalf, orbitHalf);
    }

    BenchSpread towards(BenchSpread target, float share) {
        if (share <= 0f) {
            return this;
        }
        if (share >= 1f) {
            return target;
        }
        return new BenchSpread(lerp(artWidth, target.artWidth, share), lerp(socketX, target.socketX, share), lerp(columnLeft, target.columnLeft, share),
                lerp(controlsLeft, target.controlsLeft, share), lerp(trayLeft, target.trayLeft, share), lerp(trayWidth, target.trayWidth, share),
                lerp(artShift, target.artShift, share));
    }

    static float eased(float progress) {
        return progress * progress * (3f - 2f * progress);
    }

    private static float lerp(float from, float to, float share) {
        return from + (to - from) * share;
    }
}
