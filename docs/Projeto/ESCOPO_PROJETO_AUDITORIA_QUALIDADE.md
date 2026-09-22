# Escopo do Projeto — Plataforma de Auditoria e Garantia da Qualidade

> Documento de contexto funcional e de integração para os agentes de front-end e back-end.
>
> Status: escopo inicial do produto (MVP e evolução)
>
> Atualizado em: 19/09/2026

## 1. Objetivo deste documento

Este documento descreve o produto que esta API deverá sustentar, o fluxo esperado pelo usuário, as principais regras de negócio, as telas necessárias e o estado atual da integração. Ele deve ser usado pelo agente de front-end para compreender o domínio antes de definir rotas, componentes, estados e contratos de API.

Os arquivos de exemplo fornecidos são referências de estrutura e conteúdo. Eles não constituem instruções técnicas e não substituem as regras deste escopo:

- `Template_Plano_de_Garantia_da_Qualidade.doc`;
- `Checklist de Processo e Produto_Exemplo.pdf`;
- `Exemplo_Comunicacao_NC.pdf`.

Em caso de divergência, prevalecem, nesta ordem: decisões expressas do responsável pelo produto, este escopo, contratos publicados pela API e documentos de exemplo.

## 2. Visão do produto

A plataforma automatiza o planejamento e a execução de auditorias de garantia da qualidade. Um usuário autenticado cria um Plano de Garantia da Qualidade, inclui participantes, registra documentos de referência e documentos que serão avaliados, configura artefatos, prepara os checklists dentro de cada auditoria, executa avaliações e acompanha não conformidades até sua resolução.

A aplicação também deverá usar inteligência artificial para:

- sugerir itens de checklist com base somente nos documentos de referência selecionados pelo usuário;
- auxiliar futuramente na análise e descrição de uma não conformidade;
- manter o usuário no controle, permitindo revisar, editar, aceitar ou descartar toda sugestão antes de salvar ou enviar.

O sistema controla prazos de resolução de não conformidades. Quando um prazo vence, o auditor é notificado e pode escalonar manualmente para o superior N1 definido no plano. Se o novo prazo também vencer, o auditor pode escalonar para o superior N2.

## 3. Modelo mental e hierarquia funcional

