import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { EmptyState, ErrorMessage, LoadingState } from "../components/Feedback";
import { Icon } from "../components/Icon";
import { PageHeader } from "../components/PageHeader";
import { mensagemErro } from "../lib/api";
import { formatarData, rotuloStatusNaoConformidade } from "../lib/format";
import { naoConformidadesApi } from "../services/qualityApi";
import type { NaoConformidade, Plano } from "../types/api";

type VisaoFila = "MINHAS" | "EQUIPE";

function TabelaSolicitacoes({ itens, planoId }: { itens: NaoConformidade[]; planoId: string }) {
  return (
    <div className="resolution-table-scroll">
      <table className="resolution-data-table resolution-request-table">
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
                <td>{item.responsavelNome ?? <span className="resolution-unassigned">Equipe de resolução</span>}</td>
                <td><time className={vencida ? "is-overdue" : undefined} dateTime={item.prazoEm ?? undefined}>{formatarData(item.prazoEm)}</time></td>
                <td><span className={`status status-${item.status.toLowerCase()}`}>{rotuloStatusNaoConformidade(item.status)}</span></td>
                <td><Link className="resolution-open" to={`/planos/${planoId}/nao-conformidades/${item.id}`}>Abrir</Link></td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

export function PlanResolutionInboxPage({ plano }: { plano: Plano }) {
  const { usuario } = useAuth();
  const [itens, setItens] = useState<NaoConformidade[]>([]);
  const [visao, setVisao] = useState<VisaoFila>("MINHAS");
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(true);

  const carregar = useCallback(async () => {
    try {
      setCarregando(true);
      setErro("");
      setItens(await naoConformidadesApi.listarTodasDaEquipeNoPlano(plano.id));
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setCarregando(false);
    }
  }, [plano.id]);

  useEffect(() => { void carregar(); }, [carregar]);

  const atribuidas = useMemo(
    () => itens.filter((item) => item.responsavelEmail?.toLowerCase() === usuario?.email.toLowerCase()),
    [itens, usuario?.email],
  );
  const vencidas = useMemo(() => itens.filter((item) => (
    item.prazoEm
    && new Date(item.prazoEm).getTime() < Date.now()
    && !["CONCLUIDA", "CANCELADA"].includes(item.status)
  )).length, [itens]);
  const itensVisiveis = visao === "MINHAS" ? atribuidas : itens;
  const itensOrdenados = useMemo(() => [...itensVisiveis].sort((a, b) => (
    (a.prazoEm ?? "9999").localeCompare(b.prazoEm ?? "9999")
  )), [itensVisiveis]);

  return (
    <div className="section-content resolution-plan-view">
      <PageHeader
        titulo="Solicitações de resolução"
        descricao={`${plano.nomeProjeto} · versão ${plano.versao}`}
      />
      {erro && <ErrorMessage mensagem={erro} />}
      {carregando ? <LoadingState mensagem="Carregando solicitações da equipe..." /> : (
        <>
          <dl className="resolution-inbox-summary" aria-label="Resumo das solicitações deste plano">
            <div><dt>Atribuídas a mim</dt><dd>{atribuidas.length}</dd></div>
            <div><dt>Fila da equipe</dt><dd>{itens.length}</dd></div>
            <div><dt>Com prazo vencido</dt><dd>{vencidas}</dd></div>
          </dl>

          <section className="resolution-requests" aria-labelledby="titulo-fila-resolucao">
            <header className="resolution-requests-header">
              <div className="resolution-requests-title">
                <span aria-hidden="true"><Icon name="resolution" size={21} /></span>
                <div>
                  <h2 id="titulo-fila-resolucao">Fila de trabalho</h2>
                  <p>{visao === "MINHAS" ? "Não conformidades atribuídas diretamente a você." : "Todas as não conformidades enviadas para a equipe."}</p>
                </div>
              </div>
              <div className="resolution-view-switch" aria-label="Visualização da fila">
                <button className={visao === "MINHAS" ? "active" : ""} type="button" aria-pressed={visao === "MINHAS"} onClick={() => setVisao("MINHAS")}>Minhas NCs <span>{atribuidas.length}</span></button>
                <button className={visao === "EQUIPE" ? "active" : ""} type="button" aria-pressed={visao === "EQUIPE"} onClick={() => setVisao("EQUIPE")}>Toda a equipe <span>{itens.length}</span></button>
              </div>
            </header>

            {itensOrdenados.length === 0 ? (
              <EmptyState
                titulo={visao === "MINHAS" ? "Nenhuma solicitação atribuída" : "A equipe está em dia"}
                mensagem={visao === "MINHAS"
                  ? "Quando uma não conformidade for atribuída a você, ela aparecerá nesta fila."
                  : "Nenhuma não conformidade foi enviada para esta equipe."}
              />
            ) : <TabelaSolicitacoes itens={itensOrdenados} planoId={plano.id} />}
          </section>
        </>
      )}
    </div>
  );
}
