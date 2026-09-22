# Quality

Plataforma web para planejar, executar e acompanhar auditorias de garantia da qualidade. O projeto é composto por uma API REST em Spring Boot, um frontend React e um banco PostgreSQL.

## Tecnologias principais

- Java 21;
- Spring Boot 4.1.1;
- Maven 3.9 ou superior;
- PostgreSQL 18;
- Flyway;
- React 19.1.1;
- TypeScript 5.9.3;
- Vite 7.3.6;
- Node.js 20.19 ou superior, ou 22.12 ou superior;
- Docker e Docker Compose.

## Estrutura do projeto

```text
.
├── src/                 # API Spring Boot e testes do backend
├── frontend/            # Aplicação React
├── docs/                # Documentação funcional e técnica
├── compose.yaml         # PostgreSQL e API em contêineres
├── Dockerfile           # Build da API
├── .env.example         # Exemplo das variáveis do backend e banco
└── pom.xml              # Dependências e build Maven
```

## Pré-requisitos

Para o modo recomendado, instale:

- Docker com o comando `docker compose` disponível;
- Node.js compatível com o Vite;
- npm.

Para executar a API fora do Docker, instale também:

- JDK 21;
- Maven 3.9 ou superior;
- PostgreSQL 18, ou mantenha somente o banco no Docker.

Confirme as instalações:

```bash
docker --version
docker compose version
node --version
npm --version
java --version
mvn --version
```

## Execução recomendada

Neste modo, PostgreSQL e API são executados pelo Docker Compose. O frontend é executado pelo Vite para manter a atualização automática durante o desenvolvimento.

### 1. Configure as variáveis do backend

Na raiz do projeto, copie o arquivo de exemplo:

```bash
cp .env.example .env
```

Edite o `.env` e substitua todas as credenciais de exemplo. Gere uma chave JWT segura com:

```bash
openssl rand -base64 64
```

Use o resultado em `JWT_KEY`. Um exemplo de configuração local é:

```dotenv
POSTGRES_DB=quality
POSTGRES_USER=quality
POSTGRES_PASSWORD=uma-senha-local-segura
POSTGRES_PORT=5432

APP_PORT=8080
DDL_AUTO=update

JWT_EXPIRATION=900000
JWT_KEY=chave-gerada-pelo-openssl

ADMIN_NOME=Administrador
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=uma-senha-forte
```

O arquivo `.env` contém segredos e não deve ser versionado. O repositório já está configurado para ignorá-lo.

### 2. Inicie o banco e a API

```bash
docker compose up -d --build
```

Confira o estado dos serviços:

```bash
docker compose ps
```

Para acompanhar os logs da API:

```bash
docker compose logs -f app
```

Na primeira inicialização, o Flyway cria e atualiza a estrutura do banco. O sistema também cria o administrador informado em `ADMIN_EMAIL` caso esse e-mail ainda não exista.

### 3. Configure o frontend

Em outro terminal:

```bash
cd frontend
cp .env.example .env.local
npm ci
```

O arquivo `frontend/.env.local` deve apontar para a API:

```dotenv
VITE_API_URL=http://localhost:8080
```

### 4. Inicie o frontend

```bash
npm run dev
```

### 5. Acesse o sistema

- Aplicação: <http://localhost:5173>
- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- Contrato OpenAPI: <http://localhost:8080/v3/api-docs>

Entre na aplicação com `ADMIN_EMAIL` e `ADMIN_PASSWORD` definidos no `.env`.

## Executar a API localmente

Este modo é útil para depuração no IntelliJ IDEA, Eclipse ou VS Code. O exemplo abaixo mantém apenas o PostgreSQL no Docker.

### 1. Inicie somente o banco

```bash
docker compose up -d db
```

### 2. Exporte as variáveis

Em Linux ou macOS, na raiz do projeto:

```bash
set -a
. ./.env
set +a

export DATABASE_URL="jdbc:postgresql://localhost:${POSTGRES_PORT}/${POSTGRES_DB}"
export DATABASE_USERNAME="${POSTGRES_USER}"
export DATABASE_PASSWORD="${POSTGRES_PASSWORD}"
export CORS_ALLOWED_ORIGINS="http://localhost:5173"
```

O `.env` fornece `JWT_KEY`, dados do administrador e demais configurações. As três variáveis `DATABASE_*` fazem a correspondência entre os nomes usados pelo Docker e os nomes esperados pelo Spring Boot.

### 3. Inicie a API

```bash
mvn spring-boot:run
```

Também é possível criar e executar o artefato:

```bash
mvn clean package
java -jar target/Quality-0.0.1-SNAPSHOT.jar
```

## Variáveis de ambiente

