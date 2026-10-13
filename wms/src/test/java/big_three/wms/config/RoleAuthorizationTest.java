package big_three.wms.config;

import big_three.wms.security.LoginAttemptService;
import big_three.wms.service.LocationService;
import big_three.wms.service.PhysicalMovementService;
import big_three.wms.service.PickOrderService;
import big_three.wms.service.ProductService;
import big_three.wms.service.BarcodeService;
import big_three.wms.service.ProveedorService;
import big_three.wms.service.PurchaseOrderService;
import big_three.wms.service.SupplierRatingService;
import big_three.wms.service.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@WebMvcTest
@Import(SecurityConfig.class)
class RoleAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private LoginAttemptService loginAttemptService;
    @MockitoBean private LocationService locationService;
    @MockitoBean private PhysicalMovementService physicalMovementService;
    @MockitoBean private PickOrderService pickOrderService;
    @MockitoBean private ProductService productService;
    @MockitoBean private BarcodeService barcodeService;
    @MockitoBean private ProveedorService proveedorService;
    @MockitoBean private PurchaseOrderService purchaseOrderService;
    @MockitoBean private SupplierRatingService supplierRatingService;
    @MockitoBean private UserDetailsService userDetailsService;

    @ParameterizedTest(name = "{0} {1} como {2} -> permitido={3}")
    @CsvSource({
            "GET,/api/usuarios,ADMINISTRADOR,true",
            "GET,/api/usuarios,OPERARIO,false",
            "GET,/api/usuarios,ANONIMO,false",
            "POST,/api/usuarios,ADMINISTRADOR,true",
            "POST,/api/usuarios,OPERARIO,false",
            "DELETE,/api/usuarios/1,OPERARIO,false",
            "GET,/api/proveedores,OPERARIO,true",
            "GET,/api/proveedores/1,OPERARIO,true",
            "POST,/api/proveedores,OPERARIO,false",
            "PUT,/api/proveedores/1,OPERARIO,false",
            "DELETE,/api/proveedores/1,OPERARIO,false",
            "POST,/api/proveedores,ADMINISTRADOR,true",
            "DELETE,/api/proveedores/1,ADMINISTRADOR,true",
            "GET,/api/proveedores,ANONIMO,false",
            "GET,/api/productos,OPERARIO,true",
            "GET,/api/ordenes-retiro,OPERARIO,true",
            "GET,/api/ordenes-compra,OPERARIO,true",
            "GET,/api/ubicaciones,OPERARIO,true",
            "POST,/api/ubicaciones,OPERARIO,true",
            "DELETE,/api/ubicaciones/1,OPERARIO,true",
            "GET,/api/valoraciones-proveedor,OPERARIO,true",
            "GET,/api/movimientos-fisicos,OPERARIO,true",
            "GET,/api/productos,ADMINISTRADOR,true",
            "GET,/api/productos,ANONIMO,false",
            "GET,/api/ubicaciones,ANONIMO,false",
            "GET,/api/ruta-inexistente,ANONIMO,false",
            "POST,/api/auth/login,ANONIMO,true"
    })
    void authorizationMatrix(String method, String path, String role, boolean allowed) throws Exception {
        MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(method), path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");

        if (!role.equals("ANONIMO")) {
            request = request.with(user("20-12345678-9").roles(role));
        }
        if (!method.equals("GET")) {
            request = request.with(csrf());
        }

        int status = mockMvc.perform(request).andReturn().getResponse().getStatus();

        if (allowed) {
            assertNotEquals(403, status);
        } else {
            assertEquals(403, status);
        }
    }
}
