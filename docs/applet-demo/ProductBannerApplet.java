import javax.swing.*;
import java.awt.*;

/**
 * Syllabus demo: Applet life cycle (init, start, stop, destroy, paint).
 *
 * Applets are OBSOLETE: browsers no longer run them and the Applet API was removed from newer JDKs
 * (JDK 21 still compiles it with deprecation warnings). That is why this file is NOT part of the Maven build
 * and why ShopSphere uses Servlets/JSP for the web. Use it only to explain the lifecycle in the viva.
 *
 * Compile and run with JDK 21:
 *     javac ProductBannerApplet.java
 *     appletviewer is gone, so we host the applet in a JFrame:   java ProductBannerApplet
 */
@SuppressWarnings({"removal", "deprecation"})
public class ProductBannerApplet extends JApplet implements Runnable {
    private static final long serialVersionUID = 1L;
    private String message;
    private int x;
    private volatile boolean running;
    private Thread thread;

    @Override public void init() {            // 1. called once when the applet is loaded
        message = getParameter("message") != null ? getParameter("message") : "ShopSphere - Big Sale: up to 35% off!";
        x = 0;
        System.out.println("init()");
    }

    @Override public void start() {           // 2. called after init() and every time the page becomes visible
        running = true;
        thread = new Thread(this);
        thread.start();
        System.out.println("start()");
    }

    @Override public void run() {
        while (running) {
            x = (x + 4) % Math.max(getWidth(), 1);
            repaint();
            try { Thread.sleep(40); } catch (InterruptedException e) { return; }
        }
    }

    @Override public void paint(Graphics g) { // 3. called whenever the applet must draw itself
        g.setColor(new Color(0x4f46e5));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString(message, x, getHeight() / 2 + 7);
    }

    @Override public void stop() { running = false; System.out.println("stop()"); }       // 4. page hidden
    @Override public void destroy() { System.out.println("destroy()"); }                 // 5. applet unloaded

    /** Hosts the applet in a normal window (a browser plug-in is no longer available). */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Applet host");
            ProductBannerApplet applet = new ProductBannerApplet();
            f.add(applet);
            f.setSize(520, 80);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            applet.init();
            f.setVisible(true);
            applet.start();
        });
    }
}
