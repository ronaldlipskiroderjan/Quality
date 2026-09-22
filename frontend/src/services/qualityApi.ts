import { baixarArquivo, obterArquivo, requisitar } from "../lib/api";
import {
  normalizarPaginaParticipantes,
  normalizarParticipante,
  type ParticipanteApi,
} from "../lib/participants";
import type {
  Artefato,
  ArtefatoEntrada,
  AtividadePlano,
  Auditoria,
  AuditoriaAgenda,
  AuditoriaResumo,
  ClassificacaoNaoConformidade,
  ClassificacaoDocumento,
  ConfiguracaoClassificacao,
  DecisaoValidacao,
  Documento,
  Escalonamento,
  EscalonamentoPainel,
  EncaminhamentoNaoConformidade,
  EventoNaoConformidade,
  FeriadoPlano,
  ItemAuditoria,
  ItemChecklist,
  ItemVersaoExecucao,
  NaoConformidade,
  NaoConformidadeEntrada,
  NivelEscalonamento,
  Notificacao,
  Pagina,
  PapelPlano,
  Plano,
  PlanoEntrada,
  PlanoResumo,
  Resolucao,
  ResultadoItem,
  Token,
  Usuario,
  UUID,
  SuperioresPlano,
  VersaoExecucaoChecklist,
} from "../types/api";

const pagina = (numero = 0, tamanho = 15) => `page=${numero}&size=${tamanho}`;

function dadosPlano(entrada: PlanoEntrada, imagem?: File, removerImagem = false) {
  const dados = new FormData();
  dados.append("plano", new Blob([JSON.stringify(entrada)], { type: "application/json" }));
  if (imagem) dados.append("imagem", imagem);
  if (removerImagem) dados.append("removerImagem", "true");
  return dados;
}

export const authApi = {
  entrar: (email: string, senha: string) =>
    requisitar<Token>("/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, senha }),
    }),
  cadastrar: (nome: string, email: string, senha: string) =>
    requisitar<Usuario>("/v1/auth/register", {
      method: "POST",
      body: JSON.stringify({ nome, email, senha }),
    }),
  renovar: () => requisitar<Token>("/v1/auth/refresh", { method: "POST" }),
  perfil: () => requisitar<Usuario>("/v1/usuarios/me"),
  atualizarPerfil: (nome: string, email: string) =>
    requisitar<Usuario>("/v1/usuarios/me", {
      method: "PUT",
      body: JSON.stringify({ nome, email }),
    }),
  alterarSenha: (senhaAntiga: string, novaSenha: string, confirmacaoSenha: string) =>
    requisitar<void>("/v1/usuarios/me/senha", {
      method: "PATCH",
      body: JSON.stringify({ senhaAntiga, novaSenha, confirmacaoSenha }),
    }),
  buscarImagemPerfil: () => obterArquivo("/v1/usuarios/me/imagem"),
  salvarImagemPerfil: (imagem: File) => {
    const dados = new FormData();
    dados.append("imagem", imagem);
    return requisitar<void>("/v1/usuarios/me/imagem", {
      method: "PUT",
      body: dados,
    });
  },
  removerImagemPerfil: () => requisitar<void>("/v1/usuarios/me/imagem", { method: "DELETE" }),
};

