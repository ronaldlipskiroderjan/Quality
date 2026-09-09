# Escopo do Projeto — Plataforma de Auditoria e Garantia da Qualidade

> Documento de contexto funcional e de integração para os agentes de front-end e back-end.
>
> Status: escopo inicial do produto (MVP e evolução)
>
> Atualizado em: 09/09/2026

## 1. Objetivo deste documento

Este documento descreve o produto que esta API deverá sustentar, o fluxo esperado pelo usuário, as principais regras de negócio, as telas necessárias e o estado atual da integração. Ele deve ser usado pelo agente de front-end para compreender o domínio antes de definir rotas, componentes, estados e contratos de API.

Os arquivos de exemplo fornecidos são referências de estrutura e conteúdo. Eles não constituem instruções técnicas e não substituem as regras deste escopo:

- `Template_Plano_de_Garantia_da_Qualidade.doc`;
- `Checklist de Processo e Produto_Exemplo.pdf`;
- `Exemplo_Comunicacao_NC.pdf`.

Em caso de divergência, prevalecem, nesta ordem: decisões expressas do responsável pelo produto, este escopo, contratos publicados pela API e documentos de exemplo.

## 2. Visão do produto

A plataforma automatiza o planejamento e a execução de auditorias de garantia da qualidade. Um usuário autenticado cria um Plano de Garantia da Qualidade, inclui participantes, registra documentos de referência e documentos que serão avaliados, configura artefatos e checklists, executa avaliações e acompanha não conformidades até sua resolução.

A aplicação também deverá usar inteligência artificial para:

- sugerir itens de checklist com base somente nos documentos de referência selecionados pelo usuário;
- auxiliar na redação da comunicação de uma não conformidade;
- manter o usuário no controle, permitindo revisar, editar, aceitar ou descartar toda sugestão antes de salvar ou enviar.

O sistema deverá controlar prazos de resolução de não conformidades. Quando um prazo vencer, o auditor será notificado e poderá escalonar manualmente para o responsável de nível 1 definido no plano. Se o novo prazo também vencer, o auditor será novamente notificado e poderá escalonar para o responsável de nível 2.

## 3. Modelo mental e hierarquia funcional

```text
Usuário
└── Plano de Garantia da Qualidade
    ├── participantes e papéis no plano
    ├── responsáveis pelos escalonamentos N1 e N2
    ├── documentos de referência (templates, normas e diretrizes)
    ├── documentos a serem auditados
    │   └── artefatos avaliáveis
    │       └── avaliação/auditoria
    │           └── checklist
    │               └── itens/perguntas
    │                   ├── Conforme
    │                   ├── Não conforme → não conformidade
    │                   └── N/A (não aplicável)
    ├── notificações e histórico de escalonamentos
    └── indicadores e registros de qualidade
```

### 3.1 Glossário

| Termo | Definição no produto |
|---|---|
| Plano de Garantia da Qualidade | Unidade principal de trabalho. Reúne objetivo, visão geral, equipe, referências, objetos de avaliação, regras de NC e escalonamento. |
| Documento de referência | Template, norma, padrão ou diretriz usado como fonte para preparar a auditoria e gerar sugestões com IA. Não é necessariamente auditado. |
| Documento auditado | Arquivo do projeto submetido à avaliação de qualidade. |
| Artefato | Unidade avaliável associada a um documento auditado, com versão, data prevista e auditor. |
| Checklist | Instrumento de avaliação associado ao artefato/auditoria e composto por perguntas ordenadas. |
| Item do checklist | Pergunta ou critério que recebe resultado Conforme, Não conforme ou N/A. |
| Não conformidade (NC/NCF) | Desvio originado por um item respondido como Não conforme e acompanhado até validação da correção. |
| Escalonamento | Encaminhamento de uma NC vencida ao responsável N1 ou N2, sempre registrado no histórico. |
| Aderência | Indicador percentual consolidado da avaliação. A fórmula definitiva ainda deve ser validada com o responsável pelo produto. |

## 4. Perfis e responsabilidades

