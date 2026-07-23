package com.swimteam.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageProcessingServiceTest {

    private ImageProcessingService service;

    @BeforeEach
    void setUp() {
        service = new ImageProcessingService(800, 0.75f);
    }

    @Test
    void resizesAndConvertsLargePngToJpeg() throws Exception {
        byte[] png = createPng(1600, 1200, new Color(20, 140, 180));
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", png);

        ImageProcessingService.ProcessedImage processed = service.processUpload(file);

        assertEquals("image/jpeg", processed.contentType());
        assertTrue(processed.data().length > 0);

        BufferedImage out = ImageIO.read(new ByteArrayInputStream(processed.data()));
        assertTrue(out.getWidth() <= 800);
        assertTrue(out.getHeight() <= 800);
    }

    private static byte[] createPng(int width, int height, Color color) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(color);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