export const planosApi = {
  listar: (numero = 0) => requisitar<Pagina<PlanoResumo>>(`/v1/planos?${pagina(numero)}`),
  listarTodos: async () => {
    const primeira = await requisitar<Pagina<PlanoResumo>>(
      `/v1/planos?${pagina(0, 100)}`,
    );
    if (primeira.totalPaginas <= 1) return primeira;
    const restantes = await Promise.all(
      Array.from({ length: primeira.totalPaginas - 1 }, (_, indice) =>
        requisitar<Pagina<PlanoResumo>>(
          `/v1/planos?${pagina(indice + 1, 100)}`,
        ),
      ),
    );
    return {
      ...primeira,
      conteudo: [
        ...primeira.conteudo,
        ...restantes.flatMap((paginaAtual) => paginaAtual.conteudo),
      ],
    };
  },
  buscar: (id: UUID) => requisitar<Plano>(`/v1/planos/${id}`),
  criar: (entrada: PlanoEntrada, imagem?: File) => imagem
    ? requisitar<Plano>("/v1/planos", { method: "POST", body: dadosPlano(entrada, imagem) })
    : requisitar<Plano>("/v1/planos", { method: "POST", body: JSON.stringify(entrada) }),
  atualizar: (id: UUID, entrada: PlanoEntrada, imagem?: File, removerImagem = false) =>
    imagem || removerImagem
      ? requisitar<Plano>(`/v1/planos/${id}?removerImagem=${removerImagem}`, {
          method: "PUT",
          body: dadosPlano(entrada, imagem),
        })
      : requisitar<Plano>(`/v1/planos/${id}`, { method: "PUT", body: JSON.stringify(entrada) }),
  buscarImagem: (id: UUID) => obterArquivo(`/v1/planos/${id}/imagem`),
  salvarImagem: (id: UUID, imagem: File) => {
    const dados = new FormData();
    dados.append("imagem", imagem);
    return requisitar<void>(`/v1/planos/${id}/imagem`, { method: "PUT", body: dados });
  },
  removerImagem: (id: UUID) => requisitar<void>(`/v1/planos/${id}/imagem`, { method: "DELETE" }),
  concluir: (id: UUID) => requisitar<void>(`/v1/planos/${id}/conclusao`, { method: "PATCH" }),
  excluir: (id: UUID) => requisitar<void>(`/v1/planos/${id}`, { method: "DELETE" }),
};

export const participantesApi = {
  listar: (planoId: UUID, numero = 0) =>
    requisitar<Pagina<ParticipanteApi>>(
      `/v1/planos/${planoId}/participantes?${pagina(numero)}`,
    ).then(normalizarPaginaParticipantes),
  listarTodos: (planoId: UUID) =>
    requisitar<Pagina<ParticipanteApi>>(
      `/v1/planos/${planoId}/participantes?${pagina(0, 100)}`,
    ).then(normalizarPaginaParticipantes),
  buscarImagem: (planoId: UUID, participanteId: UUID) =>
    obterArquivo(`/v1/planos/${planoId}/participantes/${participanteId}/imagem`),
  adicionar: (planoId: UUID, email: string, papel: PapelPlano) =>
    requisitar<ParticipanteApi>(`/v1/planos/${planoId}/participantes`, {
      method: "POST",
      body: JSON.stringify({ email, papel }),
    }).then(normalizarParticipante),
  atualizar: (planoId: UUID, participanteId: UUID, papel: PapelPlano) =>
    requisitar<ParticipanteApi>(`/v1/planos/${planoId}/participantes/${participanteId}`, {
      method: "PUT",
      body: JSON.stringify({ papel }),
    }).then(normalizarParticipante),
  remover: (planoId: UUID, participanteId: UUID) =>
    requisitar<void>(`/v1/planos/${planoId}/participantes/${participanteId}`, { method: "DELETE" }),
  definirSuperiores: (planoId: UUID, superiorN1Email: string, superiorN2Email: string) =>
    requisitar<SuperioresPlano>(`/v1/planos/${planoId}/participantes/superiores`, {
      method: "PUT",
      body: JSON.stringify({ superiorN1Email: superiorN1Email || null, superiorN2Email: superiorN2Email || null }),
    }),
};

export const configuracaoPlanoApi = {
  listarClassificacoes: (planoId: UUID) =>
    requisitar<ConfiguracaoClassificacao[]>(`/v1/planos/${planoId}/configuracao/classificacoes`),
  atualizarClassificacoes: (planoId: UUID, classificacoes: Omit<ConfiguracaoClassificacao, "id">[]) =>
    requisitar<ConfiguracaoClassificacao[]>(`/v1/planos/${planoId}/configuracao/classificacoes`, {
      method: "PUT",
      body: JSON.stringify({ classificacoes }),
    }),
  listarFeriados: (planoId: UUID) =>
    requisitar<FeriadoPlano[]>(`/v1/planos/${planoId}/configuracao/feriados`),
  adicionarFeriado: (planoId: UUID, entrada: { data: string; nome: string }) =>
    requisitar<FeriadoPlano>(`/v1/planos/${planoId}/configuracao/feriados`, {
      method: "POST",
      body: JSON.stringify(entrada),
    }),
  removerFeriado: (planoId: UUID, feriadoId: UUID) =>
    requisitar<void>(`/v1/planos/${planoId}/configuracao/feriados/${feriadoId}`, {
      method: "DELETE",
    }),
};

