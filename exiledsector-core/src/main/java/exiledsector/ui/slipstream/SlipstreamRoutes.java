package exiledsector.ui.slipstream;

import java.util.Random;

@FunctionalInterface
public interface SlipstreamRoutes {

    SlipstreamRoute plan(ViewWindow view, Random random);
}
