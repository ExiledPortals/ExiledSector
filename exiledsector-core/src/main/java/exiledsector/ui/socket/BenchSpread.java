package exiledsector.ui.socket;

record BenchSpread(float artWidth, float socketX, float columnLeft, float controlsLeft, float trayLeft, float artShift) {

    record Contents(boolean orbit, boolean column, boolean trays) {
    }

    static BenchSpread full(BenchMode mode, float scale, Contents contents) {
        float artWidth = BenchScene.ART_WIDTH * scale;
        float tileSize = BenchScene.tileSize(scale);
        float gap = BenchScene.COLUMN_GAP * scale;
        float rightTileLeft = artWidth - BenchScene.COLUMN_RIGHT_MARGIN * scale - tileSize;
        float columnLeft = contents.trays() ? rightTileLeft - gap - tileSize : rightTileLeft;
        return new BenchSpread(artWidth, mode.socketX() * scale, columnLeft, rightTileLeft, rightTileLeft - gap, 0f);
    }

    static BenchSpread contracted(BenchMode mode, float scale, Contents contents) {
        float tileSize = BenchScene.tileSize(scale);
        float gap = BenchScene.COLUMN_GAP * scale;
        float sideReach = sideReach(mode, scale, contents);
        float socketX = gap + sideReach;
        float columnLeft = socketX + sideReach + gap;
        float contentRight = contents.column() ? columnLeft + tileSize : socketX + sideReach;
        float controlsLeft = contentRight + gap;
        float right = contents.trays() ? controlsLeft + tileSize : contentRight;
        float artWidth = Math.min(BenchScene.ART_WIDTH * scale, right + gap);
        return new BenchSpread(artWidth, socketX, columnLeft, controlsLeft, controlsLeft - gap, socketX - mode.socketX() * scale);
    }

    static BenchSpread trayOpen(BenchMode mode, float scale, Contents contents) {
        BenchSpread full = full(mode, scale, contents);
        float tileSize = BenchScene.tileSize(scale);
        float gap = BenchScene.COLUMN_GAP * scale;
        float sideReach = sideReach(mode, scale, contents);
        float socketX = gap + sideReach;
        float columnLeft = socketX + sideReach + gap;
        float contentRight = contents.column() ? columnLeft + tileSize : socketX + sideReach;
        return new BenchSpread(full.artWidth(), socketX, columnLeft, full.controlsLeft(), contentRight + gap, 0f);
    }

    private static float sideReach(BenchMode mode, float scale, Contents contents) {
        float tileSize = BenchScene.tileSize(scale);
        float plateHalf = BenchScene.DETAILS_WIDTH / 2f * scale + CraftingBenchPanel.PLATE_PAD;
        float orbitHalf = contents.orbit()
                ? mode.orbitRadiusX() * scale * (float) Math.cos(BenchScene.ORBIT_DROP_RADIANS) + tileSize / 2f
                : BenchScene.SLOT_RADIUS * BenchScene.SOCKET_GLOW_SHARE * scale;
        return Math.max(plateHalf, orbitHalf);
    }

    float trayRight(float scale) {
        return controlsLeft - BenchScene.COLUMN_GAP * scale;
    }

    float trayWidth(float scale) {
        return Math.max(0f, trayRight(scale) - trayLeft);
    }

    BenchSpread towards(BenchSpread target, float share) {
        if (share <= 0f) {
            return this;
        }
        if (share >= 1f) {
            return target;
        }
        return new BenchSpread(lerp(artWidth, target.artWidth, share), lerp(socketX, target.socketX, share), lerp(columnLeft, target.columnLeft, share),
                lerp(controlsLeft, target.controlsLeft, share), lerp(trayLeft, target.trayLeft, share), lerp(artShift, target.artShift, share));
    }

    static float eased(float progress) {
        return progress * progress * (3f - 2f * progress);
    }

    private static float lerp(float from, float to, float share) {
        return from + (to - from) * share;
    }
}
