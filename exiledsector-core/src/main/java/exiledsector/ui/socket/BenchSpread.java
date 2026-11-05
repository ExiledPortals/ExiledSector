package exiledsector.ui.socket;

record BenchSpread(float artWidth, float socketX, float prefixX, float suffixX, float columnLeft) {

    record Contents(boolean inhibitors, boolean orbit, boolean column) {
    }

    static BenchSpread full(BenchMode mode, float scale) {
        float artWidth = BenchScene.ART_WIDTH * scale;
        return new BenchSpread(artWidth, mode.socketX() * scale, mode.leftInhibitorX() * scale, mode.rightInhibitorX() * scale,
                artWidth - BenchScene.COLUMN_RIGHT_MARGIN * scale - BenchScene.tileSize(scale));
    }

    static BenchSpread contracted(BenchMode mode, float scale, Contents contents) {
        float tileSize = BenchScene.tileSize(scale);
        float gap = BenchScene.COLUMN_GAP * scale;
        float plateHalf = BenchScene.DETAILS_WIDTH / 2f * scale + CraftingBenchPanel.PLATE_PAD;
        float orbitHalf = contents.orbit()
                ? mode.orbitRadiusX() * scale * (float) Math.cos(BenchScene.ORBIT_DROP_RADIANS) + tileSize / 2f
                : BenchScene.SLOT_RADIUS * BenchScene.SOCKET_GLOW_SHARE * scale;
        float inhibitorReach = contents.inhibitors() ? orbitHalf + gap + tileSize : orbitHalf;
        float sideReach = Math.max(plateHalf, inhibitorReach);
        float socketX = gap + sideReach;
        float columnLeft = socketX + sideReach + gap;
        float contentRight = contents.column() ? columnLeft + tileSize : socketX + sideReach;
        float inhibitorOffset = orbitHalf + gap + tileSize / 2f;
        float artWidth = Math.min(BenchScene.ART_WIDTH * scale, contentRight + gap);
        return new BenchSpread(artWidth, socketX, socketX - inhibitorOffset, socketX + inhibitorOffset, columnLeft);
    }

    BenchSpread towards(BenchSpread target, float share) {
        if (share <= 0f) {
            return this;
        }
        if (share >= 1f) {
            return target;
        }
        return new BenchSpread(lerp(artWidth, target.artWidth, share), lerp(socketX, target.socketX, share), lerp(prefixX, target.prefixX, share),
                lerp(suffixX, target.suffixX, share), lerp(columnLeft, target.columnLeft, share));
    }

    float artShift(BenchMode mode, float scale) {
        return socketX - mode.socketX() * scale;
    }

    static float eased(float progress) {
        return progress * progress * (3f - 2f * progress);
    }

    private static float lerp(float from, float to, float share) {
        return from + (to - from) * share;
    }
}