Os papéis globais já previstos na API são `ROLE_USER` e `ROLE_ADMIN`. `ROLE_ADMIN` significa exclusivamente **administrador do sistema**: essa role não representa administrador de grupo, proprietário de plano nem responsável pela qualidade. O produto também exige papéis contextuais por plano, que ainda deverão ser implementados no back-end e mantidos separados das roles globais.

| Papel | Responsabilidades esperadas |
|---|---|
| Administrador do sistema (`ROLE_ADMIN`) | Administrar usuários e configurações globais da plataforma. Essa role, isoladamente, não concede propriedade, administração ou participação em nenhum Plano de Garantia da Qualidade. |
| Responsável/Proprietário do plano | Criar e editar o plano, gerenciar participantes, definir responsáveis N1/N2 e concluir ou reabrir o plano. |
| Responsável pela qualidade (QA) | Coordenar as avaliações e o processo de garantia da qualidade. Pode também atuar como auditor. |
| Auditor | Preparar checklists, executar auditorias, registrar resultados, emitir NCs, validar correções e acionar escalonamentos. |
| Participante do plano | Consultar o plano conforme as permissões recebidas e colaborar nas áreas autorizadas. |
| Responsável pela resolução | Receber a NC, consultar seu conteúdo, informar ação/correção e submetê-la para validação do auditor. |
| Responsável N1/N2 | Receber escalonamentos de NCs não resolvidas nos respectivos prazos. |

As autorizações globais e as autorizações de plano são independentes:

- `ROLE_ADMIN` autoriza somente funções administrativas do sistema;
- `ROLE_USER` representa o acesso comum à plataforma;
- proprietário, responsável por QA, auditor, participante e responsáveis por resolução ou escalonamento são vínculos contextuais com um plano específico;
- um administrador do sistema somente terá funções dentro de um plano se também possuir o vínculo contextual correspondente.

O front-end deverá receber da API as permissões efetivas do usuário para cada plano. Não deverá inferir autorização apenas a partir da existência de um botão ou da role global do usuário.

## 5. Escopo funcional

### 5.1 Autenticação e conta

- Cadastro com nome, e-mail e senha.
- Login com e-mail e senha, retornando token JWT e expiração.
- Renovação do token enquanto a sessão for válida.
- Consulta e edição do perfil do usuário autenticado.
- Alteração de senha, validando senha atual, nova senha e confirmação.
- Inclusão e visualização de foto de perfil.
- Encerramento seguro da sessão no cliente.
- Área administrativa para listagem de usuários.

### 5.2 Plano de Garantia da Qualidade

O usuário deverá poder criar, consultar, editar e concluir um plano. A estrutura funcional deve contemplar:

- nome do projeto;
- versão do plano;
- autor/responsável por GQA;
- objetivo;
- visão geral;
- responsável pelo projeto;
- representante/responsável pela qualidade;
- participantes e respectivos papéis;
- documentação, padrões e diretrizes;
- itens/documentos a serem avaliados e seus locais de armazenamento;
- plano de avaliações, com artefato, data e auditor;
- local dos registros de qualidade;
- definição das classes de não conformidade e respectivos prazos sugeridos;
- responsável pelo escalonamento de nível 1;
- responsável pelo escalonamento de nível 2;
- prazo de resolução após cada escalonamento;
- status, datas de criação, atualização e conclusão.

O fluxo recomendado para criação é um formulário em etapas, com salvamento de rascunho:

1. identificação, objetivo e visão geral;
2. responsáveis e participantes;
3. documentos de referência;
4. documentos e artefatos a avaliar;
5. regras de NC e prazos;
6. escalonamentos N1/N2;
7. revisão e ativação.

### 5.3 Colaboração no plano

- O proprietário poderá convidar usuários cadastrados por e-mail e atribuir um papel no plano.
- Convites deverão ter estados como pendente, aceito, recusado, expirado e cancelado.
- A remoção de participante não deverá apagar sua autoria no histórico de auditoria.
- O back-end deverá validar participação e permissão em todas as operações do plano.
- A definição de responsáveis N1/N2 poderá apontar para participantes elegíveis do próprio plano.
- Deve ser decidido antes da implementação se responsáveis por resolução podem ser contatos externos sem conta. Até essa decisão, o front-end deve assumir usuários cadastrados.

