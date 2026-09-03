// Hypercube 4D → 3D → 2D — Java Swing (zero deps)
// Author: Gustavo A.F (Lowingx) — 2026-09-03
// Muse Spark v1.2 — sem Processing, só javac + java
// Run: javac Hypercube.java && java Hypercube

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class Hypercube extends JPanel implements ActionListener {

    // --- 4D geometry ---
    static final double[][] VERTS = new double[16][4];
    static final int[][] EDGES = {
        {0,1},{0,2},{0,4},{0,8},
        {1,3},{1,5},{1,9},
        {2,3},{2,6},{2,10},
        {3,7},{3,11},
        {4,5},{4,6},{4,12},
        {5,7},{5,13},
        {6,7},{6,14},
        {7,15},
        {8,9},{8,10},{8,12},
        {9,11},{9,13},
        {10,11},{10,14},
        {11,15},
        {12,13},{12,14},
        {13,15},
        {14,15}
    };
    static {
        for (int i = 0; i < 16; i++) {
            VERTS[i][0] = ((i & 8) == 0) ? -1 : 1;
            VERTS[i][1] = ((i & 4) == 0) ? -1 : 1;
            VERTS[i][2] = ((i & 2) == 0) ? -1 : 1;
            VERTS[i][3] = ((i & 1) == 0) ? -1 : 1;
        }
    }

    double t = 0;
    Timer timer;

    // palette — same as GLSL: 0.5 + 0.5*cos(2pi*(t + offset))
    static Color palette(double hue) {
        double r = 0.5 + 0.5 * Math.cos(6.28318 * (hue + 0.00));
        double g = 0.5 + 0.5 * Math.cos(6.28318 * (hue + 0.10));
        double b = 0.5 + 0.5 * Math.cos(6.28318 * (hue + 0.20));
        return new Color((float) r, (float) g, (float) b);
    }

    double[] rotate4D(double[] v, double ax, double ay, double az, double aw) {
        double x = v[0], y = v[1], z = v[2], w = v[3];
        // XW
        { double c = Math.cos(ax), s = Math.sin(ax); double nx = x*c - w*s, nw = x*s + w*c; x = nx; w = nw; }
        // YW
        { double c = Math.cos(ay), s = Math.sin(ay); double ny = y*c - w*s, nw = y*s + w*c; y = ny; w = nw; }
        // ZW
        { double c = Math.cos(az), s = Math.sin(az); double nz = z*c - w*s, nw = z*s + w*c; z = nz; w = nw; }
        // XY
        { double c = Math.cos(aw), s = Math.sin(aw); double nx = x*c - y*s, ny = x*s + y*c; x = nx; y = ny; }
        return new double[]{x, y, z, w};
    }

    Hypercube() {
        setBackground(new Color(5, 5, 12));
        setPreferredSize(new Dimension(700, 700));
        timer = new Timer(16, this); // ~60fps
        timer.start();

        // drag to scrub time
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                t = e.getX() * 0.01;
                repaint();
            }
        });
        // click to pause/resume
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (timer.isRunning()) timer.stop(); else timer.start();
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        t += 0.015;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int W = getWidth(), H = getHeight();
        double cx = W * 0.5, cy = H * 0.5;
        double scale = Math.min(W, H) * 0.22;

        double ax = t * 0.7, ay = t * 0.5, az = t * 0.3, aw = t * 0.4;

        // project all vertices
        double[] px = new double[16], py = new double[16], depth = new double[16];
        for (int i = 0; i < 16; i++) {
            double[] r = rotate4D(VERTS[i], ax, ay, az, aw);
            // 4D -> 3D perspective (d=5 avoids blow-up; w in [-1,1] → factor 0.83..1.25)
            double w4 = 5.0 / (5.0 + r[3]);
            double x3 = r[0] * w4, y3 = r[1] * w4, z3 = r[2] * w4;
            // 3D -> 2D perspective
            double w3 = 5.0 / (5.0 + z3);
            px[i] = cx + x3 * w3 * scale;
            py[i] = cy - y3 * w3 * scale; // Y flip (screen)
            depth[i] = z3;
        }

        // --- edges: back to front for correct overlap ---
        // simple depth sort via avg depth
        Integer[] order = new Integer[32];
        for (int i = 0; i < 32; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> {
            double da = (depth[EDGES[a][0]] + depth[EDGES[a][1]]) * 0.5;
            double db = (depth[EDGES[b][0]] + depth[EDGES[b][1]]) * 0.5;
            return Double.compare(da, db);
        });

        for (int idx : order) {
            int a = EDGES[idx][0], b = EDGES[idx][1];
            double avgD = (depth[a] + depth[b]) * 0.5;
            double brightness = clamp(smoothstep(2.0, -2.0, avgD), 0, 1);
            double hue = idx / 32.0 + t * 0.15 * 0.3;

            Color base = palette(hue);
            // brightness modulates alpha + saturation
            float alpha = (float)(0.35 + 0.65 * brightness);

            // outer glow (wide, transparent)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.12f));
            g2.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(base);
            g2.drawLine((int)px[a], (int)py[a], (int)px[b], (int)py[b]);

            // inner glow
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.30f));
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int)px[a], (int)py[a], (int)px[b], (int)py[b]);

            // core (thin, bright)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // core slightly brighter
            Color core = new Color(
                Math.min(1f, base.getRed()/255f * 0.5f + 0.5f),
                Math.min(1f, base.getGreen()/255f * 0.5f + 0.5f),
                Math.min(1f, base.getBlue()/255f * 0.5f + 0.5f)
            );
            g2.setColor(core);
            g2.drawLine((int)px[a], (int)py[a], (int)px[b], (int)py[b]);
        }

        // --- vertices ---
        for (int i = 0; i < 16; i++) {
            double br = clamp(smoothstep(2.0, -2.0, depth[i]), 0, 1);
            float alpha = (float)(0.5 + 0.5 * br);
            Color vc = palette(i / 16.0 + t * 0.15 * 0.5);

            // glow
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.15f));
            g2.setColor(vc);
            g2.fill(new Ellipse2D.Double(px[i]-10, py[i]-10, 20, 20));

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.5f));
            g2.fill(new Ellipse2D.Double(px[i]-5, py[i]-5, 10, 10));

            // core point
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(px[i]-2.2, py[i]-2.2, 4.4, 4.4));
            g2.setColor(vc);
            g2.fill(new Ellipse2D.Double(px[i]-1.4, py[i]-1.4, 2.8, 2.8));
        }

        // reset composite
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // vignette + hint text
        g2.setColor(new Color(255,255,255,30));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.drawString("drag = scrub  |  click = pause", 12, H - 14);
    }

    static double smoothstep(double e0, double e1, double x) {
        double t = clamp((x - e0) / (e1 - e0), 0, 1);
        return t * t * (3 - 2 * t);
    }
    static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Hypercube 4D — Lowingx / Iori  •  muse spark v1.2");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new Hypercube());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