```text
Usuário
└── Plano de Garantia da Qualidade
    ├── participantes e papéis no plano
    ├── equipe de resolução e superiores N1 e N2
    ├── documentos de referência (templates, normas e diretrizes)
    ├── documentos a serem auditados
    │   └── artefatos avaliáveis
    │       └── avaliação/auditoria
    │           └── checklists
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
| Checklist | Instrumento interno da auditoria, composto por perguntas ordenadas e versões de trabalho independentes. |
| Item do checklist | Pergunta ou critério que recebe resultado Conforme, Não conforme ou N/A. |
| Não conformidade (NC/NCF) | Desvio originado por um item respondido como Não conforme e acompanhado até validação da correção. |
| Escalonamento | Notificação de uma NC vencida ao superior N1 ou N2, sempre registrada no histórico. |
| Aderência | Indicador percentual consolidado da avaliação. A fórmula definitiva ainda deve ser validada com o responsável pelo produto. |

## 4. Perfis e responsabilidades

Os papéis globais da API são `ROLE_USER` e `ROLE_ADMIN`. `ROLE_ADMIN` identifica exclusivamente o administrador do sistema; não representa administração de grupo ou plano. Os papéis e permissões contextuais do plano são independentes.

| Papel | Responsabilidades esperadas |
|---|---|
| Administrador global | Administrar usuários e configurações globais. Não deve obter acesso automático ao conteúdo de todos os planos sem uma regra explícita de negócio. |
| Auditor e responsável pela qualidade | Criar e editar o plano, gerenciar equipe, documentos e artefatos, definir superiores N1/N2, editar e responder checklists, registrar resultados, emitir NCs, validar correções, acionar escalonamentos e concluir o plano. Quem cria um plano recebe este papel automaticamente. |
| Membro da equipe de resolução | Acessar as NCs encaminhadas à equipe, assumir uma NC sem responsável e submeter a correção para validação. |
| Superior N1/N2 | Consultar as abas do plano e receber as NCs escalonadas ao respectivo nível; não gerencia o plano nem executa a resolução. |

O front-end deverá receber permissões efetivas da API por plano. Não deverá inferir autorização apenas a partir da existência de um botão ou do papel global do usuário.

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

- O auditor e responsável de qualidade poderá adicionar auditores e membros da equipe de resolução por e-mail. Superiores N1/N2 são definidos na visão geral do plano.
- Convites deverão ter estados como pendente, aceito, recusado, expirado e cancelado.
- A remoção de participante não deverá apagar sua autoria no histórico de auditoria.
- O back-end deverá validar participação e permissão em todas as operações do plano.
- Cada plano possui no máximo um superior N1 e um superior N2, ambos usuários cadastrados.
- A aba de auditores exibe somente auditores e mantém um log com autor, alteração e horário.
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

- Um documento auditado poderá originar um ou mais artefatos avaliáveis, exibidos de forma agrupada por documento.
- Cada artefato terá nome, versão, auditor, data planejada da avaliação e um ou mais documentos de referência selecionados na criação.
- O artefato deverá indicar o estado da auditoria.
- Cada artefato possui exatamente uma auditoria e um checklist ativo, ambos criados automaticamente junto com o artefato.
- A troca de auditor deverá manter histórico.

Estados sugeridos para uma avaliação: `PLANEJADA`, `EM_PREPARACAO`, `EM_ANDAMENTO`, `PAUSADA`, `CONCLUIDA` e `CANCELADA`.

### 5.6 Checklist da auditoria

Ao criar o artefato, o sistema cria na mesma transação sua auditoria em andamento e o checklist versão `1.0`, já disponível para edição e resposta. Não existem comandos separados para criar ou abrir o checklist.

O checklist deverá oferecer:

- primeira linha criada vazia para preenchimento imediato;
- botão `Adicionar linha` abaixo da tabela;
- criação e edição manual das perguntas diretamente nas células;
- possibilidade de responder qualquer pergunta preenchida sem iniciar outra etapa;
- importação controlada das sugestões geradas por IA;
- indicação da origem de cada item: manual ou IA;
- salvamento automático ao sair da célula;
- uma versão atual editável, atualizada pelo salvamento automático;
- salvamento manual de versões independentes na linha do tempo;
- alternância entre a versão atual e qualquer versão salva sem exigir um novo salvamento;
- edição e salvamento automático dentro de qualquer versão enquanto a auditoria estiver em andamento.

Cada item deverá conter, no mínimo:

- número/ordem;
- descrição/pergunta;
- resultado inicialmente não respondido;
- resultado final: `CONFORME`, `NAO_CONFORME` ou `NAO_APLICAVEL`;
- observação do auditor apenas quando o resultado for `NAO_CONFORME`;
- dados de NC exibidos condicionalmente quando o resultado for `NAO_CONFORME`.

### 5.7 Execução da auditoria

O front-end deverá oferecer uma tela focada na execução, adequada a listas extensas:

- checklist exibido diretamente ao acessar a auditoria, sem uma etapa de abertura;
- cabeçalho com projeto, artefato, versão, auditor, data e duração;
- progresso total e quantidade por resultado;
- planilha compacta para listas extensas, sem cartões individuais por pergunta;
- edição direta nas células da planilha, com salvamento automático ao sair do campo;
- salvamento manual de pontos de controle na linha do tempo do checklist;
- seletor para consultar versões salvas na própria tabela e retornar à versão atual sem perder alterações;
- ação `Enviar para resolução` na própria linha não conforme, utilizando os dados já preenchidos sem abrir outro formulário;
- confirmação antes da conclusão da auditoria;
- resumo com total de NCs, itens N/A e aderência;
- faixas de aderência apresentadas no exemplo: 0–60%, 61–90% e 91–100%;
- bloqueio ou versionamento das respostas após conclusão, preservando a trilha de auditoria.

A versão atual funciona como base viva da execução e recebe todas as alterações salvas automaticamente. Cada versão manual começa como uma cópia independente das perguntas, resultados e dados de não conformidade daquele momento, sem substituir ou interromper a base atual. Enquanto a auditoria estiver em andamento, o auditor designado pode editar diretamente qualquer versão e cada alteração é salva na própria versão. A linha do tempo registra as versões com número sequencial, autor, data/hora e observação opcional.

A auditoria pode ser concluída quando todos os itens estiverem respondidos e todo item não conforme tiver sido enviado para resolução. NCs ainda não resolvidas impedem a conclusão, salvo quando todas estiverem escalonadas e um usuário definido como superior N1 ou N2 registrar uma autorização excepcional com justificativa. Essa autorização não conclui, cancela nem altera a NC; ela apenas libera a conclusão da auditoria, preservando a pendência e a trilha de decisão.

A fórmula de aderência deve ser definida no back-end e retornada pronta para exibição. Uma proposta inicial é desconsiderar itens N/A do denominador, mas isso não deve ser codificado no cliente antes da validação do produto.

### 5.8 Registro de não conformidade

Ao selecionar `NAO_CONFORME`, o auditor cria e envia a NC em uma única ação. Devem ser solicitados:

- data e hora da identificação, geradas pelo servidor;
- responsável pela resolução;
- classificação da NCF;
- ação corretiva indicada;
- classificação, que determina o prazo configurado no plano;
- data e hora do escalonamento, quando houver;
- data e hora de conclusão, quando validada;
- status da NC.

Também deverão ser mantidos o item/pergunta de origem, o artefato, o plano, o auditor, observações e o histórico de eventos.

Não existe prioridade separada. As classificações `SIMPLES`, `COMPLEXA`, `SEVERA` e `EXTREMA` podem ser habilitadas ou desabilitadas na visão geral, cada uma com seu prazo configurado em dias úteis e horas. O servidor armazena o total equivalente em horas, interpreta cada bloco de 24 horas como um dia útil e transforma o prazo em uma data/hora limite (`prazoEm`). Sábados, domingos, feriados nacionais e feriados adicionais cadastrados no plano não consomem dias do prazo.

`ADVERTENCIA`/`NAO_SE_APLICA` aparece no documento de exemplo, mas sua semântica em relação às três respostas do item ainda precisa ser confirmada.

Estados sugeridos para a NC:

```text
RASCUNHO
  → EM_TRATAMENTO (exibido como “Em correção”)
  → RESOLUCAO_INFORMADA
  → CONCLUIDA (após validação do auditor)

