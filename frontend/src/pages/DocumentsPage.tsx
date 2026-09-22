import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { useOutletContext } from "react-router-dom";
import { EmptyState, ErrorMessage, LoadingState, SuccessMessage } from "../components/Feedback";
import { useConfirm } from "../components/ConfirmProvider";
import { Icon } from "../components/Icon";
import { Modal } from "../components/Modal";
import { PageHeader } from "../components/PageHeader";
import type { PlanOutletContext } from "../layouts/PlanLayout";
import { mensagemErro } from "../lib/api";
import { formatarTamanho } from "../lib/format";
import { documentosApi } from "../services/qualityApi";
import type { ClassificacaoDocumento, Documento } from "../types/api";

function TabelaDocumentos({ id, titulo, descricao, documentos, gerencia, aoAbrir, aoBaixar, aoEditar, aoRemover }: { id: string; titulo: string; descricao: string; documentos: Documento[]; gerencia: boolean; aoAbrir: (doc: Documento) => void; aoBaixar: (doc: Documento) => void; aoEditar: (doc: Documento) => void; aoRemover: (doc: Documento) => void }) {
  return (
    <section className={`document-lane document-lane-${id}`} aria-labelledby={`docs-${id}`}>
      <header className="document-lane-header">
        <span className="document-lane-symbol" aria-hidden="true"><Icon name={id === "referencias" ? "archive" : "document"} size={24} /></span>
        <div><h2 id={`docs-${id}`}>{titulo}</h2><p>{descricao}</p></div>
        <span className="document-lane-count">{documentos.length} {documentos.length === 1 ? "arquivo" : "arquivos"}</span>
      </header>
      {documentos.length === 0 ? <div className="document-lane-empty"><EmptyState mensagem="Nenhum documento nesta categoria." /></div> : (
        <ul className="document-list">{documentos.map((doc) => {
          const visualizavel = doc.tipoArquivo === "application/pdf" || doc.tipoArquivo.startsWith("image/");
          return <li className="document-row" key={doc.id}>
            <div className="document-row-primary"><span aria-hidden="true"><Icon name="document" size={20} /></span><div><strong>{doc.nome}</strong><small>Versão {doc.versao}</small></div></div>
            <div className="document-row-file"><span>Arquivo</span><strong title={doc.nomeArquivo}>{doc.nomeArquivo}</strong><small>{formatarTamanho(doc.tamanho)}</small></div>
            <div className="document-row-actions">{visualizavel && <button className="text-button" type="button" onClick={() => aoAbrir(doc)}>Visualizar</button>}<button className="text-button" type="button" onClick={() => aoBaixar(doc)}>Baixar</button>{gerencia && <><button className="text-button" type="button" onClick={() => aoEditar(doc)}>Editar</button><button className="text-button danger-text" type="button" onClick={() => aoRemover(doc)}>Excluir</button></>}</div>
          </li>;
        })}</ul>
      )}
    </section>
  );
}

