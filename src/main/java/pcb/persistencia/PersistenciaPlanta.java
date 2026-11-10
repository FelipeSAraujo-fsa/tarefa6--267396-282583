package pcb.persistencia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import pcb.excecao.ArquivoInexistenteException;
import pcb.excecao.DadosInvalidosException;
import pcb.excecao.PersistenciaException;
import pcb.excecao.PlantaException;
import pcb.modelo.CatalogoProdutos;
import pcb.modelo.Cenario;
import pcb.modelo.Demanda;
import pcb.modelo.Estoque;
import pcb.modelo.LinhaDeProducao;
import pcb.modelo.Maquina;
import pcb.modelo.MateriaPrima;
import pcb.modelo.Produto;
import pcb.modelo.StatusDemanda;
import pcb.modelo.TipoMaquina;
import pcb.modelo.Orcamento;
import pcb.servico.GerenciadorProducao;

public class PersistenciaPlanta {

    public static final String ARQUIVO_PADRAO = "dados/planta.xml";

    private final Path arquivo;
    private List<String> texto;
    private int pos;

    public PersistenciaPlanta() {
        this(ARQUIVO_PADRAO);
    }

    public PersistenciaPlanta(String caminho) {
        this.arquivo = Path.of(caminho);
    }

    public Path getArquivo() {
        return arquivo;
    }

    public boolean existe() {
        return Files.isRegularFile(arquivo);
    }

    public void salvar(GerenciadorProducao planta) {
        List<String> xml = new ArrayList<>();
        xml.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.add(abre("planta", "versao", "1"));
        adicionar(xml, 1, vazio("orcamento", "saldo", String.valueOf(planta.getSaldo())));
        adicionar(xml, 1, vazio("configuracao", "cenario", planta.getCenario().name(),
                "estrategia", planta.getNomeEstrategia()));
        adicionar(xml, 1, "<estoque>");
        for (MateriaPrima mp : MateriaPrima.values()) {
            adicionar(xml, 2, vazio("item", "materiaPrima", mp.name(),
                    "quantidade", String.valueOf(planta.consultarEstoque(mp))));
        }
        adicionar(xml, 1, "</estoque>");
        adicionar(xml, 1, "<linhas>");
        for (LinhaDeProducao linha : planta.getLinhas()) {
            adicionar(xml, 2, abre("linha", "nome", linha.getNome()));
            for (Maquina m : linha.getMaquinas()) {
                gravarMaquina(xml, m);
            }
            adicionar(xml, 2, "</linha>");
        }
        adicionar(xml, 1, "</linhas>");
        adicionar(xml, 1, "<demandas>");
        for (Demanda d : planta.listarDemandas()) {
            adicionar(xml, 2, vazio("demanda", "id", String.valueOf(d.getId()),
                    "produto", d.getNomeProduto(), "quantidade", String.valueOf(d.getQuantidade()),
                    "prazoDias", String.valueOf(d.getPrazoDias()), "status", d.getStatus().name()));
        }
        adicionar(xml, 1, "</demandas>");
        xml.add("</planta>");
        try {
            Files.createDirectories(arquivo.toAbsolutePath().getParent());
            Files.write(arquivo, xml);
        } catch (IOException e) {
            throw new PersistenciaException(
                    "Não foi possível gravar o arquivo " + arquivo + ": " + e.getMessage(), e);
        }
    }

    public GerenciadorProducao carregar() {
        if (!Files.exists(arquivo)) {
            throw new ArquivoInexistenteException("O arquivo " + arquivo + " não existe.");
        }
        try {
            texto = Files.readAllLines(arquivo);
        } catch (IOException e) {
            throw new PersistenciaException(
                    "Não foi possível ler o arquivo " + arquivo + ": " + e.getMessage(), e);
        }
        if (String.join("", texto).isBlank()) {
            throw new DadosInvalidosException("O arquivo " + arquivo + " está vazio.");
        }
        pos = 0;
        try {
            return reconstruir();
        } catch (PersistenciaException e) {
            throw e;
        } catch (IllegalArgumentException | PlantaException e) {
            throw new DadosInvalidosException(
                    "Linha " + pos + ": dados incompatíveis com a planta (" + e.getMessage() + ")", e);
        }
    }

