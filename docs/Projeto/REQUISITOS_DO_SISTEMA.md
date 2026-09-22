# Especificação de Requisitos — Plataforma Quality

> Versão: 1.0  
> Data: 22/09/2026  
> Status: baseline para planejamento, desenvolvimento e testes  
> Abrangência: aplicação web, API REST, persistência, processamento de prazos e integrações futuras de IA

## 1. Objetivo

Este documento define os requisitos funcionais e não funcionais da plataforma Quality como referência única para iniciar o sistema do zero. Ele descreve o comportamento esperado do produto sem depender da implementação atual.

A plataforma deve automatizar o planejamento, a execução e a rastreabilidade de auditorias de garantia da qualidade, desde a criação do plano até a resolução ou o escalonamento de cada não conformidade.

## 2. Escopo do produto

O sistema deve permitir:

- publicar uma página inicial que explique a plataforma;
- cadastrar e autenticar usuários;
- criar e administrar Planos de Garantia da Qualidade;
- definir auditores, equipe de resolução e superiores N1 e N2;
- armazenar documentos de referência e documentos auditados;
- criar artefatos avaliáveis e suas auditorias;
- construir e executar checklists em formato de planilha;
- manter uma versão-base e versões independentes do checklist;
- registrar e encaminhar não conformidades diretamente pela plataforma;
- calcular prazos em dias úteis e horas;
- escalonar automaticamente não conformidades vencidas;
- permitir o tratamento, a validação e o retrabalho das resoluções;
- gerar notificações internas e trilhas de auditoria;
- consolidar compromissos, prioridades e planos em uma dashboard;
- futuramente, auxiliar a criação de itens com inteligência artificial sob revisão humana.

Não haverá comunicação operacional por e-mail. As comunicações de não conformidade, resolução, ajuste e escalonamento devem acontecer dentro da plataforma.

## 3. Glossário

| Termo | Definição |
|---|---|
| Plano | Plano de Garantia da Qualidade que delimita usuários, documentos, auditorias e regras de NC. |
| Auditor | Papel contextual que também representa o responsável pela qualidade. |
| Equipe de resolução | Conjunto de participantes responsáveis por tratar NCs encaminhadas ao time. |
| Superior N1 | Primeiro superior notificado automaticamente quando o prazo inicial da NC vence. |
| Superior N2 | Segundo superior notificado quando o prazo concedido após o N1 vence. |
| Documento de referência | Norma, template, processo ou diretriz usada como fonte para uma auditoria. |
| Documento auditado | Arquivo submetido à avaliação de qualidade. |
| Artefato | Unidade avaliável associada a um documento auditado. |
| Auditoria | Processo de avaliação de um artefato, com data prevista, referências e checklist. |
| Checklist | Tabela de perguntas e resultados pertencente a uma auditoria. |
| Versão-base | Estado corrente e continuamente salvo do checklist. |
| Versão salva | Cópia independente do checklist criada pelo usuário e acessível pela linha do tempo. |
| NC | Não conformidade originada de um item marcado como Não conforme. |
| Aderência | Percentual de itens conformes entre os itens aplicáveis. |
| Escalonamento | Encaminhamento automático de uma NC vencida para N1 ou N2. |

## 4. Atores e papéis

### 4.1 Papéis globais

| Papel | Responsabilidade |
|---|---|
| Usuário | Utilizar as funcionalidades permitidas pelos papéis recebidos em cada plano. |
| Administrador do sistema | Administrar usuários e configurações globais. Não é administrador de plano e não recebe acesso automático ao conteúdo dos planos. |

### 4.2 Papéis contextuais do plano

| Papel | Acesso permitido |
|---|---|
| Auditor e responsável pela qualidade | Administrar o plano, auditores, configurações, documentos, artefatos, auditorias, checklists e NCs; validar resoluções e concluir auditorias. |
| Membro da equipe de resolução | Consultar somente NCs encaminhadas à sua equipe e NCs atribuídas a si; informar resolução e evidências. |
| Superior N1 | Consultar Visão geral, Auditores e Escalonamentos; atuar nas NCs escalonadas ao N1. |
| Superior N2 | Consultar Visão geral, Auditores e Escalonamentos; atuar nas NCs escalonadas ao N2. |

### 4.3 Matriz resumida de acesso

| Recurso | Auditor | Equipe de resolução | Superior N1 | Superior N2 | Admin global |
|---|---:|---:|---:|---:|---:|
| Criar plano | Sim | Sim, recebendo papel de auditor no novo plano | Sim, recebendo papel de auditor no novo plano | Sim, recebendo papel de auditor no novo plano | Sim, recebendo papel de auditor no novo plano |
| Visão geral do plano | Sim | Não | Sim | Sim | Somente se participar do plano |
| Auditores e log | Sim | Não | Sim, leitura | Sim, leitura | Somente se participar do plano |
| Documentos, artefatos e auditorias | Sim | Não | Não | Não | Somente se participar como auditor |
| Caixa de NCs da equipe | Acompanhamento | Sim | Não | Não | Somente conforme papel contextual |
| Escalonamentos | Sim | Não | Do próprio nível | Do próprio nível | Somente conforme papel contextual |
| Validar resolução | Sim | Não | Não | Não | Somente se participar como auditor |
| Administração global | Não | Não | Não | Não | Sim |

## 5. Premissas e restrições de domínio

- Cada usuário deve possuir exatamente um papel contextual dentro de cada plano.
- O mesmo usuário pode possuir papéis diferentes em planos diferentes.
- O criador do plano deve receber automaticamente o papel Auditor e responsável pela qualidade.
- Cada plano pode possuir vários auditores e membros da equipe de resolução.
- Cada plano pode possuir no máximo um Superior N1 e um Superior N2.
- N1 e N2 não são responsáveis pela resolução; são responsáveis pelo acompanhamento após o escalonamento.
- Somente usuários cadastrados podem participar de um plano.
- Cada artefato deve possuir exatamente uma auditoria.
- Cada auditoria deve possuir exatamente um checklist ativo, criado automaticamente.
- A comunicação operacional deve ser interna; envio de e-mail está fora do escopo.
- Datas oficiais, prazos e transições de estado devem ser determinados pelo servidor.
- A exclusão de dados com histórico deve preservar a rastreabilidade por exclusão lógica ou arquivamento.

## 6. Requisitos funcionais

Prioridades utilizadas:

- **MVP:** indispensável para a primeira versão utilizável;
- **Obrigatório:** necessário para completar o produto após a fundação do MVP;
- **Evolução:** capacidade planejada que depende de decisão ou integração futura.

### 6.1 Página pública e navegação

| ID | Prioridade | Requisito |
|---|---|---|
| RF-PUB-001 | MVP | O sistema deve disponibilizar uma página inicial pública que explique o propósito, o fluxo e os benefícios da plataforma. |
| RF-PUB-002 | MVP | A página inicial deve oferecer ações claras para entrar e criar uma conta. |
| RF-PUB-003 | MVP | A página inicial, o login, o cadastro e a aplicação autenticada devem compartilhar a mesma identidade visual, paleta e cabeçalho-base. |
| RF-PUB-004 | MVP | Rotas protegidas devem redirecionar usuários não autenticados para a entrada. |
| RF-PUB-005 | MVP | Usuários autenticados que acessarem login ou cadastro devem ser direcionados à dashboard. |
| RF-PUB-006 | MVP | O sistema deve apresentar uma página de recurso não encontrado para rotas inválidas. |

### 6.2 Cadastro, autenticação e sessão

