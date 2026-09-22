package big_three.wms.service;

import big_three.wms.model.Product;
import big_three.wms.repository.ProductRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class BarcodeService {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 120;

    private final ProductRepository productRepository;

    public BarcodeService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public byte[] getBarcodePng(Long idProducto) {
        Product product = productRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(product.getCodigoBarras(), BarcodeFormat.CODE_128, WIDTH, HEIGHT);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(
                    matrix, new MatrixToImageConfig(0xFF000000, 0xFFFFFFFF));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("No se pudo generar el código de barras para el producto " + idProducto, e);
        }
    }
}