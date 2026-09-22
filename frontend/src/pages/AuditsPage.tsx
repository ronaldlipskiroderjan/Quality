import { useCallback, useEffect, useState } from "react";
import { Link, useOutletContext, useParams } from "react-router-dom";
import { EmptyState, ErrorMessage, LoadingState } from "../components/Feedback";
import { Icon } from "../components/Icon";
import { PageHeader } from "../components/PageHeader";
import type { PlanOutletContext } from "../layouts/PlanLayout";
import { mensagemErro } from "../lib/api";
import { formatarData, rotuloEnum } from "../lib/format";
import { artefatosApi, auditoriasApi } from "../services/qualityApi";
import type { Artefato, AuditoriaResumo } from "../types/api";

export function AuditsPage() {
  const { plano } = useOutletContext<PlanOutletContext>();
  const { artefatoId = "" } = useParams();
  const [artefato, setArtefato] = useState<Artefato | null>(null);
  const [auditoria, setAuditoria] = useState<AuditoriaResumo | null>(null);
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(true);

  const carregar = useCallback(async () => {
    if (!artefatoId) return;
    try {
      setCarregando(true);
      setErro("");
      const [item, lista] = await Promise.all([
        artefatosApi.buscar(plano.id, artefatoId),
        auditoriasApi.listar(plano.id, artefatoId, 0),
      ]);
      setArtefato(item);
      setAuditoria(lista.conteudo[0] ?? null);
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setCarregando(false);
    }
  }, [artefatoId, plano.id]);

  useEffect(() => { void carregar(); }, [carregar]);

  return (
    <div className="section-content audits-page">
      <Link className="back-link" to={`/planos/${plano.id}/artefatos`}>Voltar aos artefatos</Link>
      <PageHeader
        titulo={artefato ? `Auditoria de ${artefato.nome}` : "Auditoria"}
      />
      {erro && <ErrorMessage mensagem={erro} />}
      {carregando ? <LoadingState mensagem="Carregando auditoria..." /> : auditoria ? (
        <article className="audit-single-summary">
          <div className="audit-overview-title">
            <div>
              <h2>Auditoria do artefato</h2>
              <p>Criada em {formatarData(auditoria.dataInicio)} · Auditor: {auditoria.auditorNome}</p>
            </div>
            <span className="status">{rotuloEnum(auditoria.status)}</span>
          </div>
          <dl className="audit-summary">
            <div><dt>Checklists</dt><dd>{auditoria.totalChecklists}</dd></div>
            <div><dt>Itens</dt><dd>{auditoria.totalItens}</dd></div>
            <div><dt>Respondidos</dt><dd>{auditoria.totalRespondidos}</dd></div>
          </dl>
          <section className="audit-reference-panel" aria-labelledby="audit-reference-title">
            <h3 id="audit-reference-title">Documentos de referência</h3>
            {auditoria.documentosReferencia.length === 0 ? (
              <p>Nenhum documento de referência selecionado.</p>
            ) : (
              <ul className="audit-reference-list">
                {auditoria.documentosReferencia.map((documento) => (
                  <li key={documento.id}>
                    <span className="audit-reference-icon" aria-hidden="true">
                      <Icon name="document" size={18} />
                    </span>
                    <span>
                      <strong>{documento.nomeArquivo}</strong>
                      <small>{documento.nome} · versão {documento.versao}</small>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>
          <Link className="button button-primary align-start" to={`/planos/${plano.id}/artefatos/${artefatoId}/auditorias/${auditoria.id}`}>
            Acessar checklist
          </Link>
        </article>
      ) : <EmptyState mensagem="A auditoria deste artefato não foi encontrada." />}
    </div>
  );
}