| ID | Prioridade | Requisito |
|---|---|---|
| RF-AUT-001 | MVP | O usuário deve poder se cadastrar com nome, e-mail válido e senha. |
| RF-AUT-002 | MVP | O e-mail deve ser único, comparado sem diferença entre letras maiúsculas e minúsculas. |
| RF-AUT-003 | MVP | A senha deve possuir entre 8 e 72 caracteres. |
| RF-AUT-004 | MVP | O usuário deve poder entrar com e-mail e senha. |
| RF-AUT-005 | MVP | Após autenticação válida, a API deve emitir um token de acesso com expiração. |
| RF-AUT-006 | MVP | O sistema deve renovar uma sessão válida sem exigir novo login durante o período permitido. |
| RF-AUT-007 | MVP | O usuário deve poder encerrar a sessão; o cliente deve eliminar credenciais e dados locais da sessão. |
| RF-AUT-008 | MVP | Credenciais inválidas devem produzir mensagem segura, sem revelar se o e-mail existe. |
| RF-AUT-009 | Obrigatório | Tentativas repetidas de autenticação devem estar sujeitas a limitação de frequência. |

### 6.3 Conta e perfil

| ID | Prioridade | Requisito |
|---|---|---|
| RF-USR-001 | MVP | O usuário autenticado deve consultar nome, e-mail, imagem e papéis globais da própria conta. |
| RF-USR-002 | MVP | O usuário deve editar o próprio nome e e-mail. |
| RF-USR-003 | MVP | O usuário deve alterar a senha informando senha atual, nova senha e confirmação. |
| RF-USR-004 | MVP | O usuário deve adicionar, substituir ou excluir a imagem de perfil. |
| RF-USR-005 | MVP | A alteração da imagem deve ocorrer em janela flutuante aberta por uma ação de edição sobre a moldura da foto. |
| RF-USR-006 | MVP | O editor deve informar formatos aceitos e tamanho máximo do arquivo. |
| RF-USR-007 | MVP | O usuário deve poder enquadrar, ampliar e reposicionar a imagem antes de salvar. |
| RF-USR-008 | MVP | Imagens de perfil devem aceitar JPG, PNG ou WebP com até 5 MB. |
| RF-USR-009 | MVP | Na ausência de imagem, o sistema deve exibir um avatar alternativo consistente e acessível. |

### 6.4 Dashboard

| ID | Prioridade | Requisito |
|---|---|---|
| RF-DSH-001 | MVP | A dashboard deve ser a página inicial do usuário autenticado. |
| RF-DSH-002 | MVP | A dashboard deve exibir, em uma coluna fixa à esquerda, todos os planos acessíveis ao usuário. |
| RF-DSH-003 | MVP | A coluna de planos deve permitir abrir um plano e iniciar a criação de um novo. |
| RF-DSH-004 | MVP | A criação de plano deve ocorrer em uma janela modal sobre a dashboard, sem abrir nova aba ou página. |
| RF-DSH-005 | MVP | A dashboard deve possuir calendário mensal navegável. |
| RF-DSH-006 | MVP | O calendário deve exibir as auditorias nas respectivas datas previstas de entrega. |
| RF-DSH-007 | MVP | Selecionar um evento do calendário deve abrir o plano, artefato ou auditoria relacionado, conforme a permissão do usuário. |
| RF-DSH-008 | MVP | A dashboard deve exibir uma área de prioridades contendo somente NCs e escalonamentos atribuídos ou destinados ao usuário. |
| RF-DSH-009 | MVP | O cartão de prioridades deve mostrar um item por vez, sua quantidade total e permitir navegação vertical entre itens. |
| RF-DSH-010 | MVP | Notificações informativas não devem ser misturadas com prioridades. |
| RF-DSH-011 | MVP | O cabeçalho autenticado deve possuir um sino de notificações ao lado da imagem de perfil. |
| RF-DSH-012 | MVP | Membros de equipe devem visualizar na dashboard acessos para NCs atribuídas e NCs da equipe, sem ganhar acesso ao plano. |
| RF-DSH-013 | MVP | A dashboard deve refletir alterações relevantes sem exigir botões de atualização; atualização manual permanece disponível pelo navegador. |

### 6.5 Planos de Garantia da Qualidade

| ID | Prioridade | Requisito |
|---|---|---|
| RF-PLN-001 | MVP | O usuário autenticado deve criar um plano informando nome do projeto, versão, objetivo e visão geral. |
| RF-PLN-002 | MVP | O plano deve registrar criador, data de criação, data de atualização e status. |
| RF-PLN-003 | MVP | O plano deve aceitar uma imagem de capa opcional. |
| RF-PLN-004 | MVP | O usuário autorizado deve adicionar, substituir ou excluir a imagem do plano. |
| RF-PLN-005 | MVP | O sistema deve listar somente os planos aos quais o usuário possua acesso contextual. |
| RF-PLN-006 | MVP | O usuário autorizado deve consultar e editar os dados do plano. |
| RF-PLN-007 | MVP | A visão geral deve organizar as informações segundo a estrutura reconhecível de um Plano de Garantia da Qualidade. |
| RF-PLN-008 | MVP | A visão geral deve apresentar objetivo, visão geral, auditores, superiores, documentos de referência, documentos auditados, artefatos e classificações de NC. |
| RF-PLN-009 | MVP | Auditores e superiores exibidos na visão geral devem possuir avatar circular à esquerda do nome. |
| RF-PLN-010 | MVP | Documentos e artefatos apresentados na visão geral devem usar tabelas com Nome, nome real do arquivo e Versão. |
| RF-PLN-011 | MVP | O usuário autorizado deve concluir um plano mediante confirmação explícita. |
| RF-PLN-012 | MVP | O sistema deve impedir conclusão inconsistente e explicar quais pendências precisam ser resolvidas. |
| RF-PLN-013 | MVP | O usuário autorizado deve solicitar a exclusão de um plano. |
| RF-PLN-014 | MVP | Se o plano possuir execução ou vínculos, o sistema deve apresentar confirmação reforçada com as consequências. |
| RF-PLN-015 | MVP | Após a confirmação reforçada, a operação deve ser concluída sem expor erro genérico de integridade; dados históricos devem ser arquivados ou excluídos logicamente. |
| RF-PLN-016 | MVP | As ações Editar, Concluir e Excluir plano devem permanecer agrupadas na mesma área. |

### 6.6 Participantes, auditores e superiores

| ID | Prioridade | Requisito |
|---|---|---|
| RF-PAR-001 | MVP | Um auditor autorizado deve adicionar participantes por e-mail. |
| RF-PAR-002 | MVP | Ao adicionar o participante, deve ser selecionado exatamente um papel contextual. |
| RF-PAR-003 | MVP | O sistema deve rejeitar participante inexistente, duplicado ou com papel inválido. |
| RF-PAR-004 | MVP | Um auditor autorizado deve alterar o papel contextual de um participante. |
| RF-PAR-005 | MVP | Um auditor autorizado deve remover participantes, respeitando a preservação do histórico. |
| RF-PAR-006 | MVP | O criador do plano não pode perder o último acesso administrativo do plano sem que outro auditor permaneça responsável. |
| RF-PAR-007 | MVP | A página Auditores deve listar somente participantes com papel de auditor e responsável pela qualidade. |
| RF-PAR-008 | MVP | A página Auditores deve apresentar um log cronológico com autor, alteração e data/hora. |
| RF-PAR-009 | MVP | A visão geral deve permitir definir, substituir ou remover o único Superior N1 e o único Superior N2. |
| RF-PAR-010 | MVP | Um superior não pode ser tratado como membro da equipe de resolução no mesmo plano. |
| RF-PAR-011 | MVP | A remoção ou alteração de papel não deve apagar autoria, respostas, resoluções ou eventos históricos. |

