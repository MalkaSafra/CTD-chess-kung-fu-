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
            int targetWidth = targetSize.width, targetHeight = targetSize.height;
            int sourceWidth = img.getWidth(), sourceHeight = img.getHeight();

            int newWidth, newHeight;
            if (keepAspect) {
                double scale = Math.min(targetWidth / (double) sourceWidth, targetHeight / (double) sourceHeight);
                newWidth = (int) Math.round(sourceWidth * scale);
                newHeight = (int) Math.round(sourceHeight * scale);
            } else { newWidth = targetWidth; newHeight = targetHeight; }

            BufferedImage resized = new BufferedImage(
                    newWidth, newHeight,
                    img.getColorModel().hasAlpha()
                            ? BufferedImage.TYPE_INT_ARGB
                            : BufferedImage.TYPE_INT_RGB);

            Graphics2D graphics = resized.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(img, 0, 0, newWidth, newHeight, null);
            graphics.dispose();
            img = resized;
        }
        return this;
    }

    public Img read(String path) { return read(path, null, false, null); }

    /* ----------- resize an already-loaded image (not from disk) ----------- */
    public Img scaledTo(int width, int height, boolean keepAspect) {
        if (img == null) {
            throw new IllegalStateException("Image not loaded.");
        }

        int newWidth, newHeight;
        if (keepAspect) {
            double scale = Math.min(width / (double) img.getWidth(), height / (double) img.getHeight());
            newWidth = (int) Math.round(img.getWidth() * scale);
            newHeight = (int) Math.round(img.getHeight() * scale);
        } else {
            newWidth = width;
            newHeight = height;
        }
        newWidth = Math.max(1, newWidth);
        newHeight = Math.max(1, newHeight);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(img, 0, 0, newWidth, newHeight, null);
        graphics.dispose();

        Img result = new Img();
        result.img = resized;
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

        Graphics2D graphics = other.img.createGraphics();
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.drawImage(img, x, y, null);
        graphics.dispose();
    }

    /* ----------- annotate with text ----------- */
    public void putText(String text, int x, int y, float fontSize,
                        Color color, int thickness /*unused in Java2D*/) {

        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D graphics = img.createGraphics();
        applyTextRenderingHints(graphics);
        graphics.setColor(color);
        graphics.setFont(textFont(fontSize));
        graphics.drawString(text, x, y);
        graphics.dispose();
    }

    /**
     * A named font family (matching what the rest of the app's Swing components use), not the
     * unnamed logical "Dialog" font {@code Graphics2D} falls back to by default -- the default
     * renders noticeably softer at small sizes than a real font does.
     */
    private static Font textFont(float fontSize) {
        return new Font(Font.SANS_SERIF, Font.PLAIN, 1).deriveFont(fontSize * 12);
    }

    /**
     * Beyond plain antialiasing, asks for the same quality-over-speed text rendering Swing
     * components get by default (fractional glyph positions, exact rather than pixel-snapped
     * stroke placement) -- an offscreen {@link BufferedImage} doesn't get these applied
     * automatically the way an on-screen component does.
     */
    private static void applyTextRenderingHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
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
        applyTextRenderingHints(graphics);
        Font font = textFont(fontSize);
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
            JFrame frame = new JFrame("Image");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new JLabel(new ImageIcon(img)));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /* ----------- access (optional) ----------- */
    public BufferedImage get() { return img; }
}
