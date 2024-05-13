package main.java;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
public class Main
{
    public static void main(final String[] args) throws Exception {
        final Args a = new Args(args);
        if(a.contains("--help")||a.contains("-h")){
            System.out.println("usage:\n\t--path <path to save bgN.png(s)>\n\t--distro <neofetch distro>\n\t--font <path to .ttf (monospace) font file>");
            return;
        }
        final String path = a.get("--path") == null ? "." : a.get("--path");
        final GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        final String neofetch = exec("neofetch -L --ascii_distro " + (a.get("--distro") == null ? "auto" : a.get("--distro")) +" | sed 's/\\x1B[@A-Z\\\\\\]^_]\\|\\x1B\\[[0-9:;<=>?]*[-!\"#$%&'\"'\"'()*+,.\\/]*[][\\\\@A-Z^_`a-z{|}~]//g'\n"); //todo replace sed with builtin regex
        final StringBuilder feh = new StringBuilder("feh");
        final InputStream stream = a.get("--font") == null ? ClassLoader.getSystemClassLoader().getResourceAsStream("main/resources/JetBrainsMono-Regular.ttf") : new FileInputStream(a.get("--font"));
        assert stream != null;
        final Font font = Font.createFont(0, stream).deriveFont(16.0f);
        for (int i = 0; i < devices.length; ++i) {
            final BufferedImage image = forMonitor(devices[i], neofetch, font);
            final File f = new File(path+"/bg"+i+".png");
            ImageIO.write(image, "png", f);
            feh.append(" --bg-scale ").append(f.getAbsolutePath());
        }
        exec(feh.toString());
    }

    private static BufferedImage forMonitor(final GraphicsDevice device, final String neofetch, final Font font) {
        final BufferedImage image = new BufferedImage(device.getDisplayMode().getWidth(), device.getDisplayMode().getHeight(), 6);
        final Graphics2D ctx = image.createGraphics();
        final Color bg = new Color(0x202020);
        ctx.setColor(bg);
        ctx.setFont(font);
        int k = 0;
        while (!ctx.getColor().equals(Color.BLACK)) {
            ctx.setColor(darker(ctx.getColor(), 1));
            ++k;
        }
        final int l = Math.min(image.getWidth(), image.getHeight());
        final int one = l / k;
        for (int j = 0; j < k; ++j) {
            ctx.setColor(darker(bg, j));
            ctx.fillRect(j * one, j * one, image.getWidth() - j * one * 2, image.getHeight() - j * one * 2);
        }
        for (int x = 0; x < image.getWidth(); x += 2) {
            for (int y = 0; y < image.getHeight(); y += 2) {
                final int distance = (image.getWidth() / 2 - x) * (image.getWidth() / 2 - x) + (image.getHeight() / 2 - y) * (image.getHeight() / 2 - y);
                final float a = distance / (float)(image.getWidth() * image.getWidth() + image.getHeight() * image.getHeight());
                ctx.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), Math.max((0.05f - a) * 3.0f, 0.0f)));
                if (Math.random() < a * 48.0f) {
                    ctx.fillRect(x, y, 2, 2);
                }
            }
        }

        final String[] nf = neofetch.split("\n");
        int width = 0;
        final int length = nf.length - 2;
        for(int i = 2; i<nf.length; i++) { width = Math.max(width, nf[i].length()); }

        for(int i = 2; i<nf.length; i++){
            final String line = nf[i];
            for (int x = 0; x < line.length(); ++x) {
                ctx.setColor(new Color((int)(Math.random() * 256.0), (int)(Math.random() * 256.0), (int)(Math.random() * 256.0)));
                final int y = i-2;

                ctx.drawString(line.charAt(x)+"",
                        image.getWidth() / 2f + ((-width/2f + x) * ctx.getFontMetrics(font).charWidth(('0'))),
                        image.getHeight() / 2f + ((-length/2f + y) * ctx.getFontMetrics(font).getHeight())
                );
            }
        }

        return image;
    }

    private static Color darker(final Color c, final int i) {
        Color col = c;
        for (int j = 0; j < i; ++j) {
            col = new Color(Math.max((int)(col.getRed() * 0.92f), 0), Math.max((int)(col.getGreen() * 0.92f), 0), Math.max((int)(col.getBlue() * 0.92f), 0), col.getAlpha());
        }
        return col;
    }

    private static String exec(final String... cmd) throws Exception {
        final File tempScript = File.createTempFile("bggen_script", null);
        final Writer streamWriter = new OutputStreamWriter(new FileOutputStream(tempScript));
        final PrintWriter printWriter = new PrintWriter(streamWriter);
        printWriter.println("#!/bin/bash");
        for (final String s : cmd) {
            printWriter.println(s);
        }
        printWriter.close();
        try {
            final ProcessBuilder pb = new ProcessBuilder("bash", tempScript.toString());
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(tempScript));
            final Process process = pb.start();
            process.waitFor();
            return Files.readString(Path.of(tempScript.getPath()));
        } finally {
            boolean ignored = tempScript.delete();
        }
    }
}
