package exiledsector.ui.atlas;

import java.util.Arrays;

final class DepthOrder<K> {

    private static final int STARTING_CAPACITY = 32;

    private Object[] kinds = new Object[STARTING_CAPACITY];
    private int[] indices = new int[STARTING_CAPACITY];
    private int[] parts = new int[STARTING_CAPACITY];
    private float[] depths = new float[STARTING_CAPACITY];
    private int[] drawOrder = new int[STARTING_CAPACITY];
    private int count;

    void clear() {
        Arrays.fill(kinds, 0, count, null);
        count = 0;
    }

    void add(K kind, int index, int part, float depth) {
        if (count == kinds.length) {
            int capacity = count * 2;
            kinds = Arrays.copyOf(kinds, capacity);
            indices = Arrays.copyOf(indices, capacity);
            parts = Arrays.copyOf(parts, capacity);
            depths = Arrays.copyOf(depths, capacity);
            drawOrder = Arrays.copyOf(drawOrder, capacity);
        }
        kinds[count] = kind;
        indices[count] = index;
        parts[count] = part;
        depths[count] = depth;
        count++;
    }

    void sortBackToFront() {
        for (int rank = 0; rank < count; rank++) {
            int item = rank;
            int slot = rank;
            while (slot > 0 && depths[drawOrder[slot - 1]] < depths[item]) {
                drawOrder[slot] = drawOrder[slot - 1];
                slot--;
            }
            drawOrder[slot] = item;
        }
    }

    int size() {
        return count;
    }

    // only add() writes kinds, and it only takes a K
    @SuppressWarnings("unchecked")
    K kind(int rank) {
        return (K) kinds[drawOrder[rank]];
    }

    int index(int rank) {
        return indices[drawOrder[rank]];
    }

    int part(int rank) {
        return parts[drawOrder[rank]];
    }

    float depth(int rank) {
        return depths[drawOrder[rank]];
    }
}