### 6.7 Classificações e calendário útil

| ID | Prioridade | Requisito |
|---|---|---|
| RF-CFG-001 | MVP | O auditor deve configurar as classificações Simples, Complexa, Severa e Extrema. |
| RF-CFG-002 | MVP | Cada classificação deve poder ser ativada ou desativada. |
| RF-CFG-003 | MVP | Cada classificação ativa deve possuir prazo expresso separadamente em dias úteis e horas. |
| RF-CFG-004 | MVP | Dias devem aceitar valores entre 0 e 365; horas, entre 0 e 23; o prazo total ativo deve ser maior que zero. |
| RF-CFG-005 | MVP | O sistema deve permitir cadastrar e remover feriados adicionais específicos do plano. |
| RF-CFG-006 | MVP | Cada feriado do plano deve possuir data e nome. |
| RF-CFG-007 | MVP | Não deve ser possível cadastrar duas vezes a mesma data de feriado no mesmo plano. |
| RF-CFG-008 | MVP | A interface não deve exibir mensagens decorativas quando não houver feriados adicionais. |

### 6.8 Documentos

| ID | Prioridade | Requisito |
|---|---|---|
| RF-DOC-001 | MVP | O auditor deve enviar documentos classificados como Referência ou Auditado. |
| RF-DOC-002 | MVP | O envio deve registrar nome de exibição, nome original, extensão, versão, tipo MIME, tamanho, autor e datas. |
| RF-DOC-003 | MVP | A página deve separar referências à esquerda e documentos auditados à direita. |
| RF-DOC-004 | MVP | Cada grupo deve ser exibido em tabela com colunas Nome, Arquivo e Versão. |
| RF-DOC-005 | MVP | A criação e a edição devem ocorrer em janela modal aberta por ação explícita. |
| RF-DOC-006 | MVP | O auditor deve alterar nome, versão e, opcionalmente, substituir o arquivo de um documento. |
| RF-DOC-007 | MVP | Ao substituir o arquivo, o sistema deve atualizar nome original, MIME, tamanho, autor da alteração e data de atualização. |
| RF-DOC-008 | MVP | A substituição do arquivo não deve quebrar os vínculos existentes com artefatos ou auditorias. |
| RF-DOC-009 | MVP | O usuário autorizado deve baixar o arquivo original. |
| RF-DOC-010 | MVP | PDF e imagens compatíveis devem ser visualizados dentro da plataforma. |
| RF-DOC-011 | MVP | Formatos sem visualização interna devem oferecer download. |
| RF-DOC-012 | MVP | Cada upload deve aceitar no máximo 10 MB. |
| RF-DOC-013 | MVP | O usuário autorizado deve excluir um documento mediante confirmação. |
| RF-DOC-014 | MVP | Documento com vínculos históricos deve ser arquivado ou excluído logicamente após confirmação reforçada, preservando a auditoria. |
| RF-DOC-015 | MVP | O sistema deve oferecer paginação e filtro por classificação. |

### 6.9 Artefatos e auditorias

| ID | Prioridade | Requisito |
|---|---|---|
| RF-ART-001 | MVP | O auditor deve criar um artefato a partir de um documento classificado como Auditado. |
| RF-ART-002 | MVP | O artefato deve registrar nome, versão, documento auditado, auditor responsável e data prevista. |
| RF-ART-003 | MVP | A criação do artefato deve exigir a seleção de um ou mais documentos de referência. |
| RF-ART-004 | MVP | A página deve agrupar os artefatos pelo documento auditado. |
| RF-ART-005 | MVP | Cada grupo deve identificar o documento por nome de exibição, nome real do arquivo e versão. |
| RF-ART-006 | MVP | A criação e a edição de artefato devem ocorrer em janela modal. |
| RF-ART-007 | MVP | Clicar no artefato deve abrir sua auditoria; não deve existir botão redundante Abrir auditoria. |
| RF-ART-008 | MVP | Ao criar o artefato, o sistema deve criar, na mesma transação, sua auditoria e o checklist ativo. |
| RF-ART-009 | MVP | A auditoria deve estar disponível para edição e resposta imediatamente, sem comando Iniciar ou Abrir checklist. |
| RF-ART-010 | MVP | A data prevista da auditoria deve aparecer no calendário da dashboard. |
| RF-ART-011 | MVP | A auditoria deve exibir os documentos de referência pelo nome real do arquivo, incluindo a extensão. |
| RF-ART-012 | MVP | O auditor autorizado deve editar o artefato, suas referências, responsável e data prevista enquanto a regra de estado permitir. |
| RF-ART-013 | MVP | O usuário autorizado deve excluir o artefato mediante confirmação. |
| RF-ART-014 | MVP | Artefato com execução deve aceitar exclusão confirmada por arquivamento ou exclusão lógica, sem erro genérico de integridade. |
| RF-ART-015 | MVP | A troca de auditor deve preservar o histórico de responsáveis. |

### 6.10 Checklist e versões

| ID | Prioridade | Requisito |
|---|---|---|
| RF-CHK-001 | MVP | O checklist não deve possuir nome ou descrição editável; sua identificação deve ser a versão. |
| RF-CHK-002 | MVP | Um checklist novo deve conter uma primeira linha vazia pronta para edição. |
| RF-CHK-003 | MVP | Abaixo da tabela deve existir uma ação Adicionar linha. |
| RF-CHK-004 | MVP | Ao lado do número de cada pergunta deve existir uma ação em formato de menos para remover a linha. |
| RF-CHK-005 | MVP | A remoção de linha com resposta ou histórico deve exigir confirmação e preservar os registros necessários. |
| RF-CHK-006 | MVP | O checklist deve ser apresentado como tabela compacta semelhante a uma planilha, adequada a pelo menos 100 itens. |
| RF-CHK-007 | MVP | O cabeçalho da tabela deve permanecer semanticamente separado das linhas e nunca aparecer entre perguntas. |
| RF-CHK-008 | MVP | Clicar em uma célula editável deve habilitar sua edição. |
| RF-CHK-009 | MVP | Sair da célula deve salvar automaticamente a alteração. |
| RF-CHK-010 | MVP | A interface deve indicar os estados Salvando, Salvo e Falha ao salvar sem bloquear a tabela inteira. |
| RF-CHK-011 | MVP | A pergunta deve aceitar até 1.000 caracteres. |
| RF-CHK-012 | MVP | Cada item deve aceitar resultado Conforme, Não conforme ou N/A. |
| RF-CHK-013 | MVP | Itens Conforme e N/A não devem exibir campo de observação. |
| RF-CHK-014 | MVP | Ao marcar Não conforme, o servidor deve registrar imediatamente a data e hora de identificação da NC. |
| RF-CHK-015 | MVP | Enquanto a NC ainda não for enviada, a coluna Status da NC deve permanecer visualmente vazia. |
| RF-CHK-016 | MVP | A linha não conforme deve permitir selecionar um responsável entre os membros da equipe de resolução. |
| RF-CHK-017 | MVP | A linha não conforme deve permitir selecionar somente classificações ativas no plano. |
| RF-CHK-018 | MVP | A linha não conforme deve permitir informar a ação corretiva indicada. |
| RF-CHK-019 | MVP | A data prevista deve ser calculada pelo servidor a partir da classificação e exibida na linha. |
| RF-CHK-020 | MVP | A linha deve exibir Descrição/resultado, identificação, responsável, classificação, ação corretiva, previsão, escalonamento, conclusão e status da NC. |
| RF-CHK-021 | MVP | Não deve existir um campo separado chamado Descrição da não conformidade; o contexto deve vir da pergunta, resultado e ação corretiva. |
| RF-CHK-022 | MVP | Cada linha não conforme deve possuir ação Enviar para resolução no extremo direito. |
| RF-CHK-023 | MVP | Enviar para resolução deve usar os dados já preenchidos na tabela, sem abrir novo formulário. |
| RF-CHK-024 | MVP | O checklist deve possuir uma versão-base continuamente salva. |
| RF-CHK-025 | MVP | O usuário deve criar versões salvas a partir do estado visível do checklist. |
| RF-CHK-026 | MVP | Cada versão salva deve copiar perguntas, ordem, resultados e dados de NC daquele momento. |
| RF-CHK-027 | MVP | O usuário deve alternar entre a versão-base e qualquer versão salva sem salvar manualmente a versão atual. |
| RF-CHK-028 | MVP | Versões salvas devem permanecer editáveis enquanto a auditoria estiver em andamento. |
| RF-CHK-029 | MVP | Alterações em qualquer versão devem usar salvamento automático e permanecer isoladas das demais versões. |
| RF-CHK-030 | MVP | A linha do tempo deve permitir abrir cada versão com os dados correspondentes. |
| RF-CHK-031 | MVP | A linha do tempo deve registrar número da versão, autor e data/hora de criação. |
| RF-CHK-032 | MVP | Não deve existir uma operação de fechar checklist. |
| RF-CHK-033 | MVP | Um item que já teve NC resolvida deve poder ser alterado para Conforme ou outro resultado permitido. |
| RF-CHK-034 | MVP | Alterar o resultado atual não deve apagar a NC concluída nem seu histórico. |
| RF-CHK-035 | MVP | O sistema deve registrar quem alterou pergunta, resultado ou dado de NC e quando. |

