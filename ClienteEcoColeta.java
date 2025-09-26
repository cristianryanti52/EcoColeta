import java.io.*;
import java.net.*;
import java.util.*;

/**
 * ClienteEcoColeta - interface de linha de comando para interagir com o servidor.
 * Menu simples para usuário-cidadão e para administrador (após login).
 *
 * Uso: java ClienteEcoColeta [host] [port]
 * Ex: java ClienteEcoColeta localhost 12345
 */
public class ClienteEcoColeta {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 12345;

    private String host;
    private int port;
    private boolean isAdmin = false;

    private Socket socket;
    private BufferedReader serverIn;
    private PrintWriter serverOut;
    private Scanner scanner;

    public ClienteEcoColeta(String host, int port) {
        this.host = host;
        this.port = port;
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try { port = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }

        ClienteEcoColeta cliente = new ClienteEcoColeta(host, port);
        cliente.run();
    }

    private void run() {
        try {
            socket = new Socket(host, port);
            serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            serverOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Lê a saudação inicial
            List<String> welcome = readServerResponse();
            for (String l : welcome) {
                System.out.println(l);
            }

            boolean running = true;
            while (running) {
                showMenu();
                String choice = scanner.nextLine().trim();
                if (!isAdmin) {
                    switch (choice) {
                        case "1": sendSimpleCommand("LIST"); handleListResponse(); break;
                        case "2": handleFilterByType(); break;
                        case "3": handleLogin(); break;
                        case "4": sendSimpleCommand("EXIT"); running = false; break;
                        default: System.out.println("Opção inválida."); break;
                    }
                } else {
                    // menu admin
                    switch (choice) {
                        case "1": sendSimpleCommand("LIST"); handleListResponse(); break;
                        case "2": handleFilterByType(); break;
                        case "3": handleAddPoint(); break;
                        case "4": handleUpdatePoint(); break;
                        case "5": handleLogout(); break;
                        case "6": sendSimpleCommand("EXIT"); running = false; break;
                        default: System.out.println("Opção inválida."); break;
                    }
                }
            }

            System.out.println("Encerrando cliente...");
            closeConnections();

        } catch (IOException e) {
            System.err.println("Não foi possível conectar ao servidor: " + e.getMessage());
        }
    }

    private void showMenu() {
        System.out.println("\n=== EcoColeta ===");
        System.out.println("Conectado a: " + host + ":" + port + (isAdmin ? " (ADMIN)" : ""));
        System.out.println("1) Listar todos os pontos de coleta");
        System.out.println("2) Buscar por tipo de resíduo (ex: papel, plastico, vidro, metal)");
        if (!isAdmin) {
            System.out.println("3) Login como administrador");
            System.out.println("4) Sair");
        } else {
            System.out.println("3) Cadastrar novo ponto (ADMIN)");
            System.out.println("4) Atualizar ponto existente (ADMIN)");
            System.out.println("5) Logout");
            System.out.println("6) Sair");
        }
        System.out.print("Escolha: ");
    }

    private void sendSimpleCommand(String cmd) {
        serverOut.println(cmd);
    }

    private List<String> readServerResponse() throws IOException {
        List<String> lines = new ArrayList<>();
        String l;
        while ((l = serverIn.readLine()) != null) {
            if ("END".equals(l)) break;
            lines.add(l);
        }
        return lines;
    }

    private void handleListResponse() throws IOException {
        List<String> resp = readServerResponse();
        if (resp.isEmpty()) {
            System.out.println("(sem resposta)");
            return;
        }
        String first = resp.get(0);
        if (first.startsWith("ERROR|")) {
            System.out.println("Erro: " + first.substring("ERROR|".length()));
            return;
        }
        if (first.equals("OK")) {
            // demais linhas são pontos (formato protocolo)
            if (resp.size() == 1) {
                System.out.println("(nenhum ponto encontrado)");
                return;
            }
            for (int i = 1; i < resp.size(); i++) {
                try {
                    PontoColeta p = PontoColeta.fromProtocolString(resp.get(i));
                    System.out.println("-----");
                    System.out.println(p.toDisplayString());
                } catch (Exception e) {
                    System.out.println("Linha inválida recebida: " + resp.get(i));
                }
            }
        } else {
            // pode ser mensagem direta
            for (String s : resp) System.out.println(s);
        }
    }

