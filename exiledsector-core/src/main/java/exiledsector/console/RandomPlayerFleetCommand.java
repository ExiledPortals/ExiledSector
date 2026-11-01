package exiledsector.console;

import java.util.Random;

public abstract class RandomPlayerFleetCommand extends PlayerFleetCommand {

    protected final Random random;

    protected RandomPlayerFleetCommand() {
        this(new Random());
    }

    protected RandomPlayerFleetCommand(Random random) {
        this.random = random;
    }
}