### 6.11 Conclusão da auditoria

| ID | Prioridade | Requisito |
|---|---|---|
| RF-AUD-001 | MVP | A auditoria deve exibir totais de itens, respondidos, conformes, não conformes, N/A e progresso. |
| RF-AUD-002 | MVP | O sistema deve calcular a aderência desconsiderando itens N/A do denominador. |
| RF-AUD-003 | MVP | A fórmula padrão deve ser: conformes dividido pela soma de conformes e não conformes, multiplicado por 100. |
| RF-AUD-004 | MVP | Se não houver item aplicável respondido, a aderência deve ser apresentada como Não calculável. |
| RF-AUD-005 | MVP | A conclusão normal exige que todos os itens preenchidos estejam respondidos. |
| RF-AUD-006 | MVP | Todo item não conforme deve ter sua NC enviada antes da conclusão. |
| RF-AUD-007 | MVP | NC pendente deve impedir a conclusão, salvo autorização excepcional válida. |
| RF-AUD-008 | MVP | Superior N1 ou N2 deve autorizar excepcionalmente a conclusão apenas quando a NC estiver escalonada ao seu nível ou a nível anterior já envolvido. |
| RF-AUD-009 | MVP | A autorização excepcional deve exigir justificativa e registrar autor, nível e data/hora. |
| RF-AUD-010 | MVP | A autorização não deve concluir, cancelar ou alterar a NC; deve apenas liberar a auditoria. |
| RF-AUD-011 | MVP | A conclusão deve exigir confirmação e gerar uma versão final do checklist. |
| RF-AUD-012 | MVP | Após a conclusão, o conteúdo histórico deve permanecer consultável e protegido contra alteração não autorizada. |

### 6.12 Criação e encaminhamento de não conformidade

| ID | Prioridade | Requisito |
|---|---|---|
| RF-NC-001 | MVP | Uma NC deve nascer exclusivamente de um item marcado como Não conforme. |
| RF-NC-002 | MVP | Marcar o item como Não conforme deve preparar um rascunho sem iniciar prazo e sem notificar destinatários. |
| RF-NC-003 | MVP | A NC deve registrar plano, artefato, auditoria, checklist, versão, item, auditor e data/hora de identificação. |
| RF-NC-004 | MVP | Antes do envio, o usuário deve informar classificação e ação corretiva e pode selecionar responsável individual. |
| RF-NC-005 | MVP | O sistema não deve possuir prioridade separada para NC; a classificação determina o prazo. |
| RF-NC-006 | MVP | Enviar para resolução deve persistir e encaminhar a NC em uma única transação. |
| RF-NC-007 | MVP | O encaminhamento deve ser destinado à equipe de resolução do plano. |
| RF-NC-008 | MVP | O responsável individual, quando informado, deve pertencer à equipe de resolução do plano. |
| RF-NC-009 | MVP | Na ausência de responsável individual, todos os membros da equipe devem poder consultar a NC e um deles poderá assumi-la. |
| RF-NC-010 | MVP | O envio deve registrar data/hora, calcular o prazo e mudar o status exibido para Em correção. |
| RF-NC-011 | MVP | A tela de resolução deve mostrar somente NCs efetivamente enviadas. |
| RF-NC-012 | MVP | A lista de NCs deve identificar claramente plano, documento auditado, artefato, auditoria, item, responsável, prazo e status. |
| RF-NC-013 | MVP | Auditores devem acompanhar a equipe por participante, NCs atribuídas e fila sem responsável. |
| RF-NC-014 | MVP | O detalhe da NC deve apresentar dados principais e uma linha do tempo cronológica. |
| RF-NC-015 | MVP | O histórico deve incluir identificação, envio, atribuição, resolução, validação, ajustes, prazos, escalonamentos, alertas e notificações relevantes. |
| RF-NC-016 | MVP | O sistema deve permitir atualizar responsável, classificação ou ação corretiva enquanto o estado permitir, mantendo o histórico. |
| RF-NC-017 | MVP | A exclusão ou cancelamento de NC deve exigir permissão, confirmação e preservação do histórico. |

### 6.13 Prazos úteis

| ID | Prioridade | Requisito |
|---|---|---|
| RF-PRZ-001 | MVP | O prazo inicial deve começar somente quando a NC for enviada à equipe. |
| RF-PRZ-002 | MVP | O servidor deve calcular e persistir a data/hora limite. |
| RF-PRZ-003 | MVP | Sábados e domingos não devem consumir dias do prazo. |
| RF-PRZ-004 | MVP | Feriados nacionais brasileiros não devem consumir dias do prazo. |
| RF-PRZ-005 | MVP | A Sexta-feira da Paixão deve ser calculada anualmente e tratada como feriado. |
| RF-PRZ-006 | MVP | Feriados adicionais cadastrados no plano não devem consumir dias do prazo. |
| RF-PRZ-007 | MVP | Cada bloco de 24 horas configurado deve representar um dia útil; horas restantes devem ser adicionadas após os dias úteis. |
| RF-PRZ-008 | MVP | Sexta-feira mais dois dias úteis deve resultar em terça-feira no mesmo horário; se segunda for feriado, deve resultar em quarta-feira. |
| RF-PRZ-009 | MVP | O frontend deve exibir a data/hora limite e o tempo restante, usando o valor oficial do servidor. |
| RF-PRZ-010 | MVP | A interface deve diferenciar prazo normal, próximo do vencimento, vencido e concluído com texto e não apenas cor. |

### 6.14 Resolução e validação

