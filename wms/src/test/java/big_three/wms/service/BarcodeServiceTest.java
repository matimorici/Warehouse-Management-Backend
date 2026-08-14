package big_three.wms.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

class BarcodeServiceTest {

    private final BarcodeService barcodeService = new BarcodeService();

    @Test
    void generatePng_returnsValidPngBytes() {
        byte[] png = barcodeService.generatePng("INT-000001", 300, 100);

        assertNotNull(png);
        assertTrue(png.length > 0);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 0x50, png[1]);
        assertEquals((byte) 0x4E, png[2]);
        assertEquals((byte) 0x47, png[3]);
    }

    @Test
    void generatePng_decodeRoundTrip_matchesContent() throws Exception {
        String contenido = "INT-000001";
        byte[] png = barcodeService.generatePng(contenido, 300, 100);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        String decoded = new MultiFormatReader().decode(bitmap).getText();

        assertEquals(contenido, decoded);
    }

    @Test
    void generatePng_emptyContent_throws() {
        assertThrows(RuntimeException.class, () -> barcodeService.generatePng("", 300, 100));
    }
}