### 5.4 Gestão de documentos

Existem duas classificações funcionais, que devem ser visualmente separadas:

- **Referência/template:** fonte para preparação da avaliação e geração de checklist com IA.
- **Documento auditado:** conteúdo que será avaliado e ao qual poderá ser associado um artefato.

Para cada documento devem existir, no mínimo:

- identificador;
- plano ao qual pertence;
- nome de exibição;
- nome original do arquivo;
- versão;
- tipo MIME;
- tamanho;
- classificação;
- autor do envio;
- datas de envio e atualização;
- possibilidade de visualizar metadados, baixar e excluir conforme permissão.

A configuração atual limita uploads a 10 MB por arquivo/requisição. O front-end deve validar o limite antes do envio e apresentar progresso, sucesso e falha. Tipos de arquivo permitidos e política de antivírus ainda precisam ser definidos no back-end.

### 5.5 Artefatos e plano de avaliações

- Um documento auditado poderá originar um ou mais artefatos/versionamentos avaliáveis; a cardinalidade final deve ser confirmada no domínio.
- Cada artefato terá nome, versão, auditor e data planejada da avaliação.
- O artefato deverá indicar se possui checklist e qual o estado da avaliação.
- O usuário deverá poder acessar o artefato, o arquivo associado, seu checklist e o histórico de avaliações.
- A troca de auditor deverá manter histórico.

Estados sugeridos para uma avaliação: `PLANEJADA`, `EM_PREPARACAO`, `EM_ANDAMENTO`, `PAUSADA`, `CONCLUIDA` e `CANCELADA`.

### 5.6 Construtor de checklist

O sistema deverá entregar um ambiente de autoria de checklist com:

- título, descrição, versão e artefato associado;
- criação manual de itens;
- edição, duplicação, exclusão e reordenação de itens;
- importação controlada das sugestões geradas por IA;
- salvamento como rascunho;
- publicação/bloqueio da versão usada em uma auditoria;
- indicação da origem de cada item: manual ou IA;
- possibilidade de criar nova versão sem alterar auditorias já executadas.

Cada item deverá conter, no mínimo:

- número/ordem;
- descrição/pergunta;
- resultado inicialmente não respondido;
- resultado final: `CONFORME`, `NAO_CONFORME` ou `NAO_APLICAVEL`;
- observação do auditor;
- dados de NC exibidos condicionalmente quando o resultado for `NAO_CONFORME`.

### 5.7 Execução da auditoria

O front-end deverá oferecer uma tela focada na execução, adequada a listas extensas:

- cabeçalho com projeto, artefato, versão, auditor, data e duração;
- progresso total e quantidade por resultado;
- navegação entre itens e filtro por situação;
- salvamento parcial das respostas;
- confirmação antes da conclusão da auditoria;
- resumo com total de NCs, itens N/A e aderência;
- faixas de aderência apresentadas no exemplo: 0–60%, 61–90% e 91–100%;
- bloqueio ou versionamento das respostas após conclusão, preservando a trilha de auditoria.

A fórmula de aderência deve ser definida no back-end e retornada pronta para exibição. Uma proposta inicial é desconsiderar itens N/A do denominador, mas isso não deve ser codificado no cliente antes da validação do produto.

### 5.8 Registro de não conformidade

Ao selecionar `NAO_CONFORME`, deverão ser solicitados os campos mostrados no checklist de referência:

- data e hora da identificação, geradas pelo servidor;
- responsável pela resolução;
- classificação da NCF;
- ação corretiva indicada;
- prazo/data prevista de resolução;
- data e hora do escalonamento, quando houver;
- data e hora de conclusão, quando validada;
- status da NC.

Também deverão ser mantidos o item/pergunta de origem, o artefato, o plano, o auditor, observações e o histórico de eventos.

O exemplo apresenta as seguintes combinações e prazos, que devem servir como valores sugeridos — não como regra imutável — até validação do produto:

