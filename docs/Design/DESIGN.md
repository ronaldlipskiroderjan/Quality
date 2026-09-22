---
name: Quality
description: Sistema visual azul, preciso e operacional para auditorias de qualidade rastreáveis
colors:
  primary: "#0071e3"
  primary-hover: "#0066cc"
  canvas: "#eef3f9"
  surface: "#ffffff"
  ink: "#142033"
  muted: "#5c6b7d"
  border: "#d8e2ed"
  danger: "#c9342f"
  success: "#248a3d"
typography:
  display:
    fontFamily: "-apple-system, BlinkMacSystemFont, \"SF Pro Text\", \"Helvetica Neue\", \"Segoe UI\", sans-serif"
    fontSize: "clamp(2rem, 3.5vw, 3rem)"
    fontWeight: 600
    lineHeight: 1.02
    letterSpacing: "-0.04em"
  headline:
    fontFamily: "-apple-system, BlinkMacSystemFont, \"SF Pro Text\", \"Helvetica Neue\", \"Segoe UI\", sans-serif"
    fontSize: "clamp(1.75rem, 3vw, 2.6rem)"
    fontWeight: 600
    lineHeight: 1.1
    letterSpacing: "-0.035em"
  body:
    fontFamily: "-apple-system, BlinkMacSystemFont, \"SF Pro Text\", \"Helvetica Neue\", \"Segoe UI\", sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.55
  label:
    fontFamily: "-apple-system, BlinkMacSystemFont, \"SF Pro Text\", \"Helvetica Neue\", \"Segoe UI\", sans-serif"
    fontSize: "0.86rem"
    fontWeight: 600
rounded:
  control: "10px"
  panel: "14px"
  feature: "16px"
  pill: "999px"
spacing:
  xs: "8px"
  sm: "12px"
  md: "16px"
  lg: "24px"
  xl: "32px"
  section: "48px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.surface}"
    rounded: "{rounded.control}"
    padding: "0.7rem 1.35rem"
    height: "46px"
  button-secondary:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "0.7rem 1.35rem"
    height: "46px"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "0.8rem 0.9rem"
    height: "48px"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.panel}"
    padding: "clamp(1.35rem, 2.5vw, 2rem)"
---

# Design System: Quality

## Overview

**Creative North Star: "Precisão silenciosa, operação evidente"**

Quality combina a calma de uma apresentação de produto premium com a disciplina de uma ferramenta operacional. A interface controla a atenção com azul-marinho estrutural, superfícies branco-azuladas, próximo passo visível e dados densos diretamente escaneáveis.

A inspiração em experiências web da Apple é aplicada como filosofia, não como imitação: hierarquia firme, transições suaves, profundidade discreta e uma cor de ação usada com contenção. A aplicação evita ornamentação gratuita e deixa documentos, auditorias e decisões ocuparem o centro.

**Key Characteristics:**

- cabeçalho global reduzido à marca e à sessão do usuário; a marca retorna à dashboard;
- canvas azul-acinzentado, azul-marinho como âncora estrutural e superfícies claras por área;
- azul reservado para ação e trabalho ativo;
- títulos de página objetivos, contexto do plano compacto e orientação de próxima ação;
- formulários e tabelas de densidade controlada, com estados vazios instrutivos;
- movimento reservado a mudanças de estado e compatível com redução de movimento.

## Colors

A paleta deriva diretamente da autenticação: azul-marinho e branco estruturam a interface, azul vivo conduz ações e superfícies azuladas diferenciam contexto sem fragmentar a identidade. Vermelho e verde ficam reservados às situações realmente semânticas de erro, risco, sucesso ou conclusão.

### Primary

- **Azul de precisão:** ações primárias, links e foco.
- **Azul profundo:** hover de ações e links.

### Neutral

- **Canvas sereno:** fundo geral que separa as superfícies sem criar ruído.
- **Branco óptico:** painéis, formulários e tabelas.
- **Tinta profunda:** texto principal e cenas de destaque.
- **Cinza de leitura:** texto secundário e metadados.
- **Linha fantasma:** divisores de baixa opacidade.

### Tertiary

- **Vermelho de consequência:** falha, exclusão e ação destrutiva.
- **Verde de confirmação:** êxito e conclusão confirmada.
- **Azul documental:** documentos, referências e superfícies de trabalho.
- **Azul-marinho estrutural:** navegação, cabeçalhos tabulares e cenas de contexto.
- **Azul-cinza de atenção:** classificações, prazos e informações que pedem acompanhamento sem representar erro.

**The Functional Color Rule.** Uma superfície colorida sempre identifica uma área, prioridade ou tipo de trabalho; nunca é preenchimento decorativo aleatório.