    private void handleFilterByType() {
        System.out.print("Tipo de resíduo para buscar (ex: papel): ");
        String tipo = scanner.nextLine().trim();
        if (tipo.isEmpty()) {
            System.out.println("Tipo vazio.");
            return;
        }
        serverOut.println("FILTER|" + sanitize(tipo));
        try {
            List<String> resp = readServerResponse();
            if (resp.isEmpty()) { System.out.println("(sem resposta)"); return; }
            String first = resp.get(0);
            if (first.startsWith("ERROR|")) {
                System.out.println("Erro: " + first.substring("ERROR|".length()));
                return;
            }
            if (first.equals("OK")) {
                if (resp.size() == 1) {
                    System.out.println("(nenhum ponto aceita " + tipo + ")");
                    return;
                }
                for (int i = 1; i < resp.size(); i++) {
                    try {
                        PontoColeta p = PontoColeta.fromProtocolString(resp.get(i));
                        System.out.println("-----");
                        System.out.println(p.toDisplayString());
                    } catch (Exception e) {
                        System.out.println("Linha inválida recebida: " + resp.get(i));
                    }
                }
            } else {
                for (String s : resp) System.out.println(s);
            }
        } catch (IOException e) {
            System.err.println("Erro na comunicação: " + e.getMessage());
        }
    }

    private void handleLogin() {
        System.out.print("Usuário: ");
        String user = scanner.nextLine().trim();
        System.out.print("Senha: ");
        String pass = scanner.nextLine().trim();
        serverOut.println("LOGIN|" + sanitize(user) + "|" + sanitize(pass));
        try {
            List<String> resp = readServerResponse();
            if (!resp.isEmpty()) {
                String first = resp.get(0);
                if ("AUTH_OK".equals(first)) {
                    System.out.println("Autenticação bem-sucedida. Você está logado como ADMIN.");
                    isAdmin = true;
                } else if ("AUTH_FAIL".equals(first)) {
                    System.out.println("Falha na autenticação. Usuário/senha incorretos.");
                } else {
                    System.out.println(String.join("\n", resp));
                }
            }
        } catch (IOException e) {
            System.err.println("Erro lendo resposta: " + e.getMessage());
        }
    }

    private void handleLogout() {
        isAdmin = false;
        System.out.println("Logout realizado.");
    }

    private void handleAddPoint() {
        System.out.println("=== Cadastrar novo ponto de coleta ===");
        System.out.print("Nome: ");
        String nome = scanner.nextLine().trim();
        System.out.print("Endereço: ");
        String endereco = scanner.nextLine().trim();
        System.out.print("Tipos aceitos (separar por vírgula, ex: papel,plastico): ");
        String tipos = scanner.nextLine().trim();
        System.out.print("Contato (email/telefone): ");
        String contato = scanner.nextLine().trim();

        // remove pipes para não quebrar protocolo
        serverOut.println("ADD|" + sanitize(nome) + "|" + sanitize(endereco) + "|" + sanitize(tipos) + "|" + sanitize(contato));
        try {
            List<String> resp = readServerResponse();
            if (!resp.isEmpty()) {
                String first = resp.get(0);
                if (first.startsWith("ADD_OK|")) {
                    String id = first.substring("ADD_OK|".length());
                    System.out.println("Ponto adicionado com ID: " + id);
                } else if (first.startsWith("ERROR|")) {
                    System.out.println("Erro: " + first.substring("ERROR|".length()));
                } else {
                    System.out.println(String.join("\n", resp));
                }
            }
        } catch (IOException e) {
            System.err.println("Erro na comunicação: " + e.getMessage());
        }
    }

    private void handleUpdatePoint() {
        System.out.print("ID do ponto a atualizar: ");
        String idStr = scanner.nextLine().trim();
        try {
            Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }
        System.out.print("Novo nome: ");
        String nome = scanner.nextLine().trim();
        System.out.print("Novo endereço: ");
        String endereco = scanner.nextLine().trim();
        System.out.print("Novos tipos (ex: papel,vidro): ");
        String tipos = scanner.nextLine().trim();
        System.out.print("Novo contato: ");
        String contato = scanner.nextLine().trim();

        serverOut.println("UPDATE|" + sanitize(idStr) + "|" + sanitize(nome) + "|" + sanitize(endereco) + "|" + sanitize(tipos) + "|" + sanitize(contato));
        try {
            List<String> resp = readServerResponse();
            if (!resp.isEmpty()) {
                String first = resp.get(0);
                if ("UPDATE_OK".equals(first)) {
                    System.out.println("Ponto atualizado com sucesso.");
                } else if (first.startsWith("ERROR|")) {
                    System.out.println("Erro: " + first.substring("ERROR|".length()));
                } else {
                    System.out.println(String.join("\n", resp));
                }
            }
        } catch (IOException e) {
            System.err.println("Erro na comunicação: " + e.getMessage());
        }
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replace("|", "-").replace("\n", " ").trim();
    }

    private void closeConnections() {
        try {
            if (serverIn != null) serverIn.close();
            if (serverOut != null) serverOut.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
