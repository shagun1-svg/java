package smartdine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class MainFrame extends JFrame {
    private JTable menuTable;
    private JTextField searchField, customerNameField;
    private JComboBox<String> categoryComboBox;
    private DefaultTableModel menuModel;

    private DefaultListModel<CartItem> cartModel;
    private JList<CartItem> cartList;

    private Set<String> reservedTables = new HashSet<>();
    private String currentReservedTable = null;

    private AtomicInteger orderCounter = new AtomicInteger(1000); // Starting order number

    private JButton addToCartButton;
    private JLabel totalBillLabel;

    // List to hold all menu items
    private List<MenuItem> allMenuItems = new ArrayList<>();
    // List to hold all past orders
    private List<Order> allOrders = new ArrayList<>();

    // List to hold all available tables
    private DefaultListModel<String> tableListModel;
    private JList<String> tableList;

    // File paths for persistence
    private static final String MENU_FILE = "smartdine_menu.ser";
    private static final String TABLES_FILE = "smartdine_tables.ser";
    private static final String ORDERS_FILE = "smartdine_orders.ser";
    private static final String ORDER_COUNTER_FILE = "smartdine_order_counter.ser";
    private static final String BILLS_DIRECTORY = "bills"; // Directory to save individual bills

    // Custom class to represent a menu item (now Serializable)
    static class MenuItem implements Serializable {
        private static final long serialVersionUID = 1L; // For serialization versioning
        private String name;
        private String category;
        private double price;

        public MenuItem(String name, String category, double price) {
            this.name = name;
            this.category = category;
            this.price = price;
        }

        public String getName() { return name; }
        public String getCategory() { return category; }
        public double getPrice() { return price; }

        public void setName(String name) { this.name = name; }
        public void setCategory(String category) { this.category = category; }
        public void setPrice(double price) { this.price = price; }

        @Override
        public String toString() {
            return name + " (" + category + ") - $" + String.format("%.2f", price);
        }
    }

    // Custom class to represent an item in the cart with quantity (now Serializable)
    static class CartItem implements Serializable {
        private static final long serialVersionUID = 1L; // For serialization versioning
        private MenuItem menuItem;
        private int quantity;

        public CartItem(MenuItem menuItem, int quantity) {
            this.menuItem = menuItem;
            this.quantity = quantity;
        }

        public MenuItem getMenuItem() { return menuItem; }
        public int getQuantity() { return quantity; }

        public void setQuantity(int quantity) { this.quantity = quantity; }

        public double getTotalPrice() {
            return menuItem.getPrice() * quantity;
        }

        @Override
        public String toString() {
            return menuItem.getName() + " - $" + String.format("%.2f", menuItem.getPrice()) + " x " + quantity + " = $" + String.format("%.2f", getTotalPrice());
        }
    }

    // Custom class to represent a completed order (new, Serializable)
    static class Order implements Serializable {
        private static final long serialVersionUID = 1L; // For serialization versioning
        private int orderNumber;
        private String customerName;
        private String tableName;
        private List<CartItem> items;
        private double totalAmount;
        private LocalDateTime orderDateTime;

        public Order(int orderNumber, String customerName, String tableName, List<CartItem> items, double totalAmount, LocalDateTime orderDateTime) {
            this.orderNumber = orderNumber;
            this.customerName = customerName;
            this.tableName = tableName;
            this.items = new ArrayList<>(items); // Create a new list to avoid reference issues
            this.totalAmount = totalAmount;
            this.orderDateTime = orderDateTime;
        }

        public int getOrderNumber() { return orderNumber; }
        public String getCustomerName() { return customerName; }
        public String getTableName() { return tableName; }
        public List<CartItem> getItems() { return items; }
        public double getTotalAmount() { return totalAmount; }
        public LocalDateTime getOrderDateTime() { return orderDateTime; }

        @Override
        public String toString() {
            return "Order #" + orderNumber + " - " + customerName + " (Table: " + tableName + ") - $" + String.format("%.2f", totalAmount);
        }
    }

    // Custom cell renderer for the tableList to show reserved tables
    static class TableStatusCellRenderer extends DefaultListCellRenderer {
        private Set<String> reservedTables;

        public TableStatusCellRenderer(Set<String> reservedTables) {
            this.reservedTables = reservedTables;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof String) {
                String tableName = (String) value;
                if (reservedTables.contains(tableName)) {
                    c.setBackground(new Color(255, 200, 200)); // Light red for reserved
                } else {
                    c.setBackground(new Color(200, 230, 255)); // Light blue for free
                }
                if (isSelected) {
                    // Darken the background color if selected, while preserving the reserved/free color
                    Color currentBg = c.getBackground();
                    c.setBackground(new Color(Math.max(0, currentBg.getRed() - 20),
                            Math.max(0, currentBg.getGreen() - 20),
                            Math.max(0, currentBg.getBlue() - 20)));
                }
            }
            return c;
        }
    }

    // FileManager class for handling data persistence
    static class FileManager {

        // Saves a list of objects to a specified file using serialization
        public static <T> void saveToFile(List<T> data, String filename) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeObject(data);
                System.out.println("Data saved to " + filename);
            } catch (IOException e) {
                System.err.println("Error saving data to " + filename + ": " + e.getMessage());
            }
        }

        // Loads a list of objects from a specified file using deserialization
        @SuppressWarnings("unchecked") // Suppress unchecked cast warning
        public static <T> List<T> loadFromFile(String filename) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
                return (List<T>) ois.readObject();
            } catch (FileNotFoundException e) {
                System.out.println("File not found: " + filename + ". Returning empty list.");
                return new ArrayList<>();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading data from " + filename + ": " + e.getMessage());
                return new ArrayList<>();
            }
        }

        // Saves a Set of strings to a specified file
        public static void saveSetToFile(Set<String> data, String filename) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeObject(data);
                System.out.println("Set data saved to " + filename);
            } catch (IOException e) {
                System.err.println("Error saving set data to " + filename + ": " + e.getMessage());
            }
        }

        // Loads a Set of strings from a specified file
        @SuppressWarnings("unchecked")
        public static Set<String> loadSetFromFile(String filename) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
                return (Set<String>) ois.readObject();
            } catch (FileNotFoundException e) {
                System.out.println("File not found: " + filename + ". Returning empty set.");
                return new HashSet<>();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading set data from " + filename + ": " + e.getMessage());
                return new HashSet<>();
            }
        }

        // Saves an AtomicInteger to a file
        public static void saveAtomicInteger(AtomicInteger counter, String filename) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeObject(counter.get()); // Save the integer value
                System.out.println("AtomicInteger saved to " + filename);
            } catch (IOException e) {
                System.err.println("Error saving AtomicInteger to " + filename + ": " + e.getMessage());
            }
        }

        // Loads an AtomicInteger from a file
        public static AtomicInteger loadAtomicInteger(String filename, int defaultValue) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
                Integer loadedValue = (Integer) ois.readObject();
                System.out.println("AtomicInteger loaded from " + filename);
                return new AtomicInteger(loadedValue);
            } catch (FileNotFoundException e) {
                System.out.println("File not found: " + filename + ". Returning default AtomicInteger.");
                return new AtomicInteger(defaultValue);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading AtomicInteger from " + filename + ": " + e.getMessage());
                return new AtomicInteger(defaultValue);
            }
        }
    }


    public MainFrame() {
        setTitle("SmartDine – Restaurant Order and Table Management System");
        setSize(1200, 700); // Increased size for more content
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Changed to DO_NOTHING_ON_CLOSE to handle saving
        setLocationRelativeTo(null);

        // Add a WindowListener to handle saving data on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveAllData();
                dispose(); // Close the frame after saving
            }
        });

        initUI(); // Initialize UI components first
        loadAllData(); // Then load data, which will populate the initialized components

        // Ensure the bills directory exists
        File billsDir = new File(BILLS_DIRECTORY);
        if (!billsDir.exists()) {
            billsDir.mkdirs();
        }

        setVisible(true);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem adminSettings = new JMenuItem("Admin Settings");
        JMenuItem saveData = new JMenuItem("Save Data");
        JMenuItem loadData = new JMenuItem("Load Data"); // New menu item for loading
        JMenuItem viewPastOrders = new JMenuItem("View Past Orders"); // New menu item for viewing past orders

        adminSettings.addActionListener(e -> showAdminSettings());
        saveData.addActionListener(e -> saveAllData());
        loadData.addActionListener(e -> loadAllData()); // Add action listener for loading
        viewPastOrders.addActionListener(e -> showPastOrdersSearchDialog()); // Action listener for new menu item

        fileMenu.add(adminSettings);
        fileMenu.addSeparator(); // Separator for better organization
        fileMenu.add(saveData);
        fileMenu.add(loadData);
        fileMenu.add(viewPastOrders); // Add the new menu item
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);




        // Left Panel: Table List with Reservation Buttons
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(new Color(230, 240, 255));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel tableLabel = new JLabel("Tables (Select and Reserve)", SwingConstants.CENTER);
        tableLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        leftPanel.add(tableLabel, BorderLayout.NORTH);

        tableListModel = new DefaultListModel<>();
        // Table list will be populated by loadAllData()
        tableList = new JList<>(tableListModel);
        tableList.setCellRenderer(new TableStatusCellRenderer(reservedTables)); // Set custom renderer
        tableList.setBackground(new Color(200, 230, 255));
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Ensure only one table can be selected

        leftPanel.add(new JScrollPane(tableList), BorderLayout.CENTER);

        // Buttons Panel for Reserve and Free
        JPanel tableButtonPanel = new JPanel(new FlowLayout());
        JButton reserveButton = new JButton("Reserve Table");
        JButton freeButton = new JButton("Free Table");
        JButton addTableButton = new JButton("Add Table"); // New button for adding tables
        tableButtonPanel.add(reserveButton);
        tableButtonPanel.add(freeButton);
        tableButtonPanel.add(addTableButton); // Add new button
        leftPanel.add(tableButtonPanel, BorderLayout.SOUTH);

        // On table selection, update currentReservedTable only if reserved
        tableList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selected = tableList.getSelectedValue();
                    if (selected != null) {
                        if (reservedTables.contains(selected)) {
                            currentReservedTable = selected;
                        } else {
                            currentReservedTable = null; // Don't allow adding to cart for unreserved tables
                        }
                    } else {
                        currentReservedTable = null;
                    }
                    updateAddToCartButtonState();
                }
            }
        });

        reserveButton.addActionListener(e -> {
            String selectedTable = tableList.getSelectedValue();
            if (selectedTable == null) {
                JOptionPane.showMessageDialog(this, "Please select a table first.");
                return;
            }

            if (reservedTables.contains(selectedTable)) {
                JOptionPane.showMessageDialog(this, selectedTable + " is already reserved.");
            } else {
                reservedTables.add(selectedTable);
                currentReservedTable = selectedTable;
                tableList.repaint(); // Repaint to update color
                JOptionPane.showMessageDialog(this, selectedTable + " is now reserved.");
            }
            updateAddToCartButtonState();
        });

        freeButton.addActionListener(e -> {
            String selectedTable = tableList.getSelectedValue();
            if (selectedTable == null) {
                JOptionPane.showMessageDialog(this, "Please select a table first.");
                return;
            }

            if (reservedTables.contains(selectedTable)) {
                reservedTables.remove(selectedTable);
                tableList.repaint(); // Repaint to update color
                JOptionPane.showMessageDialog(this, selectedTable + " reservation has been cancelled.");
                if (selectedTable.equals(currentReservedTable)) {
                    currentReservedTable = null;
                }
            } else {
                JOptionPane.showMessageDialog(this, selectedTable + " is not currently reserved.");
            }
            updateAddToCartButtonState();
        });

        addTableButton.addActionListener(e -> {
            String newTableName = JOptionPane.showInputDialog(this, "Enter new table name:");
            if (newTableName != null && !newTableName.trim().isEmpty()) {
                if (tableListModel.contains(newTableName.trim())) {
                    JOptionPane.showMessageDialog(this, "Table with this name already exists.");
                } else {
                    tableListModel.addElement(newTableName.trim());
                    JOptionPane.showMessageDialog(this, newTableName.trim() + " added successfully.");
                }
            }
        });

        add(leftPanel, BorderLayout.WEST);

        // Center Panel: Menu
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel menuFilterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterMenu(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterMenu(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterMenu(); }
        });
        categoryComboBox = new JComboBox<>(new String[]{"All", "Appetizers", "Main Course", "Dessert", "Drinks"});
        categoryComboBox.addActionListener(e -> filterMenu());

        menuFilterPanel.add(new JLabel("Search:"));
        menuFilterPanel.add(searchField);
        menuFilterPanel.add(new JLabel("Category:"));
        menuFilterPanel.add(categoryComboBox);
        centerPanel.add(menuFilterPanel, BorderLayout.NORTH);

        String[] columns = {"Item", "Category", "Price"};
        menuModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make menu table non-editable
            }
        };
        menuTable = new JTable(menuModel);
        menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        centerPanel.add(new JScrollPane(menuTable), BorderLayout.CENTER);

        // filterMenu() will be called by loadAllData()

        // Buttons for Add/Remove to/from Cart
        JPanel buttonPanel = new JPanel();
        addToCartButton = new JButton("Add to Cart");
        JButton removeFromCartButton = new JButton("Remove from Cart");
        buttonPanel.add(addToCartButton);
        buttonPanel.add(removeFromCartButton);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);

        cartModel = new DefaultListModel<>();
        cartList = new JList<>(cartModel);
        cartList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        addToCartButton.addActionListener(e -> {
            if (currentReservedTable == null) {
                JOptionPane.showMessageDialog(this, "Please select and reserve a table before adding items.");
                return;
            }

            int selectedRow = menuTable.getSelectedRow();
            if (selectedRow != -1) {
                String itemName = menuModel.getValueAt(selectedRow, 0).toString();
                String itemCategory = menuModel.getValueAt(selectedRow, 1).toString();
                double itemPrice = Double.parseDouble(menuModel.getValueAt(selectedRow, 2).toString());

                MenuItem selectedMenuItem = new MenuItem(itemName, itemCategory, itemPrice);

                String quantityStr = JOptionPane.showInputDialog(this, "Enter quantity for " + itemName + ":", "Quantity", JOptionPane.QUESTION_MESSAGE);
                int quantity = 1; // Default quantity
                if (quantityStr != null && !quantityStr.trim().isEmpty()) {
                    try {
                        quantity = Integer.parseInt(quantityStr);
                        if (quantity <= 0) {
                            JOptionPane.showMessageDialog(this, "Quantity must be a positive number.", "Invalid Quantity", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid quantity. Please enter a number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // Check if item already exists in cart, if so, update quantity
                boolean found = false;
                for (int i = 0; i < cartModel.size(); i++) {
                    CartItem existingItem = cartModel.getElementAt(i);
                    if (existingItem.getMenuItem().getName().equals(selectedMenuItem.getName())) {
                        existingItem.setQuantity(existingItem.getQuantity() + quantity);
                        cartModel.setElementAt(existingItem, i); // Update the element in the model
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    cartModel.addElement(new CartItem(selectedMenuItem, quantity));
                }
                updateBillTotal(); // Call to update total
            } else {
                JOptionPane.showMessageDialog(this, "Please select an item from the menu to add to cart.");
            }
        });

        removeFromCartButton.addActionListener(e -> {
            int selectedIndex = cartList.getSelectedIndex();
            if (selectedIndex != -1) {
                cartModel.remove(selectedIndex);
                updateBillTotal(); // Call to update total
            } else {
                JOptionPane.showMessageDialog(this, "Please select an item from the cart to remove.");
            }
        });

        // Disable Add to Cart initially
        addToCartButton.setEnabled(false);

        add(centerPanel, BorderLayout.CENTER);

        // Right Panel: Cart and Bill
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(240, 255, 250));
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel cartLabel = new JLabel("Customer Cart", SwingConstants.CENTER);
        cartLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        cartLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(cartLabel);
        rightPanel.add(Box.createVerticalStrut(10)); // Spacer

        JLabel nameLabel = new JLabel("Customer Name:");
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        customerNameField = new JTextField();
        customerNameField.setMaximumSize(new Dimension(250, 25)); // Slightly wider
        customerNameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane cartScrollPane = new JScrollPane(cartList);
        cartScrollPane.setPreferredSize(new Dimension(250, 300));
        cartScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        totalBillLabel = new JLabel("Total: $0.00");
        totalBillLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalBillLabel.setAlignmentX(Component.LEFT_ALIGNMENT);


        JButton generateBillButton = new JButton("Generate Bill");
        generateBillButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        generateBillButton.addActionListener(e -> {
            String customerName = customerNameField.getText().trim();
            if (customerName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a customer name.");
                return;
            }
            if (cartModel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cart is empty.");
                return;
            }
            if (currentReservedTable == null) {
                JOptionPane.showMessageDialog(this, "No reserved table selected to generate a bill.");
                return;
            }

            int orderNumber = orderCounter.getAndIncrement();
            double totalAmount = calculateBillTotal();

            // Create a new list to hold cart items for the order
            List<CartItem> orderItems = new ArrayList<>();
            for (int i = 0; i < cartModel.size(); i++) {
                orderItems.add(cartModel.getElementAt(i));
            }

            // Create a new Order object and add it to allOrders
            Order newOrder = new Order(orderNumber, customerName, currentReservedTable,
                    orderItems, // Use the new list of cart items
                    totalAmount, LocalDateTime.now());
            allOrders.add(newOrder);

            StringBuilder bill = new StringBuilder();
            bill.append("--- SmartDine Bill ---\n\n");
            bill.append("Order Number: ").append(orderNumber).append("\n");
            bill.append("Table: ").append(currentReservedTable).append("\n");
            bill.append("Customer: ").append(customerName).append("\n");
            bill.append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
            bill.append("Items:\n");
            for (CartItem item : newOrder.getItems()) { // Use items from the new Order object
                bill.append(String.format("- %-25s x %-3d $%.2f\n", item.getMenuItem().getName(), item.getQuantity(), item.getTotalPrice()));
            }
            bill.append("\n-----------------------------------\n");
            bill.append(String.format("Total: $%.2f\n", totalAmount));
            bill.append("-----------------------------------\n");
            bill.append("Thank you for dining with SmartDine!\n");

            // Save the bill to an individual file
            saveBillToFile(orderNumber, bill.toString());

            JOptionPane.showMessageDialog(this, bill.toString(), "Bill Summary", JOptionPane.INFORMATION_MESSAGE);

            // Clear cart and customer name after generating bill
            cartModel.clear();
            customerNameField.setText("");

            // Free the table after billing
            reservedTables.remove(currentReservedTable);

            currentReservedTable = null; // Clear the current table selection
            tableList.clearSelection(); // Clear table selection in the list
            tableList.repaint(); // Repaint to update table color if currentReservedTable was freed

            updateBillTotal(); // Reset total to 0.00 after clearing cart
            updateAddToCartButtonState(); // Disable add to cart button
        });

        rightPanel.add(nameLabel);
        rightPanel.add(customerNameField);
        rightPanel.add(Box.createVerticalStrut(10)); // Spacer
        rightPanel.add(new JLabel("Cart Items:"));
        rightPanel.add(cartScrollPane);
        rightPanel.add(Box.createVerticalStrut(10)); // Spacer
        rightPanel.add(totalBillLabel);
        rightPanel.add(Box.createVerticalGlue()); // Pushes everything to the top
        rightPanel.add(generateBillButton);

        add(rightPanel, BorderLayout.EAST);

        // Initial update for bill total and add to cart button state
        updateBillTotal();
        updateAddToCartButtonState();
    }

    private void updateAddToCartButtonState() {
        addToCartButton.setEnabled(currentReservedTable != null);
    }

    private void loadMenuItemsDefault() {
        // Add default menu items if no data is loaded from file
        allMenuItems.add(new MenuItem("Burger", "Main Course", 5.99));
        allMenuItems.add(new MenuItem("Fries", "Appetizers", 2.99));
        allMenuItems.add(new MenuItem("Coke", "Drinks", 1.50));
        allMenuItems.add(new MenuItem("Ice Cream", "Dessert", 3.25));
        allMenuItems.add(new MenuItem("Pizza (Large)", "Main Course", 12.50));
        allMenuItems.add(new MenuItem("Salad (Caesar)", "Appetizers", 4.75));
        allMenuItems.add(new MenuItem("Lemonade", "Drinks", 2.00));
        allMenuItems.add(new MenuItem("Cheesecake", "Dessert", 4.00));
    }

    private void filterMenu() {
        menuModel.setRowCount(0); // Clear current table
        String selectedCategory = (String) categoryComboBox.getSelectedItem();
        String searchText = searchField.getText().trim().toLowerCase();

        for (MenuItem item : allMenuItems) {
            boolean categoryMatch = (selectedCategory.equals("All") || item.getCategory().equals(selectedCategory));
            boolean searchMatch = (searchText.isEmpty() || item.getName().toLowerCase().contains(searchText));

            if (categoryMatch && searchMatch) {
                menuModel.addRow(new Object[]{item.getName(), item.getCategory(), String.format("%.2f", item.getPrice())});
            }
        }
    }

    // This method calculates the total bill and updates the totalBillLabel
    private double updateBillTotal() {
        double total = 0.0;
        for (int i = 0; i < cartModel.size(); i++) {
            CartItem item = cartModel.getElementAt(i);
            total += item.getTotalPrice();
        }
        totalBillLabel.setText(String.format("Total: $%.2f", total));
        return total;
    }

    // This method is specifically for the Generate Bill button to get the final total
    private double calculateBillTotal() {
        return updateBillTotal(); // Simply re-use the update method
    }

    private void saveAllData() {
        FileManager.saveToFile(allMenuItems, MENU_FILE);
        FileManager.saveSetToFile(reservedTables, TABLES_FILE);
        FileManager.saveToFile(allOrders, ORDERS_FILE);
        FileManager.saveAtomicInteger(orderCounter, ORDER_COUNTER_FILE);
        JOptionPane.showMessageDialog(this, "All data saved successfully!");
    }

    private void loadAllData() {
        allMenuItems = FileManager.loadFromFile(MENU_FILE);
        if (allMenuItems.isEmpty()) {
            loadMenuItemsDefault(); // Load default if no data was found
        }
        reservedTables = FileManager.loadSetFromFile(TABLES_FILE);
        allOrders = FileManager.loadFromFile(ORDERS_FILE);
        orderCounter = FileManager.loadAtomicInteger(ORDER_COUNTER_FILE, 1000); // Load with default if not found

        // Populate table list model
        tableListModel.clear();
        for (int i = 1; i <= 10; i++) { // Assuming initial tables T1 to T10, adjust as needed
            tableListModel.addElement("T" + i);
        }
        // Add any tables loaded from file that aren't already in the model (e.g., if new tables were added via admin)
        for (String reservedTable : reservedTables) {
            if (!tableListModel.contains(reservedTable)) {
                tableListModel.addElement(reservedTable);
            }
        }
        // Also ensure all tables from previous orders are in the tableListModel in case of data corruption/manual deletion
        for(Order order : allOrders) {
            if (!tableListModel.contains(order.getTableName())) {
                tableListModel.addElement(order.getTableName());
            }
        }
        // Sort the table list for better presentation
        List<String> sortedTables = new ArrayList<>();
        for (int i = 0; i < tableListModel.size(); i++) {
            sortedTables.add(tableListModel.getElementAt(i));
        }
        sortedTables.sort(null); // Natural order sorting
        tableListModel.clear();
        for (String table : sortedTables) {
            tableListModel.addElement(table);
        }

        filterMenu(); // Re-populate menu table after loading menu items
        JOptionPane.showMessageDialog(this, "All data loaded successfully!");
    }

    private void saveBillToFile(int orderNumber, String billContent) {
        File billsDir = new File(BILLS_DIRECTORY);
        if (!billsDir.exists()) {
            billsDir.mkdirs(); // Create the directory if it doesn't exist
        }

        String filename = BILLS_DIRECTORY + File.separator + "order_" + orderNumber + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(billContent);
            System.out.println("Bill for order #" + orderNumber + " saved to " + filename);
        } catch (IOException e) {
            System.err.println("Error saving bill to file: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error saving bill to file: " + e.getMessage(), "File Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPastOrdersSearchDialog() {
        JDialog searchDialog = new JDialog(this, "Search Past Orders", true);
        searchDialog.setSize(400, 300);
        searchDialog.setLocationRelativeTo(this);
        searchDialog.setLayout(new BorderLayout(10, 10));
        ((JComponent) searchDialog.getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));




        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JTextField orderNumberField = new JTextField(10);
        JButton searchButton = new JButton("Search Bill");
        inputPanel.add(new JLabel("Enter Order Number:"));
        inputPanel.add(orderNumberField);
        inputPanel.add(searchButton);
        searchDialog.add(inputPanel, BorderLayout.NORTH);

        JTextArea billDisplayArea = new JTextArea();
        billDisplayArea.setEditable(false);
        billDisplayArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(billDisplayArea);
        searchDialog.add(scrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(e -> {
            String orderNumStr = orderNumberField.getText().trim();
            if (orderNumStr.isEmpty()) {
                billDisplayArea.setText("Please enter an order number.");
                return;
            }

            try {
                int orderNumber = Integer.parseInt(orderNumStr);
                String filename = BILLS_DIRECTORY + File.separator + "order_" + orderNumber + ".txt";
                File billFile = new File(filename);

                if (billFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(billFile))) {
                        StringBuilder billContent = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            billContent.append(line).append("\n");
                        }
                        billDisplayArea.setText(billContent.toString());
                    } catch (IOException ex) {
                        billDisplayArea.setText("Error reading bill file: " + ex.getMessage());
                        System.err.println("Error reading bill file: " + ex.getMessage());
                    }
                } else {
                    billDisplayArea.setText("Bill for Order #" + orderNumber + " not found.");
                }
            } catch (NumberFormatException ex) {
                billDisplayArea.setText("Invalid order number. Please enter a numeric value.");
            }
        });

        searchDialog.setVisible(true);
    }


    private void showAdminSettings() {
        JDialog adminDialog = new JDialog(this, "Admin Settings", true);
        adminDialog.setSize(500, 400);
        adminDialog.setLocationRelativeTo(this);
        adminDialog.setLayout(new BorderLayout());

        // Panel for menu item management
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.setBorder(BorderFactory.createTitledBorder("Menu Management"));

        // Table for current menu items in admin panel
        String[] adminColumns = {"Item", "Category", "Price"};
        DefaultTableModel adminMenuModel = new DefaultTableModel(adminColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make admin menu table non-editable
            }
        };
        JTable adminMenuTable = new JTable(adminMenuModel);
        adminMenuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuPanel.add(new JScrollPane(adminMenuTable), BorderLayout.CENTER);

        // Populate admin menu table
        for (MenuItem item : allMenuItems) {
            adminMenuModel.addRow(new Object[]{item.getName(), item.getCategory(), String.format("%.2f", item.getPrice())});
        }

        // Input fields for adding/editing menu items
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        JTextField itemNameField = new JTextField();
        JComboBox<String> itemCategoryComboBox = new JComboBox<>(new String[]{"Appetizers", "Main Course", "Dessert", "Drinks"});
        JTextField itemPriceField = new JTextField();

        inputPanel.add(new JLabel("Item Name:"));
        inputPanel.add(itemNameField);
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(itemCategoryComboBox);
        inputPanel.add(new JLabel("Price:"));
        inputPanel.add(itemPriceField);

        // Populate input fields when a row in adminMenuTable is selected
        adminMenuTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && adminMenuTable.getSelectedRow() != -1) {
                    int selectedRow = adminMenuTable.getSelectedRow();
                    itemNameField.setText(adminMenuModel.getValueAt(selectedRow, 0).toString());
                    itemCategoryComboBox.setSelectedItem(adminMenuModel.getValueAt(selectedRow, 1).toString());
                    itemPriceField.setText(adminMenuModel.getValueAt(selectedRow, 2).toString());
                }
            }
        });

        JPanel adminButtonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Add Item");
        JButton updateButton = new JButton("Update Item");
        JButton deleteButton = new JButton("Delete Item"); // Add delete button
        JButton clearFieldsButton = new JButton("Clear Fields"); // Add clear fields button

        adminButtonPanel.add(addButton);
        adminButtonPanel.add(updateButton);
        adminButtonPanel.add(deleteButton);
        adminButtonPanel.add(clearFieldsButton);

        addButton.addActionListener(e -> {
            String name = itemNameField.getText().trim();
            String category = (String) itemCategoryComboBox.getSelectedItem();
            String priceStr = itemPriceField.getText().trim();

            if (name.isEmpty() || priceStr.isEmpty()) {
                JOptionPane.showMessageDialog(adminDialog, "Please fill in all fields to add an item.");
                return;
            }
            try {
                double price = Double.parseDouble(priceStr);
                if (price <= 0) {
                    JOptionPane.showMessageDialog(adminDialog, "Price must be a positive number.");
                    return;
                }

                // Check for duplicate item name
                for (MenuItem item : allMenuItems) {
                    if (item.getName().equalsIgnoreCase(name)) {
                        JOptionPane.showMessageDialog(adminDialog, "An item with this name already exists.");
                        return;
                    }
                }

                MenuItem newItem = new MenuItem(name, category, price);
                allMenuItems.add(newItem);
                adminMenuModel.addRow(new Object[]{newItem.getName(), newItem.getCategory(), String.format("%.2f", newItem.getPrice())});
                filterMenu(); // Update main menu table
                JOptionPane.showMessageDialog(adminDialog, "Item added successfully!");
                itemNameField.setText("");
                itemPriceField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(adminDialog, "Invalid price. Please enter a valid number.");
            }
        });

        updateButton.addActionListener(e -> {
            int selectedRow = adminMenuTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(adminDialog, "Please select an item to update.");
                return;
            }

            String oldName = adminMenuModel.getValueAt(selectedRow, 0).toString();
            String newName = itemNameField.getText().trim();
            String newCategory = (String) itemCategoryComboBox.getSelectedItem();
            String newPriceStr = itemPriceField.getText().trim();

            if (newName.isEmpty() || newPriceStr.isEmpty()) {
                JOptionPane.showMessageDialog(adminDialog, "Please fill in all fields to update an item.");
                return;
            }

            try {
                double newPrice = Double.parseDouble(newPriceStr);
                if (newPrice <= 0) {
                    JOptionPane.showMessageDialog(adminDialog, "Price must be a positive number.");
                    return;
                }

                // Find the MenuItem object in allMenuItems
                MenuItem itemToUpdate = null;
                for (MenuItem item : allMenuItems) {
                    if (item.getName().equals(oldName)) {
                        itemToUpdate = item;
                        break;
                    }
                }

                if (itemToUpdate != null) {
                    // Check if new name already exists and is different from old name
                    if (!oldName.equalsIgnoreCase(newName)) {
                        for (MenuItem item : allMenuItems) {
                            if (item.getName().equalsIgnoreCase(newName)) {
                                JOptionPane.showMessageDialog(adminDialog, "An item with the new name already exists.");
                                return;
                            }
                        }
                    }

                    itemToUpdate.setName(newName);
                    itemToUpdate.setCategory(newCategory);
                    itemToUpdate.setPrice(newPrice);

                    adminMenuModel.setValueAt(itemToUpdate.getName(), selectedRow, 0);
                    adminMenuModel.setValueAt(itemToUpdate.getCategory(), selectedRow, 1);
                    adminMenuModel.setValueAt(String.format("%.2f", itemToUpdate.getPrice()), selectedRow, 2);

                    filterMenu(); // Update main menu table
                    JOptionPane.showMessageDialog(adminDialog, "Item updated successfully!");
                    itemNameField.setText("");
                    itemPriceField.setText("");
                    adminMenuTable.clearSelection();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(adminDialog, "Invalid price. Please enter a valid number.");
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedRow = adminMenuTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(adminDialog, "Please select an item to delete.");
                return;
            }

            String itemName = adminMenuModel.getValueAt(selectedRow, 0).toString();
            int confirm = JOptionPane.showConfirmDialog(adminDialog, "Are you sure you want to delete " + itemName + "?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // Remove from allMenuItems list
                allMenuItems.removeIf(item -> item.getName().equals(itemName));
                adminMenuModel.removeRow(selectedRow);
                filterMenu(); // Update main menu table
                JOptionPane.showMessageDialog(adminDialog, "Item deleted successfully!");
                itemNameField.setText("");
                itemPriceField.setText("");
            }
        });

        clearFieldsButton.addActionListener(e -> {
            itemNameField.setText("");
            itemPriceField.setText("");
            itemCategoryComboBox.setSelectedIndex(0); // Reset to first category
            adminMenuTable.clearSelection(); // Clear selection in the table
        });

        menuPanel.add(inputPanel, BorderLayout.NORTH);
        menuPanel.add(adminButtonPanel, BorderLayout.SOUTH);

        // Panel for viewing past orders in admin settings (optional, can be removed if the separate dialog is sufficient)
        JPanel ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBorder(BorderFactory.createTitledBorder("Past Orders History"));

        DefaultTableModel orderHistoryModel = new DefaultTableModel(new String[]{"Order #", "Customer", "Table", "Total", "Date/Time"}, 0);
        JTable orderHistoryTable = new JTable(orderHistoryModel) {
            // Make cells non-editable
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        orderHistoryTable.getTableHeader().setReorderingAllowed(false); // Prevent column reordering
        orderHistoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Allow single selection

        // Populate order history table
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Order order : allOrders) {
            orderHistoryModel.addRow(new Object[]{
                    order.getOrderNumber(),
                    order.getCustomerName(),
                    order.getTableName(),
                    String.format("%.2f", order.getTotalAmount()),
                    order.getOrderDateTime().format(formatter)
            });
        }

        JScrollPane orderHistoryScrollPane = new JScrollPane(orderHistoryTable);
        ordersPanel.add(orderHistoryScrollPane, BorderLayout.CENTER);

        // Button to view details of a selected order (in a new dialog)
        JButton viewOrderDetailsButton = new JButton("View Order Details");
        viewOrderDetailsButton.addActionListener(e -> {
            int selectedRow = orderHistoryTable.getSelectedRow();
            if (selectedRow != -1) {
                int orderNum = (int) orderHistoryModel.getValueAt(selectedRow, 0);
                // Find the corresponding Order object
                Order selectedOrder = null;
                for (Order order : allOrders) {
                    if (order.getOrderNumber() == orderNum) {
                        selectedOrder = order;
                        break;
                    }
                }

                if (selectedOrder != null) {
                    StringBuilder details = new StringBuilder();
                    details.append("Order Details for #").append(selectedOrder.getOrderNumber()).append("\n");
                    details.append("Customer: ").append(selectedOrder.getCustomerName()).append("\n");
                    details.append("Table: ").append(selectedOrder.getTableName()).append("\n");
                    details.append("Date/Time: ").append(selectedOrder.getOrderDateTime().format(formatter)).append("\n\n");
                    details.append("Items:\n");
                    for (CartItem item : selectedOrder.getItems()) {
                        details.append(String.format("  - %-25s x %-3d $%.2f\n", item.getMenuItem().getName(), item.getQuantity(), item.getTotalPrice()));
                    }
                    details.append("\nTotal Amount: $").append(String.format("%.2f", selectedOrder.getTotalAmount())).append("\n");
                    JOptionPane.showMessageDialog(adminDialog, details.toString(), "Order Details", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(adminDialog, "Please select an order to view details.");
            }
        });
        JPanel orderDetailsButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        orderDetailsButtonPanel.add(viewOrderDetailsButton);
        ordersPanel.add(orderDetailsButtonPanel, BorderLayout.SOUTH);

        // Split pane to divide menu management and order history
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, menuPanel, ordersPanel);
        splitPane.setDividerLocation(200); // Adjust as needed
        adminDialog.add(splitPane, BorderLayout.CENTER);

        adminDialog.setVisible(true);
    }
}