export const atividadesPlanoApi = {
  listar: (planoId: UUID, numero = 0) =>
    requisitar<Pagina<AtividadePlano>>(`/v1/planos/${planoId}/atividades?${pagina(numero, 30)}`),
};

export const documentosApi = {
  listar: (planoId: UUID, classificacao?: ClassificacaoDocumento, numero = 0) => {
    const filtro = classificacao ? `&classificacao=${classificacao}` : "";
    return requisitar<Pagina<Documento>>(`/v1/planos/${planoId}/documentos?${pagina(numero)}${filtro}`);
  },
  listarAuditados: (planoId: UUID) =>
    requisitar<Pagina<Documento>>(`/v1/planos/${planoId}/documentos?${pagina(0, 100)}&classificacao=AUDITADO`),
  listarTodos: (planoId: UUID, classificacao: ClassificacaoDocumento) =>
    requisitar<Pagina<Documento>>(
      `/v1/planos/${planoId}/documentos?${pagina(0, 100)}&classificacao=${classificacao}`,
    ),
  adicionar: (
    planoId: UUID,
    entrada: { nome: string; versao: string; arquivo: File; classificacao: ClassificacaoDocumento },
  ) => {
    const dados = new FormData();
    dados.append("nome", entrada.nome);
    dados.append("versao", entrada.versao);
    dados.append("arquivo", entrada.arquivo);
    dados.append("classificacao", entrada.classificacao);
    return requisitar<Documento>(`/v1/planos/${planoId}/documentos`, { method: "POST", body: dados });
  },
  atualizar: (
    planoId: UUID,
    documentoId: UUID,
    entrada: { nome: string; versao: string; arquivo?: File | null },
  ) => {
    const dados = new FormData();
    dados.append("nome", entrada.nome);
    dados.append("versao", entrada.versao);
    if (entrada.arquivo) dados.append("arquivo", entrada.arquivo);
    return requisitar<Documento>(`/v1/planos/${planoId}/documentos/${documentoId}`, {
      method: "PUT",
      body: dados,
    });
  },
  baixar: (planoId: UUID, documento: Documento) =>
    baixarArquivo(`/v1/planos/${planoId}/documentos/${documento.id}/arquivo`, documento.nomeArquivo),
  abrir: (planoId: UUID, documentoId: UUID) =>
    obterArquivo(`/v1/planos/${planoId}/documentos/${documentoId}/arquivo`),
  remover: (planoId: UUID, documentoId: UUID) =>
    requisitar<void>(`/v1/planos/${planoId}/documentos/${documentoId}`, { method: "DELETE" }),
};

export const artefatosApi = {
  listar: (planoId: UUID, numero = 0) =>
    requisitar<Pagina<Artefato>>(`/v1/planos/${planoId}/artefatos?${pagina(numero)}`),
  listarTodos: (planoId: UUID) =>
    requisitar<Pagina<Artefato>>(`/v1/planos/${planoId}/artefatos?${pagina(0, 100)}`),
  buscar: (planoId: UUID, artefatoId: UUID) =>
    requisitar<Artefato>(`/v1/planos/${planoId}/artefatos/${artefatoId}`),
  criar: (planoId: UUID, entrada: ArtefatoEntrada) =>
    requisitar<Artefato>(`/v1/planos/${planoId}/artefatos`, {
      method: "POST",
      body: JSON.stringify(entrada),
    }),
  remover: (planoId: UUID, artefatoId: UUID) =>
    requisitar<void>(`/v1/planos/${planoId}/artefatos/${artefatoId}`, { method: "DELETE" }),
};

const caminhoAuditorias = (planoId: UUID, artefatoId: UUID) =>
  `/v1/planos/${planoId}/artefatos/${artefatoId}/auditorias`;

