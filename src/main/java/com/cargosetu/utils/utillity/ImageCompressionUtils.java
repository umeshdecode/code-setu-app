package com.cargosetu.utils.utillity;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Slf4j
public class ImageCompressionUtils {

    private static final long MAX_SIZE = 1024 * 1024; // 1 MB
    private static final long TARGET_MIN_SIZE = 500 * 1024; // 500 KB

    /**
     * Compresses the input image byte array if it exceeds MAX_SIZE.
     * Keeps reducing the scale and quality iteratively (up to 5 times)
     * until the size is under MAX_SIZE or as close to the target range (500KB - 1MB) as possible.
     *
     * @param imageBytes  Original image bytes
     * @param contentType MIME type of the image (e.g. image/jpeg, image/png)
     * @return Compressed image bytes (or original bytes if type is not supported/size is already fine)
     */
    public static byte[] compressImage(byte[] imageBytes, String contentType) {
        if (contentType == null || imageBytes == null) {
            return imageBytes;
        }

        String format;
        if (contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/jpg")) {
            format = "jpg";
        } else if (contentType.equalsIgnoreCase("image/png")) {
            format = "png";
        } else {
            // Not a supported format for compression (e.g., pdf, doc), return as-is
            return imageBytes;
        }

        if (imageBytes.length <= MAX_SIZE) {
            log.info("Image size ({} bytes) is already under 1MB. Skipping compression.", imageBytes.length);
            return imageBytes;
        }

        log.info("Starting compression for {} image of original size {} bytes", format, imageBytes.length);

        double scale = 0.9;
        double quality = 0.85;
        byte[] compressedBytes = imageBytes;
        int iterations = 0;

        try {
            while (compressedBytes.length > MAX_SIZE && iterations < 5) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(new ByteArrayInputStream(imageBytes))
                        .scale(scale);

                if (format.equals("jpg")) {
                    builder.outputQuality(quality)
                           .outputFormat("jpg");
                } else {
                    // PNG compression (lossless, so we rely on scaling down)
                    builder.outputFormat("png");
                }

                builder.toOutputStream(baos);
                byte[] tempBytes = baos.toByteArray();

                // If compression actually made it worse or didn't change much, or if we hit minimum acceptable size
                if (tempBytes.length >= compressedBytes.length && iterations > 0) {
                    log.info("Compression size didn't reduce further. Stopping iteration.");
                    break;
                }

                compressedBytes = tempBytes;
                log.info("Iteration {}: scale={}, quality={}, resulting size={} bytes",
                        iterations, scale, quality, compressedBytes.length);

                if (compressedBytes.length <= MAX_SIZE && compressedBytes.length >= TARGET_MIN_SIZE) {
                    log.info("Successfully compressed to target range (500KB - 1MB): {} bytes", compressedBytes.length);
                    break;
                }

                scale *= 0.8;
                quality *= 0.8;
                iterations++;
            }
        } catch (Exception e) {
            log.error("Failed to compress image, returning original bytes", e);
            return imageBytes;
        }

        return compressedBytes;
    }
}