| Prioridade | Simples | Complexa | Severa | Extrema |
|---|---:|---:|---:|---:|
| Baixa | 4 dias | 5 dias | 6 dias | 7 dias |
| Média | 3 dias | 4 dias | 5 dias | 6 dias |
| Alta | 2 dias | 3 dias | 4 dias | 5 dias |
| Urgente | 1 dia | 2 dias | 3 dias | 4 dias |

`ADVERTENCIA`/`NAO_SE_APLICA` aparece no documento de exemplo, mas sua semântica em relação às três respostas do item ainda precisa ser confirmada.

Estados sugeridos para a NC:

```text
RASCUNHO
  → ENVIADA
  → EM_TRATAMENTO
  → RESOLUCAO_INFORMADA
  → CONCLUIDA (após validação do auditor)

ENVIADA/EM_TRATAMENTO vencida
  → VENCIDA
  → ESCALONADA_N1
  → ESCALONADA_N2
```

Reabrir uma correção rejeitada pelo auditor deve preservar todo o histórico e gerar novo prazo quando aplicável.

### 5.9 Comunicação da não conformidade

Antes do envio, a tela de comunicação deverá apresentar e permitir revisar:

- projeto;
- responsável pela resolução e destinatário;
- data da primeira solicitação;
- prazo de resolução;
- número/nível de escalonamento;
- responsável por QA/auditor;
- descrição da não conformidade;
- classificação;
- ação corretiva indicada;
- observações;
- histórico de escalonamentos, superior responsável e prazos;
- assunto e corpo do e-mail.

A IA poderá gerar ou melhorar assunto e corpo com base nesses dados, mas o envio dependerá sempre de confirmação explícita do auditor. O sistema deverá registrar versão final enviada, destinatários, data/hora, resultado do provedor e vínculo com a NC.

### 5.10 Prazos, notificações e escalonamentos

- O cronômetro começa quando a primeira comunicação é enviada ao responsável pela resolução, e não quando o item é apenas marcado como não conforme.
- O servidor é a fonte oficial do tempo; o front-end exibe uma contagem regressiva derivada de `prazoEm` em formato ISO 8601.
- O auditor define o prazo, podendo receber uma sugestão conforme a classificação.
- Ao vencer o prazo, o sistema marca a NC como vencida e notifica o auditor.
- O primeiro vencimento habilita o escalonamento manual para N1.
- Ao efetivar N1, o sistema registra autor/data/destinatário, envia a comunicação e inicia o prazo de N1.
- O vencimento de N1 gera nova notificação ao auditor e habilita o escalonamento manual para N2.
- Ao efetivar N2, o sistema registra e comunica o evento.
- Processamentos de prazo e envio devem ser idempotentes para impedir notificações duplicadas.
- Toda transição deve aparecer numa linha do tempo imutável.
- A interface deve diferenciar prazo normal, próximo do vencimento, vencido e concluído, sem depender somente de cor.

### 5.11 Assistência por IA

#### Geração de itens de checklist

1. O usuário seleciona explicitamente um ou mais documentos classificados como referência/template.
2. Define o artefato e, opcionalmente, objetivo, quantidade ou foco dos itens.
3. A API processa a solicitação de forma assíncrona e devolve o status do trabalho.
4. A interface mostra cada sugestão, sua justificativa e, quando possível, a referência ao documento/trecho de origem.
5. O auditor aceita, edita ou rejeita individualmente as sugestões.
6. Somente itens aprovados passam a integrar o checklist.

#### Redação de comunicação

- A IA recebe dados estruturados da NC e o contexto autorizado.
- O resultado é um rascunho editável; nunca deve ser enviado automaticamente.
- A interface deve indicar claramente conteúdo gerado por IA e estados de processamento, falha, cancelamento e nova tentativa.
- A API deve aplicar limites, proteção de dados, registro de auditoria e política de retenção. Credenciais do provedor jamais serão expostas ao navegador.

## 6. Telas e rotas sugeridas para o front-end