| Variável | Finalidade | Valor local comum |
|---|---|---|
| `POSTGRES_DB` | Nome do banco criado pelo contêiner | `quality` |
| `POSTGRES_USER` | Usuário do PostgreSQL | `quality` |
| `POSTGRES_PASSWORD` | Senha do PostgreSQL | Sem padrão seguro |
| `POSTGRES_PORT` | Porta local do PostgreSQL | `5432` |
| `APP_PORT` | Porta local publicada para a API | `8080` |
| `DATABASE_URL` | URL JDBC usada pela API local | `jdbc:postgresql://localhost:5432/quality` |
| `DATABASE_USERNAME` | Usuário usado diretamente pela API | Mesmo valor de `POSTGRES_USER` |
| `DATABASE_PASSWORD` | Senha usada diretamente pela API | Mesmo valor de `POSTGRES_PASSWORD` |
| `DDL_AUTO` | Estratégia do Hibernate | `update` no desenvolvimento; `validate` em ambientes controlados |
| `JWT_KEY` | Chave de assinatura dos tokens | Deve ser gerada e mantida em segredo |
| `JWT_EXPIRATION` | Duração do token em milissegundos | `900000` |
| `ADMIN_NOME` | Nome do administrador inicial | `Administrador` |
| `ADMIN_EMAIL` | E-mail do administrador inicial | Definido pela equipe |
| `ADMIN_PASSWORD` | Senha do administrador inicial | Senha forte |
| `CORS_ALLOWED_ORIGINS` | Origens autorizadas a acessar a API | `http://localhost:5173` |
| `VITE_API_URL` | Endereço da API usado pelo frontend | `http://localhost:8080` |

Também existem configurações opcionais para o processamento de prazos:

| Variável | Padrão | Finalidade |
|---|---:|---|
| `PRAZO_SCHEDULER_INTERVALO_MS` | `60000` | Intervalo entre verificações de prazo |
| `PRAZO_SCHEDULER_ATRASO_INICIAL_MS` | `30000` | Espera antes da primeira execução |
| `PRAZO_SCHEDULER_TAMANHO_LOTE` | `100` | Quantidade máxima processada por ciclo |
| `FLYWAY_BASELINE_ON_MIGRATE` | `false` | Cria baseline ao migrar um banco legado |

## Testes e verificações

### Backend

Executar os testes unitários:

```bash
mvn test
```

Executar a verificação completa, incluindo testes de integração reconhecidos pelo Maven Failsafe:

```bash
mvn verify
```

Testes que utilizam Testcontainers precisam que o Docker esteja em execução.

### Frontend

Dentro de `frontend/`:

```bash
npm test
npm run typecheck
npm run build
```

O build de produção é gerado em `frontend/dist/`.

## Encerrar o ambiente

Parar os contêineres sem apagar o banco:

```bash
docker compose down
```

Para remover também o volume do PostgreSQL e apagar todos os dados locais:

```bash
docker compose down -v
```

> Atenção: a opção `-v` remove definitivamente o banco armazenado pelo Docker neste ambiente.

## Problemas comuns

### A porta já está em uso

Altere `APP_PORT` ou `POSTGRES_PORT` no `.env`. Se mudar a porta da API, atualize também `VITE_API_URL` no frontend.

### A API não conecta ao PostgreSQL

Confira o estado e os logs:

```bash
docker compose ps
docker compose logs db
docker compose logs app
```

Ao executar a API no computador, use `localhost` na `DATABASE_URL`. Dentro do Compose, a API usa o nome de serviço `db` automaticamente.

### Erro relacionado à chave JWT

Gere outra chave com `openssl rand -base64 64`, atualize `JWT_KEY` e reinicie a API:

```bash
docker compose restart app
```

### O navegador bloqueia a chamada por CORS

Confirme que a origem exata do frontend está em `CORS_ALLOWED_ORIGINS`. No desenvolvimento padrão, use `http://localhost:5173`.

### O Vite não inicia por incompatibilidade do Node.js

Use Node.js `20.19` ou superior, ou `22.12` ou superior. Depois reinstale as dependências:

```bash
cd frontend
npm ci
```

### Alterei as credenciais do administrador, mas o usuário não mudou

O administrador inicial só é criado quando o e-mail configurado ainda não existe. Alterar o `.env` não modifica automaticamente uma conta já cadastrada.

### Erro de migration do Flyway

Não edite migrations que já tenham sido aplicadas. Crie uma nova migration em `src/main/resources/db/migration`. Em um ambiente local descartável, o volume pode ser recriado com `docker compose down -v`, sabendo que isso apaga todos os dados.

## Documentação adicional

- [Documentação do projeto](docs/README.md)
- [Especificação de requisitos](docs/Projeto/REQUISITOS_DO_SISTEMA.md)
- [Escopo do projeto](docs/Projeto/ESCOPO_PROJETO_AUDITORIA_QUALIDADE.md)
- [Documentação do frontend](docs/Frontend/README.md)
- [Plano de teste](docs/Projeto/PLANO_DE_TESTE_QUALITY_V1.4.docx)

