import {
  useCallback,
  useEffect,
  useState,
  type FormEvent,
} from "react";
import { Link, useOutletContext, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ErrorMessage, LoadingState, SuccessMessage } from "../components/Feedback";
import { useConfirm } from "../components/ConfirmProvider";
import { PageHeader } from "../components/PageHeader";
import { Modal } from "../components/Modal";
import type { PlanOutletContext } from "../layouts/PlanLayout";
import { mensagemErro } from "../lib/api";
import {
  encontrarResolucaoPendente,
  permiteInformarResolucao,
} from "../lib/nonConformity";
import {
  formatarData,
  rotuloEnum,
  rotuloStatusNaoConformidade,
} from "../lib/format";
import {
  escalonamentosApi,
  naoConformidadesApi,
  participantesApi,
  resolucoesApi,
} from "../services/qualityApi";
import type {
  DecisaoValidacao,
  Escalonamento,
  EventoNaoConformidade,
  NaoConformidade,
  Participante,
  Resolucao,
} from "../types/api";

interface FormularioResolucaoProps {
  planoId: string;
  naoConformidadeId: string;
  aoConcluir: (mensagem: string) => Promise<void>;
}

function FormularioResolucao({
  planoId,
  naoConformidadeId,
  aoConcluir,
}: FormularioResolucaoProps) {
  const [descricao, setDescricao] = useState("");
  const [evidencia, setEvidencia] = useState("");
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [aberto, setAberto] = useState(false);

  async function informar(event: FormEvent) {
    event.preventDefault();

    try {
      setSalvando(true);
      setErro("");
      await resolucoesApi.informar(planoId, naoConformidadeId, {
        descricao: descricao.trim(),
        evidencia: evidencia.trim(),
      });
      await aoConcluir("Resolução informada e encaminhada para validação do auditor.");
      setAberto(false);
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setSalvando(false);
    }
  }

  return (
    <><button className="button button-primary" type="button" onClick={() => setAberto(true)}>Informar resolução</button><Modal aberto={aberto} titulo="Informar resolução" descricao="Descreva o que foi corrigido e indique a evidência verificável." aoFechar={() => setAberto(false)} amplo><form className="form-stack" onSubmit={informar}>
      <div>
        <h2>Informar resolução</h2>
        <p>Descreva o que foi corrigido e indique onde a evidência pode ser verificada.</p>
      </div>
      <label>
        Descrição da resolução
        <textarea
          required
          maxLength={5000}
          rows={5}
          value={descricao}
          onChange={(event) => setDescricao(event.target.value)}
        />
        <small>{descricao.length}/5000 caracteres</small>
      </label>
      <label>
        Evidência
        <textarea
          maxLength={2000}
          rows={3}
          value={evidencia}
          onChange={(event) => setEvidencia(event.target.value)}
          placeholder="Informe documento, versão, link ou outra referência verificável."
        />
        <small>{evidencia.length}/2000 caracteres</small>
      </label>
      {erro && <ErrorMessage mensagem={erro} />}
      <button className="button button-primary align-start" type="submit" disabled={salvando}>
        {salvando ? "Enviando..." : "Enviar para validação"}
      </button>
    </form></Modal></>
  );
}

interface FormularioValidacaoProps {
  planoId: string;
  naoConformidadeId: string;
  resolucao: Resolucao;
  aoConcluir: (mensagem: string) => Promise<void>;
}