EM_TRATAMENTO vencida
  → ESCALONADA_N1 (automático)
  → ESCALONADA_N2 (automático após o prazo N1)
  → VENCIDA_N2 (se o prazo final também terminar)
```

Enquanto a NC estiver em `RASCUNHO`, a coluna de status permanece vazia na
interface. O estado existe apenas para permitir o preenchimento da linha antes
do envio.

Reabrir uma correção rejeitada pelo auditor deve preservar todo o histórico e gerar novo prazo quando aplicável.

### 5.9 Encaminhamento da não conformidade

No formulário único de criação e envio, a tela deverá apresentar:

- projeto;
- equipe de resolução e responsável individual opcional;
- data da primeira solicitação;
- prazo de resolução;
- número/nível de escalonamento;
- responsável por QA/auditor;
- descrição da não conformidade;
- classificação;
- ação corretiva indicada;
- observações;
- histórico de escalonamentos, superior responsável e prazos;
Ao confirmar, o sistema persiste a NC e a encaminha na mesma transação. Ele registra data/hora, total de destinatários e vínculo com a NC, calcula `prazoEm` e cria uma notificação interna para cada membro da equipe.

### 5.10 Prazos, notificações e escalonamentos

- O cronômetro começa no envio direto da NC à equipe de resolução, e não quando o item é apenas marcado como não conforme.
- O servidor é a fonte oficial do tempo; o front-end exibe uma contagem regressiva derivada de `prazoEm` em formato ISO 8601.
- O prazo é definido pela classificação configurada no plano; o auditor não informa horas ao emitir a NC.
- O calendário utiliza o fuso `America/Sao_Paulo`, ignora fins de semana e aplica automaticamente os feriados nacionais, inclusive a Sexta-feira da Paixão calculada anualmente.
- Feriados estaduais, municipais, pontos facultativos ou datas internas podem ser cadastrados por plano na visão geral.
- Ao vencer o prazo, o sistema marca a NC como vencida e notifica o auditor.
- O primeiro vencimento habilita o escalonamento manual para N1.
- Ao efetivar N1, o sistema registra autor, data e superior destinatário, gera a notificação e inicia o prazo de N1.
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

#### Análise assistida de não conformidades

- Uma evolução poderá sugerir descrições ou ações corretivas a partir do contexto autorizado.
- Toda sugestão deverá permanecer editável e depender de confirmação humana.

## 6. Telas e rotas sugeridas para o front-end

| Área | Tela/rota sugerida | Objetivo |
|---|---|---|
| Autenticação | `/login`, `/cadastro` | Entrar e criar conta. |
| Início | `/planos` | Listar planos acessíveis, filtros, status e atalhos. |
| Plano | `/planos/novo`, `/planos/:id` | Criar por etapas e consultar o espaço de trabalho. |
| Visão geral | `/planos/:id` | Síntese em formato de Plano de Garantia da Qualidade: identificação, objetivo, visão geral, nomes dos auditores e superiores, documentos de referência e auditados, artefatos por documento e classificações com prazos. As demais abas preservam a consulta e a operação detalhada. |
| Auditores | `/planos/:id/equipe` | Auditores com acesso ao plano e log cronológico de alterações. |
| Documentos | `/planos/:id/documentos` | Tabelas lado a lado de referências e documentos auditados, com visualização interna de PDF/imagem. |
| Artefatos | `/planos/:id/artefatos` | Planejar avaliações e atribuir auditor. |
| Sugestões IA | `/auditorias/:id/sugestoes` | Revisar sugestões antes de incorporá-las ao checklist da auditoria. |
| Auditoria | `/auditorias/:id` | Editar perguntas, responder itens, salvar versões e acompanhar o checklist criado automaticamente. |
| Resultado | `/auditorias/:id/resultado` | Exibir aderência, totais e NCs. |
| Equipe de resolução | `/planos/:id/nao-conformidades` | Exibir cada membro com suas NCs e a fila não atribuída para acompanhamento do auditor. |
| Detalhe da NC | `/nao-conformidades/:id` | Linha do tempo, prazo, correção, encaminhamento interno e escalonamento. |
| Notificações | `/notificacoes` | Central de prazos vencidos e eventos relevantes. |
| Perfil | `/perfil` | Dados pessoais, imagem e senha. |
| Administração | `/admin/usuarios` | Gestão global de usuários. |

Dentro do plano, a navegação persistente não possui uma seção separada de checklists: eles pertencem às auditorias. Auditores e superiores acessam essas abas. Ao abrir um plano, um membro da equipe de resolução recebe uma tela exclusiva com “Atribuídas a mim” e “Todas da equipe”, sem acesso aos demais dados do plano.

## 7. Requisitos de experiência e interface

- Interfaces responsivas, com prioridade para desktop durante autoria e execução de checklists, sem impedir uso em tablet.
- Acessibilidade compatível com WCAG 2.2 nível AA como meta.
- Estados de carregamento, vazio, erro, indisponibilidade, sem permissão e sucesso em todas as telas.
- Mensagens de erro orientadas à ação; preservar dados digitados quando houver falha recuperável.
- Confirmação para excluir, concluir auditoria, concluir plano e escalonar. Exclusões destrutivas devem usar alerta visual e explicar as consequências, especialmente quando houver execução ou histórico associado.
- Criações e edições que antes ocupavam cards permanentes devem usar janelas modais acessíveis.
- Datas exibidas no fuso do usuário, mas recebidas e enviadas em ISO 8601 com offset/UTC.
- Não calcular autorização, aderência, estado de prazo ou transições de workflow exclusivamente no cliente.
- Usar textos completos junto a ícones e cores, especialmente para resultado do checklist e urgência.
- Autosave com indicação de estado para formulários longos e execução da auditoria, depois que houver suporte transacional na API.
- Paginação e filtros no servidor para listas potencialmente grandes.
- Componentes reutilizáveis para status, usuário, upload, seletor de resultado, prazo regressivo, histórico e confirmação.

## 8. Contrato de API existente (estado atual)

Esta seção descreve o contrato validado da API em 16/09/2026. O OpenAPI publicado pela própria aplicação é a fonte executável para a integração; o front-end deve manter o acesso isolado em uma camada de serviços/adapters.

Todas as rotas, exceto cadastro e login, exigem `Authorization: Bearer <token>`.

### 8.1 Autenticação

| Método e rota | Entrada/saída atual | Observação |
|---|---|---|
| `POST /v1/auth/register` | JSON `{ nome, email, senha }`; `201` com o usuário criado | Público. |
| `POST /v1/auth/login` | JSON `{ email, senha }`; `200` com `{ accessToken, tokenType, expiresInMs }` | Público. |
| `POST /v1/auth/refresh` | Sem corpo; `200` com novo token | Exige um token JWT ainda autenticável. |

### 8.2 Usuários

| Método e rota | Entrada/saída atual | Observação |
|---|---|---|
| `GET /v1/usuarios/me` | Perfil com ID, nome, e-mail, indicador de imagem e roles globais | Exige autenticação. |
| `PUT /v1/usuarios/me` | Atualiza nome e e-mail e retorna o perfil | Exige autenticação. |
| `PATCH /v1/usuarios/me/senha` | Valida a senha atual e altera a senha; `204` | Exige autenticação. |
| `PUT /v1/usuarios/me/imagem` | Multipart JPG, PNG ou WebP de até 5 MB no campo `imagem`; `204` | Exige autenticação. |
| `GET /v1/usuarios/me/imagem` | Retorna o binário e o tipo MIME | Exige autenticação. |
| `GET /v1/usuarios?page=0&size=15` | Lista paginada de usuários | Exige exclusivamente `ROLE_ADMIN`, que é global. |

### 8.3 Planos

Payload de criação/edição:

```json
{
  "nomeProjeto": "string",
  "versao": "string",
  "objetivo": "string",
  "visaoGeral": "string"
}
```

| Método e rota | Comportamento atual |
|---|---|
| `POST /v1/planos` | Cria o plano, vincula o usuário autenticado como `AUDITOR_RESPONSAVEL_QUALIDADE` e retorna `201` com corpo e `Location`. |
| `GET /v1/planos` | Lista, de forma paginada, somente os planos do usuário autenticado. |
| `GET /v1/planos/{id}` | Retorna os dados detalhados e as permissões efetivas do usuário no plano. |
| `PUT /v1/planos/{id}` | Atualiza os dados básicos com autorização contextual. |
| `PATCH /v1/planos/{id}/conclusao` | Marca o plano como `CONCLUIDO` e retorna `204`. |
| `DELETE /v1/planos/{id}` | Exclui o plano conforme a permissão contextual e retorna `204`. |

### 8.4 Documentos

O upload usa `multipart/form-data` em `POST /v1/planos/{planoId}/documentos`, com os campos `nome`, `versao`, `arquivo` e `classificacao`. A classificação aceita `REFERENCIA` ou `AUDITADO`.

| Método e rota | Comportamento atual |
|---|---|
| `POST /v1/planos/{planoId}/documentos` | Armazena o arquivo e retorna `201` com metadados e `Location`. |
| `GET /v1/planos/{planoId}/documentos` | Lista documentos de forma paginada; aceita o filtro opcional `classificacao`. |
| `GET /v1/planos/{planoId}/documentos/{documentoId}` | Retorna metadados do documento. |
| `GET /v1/planos/{planoId}/documentos/{documentoId}/arquivo` | Retorna o arquivo binário para download. |
| `DELETE /v1/planos/{planoId}/documentos/{documentoId}` | Exclui o documento conforme as regras de vínculo e retorna `204`. |

### 8.5 Participantes e papéis do plano

Os papéis desta seção são contextuais ao plano. Eles não concedem a role global `ROLE_ADMIN`.

Os únicos papéis aceitos são `AUDITOR_RESPONSAVEL_QUALIDADE`, `MEMBRO_EQUIPE_RESOLUCAO`, `SUPERIOR_N1` e `SUPERIOR_N2`. Cada usuário possui exatamente um papel em cada plano. O papel `AUDITOR_RESPONSAVEL_QUALIDADE` reúne a gestão da qualidade e a execução das auditorias; o criador do plano recebe esse papel automaticamente. Superiores possuem consulta às abas do plano. A equipe de resolução recebe somente a caixa de NCs do time.

Todos os participantes com `MEMBRO_EQUIPE_RESOLUCAO` formam a equipe de resolução. Eles acessam somente as NCs encaminhadas ao time, e uma NC pode ter opcionalmente um desses membros como responsável individual. Cada plano pode possuir no máximo um `SUPERIOR_N1` e um `SUPERIOR_N2`; superiores são destinatários de escalonamento, não responsáveis pela resolução.

| Método e rota | Comportamento atual |
|---|---|
| `POST /v1/planos/{planoId}/participantes` | Adiciona um usuário cadastrado e retorna `201` com `Location`. |
| `GET /v1/planos/{planoId}/participantes` | Lista participantes, papel único e permissões efetivas. |
| `PUT /v1/planos/{planoId}/participantes/superiores` | Define ou remove, atomicamente, o superior N1 e o superior N2 por e-mail. |
| `PUT /v1/planos/{planoId}/participantes/{participanteId}` | Substitui o papel contextual do participante. |
| `DELETE /v1/planos/{planoId}/participantes/{participanteId}` | Remove o participante e retorna `204`, respeitando vínculos protegidos. |

### 8.6 Artefatos e auditorias

| Método e rota | Comportamento atual |
|---|---|
| `POST/GET /v1/planos/{planoId}/artefatos` | Cria o artefato com sua auditoria, checklist ativo e referências obrigatórias, ou lista artefatos do plano. |
| `GET/PUT/DELETE /v1/planos/{planoId}/artefatos/{artefatoId}` | Consulta, atualiza ou remove um artefato. |
| `GET /v1/planos/{planoId}/artefatos/{artefatoId}/auditorias` | Lista a única auditoria associada ao artefato. |
| `GET .../auditorias/{auditoriaId}` | Retorna auditoria, checklists ativos, itens, respostas e totais agregados. |
| `POST .../auditorias/{auditoriaId}/checklists/{checklistId}/itens` | Adiciona um item ao checklist selecionado. |
| `PUT .../auditorias/{auditoriaId}/checklists/{checklistId}/itens/{itemId}` | Atualiza a pergunta do item selecionado. |
| `GET/POST .../auditorias/{auditoriaId}/checklists/{checklistId}/versoes-execucao` | Lista a linha do tempo ou cria uma versão de trabalho independente. |
| `PUT .../versoes-execucao/{versaoId}/itens/{itemId}` | Salva automaticamente a edição de uma linha na versão selecionada. |
| `POST .../auditorias/{auditoriaId}/checklists/{checklistId}/encerramento` | Rota legada mantida por compatibilidade; o fluxo atual não expõe fechamento individual de checklist. |
| `POST .../auditorias/{auditoriaId}/autorizacao-conclusao` | Permite ao superior N1/N2 justificar a conclusão excepcional com NCs escalonadas e pendentes. |

Itens podem ser criados manualmente; a origem por IA já está representada no domínio, mas a geração automática ainda não foi implementada.

### 8.7 Execução e não conformidades

| Método e rota | Comportamento atual |
|---|---|
| `PUT .../auditorias/{auditoriaId}/respostas/{itemId}` | Registra `CONFORME`, `NAO_CONFORME` ou `NAO_APLICAVEL`. |
| `POST .../auditorias/{auditoriaId}/conclusao` | Valida as tabelas, finaliza os checklists automaticamente e calcula a aderência da auditoria. |
| `POST/GET /v1/planos/{planoId}/nao-conformidades` | Cria e encaminha uma NC na mesma transação, ou lista as NCs do plano. |
| `GET/PUT/DELETE /v1/planos/{planoId}/nao-conformidades/{naoConformidadeId}` | Consulta, atualiza ou remove uma NC conforme seu estado. |
| `GET /v1/planos/{planoId}/nao-conformidades/{naoConformidadeId}/historico` | Retorna identificação, encaminhamento, resoluções, validações, escalonamentos e notificações da NC. |
| `POST /v1/planos/{planoId}/nao-conformidades/{naoConformidadeId}/alertas-equipe` | Permite ao superior do nível atual alertar a equipe de resolução sobre o prazo excedido. |
| `GET /v1/minhas-nao-conformidades` | Lista as NCs encaminhadas às equipes do usuário, sem expor planos ou auditorias. |
| `GET /v1/planos/{planoId}/nao-conformidades/equipe` | Lista, para um membro da resolução, todas as NCs enviadas à equipe daquele plano. |
| `GET/PUT /v1/planos/{planoId}/configuracao/classificacoes` | Consulta ou atualiza classificações ativas e seus prazos. |
| `GET/POST /v1/planos/{planoId}/configuracao/feriados` | Lista ou adiciona feriados específicos do plano. |
| `DELETE /v1/planos/{planoId}/configuracao/feriados/{feriadoId}` | Remove um feriado específico do plano. |
| `GET /v1/planos/{planoId}/atividades` | Lista o log paginado das alterações realizadas pelos auditores. |

Somente o auditor atribuído à execução pode responder itens, gerenciar a NC e encaminhá-la à equipe.

### 8.8 Encaminhamento interno da não conformidade

O encaminhamento é executado internamente pelo `POST /v1/planos/{planoId}/nao-conformidades`; não existe um segundo passo na interface. O endpoint legado `POST .../{naoConformidadeId}/encaminhamentos` permanece apenas para compatibilidade técnica.

A primeira tentativa cria um registro e retorna `201`; repetir a mesma chave retorna o recurso existente com `200`. Na mesma transação, todos os membros da equipe recebem uma notificação interna, a NC passa para `EM_TRATAMENTO`, exibido como **Em correção**, `enviadaEm` é registrado e `prazoEm` é calculado no servidor. Não há envio por e-mail.

### 8.9 Resolução e validação da não conformidade

Um membro da equipe usa `POST /v1/planos/{planoId}/nao-conformidades/{naoConformidadeId}/resolucoes`. Se a NC estiver sem responsável individual, o primeiro membro que informar a resolução assume a atribuição; se já houver responsável, somente ele pode resolver:

```json
{
  "descricao": "Descrição da correção executada.",
  "evidencia": "Referência, link ou identificação opcional da evidência."
}
```

A criação retorna `201` com `Location`, registra o autor e `informadaEm`, e altera a NC para `RESOLUCAO_INFORMADA`. Uma resolução pode ser informada quando a NC estiver `ENVIADA`, `EM_TRATAMENTO`, `VENCIDA`, `ESCALONADA_N1`, `VENCIDA_N1`, `ESCALONADA_N2` ou `VENCIDA_N2`.

`GET /v1/planos/{planoId}/nao-conformidades/{naoConformidadeId}/resolucoes` lista o histórico, e o sufixo `/{resolucaoId}` consulta uma tentativa individual.

Somente o auditor da execução pode usar `POST .../resolucoes/{resolucaoId}/validacao`:

```json
{
  "decisao": "APROVAR",
  "observacao": "Observação opcional do auditor."
}
```

As decisões são `APROVAR` e `SOLICITAR_AJUSTES`. A aprovação marca a resolução como `APROVADA`, conclui a NC e preenche `concluidaEm`. A solicitação de ajustes exige uma observação, marca a tentativa como `AJUSTES_SOLICITADOS` e retorna a NC para `EM_TRATAMENTO`, permitindo nova submissão sem apagar o histórico anterior.

Nesta etapa, solicitar ajustes não recalcula `prazoEm`; a política para conceder um novo prazo continua como decisão de produto pendente.

Ainda não estão implementados: sugestões por IA e relatórios consolidados.

### 8.10 Vencimento e notificações internas

O monitoramento de prazos está ativo por meio de um scheduler. A cada ciclo, a API seleciona em lote as NCs `ENVIADA`, `EM_TRATAMENTO`, `ESCALONADA_N1` ou `ESCALONADA_N2` cujo `prazoEm` já terminou. Cada NC é processada em uma transação independente e com bloqueio de escrita.

O vencimento do prazo inicial cria imediatamente o escalonamento N1, altera a
NC para `ESCALONADA_N1`, notifica o superior N1 e inicia um novo prazo útil. Se
esse prazo terminar, o sistema cria imediatamente o escalonamento N2, altera a
NC para `ESCALONADA_N2`, notifica o superior N2 e inicia o prazo final. Cada
novo prazo reutiliza a duração configurada pela classificação da NC e começa no
instante do respectivo escalonamento.

Se o prazo N2 também terminar, a NC passa para `VENCIDA_N2`, e tanto o superior
N2 quanto o auditor recebem a notificação final. Uma NC em
`RESOLUCAO_INFORMADA` não vence automaticamente enquanto aguarda a decisão do
auditor.

O processamento usa bloqueio de escrita, restrição única por nível e transação
única para impedir escalonamentos duplicados ou estados parcialmente aplicados.

| Método e rota | Comportamento atual |
|---|---|
| `GET /v1/notificacoes` | Lista, da mais recente para a mais antiga, apenas as notificações destinadas ao usuário autenticado. |
| `POST /v1/notificacoes/{notificacaoId}/leitura` | Marca a notificação do próprio usuário como `LIDA`; chamadas repetidas são idempotentes. |
| `POST .../nao-conformidades/{naoConformidadeId}/alertas-equipe` | O superior N1 ou N2 do nível atual emite um alerta interno para todos os membros da equipe de resolução. |

O ciclo usa, por padrão, intervalo de 60 segundos, atraso inicial de 30 segundos e lote de 100 registros. Os valores podem ser alterados por `PRAZO_SCHEDULER_INTERVALO_MS`, `PRAZO_SCHEDULER_ATRASO_INICIAL_MS` e `PRAZO_SCHEDULER_TAMANHO_LOTE`.

Esta etapa entrega a central interna e os vencimentos inicial, N1 e N2. As comunicações operacionais acontecem exclusivamente pela plataforma.

### 8.11 Escalonamentos automáticos N1 e N2

O auditor não executa o escalonamento manualmente. Ao detectar o vencimento, o
scheduler registra o nível, o auditor, o superior responsável, a data, o prazo
concedido e a nova data limite na mesma transação que altera a NC. A operação
falha integralmente se o plano não possuir exatamente um superior para o nível,
permitindo nova tentativa depois que a configuração for corrigida.

| Método e rota | Comportamento atual |
|---|---|
| `GET .../nao-conformidades/{naoConformidadeId}/escalonamentos` | Lista o histórico cronológico de escalonamentos da NC. |
| `GET .../nao-conformidades/{naoConformidadeId}/escalonamentos/{escalonamentoId}` | Consulta um evento individual do histórico. |

As transições automáticas são:

| Nível solicitado | Estado exigido | Novo estado |
|---|---|---|
| `N1` | `ENVIADA` ou `EM_TRATAMENTO` com prazo vencido | `ESCALONADA_N1` |
| `N2` | `ESCALONADA_N1` com prazo vencido | `ESCALONADA_N2` |

Quando o prazo N2 termina, a NC passa para `VENCIDA_N2`; esse estado não
habilita um terceiro escalonamento, mas continua aceitando a submissão de uma
resolução. N1 e N2 podem emitir, pela própria página da NC, um alerta para a
equipe enquanto forem o nível responsável pelo acompanhamento.

## 9. Riscos e bloqueadores de integração atuais

1. A estrutura inicial já está versionada pelo Flyway e o Hibernate usa `ddl-auto=validate`; bancos legados criados por `ddl-auto` precisam ser conferidos e receber baseline explícito antes da primeira inicialização com Flyway.
2. O CORS aceita por padrão `http://localhost:5173`; cada ambiente deve definir suas origens exatas em `CORS_ALLOWED_ORIGINS`, separadas por vírgula. Não há liberação curinga.
3. O encaminhamento e as notificações internas são transacionais; uma evolução para outbox será importante ao integrar canais externos.
4. A geração e a análise assistidas por IA ainda não foram implementadas.
5. A migração e os mapeamentos JPA possuem teste de integração com PostgreSQL/Testcontainers. Testes adicionais de concorrência sob carga e políticas operacionais continuam como evolução pós-MVP.

