package com.paint.service;

import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FloodFillServiceTest {
    @Test
    void fillsOnlyTheContiguousRegionAndRespectsBoundaries() {
        WritableImage image = new WritableImage(5, 3);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 5; x++) {
                image.getPixelWriter().setColor(x, y, Color.WHITE);
            }
        }
        image.getPixelWriter().setColor(2, 0, Color.BLACK);
        image.getPixelWriter().setColor(2, 1, Color.BLACK);
        image.getPixelWriter().setColor(2, 2, Color.BLACK);

        assertTrue(FloodFillService.fill(image, 0, 1, Color.RED));
        assertEquals(Color.RED, image.getPixelReader().getColor(1, 1));
        assertEquals(Color.BLACK, image.getPixelReader().getColor(2, 1));
        assertEquals(Color.WHITE, image.getPixelReader().getColor(4, 1));
    }

    @Test
    void handlesReplacementColorsWithinToleranceWithoutRevisitingPixels() {
        WritableImage image = new WritableImage(3, 3);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                image.getPixelWriter().setColor(x, y, Color.WHITE);
            }
        }

        assertTrue(FloodFillService.fill(image, 1, 1, Color.rgb(254, 254, 254)));
        assertEquals(Color.rgb(254, 254, 254), image.getPixelReader().getColor(0, 0));
        assertFalse(FloodFillService.fill(image, 1, 1, Color.rgb(254, 254, 254)));
    }
}
