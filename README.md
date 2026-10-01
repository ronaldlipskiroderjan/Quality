# Quality

<p align="center">
  <strong>Auditorias de qualidade organizadas do planejamento à resolução.</strong>
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img alt="React" src="https://img.shields.io/badge/React-19-149ECA?style=for-the-badge&logo=react&logoColor=white">
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-5.9-3178C6?style=for-the-badge&logo=typescript&logoColor=white">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-18-4169E1?style=for-the-badge&logo=postgresql&logoColor=white">
</p>

## Sobre a aplicação

O **Quality** é uma plataforma web para planejar, executar e acompanhar auditorias de garantia da qualidade. Em um único ambiente, a equipe organiza planos, documentos, artefatos, checklists, não conformidades, prazos, resoluções e escalonamentos.

A aplicação preserva o histórico da auditoria por meio de versões do checklist e centraliza a comunicação entre auditores, equipe de resolução e superiores. Cada participante visualiza as informações e ações relacionadas ao seu papel no processo.

## Como funciona

1. **Criação do plano** — o auditor registra o objetivo, a visão geral e a versão do Plano de Garantia da Qualidade.
2. **Organização da equipe** — auditores, equipe de resolução e superiores N1 e N2 são associados ao plano de acordo com suas responsabilidades.
3. **Preparação da auditoria** — documentos de referência e documentos auditados são adicionados, formando a base para os artefatos e auditorias.
4. **Construção do checklist** — cada auditoria possui um checklist editável, organizado em linhas e com histórico de versões.
5. **Execução** — os itens são classificados como Conforme, Não Conforme ou Não Aplicável.
6. **Tratamento de não conformidades** — itens não conformes são encaminhados à equipe de resolução com responsável, classificação, ação corretiva e prazo.
7. **Acompanhamento** — resoluções, notificações e vencimentos ficam visíveis na plataforma. Quando necessário, a não conformidade é escalonada aos superiores N1 e N2.
8. **Conclusão** — o auditor valida a resolução e acompanha a evolução da qualidade sem perder o histórico das versões anteriores.

## Principais recursos

- dashboard com planos, calendário de auditorias, prioridades e notificações;
- criação e edição de Planos de Garantia da Qualidade;
- imagem de capa do plano e imagem de perfil do usuário;
- gestão de auditores, equipe de resolução e superiores;
- documentos separados entre referências e materiais auditados;
- visualização de arquivos PDF e imagens na própria plataforma;
- artefatos vinculados aos documentos auditados;
- auditorias com checklists editáveis em formato de tabela;
- salvamento automático e histórico de versões do checklist;
- registro e acompanhamento de não conformidades;
- classificação de não conformidades com prazos configuráveis;
- cálculo de prazos considerando dias úteis e feriados;
- notificações internas para auditores, responsáveis e superiores;
- escalonamento automático para os níveis N1 e N2;
- histórico de alterações realizadas no plano.

## Perfis da plataforma

| Perfil | Atuação |
|---|---|
| **Auditor e responsável pela qualidade** | Organiza o plano, os documentos e as auditorias, executa checklists, acompanha não conformidades e valida resoluções. |
| **Equipe de resolução** | Recebe as não conformidades encaminhadas ao time, assume responsabilidades e informa as ações realizadas. |
| **Superior N1** | Acompanha escalonamentos de primeiro nível, alerta a equipe e pode redefinir o prazo de resolução. |
| **Superior N2** | Atua no segundo nível de escalonamento quando a pendência permanece sem resolução. |
| **Administrador do sistema** | Administra a plataforma e seus usuários, sem receber automaticamente acesso aos planos. |

## Arquitetura

~~~text
┌─────────────────────────────┐
│  React + TypeScript + Vite  │
│        Interface web        │
└──────────────┬──────────────┘
               │ API REST + JWT
┌──────────────▼──────────────┐
│     Java + Spring Boot      │
│ Regras, segurança e prazos  │
└──────────────┬──────────────┘
               │ JPA / Hibernate
┌──────────────▼──────────────┐
│         PostgreSQL          │
│ Dados, histórico e versões  │
└─────────────────────────────┘
~~~

O frontend consome a API REST e apresenta experiências diferentes conforme o papel do usuário. O backend concentra autenticação, permissões contextuais, regras da auditoria, versionamento e processamento dos prazos. O PostgreSQL armazena os dados da plataforma, enquanto o Flyway mantém a evolução da estrutura do banco.

## Tecnologias

### Backend

- **Java 21**;
- **Spring Boot 4.1.1**;
- **Spring Web MVC** para a API REST;
- **Spring Security** e **JWT** para autenticação e autorização;
- **Spring Data JPA** e **Hibernate** para persistência;
- **Bean Validation** para validação dos dados;
- **Flyway** para evolução do banco;
- **Maven** para dependências e build.

### Frontend

- **React 19**;
- **TypeScript 5.9**;
- **React Router 7**;
- **Vite 7**;
- CSS responsivo com identidade visual própria.

### Dados, testes e infraestrutura

- **PostgreSQL 18** como banco principal;
- **H2** para testes isolados;
- **JUnit 5**, **Mockito**, **MockMvc** e **Testcontainers** no backend;
- **Vitest** no frontend;
- **Docker** e **Docker Compose** para conteinerização do ambiente.

## Estrutura do projeto

~~~text
Quality/
├── src/main/java/       # API, domínio, serviços e persistência
├── src/main/resources/  # configurações e migrações do banco
├── src/test/            # testes automatizados do backend
├── frontend/            # aplicação React
├── docs/                # materiais do projeto
├── compose.yaml         # serviços em contêineres
├── Dockerfile           # imagem da API
└── pom.xml              # configuração Maven
~~~

---

<p align="center">
  <strong>Quality</strong> — clareza, rastreabilidade e colaboração em auditorias de qualidade.
</p>
