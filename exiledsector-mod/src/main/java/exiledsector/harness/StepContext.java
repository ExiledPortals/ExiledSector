package exiledsector.harness;

record StepContext(int frame, float seconds, HarnessLog log, int firstLogLine, ScreenCapture capture, HarnessResults results) {

    boolean firstFrame() {
        return frame == 0;
    }
}