Para adotar o Flyway em um banco legado já conferido, a operação pode executar uma única inicialização com `FLYWAY_BASELINE_ON_MIGRATE=true`. A opção permanece desativada por padrão e não deve ser mantida como configuração permanente.

## 10. Direção esperada para o contrato alvo

O contrato final deve seguir estas convenções:

- recursos aninhados ou filtrados pelo plano autenticado, sem exigir `usuarioId` fornecido pelo cliente para listar “meus planos”;
- respostas de criação com `201`, corpo do recurso e/ou cabeçalho `Location`;
- `200` para atualizações com representação ou `204` sem corpo; evitar `202` quando não houver processamento assíncrono real;
- erros padronizados em `application/problem+json`, com código estável, mensagem, campos inválidos, timestamp e correlation ID;
- paginação, ordenação e filtros consistentes;
- controle otimista de concorrência em checklists, auditorias e NCs;
- enums distintos por agregado, sem reutilizar um único `Status` genérico;
- timestamps gerados pelo servidor e serializados com fuso explícito;
- endpoints assíncronos de IA retornando um identificador de trabalho e seu estado;
- endpoints de comandos para transições relevantes, evitando permitir mudanças arbitrárias de status;
- versão imutável do checklist utilizada em auditoria concluída;
- idempotency key para encaminhamento interno e escalonamento.

