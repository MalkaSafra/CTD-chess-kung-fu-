package com.kungfuchess.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Lightweight image-utility class using only standard JDK APIs. The only class in this project
 * that touches {@link Graphics2D} or {@link ImageIO} directly -- every other drawing operation
 * goes through this class.
 */
public class Img {

    private BufferedImage img;

    /* ----------- load & optional resize ----------- */
    public Img read(String path,
                    Dimension targetSize,
                    boolean keepAspect,
                    Object interpolation /*ignored*/) {

        try {
            img = ImageIO.read(new File(path));
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load image: " + path);
        }
        if (img == null) throw new IllegalArgumentException("Unsupported image: " + path);

        if (targetSize != null) {
            int tw = targetSize.width, th = targetSize.height;
            int w = img.getWidth(), h = img.getHeight();

            int nw, nh;
            if (keepAspect) {
                double s = Math.min(tw / (double) w, th / (double) h);
                nw = (int) Math.round(w * s);
                nh = (int) Math.round(h * s);
            } else { nw = tw; nh = th; }

            BufferedImage dst = new BufferedImage(
                    nw, nh,
                    img.getColorModel().hasAlpha()
                            ? BufferedImage.TYPE_INT_ARGB
                            : BufferedImage.TYPE_INT_RGB);

            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            img = dst;
        }
        return this;
    }

    public Img read(String path) { return read(path, null, false, null); }

    /* ----------- resize an already-loaded image (not from disk) ----------- */
    public Img scaledTo(int width, int height, boolean keepAspect) {
        if (img == null) {
            throw new IllegalStateException("Image not loaded.");
        }

        int nw, nh;
        if (keepAspect) {
            double s = Math.min(width / (double) img.getWidth(), height / (double) img.getHeight());
            nw = (int) Math.round(img.getWidth() * s);
            nh = (int) Math.round(img.getHeight() * s);
        } else {
            nw = width;
            nh = height;
        }
        nw = Math.max(1, nw);
        nh = Math.max(1, nh);

        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, nw, nh, null);
        g.dispose();

        Img result = new Img();
        result.img = dst;
        return result;
    }

    /* ----------- a blank canvas of a given size, filled with one background color ----------- */
    public static Img blank(int width, int height, Color background) {
        BufferedImage buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = buffer.createGraphics();
        graphics.setColor(background);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        Img result = new Img();
        result.img = buffer;
        return result;
    }

    /* ----------- draw this image onto another ----------- */
    public void drawOn(Img other, int x, int y) {
        if (img == null || other.img == null)
            throw new IllegalStateException("Both images must be loaded.");

        if (x + img.getWidth()  > other.img.getWidth()
         || y + img.getHeight() > other.img.getHeight())
            throw new IllegalArgumentException("Patch exceeds destination bounds.");

        Graphics2D g = other.img.createGraphics();
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(img, x, y, null);
        g.dispose();
    }

    /* ----------- annotate with text ----------- */
    public void putText(String txt, int x, int y, float fontSize,
                        Color color, int thickness /*unused in Java2D*/) {

        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(color);
        g.setFont(img.getGraphics().getFont().deriveFont(fontSize * 12));
        g.drawString(txt, x, y);
        g.dispose();
    }

    /* ----------- draw an unfilled rectangle outline ----------- */
    public void drawRect(int x, int y, int width, int height, Color color, int thickness) {
        if (img == null) {
            throw new IllegalStateException("Image not loaded.");
        }

        Graphics2D graphics = img.createGraphics();
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(thickness));
        graphics.drawRect(x, y, width, height);
        graphics.dispose();
    }

    /* ----------- measure how wide a string would render, for centering ----------- */
    public int textWidth(String text, float fontSize) {
        if (img == null) {
            throw new IllegalStateException("Image not loaded.");
        }

        Graphics2D graphics = img.createGraphics();
        Font font = graphics.getFont().deriveFont(fontSize * 12);
        int width = graphics.getFontMetrics(font).stringWidth(text);
        graphics.dispose();
        return width;
    }

    /* ----------- draw a filled (optionally translucent) rectangle ----------- */
    public void fillRect(int x, int y, int width, int height, Color color) {
        if (img == null) {
            throw new IllegalStateException("Image not loaded.");
        }

        Graphics2D graphics = img.createGraphics();
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setColor(color);
        graphics.fillRect(x, y, width, height);
        graphics.dispose();
    }

    /* ----------- independent copy of this image ----------- */
    public Img copy() {
        if (img == null) {
            throw new IllegalStateException("Image not loaded.");
        }

        BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(img, 0, 0, null);
        graphics.dispose();

        Img result = new Img();
        result.img = copy;
        return result;
    }

    /* ----------- display in a Swing window ----------- */
    public void show() {
        if (img == null) throw new IllegalStateException("Image not loaded.");

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Image");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new JLabel(new ImageIcon(img)));
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    /* ----------- access (optional) ----------- */
    public BufferedImage get() { return img; }
}
