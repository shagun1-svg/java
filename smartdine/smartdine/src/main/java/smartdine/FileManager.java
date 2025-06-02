/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package smartdine;

import java.io.*;
import java.util.ArrayList;

public class FileManager {

    public static void saveMenu(ArrayList<MenuItem> menu) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("menu.dat"))) {
            out.writeObject(menu);
        }
    }

    public static ArrayList<MenuItem> loadMenu() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("menu.dat"))) {
            return (ArrayList<MenuItem>) in.readObject();
        }
    }
}
