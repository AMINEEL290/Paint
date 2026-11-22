package com.paint.service;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public final class FileService {
    private FileService() {
    }

    public static Image open(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Le format de cette image n'est pas pris en charge.");
        }
        return SwingFXUtils.toFXImage(image, null);
    }

    public static void save(WritableImage image, File file, String format) throws IOException {
        String normalizedFormat = format.toLowerCase(Locale.ROOT);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        if ("jpg".equals(normalizedFormat) || "jpeg".equals(normalizedFormat)) {
            BufferedImage opaque = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics = opaque.createGraphics();
            try {
                graphics.setColor(java.awt.Color.WHITE);
                graphics.fillRect(0, 0, opaque.getWidth(), opaque.getHeight());
                graphics.drawImage(bufferedImage, 0, 0, null);
            } finally {
                graphics.dispose();
            }
            bufferedImage = opaque;
            normalizedFormat = "jpg";
        }
        if (!ImageIO.write(bufferedImage, normalizedFormat, file)) {
            throw new IOException("Impossible d'exporter au format " + normalizedFormat + ".");
        }
    }
}