| ID | Prioridade | Requisito |
|---|---|---|
| RF-RES-001 | MVP | Um membro da equipe deve consultar todas as NCs enviadas à sua equipe e, separadamente, as atribuídas a si. |
| RF-RES-002 | MVP | Se a NC possuir responsável individual, somente ele deve informar a resolução. |
| RF-RES-003 | MVP | Se a NC estiver sem responsável, o primeiro membro que informar resolução deve assumir a responsabilidade. |
| RF-RES-004 | MVP | A resolução deve exigir descrição da correção e aceitar evidência opcional. |
| RF-RES-005 | MVP | Informar resolução deve registrar autor e data/hora e mudar a NC para Resolução informada. |
| RF-RES-006 | MVP | A auditoria deve manter todas as tentativas de resolução, sem sobrescrever as anteriores. |
| RF-RES-007 | MVP | O auditor responsável deve aprovar a resolução ou solicitar ajustes. |
| RF-RES-008 | MVP | Solicitar ajustes deve exigir observação. |
| RF-RES-009 | MVP | A aprovação deve concluir a NC e registrar data/hora da conclusão. |
| RF-RES-010 | MVP | A solicitação de ajustes deve retornar a NC para Em correção e permitir nova tentativa. |
| RF-RES-011 | MVP | O histórico anterior deve permanecer íntegro após uma solicitação de ajustes. |
| RF-RES-012 | MVP | Uma resolução informada deve suspender o avanço automático de prazo enquanto aguarda validação do auditor. |

### 6.15 Escalonamentos e atuação dos superiores

| ID | Prioridade | Requisito |
|---|---|---|
| RF-ESC-001 | MVP | Ao vencer o prazo inicial, o sistema deve escalonar automaticamente a NC para o Superior N1. |
| RF-ESC-002 | MVP | O escalonamento N1 deve registrar superior, nível, data/hora, prazo anterior e novo prazo. |
| RF-ESC-003 | MVP | Após N1, o status deve indicar Escalonada N1 e um novo prazo deve começar. |
| RF-ESC-004 | MVP | Ao vencer o prazo N1, o sistema deve escalonar automaticamente a NC para o Superior N2. |
| RF-ESC-005 | MVP | Após N2, o status deve indicar Escalonada N2 e um prazo final deve começar. |
| RF-ESC-006 | MVP | Ao vencer o prazo N2, a NC deve mudar para Vencida N2 e não deve existir terceiro nível. |
| RF-ESC-007 | MVP | A falta do superior necessário deve impedir somente a transição inconsistente, registrar falha operacional e notificar os auditores para corrigir a configuração. |
| RF-ESC-008 | MVP | N1 e N2 devem consultar os escalonamentos do próprio nível em tela específica. |
| RF-ESC-009 | MVP | O superior responsável pelo nível atual deve poder manter ou definir nova data/hora de resolução. |
| RF-ESC-010 | MVP | Toda revisão de prazo deve registrar prazo original, novo prazo, autor e data/hora. |
| RF-ESC-011 | MVP | O superior responsável pelo nível atual deve poder emitir alerta interno à equipe sobre prazo excedido. |
| RF-ESC-012 | MVP | Cada nível pode ser criado no máximo uma vez por NC. |
| RF-ESC-013 | MVP | O processamento automático deve ser idempotente e não produzir escalonamentos ou notificações duplicadas. |

### 6.16 Notificações internas

| ID | Prioridade | Requisito |
|---|---|---|
| RF-NOT-001 | MVP | O sistema deve manter uma central de notificações exclusiva do usuário autenticado. |
| RF-NOT-002 | MVP | Cada notificação deve possuir tipo, título, mensagem, data/hora, destinatário, vínculo com a NC e estado lida/não lida. |
| RF-NOT-003 | MVP | O usuário deve marcar uma notificação como lida; repetir a operação deve ser seguro. |
| RF-NOT-004 | MVP | O sino deve indicar a quantidade de notificações não lidas. |
| RF-NOT-005 | MVP | O envio de uma NC deve notificar os membros da equipe de resolução e destacar o responsável individual quando houver. |
| RF-NOT-006 | MVP | A atribuição ou alteração de responsável deve notificar o novo responsável. |
| RF-NOT-007 | MVP | A resolução informada deve notificar todos os auditores do plano. |
| RF-NOT-008 | MVP | Se a NC foi escalonada somente para N1, a resolução informada deve notificar auditores e Superior N1. |
| RF-NOT-009 | MVP | Se a NC alcançou N2, a resolução informada deve notificar auditores, Superior N1 e Superior N2. |
| RF-NOT-010 | MVP | Solicitações de ajuste devem notificar o responsável individual ou, se não houver, toda a equipe de resolução. |
| RF-NOT-011 | MVP | Reajustes de prazo devem notificar o responsável individual ou, se não houver, toda a equipe de resolução. |
| RF-NOT-012 | MVP | Escalonamentos e vencimentos devem notificar o superior correspondente e os auditores conforme o estágio. |
| RF-NOT-013 | MVP | Alertas emitidos por superiores devem notificar a equipe de resolução. |
| RF-NOT-014 | MVP | Notificações devem conduzir diretamente ao recurso acessível ao destinatário. |
| RF-NOT-015 | MVP | O sistema não deve enviar e-mails para os eventos operacionais descritos neste documento. |

### 6.17 Feedback, confirmação e tratamento de erro

| ID | Prioridade | Requisito |
|---|---|---|
| RF-FBK-001 | MVP | Ações concluídas devem exibir feedback visual coerente com a identidade do sistema. |
| RF-FBK-002 | MVP | Feedbacks transitórios de sucesso devem permanecer aproximadamente dois segundos e desaparecer automaticamente. |
| RF-FBK-003 | MVP | Falhas devem permanecer visíveis até leitura ou nova ação quando exigirem decisão do usuário. |
| RF-FBK-004 | MVP | Mensagens técnicas, exceções de banco e erros de integridade não devem ser exibidos diretamente. |
| RF-FBK-005 | MVP | Erros devem explicar o problema e a ação possível para corrigi-lo. |
| RF-FBK-006 | MVP | Operações destrutivas ou irreversíveis devem exigir confirmação. |
| RF-FBK-007 | MVP | Exclusões de recursos em execução devem usar confirmação reforçada, informando impactos e preservação do histórico. |
| RF-FBK-008 | MVP | Formulários devem preservar dados preenchidos quando uma falha recuperável ocorrer. |

### 6.18 Administração global

| ID | Prioridade | Requisito |
|---|---|---|
| RF-ADM-001 | Obrigatório | Somente ROLE_ADMIN deve acessar funções administrativas globais. |
| RF-ADM-002 | Obrigatório | O administrador deve listar e pesquisar usuários. |
| RF-ADM-003 | Obrigatório | Operações administrativas não devem conceder acesso implícito aos planos dos usuários. |
| RF-ADM-004 | Obrigatório | Toda alteração administrativa deve ser auditada. |

### 6.19 Assistência por inteligência artificial

| ID | Prioridade | Requisito |
|---|---|---|
| RF-IA-001 | Evolução | O auditor deve selecionar explicitamente os documentos de referência usados pela IA. |
| RF-IA-002 | Evolução | A IA deve sugerir perguntas de checklist com justificativa e referência à fonte quando possível. |
| RF-IA-003 | Evolução | O processamento de documentos e geração deve ocorrer de forma assíncrona. |
| RF-IA-004 | Evolução | O usuário deve aceitar, editar ou rejeitar cada sugestão individualmente. |
| RF-IA-005 | Evolução | Somente sugestões aprovadas devem entrar no checklist. |
| RF-IA-006 | Evolução | Itens aceitos devem registrar origem IA sem reduzir a responsabilidade humana. |
| RF-IA-007 | Evolução | A IA pode sugerir ações corretivas, mas não deve enviar NC, alterar resultado ou tomar decisão automaticamente. |
| RF-IA-008 | Evolução | Conteúdo de um plano não deve ser usado para treinar modelos nem ser exposto a outro plano. |
| RF-IA-009 | Evolução | Prompts, fontes utilizadas, resposta recebida e decisão humana devem possuir rastreabilidade compatível com a política de privacidade. |

