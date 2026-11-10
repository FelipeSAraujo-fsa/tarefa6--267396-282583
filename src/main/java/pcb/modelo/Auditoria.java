package pcb.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Histórico de auditoria da planta: registra operações realizadas e também as recusadas por erro, permitindo rastrear o que aconteceu durante a execução
 *
 * @author 267396-282583
 */
public class Auditoria {

    private final List<RegistroAuditoria> registros = new ArrayList<>();

    /**
     * Cria um histórico de auditoria vazio
     */
    public Auditoria() {
    }

    /**
     * Adiciona um evento ao histórico, com a data/hora atual
     *
     * @param categoria tipo do evento
     * @param descricao descrição do evento
     */
    public void registrar(String categoria, String descricao) {
        registros.add(new RegistroAuditoria(LocalDateTime.now(), categoria, descricao));
    }

    /**
     * Retorna o histórico completo, do mais antigo ao mais recente
     *
     * @return lista imutável de registros
     */
    public List<RegistroAuditoria> listar() {
        return Collections.unmodifiableList(registros);
    }
}
