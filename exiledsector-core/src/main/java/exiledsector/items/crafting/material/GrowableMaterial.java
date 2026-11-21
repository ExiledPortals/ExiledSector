package exiledsector.items.crafting.material;

public interface GrowableMaterial {

    String itemId();

    boolean growable();

    int feedstockCost();
}
