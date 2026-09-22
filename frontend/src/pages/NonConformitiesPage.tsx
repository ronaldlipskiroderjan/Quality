import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, useOutletContext } from "react-router-dom";
import { EmptyState, ErrorMessage, LoadingState, SuccessMessage } from "../components/Feedback";
import { useConfirm } from "../components/ConfirmProvider";
import { Icon } from "../components/Icon";
import { Modal } from "../components/Modal";
import { PageHeader } from "../components/PageHeader";
import type { PlanOutletContext } from "../layouts/PlanLayout";
import { mensagemErro } from "../lib/api";
import { formatarData, rotuloStatusNaoConformidade } from "../lib/format";
import { naoConformidadesApi, participantesApi } from "../services/qualityApi";
import type { NaoConformidade, Participante } from "../types/api";

function TabelaNaoConformidades({ itens, planoId }: { itens: NaoConformidade[]; planoId: string }) {
  if (itens.length === 0) {
    return <EmptyState titulo="Nenhuma não conformidade enviada" mensagem="As solicitações encaminhadas para a equipe aparecerão aqui." />;
  }

  return (
    <div className="resolution-table-scroll">
      <table className="resolution-data-table resolution-team-table">
        <thead>
          <tr>
            <th scope="col">Artefato e item</th>
            <th scope="col">Responsável</th>
            <th scope="col">Prazo</th>
            <th scope="col">Status</th>
            <th scope="col"><span className="sr-only">Ação</span></th>
          </tr>
        </thead>
        <tbody>
          {itens.map((item) => {
            const vencida = Boolean(item.prazoEm)
              && new Date(item.prazoEm as string).getTime() < Date.now()
              && !["CONCLUIDA", "CANCELADA"].includes(item.status);
            return (
              <tr key={item.id}>
                <td>
                  <strong>{item.artefatoNome} · item {item.itemOrdem}</strong>
                  <span>{item.pergunta}</span>
                </td>
                <td>{item.responsavelNome ?? <span className="resolution-unassigned">Sem responsável</span>}</td>
                <td><time className={vencida ? "is-overdue" : undefined} dateTime={item.prazoEm ?? undefined}>{formatarData(item.prazoEm)}</time></td>
                <td><span className={`status status-${item.status.toLowerCase()}`}>{rotuloStatusNaoConformidade(item.status)}</span></td>
                <td><Link className="resolution-open" to={`/planos/${planoId}/nao-conformidades/${item.id}`}>Acompanhar</Link></td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

export function NonConformitiesPage() {
  const { plano, pode } = useOutletContext<PlanOutletContext>();
  const confirmar = useConfirm();
  const [participantes, setParticipantes] = useState<Participante[]>([]);
  const [naoConformidades, setNaoConformidades] = useState<NaoConformidade[]>([]);
  const [modalAberto, setModalAberto] = useState(false); const [email, setEmail] = useState("");
  const [erro, setErro] = useState(""); const [sucesso, setSucesso] = useState("");
  const [erroModal, setErroModal] = useState("");
  const [carregando, setCarregando] = useState(true); const [salvando, setSalvando] = useState(false);
  const gerencia = pode("GERENCIAR_PARTICIPANTES");

  const carregar = useCallback(async () => {
    try {
      setCarregando(true); setErro("");
      const [equipe, registros] = await Promise.all([
        participantesApi.listarTodos(plano.id), naoConformidadesApi.listarTodas(plano.id),
      ]);
      setParticipantes(equipe.conteudo); setNaoConformidades(registros.conteudo);
    } catch (error) { setErro(mensagemErro(error)); } finally { setCarregando(false); }
  }, [plano.id]);
  useEffect(() => { void carregar(); }, [carregar]);

  const equipe = useMemo(() => participantes.filter((p) => p.papel === "MEMBRO_EQUIPE_RESOLUCAO"), [participantes]);
  const naoConformidadesEnviadas = useMemo(
    () => naoConformidades.filter((nc) => Boolean(nc.enviadaEm)),
    [naoConformidades],
  );
  const semResponsavel = useMemo(
    () => naoConformidadesEnviadas.filter((nc) => !nc.responsavelParticipacaoId),
    [naoConformidadesEnviadas],
  );
  const porResponsavel = useMemo(() => naoConformidadesEnviadas.reduce((resultado, nc) => {
    if (!nc.responsavelParticipacaoId) return resultado;
    const itens = resultado.get(nc.responsavelParticipacaoId) ?? [];
    itens.push(nc);
    resultado.set(nc.responsavelParticipacaoId, itens);
    return resultado;
  }, new Map<string, NaoConformidade[]>()), [naoConformidadesEnviadas]);
  const naoConformidadesOrdenadas = useMemo(() => [...naoConformidadesEnviadas].sort((a, b) => {
    if (!a.responsavelParticipacaoId && b.responsavelParticipacaoId) return -1;
    if (a.responsavelParticipacaoId && !b.responsavelParticipacaoId) return 1;
    return (a.prazoEm ?? "9999").localeCompare(b.prazoEm ?? "9999");
  }), [naoConformidadesEnviadas]);
  const vencidas = useMemo(() => naoConformidadesEnviadas.filter((item) => (
    item.prazoEm
    && new Date(item.prazoEm).getTime() < Date.now()
    && !["CONCLUIDA", "CANCELADA"].includes(item.status)
  )).length, [naoConformidadesEnviadas]);
  const iniciais = (nome: string) => nome.split(" ").filter(Boolean).slice(0, 2).map((parte) => parte[0]?.toUpperCase()).join("");

  async function adicionar(event: FormEvent) {
    event.preventDefault();
    try { setSalvando(true); setErroModal(""); await participantesApi.adicionar(plano.id, email.trim(), "MEMBRO_EQUIPE_RESOLUCAO"); setEmail(""); setModalAberto(false); await carregar(); setSucesso("Membro adicionado à equipe de resolução."); }
    catch (error) { setErroModal(mensagemErro(error)); } finally { setSalvando(false); }
  }
  async function remover(membro: Participante) {
    if (!await confirmar({
      titulo: "Remover membro da equipe?",
      mensagem: `${membro.nome} perderá o acesso imediatamente. As não conformidades em aberto atribuídas a essa pessoa voltarão para a fila sem responsável, e o histórico será preservado.`,
      textoConfirmar: "Remover membro",
      perigo: true,
    })) return;
    try { await participantesApi.remover(plano.id, membro.id); await carregar(); setSucesso("Membro removido."); }
    catch (error) { setErro(mensagemErro(error)); }
  }

  return (
    <div className="section-content resolution-team-page">
      <PageHeader titulo="Equipe de resolução" descricao="Consulte rapidamente quem está responsável por cada não conformidade." acao={gerencia ? <button className="button button-primary" onClick={() => { setErroModal(""); setModalAberto(true); }}>Adicionar membro</button> : undefined} />
      {erro && <ErrorMessage mensagem={erro} />}{sucesso && <SuccessMessage mensagem={sucesso} aoFechar={() => setSucesso("")} />}
      {carregando ? <LoadingState mensagem="Carregando equipe e não conformidades..." /> : <>
        <dl className="resolution-inbox-summary" aria-label="Resumo da equipe de resolução">
          <div><dt>Membros</dt><dd>{equipe.length}</dd></div>
          <div><dt>Não conformidades enviadas</dt><dd>{naoConformidadesEnviadas.length}</dd></div>
          <div><dt>Sem responsável</dt><dd>{semResponsavel.length}</dd></div>
          <div><dt>Com prazo vencido</dt><dd>{vencidas}</dd></div>
        </dl>

        <section className="resolution-operations" aria-labelledby="fila-resolucao">
          <header className="resolution-section-header">
            <span aria-hidden="true"><Icon name="clipboard" size={22} /></span>
            <div><h2 id="fila-resolucao">Não conformidades enviadas</h2><p>Ordenadas para destacar primeiro os itens sem responsável e os prazos mais próximos.</p></div>
            <span className="resolution-section-count">{naoConformidadesEnviadas.length}</span>
          </header>
          <TabelaNaoConformidades itens={naoConformidadesOrdenadas} planoId={plano.id} />
        </section>

        <section className="resolution-roster" aria-labelledby="membros-resolucao"><header className="resolution-section-header"><span aria-hidden="true"><Icon name="users" size={22} /></span><div><h2 id="membros-resolucao">Membros da equipe</h2><p>Participantes que recebem e tratam as solicitações deste plano.</p></div><span className="resolution-section-count">{equipe.length}</span></header>
          {equipe.length === 0 ? <EmptyState mensagem="Nenhum membro cadastrado na equipe de resolução." /> : <div className="resolution-table-scroll"><table className="resolution-data-table resolution-members-table"><thead><tr><th scope="col">Membro</th><th scope="col">E-mail</th><th scope="col">Atribuições</th>{gerencia && <th scope="col"><span className="sr-only">Ação</span></th>}</tr></thead><tbody>{equipe.map((membro) => { const itens = porResponsavel.get(membro.id) ?? []; return <tr key={membro.id}><td><span className="resolution-member-identity"><span className="resolution-member-avatar" aria-hidden="true">{iniciais(membro.nome)}</span><strong>{membro.nome}</strong></span></td><td>{membro.email}</td><td>{itens.length} {itens.length === 1 ? "não conformidade" : "não conformidades"}</td>{gerencia && <td><button className="text-button danger-text" type="button" onClick={() => void remover(membro)}>Remover</button></td>}</tr>; })}</tbody></table></div>}
        </section>
      </>}
      <Modal aberto={modalAberto} titulo="Adicionar à equipe de resolução" descricao="Esse usuário verá apenas as NCs enviadas à equipe e as que forem atribuídas a ele." aoFechar={() => setModalAberto(false)}><form className="form-stack" onSubmit={adicionar}><label>E-mail do usuário cadastrado<input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></label>{erroModal && <ErrorMessage mensagem={erroModal} />}<div className="form-actions"><button className="button button-primary" disabled={salvando}>{salvando ? "Adicionando..." : "Adicionar membro"}</button><button className="button button-secondary" type="button" onClick={() => setModalAberto(false)}>Cancelar</button></div></form></Modal>
    </div>
  );
}
