package exiledsector.ui.atlas;

record OfficerBoardGeometry(float rowsCentreX, float blockTop, float rowPitch, float slotWidth, float portraitSize, int columns) {

    static final float MAX_PORTRAIT_SIZE = 64f;
    static final float PORTRAIT_SHARE_OF_SLOT = 0.8f;
    static final float MAX_ROW_PITCH_PER_PORTRAIT = 1.9f;
    static final float PREFERRED_ROW_PITCH_PER_PORTRAIT = 1.5f;

    static float preferredRowsWidth(int widestRow) {
        return Math.max(1, widestRow) * MAX_PORTRAIT_SIZE / PORTRAIT_SHARE_OF_SLOT;
    }

    static float preferredRowsHeight(int rowCount) {
        return Math.max(1, rowCount) * MAX_PORTRAIT_SIZE * PREFERRED_ROW_PITCH_PER_PORTRAIT;
    }

    static OfficerBoardGeometry fit(float rowsLeft, float rowsBottom, float rowsWidth, float rowsHeight, int rowCount, int widestRow) {
        int rows = Math.max(1, rowCount);
        int columns = Math.max(1, widestRow);
        float slotWidth = Math.min(MAX_PORTRAIT_SIZE / PORTRAIT_SHARE_OF_SLOT, rowsWidth / columns);
        float portraitSize = Math.min(slotWidth * PORTRAIT_SHARE_OF_SLOT, rowsHeight / rows * PORTRAIT_SHARE_OF_SLOT);
        float rowPitch = Math.min(rowsHeight / rows, portraitSize * MAX_ROW_PITCH_PER_PORTRAIT);
        float blockTop = rowsBottom + rowsHeight / 2f + rowPitch * rows / 2f;
        return new OfficerBoardGeometry(rowsLeft + rowsWidth / 2f, blockTop, rowPitch, slotWidth, portraitSize, columns);
    }

    float centreX(OfficerBoardLayout.Seat seat) {
        float slotsPerSeat = columns / (float) Math.max(1, seat.rowSize());
        return rowsCentreX + ((seat.column() + 0.5f) * slotsPerSeat - columns / 2f) * slotWidth;
    }

    float centreY(OfficerBoardLayout.Seat seat) {
        return rowCentreY(seat.row());
    }

    float rowCentreY(int row) {
        return blockTop - (row + 0.5f) * rowPitch;
    }
}