## 7. Regras de negócio

| ID | Regra |
|---|---|
| RN-001 | ROLE_ADMIN representa somente administração global do sistema. |
| RN-002 | Autorizações de plano devem considerar participação e papel contextual, nunca apenas a role global. |
| RN-003 | Um usuário possui exatamente um papel em cada plano. |
| RN-004 | O criador do plano torna-se auditor e responsável pela qualidade automaticamente. |
| RN-005 | Deve existir no máximo um Superior N1 e um Superior N2 por plano. |
| RN-006 | Equipe de resolução não acessa visão geral, documentos, artefatos, auditorias ou checklists. |
| RN-007 | Superiores acessam apenas Visão geral, Auditores e Escalonamentos, além do detalhe da NC escalonada permitido ao nível. |
| RN-008 | Cada artefato possui uma única auditoria e cada auditoria possui um único checklist ativo. |
| RN-009 | O checklist existe e pode ser respondido desde a criação do artefato. |
| RN-010 | Não existe fechamento individual de checklist. |
| RN-011 | O status da NC fica vazio na interface até o encaminhamento. |
| RN-012 | O prazo começa no encaminhamento, não na marcação Não conforme. |
| RN-013 | A classificação, e não uma prioridade independente, determina o prazo. |
| RN-014 | Prazos ignoram fins de semana, feriados nacionais e feriados adicionais do plano. |
| RN-015 | O primeiro vencimento escala automaticamente para N1; o segundo, para N2. |
| RN-016 | Não existe escalonamento superior ao N2. |
| RN-017 | Resolução informada aguarda validação e suspende o avanço automático do prazo. |
| RN-018 | Aprovar uma resolução conclui a NC; solicitar ajustes preserva a tentativa e reabre o tratamento. |
| RN-019 | Uma NC concluída permanece no histórico mesmo que o resultado atual do item seja alterado. |
| RN-020 | A versão-base e cada versão salva do checklist são independentes e recebem autosave. |
| RN-021 | Toda mudança relevante deve registrar usuário e data/hora. |
| RN-022 | Exclusões confirmadas não podem quebrar integridade referencial nem apagar a trilha histórica obrigatória. |
| RN-023 | Comunicações operacionais ocorrem somente dentro da plataforma. |
| RN-024 | O servidor é a fonte oficial de identidade, autorização, estado, prazo, aderência e transições. |
| RN-025 | Datas persistidas devem possuir informação inequívoca de fuso ou UTC. |

## 8. Estados e transições

### 8.1 Plano

~~~text
PENDENTE -> CONCLUIDO
PENDENTE -> EXCLUIDO_LOGICAMENTE
CONCLUIDO -> EXCLUIDO_LOGICAMENTE
~~~

### 8.2 Auditoria

~~~text
PLANEJADA -> EM_PREPARACAO -> EM_ANDAMENTO -> CONCLUIDA
                         \-> PAUSADA -> EM_ANDAMENTO
Qualquer estado não final -> CANCELADA, mediante autorização e confirmação
~~~

A auditoria nasce pronta para uso; estados intermediários podem ser atualizados automaticamente conforme a primeira pergunta ou resposta.

### 8.3 Não conformidade

~~~text
RASCUNHO
  -> EM_TRATAMENTO
  -> RESOLUCAO_INFORMADA
      -> CONCLUIDA
      -> EM_TRATAMENTO, quando ajustes são solicitados

EM_TRATAMENTO vencida
  -> ESCALONADA_N1
  -> ESCALONADA_N2
  -> VENCIDA_N2

Estados em tratamento ou escalonados
  -> RESOLUCAO_INFORMADA
  -> CONCLUIDA após aprovação
~~~

Estados cancelados ou removidos logicamente devem continuar auditáveis.

## 9. Dados mínimos

### 9.1 Usuário

- identificador;
- nome;
- e-mail normalizado;
- senha protegida;
- role global;
- imagem e tipo MIME;
- datas de criação e atualização;
- estado da conta.

### 9.2 Plano

- identificador;
- nome do projeto;
- versão;
- objetivo;
- visão geral;
- imagem opcional;
- status;
- criador;
- datas de criação, atualização e conclusão;
- participantes e papéis;
- superiores N1 e N2;
- classificações e prazos;
- feriados adicionais.

### 9.3 Documento

- identificador;
- plano;
- classificação Referência ou Auditado;
- nome de exibição;
- nome original;
- versão;
- MIME;
- tamanho;
- conteúdo ou referência segura de armazenamento;
- autor e datas;
- estado de exclusão.

### 9.4 Artefato e auditoria

- documento auditado;
- nome e versão do artefato;
- auditor;
- documentos de referência;
- data prevista;
- status;
- checklist;
- aderência;
- autorização excepcional, quando existir;
- datas de criação, execução e conclusão.

### 9.5 Item e versão do checklist

- versão;
- ordem;
- pergunta;
- origem Manual ou IA;
- resultado;
- data/hora de identificação da NC;
- responsável;
- classificação;
- ação corretiva;
- prazo;
- escalonamentos;
- data de conclusão;
- status da NC;
- autor e data de cada alteração.

### 9.6 Não conformidade

- vínculos com plano, artefato, auditoria, checklist, versão, item e resposta;
- auditor;
- equipe de resolução;
- responsável individual opcional;
- classificação;
- ação corretiva;
- datas de identificação, envio, prazo, escalonamentos e conclusão;
- status;
- resoluções e validações;
- notificações;
- histórico imutável de eventos.

## 10. Requisitos não funcionais

### 10.1 Arquitetura e manutenibilidade

| ID | Requisito |
|---|---|
| RNF-ARQ-001 | O backend deve usar Java 21, Spring Boot, Spring MVC, Spring Security, Spring Data JPA e PostgreSQL. |
| RNF-ARQ-002 | O frontend deve usar React, TypeScript e Vite, com deploy compatível com Vercel. |
| RNF-ARQ-003 | A solução deve separar apresentação, aplicação, domínio e persistência, evitando regras de negócio em controllers ou componentes visuais. |
| RNF-ARQ-004 | O código deve aplicar SOLID, Clean Code e padrões de projeto somente quando reduzirem acoplamento ou duplicação. |
| RNF-ARQ-005 | Nomes de classes, funções e variáveis devem ser claros, consistentes e sem abreviações obscuras ou extensão desnecessária. |
| RNF-ARQ-006 | Módulos devem possuir responsabilidade única e dependências explícitas. |
| RNF-ARQ-007 | Configurações variáveis por ambiente devem ser externas ao código. |
| RNF-ARQ-008 | Segredos não devem ser incluídos no repositório, bundle do frontend, logs ou respostas da API. |

### 10.2 API e interoperabilidade

| ID | Requisito |
|---|---|
| RNF-API-001 | A API deve seguir princípios REST e usar prefixo versionado, inicialmente /v1. |
| RNF-API-002 | Recursos devem usar substantivos, métodos HTTP e códigos de status semanticamente corretos. |
| RNF-API-003 | Criações devem retornar 201 e Location quando aplicável; exclusões sem corpo devem retornar 204. |
| RNF-API-004 | Erros devem usar application/problem+json com código, título, detalhe, status, caminho e identificador de correlação. |
| RNF-API-005 | Listagens potencialmente extensas devem usar paginação no servidor, com tamanho máximo de 100 itens. |
| RNF-API-006 | Filtros e ordenação devem ser executados no servidor quando a lista puder crescer. |
| RNF-API-007 | Datas devem trafegar em ISO 8601 com offset ou UTC. |
| RNF-API-008 | Uploads devem usar multipart/form-data e downloads devem preservar MIME e nome original. |
| RNF-API-009 | O contrato deve ser publicado em OpenAPI e permanecer validado por testes de contrato. |
| RNF-API-010 | Alterações incompatíveis devem criar nova versão ou seguir política explícita de depreciação. |

