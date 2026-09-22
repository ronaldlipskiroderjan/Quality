import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import { Link, useNavigate, useOutletContext } from "react-router-dom";
import { ErrorMessage, LoadingState, SuccessMessage } from "../components/Feedback";
import { useConfirm } from "../components/ConfirmProvider";
import { Modal } from "../components/Modal";
import { Icon, type IconName } from "../components/Icon";
import { PageHeader } from "../components/PageHeader";
import { ParticipantAvatar } from "../components/ParticipantAvatar";
import { PlanImage } from "../components/PlanImage";
import { PlanForm } from "../components/PlanForm";
import type { PlanOutletContext } from "../layouts/PlanLayout";
import { mensagemErro } from "../lib/api";
import { formatarData, formatarPrazo, rotuloEnum } from "../lib/format";
import {
  artefatosApi,
  configuracaoPlanoApi,
  documentosApi,
  participantesApi,
  planosApi,
} from "../services/qualityApi";
import type {
  Artefato,
  ConfiguracaoClassificacao,
  Documento,
  FeriadoPlano,
  Participante,
} from "../types/api";

function IdentidadeResponsavel({ planoId, participante }: { planoId: string; participante?: Participante }) {
  if (!participante) return <span className="overview-empty-person">Não definido</span>;

  return (
    <span className="overview-person">
      <ParticipantAvatar planoId={planoId} participante={participante} />
      <span>{participante.nome}</span>
    </span>
  );
}

function ListaResponsaveis({ planoId, participantes }: { planoId: string; participantes: Participante[] }) {
  if (participantes.length === 0) {
    return <p className="overview-empty">Nenhum auditor cadastrado.</p>;
  }

  return (
    <ul className="overview-people-list">
      {participantes.map((participante) => (
        <li key={participante.id}>
          <IdentidadeResponsavel planoId={planoId} participante={participante} />
        </li>
      ))}
    </ul>
  );
}

