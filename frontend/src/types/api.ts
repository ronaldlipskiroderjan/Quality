export type UUID = string;

export type StatusPlano = "PENDENTE" | "CONCLUIDO";
export type ClassificacaoDocumento = "REFERENCIA" | "AUDITADO";
export type PapelPlano =
  | "AUDITOR_RESPONSAVEL_QUALIDADE"
  | "MEMBRO_EQUIPE_RESOLUCAO"
  | "SUPERIOR_N1"
  | "SUPERIOR_N2";
export type PermissaoPlano =
  | "VISUALIZAR"
  | "EDITAR"
  | "CONCLUIR"
  | "EXCLUIR"
  | "GERENCIAR_PARTICIPANTES"
  | "GERENCIAR_DOCUMENTOS"
  | "GERENCIAR_ARTEFATOS"
  | "GERENCIAR_CHECKLISTS"
  | "AUDITAR"
  | "ESCALONAR_NAO_CONFORMIDADE"
  | "GERENCIAR_ESCALONAMENTOS"
  | "TRATAR_NAO_CONFORMIDADE";

export interface Pagina<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
  primeira: boolean;
  ultima: boolean;
}

export interface Usuario {
  id: UUID;
  nome: string;
  email: string;
  temImagem: boolean;
  roles: string[];
}

export interface Token {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
}

export interface PlanoResumo {
  id: UUID;
  nomeProjeto: string;
  versao: string;
  status: StatusPlano;
  criadoEm: string;
  temImagem: boolean;
  meusPapeis: PapelPlano[];
  minhasPermissoes: PermissaoPlano[];
}

export interface Plano extends PlanoResumo {
  objetivo: string;
  visaoGeral: string;
}

export interface PlanoEntrada {
  nomeProjeto: string;
  versao: string;
  objetivo: string;
  visaoGeral: string;
}

export interface Participante {
  id: UUID;
  usuarioId: UUID;
  nome: string;
  email: string;
  temImagem: boolean;
  papel: PapelPlano | null;
  permissoes: PermissaoPlano[];
  criadoEm: string;
}

export interface Documento {
  id: UUID;
  nome: string;
  nomeArquivo: string;
  versao: string;
  tipoArquivo: string;
  tamanho: number;
  classificacao: ClassificacaoDocumento;
}

export type StatusArtefato = "PLANEJADO" | "EM_PREPARACAO" | "EM_ANDAMENTO" | "PAUSADO" | "CONCLUIDO" | "CANCELADO";

export interface Artefato {
  id: UUID;
  documentoId: UUID;
  documentoNome: string;
  nome: string;
  versao: string;
  dataPlanejada: string;
  status: StatusArtefato;
  auditorParticipacaoId: UUID;
  auditorUsuarioId: UUID;
  auditorNome: string;
  auditorEmail: string;
  auditoriaId: UUID;
  documentosReferencia: Documento[];
  totalChecklists: number;
  criadoEm: string;
}

export interface ArtefatoEntrada {
  documentoId: UUID;
  auditorParticipacaoId: UUID;
  documentoReferenciaIds: UUID[];
  nome: string;
  versao: string;
  dataPlanejada: string;
}

export interface AuditoriaAgenda {
  artefatoId: UUID;
  auditoriaId: UUID;
  planoId: UUID;
  planoNome: string;
  artefatoNome: string;
  versao: string;
  dataPlanejada: string;
  status: StatusArtefato;
  auditorNome: string;
}

export type StatusChecklist = "RASCUNHO" | "PUBLICADO" | "CONCLUIDO" | "ARQUIVADO";

export interface ItemChecklist {
  id: UUID;
  ordem: number;
  pergunta: string;
  origem: "MANUAL" | "IA";
  criadoEm: string;
}

export interface ChecklistResumo {
  id: UUID;
  auditoriaId: UUID;
  artefatoId: UUID;
  versao: string;
  status: StatusChecklist;
  totalItens: number;
  criadoEm: string;
  atualizadoEm: string;
  versaoRegistro: number;
}

export interface Checklist extends Omit<ChecklistResumo, "totalItens"> {
  itens: ItemChecklist[];
}

export type ResultadoItem = "CONFORME" | "NAO_CONFORME" | "NAO_APLICAVEL";
export type StatusAuditoria =
  | "PLANEJADA"
  | "EM_PREPARACAO"
  | "EM_ANDAMENTO"
  | "PAUSADA"
  | "CONCLUIDA"
  | "CANCELADA";

export interface ItemAuditoria {
  itemId: UUID;
  respostaId: UUID | null;
  ordem: number;
  pergunta: string;
  resultado: ResultadoItem | null;
  observacao: string | null;
  respondidoEm: string | null;
  atualizadoEm: string | null;
  versaoRegistro: number | null;
}

export interface AuditoriaResumo {
  id: UUID;
  artefatoId: UUID;
  auditorParticipacaoId: UUID;
  auditorNome: string;
  status: StatusAuditoria;
  totalChecklists: number;
  totalItens: number;
  totalRespondidos: number;
  conformes: number;
  naoConformes: number;
  naoAplicaveis: number;
  aderenciaPercentual: number | null;
  dataInicio: string;
  dataFim: string | null;
  versaoRegistro: number;
  documentosReferencia: Documento[];
  checklists: ChecklistResumo[];
}

export interface ChecklistAuditoria extends ChecklistResumo {
  itens: ItemAuditoria[];
}

export interface Auditoria extends AuditoriaResumo {
  checklists: ChecklistAuditoria[];
  conclusaoExcepcionalAutorizadaPorId: UUID | null;
  conclusaoExcepcionalAutorizadaPorNome: string | null;
  conclusaoExcepcionalAutorizadaEm: string | null;
  justificativaConclusaoExcepcional: string | null;
}

