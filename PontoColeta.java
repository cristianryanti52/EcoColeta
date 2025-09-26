import java.util.*;
import java.util.stream.Collectors;

/**
 * PontoColeta - POJO que representa um ponto de coleta seletiva.
 * Mantém id, nome, endereço, tipos de resíduos aceitos (set) e contato.
 *
 * Também contém utilitários para serializar/deserializar para o protocolo texto
 * usado entre cliente e servidor (formato: id|nome|endereco|tipo1,tipo2|contato)
 */
public class PontoColeta {
    private final int id;
    private String nome;
    private String endereco;
    private Set<String> tipos; // armazenados em lowercase, sem espaços extras
    private String contato;

    public PontoColeta(int id, String nome, String endereco, Set<String> tipos, String contato) {
        this.id = id;
        this.nome = safe(nome);
        this.endereco = safe(endereco);
        this.tipos = normalizeTipos(tipos);
        this.contato = safe(contato);
    }

    // Normaliza e remove pipes/newlines (para evitar quebra do protocolo)
    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("|", "-").replace("\n", " ").trim();
    }

    private static Set<String> normalizeTipos(Set<String> raw) {
        if (raw == null) return new HashSet<>();
        return raw.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getEndereco() { return endereco; }
    public Set<String> getTipos() { return Collections.unmodifiableSet(tipos); }
    public String getContato() { return contato; }

    public void setNome(String nome) { this.nome = safe(nome); }
    public void setEndereco(String endereco) { this.endereco = safe(endereco); }
    public void setTipos(Set<String> tipos) { this.tipos = normalizeTipos(tipos); }
    public void setContato(String contato) { this.contato = safe(contato); }

    /**
     * Formata o PontoColeta em uma string compatível com o protocolo:
     * id|nome|endereco|tipo1,tipo2|contato
     */
    public String toProtocolString() {
        String tiposJoined = String.join(",", tipos);
        return id + "|" + nome + "|" + endereco + "|" + tiposJoined + "|" + contato;
    }

    /**
     * Converte uma string do protocolo para um objeto PontoColeta.
     * Espera formato: id|nome|endereco|tipo1,tipo2|contato
     */
    public static PontoColeta fromProtocolString(String line) throws IllegalArgumentException {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 5) throw new IllegalArgumentException("Linha inválida do protocolo: " + line);
        int id = Integer.parseInt(parts[0]);
        String nome = parts[1];
        String endereco = parts[2];
        String tiposStr = parts[3];
        Set<String> tipos = Arrays.stream(tiposStr.split(",", -1))
                                  .map(String::trim)
                                  .filter(s -> !s.isEmpty())
                                  .map(String::toLowerCase)
                                  .collect(Collectors.toCollection(LinkedHashSet::new));
        String contato = parts[4];
        return new PontoColeta(id, nome, endereco, tipos, contato);
    }

    /**
     * Checa se o ponto aceita um tipo de resíduo (case-insensitive)
     */
    public boolean aceitaTipo(String tipo) {
        if (tipo == null) return false;
        return tipos.contains(tipo.trim().toLowerCase());
    }

    /**
     * Representação amigável para exibição no cliente (multilinha)
     */
    public String toDisplayString() {
        return String.format("ID: %d\nNome: %s\nEndereço: %s\nTipos aceitos: %s\nContato: %s\n",
                id, nome, endereco, String.join(", ", tipos), contato);
    }

    @Override
    public String toString() {
        return toProtocolString();
    }
}
