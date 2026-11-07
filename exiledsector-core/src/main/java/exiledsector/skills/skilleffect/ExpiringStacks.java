package exiledsector.skills.skilleffect;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.ToDoubleFunction;

public final class ExpiringStacks<V> {

    private record Stack<V>(float expiresAtSeconds, V value) {
    }

    private final int maxStacks;
    private final float stackSeconds;
    private final Deque<Stack<V>> stacks = new ArrayDeque<>();

    public ExpiringStacks(int maxStacks, float stackSeconds) {
        this.maxStacks = maxStacks;
        this.stackSeconds = stackSeconds;
    }

    public void add(float nowSeconds) {
        add(nowSeconds, null);
    }

    public void add(float nowSeconds, V value) {
        dropExpired(nowSeconds);
        if (stacks.size() >= maxStacks) {
            stacks.pollFirst();
        }
        stacks.addLast(new Stack<>(nowSeconds + stackSeconds, value));
    }

    public void dropExpired(float nowSeconds) {
        while (!stacks.isEmpty() && stacks.peekFirst().expiresAtSeconds() <= nowSeconds) {
            stacks.pollFirst();
        }
    }

    public void clear() {
        stacks.clear();
    }

    public int size() {
        return stacks.size();
    }

    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    public float sum(ToDoubleFunction<V> amountOf) {
        float total = 0f;
        for (Stack<V> stack : stacks) {
            total += (float) amountOf.applyAsDouble(stack.value());
        }
        return total;
    }
}
