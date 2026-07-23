package com.swimteam.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resizes and converts uploaded images to compressed JPEG to keep storage small.
 */
@Service
public class ImageProcessingService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");

    private final int maxDimension;
    private final float jpegQuality;

    public ImageProcessingService(
            @Value("${app.photo.max-dimension:800}") int maxDimension,
            @Value("${app.photo.jpeg-quality:0.75}") float jpegQuality) {
        this.maxDimension = maxDimension;
        this.jpegQuality = Math.min(1.0f, Math.max(0.1f, jpegQuality));
    }

    public ProcessedImage processUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo file is required");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)
                && !looksLikeImageFilename(file.getOriginalFilename())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only JPEG, PNG, GIF, or WebP images are allowed");
        }

        try (InputStream in = file.getInputStream()) {
            return processImageStream(in);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to process image upload");
        }
    }

    public ProcessedImage processImageStream(InputStream in) {
        try {
            BufferedImage source = ImageIO.read(in);
            if (source == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read image file");
            }

            BufferedImage resized = resize(source, maxDimension);
            BufferedImage rgb = toRgb(resized);
            byte[] jpegBytes = encodeJpeg(rgb, jpegQuality);
            return new ProcessedImage(jpegBytes, "image/jpeg");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to process image upload");
        }
    }

    private static boolean looksLikeImageFilename(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".gif")
                || lower.endsWith(".webp");
    }

    private static BufferedImage resize(BufferedImage source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= maxDimension && height <= maxDimension) {
            return source;
        }

        double scale = Math.min((double) maxDimension / width, (double) maxDimension / height);
        int targetW = Math.max(1, (int) Math.round(width * scale));
        int targetH = Math.max(1, (int) Math.round(height * scale));

        BufferedImage target = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, targetW, targetH, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.drawImage(source, 0, 0, java.awt.Color.WHITE, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private static byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            // Fallback if JPEG writer is unavailable
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    public record ProcessedImage(byte[] data, String contentType) {}
}