O contrato de erros já está disponível para falhas de validação, domínio, infraestrutura e segurança (`401` e `403`). A resposta contém `type`, `title`, `status`, `detail`, `instance`, `codigo`, `correlationId` e `timestamp`; erros de validação também incluem `campos`. O cliente pode enviar `X-Correlation-ID` usando letras, números, ponto, hífen ou sublinhado, com até 100 caracteres. Quando ausente ou inválido, a API gera um novo identificador, devolve-o no mesmo cabeçalho e o expõe via CORS.

Todas as listagens do MVP usam o contrato paginado estável com `conteudo`, `pagina`, `tamanho`, `totalElementos`, `totalPaginas`, `primeira` e `ultima`. Os parâmetros são `page`, `size` e `sort`, com página inicial `0`, tamanho padrão `15` e limite máximo `100`.

O contrato OpenAPI está disponível em `GET /v3/api-docs` e a interface Swagger UI em `/swagger-ui/index.html`. O esquema global é `bearerAuth`, do tipo HTTP Bearer com formato JWT; cadastro e login estão explicitamente marcados como públicos.

Exemplos conceituais dos recursos que permanecem futuros:

```text
POST /v1/planos/{planoId}/artefatos/{artefatoId}/auditorias/{auditoriaId}/checklist/sugestoes-ia
```