### 10.3 Segurança

| ID | Requisito |
|---|---|
| RNF-SEG-001 | Todo endpoint não público deve exigir autenticação. |
| RNF-SEG-002 | Senhas devem ser armazenadas somente com algoritmo de hash adaptativo e salt, nunca de forma reversível. |
| RNF-SEG-003 | Tokens devem possuir expiração curta e assinatura com chave fornecida por ambiente. |
| RNF-SEG-004 | Autorização deve ser validada no servidor em todas as operações de plano, arquivo, auditoria e NC. |
| RNF-SEG-005 | Identificadores fornecidos pelo cliente não podem permitir acesso horizontal a recursos de outro plano. |
| RNF-SEG-006 | Uploads devem validar tamanho, MIME real, extensão e nome seguro; política antimalware deve ser suportada. |
| RNF-SEG-007 | Arquivos privados não devem ser publicados por URL aberta e permanente. |
| RNF-SEG-008 | CORS deve aceitar somente origens configuradas, nunca curinga com credenciais. |
| RNF-SEG-009 | Entradas devem ser validadas no cliente por usabilidade e novamente no servidor por segurança. |
| RNF-SEG-010 | O sistema deve mitigar riscos do OWASP Top 10, incluindo injeção, XSS, CSRF conforme o mecanismo de autenticação, upload malicioso e enumeração de usuários. |
| RNF-SEG-011 | Operações sensíveis devem gerar registro de auditoria. |
| RNF-SEG-012 | Logs não devem registrar senha, token, conteúdo integral de documentos ou outros segredos. |

### 10.4 Privacidade e conformidade

| ID | Requisito |
|---|---|
| RNF-PRI-001 | O tratamento de dados pessoais deve seguir os princípios da LGPD: finalidade, necessidade, transparência, segurança e responsabilização. |
| RNF-PRI-002 | O sistema deve coletar somente dados necessários à operação. |
| RNF-PRI-003 | Exclusão de conta e retenção de registros de auditoria devem seguir política formal definida pela organização. |
| RNF-PRI-004 | O acesso a documentos e dados pessoais deve ser rastreável. |
| RNF-PRI-005 | Integrações de IA devem possuir contrato de privacidade e impedir uso não autorizado dos dados para treinamento. |

### 10.5 Persistência, migrações e integridade

| ID | Requisito |
|---|---|
| RNF-DAD-001 | PostgreSQL deve ser o banco de produção. |
| RNF-DAD-002 | Mudanças de esquema devem ser versionadas por Flyway e não depender de criação automática em produção. |
| RNF-DAD-003 | H2 pode ser usado somente em testes rápidos que não dependam de comportamento específico do PostgreSQL. |
| RNF-DAD-004 | Testes de integração de persistência devem executar também contra PostgreSQL por Testcontainers. |
| RNF-DAD-005 | Transações devem garantir atomicidade em criação de artefato, auditoria e checklist; envio de NC; validação; e escalonamento. |
| RNF-DAD-006 | Chaves estrangeiras, unicidade e controle otimista devem proteger a consistência. |
| RNF-DAD-007 | Exclusões lógicas devem ser aplicadas quando a remoção física violar rastreabilidade. |
| RNF-DAD-008 | A organização deve definir rotina de backup, restauração testada, RPO e RTO antes da produção. |

### 10.6 Confiabilidade e concorrência

| ID | Requisito |
|---|---|
| RNF-CON-001 | Autosave deve suportar concorrência otimista e informar conflito em vez de sobrescrever alteração silenciosamente. |
| RNF-CON-002 | Envio de NC, marcação de notificação e escalonamentos devem ser idempotentes. |
| RNF-CON-003 | O scheduler deve processar cada NC em transação isolada e impedir processamento simultâneo duplicado. |
| RNF-CON-004 | Falha parcial não pode deixar status, prazo, histórico e notificações divergentes. |
| RNF-CON-005 | Processos interrompidos devem poder ser retomados com segurança. |
| RNF-CON-006 | A resolução de uma NC deve continuar possível mesmo após N2 vencido. |

### 10.7 Desempenho e capacidade

| ID | Requisito |
|---|---|
| RNF-DES-001 | Em carga normal, 95% das consultas comuns da API devem responder em até 2 segundos, sem considerar download de arquivos. |
| RNF-DES-002 | Autosave deve confirmar sucesso ou falha em até 1,5 segundo em condições normais. |
| RNF-DES-003 | A primeira renderização útil das páginas principais deve ocorrer em até 3 segundos em conexão de banda larga típica. |
| RNF-DES-004 | O checklist deve manter edição utilizável com pelo menos 100 linhas. |
| RNF-DES-005 | Consultas devem evitar carregamento N+1 e retornar somente os dados necessários. |
| RNF-DES-006 | O processamento de prazos deve usar lotes configuráveis e suportar reexecução. |
| RNF-DES-007 | Metas de volume simultâneo e crescimento devem ser confirmadas por teste de carga antes da produção. |

### 10.8 Disponibilidade e recuperação

| ID | Requisito |
|---|---|
| RNF-DIS-001 | A aplicação deve possuir verificações de saúde para serviço e banco de dados. |
| RNF-DIS-002 | Falhas temporárias devem produzir estado recuperável e mensagem clara ao usuário. |
| RNF-DIS-003 | A implantação deve permitir rollback de aplicação sem reverter migrações destrutivas. |
| RNF-DIS-004 | A meta inicial de disponibilidade deve ser definida em acordo operacional antes da produção; recomenda-se no mínimo 99,5% mensal para o MVP. |

### 10.9 Observabilidade e auditabilidade

| ID | Requisito |
|---|---|
| RNF-OBS-001 | Cada requisição deve possuir identificador de correlação retornado ao cliente e propagado nos logs. |
| RNF-OBS-002 | Logs devem ser estruturados e conter nível, instante, correlação, operação e resultado. |
| RNF-OBS-003 | Métricas devem cobrir autenticação, latência, erros, uploads, autosave, NCs vencidas, escalonamentos e falhas do scheduler. |
| RNF-OBS-004 | Eventos de negócio devem registrar ator, ação, recurso, estado anterior, estado posterior e data/hora quando aplicável. |
| RNF-OBS-005 | O histórico de NC e autorizações excepcionais deve ser imutável para usuários comuns. |
| RNF-OBS-006 | Alertas operacionais devem existir para indisponibilidade, crescimento de erros, falha de scheduler e falha de banco. |

### 10.10 Usabilidade, interface e acessibilidade