export const auditoriasApi = {
  listar: (planoId: UUID, artefatoId: UUID, numero = 0) =>
    requisitar<Pagina<AuditoriaResumo>>(`${caminhoAuditorias(planoId, artefatoId)}?${pagina(numero)}`),
  buscar: (planoId: UUID, artefatoId: UUID, auditoriaId: UUID) =>
    requisitar<Auditoria>(`${caminhoAuditorias(planoId, artefatoId)}/${auditoriaId}`),
  adicionarItem: (
    planoId: UUID,
    artefatoId: UUID,
    auditoriaId: UUID,
    checklistId: UUID,
    entrada: { ordem: number; pergunta: string },
  ) =>
    requisitar<ItemChecklist>(`${caminhoAuditorias(planoId, artefatoId)}/${auditoriaId}/checklists/${checklistId}/itens`, {
      method: "POST",
      body: JSON.stringify(entrada),
    }),
  atualizarItem: (
    planoId: UUID,
    artefatoId: UUID,
    auditoriaId: UUID,
    checklistId: UUID,
    itemId: UUID,
    entrada: { ordem: number; pergunta: string },
  ) =>
    requisitar<ItemChecklist>(
      `${caminhoAuditorias(planoId, artefatoId)}/${auditoriaId}/checklists/${checklistId}/itens/${itemId}`,
      { method: "PUT", body: JSON.stringify(entrada) },
    ),
  removerItem: (
    planoId: UUID,
    artefatoId: UUID,
    auditoriaId: UUID,
    checklistId: UUID,
    itemId: UUID,
  ) =>
    requisitar<void>(
      `${caminhoAuditorias(planoId, artefatoId)}/${auditoriaId}/checklists/${checklistId}/itens/${itemId}`,
      { method: "DELETE" },
    ),
  responder: (
    planoId: UUID,
    artefatoId: UUID,
    auditoriaId: UUID,
    itemId: UUID,
    resultado: ResultadoItem,
    observacao: string,
  ) =>
    requisitar<ItemAuditoria>(`${caminhoAuditorias(planoId, artefatoId)}/${auditoriaId}/respostas/${itemId}`, {
      method: "PUT",
      body: JSON.stringify({ resultado, observacao }),
    }),
  listarVersoesExecucao: (planoId: UUID, artefatoId: UUID, auditoriaId: UUID, checklistId: UUID) =>
    requisitar<VersaoExecucaoChecklist[]>(
      `${caminhoAuditorias(planoId, artefatoId)}/${auditoriaId}/checklists/${checklistId}/versoes-execucao`,
    ),
  salvarVersaoExecucao: (
    planoId: UUID,
    artefatoId: UUID,
    auditoriaId: UUID,
    checklistId: UUID,
    observacao: string,
  ) => requisitar<VersaoExecucaoChecklist>(
    `${caminhoAuditorias(planoId, artefatoId)}/${auditoriaId}/checklists/${checklistId}/versoes-execucao`,
    { method: "POST", body: JSON.stringify({ observacao }) },
  ),
  atualizarItemVersaoExecucao: (
    planoId: UUID,
    artefatoId: UUID,
    auditoriaId: UUID,
    checklistId: UUID,
    versaoId: UUID,
    itemId: UUID,
    entrada: {
      descricao: string;
      resultado: ResultadoItem | null;
      responsavelParticipacaoId: UUID | null;
      responsavelResolucao: string | null;
      classificacaoNc: ClassificacaoNaoConformidade | null;
      acaoCorretiva: string | null;
    },
  ) => requisitar<ItemVersaoExecucao>(
    `${caminhoAuditorias(planoId, artefatoId)}/${auditoriaId}/checklists/${checklistId}/versoes-execucao/${versaoId}/itens/${itemId}`,
      { method: "PUT", body: JSON.stringify(entrada) },
    ),
  removerItemVersaoExecucao: (
    planoId: UUID,
    artefatoId: UUID,
    auditoriaId: UUID,
    checklistId: UUID,
    versaoId: UUID,
    itemId: UUID,
  ) =>
    requisitar<VersaoExecucaoChecklist>(
      `${caminhoAuditorias(planoId, artefatoId)}/${auditoriaId}/checklists/${checklistId}/versoes-execucao/${versaoId}/itens/${itemId}`,
      { method: "DELETE" },
    ),
  autorizarConclusaoExcepcional: (
    planoId: UUID,
    artefatoId: UUID,
    auditoriaId: UUID,
    justificativa: string,
  ) => requisitar<Auditoria>(
    `${caminhoAuditorias(planoId, artefatoId)}/${auditoriaId}/autorizacao-conclusao`,
    { method: "POST", body: JSON.stringify({ justificativa }) },
  ),
  concluir: (planoId: UUID, artefatoId: UUID, auditoriaId: UUID) =>
    requisitar<Auditoria>(`${caminhoAuditorias(planoId, artefatoId)}/${auditoriaId}/conclusao`, {
      method: "POST",
    }),
};