function TabelaResumoDocumentos({ documentos, vazio, rotulo }: { documentos: Documento[]; vazio: string; rotulo: string }) {
  if (documentos.length === 0) return <p className="overview-empty">{vazio}</p>;

  return (
    <div className="overview-document-table-wrap">
      <table className="overview-document-table">
        <caption className="visually-hidden">{rotulo}</caption>
        <thead>
          <tr>
            <th scope="col">Nome</th>
            <th scope="col">Arquivo</th>
            <th scope="col">Versão</th>
          </tr>
        </thead>
        <tbody>
          {documentos.map((documento) => (
            <tr key={documento.id}>
              <td>{documento.nome}</td>
              <td className="overview-document-filename">{documento.nomeArquivo}</td>
              <td>{documento.versao}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function TabelaResumoArtefatos({
  artefatos,
  documentos,
}: {
  artefatos: Artefato[];
  documentos: Documento[];
}) {
  if (artefatos.length === 0) {
    return <p className="overview-empty">Nenhum artefato avaliado cadastrado.</p>;
  }

  const documentosPorId = new Map(
    documentos.map((documento) => [documento.id, documento]),
  );

  return (
    <div className="overview-document-table-wrap">
      <table className="overview-document-table overview-artifact-table">
        <caption className="visually-hidden">Artefatos avaliados</caption>
        <thead>
          <tr>
            <th scope="col">Nome</th>
            <th scope="col">Nome real do arquivo</th>
            <th scope="col">Versão</th>
          </tr>
        </thead>
        <tbody>
          {[...artefatos]
            .sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"))
            .map((artefato) => (
              <tr key={artefato.id}>
                <td>{artefato.nome}</td>
                <td className="overview-document-filename">
                  {documentosPorId.get(artefato.documentoId)?.nomeArquivo ?? "Arquivo não localizado"}
                </td>
                <td>{artefato.versao}</td>
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
}

function CabecalhoSecao({ icone, titulo, descricao, acao }: { icone: IconName; titulo: string; descricao?: string; acao: ReactNode }) {
  return (
    <header className="quality-plan-section-header">
      <div className="quality-plan-section-title">
        <span className="quality-plan-section-symbol" aria-hidden="true"><Icon name={icone} size={26} /></span>
        <div><h2>{titulo}</h2>{descricao && <p>{descricao}</p>}</div>
      </div>
      {acao}
    </header>
  );
}

export function PlanOverviewPage() {
  const { plano, pode, recarregarPlano } = useOutletContext<PlanOutletContext>();
  const confirmar = useConfirm();
  const navigate = useNavigate();
  const carregamentoId = useRef(0);
  const [participantes, setParticipantes] = useState<Participante[]>([]);
  const [classificacoes, setClassificacoes] = useState<ConfiguracaoClassificacao[]>([]);
  const [feriados, setFeriados] = useState<FeriadoPlano[]>([]);
  const [documentosReferencia, setDocumentosReferencia] = useState<Documento[]>([]);
  const [documentosAuditados, setDocumentosAuditados] = useState<Documento[]>([]);
  const [artefatos, setArtefatos] = useState<Artefato[]>([]);
  const [editandoPlano, setEditandoPlano] = useState(false);
  const [revisaoImagem, setRevisaoImagem] = useState(0);
  const [editandoSuperiores, setEditandoSuperiores] = useState(false);
  const [editandoClassificacoes, setEditandoClassificacoes] = useState(false);
  const [editandoFeriados, setEditandoFeriados] = useState(false);
  const [emailN1, setEmailN1] = useState("");
  const [emailN2, setEmailN2] = useState("");
  const [erro, setErro] = useState("");
  const [erroSuperiores, setErroSuperiores] = useState("");
  const [erroClassificacoes, setErroClassificacoes] = useState("");
  const [erroFeriados, setErroFeriados] = useState("");
  const [dataFeriado, setDataFeriado] = useState("");
  const [nomeFeriado, setNomeFeriado] = useState("");
  const [mensagem, setMensagem] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);

  const carregarResumo = useCallback(async () => {
    const requisicaoId = ++carregamentoId.current;
    try {
      setCarregando(true);
      setErro("");
      const [equipe, regras, calendario, referencias, auditados, itensAuditaveis] = await Promise.all([
        participantesApi.listarTodos(plano.id),
        configuracaoPlanoApi.listarClassificacoes(plano.id),
        configuracaoPlanoApi.listarFeriados(plano.id),
        documentosApi.listarTodos(plano.id, "REFERENCIA"),
        documentosApi.listarTodos(plano.id, "AUDITADO"),
        artefatosApi.listarTodos(plano.id),
      ]);

      if (requisicaoId !== carregamentoId.current) return;
      setParticipantes(equipe.conteudo);
      setClassificacoes(regras);
      setFeriados(calendario);
      setDocumentosReferencia(referencias.conteudo);
      setDocumentosAuditados(auditados.conteudo);
      setArtefatos(itensAuditaveis.conteudo);
      setEmailN1(equipe.conteudo.find((item) => item.papel === "SUPERIOR_N1")?.email ?? "");
      setEmailN2(equipe.conteudo.find((item) => item.papel === "SUPERIOR_N2")?.email ?? "");
    } catch (error) {
      if (requisicaoId === carregamentoId.current) setErro(mensagemErro(error));
    } finally {
      if (requisicaoId === carregamentoId.current) setCarregando(false);
    }
  }, [plano.id]);

  useEffect(() => {
    void carregarResumo();
    return () => {
      carregamentoId.current += 1;
    };
  }, [carregarResumo]);

  const auditores = useMemo(
    () => [...participantes]
      .filter((item) => item.papel === "AUDITOR_RESPONSAVEL_QUALIDADE")
      .sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR")),
    [participantes],
  );
  const superiorN1 = participantes.find((item) => item.papel === "SUPERIOR_N1");
  const superiorN2 = participantes.find((item) => item.papel === "SUPERIOR_N2");
  async function salvarSuperiores(event: FormEvent) {
    event.preventDefault();
    try {
      setSalvando(true);
      setErroSuperiores("");
      await participantesApi.definirSuperiores(plano.id, emailN1.trim(), emailN2.trim());
      await carregarResumo();
      setEditandoSuperiores(false);
      setMensagem("Superiores atualizados.");
    } catch (error) {
      setErroSuperiores(mensagemErro(error));
    } finally {
      setSalvando(false);
    }
  }

  async function salvarClassificacoes(event: FormEvent) {
    event.preventDefault();
    try {
      setSalvando(true);
      setErroClassificacoes("");
      const atualizadas = await configuracaoPlanoApi.atualizarClassificacoes(
        plano.id,
        classificacoes.map(({ classificacao, prazoDias, prazoHoras, ativa }) => ({
          classificacao,
          prazoDias,
          prazoHoras,
          ativa,
        })),
      );
      setClassificacoes(atualizadas);
      setEditandoClassificacoes(false);
      setMensagem("Classificações e prazos atualizados.");
    } catch (error) {
      setErroClassificacoes(mensagemErro(error));
    } finally {
      setSalvando(false);
    }
  }

  async function adicionarFeriado(event: FormEvent) {
    event.preventDefault();
    try {
      setSalvando(true);
      setErroFeriados("");
      const criado = await configuracaoPlanoApi.adicionarFeriado(plano.id, {
        data: dataFeriado,
        nome: nomeFeriado.trim(),
      });
      setFeriados((atuais) => [...atuais, criado]
        .sort((a, b) => a.data.localeCompare(b.data)));
      setDataFeriado("");
      setNomeFeriado("");
      setMensagem("Feriado adicionado ao calendário do plano.");
    } catch (error) {
      setErroFeriados(mensagemErro(error));
    } finally {
      setSalvando(false);
    }
  }

  async function removerFeriado(feriado: FeriadoPlano) {
    if (!await confirmar({
      titulo: "Remover este feriado?",
      mensagem: `${feriado.nome}, em ${formatarData(feriado.data)}, deixará de ser ignorado nos novos prazos.`,
      textoConfirmar: "Remover feriado",
      perigo: true,
    })) return;
    try {
      setErroFeriados("");
      await configuracaoPlanoApi.removerFeriado(plano.id, feriado.id);
      setFeriados((atuais) => atuais.filter((item) => item.id !== feriado.id));
      setMensagem("Feriado removido do calendário do plano.");
    } catch (error) {
      setErroFeriados(mensagemErro(error));
    }
  }

  async function concluir() {
    if (!await confirmar({
      titulo: "Concluir este plano?",
      mensagem: "O plano ficará concluído e continuará disponível para consulta.",
      textoConfirmar: "Concluir plano",
    })) return;
    try {
      await planosApi.concluir(plano.id);
      await recarregarPlano();
      setMensagem("Plano concluído.");
    } catch (error) {
      setErro(mensagemErro(error));
    }
  }

  async function excluir() {
    if (!await confirmar({
      titulo: "Excluir este plano definitivamente?",
      mensagem: plano.status === "PENDENTE"
        ? "Este plano ainda está em execução. Ao confirmar, documentos, auditorias, checklists, não conformidades e históricos associados serão excluídos definitivamente."
        : "O plano e todos os dados associados serão excluídos definitivamente.",
      textoConfirmar: "Excluir plano",
      perigo: true,
    })) return;
    try {
      await planosApi.excluir(plano.id);
      navigate("/dashboard", { replace: true });
    } catch (error) {
      setErro(mensagemErro(error));
    }
  }

  return (
    <div className="section-content plan-overview-page">
      <PageHeader
        titulo="Plano de Garantia da Qualidade"
        acao={pode("EDITAR") || pode("CONCLUIR") || pode("EXCLUIR") ? (
          <div className="plan-header-actions">
            {pode("EDITAR") && (
              <button className="button button-primary" onClick={() => setEditandoPlano(true)}>
                Editar plano
              </button>
            )}
            {pode("CONCLUIR") && plano.status !== "CONCLUIDO" && (
              <button className="button button-secondary" onClick={() => void concluir()}>
                Concluir plano
              </button>
            )}
            {pode("EXCLUIR") && (
              <button className="button button-danger" onClick={() => void excluir()}>
                Excluir plano
              </button>
            )}
          </div>
        ) : undefined}
      />
      {erro && <ErrorMessage mensagem={erro} />}
      {mensagem && <SuccessMessage mensagem={mensagem} aoFechar={() => setMensagem("")} />}

      <article className="quality-plan-sheet" aria-label="Resumo do Plano de Garantia da Qualidade">
        <section className={`quality-plan-section quality-plan-purpose${plano.temImagem ? " has-image" : ""}`}>
          {plano.temImagem && <PlanImage planoId={plano.id} revisao={revisaoImagem} className="quality-plan-cover" alt="Imagem do plano" />}
          <header className="quality-plan-section-header">
            <div>
              <h2>Identificação e finalidade</h2>
              <p>Informações que orientam a aplicação deste plano.</p>
            </div>
          </header>
          <dl className="quality-plan-identification">
            <div><dt>Projeto</dt><dd>{plano.nomeProjeto}</dd></div>
            <div><dt>Versão</dt><dd>{plano.versao}</dd></div>
            <div><dt>Situação</dt><dd>{rotuloEnum(plano.status)}</dd></div>
          </dl>
          <div className="quality-plan-copy">
            <div><h3>Objetivo</h3><p>{plano.objetivo}</p></div>
            <div><h3>Visão geral</h3><p>{plano.visaoGeral}</p></div>
          </div>
        </section>

        {carregando ? (
          <LoadingState mensagem="Organizando as informações do plano..." />
        ) : (
          <div className="plan-overview-sections">
            <section className="quality-plan-section quality-plan-people">
              <CabecalhoSecao icone="users" titulo="Responsáveis pela qualidade" acao={<Link className="text-button" to={`/planos/${plano.id}/equipe`}>Ver auditores</Link>} />
              <div className="quality-plan-columns">
                <div>
                  <h3>Auditores</h3>
                  <ListaResponsaveis planoId={plano.id} participantes={auditores} />
                </div>
                <div>
                  <div className="overview-subsection-heading">
                    <h3>Escalonamento</h3>
                    {pode("GERENCIAR_PARTICIPANTES") && (
                      <button
                        className="text-button"
                        onClick={() => {
                          setErroSuperiores("");
                          setEditandoSuperiores(true);
                        }}
                      >
                        Configurar
                      </button>
                    )}
                  </div>
                  <dl className="overview-compact-details">
                    <div><dt>Superior N1</dt><dd><IdentidadeResponsavel planoId={plano.id} participante={superiorN1} /></dd></div>
                    <div><dt>Superior N2</dt><dd><IdentidadeResponsavel planoId={plano.id} participante={superiorN2} /></dd></div>
                  </dl>
                </div>
              </div>
            </section>

            <section className="quality-plan-section quality-plan-documents">
              <CabecalhoSecao icone="document" titulo="Documentação aplicável" acao={pode("GERENCIAR_DOCUMENTOS") ? <Link className="text-button" to={`/planos/${plano.id}/documentos`}>Ver documentos</Link> : null} />
              <div className="quality-plan-columns">
                <div>
                  <h3>Documentos de referência</h3>
                  <TabelaResumoDocumentos
                    documentos={documentosReferencia}
                    vazio="Nenhum documento de referência cadastrado."
                    rotulo="Documentos de referência"
                  />
                </div>
                <div>
                  <h3>Documentos auditados</h3>
                  <TabelaResumoDocumentos
                    documentos={documentosAuditados}
                    vazio="Nenhum documento auditado cadastrado."
                    rotulo="Documentos auditados"
                  />
                </div>
              </div>
            </section>

            <section className="quality-plan-section quality-plan-audits">
              <CabecalhoSecao icone="clipboard" titulo="Artefatos avaliados" acao={pode("AUDITAR") ? <Link className="text-button" to={`/planos/${plano.id}/artefatos`}>Ver auditorias</Link> : null} />
              <TabelaResumoArtefatos
                artefatos={artefatos}
                documentos={documentosAuditados}
              />
            </section>

            <section className="quality-plan-section quality-plan-rules">
              <CabecalhoSecao icone="warning" titulo="Classificações de não conformidade" descricao="Prazos em dias úteis, desconsiderando fins de semana e feriados." acao={pode("EDITAR") ? (
                <div className="quality-plan-section-actions">
                  <button className="text-button" onClick={() => { setErroFeriados(""); setEditandoFeriados(true); }}>Feriados</button>
                  <button className="text-button" onClick={() => { setErroClassificacoes(""); setEditandoClassificacoes(true); }}>Configurar prazos</button>
                </div>
              ) : null} />
              <div className="classification-overview-table-wrap">
                <table className="classification-overview-table">
                  <caption className="visually-hidden">Classificações e prazos das não conformidades</caption>
                  <thead>
                    <tr>
                      <th scope="col">Classificação</th>
                      <th scope="col">Prazo para resolução</th>
                      <th scope="col">Situação</th>
                    </tr>
                  </thead>
                  <tbody>
                    {classificacoes.map((item) => (
                      <tr key={item.id}>
                        <th scope="row">{rotuloEnum(item.classificacao)}</th>
                        <td>{item.ativa ? formatarPrazo(item.prazoDias, item.prazoHoras) : "—"}</td>
                        <td>
                          <span className={`classification-state ${item.ativa ? "is-active" : "is-inactive"}`}>
                            {item.ativa ? "Ativa" : "Desativada"}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="business-calendar-summary">
                <Icon name="clock" size={18} />
                <p>
                  Feriados nacionais do Brasil são aplicados automaticamente.
                  {feriados.length > 0 && ` ${feriados.length} ${feriados.length === 1 ? "feriado adicional cadastrado" : "feriados adicionais cadastrados"}.`}
                </p>
              </div>
            </section>
          </div>
        )}
      </article>

      <Modal aberto={editandoPlano} titulo="Editar plano" aoFechar={() => setEditandoPlano(false)} amplo>
        <PlanForm
          valorInicial={{ nomeProjeto: plano.nomeProjeto, versao: plano.versao, objetivo: plano.objetivo, visaoGeral: plano.visaoGeral }}
          planoId={plano.id}
          temImagem={plano.temImagem}
          revisaoImagem={revisaoImagem}
          textoBotao="Salvar alterações"
          aoSalvar={async (entrada, imagem, removerImagem) => {
            await planosApi.atualizar(plano.id, entrada, imagem, removerImagem);
            await recarregarPlano();
            setRevisaoImagem((atual) => atual + 1);
            setEditandoPlano(false);
            setMensagem("Alterações salvas.");
          }}
          aoCancelar={() => setEditandoPlano(false)}
        />
      </Modal>

      <Modal aberto={editandoSuperiores} titulo="Definir superiores" descricao="Informe usuários cadastrados. Cada nível aceita somente uma pessoa." aoFechar={() => setEditandoSuperiores(false)}>
        <form className="form-stack" onSubmit={salvarSuperiores}>
          <label>Superior N1<input type="email" value={emailN1} onChange={(event) => setEmailN1(event.target.value)} placeholder="superior.n1@empresa.com" /></label>
          <label>Superior N2<input type="email" value={emailN2} onChange={(event) => setEmailN2(event.target.value)} placeholder="superior.n2@empresa.com" /></label>
          <p className="form-help">Deixe o campo vazio para remover o superior daquele nível.</p>
          {erroSuperiores && <ErrorMessage mensagem={erroSuperiores} />}
          <div className="form-actions">
            <button className="button button-primary" disabled={salvando}>{salvando ? "Salvando..." : "Salvar superiores"}</button>
            <button className="button button-secondary" type="button" onClick={() => setEditandoSuperiores(false)}>Cancelar</button>
          </div>
        </form>
      </Modal>

      <Modal aberto={editandoClassificacoes} titulo="Classificações e prazos" descricao="Cada dia representa um dia útil. Sábados, domingos e feriados não consomem o prazo." aoFechar={() => setEditandoClassificacoes(false)}>
        <form className="form-stack" onSubmit={salvarClassificacoes}>
          <div className="classification-editor">
            {classificacoes.map((item, indice) => (
              <div className="classification-row" key={item.id}>
                <label className="checkbox-label">
                  <input type="checkbox" checked={item.ativa} onChange={(event) => setClassificacoes((atuais) => atuais.map((atual, i) => i === indice ? { ...atual, ativa: event.target.checked } : atual))} />
                  {rotuloEnum(item.classificacao)}
                </label>
                <label>Dias úteis<input type="number" min={0} max={365} required disabled={!item.ativa} value={item.prazoDias} onChange={(event) => setClassificacoes((atuais) => atuais.map((atual, i) => i === indice ? { ...atual, prazoDias: Number(event.target.value) } : atual))} /></label>
                <label>Horas<input type="number" min={0} max={23} required disabled={!item.ativa} value={item.prazoHoras} onChange={(event) => setClassificacoes((atuais) => atuais.map((atual, i) => i === indice ? { ...atual, prazoHoras: Number(event.target.value) } : atual))} /></label>
              </div>
            ))}
          </div>
          {erroClassificacoes && <ErrorMessage mensagem={erroClassificacoes} />}
          <div className="form-actions">
            <button className="button button-primary" disabled={salvando}>{salvando ? "Salvando..." : "Salvar prazos"}</button>
            <button className="button button-secondary" type="button" onClick={() => setEditandoClassificacoes(false)}>Cancelar</button>
          </div>
        </form>
      </Modal>

      <Modal aberto={editandoFeriados} titulo="Calendário de feriados" descricao="Os feriados nacionais já são considerados. Cadastre aqui feriados estaduais, municipais ou internos." aoFechar={() => setEditandoFeriados(false)} amplo>
        <form className="form-stack" onSubmit={adicionarFeriado}>
          <div className="holiday-entry-form">
            <label>Data<input type="date" required value={dataFeriado} onChange={(event) => setDataFeriado(event.target.value)} /></label>
            <label>Nome<input required maxLength={120} value={nomeFeriado} onChange={(event) => setNomeFeriado(event.target.value)} placeholder="Ex.: aniversário da cidade" /></label>
            <button className="button button-primary" disabled={salvando}>{salvando ? "Adicionando..." : "Adicionar feriado"}</button>
          </div>
          {erroFeriados && <ErrorMessage mensagem={erroFeriados} />}
        </form>
        <div className="holiday-list-section">
          <h3>Feriados adicionais</h3>
          {feriados.length === 0 ? <p className="form-help">Nenhum feriado estadual, municipal ou interno cadastrado.</p> : (
            <ul className="holiday-list">
              {feriados.map((feriado) => (
                <li key={feriado.id}>
                  <span><strong>{feriado.nome}</strong><time dateTime={feriado.data}>{formatarData(feriado.data)}</time></span>
                  <button className="text-button danger-text" type="button" onClick={() => void removerFeriado(feriado)}>Remover</button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </Modal>
    </div>
  );
}
