package pcb.modelo;

import java.time.LocalDateTime;

/**
 * Entrada imutável do histórico de auditoria da planta.
 *
 * @param momento instante em que o evento ocorreu
 * @param categoria tipo do evento (por exemplo PRODUCAO, COMPRA, ERRO)
 * @param descricao texto descritivo do evento
 * @author 267396-282583
 */
public record RegistroAuditoria(LocalDateTime momento, String categoria, String descricao) {
}
