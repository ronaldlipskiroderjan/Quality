# Quality

API de gestão da qualidade construída com Java 21, Spring Boot, PostgreSQL e autenticação JWT.

Este guia prepara um Fedora Workstation para executar a API e o PostgreSQL com Docker Compose e publicar o código no GitHub sem enviar credenciais.

## 1. Atualizar o Fedora

As instruções oficiais atuais do Docker suportam Fedora 43 e 44. Se o computador ainda estiver no Fedora 42, faça backup dos arquivos importantes e atualize para o Fedora 44 antes de instalar o Docker:

```bash
sudo dnf upgrade --refresh
sudo reboot
```

Depois que o computador reiniciar:

```bash
sudo dnf system-upgrade download --releasever=44
sudo dnf5 offline reboot
```

A atualização reinicia o computador e pode demorar. Não interrompa o processo. Consulte também a [documentação de atualização do Fedora](https://docs.fedoraproject.org/en-US/quick-docs/upgrading-fedora-offline/).

## 2. Instalar o Docker no Fedora

Execute estes comandos no terminal nativo do Fedora, fora do terminal do IntelliJ instalado via Flatpak.

Adicione o repositório oficial e instale o Engine, Buildx e Compose:

```bash
sudo dnf config-manager addrepo --from-repofile https://download.docker.com/linux/fedora/docker-ce.repo
sudo dnf install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
```

Teste a instalação:

```bash
sudo docker run hello-world
```

Para executar Docker sem `sudo`, adicione seu usuário ao grupo `docker`:

```bash
sudo usermod -aG docker "$USER"
```

Encerre a sessão do Fedora e entre novamente. O grupo `docker` concede privilégios equivalentes aos de root. Depois valide:

```bash
docker version
docker compose version
```

Documentação oficial: [Docker Engine no Fedora](https://docs.docker.com/engine/install/fedora/) e [pós-instalação no Linux](https://docs.docker.com/engine/install/linux-postinstall/).

## 3. Configurar o ambiente local

Entre na raiz deste projeto e crie o arquivo local de ambiente:

```bash
cp .env.example .env
openssl rand -base64 64
```

Abra `.env`, substitua as senhas de exemplo e copie a saída do `openssl` para `JWT_KEY`. O `.env` contém segredos e é ignorado pelo Git e pelo build Docker. Nunca o envie ao GitHub.

As variáveis mais importantes são:

- `POSTGRES_PASSWORD`: senha do banco local;
- `JWT_KEY`: chave usada para assinar tokens;
- `ADMIN_EMAIL` e `ADMIN_PASSWORD`: credenciais do administrador inicial;
- `DDL_AUTO=update`: mantém o esquema e os dados entre reinicializações.

## 4. Subir a API e o PostgreSQL

Valide o Compose sem imprimir os segredos:

```bash
docker compose config --quiet
```

Construa e inicie os containers:

```bash
docker compose up -d --build --wait
docker compose ps
```

Veja os logs da API:

```bash
docker compose logs -f app
```

O startup terminou corretamente quando aparecer `Started QualityApplication`. Como as rotas estão protegidas, uma resposta `401 Unauthorized` também comprova que o servidor está atendendo:

```bash
curl -i http://localhost:8080/
```

Confira o PostgreSQL:

```bash
docker compose exec db sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Para reconstruir depois de alterar o código:

```bash
docker compose up -d --build
```

Para parar sem apagar o banco:

```bash
docker compose down
```

> **Atenção:** `docker compose down -v` remove o volume `postgres_data` e apaga os dados locais.

## 5. Publicar no GitHub

Instale Git e GitHub CLI:

```bash
sudo dnf install git gh
gh auth login
```

No fluxo do `gh auth login`, escolha `GitHub.com`, `HTTPS` e autenticação pelo navegador.

Configure sua identidade do Git, substituindo os valores:

```bash
git config --global user.name "SEU NOME"
git config --global user.email "SEU EMAIL"
```

Antes do primeiro commit, confirme que `.env` e as configurações locais do IntelliJ estão ignorados:

```bash
git check-ignore -v .env .idea/workspace.xml
```

Inicialize e publique o repositório público:

```bash
git init -b main
git add .
git status
git commit -m "chore: configure Docker environment"
gh repo create Quality --public --source=. --remote=origin --push
```

Depois do push, esta verificação não deve mostrar nenhum arquivo:

```bash
git ls-files .env .idea/workspace.xml
```

## Arquivos Docker

- `Dockerfile`: compila a API com Maven/Java 21 e cria uma imagem de execução menor;
- `compose.yaml`: sobe a API e o PostgreSQL, aguarda o banco ficar saudável e mantém os dados em volume;
- `.env.example`: modelo seguro das variáveis necessárias;
- `.dockerignore`: impede o envio de arquivos locais e segredos ao contexto de build.
