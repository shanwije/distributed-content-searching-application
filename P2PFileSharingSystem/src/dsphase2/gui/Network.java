/*
 * CS4262 Distributed Systems Mini Project
 */
package dsphase2.gui;

import dsphase2.entities.Config;
import javax.swing.UIManager;

public class Network {


    public static void main(String[] args) {

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel(
                            UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                }
                ConfigGUI configWindow = new ConfigGUI();
                Config.CONFIG_WINDOW = configWindow;
                configWindow.setVisible(true);
            }
        });
    }
}
