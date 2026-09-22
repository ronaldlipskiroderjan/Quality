import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from "react";
import { Link, useOutletContext, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { EmptyState, ErrorMessage, LoadingState, SuccessMessage } from "../components/Feedback";
import { useConfirm } from "../components/ConfirmProvider";
import { Modal } from "../components/Modal";
import { PageHeader } from "../components/PageHeader";
import type { PlanOutletContext } from "../layouts/PlanLayout";
import { ApiError, mensagemErro } from "../lib/api";
import { calcularResumoAuditoria } from "../lib/audit";
import { formatarData, rotuloEnum, rotuloStatusNaoConformidade } from "../lib/format";
import { auditoriasApi, configuracaoPlanoApi, naoConformidadesApi, participantesApi } from "../services/qualityApi";
import type {
  Auditoria,
  ClassificacaoNaoConformidade,
  ConfiguracaoClassificacao,
  ItemAuditoria,
  ItemVersaoExecucao,
  NaoConformidade,
  Participante,
  ResultadoItem,
  VersaoExecucaoChecklist,
} from "../types/api";

const resultados: { valor: ResultadoItem; rotulo: string }[] = [
  { valor: "CONFORME", rotulo: "Conforme" },
  { valor: "NAO_CONFORME", rotulo: "Não conforme" },
  { valor: "NAO_APLICAVEL", rotulo: "N/A" },
];

const statusNcFinais = new Set(["CONCLUIDA", "CANCELADA"]);

function mensagemErroExecucao(error: unknown): string {
  if (
    error instanceof ApiError
    && error.status === 404
    && (
      error.problema?.codigo === "ENDPOINT_NAO_ENCONTRADO"
      || error.message.includes("No static resource")
      || error.message.includes("versoes-execucao")
    )
  ) {
    return "O servidor da API ainda não possui esta versão do fluxo de auditoria. Reinicie ou publique novamente o backend e tente outra vez.";
  }
  return mensagemErro(error);
}

interface ItemAuditoriaProps {
  item: ItemAuditoria;
  editavel: boolean;
  removendo: boolean;
  naoConformidade?: NaoConformidade;
  responsaveis: Participante[];
  classificacoes: ConfiguracaoClassificacao[];
  aoSalvar: (item: ItemAuditoria) => void;
  aoAtualizarPergunta: (itemId: string, pergunta: string) => void;
  aoAtualizarNaoConformidade: (naoConformidade: NaoConformidade) => void;
  aoRemoverNaoConformidade: (respostaId: string) => void;
  aoMudarSalvamento: (itemId: string, salvando: boolean) => void;
  aoRemover: (item: ItemAuditoria) => void;
  planoId: string;
  artefatoId: string;
  auditoriaId: string;
  checklistId: string;
}

function CelulaDado({ children, rotulo }: { children: ReactNode; rotulo: string }) {
  return <div className="audit-data-cell" role="gridcell" data-label={rotulo}>{children || <span className="audit-empty">—</span>}</div>;
}

function ItemAuditoriaForm({
  item,
  editavel,
  removendo,
  naoConformidade,
  responsaveis,
  classificacoes,
  aoSalvar,
  aoAtualizarPergunta,
  aoAtualizarNaoConformidade,
  aoRemoverNaoConformidade,
  aoMudarSalvamento,
  aoRemover,
  planoId,
  artefatoId,
  auditoriaId,
  checklistId,
}: ItemAuditoriaProps) {
  const [pergunta, setPergunta] = useState(item.pergunta);
  const [resultado, setResultado] = useState<ResultadoItem | "">(item.resultado ?? "");
  const [responsavelId, setResponsavelId] = useState(naoConformidade?.responsavelParticipacaoId ?? "");
  const [classificacao, setClassificacao] = useState<ClassificacaoNaoConformidade | "">(naoConformidade?.classificacao ?? classificacoes[0]?.classificacao ?? "");
  const [acaoCorretiva, setAcaoCorretiva] = useState(naoConformidade?.acaoCorretiva ?? "");
  const [editandoPergunta, setEditandoPergunta] = useState(item.pergunta.trim() === "");
  const [editandoResultado, setEditandoResultado] = useState(false);
  const [editandoResponsavel, setEditandoResponsavel] = useState(false);
  const [editandoClassificacao, setEditandoClassificacao] = useState(false);
  const [editandoAcao, setEditandoAcao] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const chaveEncaminhamento = useRef(crypto.randomUUID());
  const salvamentosAtivos = useRef(0);
  const filaDadosNc = useRef<Promise<void>>(Promise.resolve());
  const versaoDadosNc = useRef(0);
  const celulaEmEdicao = useRef<"PERGUNTA" | "RESULTADO" | "RESPONSAVEL" | "CLASSIFICACAO" | "ACAO" | null>(null);

  useEffect(() => {
    if (celulaEmEdicao.current !== "PERGUNTA") setPergunta(item.pergunta);
    setResultado(item.resultado ?? "");
    if (!item.pergunta.trim()) {
      celulaEmEdicao.current = "PERGUNTA";
      setEditandoPergunta(true);
    }
  }, [item]);

  useEffect(() => {
    if (celulaEmEdicao.current !== "RESPONSAVEL") {
      setResponsavelId(naoConformidade?.responsavelParticipacaoId ?? "");
    }
    if (celulaEmEdicao.current !== "CLASSIFICACAO") {
      setClassificacao(naoConformidade?.classificacao ?? classificacoes[0]?.classificacao ?? "");
    }
    if (celulaEmEdicao.current !== "ACAO") {
      setAcaoCorretiva(naoConformidade?.acaoCorretiva ?? "");
    }
  }, [classificacoes, naoConformidade]);

  const rascunho = naoConformidade?.status === "RASCUNHO";
  const respostaBloqueada = Boolean(
    naoConformidade
    && !rascunho
    && !statusNcFinais.has(naoConformidade.status),
  );
  const identificadaEm = naoConformidade?.identificadoEm
    ?? (resultado === "NAO_CONFORME" ? item.respondidoEm : null);
  const responsavelSelecionado = responsaveis.find((responsavel) => responsavel.id === responsavelId);
  const perguntaPreenchida = pergunta.trim() !== "";

  function iniciarSalvamento() {
    salvamentosAtivos.current += 1;
    if (salvamentosAtivos.current === 1) {
      setSalvando(true);
      aoMudarSalvamento(item.itemId, true);
    }
    setErro("");
  }

  function finalizarSalvamento() {
    salvamentosAtivos.current = Math.max(0, salvamentosAtivos.current - 1);
    if (salvamentosAtivos.current === 0) {
      setSalvando(false);
      aoMudarSalvamento(item.itemId, false);
    }
  }

  async function criarRascunho(respostaId: string) {
    const classificacaoInicial = classificacoes[0]?.classificacao;
    if (!classificacaoInicial) {
      throw new Error("Defina ao menos uma classificação ativa na visão geral do plano.");
    }
    const registro = await naoConformidadesApi.criarRascunho(planoId, {
      respostaId,
      responsavelParticipacaoId: null,
      classificacao: classificacaoInicial,
      acaoCorretiva: "",
    });
    aoAtualizarNaoConformidade(registro);
  }

  async function alterarResultado(novoResultado: ResultadoItem) {
    const resultadoAnterior = resultado;
    let respostaSalva = false;
    setResultado(novoResultado);
    iniciarSalvamento();
    try {
      const resposta = await auditoriasApi.responder(
        planoId,
        artefatoId,
        auditoriaId,
        item.itemId,
        novoResultado,
        "",
      );
      aoSalvar(resposta);
      respostaSalva = true;
      if (novoResultado === "NAO_CONFORME" && !naoConformidade) {
        await criarRascunho(resposta.respostaId!);
      } else if (novoResultado !== "NAO_CONFORME" && rascunho && item.respostaId) {
        aoRemoverNaoConformidade(item.respostaId);
      }
    } catch (error) {
      if (!respostaSalva) setResultado(resultadoAnterior);
      setErro(mensagemErro(error));
    } finally {
      finalizarSalvamento();
    }
  }

  async function tentarCriarRascunho() {
    if (!item.respostaId) return;
    iniciarSalvamento();
    try {
      await criarRascunho(item.respostaId);
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      finalizarSalvamento();
    }
  }

  async function salvarPergunta() {
    const perguntaNormalizada = pergunta.trim();
    if (!perguntaNormalizada) {
      setPergunta(item.pergunta);
      setErro("A pergunta não pode ficar vazia.");
      return;
    }
    if (perguntaNormalizada === item.pergunta) return;
    iniciarSalvamento();
    try {
      const atualizada = await auditoriasApi.atualizarItem(
        planoId,
        artefatoId,
        auditoriaId,
        checklistId,
        item.itemId,
        {
          ordem: item.ordem,
          pergunta: perguntaNormalizada,
        },
      );
      setPergunta(atualizada.pergunta);
      aoAtualizarPergunta(item.itemId, atualizada.pergunta);
    } catch (error) {
      setPergunta(item.pergunta);
      setErro(mensagemErro(error));
    } finally {
      finalizarSalvamento();
    }
  }

  async function salvarDadosNc(
    proximos: {
      responsavelParticipacaoId: string;
      classificacao: ClassificacaoNaoConformidade | "";
      acaoCorretiva: string;
    },
  ) {
    if (!naoConformidade || !rascunho || !proximos.classificacao) return;
    setResponsavelId(proximos.responsavelParticipacaoId);
    setClassificacao(proximos.classificacao);
    setAcaoCorretiva(proximos.acaoCorretiva);
    const versaoSalvamento = ++versaoDadosNc.current;
    iniciarSalvamento();
    const tarefa = filaDadosNc.current
      .catch(() => undefined)
      .then(async () => {
        const atualizada = await naoConformidadesApi.atualizar(
          planoId,
          naoConformidade.id,
          {
            responsavelParticipacaoId: proximos.responsavelParticipacaoId || null,
            classificacao: proximos.classificacao as ClassificacaoNaoConformidade,
            acaoCorretiva: proximos.acaoCorretiva.trim(),
          },
        );
        if (versaoSalvamento === versaoDadosNc.current) {
          aoAtualizarNaoConformidade(atualizada);
        }
      });
    filaDadosNc.current = tarefa;
    try {
      await tarefa;
    } catch (error) {
      if (versaoSalvamento === versaoDadosNc.current) {
        setResponsavelId(naoConformidade.responsavelParticipacaoId ?? "");
        setClassificacao(naoConformidade.classificacao);
        setAcaoCorretiva(naoConformidade.acaoCorretiva);
      }
      setErro(mensagemErro(error));
    } finally {
      finalizarSalvamento();
    }
  }

  async function enviarParaResolucao() {
    if (!naoConformidade || !rascunho) return;
    if (!classificacao) {
      setErro("Selecione uma classificação antes de enviar para resolução.");
      return;
    }
    if (!acaoCorretiva.trim()) {
      setErro("Informe a ação corretiva antes de enviar para resolução.");
      return;
    }
    iniciarSalvamento();
    try {
      await filaDadosNc.current.catch(() => undefined);
      const atualizada = await naoConformidadesApi.atualizar(
        planoId,
        naoConformidade.id,
        {
          responsavelParticipacaoId: responsavelId || null,
          classificacao,
          acaoCorretiva: acaoCorretiva.trim(),
        },
      );
      await naoConformidadesApi.encaminhar(
        planoId,
        atualizada.id,
        chaveEncaminhamento.current,
      );
      aoAtualizarNaoConformidade(
        await naoConformidadesApi.buscar(planoId, atualizada.id),
      );
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      finalizarSalvamento();
    }
  }

  return (
    <article className="audit-item" data-result={resultado || "PENDENTE"}>
      <div className="audit-checklist-row" role="row">
        <div className="audit-row-number" role="gridcell" aria-label={`Item ${item.ordem}`}>
          <span>{item.ordem}</span>
          {editavel && (
            <button
              className="audit-remove-row"
              type="button"
              aria-label={`Remover pergunta ${item.ordem}`}
              title="Remover pergunta"
              disabled={salvando || removendo}
              onClick={() => aoRemover(item)}
            >
              <span aria-hidden="true">−</span>
            </button>
          )}
        </div>
        <div className="audit-row-question" role="gridcell">
          {editavel && editandoPergunta ? (
            <label>
              <span className="visually-hidden">Pergunta do item {item.ordem}</span>
              <textarea
                autoFocus
                rows={2}
                maxLength={1000}
                value={pergunta}
                onChange={(event) => setPergunta(event.target.value)}
                onBlur={() => {
                  celulaEmEdicao.current = null;
                  setEditandoPergunta(false);
                  void salvarPergunta();
                }}
              />
            </label>
          ) : editavel ? (
            <button className="audit-cell-trigger audit-question-trigger" type="button" onClick={() => { celulaEmEdicao.current = "PERGUNTA"; setEditandoPergunta(true); }}>
              {pergunta || "Clique para escrever a pergunta"}
            </button>
          ) : <strong>{item.pergunta}</strong>}
        </div>
        <div className="audit-result-cell" role="gridcell" data-label="Resultado">
          {!perguntaPreenchida ? (
            <span className="audit-empty">Preencha a pergunta</span>
          ) : editavel && !respostaBloqueada && editandoResultado ? (
            <select
              autoFocus
              className="audit-inline-control"
              aria-label={`Resultado do item ${item.ordem}`}
              value={resultado}
              onBlur={() => { celulaEmEdicao.current = null; setEditandoResultado(false); }}
              onChange={(event) => {
                const novoResultado = event.target.value as ResultadoItem;
                celulaEmEdicao.current = null;
                setEditandoResultado(false);
                if (novoResultado) void alterarResultado(novoResultado);
              }}
            >
              <option value="" disabled>Selecione</option>
              {resultados.map((opcao) => <option key={opcao.valor} value={opcao.valor}>{opcao.rotulo}</option>)}
            </select>
          ) : editavel && !respostaBloqueada ? (
            <button className="audit-cell-trigger" type="button" onClick={() => { celulaEmEdicao.current = "RESULTADO"; setEditandoResultado(true); }}>
              {resultado ? rotuloEnum(resultado) : "Clique para responder"}
            </button>
          ) : <span>{resultado ? rotuloEnum(resultado) : "—"}</span>}
        </div>
        <CelulaDado rotulo="Identificação da NC">{formatarData(identificadaEm)}</CelulaDado>
        <CelulaDado rotulo="Responsável">
          {rascunho && editavel && editandoResponsavel ? (
            <select
              autoFocus
              className="audit-inline-control"
              aria-label={`Responsável pela resolução do item ${item.ordem}`}
              value={responsavelId}
              onChange={(event) => setResponsavelId(event.target.value)}
              onBlur={(event) => {
                celulaEmEdicao.current = null;
                setEditandoResponsavel(false);
                const novoResponsavel = event.currentTarget.value;
                if (novoResponsavel !== (naoConformidade.responsavelParticipacaoId ?? "")) {
                  void salvarDadosNc({ responsavelParticipacaoId: novoResponsavel, classificacao, acaoCorretiva });
                }
              }}
            >
              <option value="">Toda a equipe</option>
              {responsaveis.map((responsavel) => (
                <option key={responsavel.id} value={responsavel.id}>{responsavel.nome}</option>
              ))}
            </select>
          ) : rascunho && editavel ? (
            <button className="audit-cell-trigger" type="button" onClick={() => { celulaEmEdicao.current = "RESPONSAVEL"; setEditandoResponsavel(true); }}>
              {responsavelSelecionado?.nome ?? "Toda a equipe"}
            </button>
          ) : naoConformidade?.responsavelNome ?? (naoConformidade ? "Equipe de resolução" : "—")}
        </CelulaDado>
        <CelulaDado rotulo="Classificação">
          {rascunho && editavel && editandoClassificacao ? (
            <select
              autoFocus
              className="audit-inline-control"
              aria-label={`Classificação da não conformidade do item ${item.ordem}`}
              value={classificacao}
              onChange={(event) => setClassificacao(event.target.value as ClassificacaoNaoConformidade)}
              onBlur={(event) => {
                celulaEmEdicao.current = null;
                setEditandoClassificacao(false);
                const novaClassificacao = event.currentTarget.value as ClassificacaoNaoConformidade;
                if (novaClassificacao !== naoConformidade.classificacao) {
                  void salvarDadosNc({ responsavelParticipacaoId: responsavelId, classificacao: novaClassificacao, acaoCorretiva });
                }
              }}
            >
              {classificacoes.map((configuracao) => (
                <option key={configuracao.id} value={configuracao.classificacao}>{rotuloEnum(configuracao.classificacao)}</option>
              ))}
            </select>
          ) : rascunho && editavel ? (
            <button className="audit-cell-trigger" type="button" onClick={() => { celulaEmEdicao.current = "CLASSIFICACAO"; setEditandoClassificacao(true); }}>
              {classificacao ? rotuloEnum(classificacao) : "Selecionar"}
            </button>
          ) : naoConformidade ? rotuloEnum(naoConformidade.classificacao) : "—"}
        </CelulaDado>
        <CelulaDado rotulo="Ação corretiva">
          {rascunho && editavel && editandoAcao ? (
            <input
              autoFocus
              className="audit-inline-control"
              aria-label={`Ação corretiva do item ${item.ordem}`}
              maxLength={3000}
              value={acaoCorretiva}
              placeholder="Informe a ação"
              onChange={(event) => setAcaoCorretiva(event.target.value)}
              onBlur={(event) => {
                celulaEmEdicao.current = null;
                setEditandoAcao(false);
                const novaAcao = event.currentTarget.value;
                if (novaAcao.trim() !== naoConformidade.acaoCorretiva) {
                  void salvarDadosNc({
                    responsavelParticipacaoId: responsavelId,
                    classificacao,
                    acaoCorretiva: novaAcao,
                  });
                }
              }}
            />
          ) : rascunho && editavel ? (
            <button className="audit-cell-trigger" type="button" onClick={() => { celulaEmEdicao.current = "ACAO"; setEditandoAcao(true); }}>
              {acaoCorretiva.trim() || "Clique para informar"}
            </button>
          ) : naoConformidade?.acaoCorretiva || "—"}
        </CelulaDado>
        <CelulaDado rotulo="Previsão de resolução">{formatarData(naoConformidade?.prazoEm)}</CelulaDado>
        <CelulaDado rotulo="Escalonamento">{formatarData(naoConformidade?.ultimoEscalonamentoEm)}</CelulaDado>
        <CelulaDado rotulo="Conclusão da NC">{formatarData(naoConformidade?.concluidaEm)}</CelulaDado>
        <CelulaDado rotulo="Status da NC">
          {naoConformidade && rotuloStatusNaoConformidade(naoConformidade.status) && (
            <span className={`status nc-status-${naoConformidade.status.toLowerCase()}`}>
              {rotuloStatusNaoConformidade(naoConformidade.status)}
            </span>
          )}
        </CelulaDado>
        <div className="audit-row-action" role="gridcell" data-label="Ação">
          {salvando && <span className="audit-row-saving">Salvando…</span>}
          {salvando && !naoConformidade ? null
            : naoConformidade && !rascunho ? (
              <Link className="audit-history-link" to={`/planos/${planoId}/nao-conformidades/${naoConformidade.id}`}>Ver NC</Link>
            ) : resultado !== "NAO_CONFORME" ? <span className="audit-empty">—</span>
              : rascunho ? (
                <button className="button button-primary audit-send-button" type="button" disabled={!classificacao || !acaoCorretiva.trim() || responsaveis.length === 0} onClick={() => void enviarParaResolucao()}>
                  Enviar para resolução
                </button>
              ) : (
                <button className="text-button" type="button" onClick={() => void tentarCriarRascunho()}>Preparar envio</button>
              )}
          {erro && <span className="audit-row-error" role="alert">{erro}</span>}
        </div>
      </div>
    </article>
  );
}

interface ItemVersaoEditavelProps {
  item: ItemVersaoExecucao;
  versaoId: string;
  editavel: boolean;
  responsaveis: Participante[];
  classificacoes: ConfiguracaoClassificacao[];
  planoId: string;
  artefatoId: string;
  auditoriaId: string;
  checklistId: string;
  removendo: boolean;
  aoAtualizar: (versaoId: string, item: ItemVersaoExecucao) => void;
  aoRemover: (versaoId: string, item: ItemVersaoExecucao) => void;
}

function ItemVersaoEditavel({
  item,
  versaoId,
  editavel,
  responsaveis,
  classificacoes,
  planoId,
  artefatoId,
  auditoriaId,
  checklistId,
  removendo,
  aoAtualizar,
  aoRemover,
}: ItemVersaoEditavelProps) {
  const [rascunho, setRascunho] = useState(item);
  const [salvando, setSalvando] = useState(false);
  const [salvo, setSalvo] = useState(false);
  const [erro, setErro] = useState("");
  const fila = useRef<Promise<void>>(Promise.resolve());
  const ultimaAlteracao = useRef(0);
  const ultimoSalvo = useRef(item);

  useEffect(() => {
    ultimoSalvo.current = item;
    setRascunho(item);
  }, [item]);

  function salvar(proximo: ItemVersaoExecucao) {
    if (!editavel) return;
    const sequencia = ++ultimaAlteracao.current;
    setSalvando(true);
    setSalvo(false);
    setErro("");
    fila.current = fila.current
      .catch(() => undefined)
      .then(async () => {
        const atualizado = await auditoriasApi.atualizarItemVersaoExecucao(
          planoId,
          artefatoId,
          auditoriaId,
          checklistId,
          versaoId,
          item.id,
          {
            descricao: proximo.descricao.trim(),
            resultado: proximo.resultado,
            responsavelParticipacaoId: proximo.responsavelParticipacaoId,
            responsavelResolucao: proximo.responsavelResolucao,
            classificacaoNc: proximo.classificacaoNc,
            acaoCorretiva: proximo.acaoCorretiva?.trim() || null,
          },
        );
        ultimoSalvo.current = atualizado;
        if (sequencia === ultimaAlteracao.current) {
          setRascunho(atualizado);
          aoAtualizar(versaoId, atualizado);
          setSalvo(true);
        }
      })
      .catch((error) => {
        if (sequencia === ultimaAlteracao.current) {
          setRascunho(ultimoSalvo.current);
          aoAtualizar(versaoId, ultimoSalvo.current);
          setErro(mensagemErro(error));
        }
      })
      .finally(() => {
        if (sequencia === ultimaAlteracao.current) setSalvando(false);
      });
  }

  function aplicar(alteracoes: Partial<ItemVersaoExecucao>) {
    const proximo = { ...rascunho, ...alteracoes };
    setRascunho(proximo);
    salvar(proximo);
  }

  const naoConforme = rascunho.resultado === "NAO_CONFORME";
  const responsavelLegado = rascunho.responsavelResolucao && !rascunho.responsavelParticipacaoId;

  return (
    <div className="audit-snapshot-editable-row" role="row">
      <span className="audit-snapshot-number" role="cell">
        <span>{rascunho.ordem}</span>
        {editavel && (
          <button
            className="audit-remove-row"
            type="button"
            aria-label={`Remover pergunta ${rascunho.ordem} desta versão`}
            title="Remover pergunta desta versão"
            disabled={salvando || removendo}
            onClick={() => aoRemover(versaoId, item)}
          >
            <span aria-hidden="true">−</span>
          </button>
        )}
      </span>
      <span role="cell">
        <input
          className="audit-snapshot-control"
          aria-label={`Pergunta do item ${rascunho.ordem}`}
          disabled={!editavel}
          maxLength={1000}
          value={rascunho.descricao}
          onChange={(event) => setRascunho((atual) => ({ ...atual, descricao: event.target.value }))}
          onBlur={() => {
            if (!rascunho.descricao.trim()) {
              setRascunho(item);
              setErro("A pergunta não pode ficar vazia.");
            } else if (rascunho.descricao.trim() !== item.descricao) {
              salvar({ ...rascunho, descricao: rascunho.descricao.trim() });
            }
          }}
        />
      </span>
      <span role="cell">
        <select
          className="audit-snapshot-control"
          aria-label={`Resultado do item ${rascunho.ordem}`}
          disabled={!editavel}
          value={rascunho.resultado ?? ""}
          onChange={(event) => aplicar({ resultado: (event.target.value || null) as ResultadoItem | null })}
        >
          <option value="">Pendente</option>
          {resultados.map((resultado) => <option key={resultado.valor} value={resultado.valor}>{resultado.rotulo}</option>)}
        </select>
      </span>
      <span role="cell">{formatarData(rascunho.ncIdentificadaEm)}</span>
      <span role="cell">
        {naoConforme ? <select
          className="audit-snapshot-control"
          aria-label={`Responsável do item ${rascunho.ordem}`}
          disabled={!editavel}
          value={rascunho.responsavelParticipacaoId ?? (responsavelLegado ? "LEGADO" : "")}
          onChange={(event) => {
            const responsavel = responsaveis.find((participante) => participante.id === event.target.value);
            aplicar({
              responsavelParticipacaoId: responsavel?.id ?? null,
              responsavelResolucao: responsavel?.nome ?? null,
            });
          }}
        >
          <option value="">Equipe de resolução</option>
          {responsavelLegado && <option value="LEGADO" disabled>{rascunho.responsavelResolucao}</option>}
          {responsaveis.map((responsavel) => <option key={responsavel.id} value={responsavel.id}>{responsavel.nome}</option>)}
        </select> : "—"}
      </span>
      <span role="cell">
        {naoConforme ? <select
          className="audit-snapshot-control"
          aria-label={`Classificação do item ${rascunho.ordem}`}
          disabled={!editavel}
          value={rascunho.classificacaoNc ?? ""}
          onChange={(event) => aplicar({ classificacaoNc: (event.target.value || null) as ClassificacaoNaoConformidade | null })}
        >
          <option value="">Selecionar</option>
          {classificacoes.map((configuracao) => <option key={configuracao.id} value={configuracao.classificacao}>{rotuloEnum(configuracao.classificacao)}</option>)}
        </select> : "—"}
      </span>
      <span role="cell">
        {naoConforme ? <input
          className="audit-snapshot-control"
          aria-label={`Ação corretiva do item ${rascunho.ordem}`}
          disabled={!editavel}
          maxLength={3000}
          value={rascunho.acaoCorretiva ?? ""}
          onChange={(event) => setRascunho((atual) => ({ ...atual, acaoCorretiva: event.target.value }))}
          onBlur={() => {
            if ((rascunho.acaoCorretiva ?? "").trim() !== (item.acaoCorretiva ?? "")) {
              salvar({ ...rascunho, acaoCorretiva: rascunho.acaoCorretiva?.trim() || null });
            }
          }}
        /> : "—"}
      </span>
      <span role="cell">{formatarData(rascunho.prazoResolucaoEm)}</span>
      <span role="cell">{formatarData(rascunho.escalonadoEm)}</span>
      <span role="cell">{formatarData(rascunho.ncConcluidaEm)}</span>
      <span role="cell">{rotuloStatusNaoConformidade(rascunho.statusNc) || "—"}</span>
      <span className="audit-snapshot-save-state" role="cell" aria-live="polite">
        {salvando ? "Salvando…" : erro ? <span role="alert">Não salvo</span> : salvo ? "Salvo" : editavel ? "Automático" : "Somente leitura"}
        {erro && <small title={erro}>{erro}</small>}
      </span>
    </div>
  );
}

function TabelaVersaoSalva({ versao, editavel, responsaveis, classificacoes, planoId, artefatoId, auditoriaId, checklistId, removendoItemId, aoAtualizar, aoRemover }: {
  versao: VersaoExecucaoChecklist;
  editavel: boolean;
  responsaveis: Participante[];
  classificacoes: ConfiguracaoClassificacao[];
  planoId: string;
  artefatoId: string;
  auditoriaId: string;
  checklistId: string;
  removendoItemId: string;
  aoAtualizar: (versaoId: string, item: ItemVersaoExecucao) => void;
  aoRemover: (versaoId: string, item: ItemVersaoExecucao) => void;
}) {
  return (
    <div className="audit-version-sheet audit-version-sheet-main" role="table" aria-label={`Versão salva ${versao.numero}`}>
      <div className="audit-version-columns" role="row">
        <span role="columnheader">Nº</span><span role="columnheader">Pergunta</span><span role="columnheader">Resultado</span><span role="columnheader">Identificação da NC</span><span role="columnheader">Responsável</span><span role="columnheader">Classificação</span><span role="columnheader">Ação corretiva</span><span role="columnheader">Previsão</span><span role="columnheader">Escalonamento</span><span role="columnheader">Conclusão</span><span role="columnheader">Status da NC</span><span role="columnheader">Salvamento</span>
      </div>
      <div className="audit-snapshot-items" role="rowgroup">
        {versao.itens.map((item) => (
          <ItemVersaoEditavel
            key={`${versao.id}-${item.id}`}
            item={item}
            versaoId={versao.id}
            editavel={editavel}
            responsaveis={responsaveis}
            classificacoes={classificacoes}
            planoId={planoId}
            artefatoId={artefatoId}
            auditoriaId={auditoriaId}
            checklistId={checklistId}
            removendo={removendoItemId === item.id}
            aoAtualizar={aoAtualizar}
            aoRemover={aoRemover}
          />
        ))}
      </div>
    </div>
  );
}

function LinhaDoTempo({ versoes, carregando, aoAbrir }: { versoes: VersaoExecucaoChecklist[]; carregando: boolean; aoAbrir: (versaoId: string) => void }) {
  if (carregando) return <LoadingState mensagem="Carregando versões da execução..." />;
  if (versoes.length === 0) return <EmptyState titulo="Nenhuma versão salva" mensagem="Use “Salvar versão” quando quiser preservar o estado atual do checklist." />;
  return (
    <ol className="audit-timeline">
      {versoes.map((versao) => {
        const respondidos = versao.itens.filter((item) => item.resultado).length;
        const naoConformes = versao.itens.filter((item) => item.resultado === "NAO_CONFORME").length;
        return (
          <li key={versao.id}>
            <span className="audit-timeline-marker" aria-hidden="true" />
            <details>
              <summary><span><strong>Versão {versao.numero} · {rotuloEnum(versao.tipo)}</strong><small>{formatarData(versao.criadoEm)} por {versao.autorNome}</small></span><span>{respondidos}/{versao.itens.length} respondidos · {naoConformes} NC</span><button className="text-button audit-version-open" type="button" onClick={(event) => { event.preventDefault(); aoAbrir(versao.id); }}>Abrir versão</button></summary>
              {versao.observacao && <p>{versao.observacao}</p>}
              <div className="audit-version-sheet">
                <div className="audit-version-columns" aria-hidden="true">
                  <span>Nº</span><span>Pergunta</span><span>Resultado</span><span>Identificação da NC</span><span>Responsável</span><span>Classificação</span><span>Ação corretiva</span><span>Previsão</span><span>Escalonamento</span><span>Conclusão</span><span>Status da NC</span>
                </div>
                <div className="audit-snapshot-items">
                  {versao.itens.map((item) => (
                    <div key={`${versao.id}-${item.id}`}>
                      <span>{item.ordem}</span>
                      <strong>{item.descricao}</strong>
                      <span>{rotuloEnum(item.resultado) || "Pendente"}</span>
                      <span>{formatarData(item.ncIdentificadaEm)}</span>
                      <span>{item.responsavelResolucao || "—"}</span>
                      <span>{item.classificacaoNc ? rotuloEnum(item.classificacaoNc) : "—"}</span>
                      <span>{item.acaoCorretiva || "—"}</span>
                      <span>{formatarData(item.prazoResolucaoEm)}</span>
                      <span>{formatarData(item.escalonadoEm)}</span>
                      <span>{formatarData(item.ncConcluidaEm)}</span>
                      <span>{rotuloStatusNaoConformidade(item.statusNc) || "—"}</span>
                    </div>
                  ))}
                </div>
              </div>
            </details>
          </li>
        );
      })}
    </ol>
  );
}

export function AuditExecutionPage() {
  const { plano, pode } = useOutletContext<PlanOutletContext>();
  const confirmar = useConfirm();
  const { usuario } = useAuth();
  const { artefatoId = "", auditoriaId = "" } = useParams();
  const [auditoria, setAuditoria] = useState<Auditoria | null>(null);
  const [participantes, setParticipantes] = useState<Participante[]>([]);
  const [classificacoes, setClassificacoes] = useState<ConfiguracaoClassificacao[]>([]);
  const [naoConformidades, setNaoConformidades] = useState<NaoConformidade[]>([]);
  const [versoes, setVersoes] = useState<VersaoExecucaoChecklist[]>([]);
  const [erro, setErro] = useState("");
  const [sucesso, setSucesso] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [processando, setProcessando] = useState(false);
  const [removendoItemId, setRemovendoItemId] = useState("");
  const [carregandoVersoes, setCarregandoVersoes] = useState(false);
  const [erroVersoes, setErroVersoes] = useState("");
  const [salvamentosPendentes, setSalvamentosPendentes] = useState<Set<string>>(new Set());
  const [checklistSelecionadoId, setChecklistSelecionadoId] = useState("");
  const [versaoVisualizadaId, setVersaoVisualizadaId] = useState<"ATUAL" | string>("ATUAL");
  const [modalVersao, setModalVersao] = useState(false);
  const [modalAutorizacao, setModalAutorizacao] = useState(false);
  const [observacaoVersao, setObservacaoVersao] = useState("");
  const [justificativa, setJustificativa] = useState("");
  const [erroModal, setErroModal] = useState("");

  const carregar = useCallback(async () => {
    if (!artefatoId || !auditoriaId) return;
    try {
      setCarregando(true);
      setErro("");
      const [detalhes, equipe, registros, regras] = await Promise.all([
        auditoriasApi.buscar(plano.id, artefatoId, auditoriaId),
        participantesApi.listarTodos(plano.id),
        naoConformidadesApi.listarTodas(plano.id),
        configuracaoPlanoApi.listarClassificacoes(plano.id),
      ]);
      setAuditoria(detalhes);
      setChecklistSelecionadoId((atual) =>
        detalhes.checklists.some((checklist) => checklist.id === atual)
          ? atual
          : detalhes.checklists.at(-1)?.id ?? "");
      setParticipantes(equipe.conteudo);
      setClassificacoes(regras.filter((item) => item.ativa));
      setNaoConformidades(registros.conteudo.filter((registro) => registro.auditoriaId === auditoriaId));
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setCarregando(false);
    }
  }, [artefatoId, auditoriaId, plano.id]);

  useEffect(() => { void carregar(); }, [carregar]);

  const participacaoAtual = participantes.find((participante) => participante.usuarioId === usuario?.id);
  const auditorDesignado = pode("AUDITAR") && participacaoAtual?.id === auditoria?.auditorParticipacaoId;
  const podeEditar = Boolean(auditorDesignado && auditoria?.status === "EM_ANDAMENTO");
  const superior = participacaoAtual?.papel === "SUPERIOR_N1" || participacaoAtual?.papel === "SUPERIOR_N2";
  const responsaveis = useMemo(() => participantes.filter((item) => item.papel === "MEMBRO_EQUIPE_RESOLUCAO"), [participantes]);
  const naoConformidadePorResposta = useMemo(() => new Map(naoConformidades.map((item) => [item.respostaId, item])), [naoConformidades]);
  const checklistAtual = auditoria?.checklists.find((item) => item.id === checklistSelecionadoId) ?? null;
  const versaoVisualizada = versaoVisualizadaId === "ATUAL"
    ? null
    : versoes.find((versao) => versao.id === versaoVisualizadaId) ?? null;
  const checklistDisponivel = Boolean(
    auditoria?.checklists.length
    && auditoria.checklists.every((item) => item.status === "PUBLICADO"),
  );
  const todosItensRespondidos = Boolean(auditoria?.totalItens && auditoria.totalRespondidos === auditoria.totalItens);
  const naoConformidadesEnviadas = naoConformidades.filter((item) => item.status !== "RASCUNHO").length === (auditoria?.naoConformes ?? 0);
  const pendentes = naoConformidades.filter((item) => item.status !== "RASCUNHO" && !statusNcFinais.has(item.status));
  const pendentesEscalonadas = pendentes.filter((item) => item.ultimoEscalonamentoEm);
  const pendenciasLiberadas = pendentes.length === 0 || Boolean(auditoria?.conclusaoExcepcionalAutorizadaEm);
  const podeConcluir = checklistDisponivel && todosItensRespondidos && naoConformidadesEnviadas && pendenciasLiberadas;
  const mensagemConclusao = !checklistDisponivel
    ? "O checklist da auditoria não está disponível."
    : !todosItensRespondidos
      ? "Responda todos os itens da tabela antes de concluir a auditoria."
      : !naoConformidadesEnviadas
        ? "Envie todas as não conformidades para resolução antes de concluir."
        : !pendenciasLiberadas
          ? "Resolva as não conformidades pendentes ou obtenha a autorização de um superior após o escalonamento."
          : "A tabela está completa e a auditoria pode ser concluída.";

  const carregarVersoes = useCallback(async (checklistId: string) => {
    if (!checklistId) {
      setVersoes([]);
      setErroVersoes("");
      return;
    }
    try {
      setCarregandoVersoes(true);
      setErroVersoes("");
      setVersoes(await auditoriasApi.listarVersoesExecucao(plano.id, artefatoId, auditoriaId, checklistId));
    } catch (error) {
      setVersoes([]);
      setErroVersoes(mensagemErroExecucao(error));
    } finally {
      setCarregandoVersoes(false);
    }
  }, [artefatoId, auditoriaId, plano.id]);

  useEffect(() => { void carregarVersoes(checklistAtual?.id ?? ""); }, [carregarVersoes, checklistAtual?.id]);

  useEffect(() => {
    if (versaoVisualizadaId !== "ATUAL" && !versoes.some((versao) => versao.id === versaoVisualizadaId)) {
      setVersaoVisualizadaId("ATUAL");
    }
  }, [versaoVisualizadaId, versoes]);

  function atualizarItem(itemAtualizado: ItemAuditoria) {
    setAuditoria((atual) => {
      if (!atual) return atual;
      const checklists = atual.checklists.map((checklist) => ({ ...checklist, itens: checklist.itens.map((item) => item.itemId === itemAtualizado.itemId ? itemAtualizado : item) }));
      return { ...atual, ...calcularResumoAuditoria(checklists.flatMap((checklist) => checklist.itens)), checklists };
    });
  }

  function atualizarItemVersao(versaoId: string, itemAtualizado: ItemVersaoExecucao) {
    setVersoes((atuais) => atuais.map((versao) => versao.id === versaoId
      ? {
          ...versao,
          itens: versao.itens.map((item) => item.id === itemAtualizado.id
            ? itemAtualizado
            : item),
        }
      : versao));
  }

  function atualizarPergunta(
    itemId: string,
    pergunta: string,
  ) {
    setAuditoria((atual) => {
      if (!atual) return atual;
      return {
        ...atual,
        checklists: atual.checklists.map((checklist) => ({
          ...checklist,
          itens: checklist.itens.map((item) => item.itemId === itemId
            ? { ...item, pergunta }
            : item),
        })),
      };
    });
    setNaoConformidades((atuais) => atuais.map((registro) =>
      registro.itemId === itemId ? { ...registro, pergunta } : registro));
  }

  function atualizarNaoConformidade(registro: NaoConformidade) {
    setNaoConformidades((atuais) => {
      const existente = atuais.some((item) => item.id === registro.id);
      return existente
        ? atuais.map((item) => item.id === registro.id ? registro : item)
        : [...atuais, registro];
    });
  }

  function removerNaoConformidade(respostaId: string) {
    setNaoConformidades((atuais) =>
      atuais.filter((registro) => registro.respostaId !== respostaId));
  }

  function mudarSalvamento(itemId: string, salvando: boolean) {
    setSalvamentosPendentes((atuais) => {
      const proximos = new Set(atuais);
      if (salvando) proximos.add(itemId);
      else proximos.delete(itemId);
      return proximos;
    });
  }

  async function adicionarLinha() {
    if (!checklistAtual) return;
    const proximaOrdem = Math.max(0, ...checklistAtual.itens.map((item) => item.ordem)) + 1;
    try {
      setProcessando(true); setErro("");
      await auditoriasApi.adicionarItem(
        plano.id,
        artefatoId,
        auditoriaId,
        checklistAtual.id,
        { ordem: proximaOrdem, pergunta: "" },
      );
      await carregar();
      setChecklistSelecionadoId(checklistAtual.id);
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setProcessando(false);
    }
  }

  async function removerItemAtual(item: ItemAuditoria) {
    if (!checklistAtual) return;
    const naoConformidade = item.respostaId
      ? naoConformidadePorResposta.get(item.respostaId)
      : undefined;
    if (naoConformidade && naoConformidade.status !== "RASCUNHO") {
      setErro("Esta pergunta possui uma não conformidade já enviada e deve permanecer no histórico da auditoria.");
      return;
    }
    const confirmou = await confirmar({
      titulo: "Remover esta pergunta?",
      mensagem: item.respostaId
        ? "A pergunta, a resposta atual e o rascunho de não conformidade serão removidos. As versões já salvas não serão alteradas."
        : "A pergunta será removida da versão atual. As versões já salvas não serão alteradas.",
      textoConfirmar: "Remover pergunta",
      perigo: true,
    });
    if (!confirmou) return;
    try {
      setRemovendoItemId(item.itemId);
      setErro("");
      await auditoriasApi.removerItem(
        plano.id,
        artefatoId,
        auditoriaId,
        checklistAtual.id,
        item.itemId,
      );
      await carregar();
      setSucesso("Pergunta removida do checklist.");
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setRemovendoItemId("");
    }
  }

  async function removerItemVersao(
    versaoId: string,
    item: ItemVersaoExecucao,
  ) {
    if (!checklistAtual) return;
    const confirmou = await confirmar({
      titulo: "Remover esta pergunta da versão?",
      mensagem: "A linha será removida somente desta versão salva. A versão atual e as demais versões não serão alteradas.",
      textoConfirmar: "Remover pergunta",
      perigo: true,
    });
    if (!confirmou) return;
    try {
      setRemovendoItemId(item.id);
      setErro("");
      const versaoAtualizada = await auditoriasApi.removerItemVersaoExecucao(
        plano.id,
        artefatoId,
        auditoriaId,
        checklistAtual.id,
        versaoId,
        item.id,
      );
      setVersoes((atuais) => atuais.map((versao) =>
        versao.id === versaoId ? versaoAtualizada : versao));
      setSucesso("Pergunta removida da versão selecionada.");
    } catch (error) {
      setErro(mensagemErroExecucao(error));
    } finally {
      setRemovendoItemId("");
    }
  }

  async function salvarVersao(event: FormEvent) {
    event.preventDefault();
    if (!checklistAtual) return;
    if (salvamentosPendentes.size > 0) {
      setErroModal("Aguarde o salvamento automático dos itens antes de criar a versão.");
      return;
    }
    try {
      setProcessando(true); setErroModal("");
      await auditoriasApi.salvarVersaoExecucao(plano.id, artefatoId, auditoriaId, checklistAtual.id, observacaoVersao.trim());
      await carregarVersoes(checklistAtual.id);
      setVersaoVisualizadaId("ATUAL");
      setObservacaoVersao(""); setModalVersao(false); setSucesso("Versão da execução salva na linha do tempo.");
    } catch (error) { setErroModal(mensagemErroExecucao(error)); } finally { setProcessando(false); }
  }

  async function autorizarConclusao(event: FormEvent) {
    event.preventDefault();
    try {
      setProcessando(true); setErroModal("");
      setAuditoria(await auditoriasApi.autorizarConclusaoExcepcional(plano.id, artefatoId, auditoriaId, justificativa.trim()));
      setModalAutorizacao(false); setJustificativa(""); setSucesso("Conclusão excepcional autorizada.");
    } catch (error) { setErroModal(mensagemErro(error)); } finally { setProcessando(false); }
  }

  async function concluir() {
    if (!auditoria || !await confirmar({ titulo: "Concluir a auditoria?", mensagem: "Todos os checklists serão mantidos para consulta com suas versões e histórico.", textoConfirmar: "Concluir auditoria" })) return;
    try {
      setProcessando(true); setErro("");
      setAuditoria(await auditoriasApi.concluir(plano.id, artefatoId, auditoria.id));
      setSucesso("Auditoria concluída.");
    } catch (error) { setErro(mensagemErro(error)); } finally { setProcessando(false); }
  }

  if (carregando) return <LoadingState mensagem="Carregando auditoria..." />;

  return (
    <div className="section-content audit-execution-page">
      <Link className="back-link" to={`/planos/${plano.id}/artefatos/${artefatoId}/auditorias`}>Voltar às auditorias</Link>
      <PageHeader titulo="Auditoria" />
      {erro && <ErrorMessage mensagem={erro} />}
      {sucesso && <SuccessMessage mensagem={sucesso} aoFechar={() => setSucesso("")} />}

      {auditoria && <>
        <section className="audit-overview" aria-labelledby="resumo-auditoria">
          <div className="audit-overview-title"><div><h2 id="resumo-auditoria">Progresso da auditoria</h2><p>{auditoria.totalRespondidos} de {auditoria.totalItens} itens respondidos</p></div><span className="status">{rotuloEnum(auditoria.status)}</span></div>
          <progress max={Math.max(auditoria.totalItens, 1)} value={auditoria.totalRespondidos} />
          <dl className="audit-summary"><div><dt>Itens</dt><dd>{auditoria.totalItens}</dd></div><div><dt>Conformes</dt><dd>{auditoria.conformes}</dd></div><div><dt>Não conformes</dt><dd>{auditoria.naoConformes}</dd></div><div><dt>N/A</dt><dd>{auditoria.naoAplicaveis}</dd></div><div><dt>Aderência</dt><dd>{auditoria.aderenciaPercentual === null ? "—" : `${auditoria.aderenciaPercentual.toLocaleString("pt-BR")}%`}</dd></div></dl>
        </section>

        {auditoria.conclusaoExcepcionalAutorizadaEm && <aside className="audit-exception-approval"><strong>Conclusão excepcional autorizada</strong><p>Por {auditoria.conclusaoExcepcionalAutorizadaPorNome}, em {formatarData(auditoria.conclusaoExcepcionalAutorizadaEm)}. {auditoria.justificativaConclusaoExcepcional}</p></aside>}

        {auditoria.checklists.length > 1 && <label className="audit-checklist-selector">Checklist legado<select value={checklistSelecionadoId} onChange={(event) => setChecklistSelecionadoId(event.target.value)}>{auditoria.checklists.map((checklist) => <option key={checklist.id} value={checklist.id}>Versão {checklist.versao}</option>)}</select></label>}

        {!checklistAtual && <EmptyState titulo="Checklist indisponível" mensagem="Não foi possível localizar o checklist automático desta auditoria." />}

        {checklistAtual && <>
          <section className="audit-sheet" aria-labelledby="itens-execucao">
            <div className="audit-sheet-heading">
              <div>
                <h2 id="itens-execucao">{versaoVisualizada ? `Versão salva ${versaoVisualizada.numero}` : "Versão atual"}</h2>
                <p>{versaoVisualizada
                  ? podeEditar
                    ? `Versão independente criada em ${formatarData(versaoVisualizada.criadoEm)}. As alterações são salvas automaticamente.`
                    : `Versão criada em ${formatarData(versaoVisualizada.criadoEm)}. Você possui acesso somente para leitura.`
                  : "Clique em uma célula para editar. A versão atual é salva automaticamente."}</p>
              </div>
              <div className="audit-sheet-actions">
                <label className="audit-version-selector">
                  Exibir
                  <select value={versaoVisualizadaId} onChange={(event) => setVersaoVisualizadaId(event.target.value)}>
                    <option value="ATUAL">Atual · salvamento automático</option>
                    {versoes.map((versao) => (
                      <option key={versao.id} value={versao.id}>Versão {versao.numero} · {formatarData(versao.criadoEm)}</option>
                    ))}
                  </select>
                </label>
                {!versaoVisualizada && checklistAtual.status === "PUBLICADO" && auditorDesignado && auditoria.status === "EM_ANDAMENTO" && <button className="button button-secondary" type="button" onClick={() => { setErroModal(""); setModalVersao(true); }}>Salvar versão</button>}
              </div>
            </div>
            {versaoVisualizada ? <TabelaVersaoSalva
              versao={versaoVisualizada}
              editavel={podeEditar && checklistAtual.status === "PUBLICADO"}
              responsaveis={responsaveis}
              classificacoes={classificacoes}
              planoId={plano.id}
              artefatoId={artefatoId}
              auditoriaId={auditoria.id}
              checklistId={checklistAtual.id}
              removendoItemId={removendoItemId}
              aoAtualizar={atualizarItemVersao}
              aoRemover={(versaoId, item) => void removerItemVersao(versaoId, item)}
            /> : <>
            <div className="audit-sheet-table" role="grid" aria-label="Checklist na versão atual">
              <div className="audit-sheet-columns" role="row"><span role="columnheader">Nº</span><span role="columnheader">Descrição</span><span role="columnheader">Resultado</span><span role="columnheader">Data e hora da identificação da NC</span><span role="columnheader">Responsável pela resolução</span><span role="columnheader">Classificação da NCF</span><span role="columnheader">Ação corretiva indicada</span><span role="columnheader">Data prevista de resolução</span><span role="columnheader">Data e hora do escalonamento</span><span role="columnheader">Data e hora da conclusão da NC</span><span role="columnheader">Status da NC</span><span role="columnheader">Ação</span></div>
              <div className="audit-items" role="rowgroup">{checklistAtual.itens.map((item) => (
                <ItemAuditoriaForm
                  key={item.itemId}
                  item={item}
                  editavel={Boolean(auditorDesignado && auditoria.status === "EM_ANDAMENTO" && checklistAtual.status === "PUBLICADO")}
                  removendo={removendoItemId === item.itemId}
                  naoConformidade={item.respostaId ? naoConformidadePorResposta.get(item.respostaId) : undefined}
                  responsaveis={responsaveis}
                  classificacoes={classificacoes}
                  aoSalvar={atualizarItem}
                  aoAtualizarPergunta={atualizarPergunta}
                  aoAtualizarNaoConformidade={atualizarNaoConformidade}
                  aoRemoverNaoConformidade={removerNaoConformidade}
                  aoMudarSalvamento={mudarSalvamento}
                  aoRemover={(itemAtual) => void removerItemAtual(itemAtual)}
                  planoId={plano.id}
                  artefatoId={artefatoId}
                  auditoriaId={auditoria.id}
                  checklistId={checklistAtual.id}
                />
              ))}</div>
            </div>
            {podeEditar && checklistAtual.status === "PUBLICADO" && <button className="checklist-add-row" type="button" disabled={processando} onClick={() => void adicionarLinha()}>{processando ? "Adicionando..." : "Adicionar linha"}</button>}
            </>}
          </section>
          <section className="audit-version-history" aria-labelledby="linha-tempo"><div className="audit-section-heading"><div><h2 id="linha-tempo">Linha do tempo do checklist</h2><p>Cada versão mantém seu próprio conteúdo e pode ser aberta sem perder o trabalho da versão atual.</p></div></div>{erroVersoes ? <ErrorMessage titulo="Linha do tempo indisponível" mensagem={erroVersoes} acao={<button className="text-button audit-retry" type="button" onClick={() => void carregarVersoes(checklistAtual.id)}>Tentar novamente</button>} /> : <LinhaDoTempo versoes={versoes} carregando={carregandoVersoes} aoAbrir={setVersaoVisualizadaId} />}</section>
        </>}

        {superior && auditoria.status === "EM_ANDAMENTO" && pendentes.length > 0 && pendentes.length === pendentesEscalonadas.length && !auditoria.conclusaoExcepcionalAutorizadaEm && <section className="audit-exception-action"><div><h2>Conclusão com pendências</h2><p>{pendentesEscalonadas.length} NC(s) escalonada(s) permanecem sem resolução. Um superior pode autorizar a conclusão excepcional da auditoria.</p></div><button className="button button-secondary" type="button" onClick={() => { setErroModal(""); setModalAutorizacao(true); }}>Avaliar autorização</button></section>}

        {auditorDesignado && auditoria.status === "EM_ANDAMENTO" && <section className="audit-conclusion"><div><h2>Concluir auditoria</h2><p>{mensagemConclusao}</p></div><button className="button button-primary" type="button" disabled={!podeConcluir || processando} onClick={() => void concluir()}>Concluir auditoria</button></section>}
      </>}

      <Modal aberto={modalVersao} titulo="Salvar versão da execução" descricao="Crie um ponto de controle na linha do tempo sem interromper a auditoria." aoFechar={() => setModalVersao(false)}><form className="form-stack" onSubmit={salvarVersao}><label>Observação da versão<textarea maxLength={500} rows={3} value={observacaoVersao} onChange={(event) => setObservacaoVersao(event.target.value)} placeholder="Ex.: revisão após reunião com a equipe responsável." /></label>{erroModal && <ErrorMessage mensagem={erroModal} />}<div className="form-actions"><button className="button button-primary" disabled={processando || salvamentosPendentes.size > 0}>{processando ? "Salvando..." : salvamentosPendentes.size > 0 ? "Aguardando autosave..." : "Salvar versão"}</button><button className="button button-secondary" type="button" onClick={() => setModalVersao(false)}>Cancelar</button></div></form></Modal>
      <Modal aberto={modalAutorizacao} titulo="Autorizar conclusão excepcional" descricao="A autorização não resolve nem cancela as NCs; ela apenas permite concluir a auditoria com pendências escalonadas." aoFechar={() => setModalAutorizacao(false)} amplo><form className="form-stack" onSubmit={autorizarConclusao}><label>Justificativa<textarea required maxLength={2000} rows={5} value={justificativa} onChange={(event) => setJustificativa(event.target.value)} /></label>{erroModal && <ErrorMessage mensagem={erroModal} />}<div className="form-actions"><button className="button button-primary" disabled={processando}>{processando ? "Autorizando..." : "Autorizar conclusão"}</button><button className="button button-secondary" type="button" onClick={() => setModalAutorizacao(false)}>Cancelar</button></div></form></Modal>
    </div>
  );
}
