import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ServidorEcoColeta
 * - Mantém os pontos de coleta em memória (ConcurrentHashMap).
 * - Aceita múltiplas conexões (thread por cliente).
 * - Protocolo simples baseado em linhas de texto (com '|' como separador).
 *
 * Comandos suportados (cliente -> servidor):
 *  - LIST
 *  - FILTER|tipo
 *  - LOGIN|usuario|senha
 *  - ADD|nome|endereco|tipo1,tipo2|contato    (admin somente)
 *  - UPDATE|id|nome|endereco|tipo1,tipo2|contato (admin somente)
 *  - EXIT
 *
 * Respostas do servidor: múltiplas linhas terminadas por "END".
 * Primeira linha costuma ser OK, AUTH_OK, AUTH_FAIL ou ERROR|mensagem
 * Linhas seguintes (quando houver dados) contêm os pontos no formato do protocolo.
 */
public class ServidorEcoColeta {

    private static final int PORT = 12345;
    // Credenciais fixas (RNF03) - para protótipo
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "12345";

    // Armazenamento em memória
    private final ConcurrentMap<Integer, PontoColeta> pontos = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public static void main(String[] args) {
        ServidorEcoColeta servidor = new ServidorEcoColeta();
        servidor.criarPontosDemo();
        servidor.start();
    }

    public void start() {
        System.out.println("Servidor EcoColeta iniciando na porta " + PORT + " ...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor ouvido em: " + serverSocket.getLocalSocketAddress());
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Conexão aceita: " + clientSocket.getRemoteSocketAddress());
                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start(); // RNF04: thread por cliente (simples)
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Popula alguns pontos de exemplo para facilitar testes
    private void criarPontosDemo() {
        addPontoInterno("Ponto Central - Papel e Plástico",
                "Praça Central, 100", new HashSet<>(Arrays.asList("papel", "plastico")), "contato@municipio.org");
        addPontoInterno("ReciclaMais - Vidro e Metal",
                "Rua das Flores, 45", new HashSet<>(Arrays.asList("vidro", "metal")), "reciclamais@ex.com");
        addPontoInterno("EcoPonto Bairro Alto - Todos",
                "Av. Brasil, 777", new HashSet<>(Arrays.asList("papel", "plastico", "vidro", "metal")), "ecoponto@bairroalto.com");
        System.out.println("Pontos demo criados.");
    }

    // Método interno para criar pontos sem checar autenticação (usado apenas no servidor)
    private void addPontoInterno(String nome, String endereco, Set<String> tipos, String contato) {
        int id = nextId.getAndIncrement();
        PontoColeta p = new PontoColeta(id, nome, endereco, tipos, contato);
        pontos.put(id, p);
    }

    // Implementação do handler por cliente
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private boolean isAdmin = false;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

                // Saudação inicial (opcional)
                out.println("OK|Bem-vindo ao EcoColeta");
                out.println("END");

                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    // dividir por '|' mantendo vazios
                    String[] parts = line.split("\\|", -1);
                    String cmd = parts[0].trim().toUpperCase();

                    switch (cmd) {
                        case "LIST":
                            handleList(out);
                            break;
                        case "FILTER":
                            handleFilter(parts, out);
                            break;
                        case "LOGIN":
                            handleLogin(parts, out);
                            break;
                        case "ADD":
                            handleAdd(parts, out);
                            break;
                        case "UPDATE":
                            handleUpdate(parts, out);
                            break;
                        case "EXIT":
                            out.println("OK|Bye");
                            out.println("END");
                            socket.close();
                            return;
                        default:
                            out.println("ERROR|Comando desconhecido: " + cmd);
                            out.println("END");
                    }
                }
            } catch (IOException e) {
                System.err.println("Conexão encerrada com cliente: " + e.getMessage());
            }
        }

        private void handleList(PrintWriter out) {
            out.println("OK");
            for (PontoColeta p : pontos.values()) {
                out.println(p.toProtocolString());
            }
            out.println("END");
        }

        private void handleFilter(String[] parts, PrintWriter out) {
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                out.println("ERROR|Tipo para filtro ausente");
                out.println("END");
                return;
            }
            String tipo = parts[1].trim().toLowerCase();
            out.println("OK");
            for (PontoColeta p : pontos.values()) {
                if (p.aceitaTipo(tipo)) {
                    out.println(p.toProtocolString());
                }
            }
            out.println("END");
        }

        private void handleLogin(String[] parts, PrintWriter out) {
            if (parts.length < 3) {
                out.println("ERROR|Formato LOGIN incorreto. Uso: LOGIN|usuario|senha");
                out.println("END");
                return;
            }
            String user = parts[1];
            String pass = parts[2];
            if (ADMIN_USER.equals(user) && ADMIN_PASS.equals(pass)) {
                isAdmin = true;
                out.println("AUTH_OK");
            } else {
                out.println("AUTH_FAIL");
            }
            out.println("END");
        }

        private void handleAdd(String[] parts, PrintWriter out) {
            if (!isAdmin) {
                out.println("ERROR|Operação requer autenticação de administrador");
                out.println("END");
                return;
            }
            // Esperado: ADD|nome|endereco|tipo1,tipo2|contato
            if (parts.length < 5) {
                out.println("ERROR|Formato ADD incorreto. Uso: ADD|nome|endereco|tipo1,tipo2|contato");
                out.println("END");
                return;
            }
            try {
                String nome = parts[1];
                String endereco = parts[2];
                String tiposStr = parts[3];
                String contato = parts[4];

                Set<String> tipos = parseTipos(tiposStr);
                int id = nextId.getAndIncrement();
                PontoColeta p = new PontoColeta(id, nome, endereco, tipos, contato);
                pontos.put(id, p);
                out.println("ADD_OK|" + id);
                out.println("END");
            } catch (Exception e) {
                out.println("ERROR|Erro ao adicionar ponto: " + e.getMessage());
                out.println("END");
            }
        }

        private void handleUpdate(String[] parts, PrintWriter out) {
            if (!isAdmin) {
                out.println("ERROR|Operação requer autenticação de administrador");
                out.println("END");
                return;
            }
            // Esperado: UPDATE|id|nome|endereco|tipo1,tipo2|contato
            if (parts.length < 6) {
                out.println("ERROR|Formato UPDATE incorreto. Uso: UPDATE|id|nome|endereco|tipo1,tipo2|contato");
                out.println("END");
                return;
            }
            try {
                int id = Integer.parseInt(parts[1].trim());
                PontoColeta existente = pontos.get(id);
                if (existente == null) {
                    out.println("ERROR|Ponto com ID " + id + " não encontrado");
                    out.println("END");
                    return;
                }
                String nome = parts[2];
                String endereco = parts[3];
                String tiposStr = parts[4];
                String contato = parts[5];

                existente.setNome(nome);
                existente.setEndereco(endereco);
                existente.setTipos(parseTipos(tiposStr));
                existente.setContato(contato);

                out.println("UPDATE_OK");
                out.println("END");
            } catch (NumberFormatException nfe) {
                out.println("ERROR|ID inválido");
                out.println("END");
            } catch (Exception e) {
                out.println("ERROR|Erro ao atualizar: " + e.getMessage());
                out.println("END");
            }
        }

        private Set<String> parseTipos(String tiposStr) {
            if (tiposStr == null || tiposStr.trim().isEmpty()) return new HashSet<>();
            String[] arr = tiposStr.split(",", -1);
            Set<String> s = new LinkedHashSet<>();
            for (String t : arr) {
                String norm = t.trim().toLowerCase();
                if (!norm.isEmpty()) s.add(norm);
            }
            return s;
        }
    }
}
