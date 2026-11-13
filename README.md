# 🏛️ Arquitetura da API — Projeto Littera

Este documento descreve a organização arquitetural, os padrões de projeto, as regras de negócio e a infraestrutura de implantação da API RESTful do sistema **Littera** (`littera-api`).

---

## 1. Visão Geral das Camadas (Layered Architecture)

A aplicação segue o padrão de **Arquitetura em Camadas (Layered Architecture)** sobre o ecossistema Spring Boot, promovendo baixo acoplamento, alta coesão e clara separação de responsabilidades entre entrada/saída HTTP, segurança, regras de domínio, integrações externas e persistência de dados.

```text
[ Cliente / Front-end React ]
             │
             ▼
┌─────────────────────────────────────────┐
│  Camada de Segurança (JWT / RBAC)       │
├─────────────────────────────────────────┤
│  Camada de Apresentação (Controllers)   │
├─────────────────────────────────────────┤
│  Camada de Negócio (Services)           │ ──► [ Integrações Externas (RestClient) ]
├─────────────────────────────────────────┤
│  Camada de Persistência (Repositories)  │
└─────────────────────────────────────────┘
             │
             ▼
     [ PostgreSQL 17 ]
```

---

## 2. Detalhamento de Cada Camada

### A. Camada de Apresentação (`controller/` e `dto/`)
Responsável por receber as requisições HTTP, validar contratos de entrada e devolver respostas padronizadas.

* **Controllers REST (`controller/`):** Expõem endpoints RESTful sob o prefixo `/api/...` utilizando métodos HTTP semânticos (`GET`, `POST`, `PUT`, `DELETE`).
* **Tratamento de Exceções Global:** Centralizado em `GlobalExceptionHandler.java` anotado com `@RestControllerAdvice`, interceptando e padronizando respostas de erro para:
  * Validações de argumentos e regras de entrada (`400 Bad Request`).
  * Falhas de autenticação e credenciais inválidas (`401 Unauthorized`).
  * Recursos não encontrados na base de dados (`404 Not Found`).
  * Violações de integridade referencial no banco de dados.
* **Padrão DTO — Data Transfer Objects (`dto/`):** Uso intensivo de **Java Records** imutáveis para contratos de entrada e saída (ex.: `EmprestimoRequest`, `EmprestimoResponse`, `UsuarioResumo`), desacoplando completamente as entidades do banco de dados das respostas expostas pela API.

---

### B. Camada de Segurança e Autenticação
Gerencia a identidade, integridade das requisições e permissões de acesso aos recursos da biblioteca.

* **Configuração Stateless:** Definida em `SecurityConfig.java` utilizando `SessionCreationPolicy.STATELESS` e proteção CSRF desabilitada, ideal para APIs REST consumidas por SPAs (React).
* **JWT (JSON Web Token):** Geração e validação de tokens implementadas em `TokenService.java`, com interceptação automática a cada requisição através do filtro `SecurityFilter.java` (`OncePerRequestFilter`).
* **Controle de Acesso Baseado em Perfis (RBAC):** Distinção granular de autorização entre os perfis `ROLE_ADMIN` e `ROLE_LEITOR` diretamente nas regras de roteamento do Spring Security. As senhas são armazenadas exclusivamente com hash criptográfico **BCrypt**.

---

### C. Camada de Negócio (`service/`)
Encapsula toda a lógica de domínio, cálculos de prazos, penalidades e validações operacionais.

* **Empréstimos (`EmprestimoService.java`):**
  * **Prazos Dinâmicos:** Cálculo automático da data de devolução prevista conforme a categoria do item (**14 dias** para `Livro` e **7 dias** para `Revista`).
  * **Sistema de Penalidades:** Ao registrar a devolução real, calcula eventuais dias de atraso e aplica bloqueio automático ao usuário equivalente ao **dobro dos dias em atraso**.
  * **Consistência de Estoque e Status:** Verifica previamente a disponibilidade física do exemplar e se o leitor possui restrições ativas antes de efetivar o empréstimo.
* **Reservas (`ReservaService.java`):**
  * Impede a criação de reservas para obras que ainda possuem exemplares disponíveis em estoque, direcionando o fluxo para o empréstimo imediato.

---

### D. Camada de Integração Externa (Third-Party Services)
Utiliza o cliente HTTP moderno e síncrono do Spring (**`RestClient`**) para comunicação com APIs externas de catalogação e tradução:

* **`OpenLibraryService.java`:** Consulta e enriquecimento automático de metadados de livros a partir do código **ISBN**, contando com política de resiliência e retentativas (*retry*).
* **`CrossrefService.java`:** Consulta de periódicos e revistas científicas a partir do código **ISSN**.
* **`DeepLTranslationService.java`:** Tradução automatizada de sinopses e descrições importadas para o idioma português.

---

### E. Camada de Persistência e Modelo de Dados (`model/` e `repository/`)
Responsável pelo mapeamento objeto-relacional, consultas e evolução controlada do esquema de banco de dados.

* **Mapeamento Objeto-Relacional (JPA / Hibernate):**
  * **Herança Polimórfica (`InheritanceType.JOINED`):** A entidade abstrata `ItemAcervo.java` concentra os atributos comuns do catálogo na tabela `item_acervo`, enquanto as subclasses `Livro.java` e `Revista.java` persistem seus atributos específicos em tabelas filhas relacionadas por chave primária/estrangeira.
* **Spring Data Repositories (`repository/`):** Interfaces que abstraem operações CRUD e consultas derivadas otimizadas para o domínio.
* **Migrações de Banco de Dados:** Versionamento determinístico do schema gerenciado pelo **Flyway** (`V1__schema_inicial.sql`), garantindo reprodutibilidade entre ambientes locais e de produção.

---

## 3. Infraestrutura e Implantação

* **Containerização Multi-Stage (`Dockerfile`):**
  * **Estágio 1 (`Builder`):** Utiliza a imagem `eclipse-temurin:21-jdk-jammy` para compilar e empacotar o artefato `.jar` via Gradle sem daemon (`--no-daemon`).
  * **Estágio 2 (`Runtime`):** Utiliza a imagem JRE enxuta `eclipse-temurin:21-jre-jammy`, executando o processo sob um usuário dedicado não-root (`spring:spring`) seguindo boas práticas de segurança de containers.
* **Orquestração (`docker-compose.yml`):**
  * Gerencia de forma integrada os containers da `api` e do banco de dados `db` (**PostgreSQL 17 Alpine**).
  * Implementa `healthcheck` nativo com `pg_isready`, garantindo que a inicialização da aplicação Spring Boot ocorra somente após o banco de dados estar totalmente operacional.

---

## 4. Resumo da Stack Tecnológica

| Componente | Tecnologia / Versão |
| :--- | :--- |
| **Linguagem / Runtime** | Java 21 |
| **Framework Base** | Spring Boot |
| **Segurança** | Spring Security + JWT (`auth0:java-jwt`, `jjwt`) + BCrypt |
| **Persistência / ORM** | Spring Data JPA + Hibernate |
| **Banco de Dados** | PostgreSQL 17 |
| **Migração de Schema** | Flyway |
| **Cliente HTTP** | Spring `RestClient` |
| **Documentação da API** | SpringDoc OpenAPI (Swagger UI) |
| **Build & Deploy** | Gradle + Docker & Docker Compose |