    private static void gravarMaquina(List<String> xml, Maquina m) {
        adicionar(xml, 3, abre("maquina", "nome", m.getNome(), "tipo", m.getTipo().name(),
                "capacidadePorHora", String.valueOf(m.getCapacidadePorHora()),
                "custoPorHora", String.valueOf(m.getCustoPorHora()),
                "temperaturaOperacaoC", String.valueOf(m.getTemperaturaOperacaoC()),
                "habilitada", String.valueOf(m.isHabilitada()),
                "horasAcumuladas", String.valueOf(m.getHorasAcumuladas())));
        List<Double> desgastes = m.getDesgastes();
        adicionar(xml, 4, vazio("cabecote", "desgaste", String.valueOf(desgastes.get(0))));
        adicionar(xml, 4, vazio("sensor", "descalibracao", String.valueOf(desgastes.get(1))));
        for (int i = 2; i < desgastes.size(); i++) {
            adicionar(xml, 4, vazio("atuador", "numero", String.valueOf(i - 1),
                    "desgaste", String.valueOf(desgastes.get(i))));
        }
        adicionar(xml, 3, "</maquina>");
    }

    private GerenciadorProducao reconstruir() {
        if (!ler().startsWith("<?xml")) {
            throw invalido("o arquivo deve começar com a declaração XML.");
        }
        String raiz = ler();
        if (!raiz.startsWith("<planta ") || !atributo(raiz, "versao").equals("1")) {
            throw invalido("elemento <planta versao=\"1\"> não encontrado.");
        }
        Orcamento orcamento = new Orcamento(decimal(elemento("orcamento"), "saldo"));
        String configuracao = elemento("configuracao");
        Cenario cenario = Cenario.valueOf(atributo(configuracao, "cenario"));
        String estrategia = atributo(configuracao, "estrategia");
        Estoque estoque = lerEstoque();
        List<LinhaDeProducao> linhas = lerLinhas();

        GerenciadorProducao planta = new GerenciadorProducao(estoque, orcamento, linhas,
                CatalogoProdutos.padrao());
        planta.setCenario(cenario);
        selecionarEstrategia(planta, estrategia);
        lerDemandas(planta);
        esperar("</planta>");
        return planta;
    }

    private Estoque lerEstoque() {
        Estoque estoque = new Estoque(Map.of());
        boolean[] vistos = new boolean[MateriaPrima.values().length];
        esperar("<estoque>");
        while (!espiar().equals("</estoque>")) {
            String item = elemento("item");
            MateriaPrima mp = MateriaPrima.valueOf(atributo(item, "materiaPrima"));
            int quantidade = inteiro(item, "quantidade");
            if (vistos[mp.ordinal()] || quantidade < 0) {
                throw invalido("item de estoque repetido ou com quantidade negativa: " + mp.getNome() + ".");
            }
            vistos[mp.ordinal()] = true;
            if (quantidade > 0) {
                estoque.adicionar(mp, quantidade);
            }
        }
        ler();
        for (boolean visto : vistos) {
            if (!visto) {
                throw invalido("o estoque deve informar todas as matérias-primas.");
            }
        }
        return estoque;
    }

    private List<LinhaDeProducao> lerLinhas() {
        List<LinhaDeProducao> linhas = new ArrayList<>();
        esperar("<linhas>");
        while (!espiar().equals("</linhas>")) {
            LinhaDeProducao linha = new LinhaDeProducao(atributo(elemento("linha"), "nome"));
            for (LinhaDeProducao outra : linhas) {
                if (outra.getNome().equalsIgnoreCase(linha.getNome())) {
                    throw invalido("linha de produção repetida: " + linha.getNome() + ".");
                }
            }
            while (!espiar().equals("</linha>")) {
                linha.adicionarMaquina(lerMaquina());
            }
            ler();
            linhas.add(linha);
        }
        ler();
        return linhas;
    }

    private Maquina lerMaquina() {
        String m = elemento("maquina");
        List<Double> desgastes = new ArrayList<>();
        desgastes.add(decimal(elemento("cabecote"), "desgaste"));
        desgastes.add(decimal(elemento("sensor"), "descalibracao"));
        while (espiar().startsWith("<atuador ")) {
            desgastes.add(decimal(elemento("atuador"), "desgaste"));
        }
        esperar("</maquina>");
        Maquina maquina = new Maquina(atributo(m, "nome"), TipoMaquina.valueOf(atributo(m, "tipo")),
                inteiro(m, "capacidadePorHora"), decimal(m, "custoPorHora"),
                decimal(m, "temperaturaOperacaoC"), desgastes.size() - 2);
        maquina.restaurarEstado(booleano(m, "habilitada"), decimal(m, "horasAcumuladas"), desgastes);
        return maquina;
    }