**The Semantic Color Rule.** Vermelho, verde e azul de atenção sempre vêm acompanhados de texto que explica o estado.

## Typography

**Display Font:** pilha de interface nativa do sistema  
**Body Font:** pilha de interface nativa do sistema  
**Character:** limpa, direta e generosa; a sensação premium vem de escala, ritmo e espaço, não de peso excessivo.

### Hierarchy

- **Display** (600, responsivo até 3rem, 1.08): título único da página; o contexto do plano usa título compacto.
- **Headline** (600, responsivo até 2.6rem, 1.1): abertura de seções de alto valor.
- **Title** (600, 1.2–1.55rem): cartões, recursos e diálogos.
- **Body** (400, 1rem, 1.55): conteúdo; blocos longos ficam entre 65 e 75 caracteres.
- **Label** (600, 0.86rem): campos e controles; caixa alta é reservada a cabeçalhos tabulares e estados curtos.

**The Scale Before Weight Rule.** Para elevar uma mensagem, aumentar primeiro a escala e o espaço; evitar pesos 800 ou 900.

## Layout

O conteúdo vive em um container de 1400px com margens fluidas, mas páginas operacionais usam toda essa largura em vez de formar uma coluna estreita de cartões. O cabeçalho global ocupa 72px e contém somente marca, identidade do usuário e saída; a marca é o retorno natural à dashboard. Dentro do plano, um breadcrumb discreto antecede as áreas em uma faixa azul-marinho com ícone, nome e descrição curta.

Listagens principais podem assumir uma grade editorial assimétrica, enquanto fluxos sequenciais, históricos e tabelas preservam leitura linear. Checklists são uma planilha operacional conectada: cabeçalho de colunas, linhas compactas, numeração e respostas em células substituem um cartão por pergunta. A descrição textual é revelada somente para itens marcados como não conformes. Documentos, artefatos e equipes usam seções contínuas com hierarquia por títulos, tons e divisores. Abaixo de 900px, grids de trabalho se tornam coluna única; abaixo de 700px, ações se reorganizam e abas passam a rolar horizontalmente sem alterar o tamanho do texto.

O ritmo usa múltiplos de 4 e os passos 8, 12, 16, 24, 32 e 48px. Mais espaço precede uma nova seção do que separa seu título do conteúdo.

## Elevation & Depth

O sistema é tonal por padrão. Bordas quase invisíveis separam tabelas e campos; sombras difusas aparecem somente em superfícies que realmente flutuam: navegação translúcida, modal, painel de autenticação e hover de recursos.

### Shadow Vocabulary

- **Flutuação de navegação:** sombra difusa de baixa opacidade sob abas aderentes.
- **Elevação interativa:** sombra ampla e suave ao passar sobre um recurso clicável.
- **Foco protegido:** sombra profunda em modal e autenticação, combinada com backdrop discreto.

**The Earned Depth Rule.** Uma sombra deve comunicar flutuação ou interação; painéis estáticos usam contraste tonal.

## Shapes

Campos e botões usam cantos de 10px; painéis operacionais usam 14px; cenas de destaque e modais usam 16px. Tags e status permanecem compactos; abas contextuais usam cantos de 10px para comunicar agrupamento sem parecer uma coleção de botões. Bordas têm 1px e tonalidade azul-acinzentada.

## Components

### Buttons

- **Shape:** retângulo arredondado de 10px, altura mínima de 44px.
- **Primary:** azul com texto branco e sombra curta difusa.
- **Hover / Focus:** escala máxima de 1.015 e anel azul translúcido; sem deslocamento vertical.
- **Secondary:** branco translúcido, borda suave e blur apenas quando sobre conteúdo.

### Chips

- **Style:** pílula pequena em neutro; texto completo em peso 650.
- **State:** semântica por tonalidade clara e texto contrastante, nunca somente por cor.

### Cards / Containers

- **Corner Style:** 16px; 18px para a cena principal. Objetos independentes podem usar cards; itens sequenciais e registros permanecem em linhas dentro de uma única superfície.
- **Background:** branco sobre canvas cinza; cenas principais podem inverter para tinta profunda.
- **Shadow Strategy:** plana em repouso, sombra difusa apenas em hover quando clicável.
- **Border:** linha fantasma quando necessária.
- **Internal Padding:** 24–40px conforme a densidade.

### Inputs / Fields

- **Style:** altura mínima de 48px, fundo branco translúcido, borda suave e raio de 12px.
- **Focus:** borda azul e halo externo de baixa opacidade.
- **Error / Disabled:** estado sempre explicado por mensagem; desabilitado reduz opacidade sem ocultar rótulo.

### Autenticação

