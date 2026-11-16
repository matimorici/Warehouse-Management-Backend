package big_three.wms.config;

import big_three.wms.controller.ProveedorController;
import big_three.wms.service.ProveedorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest(ProveedorController.class)
@Import(SecurityConfig.class)
class AccessDeniedLoggingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProveedorService proveedorService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "20-12345678-9", roles = "OPERARIO")
    void operario_deletingProveedor_isDeniedAndLogged(CapturedOutput output) throws Exception {
        mockMvc.perform(delete("/api/proveedores/1").with(csrf()))
                .andExpect(status().isForbidden());

        assertTrue(output.getAll().contains("Acceso denegado: usuario=20-12345678-9 metodo=DELETE ruta=/api/proveedores/1"));
    }

    @Test
    @WithMockUser(username = "20-12345678-9", roles = "ADMINISTRADOR")
    void missingCsrfToken_isDeniedAndLogged(CapturedOutput output) throws Exception {
        mockMvc.perform(delete("/api/proveedores/1"))
                .andExpect(status().isForbidden());

        assertTrue(output.getAll().contains("Acceso denegado: usuario=20-12345678-9"));
    }
}
