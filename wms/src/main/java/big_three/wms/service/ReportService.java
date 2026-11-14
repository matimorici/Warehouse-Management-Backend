package big_three.wms.service;

import big_three.wms.dto.report.ActividadOperarioDTO;
import big_three.wms.dto.report.BajaAuditoriaDTO;
import big_three.wms.dto.report.OrdenCompraResumenDTO;
import big_three.wms.dto.report.ProductoMasRetiradoDTO;
import big_three.wms.dto.report.ProductoSinRotacionDTO;
import big_three.wms.dto.report.ProductoUbicacionActualDTO;
import big_three.wms.dto.report.ProveedorRankingDTO;
import big_three.wms.dto.report.RetiroPorPeriodoDTO;
import big_three.wms.dto.report.StockBajoDTO;
import big_three.wms.dto.report.StockPorProveedorDTO;
import big_three.wms.dto.report.UbicacionOcupacionDTO;
import big_three.wms.model.Role;
import big_three.wms.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final int UMBRAL_POR_DEFECTO = 10;
    private static final int LIMITE_POR_DEFECTO = 10;
    private static final int LIMITE_MAXIMO = 100;
    private static final String AGRUPACION_POR_DEFECTO = "day";
    private static final int LIMITE_BAJAS_POR_DEFECTO = 100;
    private static final int LIMITE_BAJAS_MAXIMO = 500;
    private static final String TODAS_LAS_ENTIDADES = "TODAS";
    private static final Set<String> ENTIDADES_BAJA = Set.of(
            "USUARIO", "PRODUCTO", "PROVEEDOR", "UBICACION", "ORDEN_COMPRA", "ORDEN_RETIRO");

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public List<StockBajoDTO> findStockBajo(Integer umbral) {
        int umbralResuelto = umbral != null ? umbral : UMBRAL_POR_DEFECTO;
        if (umbralResuelto < 0) {
            throw new IllegalArgumentException("El umbral no puede ser negativo");
        }
        return reportRepository.findStockBajo(umbralResuelto);
    }

    public List<StockPorProveedorDTO> findStockPorProveedor() {
        return reportRepository.findStockPorProveedor();
    }

    public List<ProductoUbicacionActualDTO> findProductosUbicacionActual(Boolean soloSinUbicar) {
        boolean filtro = Boolean.TRUE.equals(soloSinUbicar);
        return reportRepository.findProductosUbicacionActual(filtro).stream()
                .map(this::convertirProductoUbicacion)
                .toList();
    }

    public List<UbicacionOcupacionDTO> findUbicacionesOcupacion() {
        return reportRepository.findUbicacionesOcupacion().stream()
                .map(this::convertirUbicacionOcupacion)
                .toList();
    }

    public List<ProductoMasRetiradoDTO> findProductosMasRetirados(LocalDate from, LocalDate to, Integer limit) {
        ReportPeriod periodo = ReportPeriod.of(from, to);
        int limiteResuelto = limit != null ? limit : LIMITE_POR_DEFECTO;
        if (limiteResuelto < 1 || limiteResuelto > LIMITE_MAXIMO) {
            throw new IllegalArgumentException("El límite debe estar entre 1 y 100");
        }
        return reportRepository.findProductosMasRetirados(
                        periodo.from(), periodo.toExclusive(), limiteResuelto)
                .stream()
                .map(this::convertirProductoMasRetirado)
                .toList();
    }

    public List<ProductoSinRotacionDTO> findProductosSinRotacion(LocalDate from, LocalDate to, Boolean soloConStock) {
        ReportPeriod periodo = ReportPeriod.of(from, to);
        boolean filtro = soloConStock == null || soloConStock;
        return reportRepository.findProductosSinRotacion(
                periodo.from(), periodo.toExclusive(), filtro);
    }

    public List<RetiroPorPeriodoDTO> findRetirosPorPeriodo(LocalDate from, LocalDate to, String agrupacion) {
        ReportPeriod periodo = ReportPeriod.of(from, to);
        String agrupacionNormalizada = normalizarAgrupacion(agrupacion);
        return reportRepository.findRetirosPorPeriodo(
                        periodo.from(), periodo.toExclusive(), agrupacionNormalizada)
                .stream()
                .map(this::convertirRetiroPorPeriodo)
                .toList();
    }

    public List<ActividadOperarioDTO> findActividadOperarios(LocalDate from, LocalDate to) {
        ReportPeriod periodo = ReportPeriod.of(from, to);

        List<ReportRepository.UsuarioActividadBaseProjection> usuarios =
                reportRepository.findUsuariosActividad();
        Map<Long, ReportRepository.RetirosUsuarioProjection> retiros =
                reportRepository.findRetirosPorUsuario(periodo.from(), periodo.toExclusive()).stream()
                        .collect(Collectors.toMap(
                                ReportRepository.RetirosUsuarioProjection::getIdUsuario,
                                Function.identity()));
        Map<Long, ReportRepository.MovimientosUsuarioProjection> movimientos =
                reportRepository.findMovimientosPorUsuario(periodo.from(), periodo.toExclusive()).stream()
                        .collect(Collectors.toMap(
                                ReportRepository.MovimientosUsuarioProjection::getIdUsuario,
                                Function.identity()));

        return usuarios.stream()
                .map(usuario -> convertirActividadOperario(
                        usuario,
                        retiros.get(usuario.getIdUsuario()),
                        movimientos.get(usuario.getIdUsuario())))
                .sorted(Comparator
                        .comparingLong((ActividadOperarioDTO dto) ->
                                dto.getCantidadRetiros() + dto.getCantidadMovimientos())
                        .reversed()
                        .thenComparing(ActividadOperarioDTO::getApellido,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(ActividadOperarioDTO::getNombre,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(ActividadOperarioDTO::getIdUsuario))
                .toList();
    }

    public List<OrdenCompraResumenDTO> findOrdenesCompraResumen(LocalDate from, LocalDate to) {
        ReportPeriod periodo = ReportPeriod.of(from, to);
        return reportRepository.findOrdenesCompraResumen(
                        periodo.from(), periodo.toExclusive())
                .stream()
                .map(this::convertirOrdenCompraResumen)
                .toList();
    }

    public List<ProveedorRankingDTO> findProveedoresRanking(LocalDate from, LocalDate to) {
        ReportPeriod periodo = ReportPeriod.of(from, to);

        Map<Long, ReportRepository.ValoracionesProveedorProjection> valoraciones =
                reportRepository.findValoracionesPorProveedor(
                                periodo.from(), periodo.toExclusive())
                        .stream()
                        .collect(Collectors.toMap(
                                ReportRepository.ValoracionesProveedorProjection::getIdProveedor,
                                Function.identity()));
        Map<Long, ReportRepository.OrdenesRecibidasProveedorProjection> ordenes =
                reportRepository.findOrdenesRecibidasPorProveedor(
                                periodo.from(), periodo.toExclusive())
                        .stream()
                        .collect(Collectors.toMap(
                                ReportRepository.OrdenesRecibidasProveedorProjection::getIdProveedor,
                                Function.identity()));

        return reportRepository.findProveedoresRankingBase(
                        periodo.from(), periodo.toExclusive())
                .stream()
                .map(proveedor -> convertirProveedorRanking(
                        proveedor,
                        valoraciones.get(proveedor.getIdProveedor()),
                        ordenes.get(proveedor.getIdProveedor())))
                .sorted(Comparator
                        .comparing(ProveedorRankingDTO::getPromedioDiasRecepcion,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Comparator.comparingLong(
                                (ProveedorRankingDTO dto) -> dto.getCantidadValoraciones()).reversed())
                        .thenComparing(ProveedorRankingDTO::getRazonSocial,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(ProveedorRankingDTO::getIdProveedor))
                .toList();
    }

    public List<BajaAuditoriaDTO> findBajasAuditoria(
            LocalDate from, LocalDate to, String entidad, Integer limit) {
        ReportPeriod periodo = ReportPeriod.of(from, to);
        String entidadResuelta = normalizarEntidad(entidad);
        int limiteResuelto = limit != null ? limit : LIMITE_BAJAS_POR_DEFECTO;
        if (limiteResuelto < 1 || limiteResuelto > LIMITE_BAJAS_MAXIMO) {
            throw new IllegalArgumentException("El límite debe estar entre 1 y 500");
        }
        return reportRepository.findBajasAuditoria(
                        periodo.from(), periodo.toExclusive(), entidadResuelta, limiteResuelto)
                .stream()
                .map(this::convertirBajaAuditoria)
                .toList();
    }

    private String normalizarEntidad(String entidad) {
        if (entidad == null) {
            return TODAS_LAS_ENTIDADES;
        }
        String entidadNormalizada = entidad.trim().toUpperCase(Locale.ROOT);
        if (!ENTIDADES_BAJA.contains(entidadNormalizada)) {
            throw new IllegalArgumentException(
                    "La entidad debe ser USUARIO, PRODUCTO, PROVEEDOR, UBICACION, ORDEN_COMPRA u ORDEN_RETIRO");
        }
        return entidadNormalizada;
    }

    private BajaAuditoriaDTO convertirBajaAuditoria(
            ReportRepository.BajaAuditoriaProjection fila) {
        return new BajaAuditoriaDTO(
                fila.getEntidad(),
                fila.getIdEntidad(),
                fila.getDescripcion(),
                fila.getFechaBaja(),
                fila.getIdUsuarioBaja(),
                fila.getUsuarioBaja());
    }

    private OrdenCompraResumenDTO convertirOrdenCompraResumen(
            ReportRepository.OrdenCompraResumenProjection fila) {
        return new OrdenCompraResumenDTO(
                fila.getIdProveedor(),
                fila.getRazonSocial(),
                Boolean.TRUE.equals(fila.getActivo()),
                fila.getOrdenesPendientes(),
                fila.getOrdenesRecibidas(),
                fila.getOrdenesCanceladas(),
                fila.getUnidadesPedidas(),
                fila.getUnidadesRecibidas());
    }

    private ProveedorRankingDTO convertirProveedorRanking(
            ReportRepository.ProveedorRankingBaseProjection proveedor,
            ReportRepository.ValoracionesProveedorProjection valoraciones,
            ReportRepository.OrdenesRecibidasProveedorProjection ordenes) {
        long cantidadValoraciones = valoraciones != null
                ? valoraciones.getCantidadValoraciones()
                : 0L;
        Double promedioTiempoEntrega = valoraciones != null
                ? valoraciones.getPromedioTiempoEntregaValorado()
                : null;
        long ordenesRecibidas = ordenes != null ? ordenes.getOrdenesRecibidas() : 0L;
        Double promedioDiasRecepcion = ordenes != null
                ? redondear2(ordenes.getPromedioDiasRecepcion())
                : null;
        long recibidasSinFecha = ordenes != null ? ordenes.getRecibidasSinFecha() : 0L;

        return new ProveedorRankingDTO(
                proveedor.getIdProveedor(),
                proveedor.getRazonSocial(),
                Boolean.TRUE.equals(proveedor.getActivo()),
                cantidadValoraciones,
                promedioTiempoEntrega,
                ordenesRecibidas,
                promedioDiasRecepcion,
                recibidasSinFecha);
    }

    private Double redondear2(Double valor) {
        if (valor == null) {
            return null;
        }
        return Math.round(valor * 100.0) / 100.0;
    }

    private String normalizarAgrupacion(String agrupacion) {
        if (agrupacion == null) {
            return AGRUPACION_POR_DEFECTO;
        }
        return switch (agrupacion.trim().toUpperCase(Locale.ROOT)) {
            case "DIA" -> "day";
            case "SEMANA" -> "week";
            case "MES" -> "month";
            default -> throw new IllegalArgumentException("La agrupación debe ser DIA, SEMANA o MES");
        };
    }

    private ProductoMasRetiradoDTO convertirProductoMasRetirado(
            ReportRepository.ProductoMasRetiradoProjection fila) {
        return new ProductoMasRetiradoDTO(
                fila.getIdProducto(),
                fila.getNombreProducto(),
                fila.getCodigoBarras(),
                fila.getActivo(),
                fila.getUnidadesRetiradas(),
                fila.getCantidadOrdenes());
    }

    private RetiroPorPeriodoDTO convertirRetiroPorPeriodo(
            ReportRepository.RetiroPorPeriodoProjection fila) {
        return new RetiroPorPeriodoDTO(
                fila.getPeriodo(),
                fila.getCantidadOrdenes(),
                fila.getUnidadesRetiradas());
    }

    private ActividadOperarioDTO convertirActividadOperario(
            ReportRepository.UsuarioActividadBaseProjection usuario,
            ReportRepository.RetirosUsuarioProjection retiros,
            ReportRepository.MovimientosUsuarioProjection movimientos) {
        long cantidadRetiros = retiros != null ? retiros.getCantidadRetiros() : 0L;
        long unidadesRetiradas = retiros != null ? retiros.getUnidadesRetiradas() : 0L;
        long cantidadMovimientos = movimientos != null ? movimientos.getCantidadMovimientos() : 0L;
        LocalDateTime ultimaActividad = ultimaFecha(
                retiros != null ? retiros.getUltimaActividad() : null,
                movimientos != null ? movimientos.getUltimaActividad() : null);

        return new ActividadOperarioDTO(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCuil(),
                Role.valueOf(usuario.getRol()),
                Boolean.TRUE.equals(usuario.getActivo()),
                cantidadRetiros,
                unidadesRetiradas,
                cantidadMovimientos,
                ultimaActividad);
    }

    private LocalDateTime ultimaFecha(LocalDateTime primera, LocalDateTime segunda) {
        if (primera == null) {
            return segunda;
        }
        if (segunda == null) {
            return primera;
        }
        return primera.isAfter(segunda) ? primera : segunda;
    }

    private ProductoUbicacionActualDTO convertirProductoUbicacion(
            ReportRepository.ProductoUbicacionActualProjection fila) {
        return new ProductoUbicacionActualDTO(
                fila.getIdProducto(),
                fila.getNombreProducto(),
                fila.getCodigoBarras(),
                fila.getIdUbicacion(),
                fila.getNombreUbicacion(),
                fila.getFechaUltimoMovimiento());
    }

    private UbicacionOcupacionDTO convertirUbicacionOcupacion(
            ReportRepository.UbicacionOcupacionProjection fila) {
        return new UbicacionOcupacionDTO(
                fila.getIdUbicacion(),
                fila.getNombreUbicacion(),
                fila.getCantidadProductos());
    }
}
