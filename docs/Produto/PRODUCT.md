# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Frontend confirmado pelo usuário: React, TypeScript e Vite, em aplicação independente na pasta `frontend/`. O deploy do frontend será realizado na Vercel. A API permanece em Java Spring Boot com PostgreSQL.

## Users

- Usuários autenticados que criam e participam de Planos de Garantia da Qualidade.
- Auditores e responsáveis da qualidade que organizam o plano e a equipe, constroem checklists, executam avaliações, registram não conformidades e validam correções.
- Equipes de resolução, com acesso às NCs encaminhadas ao time, e superiores N1/N2 notificados somente em caso de escalonamento.
- Administradores globais do sistema; `ROLE_ADMIN` não representa administração de plano ou grupo.

## Product Purpose

Automatizar o planejamento, a execução e a rastreabilidade de auditorias de qualidade. O fluxo principal permite criar um plano, reunir equipe e documentos, preparar o checklist dentro da auditoria, congelar versões, executar a avaliação e acompanhar não conformidades até a resolução ou escalonamento.

## Positioning

O produto conecta o plano de qualidade, suas fontes documentais, a execução do checklist e o ciclo completo de resolução de não conformidades em uma única trilha auditável. A IA atua somente como assistência revisável pelo usuário, nunca como decisão automática.

## Operating Context

O trabalho acontece em espaços isolados por plano, com exatamente um papel contextual por usuário: Auditor e Responsável da Qualidade, Membro da Equipe de Resolução, Superior N1 ou Superior N2. A função de auditoria e gestão da qualidade é única, e quem cria um plano recebe esse papel automaticamente. A equipe de resolução não navega pelo plano e trabalha em uma caixa própria; superiores entram somente quando recebem um escalonamento.

## Capabilities and Constraints

- Autenticação JWT e API REST em `/v1`, com erros `application/problem+json`.
- Listagens paginadas com `conteudo`, `pagina`, `tamanho`, `totalElementos`, `totalPaginas`, `primeira` e `ultima`.
- O frontend deve obedecer às permissões efetivas retornadas pela API, sem inferir acesso a partir de `ROLE_ADMIN`.
- Upload e download de documentos exigem integração multipart/binária autenticada.
- O MVP backend está liberado para integração; assistência por IA permanece evolução posterior.
- Datas trafegam em ISO 8601 e devem ser apresentadas no fuso do usuário.
- A identidade visual segue uma direção premium, minimalista e silenciosa, inspirada em princípios de hierarquia, espaço e movimento das experiências web da Apple sem copiar sua marca; funcionalidade, clareza e acessibilidade continuam prioritárias.

## Evidence on Hand

- Escopo funcional e contratos: `docs/Projeto/ESCOPO_PROJETO_AUDITORIA_QUALIDADE.md`.
- Exemplos de checklist, comunicação de NC e plano de qualidade foram fornecidos como referências de domínio.
- A API possui documentação OpenAPI em `/v3/api-docs` e Swagger UI em `/swagger-ui/index.html`.
- Não há identidade visual, logotipo, clientes, métricas comerciais ou conteúdo promocional confirmados; trabalhos futuros não devem fabricá-los.

## Product Principles

- Manter autorização e responsabilidades explícitas por plano.
- Preservar rastreabilidade e histórico em todo o ciclo de auditoria.
- Fazer o fluxo operacional ficar claro antes do refinamento visual.
- Exigir confirmação humana para ações sensíveis, comunicações e sugestões de IA.
- Comunicar estados, erros e próximos passos em linguagem direta.

## Accessibility & Inclusion

A interface deve ser utilizável por teclado, apresentar foco visível, associar rótulos aos campos e não depender apenas de cor ou ícones para comunicar estado. O uso principal prioriza desktop, sem impedir operação em tablet e telas menores.