| Área | Tela/rota sugerida | Objetivo |
|---|---|---|
| Autenticação | `/login`, `/cadastro` | Entrar e criar conta. |
| Início | `/planos` | Listar planos acessíveis, filtros, status e atalhos. |
| Plano | `/planos/novo`, `/planos/:id` | Criar por etapas e consultar o espaço de trabalho. |
| Visão geral | `/planos/:id/visao-geral` | Objetivo, visão geral, responsáveis e indicadores. |
| Equipe | `/planos/:id/equipe` | Participantes, convites, papéis e escalonadores. |
| Documentos | `/planos/:id/documentos` | Abas de referências e documentos auditados. |
| Artefatos | `/planos/:id/artefatos` | Planejar avaliações e atribuir auditor. |
| Checklist | `/artefatos/:id/checklist` | Montar, versionar e publicar o checklist. |
| Sugestões IA | `/checklists/:id/sugestoes` | Revisar sugestões antes de incorporá-las. |
| Auditoria | `/auditorias/:id/executar` | Responder itens e acompanhar progresso. |
| Resultado | `/auditorias/:id/resultado` | Exibir aderência, totais e NCs. |
| Não conformidades | `/planos/:id/nao-conformidades` | Listar, filtrar e priorizar NCs. |
| Detalhe da NC | `/nao-conformidades/:id` | Linha do tempo, prazo, correção, comunicação e escalonamento. |
| Notificações | `/notificacoes` | Central de prazos vencidos e eventos relevantes. |
| Perfil | `/perfil` | Dados pessoais, imagem e senha. |
| Administração | `/admin/usuarios` | Gestão global de usuários. |

Dentro do plano, recomenda-se uma navegação persistente por abas: Visão geral, Equipe, Referências, Documentos auditados, Artefatos, Checklists, Auditorias, Não conformidades e Configurações.

## 7. Requisitos de experiência e interface

- Interfaces responsivas, com prioridade para desktop durante autoria e execução de checklists, sem impedir uso em tablet.
- Acessibilidade compatível com WCAG 2.2 nível AA como meta.
- Estados de carregamento, vazio, erro, indisponibilidade, sem permissão e sucesso em todas as telas.
- Mensagens de erro orientadas à ação; preservar dados digitados quando houver falha recuperável.
- Confirmação para excluir, concluir auditoria, concluir plano, enviar e-mail e escalonar.
- Datas exibidas no fuso do usuário, mas recebidas e enviadas em ISO 8601 com offset/UTC.
- Não calcular autorização, aderência, estado de prazo ou transições de workflow exclusivamente no cliente.
- Usar textos completos junto a ícones e cores, especialmente para resultado do checklist e urgência.
- Autosave com indicação de estado para formulários longos e execução da auditoria, depois que houver suporte transacional na API.
- Paginação e filtros no servidor para listas potencialmente grandes.
- Componentes reutilizáveis para status, usuário, upload, seletor de resultado, prazo regressivo, histórico e confirmação.

## 8. Contrato de API existente (estado atual)

Esta seção descreve o contrato após o primeiro incremento de estabilização concluído em 09/09/2026. Todas as rotas, exceto cadastro e login, exigem `Authorization: Bearer <token>`.

As respostas de erro tratadas pela aplicação seguem `application/problem+json`, com `title`, `status`, `detail`, `instance` e `timestamp`. Erros de validação também retornam a propriedade `campos`.

### 8.1 Autenticação

| Método e rota | Entrada/saída |
|---|---|
| `POST /v1/auth/register` | JSON `{ nome, email, senha }`; retorna `201` com `{ id, nome, email, roles }`. |
| `POST /v1/auth/login` | JSON `{ email, senha }`; retorna `200` com `{ accessToken, tokenType, expiresInMs }`. |
| `POST /v1/auth/refresh` | Sem corpo; retorna `200` com um novo token. Exige autenticação válida. |

O login devolve a mesma resposta `401` para e-mail inexistente ou senha incorreta, evitando expor quais contas estão cadastradas.

### 8.2 Usuários

