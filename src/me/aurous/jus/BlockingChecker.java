package me.aurous.jus;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.swing.JFrame;
import me.aurous.config.AppConstants;

/**
 *
 * @author Luke Fay
 */
public class BlockingChecker {

    static DynamicUpdater.Progress progress;
    static UpdateResult result = new UpdateResult() {

        @Override
        public void finished(long time, boolean updated) {
            System.out.println("Update finished");
            started();
            if (updated) {
                // should be properly integrated into the javafx ui eventually
                javax.swing.JOptionPane.showConfirmDialog(null, "Aurous has updated, and must restart.");
                Platform.exit();
            } else {
                working = false;
            }
        }

        @Override
        public void failed(long time, Exception exception) {
            System.out.println("Update failed!");
            // new ExceptionWidget? Will it work before initializing javafx?
            working = false;
        }

        @Override
        public void started() {
            // using awt for now
            java.awt.EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    JFrame update = new JFrame("Aurous is updating...");
                    update.add(new Component() {

                        {
                            Dimension size = new Dimension(798, 54);
                            setSize(size);
                            setPreferredSize(size);
                        }

                        @Override
                        public void paint(Graphics g) {
                            g.setColor(Color.DARK_GRAY);
                            g.fillRect(0, 0, 800, 100);
                            g.setColor(Color.WHITE);
                            g.drawRect(4, 24, 788, 24);
                            if(progress.current != null){
                                g.drawString("Download rate: " + progress.current.getDownloadRate() / 1024
                                    + "Kb/s", 8, 18);
                                int width = (int) (785 * progress.current.getProgress());
                                g.fillRect(6, 26, width, 21);
                            }
                        }

                    });
                    update.pack();
                    update.setLocationRelativeTo(null);
                    update.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    update.setVisible(true);
                }
            });
        }

    };
    
    public static void rebase() throws IOException, NoSuchAlgorithmException{
        DynamicUpdater updater = new DynamicUpdater();
        updater.addExclude("windows", "linux", "mac", "jre");
        updater.setUpdateDirectory(new File("./"));
        updater.rebase(new File("./update.json"));
        
    }

    public static void check() { // must be blocking
        DynamicUpdater updater = new DynamicUpdater();
        updater.setUpdateDirectory(new File("./"));
        updater.addExclude("windows", "linux", "mac", "jre");
        progress = updater.update("https://aurous.me/updates/", result);

        
        while (working) {
            try {
                Thread.sleep(32);
            } catch (InterruptedException ex) {
                Logger.getLogger(BlockingChecker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    static boolean working = false;

}
