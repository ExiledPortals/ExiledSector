package exiledsector.console;

import exiledsector.charts.run.ChartRunActions.OpenResult;

final class ChartConsoleText {

    static final String RUN_ALREADY_OPEN = "A Star Chart run is already open. Collapse it or finish it first (see ExiledSectorChartStatus).";

    private ChartConsoleText() {
    }

    static String openFailure(OpenResult result, String otherFailure) {
        return switch (result) {
            case RUN_EXISTS -> RUN_ALREADY_OPEN;
            case NO_PLAYER_FLEET -> PlayerFleetCommand.NO_PLAYER_FLEET;
            case PLAYER_IN_TRANSITION -> "Your fleet is jumping. Try again once it arrives.";
            case PLAYER_IN_SYSTEM -> "Your fleet is inside a Star Chart system.";
            default -> otherFailure;
        };
    }
}