| ID | Requisito |
|---|---|
| RNF-UX-001 | A interface deve seguir as dez heurísticas de Nielsen. |
| RNF-UX-002 | O sistema deve manter a identidade azul, azul-marinho, branco e superfícies azuladas definida no design system. |
| RNF-UX-003 | A aplicação deve priorizar hierarquia, contraste, alinhamento e densidade adequada, evitando fragmentação excessiva em cartões. |
| RNF-UX-004 | Operações de criação e edição devem usar modais acessíveis quando não exigirem contexto de página completa. |
| RNF-UX-005 | O sistema não deve exibir textos de preenchimento, instruções óbvias ou comentários decorativos. |
| RNF-UX-006 | Não devem existir botões genéricos Atualizar página. |
| RNF-UX-007 | Componentes devem comunicar carregamento, vazio, sucesso, erro, indisponibilidade e falta de permissão. |
| RNF-UX-008 | A navegação deve manter o usuário orientado sobre plano, artefato, auditoria e NC atuais. |
| RNF-UX-009 | O layout deve funcionar em desktop, tablet e celular; autoria de checklist pode priorizar desktop sem impedir consulta móvel. |
| RNF-UX-010 | A interface deve atender WCAG 2.2 nível AA. |
| RNF-UX-011 | Todo controle deve ser utilizável por teclado e possuir foco visível. |
| RNF-UX-012 | Rótulos, mensagens e nomes acessíveis devem existir para campos, botões, ícones e diálogos. |
| RNF-UX-013 | Estado não pode ser comunicado apenas por cor. |
| RNF-UX-014 | Texto e controles devem alcançar contraste mínimo exigido pela WCAG AA. |
| RNF-UX-015 | Animações devem respeitar preferência por movimento reduzido. |
| RNF-UX-016 | Feedbacks devem usar linguagem humana e consistente em português do Brasil. |

### 10.11 Compatibilidade

| ID | Requisito |
|---|---|
| RNF-CMP-001 | O frontend deve suportar as duas versões estáveis mais recentes de Chrome, Edge, Firefox e Safari. |
| RNF-CMP-002 | O backend deve executar em ambiente compatível com Java 21. |
| RNF-CMP-003 | O frontend deve suportar acesso direto às rotas internas quando publicado na Vercel. |
| RNF-CMP-004 | O sistema deve usar codificação UTF-8 em aplicação, API e banco. |

### 10.12 Testabilidade e qualidade

| ID | Requisito |
|---|---|
| RNF-TST-001 | Regras de domínio devem possuir testes unitários independentes de infraestrutura. |
| RNF-TST-002 | Repositórios, transações e migrações devem possuir testes de integração com PostgreSQL. |
| RNF-TST-003 | Autenticação e autorização devem possuir testes positivos, negativos e de isolamento entre planos. |
| RNF-TST-004 | A API deve possuir testes de contrato para rotas, paginação, Problem Details e OpenAPI. |
| RNF-TST-005 | O frontend deve possuir testes de componentes e fluxos críticos. |
| RNF-TST-006 | O fluxo principal deve possuir teste ponta a ponta: plano, documento, artefato, checklist, NC, resolução, validação e notificação. |
| RNF-TST-007 | O cálculo de prazo deve cobrir fins de semana, feriados nacionais, feriados do plano, virada de ano e horário de verão histórico quando relevante. |
| RNF-TST-008 | O escalonamento deve possuir testes de idempotência, concorrência e ausência de superior configurado. |
| RNF-TST-009 | Correções de defeitos devem incluir teste de regressão. |
| RNF-TST-010 | A cobertura deve priorizar regras críticas; serviços de autorização, prazo, transição e auditoria devem atingir ao menos 80% de cobertura de linhas e ramos. |
| RNF-TST-011 | Pull requests devem passar por compilação, testes, análise estática e verificação de dependências antes da integração. |

### 10.13 Implantação e configuração

| ID | Requisito |
|---|---|
| RNF-IMP-001 | Backend e banco devem possuir execução reproduzível por contêiner. |
| RNF-IMP-002 | O frontend deve possuir build estático reproduzível e configuração por ambiente. |
| RNF-IMP-003 | Ambientes de desenvolvimento, teste e produção devem usar credenciais e bancos separados. |
| RNF-IMP-004 | Migrações devem ser executadas de maneira controlada durante a implantação. |
| RNF-IMP-005 | A aplicação não deve iniciar em produção sem variáveis obrigatórias, como banco, JWT e administrador inicial. |
| RNF-IMP-006 | Origem pública da API e origens CORS devem ser configuráveis sem recompilar o backend. |

## 11. Critérios de aceite do fluxo principal

1. Um usuário se cadastra, entra e edita sua conta.
2. Ele cria um plano em modal e torna-se auditor automaticamente.
3. Define auditores, equipe de resolução, N1, N2, classificações e feriados.
4. Envia ao menos uma referência e um documento auditado.
5. Atualiza arquivo e versão de um documento sem perder vínculos.
6. Cria um artefato, escolhe referências, auditor e data prevista.
7. A auditoria e o checklist aparecem automaticamente.
8. A data prevista aparece no calendário da dashboard.
9. O auditor preenche perguntas, adiciona e remove linhas e recebe autosave.
10. Salva uma versão, troca para outra, edita ambas e retorna à base sem perder dados.
11. Marca um item como Não conforme e a identificação é registrada imediatamente.
12. Preenche responsável, classificação e ação corretiva na própria linha.
13. Envia a NC pelo botão da linha; o prazo começa e a equipe é notificada.
14. A equipe visualiza a NC com contexto claro do artefato.
15. Um membro informa resolução; auditores e superiores envolvidos são notificados.
16. O auditor solicita ajustes ou aprova sem apagar tentativas anteriores.
17. Se o prazo vencer, N1 e depois N2 recebem escalonamentos automáticos sem duplicidade.
18. Um superior revisa o prazo ou alerta a equipe, gerando histórico e notificações.
19. A NC aprovada é concluída, mas o item continua editável e pode virar Conforme.
20. A auditoria é concluída normalmente ou por autorização excepcional justificada.
21. Exclusões confirmadas de plano ou artefato não geram erro técnico e preservam histórico obrigatório.

## 12. Recorte recomendado de entrega

### 12.1 MVP

Inclui:

- página pública;
- cadastro, login, sessão e perfil;
- dashboard com planos, calendário, prioridades e notificações;
- planos, papéis, auditores e superiores;
- documentos e atualização de versão/arquivo;
- artefatos, auditoria e checklist automático;
- planilha editável, autosave e versões;
- NC, resolução, validação e histórico;
- calendário útil, escalonamentos automáticos e alertas;
- permissões, auditabilidade e testes críticos.

### 12.2 Evoluções posteriores

- geração assistida de itens por IA;
- sugestões de ação corretiva por IA;
- relatórios executivos e exportações;
- recuperação de senha por canal externo;
- integrações corporativas de identidade;
- armazenamento de objetos e antivírus gerenciado;
- métricas e painéis analíticos avançados.

## 13. Decisões pendentes antes da produção

Estas definições não impedem a construção do domínio, mas precisam ser formalizadas antes da produção:

1. política de retenção e descarte de documentos, eventos e contas;
2. RPO, RTO e disponibilidade contratada;
3. tipos de documento permitidos além de PDF e imagens;
4. solução de antivírus e armazenamento de arquivos;
5. volume esperado de usuários, planos, documentos e checklists;
6. política de recuperação de senha;
7. provedor, modelo, limites e política de dados da integração de IA;
8. política de concessão de novo prazo após solicitação de ajustes;
9. possibilidade de reabrir plano ou auditoria concluída;
10. níveis de exportação, assinatura e validade formal dos relatórios.

## 14. Definição de pronto

Um requisito somente pode ser considerado concluído quando:

- possui regra de autorização implementada no servidor;
- possui validação e tratamento de erro orientado ao usuário;
- preserva a trilha de auditoria aplicável;
- possui testes automatizados proporcionais ao risco;
- está documentado no OpenAPI quando expõe contrato de API;
- funciona nos estados de carregamento, vazio, sucesso, erro e sem permissão;
- atende teclado, foco, contraste e nomes acessíveis;
- não introduz erro de análise estática nem regressão nos testes;
- foi validado contra os critérios de aceite deste documento.