export function DocumentsPage() {
  const { plano, pode } = useOutletContext<PlanOutletContext>();
  const confirmar = useConfirm();
  const [referencias, setReferencias] = useState<Documento[]>([]);
  const [auditados, setAuditados] = useState<Documento[]>([]);
  const [modalAberto, setModalAberto] = useState(false);
  const [documentoEdicao, setDocumentoEdicao] = useState<Documento | null>(null);
  const [preview, setPreview] = useState<{ documento: Documento; url: string } | null>(null);
  const [nome, setNome] = useState(""); const [versao, setVersao] = useState("");
  const [nomeEdicao, setNomeEdicao] = useState(""); const [versaoEdicao, setVersaoEdicao] = useState("");
  const [classificacao, setClassificacao] = useState<ClassificacaoDocumento>("REFERENCIA");
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [arquivoEdicao, setArquivoEdicao] = useState<File | null>(null);
  const [erro, setErro] = useState(""); const [sucesso, setSucesso] = useState("");
  const [erroModal, setErroModal] = useState("");
  const [erroEdicao, setErroEdicao] = useState("");
  const [carregando, setCarregando] = useState(true); const [salvando, setSalvando] = useState(false);
  const [salvandoEdicao, setSalvandoEdicao] = useState(false);
  const inputArquivo = useRef<HTMLInputElement>(null);
  const inputArquivoEdicao = useRef<HTMLInputElement>(null);
  const gerencia = pode("GERENCIAR_DOCUMENTOS");

  const carregar = useCallback(async () => {
    try {
      setCarregando(true); setErro("");
      const [refs, docs] = await Promise.all([
        documentosApi.listarTodos(plano.id, "REFERENCIA"),
        documentosApi.listarTodos(plano.id, "AUDITADO"),
      ]);
      setReferencias(refs.conteudo); setAuditados(docs.conteudo);
    } catch (error) { setErro(mensagemErro(error)); } finally { setCarregando(false); }
  }, [plano.id]);

  useEffect(() => { void carregar(); }, [carregar]);
  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview.url); }, [preview]);

  async function enviar(event: FormEvent) {
    event.preventDefault();
    if (!arquivo) return setErroModal("Selecione um arquivo.");
    try {
      setSalvando(true); setErroModal("");
      await documentosApi.adicionar(plano.id, { nome, versao, arquivo, classificacao });
      setNome(""); setVersao(""); setArquivo(null); if (inputArquivo.current) inputArquivo.current.value = "";
      setModalAberto(false); await carregar(); setSucesso("Documento adicionado.");
    } catch (error) { setErroModal(mensagemErro(error)); } finally { setSalvando(false); }
  }

  async function abrir(documento: Documento) {
    try {
      setErro("");
      const blob = await documentosApi.abrir(plano.id, documento.id);
      setPreview({ documento, url: URL.createObjectURL(blob) });
    } catch (error) { setErro(mensagemErro(error)); }
  }

  function iniciarEdicao(documento: Documento) {
    setDocumentoEdicao(documento);
    setNomeEdicao(documento.nome);
    setVersaoEdicao(documento.versao);
    setArquivoEdicao(null);
    setErroEdicao("");
    if (inputArquivoEdicao.current) inputArquivoEdicao.current.value = "";
  }

  function fecharEdicao() {
    if (salvandoEdicao) return;
    setDocumentoEdicao(null);
    setArquivoEdicao(null);
    setErroEdicao("");
  }

  async function atualizar(event: FormEvent) {
    event.preventDefault();
    if (!documentoEdicao) return;

    try {
      setSalvandoEdicao(true);
      setErroEdicao("");
      await documentosApi.atualizar(plano.id, documentoEdicao.id, {
        nome: nomeEdicao,
        versao: versaoEdicao,
        arquivo: arquivoEdicao,
      });
      setDocumentoEdicao(null);
      setArquivoEdicao(null);
      await carregar();
      setSucesso("Documento atualizado.");
    } catch (error) {
      setErroEdicao(mensagemErro(error));
    } finally {
      setSalvandoEdicao(false);
    }
  }

  async function baixar(documento: Documento) { try { await documentosApi.baixar(plano.id, documento); } catch (error) { setErro(mensagemErro(error)); } }
  async function remover(documento: Documento) {
    if (!await confirmar({
      titulo: "Excluir documento?",
      mensagem: documento.classificacao === "AUDITADO"
        ? `“${documento.nome}” pode possuir artefatos e auditorias em andamento. Ao confirmar, o documento e os dados associados serão excluídos definitivamente.`
        : `“${documento.nome}” pode estar sendo usado como referência em auditorias. Ao confirmar, o documento será excluído definitivamente.`,
      textoConfirmar: "Excluir documento",
      perigo: true,
    })) return;
    try { await documentosApi.remover(plano.id, documento.id); await carregar(); setSucesso("Documento excluído."); }
    catch (error) { setErro(mensagemErro(error)); }
  }

  return (
    <div className="section-content documents-page">
      <PageHeader titulo="Documentos" acao={gerencia ? <button className="button button-primary" onClick={() => { setErroModal(""); setModalAberto(true); }}>Adicionar documento</button> : undefined} />
      {erro && <ErrorMessage mensagem={erro} />}{sucesso && <SuccessMessage mensagem={sucesso} aoFechar={() => setSucesso("")} />}
      {carregando ? <LoadingState mensagem="Carregando documentos..." /> : <>
        <section className="document-library-summary" aria-label="Resumo da biblioteca do plano"><span className="document-library-symbol" aria-hidden="true"><Icon name="layers" size={26} /></span><div><h2>Biblioteca do plano</h2><p>Os critérios ficam separados dos arquivos submetidos à auditoria para evitar o uso do documento errado.</p></div><dl><div><dt>Referências</dt><dd>{referencias.length}</dd></div><div><dt>Auditados</dt><dd>{auditados.length}</dd></div></dl></section>
        <div className="documents-grid"><TabelaDocumentos id="referencias" titulo="Documentos de referência" descricao="Normas, modelos e materiais usados como critério durante a verificação." documentos={referencias} gerencia={gerencia} aoAbrir={(doc) => void abrir(doc)} aoBaixar={(doc) => void baixar(doc)} aoEditar={iniciarEdicao} aoRemover={(doc) => void remover(doc)} /><TabelaDocumentos id="auditados" titulo="Documentos auditados" descricao="Arquivos do projeto que originam os artefatos e suas auditorias." documentos={auditados} gerencia={gerencia} aoAbrir={(doc) => void abrir(doc)} aoBaixar={(doc) => void baixar(doc)} aoEditar={iniciarEdicao} aoRemover={(doc) => void remover(doc)} /></div>
      </>}

      <Modal aberto={modalAberto} titulo="Adicionar documento" descricao="Classifique o arquivo para mantê-lo na lista correta." aoFechar={() => setModalAberto(false)}><form className="form-stack" onSubmit={enviar}><div className="form-row"><label>Nome<input required maxLength={150} value={nome} onChange={(e) => setNome(e.target.value)} /></label><label>Versão<input required maxLength={30} value={versao} onChange={(e) => setVersao(e.target.value)} /></label></div><label>Classificação<select value={classificacao} onChange={(e) => setClassificacao(e.target.value as ClassificacaoDocumento)}><option value="REFERENCIA">Referência</option><option value="AUDITADO">Auditado</option></select></label><label>Arquivo<input ref={inputArquivo} type="file" required onChange={(e) => setArquivo(e.target.files?.[0] ?? null)} /></label>{erroModal && <ErrorMessage mensagem={erroModal} />}<div className="form-actions"><button className="button button-primary" disabled={salvando}>{salvando ? "Enviando..." : "Adicionar documento"}</button><button className="button button-secondary" type="button" onClick={() => setModalAberto(false)}>Cancelar</button></div></form></Modal>
      <Modal aberto={Boolean(documentoEdicao)} titulo="Editar documento" descricao="Atualize a identificação, a versão e, se necessário, substitua o arquivo." aoFechar={fecharEdicao}><form className="form-stack" onSubmit={atualizar}><div className="form-row"><label>Nome<input required maxLength={150} value={nomeEdicao} onChange={(e) => setNomeEdicao(e.target.value)} /></label><label>Versão<input required maxLength={30} value={versaoEdicao} onChange={(e) => setVersaoEdicao(e.target.value)} /></label></div>{documentoEdicao && <div className="document-edit-current"><span>Arquivo atual</span><strong>{documentoEdicao.nomeArquivo}</strong><small>{formatarTamanho(documentoEdicao.tamanho)} · {documentoEdicao.classificacao === "REFERENCIA" ? "Documento de referência" : "Documento auditado"}</small></div>}<label>Substituir arquivo <small>(opcional)</small><input ref={inputArquivoEdicao} type="file" onChange={(e) => setArquivoEdicao(e.target.files?.[0] ?? null)} /></label><p className="field-hint">Se nenhum arquivo for selecionado, o arquivo atual será mantido.</p>{erroEdicao && <ErrorMessage mensagem={erroEdicao} />}<div className="form-actions"><button className="button button-primary" disabled={salvandoEdicao}>{salvandoEdicao ? "Salvando..." : "Salvar alterações"}</button><button className="button button-secondary" type="button" disabled={salvandoEdicao} onClick={fecharEdicao}>Cancelar</button></div></form></Modal>
      <Modal aberto={Boolean(preview)} titulo={preview?.documento.nome ?? "Visualizar documento"} descricao={preview ? `${preview.documento.nomeArquivo} · versão ${preview.documento.versao}` : undefined} aoFechar={() => setPreview(null)} amplo>{preview?.documento.tipoArquivo.startsWith("image/") ? <img className="document-preview-image" src={preview.url} alt={`Pré-visualização de ${preview.documento.nome}`} /> : preview && <iframe className="document-preview-frame" src={preview.url} title={`Pré-visualização de ${preview.documento.nome}`} />}</Modal>
    </div>
  );
}