export type ClassificacaoNaoConformidade = "SIMPLES" | "COMPLEXA" | "SEVERA" | "EXTREMA";
export type StatusNaoConformidade =
  | "RASCUNHO"
  | "ENVIADA"
  | "EM_TRATAMENTO"
  | "RESOLUCAO_INFORMADA"
  | "VENCIDA"
  | "ESCALONADA_N1"
  | "VENCIDA_N1"
  | "ESCALONADA_N2"
  | "VENCIDA_N2"
  | "CONCLUIDA"
  | "CANCELADA";

export interface NaoConformidadeEntrada {
  respostaId: UUID;
  responsavelParticipacaoId: UUID | null;
  classificacao: ClassificacaoNaoConformidade;
  acaoCorretiva: string;
}

export interface NaoConformidade extends NaoConformidadeEntrada {
  id: UUID;
  planoId: UUID;
  planoNome: string;
  auditoriaId: UUID;
  artefatoId: UUID;
  artefatoNome: string;
  itemId: UUID;
  itemOrdem: number;
  pergunta: string;
  auditorParticipacaoId: UUID;
  auditorNome: string;
  responsavelNome: string | null;
  responsavelEmail: string | null;
  status: StatusNaoConformidade;
  identificadoEm: string;
  enviadaEm: string | null;
  prazoEm: string | null;
  ultimoEscalonamentoEm: string | null;
  concluidaEm: string | null;
  atualizadoEm: string;
  versaoRegistro: number;
}

export interface EncaminhamentoNaoConformidade {
  id: UUID;
  naoConformidadeId: UUID;
  totalDestinatarios: number;
  encaminhadoEm: string;
  versaoRegistro: number;
}

export type TipoVersaoExecucaoChecklist = "ABERTURA" | "MANUAL" | "ENCERRAMENTO";

export interface ItemVersaoExecucao {
  id: UUID;
  ordem: number;
  descricao: string;
  resultado: ResultadoItem | null;
  observacao: string | null;
  ncIdentificadaEm: string | null;
  responsavelParticipacaoId: UUID | null;
  responsavelResolucao: string | null;
  classificacaoNc: ClassificacaoNaoConformidade | null;
  acaoCorretiva: string | null;
  prazoResolucaoEm: string | null;
  escalonadoEm: string | null;
  ncConcluidaEm: string | null;
  statusNc: StatusNaoConformidade | null;
}

export interface VersaoExecucaoChecklist {
  id: UUID;
  checklistId: UUID;
  numero: number;
  tipo: TipoVersaoExecucaoChecklist;
  observacao: string | null;
  autorNome: string;
  criadoEm: string;
  itens: ItemVersaoExecucao[];
}


export type StatusResolucao = "INFORMADA" | "APROVADA" | "AJUSTES_SOLICITADOS";
export type DecisaoValidacao = "APROVAR" | "SOLICITAR_AJUSTES";

export interface Resolucao {
  id: UUID;
  naoConformidadeId: UUID;
  responsavelParticipacaoId: UUID;
  responsavelNome: string;
  descricao: string;
  evidencia: string | null;
  status: StatusResolucao;
  informadaEm: string;
  auditorParticipacaoId: UUID | null;
  auditorNome: string | null;
  observacaoAuditor: string | null;
  validadaEm: string | null;
  versaoRegistro: number;
}

export type NivelEscalonamento = "N1" | "N2";

export interface Escalonamento {
  id: UUID;
  naoConformidadeId: UUID;
  nivel: NivelEscalonamento;
  responsavelParticipacaoId: UUID;
  responsavelNome: string;
  responsavelEmail: string;
  auditorParticipacaoId: UUID;
  auditorNome: string;
  observacao: string | null;
  prazoHoras: number;
  escalonadoEm: string;
  prazoOriginalEm: string;
  prazoEm: string;
  revisadoEm: string | null;
  versaoRegistro: number;
}

export interface EscalonamentoPainel {
  id: UUID;
  naoConformidadeId: UUID;
  planoId: UUID;
  planoNome: string;
  nivel: NivelEscalonamento;
  artefatoId: UUID;
  artefatoNome: string;
  itemOrdem: number;
  pergunta: string;
  responsavelResolucaoNome: string | null;
  statusNaoConformidade: StatusNaoConformidade;
  escalonadoEm: string;
  prazoOriginalEm: string;
  prazoEm: string;
  revisadoEm: string | null;
  versaoRegistro: number;
}

export interface EventoNaoConformidade {
  referenciaId: UUID;
  tipo: string;
  ocorridoEm: string;
  titulo: string;
  detalhe: string | null;
}

export interface Notificacao {
  id: UUID;
  planoId: UUID;
  naoConformidadeId: UUID;
  planoNome: string;
  artefatoNome: string;
  tipo: string;
  titulo: string;
  mensagem: string;
  status: "NAO_LIDA" | "LIDA";
  criadaEm: string;
  lidaEm: string | null;
  versaoRegistro: number;
}

export interface ConfiguracaoClassificacao {
  id: UUID;
  classificacao: ClassificacaoNaoConformidade;
  prazoDias: number;
  prazoHoras: number;
  ativa: boolean;
}

export interface FeriadoPlano {
  id: UUID;
  data: string;
  nome: string;
}

export interface SuperioresPlano {
  superiorN1: Participante | null;
  superiorN2: Participante | null;
}

export interface AtividadePlano {
  id: UUID;
  autorParticipacaoId: UUID;
  autorNome: string;
  acao: string;
  descricao: string;
  criadoEm: string;
}

export interface ProblemaApi {
  title?: string;
  detail?: string;
  codigo?: string;
  correlationId?: string;
  campos?: Record<string, string>;
}