| Método e rota | Entrada/saída |
|---|---|
| `GET /v1/usuarios/me` | Retorna `{ id, nome, email, roles }` do usuário autenticado. |
| `PUT /v1/usuarios/me` | JSON `{ nome, email }`; substitui os dados editáveis e retorna o perfil atualizado. |
| `PATCH /v1/usuarios/me/senha` | JSON `{ senhaAntiga, novaSenha, confirmacaoSenha }`; retorna `204`. |
| `PUT /v1/usuarios/me/imagem` | Multipart no campo `imagem`; aceita somente MIME `image/*` e retorna `204`. |
| `GET /v1/usuarios/me/imagem` | Retorna o binário da imagem ou `404` quando ausente. |
| `GET /v1/usuarios?page=0&size=15` | Retorna página de usuários; exige `ROLE_ADMIN`. |

`ROLE_ADMIN` continua sendo exclusivamente uma autoridade global de administração do sistema. Ela não concede acesso automático a planos.

### 8.3 Planos

Payload de criação e atualização:

```json
{
  "nomeProjeto": "string",
  "versao": "string",
  "objetivo": "string",
  "visaoGeral": "string"
}
```

| Método e rota | Comportamento |
|---|---|
| `POST /v1/planos` | Cria o plano, vincula o usuário autenticado e retorna `201`, `Location` e o recurso detalhado. |
| `GET /v1/planos` | Lista somente os planos vinculados ao usuário autenticado. |
| `GET /v1/planos/{id}` | Retorna ID, projeto, versão, objetivo, visão geral, status e criação. |
| `PUT /v1/planos/{id}` | Atualiza os campos editáveis e retorna o recurso atualizado. |
| `PATCH /v1/planos/{id}/conclusao` | Marca o plano como concluído e retorna `204`. |
| `DELETE /v1/planos/{id}` | Remove vínculos e exclui o plano; retorna `204`. |

Busca, atualização, conclusão e exclusão utilizam o vínculo entre usuário e plano. Um plano sem vínculo é respondido como `404`, inclusive para um administrador global.

### 8.4 Documentos

Todos os endpoints de documentos são subordinados ao plano:

```text
/v1/planos/{planoId}/documentos
```

O upload usa `multipart/form-data`:

| Campo | Tipo |
|---|---|
| `nome` | texto, obrigatório |
| `versao` | texto, obrigatório |
| `arquivo` | arquivo não vazio |
| `classificacao` | `REFERENCIA` ou `AUDITADO` |

| Método e rota | Comportamento |
|---|---|
| `POST /v1/planos/{planoId}/documentos` | Faz upload e retorna `201`, `Location` e metadados completos. |
| `GET /v1/planos/{planoId}/documentos` | Lista documentos; aceita filtro opcional `?classificacao=REFERENCIA\|AUDITADO`. |
| `GET /v1/planos/{planoId}/documentos/{documentoId}` | Retorna metadados do documento. |
| `GET /v1/planos/{planoId}/documentos/{documentoId}/arquivo` | Retorna o arquivo para download. |
| `DELETE /v1/planos/{planoId}/documentos/{documentoId}` | Exclui o documento e retorna `204`. |

O limite configurado permanece em 10 MB. Os arquivos são armazenados em `BYTEA` no PostgreSQL nesta fase.

### 8.5 Recursos modelados sem endpoints

As entidades e os repositórios abaixo existem parcialmente, mas ainda não possuem contratos HTTP nem serviços funcionais completos:

- artefatos;
- auditorias;
- checklists;
- itens de checklist.

Ainda não existem implementações para:

- participantes, convites e papéis contextuais por plano;
- responsáveis N1/N2;
- não conformidade como agregado com workflow próprio;
- comentários e evidências de resolução;
- notificações, cronômetros e tarefas agendadas;
- histórico de eventos e escalonamentos;
- e-mail;
- geração por IA;
- relatórios e indicadores consolidados.

## 9. Situação técnica e próximos bloqueadores

O primeiro incremento corrigiu rotas conflitantes, verificações invertidas de documentos, atualização de perfil/senha, respostas detalhadas de plano, tratamento HTTP de exceções, validações básicas e controle de acesso por vínculo com o plano.

A aplicação compila e a suíte executa 35 testes sem falhas, incluindo o carregamento completo do contexto com H2 em modo de teste. PostgreSQL continua sendo o banco da aplicação.

Os próximos pontos estruturais são:

