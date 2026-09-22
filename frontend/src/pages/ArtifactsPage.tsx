import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, useOutletContext } from "react-router-dom";
import { EmptyState, ErrorMessage, LoadingState, SuccessMessage } from "../components/Feedback";
import { useConfirm } from "../components/ConfirmProvider";
import { Icon } from "../components/Icon";
import { Modal } from "../components/Modal";
import { PageHeader } from "../components/PageHeader";
import type { PlanOutletContext } from "../layouts/PlanLayout";
import { mensagemErro } from "../lib/api";
import { formatarData, rotuloEnum } from "../lib/format";
import { artefatosApi, documentosApi, participantesApi } from "../services/qualityApi";
import type { Artefato, Documento, Participante } from "../types/api";

export function ArtifactsPage() {
  const { plano, pode } = useOutletContext<PlanOutletContext>();
  const confirmar = useConfirm();
  const [artefatos, setArtefatos] = useState<Artefato[]>([]);
  const [documentos, setDocumentos] = useState<Documento[]>([]);
  const [referencias, setReferencias] = useState<Documento[]>([]);
  const [auditores, setAuditores] = useState<Participante[]>([]);
  const [modalAberto, setModalAberto] = useState(false);
  const [nome, setNome] = useState(""); const [versao, setVersao] = useState("");
  const [documentoId, setDocumentoId] = useState(""); const [auditorId, setAuditorId] = useState("");
  const [dataPlanejada, setDataPlanejada] = useState("");
  const [referenciasSelecionadas, setReferenciasSelecionadas] = useState<string[]>([]);
  const [erro, setErro] = useState(""); const [sucesso, setSucesso] = useState("");
  const [erroModal, setErroModal] = useState("");
  const [carregando, setCarregando] = useState(true); const [salvando, setSalvando] = useState(false);
  const gerencia = pode("GERENCIAR_ARTEFATOS");

  const carregar = useCallback(async () => {
    try {
      setCarregando(true); setErro("");
      const [lista, docs, refs, equipe] = await Promise.all([
        artefatosApi.listarTodos(plano.id), documentosApi.listarAuditados(plano.id), documentosApi.listarTodos(plano.id, "REFERENCIA"), participantesApi.listarTodos(plano.id),
      ]);
      setArtefatos(lista.conteudo); setDocumentos(docs.conteudo); setReferencias(refs.conteudo);
      setAuditores(equipe.conteudo.filter((p) => p.papel === "AUDITOR_RESPONSAVEL_QUALIDADE"));
    } catch (error) { setErro(mensagemErro(error)); } finally { setCarregando(false); }
  }, [plano.id]);
  useEffect(() => { void carregar(); }, [carregar]);

  const grupos = useMemo(() => documentos.map((documento) => ({
    documento,
    artefatos: artefatos.filter((artefato) => artefato.documentoId === documento.id),
  })), [artefatos, documentos]);

  async function criar(event: FormEvent) {
    event.preventDefault();
    try {
      setSalvando(true); setErroModal("");
      if (referenciasSelecionadas.length === 0) return setErroModal("Selecione ao menos um documento de referência.");
      await artefatosApi.criar(plano.id, { documentoId, auditorParticipacaoId: auditorId, documentoReferenciaIds: referenciasSelecionadas, nome, versao, dataPlanejada });
      setNome(""); setVersao(""); setDocumentoId(""); setAuditorId(""); setDataPlanejada("");
      setReferenciasSelecionadas([]);
      setModalAberto(false); await carregar(); setSucesso("Artefato criado.");
    } catch (error) { setErroModal(mensagemErro(error)); } finally { setSalvando(false); }
  }
  async function remover(artefato: Artefato) {
    const emExecucao = artefato.status === "EM_ANDAMENTO"
      || artefato.status === "EM_PREPARACAO"
      || artefato.status === "PAUSADO";
    if (!await confirmar({
      titulo: emExecucao ? "Excluir auditoria em andamento?" : "Excluir artefato e auditoria?",
      mensagem: emExecucao
        ? `“${artefato.nome}” ainda está em execução. O checklist, as respostas, as não conformidades e a linha do tempo associados serão excluídos definitivamente.`
        : `“${artefato.nome}” e sua auditoria associada serão excluídos definitivamente.`,
      textoConfirmar: "Excluir artefato",
      perigo: true,
    })) return;
    try { await artefatosApi.remover(plano.id, artefato.id); await carregar(); setSucesso("Artefato excluído."); }
    catch (error) { setErro(mensagemErro(error)); }
  }

  return (
    <div className="section-content artifacts-page">
      <PageHeader titulo="Auditorias" acao={gerencia ? <button className="button button-primary" onClick={() => { setErroModal(""); setModalAberto(true); }}>Criar artefato e auditoria</button> : undefined} />
      {erro && <ErrorMessage mensagem={erro} />}{sucesso && <SuccessMessage mensagem={sucesso} aoFechar={() => setSucesso("")} />}
      {carregando ? <LoadingState mensagem="Carregando artefatos..." /> : grupos.length === 0 ? <EmptyState mensagem="Adicione um documento auditado antes de criar artefatos." /> : (
        <div className="audit-document-groups">{grupos.map(({ documento, artefatos: itens }) => <section className="audit-document-group" key={documento.id}>
            <header className="audit-document-header"><span className="audit-document-symbol" aria-hidden="true"><Icon name="document" size={22} /></span><div><h2>{documento.nome}</h2><p>{documento.nomeArquivo} · versão {documento.versao}</p></div><span className="audit-document-count">{itens.length} {itens.length === 1 ? "auditoria" : "auditorias"}</span></header>
            {itens.length === 0 ? <div className="audit-document-empty"><p>Nenhum artefato foi preparado para este documento.</p>{gerencia && <button className="text-button" type="button" onClick={() => { setDocumentoId(documento.id); setErroModal(""); setModalAberto(true); }}>Criar primeira auditoria</button>}</div> : <div className="audit-record-list">{itens.map((artefato) => <article className="audit-record" key={artefato.id}>
              <Link
                className="audit-record-link"
                to={`/planos/${plano.id}/artefatos/${artefato.id}/auditorias/${artefato.auditoriaId}`}
                aria-label={`Abrir auditoria do artefato ${artefato.nome}`}
              >
                <div className="audit-record-title"><span aria-hidden="true"><Icon name="clipboard" size={20} /></span><div><h3>{artefato.nome}</h3><p>Versão {artefato.versao}</p></div></div>
                <dl className="audit-record-meta"><div><dt>Auditor</dt><dd>{artefato.auditorNome}</dd></div><div><dt>Data planejada</dt><dd>{formatarData(artefato.dataPlanejada)}</dd></div><div><dt>Checklists</dt><dd>{artefato.totalChecklists}</dd></div></dl>
                <div className="audit-record-references"><span>Referências</span><p>{artefato.documentosReferencia.map((ref) => ref.nomeArquivo).join(", ") || "Nenhuma referência vinculada."}</p></div>
                <div className="audit-record-state"><span className={`status status-${artefato.status.toLowerCase()}`}>{rotuloEnum(artefato.status)}</span><span className="audit-record-arrow" aria-hidden="true"><Icon name="arrowRight" size={19} /></span></div>
              </Link>
              {gerencia && <button className="audit-record-delete text-button danger-text" type="button" onClick={() => void remover(artefato)}>Excluir</button>}
            </article>)}</div>}
          </section>)}</div>
      )}
      <Modal aberto={modalAberto} titulo="Adicionar artefato e auditoria" descricao="Defina o documento auditado, o auditor e as referências que orientarão todos os checklists." aoFechar={() => setModalAberto(false)} amplo><form className="form-stack" onSubmit={criar}>{documentos.length === 0 && <p className="inline-note">Adicione um documento auditado antes de continuar.</p>}{auditores.length === 0 && <p className="inline-note">Adicione ao menos um auditor ao plano.</p>}{referencias.length === 0 && <p className="inline-note">Adicione ao menos um documento de referência.</p>}<div className="form-row"><label>Nome<input required maxLength={150} value={nome} onChange={(e) => setNome(e.target.value)} /></label><label>Versão<input required maxLength={30} value={versao} onChange={(e) => setVersao(e.target.value)} /></label></div><label>Documento auditado<select required value={documentoId} onChange={(e) => setDocumentoId(e.target.value)}><option value="">Selecione</option>{documentos.map((doc) => <option key={doc.id} value={doc.id}>{doc.nome} · {doc.versao}</option>)}</select></label><div className="form-row"><label>Auditor<select required value={auditorId} onChange={(e) => setAuditorId(e.target.value)}><option value="">Selecione</option>{auditores.map((auditor) => <option key={auditor.id} value={auditor.id}>{auditor.nome}</option>)}</select></label><label>Data planejada<input type="date" min={new Date().toISOString().slice(0, 10)} required value={dataPlanejada} onChange={(e) => setDataPlanejada(e.target.value)} /></label></div><fieldset className="reference-selector"><legend>Documentos de referência</legend>{referencias.map((ref) => <label key={ref.id}><input type="checkbox" checked={referenciasSelecionadas.includes(ref.id)} onChange={(e) => setReferenciasSelecionadas((atuais) => e.target.checked ? [...atuais, ref.id] : atuais.filter((id) => id !== ref.id))} /><span><strong>{ref.nome}</strong><small>{ref.nomeArquivo} · versão {ref.versao}</small></span></label>)}</fieldset>{erroModal && <ErrorMessage mensagem={erroModal} />}<div className="form-actions"><button className="button button-primary" disabled={salvando || !documentos.length || !auditores.length || !referencias.length}>{salvando ? "Criando..." : "Criar artefato e auditoria"}</button><button className="button button-secondary" type="button" onClick={() => setModalAberto(false)}>Cancelar</button></div></form></Modal>
    </div>
  );
}
