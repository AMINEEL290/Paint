package com.paint.service;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesAndOpensPngWithoutChangingPixels() throws Exception {
        WritableImage original = new WritableImage(3, 2);
        original.getPixelWriter().setColor(1, 1, Color.DODGERBLUE);
        File file = temporaryDirectory.resolve("drawing.png").toFile();

        FileService.save(original, file, "png");
        Image restored = FileService.open(file);

        assertEquals(3, restored.getWidth());
        assertEquals(2, restored.getHeight());
        assertEquals(Color.DODGERBLUE, restored.getPixelReader().getColor(1, 1));
    }

    @Test
    void exportsJpegAsAnOpaqueImage() throws Exception {
        WritableImage original = new WritableImage(2, 2);
        original.getPixelWriter().setColor(0, 0, Color.TRANSPARENT);
        File file = temporaryDirectory.resolve("drawing.jpg").toFile();

        FileService.save(original, file, "jpeg");
        Image restored = FileService.open(file);

        assertEquals(2, restored.getWidth());
        assertEquals(1.0, restored.getPixelReader().getColor(0, 0).getOpacity());
    }
}