Login e cadastro usam uma única peça bipartida centralizada. O lado institucional combina azul profundo e preto estrutural, preserva o slogan “Qualidade, conduzida com clareza.” e mantém a mensagem do produto como foco. O lado funcional permanece branco, compacto e dedicado ao acesso, com campos rotulados, controle de visibilidade da senha e ação primária azul.

Em telas menores, a composição passa para uma coluna: o acolhimento é reduzido sem esconder o slogan e o formulário aparece logo em seguida.

### Navigation

A barra global usa azul-marinho e contém somente marca, identidade do usuário e saída; a marca retorna à dashboard. No plano, cada aba explica sua função dentro de uma faixa azul profunda; a ativa usa azul vivo e texto branco. Em telas estreitas, a faixa rola horizontalmente sem scrollbar visível.

### Painéis operacionais

Documentos, auditorias e resolução começam com uma faixa azul-marinho compacta que explica o modelo mental da tela e mostra apenas contagens reais. O conteúdo usa superfícies brancas e azuladas, enquanto cores semânticas aparecem somente nos estados. Dentro de cada área, registros são linhas conectadas com colunas estáveis; cartões separados ficam reservados para entidades realmente independentes.

### Checklist de auditoria

O checklist replica a lógica visual de uma planilha de auditoria. Suas colunas fixas são: número, descrição, resultado, identificação da NC, responsável pela resolução, classificação da NCF, ação corretiva, previsão de resolução, escalonamento, conclusão e status da NC. A planilha pode rolar horizontalmente, mas seu cabeçalho permanece no fluxo do documento para nunca encobrir perguntas. Cada linha permanece curta para comportar dezenas de itens sem perder a visão do conjunto. Conforme e N/A não expõem campos textuais; ao selecionar Não conforme, a descrição obrigatória aparece logo abaixo da própria linha, preservando o vínculo entre decisão e evidência.

O resultado e o fluxo de resolução são etapas distintas. Depois de salvar um item como Não conforme, a linha oferece a ação explícita **Enviar para resolução**. Após o envio, essa ação é substituída por um histórico permanente com data, responsável, prazo, situação atual e acesso à linha do tempo completa. A interface usa “enviar”, nunca “registrar”, porque a confirmação notifica a equipe e inicia o prazo imediatamente.

Na entrada da auditoria, os checklists aparecem como linhas recolhidas com estado e contagem, evitando carregar uma planilha extensa antes da escolha do usuário. Abrir um checklist cria a primeira versão da execução e **Salvar versão** adiciona um ponto de controle manual. Não existe fechamento individual: a auditoria finaliza os checklists automaticamente depois de validar todas as tabelas. Os registros são apresentados numa linha do tempo contínua e expansível, com autoria, horário e resumo dos resultados.

### Dashboard

A entrada do sistema é uma composição única: abertura em azul-marinho com indicadores reais, fila unificada do que exige atenção, acesso às listagens completas e planos recentes. Representações geométricas de documentos e checklists dão identidade às áreas sem depender de fotografia ou dados inventados.

### Histórico

O histórico usa cabeçalho preto e uma linha cronológica contínua. Cada evento mostra iniciais do autor, ação, descrição e data em posições estáveis para permitir leitura rápida e comparação entre alterações.

### Feedback e prevenção de erros

Carregamentos usam skeleton e mensagem de status; estados vazios informam o que falta e oferecem a próxima ação quando permitido. Erros e sucessos combinam ícone, título e mensagem. Ações destrutivas ou irreversíveis usam diálogo próprio, descrevem a consequência, permitem cancelamento e restauram o foco ao controle de origem.

### Plano de Garantia

A primeira seção do resumo é uma cena azul-marinho ampla que reúne identidade, objetivo e visão geral. As demais áreas usam superfícies brancas e azuladas, com símbolo azul, título, explicação e acesso ao detalhe.

## Do's and Don'ts

### Do:

- **Do** mostrar uma decisão ou tarefa dominante por região.
- **Do** usar espaço negativo para separar contextos e reduzir carga cognitiva.
- **Do** manter estados remotos completos: carregando, vazio, erro, sucesso e desabilitado.
- **Do** preservar foco visível, rótulos associados e redução de movimento.
- **Do** adaptar a composição para mobile em vez de apenas reduzir medidas.

### Don't:

- **Don't** copiar marcas, textos ou elementos proprietários da Apple; usar somente os princípios de atenção e acabamento.
- **Don't** espalhar gradientes, glassmorphism, sombras ou animações por toda a interface.
- **Don't** montar páginas como sequências de cartões idênticos.
- **Don't** usar a role global ADMIN para inferir permissão dentro de um plano.
- **Don't** inventar métricas ou dados que a API não forneça.
