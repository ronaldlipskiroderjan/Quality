import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { useOutletContext } from "react-router-dom";
import { EmptyState, ErrorMessage, LoadingState, SuccessMessage } from "../components/Feedback";
import { useConfirm } from "../components/ConfirmProvider";
import { Icon } from "../components/Icon";
import { Modal } from "../components/Modal";
import { PageHeader } from "../components/PageHeader";
import { Pagination } from "../components/Pagination";
import type { PlanOutletContext } from "../layouts/PlanLayout";
import { mensagemErro } from "../lib/api";
import { formatarData } from "../lib/format";
import { atividadesPlanoApi, participantesApi } from "../services/qualityApi";
import type { AtividadePlano, Pagina, Participante } from "../types/api";

export function ParticipantsPage() {
  const { plano, pode } = useOutletContext<PlanOutletContext>();
  const confirmar = useConfirm();
  const [participantes, setParticipantes] = useState<Participante[]>([]);
  const [atividades, setAtividades] = useState<Pagina<AtividadePlano> | null>(null);
  const [modalAberto, setModalAberto] = useState(false);
  const [email, setEmail] = useState("");
  const [erro, setErro] = useState("");
  const [erroModal, setErroModal] = useState("");
  const [sucesso, setSucesso] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const gerencia = pode("GERENCIAR_PARTICIPANTES");

  const carregar = useCallback(async () => {
    try {
      setCarregando(true); setErro("");
      const [equipe, log] = await Promise.all([
        participantesApi.listarTodos(plano.id),
        atividadesPlanoApi.listar(plano.id),
      ]);
      setParticipantes(equipe.conteudo);
      setAtividades(log);
    } catch (error) { setErro(mensagemErro(error)); } finally { setCarregando(false); }
  }, [plano.id]);

  useEffect(() => { void carregar(); }, [carregar]);

  async function carregarAtividades(numero: number) {
    try {
      setAtividades(await atividadesPlanoApi.listar(plano.id, numero));
    } catch (error) {
      setErro(mensagemErro(error));
    }
  }
  const auditores = useMemo(
    () => participantes.filter((item) => item.papel === "AUDITOR_RESPONSAVEL_QUALIDADE"),
    [participantes],
  );
  const iniciais = (nome: string) => nome
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0]?.toUpperCase())
    .join("");

  async function adicionar(event: FormEvent) {
    event.preventDefault();
    try {
      setSalvando(true); setErroModal("");
      await participantesApi.adicionar(plano.id, email.trim(), "AUDITOR_RESPONSAVEL_QUALIDADE");
      setEmail(""); setModalAberto(false); await carregar(); setSucesso("Auditor adicionado.");
    } catch (error) { setErroModal(mensagemErro(error)); } finally { setSalvando(false); }
  }

  async function remover(participante: Participante) {
    if (!await confirmar({
      titulo: "Remover auditor do plano?",
      mensagem: `${participante.nome} perderá o acesso imediatamente. Auditorias em andamento serão transferidas para outro auditor, e o histórico já registrado será preservado.`,
      textoConfirmar: "Remover auditor",
      perigo: true,
    })) return;
    try { await participantesApi.remover(plano.id, participante.id); await carregar(); setSucesso("Auditor removido."); }
    catch (error) { setErro(mensagemErro(error)); }
  }

  return (
    <div className="section-content participants-page">
      <PageHeader titulo="Auditores" acao={gerencia ? <button className="button button-primary" onClick={() => { setErroModal(""); setModalAberto(true); }}>Adicionar auditor</button> : undefined} />
      {erro && <ErrorMessage mensagem={erro} />}{sucesso && <SuccessMessage mensagem={sucesso} aoFechar={() => setSucesso("")} />}
      {carregando ? <LoadingState mensagem="Carregando auditores e atividades..." /> : (
        <>
          {auditores.length === 0 ? <EmptyState mensagem="Nenhum auditor encontrado." /> : (
            <div className="table-wrap"><table><thead><tr><th>Auditor</th><th>Incluído em</th>{gerencia && <th>Ações</th>}</tr></thead><tbody>{auditores.map((auditor) => <tr key={auditor.id}><td><strong>{auditor.nome}</strong><span className="cell-detail">{auditor.email}</span></td><td>{formatarData(auditor.criadoEm)}</td>{gerencia && <td><button className="text-button danger-text" onClick={() => void remover(auditor)}>Remover</button></td>}</tr>)}</tbody></table></div>
          )}
          <section className="activity-section" aria-labelledby="log-atividades">
            <header className="activity-header">
              <span className="activity-header-icon" aria-hidden="true"><Icon name="activity" size={24} /></span>
              <div><h2 id="log-atividades">Histórico do plano</h2></div>
            </header>
            {!atividades || atividades.conteudo.length === 0 ? <EmptyState titulo="Nenhuma alteração registrada" mensagem="As próximas mudanças feitas pelos auditores aparecerão nesta linha do tempo." /> : <>
              <ol className="activity-feed">
                {atividades.conteudo.map((atividade) => (
                  <li key={atividade.id}>
                    <span className="activity-avatar" aria-hidden="true">{iniciais(atividade.autorNome)}</span>
                    <div className="activity-entry">
                      <div className="activity-entry-heading">
                        <div><strong>{atividade.autorNome}</strong><span className="activity-action">{rotuloAcao(atividade.acao)}</span></div>
                        <time dateTime={atividade.criadoEm}><Icon name="clock" size={14} />{formatarData(atividade.criadoEm)}</time>
                      </div>
                      <p>{atividade.descricao}</p>
                    </div>
                  </li>
                ))}
              </ol>
              <Pagination pagina={atividades} aoMudar={(numero) => void carregarAtividades(numero)} />
            </>}
          </section>
        </>
      )}
      <Modal aberto={modalAberto} titulo="Adicionar auditor" descricao="O usuário receberá acesso completo ao plano como auditor e responsável de qualidade." aoFechar={() => setModalAberto(false)}><form className="form-stack" onSubmit={adicionar}><label>E-mail do usuário cadastrado<input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></label>{erroModal && <ErrorMessage mensagem={erroModal} />}<div className="form-actions"><button className="button button-primary" disabled={salvando}>{salvando ? "Adicionando..." : "Adicionar auditor"}</button><button className="button button-secondary" type="button" onClick={() => setModalAberto(false)}>Cancelar</button></div></form></Modal>
    </div>
  );
}

function rotuloAcao(acao: string) {
  return acao
    .toLocaleLowerCase("pt-BR")
    .split("_")
    .map((parte) => parte.charAt(0).toLocaleUpperCase("pt-BR") + parte.slice(1))
    .join(" ");
}