export const minhasAuditoriasApi = {
  listarTodas: async () => {
    const primeira = await requisitar<Pagina<AuditoriaAgenda>>(
      `/v1/minhas-auditorias?${pagina(0, 100)}`,
    );
    if (primeira.totalPaginas <= 1) return primeira;
    const restantes = await Promise.all(
      Array.from({ length: primeira.totalPaginas - 1 }, (_, indice) =>
        requisitar<Pagina<AuditoriaAgenda>>(
          `/v1/minhas-auditorias?${pagina(indice + 1, 100)}`,
        ),
      ),
    );
    return {
      ...primeira,
      conteudo: [
        ...primeira.conteudo,
        ...restantes.flatMap((paginaAtual) => paginaAtual.conteudo),
      ],
    };
  },
};

const caminhoNaoConformidades = (planoId: UUID) => `/v1/planos/${planoId}/nao-conformidades`;

export const naoConformidadesApi = {
  listar: (planoId: UUID, numero = 0) =>
    requisitar<Pagina<NaoConformidade>>(`${caminhoNaoConformidades(planoId)}?${pagina(numero)}`),
  listarTodas: async (planoId: UUID) => {
    const primeira = await requisitar<Pagina<NaoConformidade>>(
      `${caminhoNaoConformidades(planoId)}?${pagina(0, 100)}`,
    );
    if (primeira.totalPaginas <= 1) return primeira;
    const restantes = await Promise.all(
      Array.from({ length: primeira.totalPaginas - 1 }, (_, indice) =>
        requisitar<Pagina<NaoConformidade>>(
          `${caminhoNaoConformidades(planoId)}?${pagina(indice + 1, 100)}`,
        ),
      ),
    );
    const conteudo = [primeira, ...restantes].flatMap((resultado) => resultado.conteudo);
    return { ...primeira, conteudo, tamanho: conteudo.length, totalElementos: conteudo.length, totalPaginas: 1, ultima: true };
  },
  buscar: (planoId: UUID, naoConformidadeId: UUID) =>
    requisitar<NaoConformidade>(`${caminhoNaoConformidades(planoId)}/${naoConformidadeId}`),
  criar: (planoId: UUID, entrada: NaoConformidadeEntrada) =>
    requisitar<NaoConformidade>(caminhoNaoConformidades(planoId), {
      method: "POST",
      body: JSON.stringify(entrada),
    }),
  criarRascunho: (planoId: UUID, entrada: NaoConformidadeEntrada) =>
    requisitar<NaoConformidade>(`${caminhoNaoConformidades(planoId)}/rascunhos`, {
      method: "POST",
      body: JSON.stringify(entrada),
    }),
  atualizar: (
    planoId: UUID,
    naoConformidadeId: UUID,
    entrada: Omit<NaoConformidadeEntrada, "respostaId">,
  ) => requisitar<NaoConformidade>(
    `${caminhoNaoConformidades(planoId)}/${naoConformidadeId}`,
    { method: "PUT", body: JSON.stringify(entrada) },
  ),
  encaminhar: (
    planoId: UUID,
    naoConformidadeId: UUID,
    chaveIdempotencia: string,
  ) => requisitar<EncaminhamentoNaoConformidade>(
    `${caminhoNaoConformidades(planoId)}/${naoConformidadeId}/encaminhamentos`,
    {
      method: "POST",
      headers: { "Idempotency-Key": chaveIdempotencia },
    },
  ),
  listarDaEquipeNoPlano: (planoId: UUID) =>
    requisitar<Pagina<NaoConformidade>>(`${caminhoNaoConformidades(planoId)}/equipe?${pagina(0, 100)}`),
  listarTodasDaEquipeNoPlano: async (planoId: UUID) => {
    const primeira = await requisitar<Pagina<NaoConformidade>>(
      `${caminhoNaoConformidades(planoId)}/equipe?${pagina(0, 100)}`,
    );
    if (primeira.totalPaginas <= 1) return primeira.conteudo;
    const restantes = await Promise.all(
      Array.from({ length: primeira.totalPaginas - 1 }, (_, indice) =>
        requisitar<Pagina<NaoConformidade>>(
          `${caminhoNaoConformidades(planoId)}/equipe?${pagina(indice + 1, 100)}`,
        ),
      ),
    );
    return [primeira, ...restantes].flatMap((resultado) => resultado.conteudo);
  },
  listarHistorico: (planoId: UUID, naoConformidadeId: UUID) =>
    requisitar<Pagina<EventoNaoConformidade>>(
      `${caminhoNaoConformidades(planoId)}/${naoConformidadeId}/historico?${pagina(0, 100)}`,
    ),
  alertarEquipe: (planoId: UUID, naoConformidadeId: UUID) =>
    requisitar<void>(
      `${caminhoNaoConformidades(planoId)}/${naoConformidadeId}/alertas-equipe`,
      { method: "POST" },
    ),
};

