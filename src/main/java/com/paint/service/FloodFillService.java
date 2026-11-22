package com.paint.service;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public final class FloodFillService {
    private FloodFillService() {
    }

    public static boolean fill(WritableImage image, int x, int y, Color replacement) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IllegalArgumentException("Fill coordinates are outside the image.");
        }
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = image.getPixelWriter();
        int target = reader.getArgb(x, y);
        int replacementArgb = toArgb(replacement);
        if (target == replacementArgb) {
            return false;
        }
        int pixelCount = Math.multiplyExact(width, height);
        int[] queue = new int[pixelCount];
        boolean[] visited = new boolean[pixelCount];
        int startPosition = y * width + x;
        int head = 0;
        int tail = 0;
        queue[tail++] = startPosition;
        visited[startPosition] = true;
        writer.setArgb(x, y, replacementArgb);
        while (head < tail) {
            int position = queue[head++];
            int px = position % width;
            int py = position / width;
            if (px > 0) tail = enqueue(px - 1, py, width, target, replacementArgb, reader, writer, queue, visited, tail);
            if (px + 1 < width) tail = enqueue(px + 1, py, width, target, replacementArgb, reader, writer, queue, visited, tail);
            if (py > 0) tail = enqueue(px, py - 1, width, target, replacementArgb, reader, writer, queue, visited, tail);
            if (py + 1 < height) tail = enqueue(px, py + 1, width, target, replacementArgb, reader, writer, queue, visited, tail);
        }
        return true;
    }

    private static int enqueue(int x, int y, int width, int target, int replacement,
                               PixelReader reader, PixelWriter writer, int[] queue, boolean[] visited, int tail) {
        int position = y * width + x;
        if (visited[position]) {
            return tail;
        }
        int existing = reader.getArgb(x, y);
        if (closeColor(existing, target)) {
            visited[position] = true;
            writer.setArgb(x, y, replacement);
            queue[tail++] = position;
        }
        return tail;
    }

    private static boolean closeColor(int first, int second) {
        int redDifference = Math.abs(((first >>> 16) & 0xff) - ((second >>> 16) & 0xff));
        int greenDifference = Math.abs(((first >>> 8) & 0xff) - ((second >>> 8) & 0xff));
        int blueDifference = Math.abs((first & 0xff) - (second & 0xff));
        int alphaDifference = Math.abs(((first >>> 24) & 0xff) - ((second >>> 24) & 0xff));
        return redDifference <= 12 && greenDifference <= 12
                && blueDifference <= 12 && alphaDifference <= 12;
    }

    private static int toArgb(Color color) {
        return ((int) Math.round(color.getOpacity() * 255) << 24)
                | ((int) Math.round(color.getRed() * 255) << 16)
                | ((int) Math.round(color.getGreen() * 255) << 8)
                | (int) Math.round(color.getBlue() * 255);
    }
}