function FormularioValidacao({
  planoId,
  naoConformidadeId,
  resolucao,
  aoConcluir,
}: FormularioValidacaoProps) {
  const confirmar = useConfirm();
  const [decisao, setDecisao] = useState<DecisaoValidacao>("APROVAR");
  const [observacao, setObservacao] = useState("");
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [aberto, setAberto] = useState(false);

  async function validar(event: FormEvent) {
    event.preventDefault();
    if (decisao === "SOLICITAR_AJUSTES" && !observacao.trim()) {
      setErro("Descreva os ajustes necessários.");
      return;
    }
    const aprovando = decisao === "APROVAR";
    if (!await confirmar({
      titulo: aprovando ? "Aprovar esta resolução?" : "Solicitar ajustes?",
      mensagem: aprovando
        ? "A não conformidade será concluída após a aprovação."
        : "A resolução voltará para o responsável com a observação registrada.",
      textoConfirmar: aprovando ? "Aprovar e concluir" : "Solicitar ajustes",
      perigo: !aprovando,
    })) return;

    try {
      setSalvando(true);
      setErro("");
      await resolucoesApi.validar(
        planoId,
        naoConformidadeId,
        resolucao.id,
        decisao,
        observacao.trim(),
      );
      await aoConcluir(
        decisao === "APROVAR"
          ? "Resolução aprovada. A não conformidade foi concluída."
          : "Ajustes solicitados ao responsável.",
      );
      setAberto(false);
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setSalvando(false);
    }
  }

  return (
    <><button className="button button-primary" type="button" onClick={() => setAberto(true)}>Validar resolução</button><Modal aberto={aberto} titulo="Validar resolução" descricao="Confira a evidência e registre a decisão do auditor." aoFechar={() => setAberto(false)} amplo><form className="form-stack" onSubmit={validar}>
      <div>
        <h2>Validar resolução</h2>
        <p>Confira a descrição e a evidência antes de tomar a decisão.</p>
      </div>
      <div className="resolution-review">
        <strong>{resolucao.responsavelNome}</strong>
        <p>{resolucao.descricao}</p>
        {resolucao.evidencia && <p><strong>Evidência:</strong> {resolucao.evidencia}</p>}
      </div>
      <fieldset className="result-options">
        <legend>Decisão</legend>
        <label>
          <input
            type="radio"
            name="decisao-validacao"
            checked={decisao === "APROVAR"}
            onChange={() => setDecisao("APROVAR")}
          />
          Aprovar
        </label>
        <label>
          <input
            type="radio"
            name="decisao-validacao"
            checked={decisao === "SOLICITAR_AJUSTES"}
            onChange={() => setDecisao("SOLICITAR_AJUSTES")}
          />
          Solicitar ajustes
        </label>
      </fieldset>
      <label>
        Observação {decisao === "SOLICITAR_AJUSTES" && <span aria-hidden="true">*</span>}
        <textarea
          required={decisao === "SOLICITAR_AJUSTES"}
          maxLength={3000}
          rows={4}
          value={observacao}
          onChange={(event) => setObservacao(event.target.value)}
        />
        <small>{observacao.length}/3000 caracteres</small>
      </label>
      {erro && <ErrorMessage mensagem={erro} />}
      <button className="button button-primary align-start" type="submit" disabled={salvando}>
        {salvando ? "Registrando..." : "Registrar decisão"}
      </button>
    </form></Modal></>
  );
}

