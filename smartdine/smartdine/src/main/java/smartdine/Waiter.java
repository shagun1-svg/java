/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package smartdine;

public class Waiter extends User {
    public Waiter(String username, String password) {
        super(username, password);
    }

    @Override
    public void showDashboard() {
        new MainFrame ().setVisible(true);
    }
}