export const minhasNaoConformidadesApi = {
  listar: (numero = 0) =>
    requisitar<Pagina<NaoConformidade>>(
      `/v1/minhas-nao-conformidades?${pagina(numero)}`,
    ),
  listarAtribuidas: () =>
    requisitar<Pagina<NaoConformidade>>(
      `/v1/minhas-nao-conformidades/atribuidas?${pagina(0, 100)}`,
    ),
};

const caminhoResolucoes = (planoId: UUID, naoConformidadeId: UUID) =>
  `${caminhoNaoConformidades(planoId)}/${naoConformidadeId}/resolucoes`;

export const resolucoesApi = {
  listarTodas: (planoId: UUID, naoConformidadeId: UUID) =>
    requisitar<Pagina<Resolucao>>(`${caminhoResolucoes(planoId, naoConformidadeId)}?${pagina(0, 100)}`),
  informar: (planoId: UUID, naoConformidadeId: UUID, entrada: { descricao: string; evidencia: string }) =>
    requisitar<Resolucao>(caminhoResolucoes(planoId, naoConformidadeId), {
      method: "POST",
      body: JSON.stringify(entrada),
    }),
  validar: (
    planoId: UUID,
    naoConformidadeId: UUID,
    resolucaoId: UUID,
    decisao: DecisaoValidacao,
    observacao: string,
  ) =>
    requisitar<Resolucao>(`${caminhoResolucoes(planoId, naoConformidadeId)}/${resolucaoId}/validacao`, {
      method: "POST",
      body: JSON.stringify({ decisao, observacao }),
    }),
};

const caminhoEscalonamentos = (planoId: UUID, naoConformidadeId: UUID) =>
  `${caminhoNaoConformidades(planoId)}/${naoConformidadeId}/escalonamentos`;

export const escalonamentosApi = {
  listarTodos: (planoId: UUID, naoConformidadeId: UUID) =>
    requisitar<Pagina<Escalonamento>>(
      `${caminhoEscalonamentos(planoId, naoConformidadeId)}?${pagina(0, 100)}`,
    ),
  criar: (
    planoId: UUID,
    naoConformidadeId: UUID,
    chaveIdempotencia: string,
    entrada: { nivel: NivelEscalonamento; prazoHoras: number; observacao: string },
  ) =>
    requisitar<Escalonamento>(caminhoEscalonamentos(planoId, naoConformidadeId), {
      method: "POST",
      headers: { "Idempotency-Key": chaveIdempotencia },
      body: JSON.stringify(entrada),
    }),
};

export const escalonamentosPlanoApi = {
  listar: (planoId: UUID) =>
    requisitar<Pagina<EscalonamentoPainel>>(
      `/v1/planos/${planoId}/escalonamentos?${pagina(0, 100)}`,
    ),
  revisarPrazo: (planoId: UUID, escalonamentoId: UUID, prazoEm: string) =>
    requisitar<EscalonamentoPainel>(
      `/v1/planos/${planoId}/escalonamentos/${escalonamentoId}/prazo`,
      { method: "PATCH", body: JSON.stringify({ prazoEm }) },
    ),
};

export const meusEscalonamentosApi = {
  listar: () =>
    requisitar<Pagina<EscalonamentoPainel>>(
      `/v1/meus-escalonamentos?${pagina(0, 100)}`,
    ),
};

export const notificacoesApi = {
  listar: (numero = 0) => requisitar<Pagina<Notificacao>>(`/v1/notificacoes?${pagina(numero)}`),
  marcarLida: (id: UUID) => requisitar<Notificacao>(`/v1/notificacoes/${id}/leitura`, { method: "POST" }),
};
