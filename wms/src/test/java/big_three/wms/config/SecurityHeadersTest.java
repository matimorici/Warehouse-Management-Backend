package big_three.wms.config;

import big_three.wms.controller.ProductController;
import big_three.wms.service.BarcodeService;
import big_three.wms.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMINISTRADOR")
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private BarcodeService barcodeService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void response_hasNosniff() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void response_deniesFraming() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void response_disablesCaching() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"));
    }

    @Test
    void secureRequest_hasHsts() throws Exception {
        mockMvc.perform(get("/api/productos").secure(true))
                .andExpect(header().exists("Strict-Transport-Security"));
    }

    @Test
    void insecureRequest_hasNoHsts() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }
}