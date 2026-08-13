package big_three.wms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orden_retiro")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PickOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden_retiro")
    private Long idOrdenRetiro;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "id_usuario", nullable = false)
    // Decisión deliberada: se guarda como Long crudo y NO se mapea una relación @ManyToOne a User.
    // Motivo: la FK existe a nivel de BD (db/migration/V1__create_schema.sql), y mapearla en JPA obligaría a cargar el
    // usuario (lazy) al armar las respuestas — inviable con open-in-view=false y sin fetch explícito
    // en buildResponse/buildSummary — y tocaría PickOrderResponseDTO/PickOrderService de forma riesgosa.
    // Posible conflicto a futuro: sin integridad referencial a nivel JPA, un idUsuario inexistente pasa
    // el guardado; hoy lo valida PickOrderService contra UserRepository, pero la BD sigue siendo la única garantía.
    private Long idUsuario;
}
