package exiledsector.charts.officer.reward;

public record RewardCandidate(CarriedItem item, float weight) {

    public RewardCandidate withWeight(float replacementWeight) {
        return new RewardCandidate(item, replacementWeight);
    }
}