export function NonConformityPage() {
  const { plano, pode } = useOutletContext<PlanOutletContext>();
  const confirmar = useConfirm();
  const { usuario } = useAuth();
  const { naoConformidadeId = "" } = useParams();
  const [naoConformidade, setNaoConformidade] = useState<NaoConformidade | null>(null);
  const [participantes, setParticipantes] = useState<Participante[]>([]);
  const [resolucoes, setResolucoes] = useState<Resolucao[]>([]);
  const [escalonamentos, setEscalonamentos] = useState<Escalonamento[]>([]);
  const [historico, setHistorico] = useState<EventoNaoConformidade[]>([]);
  const [erro, setErro] = useState("");
  const [sucesso, setSucesso] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [alertandoEquipe, setAlertandoEquipe] = useState(false);

  const carregar = useCallback(async (mostrarCarregamento = true) => {
    if (!naoConformidadeId) return;

    try {
      if (mostrarCarregamento) setCarregando(true);
      setErro("");
      const [nc, equipe, listaResolucoes, listaEscalonamentos, eventos] =
        await Promise.all([
          naoConformidadesApi.buscar(plano.id, naoConformidadeId),
          participantesApi.listarTodos(plano.id),
          resolucoesApi.listarTodas(plano.id, naoConformidadeId),
          escalonamentosApi.listarTodos(plano.id, naoConformidadeId),
          naoConformidadesApi.listarHistorico(plano.id, naoConformidadeId),
        ]);
      setNaoConformidade(nc);
      setParticipantes(equipe.conteudo);
      setResolucoes(listaResolucoes.conteudo);
      setEscalonamentos(listaEscalonamentos.conteudo);
      setHistorico(eventos.conteudo);
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setCarregando(false);
    }
  }, [naoConformidadeId, plano.id]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  async function concluirAcao(mensagem: string) {
    await carregar(false);
    setSucesso(mensagem);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  const participacaoAtual = participantes.find((participante) => participante.usuarioId === usuario?.id);
  const ehAuditor = participacaoAtual?.id === naoConformidade?.auditorParticipacaoId;
  const ehResponsavel =
    naoConformidade?.responsavelParticipacaoId == null
    || participacaoAtual?.id === naoConformidade.responsavelParticipacaoId;
  const resolucaoPendente = encontrarResolucaoPendente(resolucoes);
  const podeInformar =
    Boolean(naoConformidade && permiteInformarResolucao(naoConformidade.status)) &&
    ehResponsavel &&
    pode("TRATAR_NAO_CONFORMIDADE");
  const podeValidar =
    naoConformidade?.status === "RESOLUCAO_INFORMADA" &&
    Boolean(resolucaoPendente) &&
    ehAuditor &&
    pode("AUDITAR");
  const nivelSuperiorAtual =
    plano.meusPapeis.includes("SUPERIOR_N1")
      && naoConformidade?.status === "ESCALONADA_N1"
      ? "N1"
      : plano.meusPapeis.includes("SUPERIOR_N2")
          && ["ESCALONADA_N2", "VENCIDA_N2"].includes(naoConformidade?.status ?? "")
        ? "N2"
        : null;
  const temAcoesDisponiveis = podeInformar || podeValidar || Boolean(nivelSuperiorAtual);
  const orientacaoAtual = podeInformar
    ? "Registre o que foi corrigido e envie a evidência para validação."
    : podeValidar
      ? "A resolução está pronta para conferência e decisão do auditor."
      : nivelSuperiorAtual
        ? `O prazo foi excedido no nível ${nivelSuperiorAtual}. Você pode alertar a equipe de resolução.`
        : "Acompanhe abaixo o andamento registrado pela equipe e o histórico desta não conformidade.";

  async function alertarEquipe() {
    if (!naoConformidade || !nivelSuperiorAtual) return;
    const confirmado = await confirmar({
      titulo: "Alertar a equipe de resolução?",
      mensagem:
        `A equipe receberá um aviso do superior ${nivelSuperiorAtual} de que o prazo desta não conformidade foi excedido.`,
      textoConfirmar: "Emitir alerta",
    });
    if (!confirmado) return;
    try {
      setAlertandoEquipe(true);
      setErro("");
      await naoConformidadesApi.alertarEquipe(plano.id, naoConformidade.id);
      await concluirAcao("Alerta enviado à equipe de resolução.");
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setAlertandoEquipe(false);
    }
  }

  if (carregando) return <LoadingState mensagem="Carregando não conformidade..." />;

  return (
    <div className="section-content nonconformity-page">
      <Link className="back-link nc-back-link" to={"/planos/" + plano.id + "/nao-conformidades"}>
        Voltar à equipe de resolução
      </Link>
      <PageHeader
        titulo="Não conformidade"
        descricao={naoConformidade ? `${naoConformidade.artefatoNome} · Item ${naoConformidade.itemOrdem}` : undefined}
      />

      {erro && <ErrorMessage mensagem={erro} />}
      {sucesso && <SuccessMessage mensagem={sucesso} aoFechar={() => setSucesso("")} />}

      {naoConformidade && (
        <>
          <section className="nc-command" aria-labelledby="dados-nao-conformidade">
            <header className="nc-command-header">
              <div>
                <h2 id="dados-nao-conformidade">{naoConformidade.pergunta}</h2>
                <p>{naoConformidade.artefatoNome} · Item {naoConformidade.itemOrdem}</p>
              </div>
              {rotuloStatusNaoConformidade(naoConformidade.status) && (
                <span className={`status status-${naoConformidade.status.toLowerCase()}`}>
                  {rotuloStatusNaoConformidade(naoConformidade.status)}
                </span>
              )}
            </header>
            <dl className="nc-command-meta">
              <div><dt>Responsável</dt><dd>{naoConformidade.responsavelNome ?? "Equipe de resolução"}</dd></div>
              <div><dt>Classificação</dt><dd>{rotuloEnum(naoConformidade.classificacao)}</dd></div>
              <div><dt>Prazo atual</dt><dd>{formatarData(naoConformidade.prazoEm)}</dd></div>
              <div><dt>Identificada em</dt><dd>{formatarData(naoConformidade.identificadoEm)}</dd></div>
            </dl>
          </section>

          <div className="nc-workspace">
            <div className="nc-main-column">
              <section className="nc-section nc-treatment" aria-labelledby="tratamento-nc">
                <header className="nc-section-header">
                  <div><h2 id="tratamento-nc">Tratamento</h2><p>{orientacaoAtual}</p></div>
                </header>
                <div className="nc-corrective-action">
                  <strong>Ação corretiva indicada</strong>
                  <p>{naoConformidade.acaoCorretiva}</p>
                </div>
                {temAcoesDisponiveis && (
                  <div className="workflow-actions">
                    {podeInformar && <FormularioResolucao planoId={plano.id} naoConformidadeId={naoConformidade.id} aoConcluir={concluirAcao} />}
                    {podeValidar && resolucaoPendente && <FormularioValidacao planoId={plano.id} naoConformidadeId={naoConformidade.id} resolucao={resolucaoPendente} aoConcluir={concluirAcao} />}
                    {nivelSuperiorAtual && (
                      <button className="button button-secondary" type="button" disabled={alertandoEquipe} onClick={() => void alertarEquipe()}>
                        {alertandoEquipe ? "Enviando alerta..." : "Alertar equipe sobre atraso"}
                      </button>
                    )}
                  </div>
                )}
              </section>

              <section className="nc-section" aria-labelledby="resolucoes-nc">
                <header className="nc-section-header">
                  <div><h2 id="resolucoes-nc">Resoluções</h2><p>Correções informadas pela equipe e decisões registradas pelo auditor.</p></div>
                  <span className="nc-section-count">{resolucoes.length}</span>
                </header>
                {resolucoes.length === 0 ? <p className="nc-empty-row">Nenhuma resolução informada até o momento.</p> : (
                  <div className="nc-record-list">
                    {resolucoes.map((resolucao) => (
                      <article className="nc-record" key={resolucao.id}>
                        <header className="nc-record-header">
                          <div><strong>{resolucao.responsavelNome}</strong><time dateTime={resolucao.informadaEm}>{formatarData(resolucao.informadaEm)}</time></div>
                          <span className={`status status-${resolucao.status.toLowerCase()}`}>{rotuloEnum(resolucao.status)}</span>
                        </header>
                        <p>{resolucao.descricao}</p>
                        {resolucao.evidencia && <p><strong>Evidência:</strong> {resolucao.evidencia}</p>}
                        {resolucao.observacaoAuditor && <p><strong>Observação do auditor:</strong> {resolucao.observacaoAuditor}</p>}
                      </article>
                    ))}
                  </div>
                )}
              </section>
            </div>

            <aside className="nc-side-column" aria-label="Informações complementares da não conformidade">
              <section className="nc-section" aria-labelledby="detalhes-nc">
                <header className="nc-section-header"><div><h2 id="detalhes-nc">Detalhes</h2></div></header>
                <dl className="nc-facts-list">
                  <div><dt>Auditor</dt><dd>{naoConformidade.auditorNome}</dd></div>
                  <div><dt>Responsável</dt><dd>{naoConformidade.responsavelNome ?? "Equipe de resolução"}</dd></div>
                  <div><dt>Enviada em</dt><dd>{formatarData(naoConformidade.enviadaEm)}</dd></div>
                  <div><dt>Último escalonamento</dt><dd>{formatarData(naoConformidade.ultimoEscalonamentoEm)}</dd></div>
                  <div><dt>Concluída em</dt><dd>{formatarData(naoConformidade.concluidaEm)}</dd></div>
                  <div><dt>Última atualização</dt><dd>{formatarData(naoConformidade.atualizadoEm)}</dd></div>
                </dl>
              </section>

              <section className="nc-section" aria-labelledby="escalonamentos-nc">
                <header className="nc-section-header">
                  <div><h2 id="escalonamentos-nc">Escalonamentos</h2><p>Prazos e responsáveis superiores acionados.</p></div>
                  <span className="nc-section-count">{escalonamentos.length}</span>
                </header>
                {escalonamentos.length === 0 ? <p className="nc-empty-row">Nenhum escalonamento registrado.</p> : (
                  <div className="nc-escalation-list">
                    {escalonamentos.map((escalonamento) => (
                      <article className="nc-escalation-record" key={escalonamento.id}>
                        <header><span className="status">{escalonamento.nivel}</span><strong>{escalonamento.responsavelNome}</strong></header>
                        <dl>
                          <div><dt>Escalonado</dt><dd>{formatarData(escalonamento.escalonadoEm)}</dd></div>
                          <div><dt>Novo prazo</dt><dd>{formatarData(escalonamento.prazoEm)}</dd></div>
                        </dl>
                        {escalonamento.observacao && <p>{escalonamento.observacao}</p>}
                      </article>
                    ))}
                  </div>
                )}
              </section>
            </aside>
          </div>

          <section className="nc-section nc-history-section" aria-labelledby="historico-nc">
            <header className="nc-section-header">
              <div><h2 id="historico-nc">Linha do tempo</h2><p>Todas as mudanças registradas desde a identificação.</p></div>
              <span className="nc-section-count">{historico.length}</span>
            </header>
            {historico.length === 0 ? <p className="nc-empty-row">Ainda não há eventos no histórico.</p> : (
              <ol className="nc-timeline">
                {historico.map((evento) => (
                  <li key={evento.tipo + "-" + evento.referenciaId + "-" + evento.ocorridoEm}>
                    <time dateTime={evento.ocorridoEm}>{formatarData(evento.ocorridoEm)}</time>
                    <div><strong>{evento.titulo}</strong>{evento.detalhe && <p>{evento.detalhe}</p>}</div>
                  </li>
                ))}
              </ol>
            )}
          </section>
        </>
      )}
    </div>
  );
}