Os nomes acima servem para alinhamento e não devem ser tratados como endpoints disponíveis até sua publicação no OpenAPI da aplicação.

## 11. Requisitos não funcionais e qualidade

### Segurança e privacidade

- Autorização por plano e por ação aplicada no servidor.
- Senhas protegidas por hash forte; tokens e segredos nunca persistidos no front-end além do estritamente necessário.
- Preferir cookie `HttpOnly`, `Secure` e `SameSite` para sessão quando a arquitetura for revisada; se JWT continuar no cliente, documentar estratégia segura de armazenamento e renovação.
- Validar tipo, extensão, tamanho e conteúdo de uploads; prever verificação antimalware.
- Não enviar documentos ou dados à IA sem base legal, aviso e escopo autorizado.
- Logs não devem expor senha, token, arquivo, descrições completas de não conformidades ou qualquer conteúdo sensível.

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
- Testes end-to-end dos fluxos críticos: criar plano, gerar o checklist com a auditoria, preencher itens, emitir NC, resolver e escalonar.
- Contrato OpenAPI como fonte de integração do front-end após estabilização.

## 12. Recorte recomendado do MVP

> **MVP BACKEND ATENDIDO — front-end liberado para integração.**
>
> O recorte liberado inclui as fases 1 e 2. Os recursos de IA, relatórios e demais itens da fase 3 continuam como evolução e não bloqueiam o início do front-end.

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

