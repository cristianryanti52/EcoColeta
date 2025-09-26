# 🌱 EcoColeta

**EcoColeta** é um sistema cliente-servidor simples em Java para cadastro e consulta de pontos de coleta seletiva de resíduos recicláveis.  
O objetivo é demonstrar arquitetura cliente-servidor, comunicação via `Socket` e manipulação de dados em memória sem persistência em banco de dados ou arquivos.

---

## 📌 Funcionalidades

- **RF01 - Listar pontos de coleta:** qualquer usuário pode consultar os pontos cadastrados.  
- **RF02 - Filtrar por tipo de resíduo:** o usuário pode buscar apenas pontos que aceitam **papel, plástico, metal, vidro**, etc.  
- **RF03 - Cadastrar ponto de coleta:** disponível apenas para administradores.  
- **RF04 - Atualizar ponto de coleta:** disponível apenas para administradores.  
- **RF05 - Login administrador:** acesso restrito via credenciais fixas no servidor.  

---

## ⚙️ Requisitos

- Java 8 ou superior
- Terminal/Prompt de comando

---

## 📂 Estrutura do Projeto

EcoColeta/
│
├── PontoColeta.java # Classe modelo (POJO) para pontos de coleta
├── ServidorEcoColeta.java # Servidor multithread que gerencia os dados em memória
├── ClienteEcoColeta.java # Cliente de console para interação com o servidor


---

## ▶️ Como Executar

### 1. Organizar os arquivos
Coloque os três arquivos `.java` (**PontoColeta.java**, **ServidorEcoColeta.java**, **ClienteEcoColeta.java**) em uma mesma pasta, por exemplo:

C:\EcoColeta


### 2. Compilar o código
No terminal, navegue até a pasta e rode:

```bash
javac *.java


Isso vai gerar os arquivos .class.

3. Executar o servidor

Abra um terminal na pasta e rode:

java ServidorEcoColeta


Você verá a mensagem:

Servidor EcoColeta iniciado na porta 12345

4. Executar o cliente

Abra outro terminal na mesma pasta e rode:

java ClienteEcoColeta


O menu do cliente será exibido.

🖥️ Exemplo de Uso
Usuário Cidadão

Listar pontos de coleta

Escolha a opção 1.

O servidor retornará todos os pontos cadastrados.

Consultar por tipo de resíduo

Escolha a opção 2.

Digite, por exemplo: papel.

Serão listados apenas os pontos que aceitam papel.

Usuário Administrador

Login

Escolha a opção 3.

Digite usuário: admin

Digite senha: 1234

Cadastrar ponto de coleta

Após login, escolha a opção 4.

Informe: nome, endereço e materiais aceitos.

Atualizar ponto existente

Escolha a opção 5.

Informe o nome do ponto já cadastrado e os novos dados.

🔑 Credenciais do Administrador

Usuário: admin

Senha: 1234

📜 Regras do Projeto

Sem persistência: todos os dados são armazenados apenas em memória no servidor.

Ao encerrar o servidor, todos os pontos cadastrados são perdidos.
    



