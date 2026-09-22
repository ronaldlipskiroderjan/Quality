# Frontend do MVP

## Escopo desta entrega

Aplicação React independente para operar autenticação, planos, equipe, documentos, artefatos, auditorias com checklist versionado, não conformidades, notificações, resoluções e escalonamentos contra a API REST existente. O deploy será feito na Vercel e a URL da API será configurada por ambiente.

A visão geral de cada plano funciona como uma síntese do Plano de Garantia da Qualidade: reúne objetivo, responsáveis, documentação, artefatos e regras de não conformidade somente no nível necessário para leitura. As abas especializadas permanecem responsáveis pela consulta detalhada e pelas operações de cada área.

## Direction contract

**THESIS:** uma área de trabalho operacional que torna o próximo passo evidente e evita o dashboard genérico de métricas.

**OWN-WORLD:** nesta fase, superfícies claras, tipografia de sistema, bordas discretas e estados textuais; identidade visual permanece deliberadamente aberta.

**STORY:** o usuário entra, prepara o plano, executa a auditoria e acompanha cada não conformidade do encaminhamento interno até a resolução ou escalonamento.

**FIRST VIEWPORT:** navegação global lateral, contexto do plano no topo e conteúdo principal com ação primária próxima ao título.

**FORM:** aplicação web operacional, code-first por solicitação de priorizar funcionalidade; seed key `functional-scaffold-user-pinned`.

**FINISH:** unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.

## Limites atuais

- Sem assistência por IA, pois ainda não há contrato backend publicado para esse recurso.
- Cada usuário possui um único papel contextual: Auditor e Responsável da Qualidade, Membro da Equipe de Resolução, Superior N1 ou Superior N2. Quem cria um plano recebe automaticamente o papel de Auditor e Responsável da Qualidade.
- Checklists não possuem navegação própria: vários podem ser criados, editados, versionados e bloqueados dentro da mesma auditoria.
- Membros da equipe usam a caixa “Resoluções da equipe” e não acessam planos ou auditorias. Superiores acessam apenas NCs efetivamente escalonadas.
- O encaminhamento ocorre integralmente pela plataforma e notifica todos os membros da equipe.
- Sem identidade visual definitiva; os componentes já preservam acessibilidade, responsividade e estados de carregamento, erro e vazio.