1. substituir o vínculo simples `usuario_planos` por participantes com papéis contextuais e permissões explícitas;
2. definir quem pode editar, concluir e excluir o plano quando houver vários participantes;
3. redesenhar os relacionamentos entre documento, artefato, auditoria e checklist antes de publicar esses endpoints;
4. criar enums de status específicos para plano, avaliação, checklist e NC;
5. adicionar migrações versionadas para PostgreSQL e deixar de usar `ddl-auto=update` fora do desenvolvimento;
6. configurar CORS para as origens reais do front-end;
7. padronizar também as respostas `401/403` geradas diretamente pelos filtros do Spring Security;
8. decidir a estratégia definitiva de arquivos, pois `BYTEA` é adequado ao protótipo, mas pode não atender ao volume futuro;
9. adicionar testes de integração com PostgreSQL/Testcontainers;
10. implementar os agregados de artefato, checklist, auditoria e não conformidade antes de iniciar IA, e-mail e scheduler.

## 10. Direção esperada para o contrato alvo

O contrato final deve seguir estas convenções:

- recursos aninhados ou filtrados pelo plano autenticado, sem exigir `usuarioId` fornecido pelo cliente para listar “meus planos”;
- respostas de criação com `201`, corpo do recurso e/ou cabeçalho `Location`;
- `200` para atualizações com representação ou `204` sem corpo; evitar `202` quando não houver processamento assíncrono real;
- erros padronizados, idealmente `application/problem+json`, com código estável, mensagem, campos inválidos, timestamp e correlation ID;
- paginação, ordenação e filtros consistentes;
- controle otimista de concorrência em checklists, auditorias e NCs;
- enums distintos por agregado, sem reutilizar um único `Status` genérico;
- timestamps gerados pelo servidor e serializados com fuso explícito;
- endpoints assíncronos de IA retornando um identificador de trabalho e seu estado;
- endpoints de comandos para transições relevantes, evitando permitir mudanças arbitrárias de status;
- versão imutável do checklist utilizada em auditoria concluída;
- idempotency key para envio de comunicação e escalonamento.

Exemplos conceituais de recursos futuros, ainda não implementados:

```text
GET  /v1/planos/me
POST /v1/planos/{planoId}/participantes
GET  /v1/planos/{planoId}/documentos?classificacao=REFERENCIA
POST /v1/planos/{planoId}/artefatos
POST /v1/artefatos/{artefatoId}/checklists
POST /v1/checklists/{checklistId}/itens
POST /v1/checklists/{checklistId}/sugestoes-ia
POST /v1/auditorias/{auditoriaId}/respostas
POST /v1/nao-conformidades/{id}/comunicacoes
POST /v1/nao-conformidades/{id}/escalonamentos
POST /v1/nao-conformidades/{id}/resolucoes
POST /v1/nao-conformidades/{id}/validacoes
GET  /v1/notificacoes
```

Os nomes acima servem para alinhamento e não devem ser tratados como endpoints disponíveis até sua publicação no OpenAPI da aplicação.

## 11. Requisitos não funcionais e qualidade

### Segurança e privacidade

- Autorização por plano e por ação aplicada no servidor.
- Senhas protegidas por hash forte; tokens e segredos nunca persistidos no front-end além do estritamente necessário.
- Preferir cookie `HttpOnly`, `Secure` e `SameSite` para sessão quando a arquitetura for revisada; se JWT continuar no cliente, documentar estratégia segura de armazenamento e renovação.
- Validar tipo, extensão, tamanho e conteúdo de uploads; prever verificação antimalware.
- Não enviar documentos ou dados à IA sem base legal, aviso e escopo autorizado.
- Logs não devem expor senha, token, arquivo, corpo completo de e-mail ou conteúdo sensível.

### Auditabilidade e confiabilidade

- Registrar ator, data/hora, ação, estado anterior e novo estado para mudanças críticas.
- Não apagar histórico de auditorias, comunicações ou NCs concluídas; usar retenção/arquivamento coerente com a política do produto.
- Scheduler de prazos resiliente a reinicializações e com execução idempotente.
- E-mails com rastreabilidade de tentativa, sucesso e erro, sem marcar como enviados antes da confirmação do provedor.

