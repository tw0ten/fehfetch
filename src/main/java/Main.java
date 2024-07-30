package main.java;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class Main {
	public static void main(final String[] args) throws Exception {
		final Args a = new Args(args);
		if (a.has("--help") || a.has("-h")) {
			System.out.println(
					"usage:\n\t--path <path to save bg*.png(s)>\n\t--distro <neofetch --ascii_distro>\n\t--font <path to .ttf (monospace) font file>");
			return;
		}
		new Main(a);
	}

	private final String neofetch;
	private final Font font;

	private Main(final Args args) throws Exception {
		final Path path = Path.of(new Optionull<>(args.get("--path")).or("."));
		final GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		this.neofetch = exec("neofetch -L --ascii_distro " + new Optionull<>(args.get("--distro")).or("auto"))
				.replaceAll("\\u001B\\[[\\d;]+[mAD]", "").replaceAll("\\u001B\\[\\?\\d+[lh]", "");
		{
			final String fontPath = args.get("--font");
			final InputStream stream = fontPath == null
					? ClassLoader.getSystemClassLoader().getResourceAsStream("main/resources/JetBrainsMono-Regular.ttf")
					: new FileInputStream(fontPath);
			this.font = Font.createFont(0, stream).deriveFont(16.0f);
		}
		final StringBuilder command = new StringBuilder("feh --no-fehbg");
		for (int i = 0; i < devices.length; i++) {
			final BufferedImage image = getImage(devices[i]);
			final File f = new File(path + "/bg" + i + ".png");
			ImageIO.write(image, "png", f);
			command.append(" --bg-scale ").append(f.getAbsolutePath());
		}
		exec(command.toString());
	}

	private BufferedImage getImage(final GraphicsDevice device) {
		final int w = device.getDisplayMode().getWidth(), h = device.getDisplayMode().getHeight();
		final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		final Graphics2D ctx = image.createGraphics();
		ctx.setFont(font);

		{
			final int col = 12;
			Color bg = new Color(0x202020 + 0x030303);
			final int one = Math.min(w, h) / 2 / col;
			for (int j = 0; j < col; j++) {
				bg = new Color(Math.max(bg.getRed() - 0x03, 0), Math.max(bg.getGreen() - 0x03, 0),
						Math.max(bg.getBlue() - 0x03, 0), bg.getAlpha());
				ctx.setColor(bg);
				ctx.fillRect(j * one, j * one, w - j * one * 2, h - j * one * 2);
			}
		}

		final Random r = new Random();

		for (int x = 0; x < w; x += 2) {
			for (int y = 0; y < h; y += 2) {
				final int distance = (w / 2 - x) * (w / 2 - x) + (h / 2 - y) * (h / 2 - y);
				final float a = distance / (float) (w * w + h * h);
				ctx.setColor(new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), Math.max((0.05f - a) * 3, 0)));
				if (r.nextFloat() < a * 48.0f)
					ctx.fillRect(x, y, 2, 2);
			}
		}

		{
			final String[] nf = neofetch.split("\n");
			int width = 0;
			for (final String s : nf)
				width = Math.max(width, s.length());
			final int fx = ctx.getFontMetrics(font).charWidth(' '), fy = ctx.getFontMetrics(font).getHeight();
			for (int i = 0; i < nf.length; i++) {
				final String line = nf[i];
				for (int x = 0; x < line.length(); x++) {
					ctx.setColor(new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256)));
					ctx.drawString(String.valueOf(line.charAt(x)), w / 2f + ((-width / 2f + x) * fx),
							h / 2f + ((-nf.length / 2f + i + 1) * fy));
				}
			}
		}

		return image;
	}

	private static String exec(final String cmd) throws Exception {
		final File tempFile = File.createTempFile("neofetch-output", "fehfetch");
		final ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
		pb.redirectOutput(ProcessBuilder.Redirect.to(tempFile));
		final Process process = pb.start();
		process.waitFor();
		final String o = Files.readString(Path.of(tempFile.getPath()));
		tempFile.delete();
		return o;
	}
}