    private void lerDemandas(GerenciadorProducao planta) {
        int esperado = 1;
        esperar("<demandas>");
        while (!espiar().equals("</demandas>")) {
            String d = elemento("demanda");
            if (inteiro(d, "id") != esperado++) {
                throw invalido("os identificadores das demandas devem ser sequenciais a partir de 1.");
            }
            Produto produto = buscarProduto(planta, atributo(d, "produto"));
            StatusDemanda status = StatusDemanda.valueOf(atributo(d, "status"));
            Demanda demanda = planta.criarDemanda(produto, inteiro(d, "quantidade"), inteiro(d, "prazoDias"));
            if (status == StatusDemanda.CONCLUIDA) {
                demanda.concluir();
            } else if (status == StatusDemanda.CANCELADA) {
                demanda.cancelar();
            }
        }
        ler();
    }

    private Produto buscarProduto(GerenciadorProducao planta, String nome) {
        for (Produto p : planta.getProdutos()) {
            if (p.getNome().equals(nome)) {
                return p;
            }
        }
        throw invalido("produto desconhecido: " + nome + ".");
    }

    private void selecionarEstrategia(GerenciadorProducao planta, String nome) {
        for (int i = 0; i < planta.getEstrategiasDisponiveis().size(); i++) {
            if (planta.getEstrategiasDisponiveis().get(i).getNome().equals(nome)) {
                planta.selecionarEstrategia(i + 1);
                return;
            }
        }
        throw invalido("estratégia de produção desconhecida: " + nome + ".");
    }

    private String ler() {
        while (pos < texto.size()) {
            String linha = texto.get(pos++).trim();
            if (!linha.isEmpty()) {
                return linha;
            }
        }
        throw new DadosInvalidosException("Arquivo incompleto: fim inesperado após a linha " + pos + ".");
    }

    private String espiar() {
        int anterior = pos;
        String linha = ler();
        pos = anterior;
        return linha;
    }

    private void esperar(String tag) {
        String linha = ler();
        if (!linha.equals(tag)) {
            throw invalido("esperado " + tag + " mas encontrado: " + linha);
        }
    }

    private String elemento(String nome) {
        String linha = ler();
        if (!linha.startsWith("<" + nome + " ")) {
            throw invalido("esperado <" + nome + "> mas encontrado: " + linha);
        }
        return linha;
    }

    private DadosInvalidosException invalido(String mensagem) {
        return new DadosInvalidosException("Linha " + pos + ": " + mensagem);
    }

    private String atributo(String linha, String nome) {
        int inicio = linha.indexOf(" " + nome + "=\"");
        int fim = inicio < 0 ? -1 : linha.indexOf('"', inicio + nome.length() + 3);
        if (fim < 0) {
            throw invalido("atributo \"" + nome + "\" ausente em: " + linha);
        }
        return desescapar(linha.substring(inicio + nome.length() + 3, fim));
    }

    private int inteiro(String linha, String nome) {
        return Integer.parseInt(atributo(linha, nome));
    }

    private double decimal(String linha, String nome) {
        double valor = Double.parseDouble(atributo(linha, nome));
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw invalido("valor numérico inválido em \"" + nome + "\".");
        }
        return valor;
    }

    private boolean booleano(String linha, String nome) {
        String valor = atributo(linha, nome);
        if (!valor.equals("true") && !valor.equals("false")) {
            throw invalido("valor lógico inválido em \"" + nome + "\": " + valor);
        }
        return valor.equals("true");
    }

    private static void adicionar(List<String> xml, int nivel, String tag) {
        xml.add("  ".repeat(nivel) + tag);
    }

    private static String abre(String nome, String... atributos) {
        return montar(nome, ">", atributos);
    }

    private static String vazio(String nome, String... atributos) {
        return montar(nome, "/>", atributos);
    }

    private static String montar(String nome, String fim, String... atributos) {
        StringBuilder tag = new StringBuilder("<").append(nome);
        for (int i = 0; i < atributos.length; i += 2) {
            tag.append(' ').append(atributos[i]).append("=\"").append(escapar(atributos[i + 1])).append('"');
        }
        return tag.append(fim).toString();
    }

    private static String escapar(String valor) {
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;");
    }

    private static String desescapar(String valor) {
        return valor.replace("&quot;", "\"").replace("&lt;", "<").replace("&amp;", "&");
    }
}