### Engenharia e testes

- Java 21, Spring Boot e PostgreSQL são a base técnica atual.
- Aplicar SOLID, Clean Code, separação por responsabilidades e regras de domínio fora de controllers.
- Usar migrações versionadas de banco (Flyway ou Liquibase) em vez de depender de `ddl-auto=update` em produção.
- Testes unitários para regras de prazo, workflow, autorização e aderência.
- Testes de integração com PostgreSQL/Testcontainers para persistência e concorrência.
- Testes de controller/contrato para status HTTP, validação e segurança.
- Testes end-to-end dos fluxos críticos: criar plano, preparar checklist, executar auditoria, emitir NC, resolver e escalonar.
- Contrato OpenAPI como fonte de integração do front-end após estabilização.

## 12. Recorte recomendado do MVP

### Fase 1 — fundação utilizável

- estabilizar autenticação, usuários, erros, testes e autorização;
- CRUD detalhado do plano;
- participantes e papéis no plano;
- upload, listagem, metadados e download das duas categorias de documentos;
- modelagem consistente de artefato, auditoria, checklist e item;
- construtor manual de checklist;
- execução com Conforme, Não conforme e N/A;
- registro básico de NC e cálculo de aderência.

### Fase 2 — resolução e escalonamento

- comunicação por e-mail revisável;
- responsável e submissão de resolução;
- validação/reabertura pelo auditor;
- prazos persistentes e scheduler;
- notificações ao auditor;
- escalonamentos N1 e N2 com histórico.

### Fase 3 — inteligência artificial e evolução

- geração assistida de itens a partir das referências selecionadas;
- assistência de redação de e-mail;
- citações/rastreabilidade das sugestões;
- relatórios, métricas e refinamentos de experiência;
- políticas avançadas de armazenamento, retenção e integrações.

## 13. Critérios de aceite do fluxo principal

O MVP funcional estará demonstrável quando:

1. um usuário puder se cadastrar, autenticar e criar um plano;
2. o criador puder convidar participantes e definir auditor, responsável pela resolução e escalonadores N1/N2;
3. o plano separar claramente documentos de referência e documentos auditados;
4. um documento auditado puder ser associado a um artefato e a uma avaliação;
5. o auditor puder montar, ordenar e publicar um checklist;
6. cada item puder ser respondido com Conforme, Não conforme ou N/A;
7. uma resposta Não conforme exigir os dados mínimos da NC;
8. o auditor puder revisar e enviar a comunicação, iniciando o prazo no servidor;
9. o responsável puder informar a resolução e o auditor puder aprovar ou reabrir;
10. um prazo vencido gerar uma única notificação e habilitar N1; o vencimento posterior habilitar N2;
11. todos os eventos aparecerem em ordem cronológica no histórico;
12. o resultado da auditoria exibir totais e aderência calculados pela API;
13. um usuário sem permissão não conseguir consultar ou alterar recursos do plano, mesmo chamando a API diretamente;
14. os fluxos críticos possuírem testes automatizados e contrato OpenAPI consumível pelo front-end.

## 14. Decisões de produto ainda pendentes

- Fórmula oficial de aderência e tratamento exato de N/A.
- Semântica de advertência e sua relação com uma NC.
- Dias corridos ou úteis, calendário de feriados e horário limite dos prazos.
- Prazo após N1 e N2: valor global, por plano, por classificação ou informado a cada escalonamento.
- Participação de responsáveis externos sem conta.
- Cardinalidade entre documento, artefato, checklist e auditorias recorrentes.
- Tipos de arquivo permitidos, retenção, antivírus e armazenamento definitivo.
- Provedor de e-mail e política de remetente, cópia e resposta.
- Provedor/modelo de IA, limites de uso, política de dados e rastreabilidade exigida.
- Possibilidade de assinatura/aprovação formal do plano.
- Política para exclusão, arquivamento e reabertura de plano/auditoria/NC.
- Canais de notificação além da central interna e do e-mail.

Até que essas decisões sejam tomadas, o front-end deve manter componentes configuráveis e evitar embutir regras temporais ou transições de estado como constantes espalhadas pela aplicação.
