package big_three.wms.config;

import big_three.wms.controller.ProductController;
import big_three.wms.service.BarcodeService;
import big_three.wms.service.ProductService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMINISTRADOR")
class CsrfFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private BarcodeService barcodeService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void firstGet_issuesXsrfCookie() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void delete_withCookieValueInHeader_isAccepted() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/productos")).andReturn();
        Cookie xsrf = first.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(delete("/api/productos/1")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_withoutCsrfToken_isRejected() throws Exception {
        mockMvc.perform(delete("/api/productos/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withWrongCsrfHeader_isRejected() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/productos")).andReturn();
        Cookie xsrf = first.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(delete("/api/productos/1")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", "token-falso"))
                .andExpect(status().isForbidden());
    }
}
