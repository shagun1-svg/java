package smartdine;

public class Admin extends User {
    public Admin(String username, String password) {
        super(username, password);
    }

    @Override
    public void showDashboard() {
        new MainFrame().setVisible(true);
    }
}
