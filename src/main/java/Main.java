package main.java;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class Main {
    public static void main(final String[] args) throws Exception{
        final Args a = new Args(args);
        if(a.contains("--help") || a.contains("-h")) {
            System.out.println("usage:\n\t--path <path to save bg*.png(s)>\n\t--distro <neofetch --ascii_distro>\n\t--font <path to .ttf (monospace) font file>");
            return;
        }
        final Path path = Path.of(new Optionull<>(a.get("--path")).or("."));
        final GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        final String neofetch = exec("neofetch -L --ascii_distro " + new Optionull<>(a.get("--distro")).or("auto"))
                .replaceAll("\\u001B\\[[\\d;]+[mAD]", "").replaceAll("\\u001B\\[\\?\\d+[lh]", ""); //works
        final StringBuilder feh = new StringBuilder("feh");
        final String fontPath = a.get("--font");
        final InputStream stream = fontPath == null ? ClassLoader.getSystemClassLoader().getResourceAsStream("main/resources/JetBrainsMono-Regular.ttf") : new FileInputStream(fontPath);
        assert stream != null;
        final Font font = Font.createFont(0, stream).deriveFont(16.0f);
        for(int i = 0; i < devices.length; ++i){
            final BufferedImage image = forMonitor(devices[i], neofetch, font);
            final File f = new File(path + "/bg" + i + ".png");
            ImageIO.write(image, "png", f);
            feh.append(" --bg-scale ").append(f.getAbsolutePath());
        }
        exec(feh.toString());
    }

    private static BufferedImage forMonitor(final GraphicsDevice device, final String neofetch, final Font font){
        final BufferedImage image = new BufferedImage(device.getDisplayMode().getWidth(), device.getDisplayMode().getHeight(), 6);
        final Graphics2D ctx = image.createGraphics();
        ctx.setFont(font);
        final int w = image.getWidth(), h = image.getHeight();
        {
            Color bg = new Color(0x202020);
            final int one = Math.min(w, h) / 20;
            for(int j = 0; j < 10; j++){
                ctx.setColor(bg);
                bg = new Color(bg.getRed() - 0x02, bg.getGreen() - 0x02, bg.getBlue() - 0x02, bg.getAlpha());
                ctx.fillRect(j * one, j * one, w - j * one * 2, h - j * one * 2);
            }
        }
        {
            final Random r = new Random();
            for(int x = 0; x < w; x += 2){
                for(int y = 0; y < h; y += 2){
                    final int distance = (w / 2 - x) * (w / 2 - x) + (h / 2 - y) * (h / 2 - y);
                    final float a = distance / (float) (w * w + h * h);
                    ctx.setColor(new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), Math.max((0.05f - a) * 3, 0)));
                    if(r.nextFloat() < a * 48.0f) ctx.fillRect(x, y, 2, 2);
                }
            }
        }
        {
            final String[] nf = neofetch.split("\n");
            int width = 0;
            for(final String s : nf)
                width = Math.max(width, s.length());

            for(int i = 0; i < nf.length; i++){
                final String line = nf[i];
                for(int x = 0; x < line.length(); ++x){
                    ctx.setColor(new Color((int) (Math.random() * 256.0), (int) (Math.random() * 256.0), (int) (Math.random() * 256.0)));
                    i++;
                    ctx.drawString(line.charAt(x) + "", w / 2f + ((-width / 2f + x) * ctx.getFontMetrics(font).charWidth(('0'))), h / 2f + ((-nf.length / 2f + i) * ctx.getFontMetrics(font).getHeight()));
                    i--;
                }
            }
        }
        return image;
    }

    private static String exec(final String cmd) throws Exception{
        final File tempScript = File.createTempFile("neofetch-output", null);
        try {
            final ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            pb.redirectOutput(ProcessBuilder.Redirect.to(tempScript));
            final Process process = pb.start();
            process.waitFor();
            return Files.readString(Path.of(tempScript.getPath()));
        } finally {
            boolean ignored = tempScript.delete();
        }
    }
}
