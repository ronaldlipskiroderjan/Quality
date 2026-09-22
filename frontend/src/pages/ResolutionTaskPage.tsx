import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { EmptyState, ErrorMessage, LoadingState, SuccessMessage } from "../components/Feedback";
import { Icon } from "../components/Icon";
import { Modal } from "../components/Modal";
import { PageHeader } from "../components/PageHeader";
import { mensagemErro } from "../lib/api";
import { formatarData, rotuloEnum, rotuloStatusNaoConformidade } from "../lib/format";
import { permiteInformarResolucao } from "../lib/nonConformity";
import { naoConformidadesApi, resolucoesApi } from "../services/qualityApi";
import type { NaoConformidade, Resolucao } from "../types/api";

export function ResolutionTaskPage() {
  const { usuario } = useAuth();
  const { planoId = "", naoConformidadeId = "" } = useParams();
  const [naoConformidade, setNaoConformidade] = useState<NaoConformidade | null>(null);
  const [resolucoes, setResolucoes] = useState<Resolucao[]>([]);
  const [descricao, setDescricao] = useState("");
  const [evidencia, setEvidencia] = useState("");
  const [erro, setErro] = useState("");
  const [erroModal, setErroModal] = useState("");
  const [sucesso, setSucesso] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [modalResolucao, setModalResolucao] = useState(false);

  const carregar = useCallback(async () => {
    if (!planoId || !naoConformidadeId) return;
    try {
      setCarregando(true);
      setErro("");
      const [nc, pagina] = await Promise.all([
        naoConformidadesApi.buscar(planoId, naoConformidadeId),
        resolucoesApi.listarTodas(planoId, naoConformidadeId),
      ]);
      setNaoConformidade(nc);
      setResolucoes(pagina.conteudo);
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setCarregando(false);
    }
  }, [naoConformidadeId, planoId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  async function informar(event: FormEvent) {
    event.preventDefault();
    try {
      setSalvando(true);
      setErroModal("");
      setSucesso("");
      await resolucoesApi.informar(planoId, naoConformidadeId, {
        descricao: descricao.trim(),
        evidencia: evidencia.trim(),
      });
      setDescricao("");
      setEvidencia("");
      await carregar();
      setSucesso("Resolução enviada para validação do auditor.");
      setModalResolucao(false);
    } catch (error) {
      setErroModal(mensagemErro(error));
    } finally {
      setSalvando(false);
    }
  }

  const semResponsavel = naoConformidade?.responsavelEmail == null;
  const ehResponsavel =
    naoConformidade?.responsavelEmail?.toLowerCase() === usuario?.email.toLowerCase();
  const podeInformar =
    Boolean(naoConformidade && permiteInformarResolucao(naoConformidade.status))
    && (semResponsavel || ehResponsavel);
  const prazoVencido = Boolean(naoConformidade?.prazoEm)
    && new Date(naoConformidade?.prazoEm as string).getTime() < Date.now()
    && !["CONCLUIDA", "CANCELADA"].includes(naoConformidade?.status ?? "");

  if (carregando) return <LoadingState mensagem="Carregando solicitação..." />;

  return (
    <section className="section-content resolution-task-page">
      <Link className="back-link resolution-task-back" to={`/planos/${planoId}`}>Voltar às solicitações do plano</Link>
      <PageHeader
        titulo="Tratamento da não conformidade"
        descricao={naoConformidade
          ? `${naoConformidade.planoNome || "Plano de qualidade"} · ${naoConformidade.artefatoNome} · item ${naoConformidade.itemOrdem}`
          : undefined}
      />
      {erro && <ErrorMessage mensagem={erro} />}
      {sucesso && <SuccessMessage mensagem={sucesso} aoFechar={() => setSucesso("")} />}

      {naoConformidade ? (
        <>
          <section className="resolution-task-command" aria-labelledby="titulo-solicitacao">
            <header>
              <div>
                <h2 id="titulo-solicitacao">{naoConformidade.pergunta}</h2>
                <p>{naoConformidade.artefatoNome} · item {naoConformidade.itemOrdem}</p>
              </div>
              <span className={`status status-${naoConformidade.status.toLowerCase()}`}>
                {rotuloStatusNaoConformidade(naoConformidade.status)}
              </span>
            </header>
            <dl>
              <div><dt>Responsável</dt><dd>{naoConformidade.responsavelNome ?? "Equipe de resolução"}</dd></div>
              <div><dt>Classificação</dt><dd>{rotuloEnum(naoConformidade.classificacao)}</dd></div>
              <div><dt>Prazo atual</dt><dd><time className={prazoVencido ? "is-overdue" : undefined} dateTime={naoConformidade.prazoEm ?? undefined}>{formatarData(naoConformidade.prazoEm)}</time></dd></div>
              <div><dt>Identificada em</dt><dd><time dateTime={naoConformidade.identificadoEm}>{formatarData(naoConformidade.identificadoEm)}</time></dd></div>
            </dl>
          </section>

          <div className="resolution-task-workspace">
            <main className="resolution-task-main">
              <section className="resolution-task-section" aria-labelledby="acao-corretiva">
                <header className="resolution-task-section-header">
                  <div>
                    <h2 id="acao-corretiva">Ação corretiva</h2>
                    <p>Orientação definida pelo auditor para corrigir esta não conformidade.</p>
                  </div>
                  {podeInformar && (
                    <button className="button button-primary" type="button" onClick={() => { setErroModal(""); setModalResolucao(true); }}>
                      Informar resolução
                    </button>
                  )}
                </header>
                <p className="resolution-corrective-action">{naoConformidade.acaoCorretiva}</p>
                {!podeInformar && (
                  <div className="resolution-task-state">
                    <Icon name="activity" size={19} />
                    <p>{ehResponsavel || semResponsavel
                      ? "Esta solicitação não aceita uma nova resolução no estado atual."
                      : `A resolução está atribuída a ${naoConformidade.responsavelNome}.`}</p>
                  </div>
                )}
              </section>

              <section className="resolution-task-section resolution-history" aria-labelledby="resolucoes-enviadas">
                <header className="resolution-task-section-header">
                  <div><h2 id="resolucoes-enviadas">Histórico de resoluções</h2><p>Correções enviadas e decisões registradas pelo auditor.</p></div>
                  <span className="resolution-history-count">{resolucoes.length}</span>
                </header>
                {resolucoes.length === 0 ? (
                  <EmptyState titulo="Nenhuma resolução enviada" mensagem="A primeira resolução aparecerá aqui depois de ser encaminhada para validação." />
                ) : (
                  <ol className="resolution-history-list">
                    {resolucoes.map((resolucao) => (
                      <li key={resolucao.id}>
                        <span className="resolution-history-marker" aria-hidden="true" />
                        <article>
                          <header>
                            <div><strong>{resolucao.responsavelNome}</strong><time dateTime={resolucao.informadaEm}>{formatarData(resolucao.informadaEm)}</time></div>
                            <span className={`status status-${resolucao.status.toLowerCase()}`}>{rotuloEnum(resolucao.status)}</span>
                          </header>
                          <p>{resolucao.descricao}</p>
                          {resolucao.evidencia && <div className="resolution-evidence"><strong>Evidência</strong><p>{resolucao.evidencia}</p></div>}
                          {resolucao.observacaoAuditor && <div className="resolution-auditor-note"><strong>Retorno do auditor</strong><p>{resolucao.observacaoAuditor}</p></div>}
                          {resolucao.validadaEm && <footer>Validada por {resolucao.auditorNome ?? "auditor"} em {formatarData(resolucao.validadaEm)}</footer>}
                        </article>
                      </li>
                    ))}
                  </ol>
                )}
              </section>
            </main>

            <aside className="resolution-task-side" aria-label="Rastreabilidade da não conformidade">
              <section className="resolution-task-section">
                <header className="resolution-task-section-header"><div><h2>Rastreabilidade</h2></div></header>
                <dl className="resolution-trace-list">
                  <div><dt>Auditor</dt><dd>{naoConformidade.auditorNome}</dd></div>
                  <div><dt>Enviada em</dt><dd>{formatarData(naoConformidade.enviadaEm)}</dd></div>
                  <div><dt>Último escalonamento</dt><dd>{formatarData(naoConformidade.ultimoEscalonamentoEm)}</dd></div>
                  <div><dt>Concluída em</dt><dd>{formatarData(naoConformidade.concluidaEm)}</dd></div>
                  <div><dt>Última atualização</dt><dd>{formatarData(naoConformidade.atualizadoEm)}</dd></div>
                </dl>
              </section>
            </aside>
          </div>

          <Modal aberto={modalResolucao} titulo="Informar resolução" descricao="Registre a correção realizada e uma evidência verificável para o auditor." aoFechar={() => setModalResolucao(false)} amplo>
            <form className="form-stack" onSubmit={informar}>
              <label>
                Descrição da resolução
                <textarea required maxLength={5000} rows={5} value={descricao} onChange={(event) => setDescricao(event.target.value)} />
              </label>
              <label>
                Evidência
                <textarea maxLength={2000} rows={3} value={evidencia} onChange={(event) => setEvidencia(event.target.value)} placeholder="Documento, versão, link ou outra referência verificável." />
              </label>
              {erroModal && <ErrorMessage mensagem={erroModal} />}
              <div className="form-actions">
                <button className="button button-primary" type="submit" disabled={salvando}>{salvando ? "Enviando..." : "Enviar para validação"}</button>
                <button className="button button-secondary" type="button" onClick={() => setModalResolucao(false)}>Cancelar</button>
              </div>
            </form>
          </Modal>
        </>
      ) : (
        !erro && <EmptyState titulo="Solicitação não encontrada" mensagem="Esta não conformidade não está mais disponível para consulta." />
      )}
    </section>
  );
}
