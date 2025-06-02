package smartdine;


import java.io.Serializable;

public class MenuItem implements Serializable {
    private String name;
    private String category;
    private double price;

    public MenuItem(String name, String category, double price) {
        this.name = name;
        this.category = category;
        this.price = price;
    }


    public double getPrice() {
        return price;
    }


    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