- criação e encaminhamento direto, na mesma transação, para toda a equipe de resolução (implementado);
- responsável e submissão de resolução (implementados);
- validação, conclusão e retorno para ajustes pelo auditor (implementados);
- prazo inicial persistente e scheduler idempotente (implementados);
- notificação interna do primeiro vencimento ao auditor (implementada);
- escalonamento manual N1, novo prazo, histórico e notificação interna (implementados);
- vencimento N1, notificação ao auditor e escalonamento N2 com novo prazo (implementados);
- vencimento final N2 e notificação interna ao auditor (implementados).

### Fase 3 — inteligência artificial e evolução

- geração assistida de itens a partir das referências selecionadas;
- assistência na análise e descrição de não conformidades;
- citações/rastreabilidade das sugestões;
- relatórios, métricas e refinamentos de experiência;
- políticas avançadas de armazenamento, retenção e integrações.

## 13. Critérios de aceite do fluxo principal

O MVP funcional estará demonstrável quando:

1. um usuário puder se cadastrar, autenticar e criar um plano;
2. o criador puder adicionar usuários com um único papel e definir auditores, equipe de resolução e superiores N1/N2;
3. o plano separar claramente documentos de referência e documentos auditados;
4. um documento auditado puder ser associado a um artefato e a uma avaliação;
5. o auditor puder montar, ordenar e executar o checklist, salvar versões independentes, editá-las com autosave e alternar entre elas e a versão atual;
6. cada item puder ser respondido com Conforme, Não conforme ou N/A;
7. uma resposta Não conforme exigir os dados mínimos da NC;
8. o auditor puder criar e enviar a NC à equipe em uma única ação, iniciando o prazo configurado no servidor;
9. um membro da equipe puder acessar as NCs do time, assumir uma demanda sem responsável e informar a resolução;
10. cada prazo vencido gerar uma única notificação; os vencimentos inicial e N1 habilitarem respectivamente N1 e N2, e o vencimento N2 registrar o atraso final;
11. todos os eventos aparecerem em ordem cronológica no histórico;
12. o resultado da auditoria exibir totais e aderência calculados pela API;
13. um usuário sem permissão não conseguir consultar ou alterar recursos do plano, mesmo chamando a API diretamente;
14. os fluxos críticos possuírem testes automatizados e contrato OpenAPI consumível pelo front-end.

