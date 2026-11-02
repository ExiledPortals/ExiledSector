package exiledsector.items;

public interface GrowableMaterial {

    String itemId();

    boolean growable();

    int feedstockCost();
}
