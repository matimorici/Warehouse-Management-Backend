package big_three.wms.controller;

import lombok.Data;

@Data
public class CrearUserRequest {
    private String nombre;
    private String apellido;
    private String cuil;
    private String rol;
    private String contrasena;
}