A validação mais recente foi executada em 21/09/2026: o ciclo `mvn verify` passou com 170 testes executados e um teste PostgreSQL/Testcontainers ignorado por indisponibilidade do Docker; no frontend, os 11 testes, a verificação TypeScript e o build de produção passaram. O banco possui 16 migrações Flyway versionadas.

O ciclo HTTP inclui cadastro, login, criação de plano, papel contextual único, upload de documentos, artefato, auditoria única com checklist versionado e referências, execução, resposta não conforme, criação e envio direto da NC, caixa da equipe, resolução, aprovação, conclusão e histórico cronológico.

## 14. Decisões de produto ainda pendentes

- Fórmula oficial de aderência e tratamento exato de N/A.
- Semântica de advertência e sua relação com uma NC.
- Horário comercial e eventual suspensão parcial de expediente; enquanto não houver configuração específica, as horas restantes preservam o horário da identificação ou do envio e datas não úteis são avançadas para o próximo dia útil.
- Política definitiva de prazo após N1 e N2: manter o valor informado a cada escalonamento ou adotar configuração global, por plano ou por classificação.
- Participação de responsáveis externos sem conta.
- Cardinalidade entre documento, artefato e auditorias recorrentes.
- Tipos de arquivo permitidos, retenção, antivírus e armazenamento definitivo.
- Provedor/modelo de IA, limites de uso, política de dados e rastreabilidade exigida.
- Possibilidade de assinatura/aprovação formal do plano.
- Política para exclusão, arquivamento e reabertura de plano/auditoria/NC.
- Canais de notificação futuros além da central interna.

Até que essas decisões sejam tomadas, o front-end deve manter componentes configuráveis e evitar embutir regras temporais ou transições de estado como constantes espalhadas pela aplicação.
