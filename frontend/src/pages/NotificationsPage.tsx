import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { EmptyState, ErrorMessage, LoadingState } from "../components/Feedback";
import { PageHeader } from "../components/PageHeader";
import { Pagination } from "../components/Pagination";
import { mensagemErro } from "../lib/api";
import { formatarData, rotuloEnum } from "../lib/format";
import { notificacoesApi } from "../services/qualityApi";
import type { Notificacao, Pagina } from "../types/api";

export function NotificationsPage() {
  const [dados, setDados] = useState<Pagina<Notificacao> | null>(null);
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(true);

  const carregar = useCallback(async (pagina = 0) => {
    try {
      setCarregando(true);
      setErro("");
      setDados(await notificacoesApi.listar(pagina));
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => { void carregar(); }, [carregar]);

  async function marcarLida(notificacao: Notificacao) {
    try {
      setErro("");
      await notificacoesApi.marcarLida(notificacao.id);
      window.dispatchEvent(new Event("quality:notifications-updated"));
      await carregar(dados?.pagina ?? 0);
    } catch (error) {
      setErro(mensagemErro(error));
    }
  }

  return (
    <section>
      <PageHeader titulo="Notificações" />
      {erro && <ErrorMessage mensagem={erro} />}
      {carregando && <LoadingState mensagem="Carregando notificações..." />}
      {!carregando && dados?.conteudo.length === 0 && (
        <EmptyState titulo="Você está em dia" mensagem="Novos vencimentos, escalonamentos e eventos aparecerão aqui." />
      )}
      {!carregando && dados && dados.conteudo.length > 0 && (
        <>
          <div className="notification-list">
            {dados.conteudo.map((notificacao) => (
              <article className={notificacao.status === "NAO_LIDA" ? "notification unread" : "notification"} key={notificacao.id}>
                <div><div className="notification-meta"><span className="tag">{rotuloEnum(notificacao.tipo)}</span><time dateTime={notificacao.criadaEm}>{formatarData(notificacao.criadaEm)}</time></div><h2>{notificacao.titulo}</h2><p>{notificacao.mensagem}</p><Link className="inline-action" to={`/planos/${notificacao.planoId}/nao-conformidades/${notificacao.naoConformidadeId}`}>Ver detalhes da solicitação</Link></div>
                {notificacao.status === "NAO_LIDA" && <button className="button button-secondary" type="button" onClick={() => void marcarLida(notificacao)}>Marcar como lida</button>}
              </article>
            ))}
          </div>
          <Pagination pagina={dados} aoMudar={(pagina) => void carregar(pagina)} />
        </>
      )}
    </section>
  );
}
