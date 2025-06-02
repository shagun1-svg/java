package smartdine;

import java.io.Serializable;
import java.util.ArrayList;

public class Order implements Serializable {
    private int tableNumber;
    private ArrayList<MenuItem> items;
    private double total;

    // Constructor
    public Order(int tableNumber, ArrayList<MenuItem> items) {
        this.tableNumber = tableNumber;
        this.items = items;
        calculateTotal();
    }

    // Method to calculate the total bill
    public void calculateTotal() {
        total = 0;
        for (MenuItem item : items) {
            total += item.getPrice();  //
        }
    }

    // Getters
    public int getTableNumber() {
        return tableNumber;
    }

    public ArrayList<MenuItem> getItems() {
        return items;
    }

    public double getTotal() {
        return total;
    }

    // Setters
    public void setItems(ArrayList<MenuItem> items) {
        this.items = items;
        calculateTotal();  // Recalculate total when items are updated
    }

    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
    }
}
