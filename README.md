# SGPE — Sistema de Gestão de Projetos e Equipes

Aplicação **desktop Java Swing** para gestão de projetos, equipes, tarefas e
usuários, com **controle de acesso por perfil (RBAC)** e persistência local em
**SQLite**. A arquitetura segue o padrão **MVC clássico** (View → Controller →
DAO → banco), sem frameworks externos além do driver JDBC.

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Stack tecnológica](#stack-tecnológica)
- [Arquitetura](#arquitetura)
  - [Diagrama de pacotes](#diagrama-de-pacotes)
  - [Diagrama de componentes](#diagrama-de-componentes)
- [Modelo de dados](#modelo-de-dados)
- [Fluxos](#fluxos)
  - [Autenticação e navegação](#autenticação-e-navegação)
  - [Estados de projeto e tarefa](#estados-de-projeto-e-tarefa)
- [Perfis e permissões](#perfis-e-permissões)
- [Pré-requisitos](#pré-requisitos)
- [Instalação e configuração](#instalação-e-configuração)
  - [Linux](#linux)
  - [Windows](#windows)
- [Comandos principais](#comandos-principais)
- [Como executar](#como-executar)
- [Banco de dados](#banco-de-dados)
- [Estrutura do projeto](#estrutura-do-projeto)

---

## Funcionalidades

- **Autenticação** por login e senha, com sessão única em memória.
- **Tela principal (hub)** que expõe os recursos conforme o perfil do usuário.
- **Cadastro de usuários** (exclusivo do Administrador).
- **Cadastro de projetos, equipes e tarefas** (CRUD).
- **Vínculo** de equipes a projetos e de usuários a equipes.
- **Consulta de projetos** com filtros (nome, status, período, equipe).
- **Relatório de desempenho** por projeto (percentual de conclusão, contagens,
  equipes e responsáveis envolvidos).
- **Escopo do colaborador**: o colaborador visualiza apenas os dados das equipes
  de que participa e opera em modo leitura.

---

## Stack tecnológica

| Camada | Tecnologia | Versão |
|--------|------------|--------|
| Linguagem | Java (JDK) | **21** |
| Build | Apache Maven | 3.6+ |
| Interface gráfica | Java Swing | nativo do JDK |
| Banco de dados | SQLite (via `sqlite-jdbc` da Xerial) | 3.53.1.0 |
| Testes | JUnit Jupiter (JUnit 5) | 5.14.4 |
| Empacotamento | `maven-shade-plugin` (fat jar executável) | 3.6.2 |

Sem dependências de runtime além do driver SQLite — a interface usa apenas o
Swing do próprio JDK.

---

## Arquitetura

O projeto adota **MVC clássico** com injeção de dependências manual feita no
ponto de entrada (`Main`). Cada camada tem responsabilidade única:

- **View** (`view/`): telas Swing; não acessam o banco diretamente.
- **Controller** (`controller/`): regras de negócio e orquestração dos DAOs.
- **DAO** (`dao/`): persistência via JDBC puro (`PreparedStatement`).
- **Model** (`model/`): entidades, enums, DTOs e filtros.
- **Security** (`security/`): RBAC declarativo e escopo de dados do colaborador.
- **Database** (`database/`): conexão SQLite e migração do schema.
- **Config** (`config/`): sessão do usuário autenticado (Singleton).

### Diagrama de pacotes

```mermaid
flowchart TD
    Main["Main<br>(entry point + wiring)"]
    view["view<br>(Swing UI)"]
    security["security<br>(RBAC + escopo)"]
    controller["controller<br>(regras de negócio)"]
    config["config<br>(sessão)"]
    dao["dao<br>(JDBC)"]
    database["database<br>(conexão + migração)"]
    model["model<br>(entity, enums, dto, filter)"]
    exception["exception"]
    util["util"]

    Main --> database
    Main --> dao
    Main --> controller
    Main --> security
    Main --> view

    view --> controller
    view --> security
    view --> config
    view --> model
    view --> util
    view --> exception

    controller --> dao
    controller --> model
    controller --> exception

    security --> dao
    security --> model

    dao --> database
    dao --> model
    config --> model
```

### Diagrama de componentes

```mermaid
flowchart LR
    subgraph Apresentacao["Apresentação (Swing)"]
        LoginView
        MainView["MainView (hub)"]
        Telas["Telas de recurso<br>(Usuario/Projeto/Tarefa/Equipe/Consulta/Relatorio)"]
    end

    subgraph SessaoSeg["Sessão e Segurança"]
        AppConfig["AppConfig<br>(Singleton de sessão)"]
        PermissoesPerfil["PermissoesPerfil<br>(perfil → recursos)"]
        EscopoColaborador["EscopoColaborador<br>(projetos do colaborador)"]
    end

    subgraph Negocio["Negócio"]
        Controllers["Controllers"]
    end

    subgraph Persistencia["Persistência"]
        DAOs["DAOs (JDBC)"]
        DBConn["DatabaseConnection<br>(PRAGMA foreign_keys = ON)"]
    end

    DB[("SQLite<br>data/sgpe.db")]

    LoginView -->|autenticar| Controllers
    Controllers -->|sessão| AppConfig
    MainView -->|recursosDe / podeAcessar| PermissoesPerfil
    MainView --> Telas
    Telas --> Controllers
    Telas -->|filtro de escopo| EscopoColaborador
    Controllers --> DAOs
    EscopoColaborador --> DAOs
    DAOs --> DBConn
    DBConn --> DB
```

---

## Modelo de dados

Seis tabelas no SQLite (DDL em `src/main/resources/db/schema.sql`). As duas
tabelas de junção (`equipe_usuario`, `projeto_equipe`) modelam relações
muitos-para-muitos. Datas são persistidas como `TEXT` (ISO) e enums via o
`name()` da constante.

```mermaid
erDiagram
    usuarios {
        INTEGER id PK
        TEXT nome_completo
        TEXT cpf UK
        TEXT email
        TEXT cargo
        TEXT login UK
        TEXT senha
        TEXT perfil
    }
    projetos {
        INTEGER id PK
        TEXT nome
        TEXT descricao
        TEXT data_inicio
        TEXT data_termino_prevista
        TEXT status
    }
    equipes {
        INTEGER id PK
        TEXT nome
        TEXT descricao
    }
    tarefas {
        INTEGER id PK
        TEXT titulo
        TEXT descricao
        INTEGER projeto_id FK
        INTEGER responsavel_id FK
        TEXT data_inicio
        TEXT data_termino_prevista
        TEXT status
    }
    equipe_usuario {
        INTEGER equipe_id PK
        INTEGER usuario_id PK
    }
    projeto_equipe {
        INTEGER projeto_id PK
        INTEGER equipe_id PK
    }

    usuarios  ||--o{ tarefas        : "responsável por"
    projetos  ||--o{ tarefas        : "contém"
    usuarios  ||--o{ equipe_usuario : "participa"
    equipes   ||--o{ equipe_usuario : "tem membro"
    projetos  ||--o{ projeto_equipe : "atendido por"
    equipes   ||--o{ projeto_equipe : "atua em"
```

---

## Fluxos

### Autenticação e navegação

Após o login, o `LoginController` registra o usuário em `AppConfig` e abre a
`MainView`, que monta os botões conforme o perfil (RBAC). Cada recurso é aberto
sob demanda, recebendo o contexto do usuário (perfil/escopo) quando há filtro ou
modo leitura.

```mermaid
sequenceDiagram
    actor U as Usuário
    participant LV as LoginView
    participant LC as LoginController
    participant AC as AppConfig
    participant MV as MainView
    participant PP as PermissoesPerfil
    participant T as Tela do recurso

    U->>LV: informa login e senha
    LV->>LC: autenticar(login, senha)
    LC->>AC: setUsuarioAutenticado(usuário)
    LC-->>LV: usuário autenticado
    LV->>MV: abre o hub (injeta controllers + escopo)
    MV->>PP: recursosDe(perfil)
    PP-->>MV: recursos permitidos
    MV-->>U: exibe apenas os botões permitidos
    U->>MV: clica num recurso
    MV->>PP: podeAcessar(perfil, recurso)?
    PP-->>MV: sim
    MV->>T: abre a tela (contexto: usuário, modo leitura/escopo)
    U->>MV: clica em "Sair"
    MV->>AC: limpar()
    MV->>LV: retorna ao login
```

### Estados de projeto e tarefa

Os enums `StatusProjeto` e `StatusTarefa` representam o ciclo de vida. As
transições abaixo refletem o fluxo de negócio esperado (o modelo persiste o
status como texto livre; as transições não são impostas pelo código).

```mermaid
stateDiagram-v2
    direction LR
    state "StatusProjeto" as SP {
        [*] --> PLANEJADO
        PLANEJADO --> EM_ANDAMENTO
        PLANEJADO --> CANCELADO
        EM_ANDAMENTO --> CONCLUIDO
        EM_ANDAMENTO --> CANCELADO
        CONCLUIDO --> [*]
        CANCELADO --> [*]
    }
```

```mermaid
stateDiagram-v2
    direction LR
    state "StatusTarefa" as ST {
        [*] --> PENDENTE
        PENDENTE --> EM_ANDAMENTO
        PENDENTE --> CANCELADA
        EM_ANDAMENTO --> CONCLUIDA
        EM_ANDAMENTO --> CANCELADA
        CONCLUIDA --> [*]
        CANCELADA --> [*]
    }
```

---

## Perfis e permissões

O acesso aos recursos é resolvido de forma declarativa em
`security/PermissoesPerfil` (mapa `PerfilUsuario → conjunto de recursos`). A
`MainView` renderiza apenas os botões permitidos e revalida o acesso antes de
abrir cada tela (defesa em profundidade).

| Recurso | Administrador | Gerente | Colaborador |
|---------|:---:|:---:|:---:|
| Usuários | ✅ | — | — |
| Projetos | ✅ | ✅ | — |
| Tarefas | ✅ | ✅ | ✅ (modo leitura, só as suas) |
| Equipes | ✅ | ✅ | — |
| Consultar Projetos | ✅ | ✅ | ✅ (apenas os do seu escopo) |
| Relatório de Desempenho | ✅ | ✅ | ✅ (apenas os do seu escopo) |

O **Colaborador** visualiza apenas dados das equipes de que participa
(`EscopoColaborador`) e não pode criar, editar nem excluir registros.

---

## Pré-requisitos

| Ferramenta | Versão mínima | Observação |
|------------|---------------|------------|
| JDK | 21 | Temurin/OpenJDK recomendado |
| Apache Maven | 3.6 | Gerencia build, testes e empacotamento |
| Git | qualquer | Para clonar o repositório |
| Ambiente gráfico | — | A interface Swing exige um display (não roda em servidor headless) |

Verifique as versões com:

```bash
java -version
mvn -version
```

---

## Instalação e configuração

### Linux

**Opção A — gerenciador de pacotes (Debian/Ubuntu):**

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven git
```

**Opção B — SDKMAN (qualquer distribuição):**

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21-tem
sdk install maven
```

**Clonar e construir:**

```bash
git clone <URL-DO-REPOSITORIO>
cd project-manager
mvn clean package
java -jar target/sistema-gestao-projetos-equipes-1.0.0.jar
```

### Windows

**Opção A — winget (PowerShell):**

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Maven
winget install Git.Git
```

Feche e reabra o terminal para recarregar o `PATH`. Se o Maven não definir o
`JAVA_HOME` automaticamente, configure-o (ajuste o caminho para a versão
instalada):

```powershell
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21"
```

**Opção B — instalação manual:**

1. Baixe e instale o JDK 21 (Temurin/Oracle).
2. Baixe o Apache Maven, extraia e adicione a pasta `bin` ao `PATH`.
3. Defina `JAVA_HOME` apontando para a pasta do JDK.

**Clonar e construir (PowerShell ou CMD):**

```powershell
git clone <URL-DO-REPOSITORIO>
cd project-manager
mvn clean package
java -jar target\sistema-gestao-projetos-equipes-1.0.0.jar
```

---

## Comandos principais

Todos executados na raiz do projeto (onde está o `pom.xml`):

| Comando | Descrição |
|---------|-----------|
| `mvn clean` | Remove a pasta `target/` (artefatos de build) |
| `mvn compile` | Compila o código-fonte principal |
| `mvn test` | Executa a suíte de testes (JUnit 5) |
| `mvn package` | Compila, testa e gera o **fat jar** executável em `target/` |
| `mvn clean package` | Build limpo completo (recomendado antes de distribuir) |
| `mvn clean package -DskipTests` | Empacota sem rodar os testes (mais rápido) |

O artefato gerado é `target/sistema-gestao-projetos-equipes-1.0.0.jar`, já com
todas as dependências embutidas (`maven-shade-plugin`) e a classe principal
`br.com.dual.sgpe.Main` declarada no manifesto.

---

## Como executar

Após `mvn package`, execute o fat jar:

```bash
java -jar target/sistema-gestao-projetos-equipes-1.0.0.jar
```

No primeiro acesso, use as credenciais do administrador semeado
automaticamente:

| Login | Senha |
|-------|-------|
| `admin` | `admin` |

> A senha é armazenada em texto simples nesta versão (uso acadêmico). Não use
> credenciais reais.

---

## Banco de dados

- Engine: **SQLite**, arquivo único em `data/sgpe.db`, criado relativo ao
  diretório de execução.
- O schema é aplicado automaticamente na inicialização por
  `database/DatabaseMigrator` (a partir de `src/main/resources/db/schema.sql`,
  com `CREATE TABLE IF NOT EXISTS` — idempotente).
- As chaves estrangeiras são habilitadas por conexão (`PRAGMA foreign_keys = ON`
  em `DatabaseConnection`), pois o SQLite as desativa por padrão.
- Para reiniciar do zero, encerre a aplicação e remova o arquivo `data/sgpe.db`
  (mova para a lixeira) — ele será recriado e o admin inicial, semeado novamente.

---

## Estrutura do projeto

```text
project-manager/
├── pom.xml
├── README.md
├── data/                         # banco SQLite gerado em runtime (não versionado)
└── src/
    ├── main/
    │   ├── java/br/com/dual/sgpe/
    │   │   ├── Main.java          # ponto de entrada + wiring de dependências
    │   │   ├── config/            # AppConfig (sessão Singleton)
    │   │   ├── controller/        # regras de negócio
    │   │   ├── dao/               # acesso a dados (JDBC)
    │   │   ├── database/          # conexão e migração
    │   │   ├── exception/         # exceções de domínio
    │   │   ├── model/             # entity, enums, dto, filter
    │   │   ├── security/          # RBAC (PermissoesPerfil) e escopo (EscopoColaborador)
    │   │   └── view/              # telas Swing
    │   └── resources/db/
    │       └── schema.sql         # DDL das tabelas
    └── test/
        └── java/br/com/dual/sgpe/ # testes JUnit 5 (controllers, DAOs, security, util)
```
