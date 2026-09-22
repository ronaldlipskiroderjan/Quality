import { useCallback, useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { Icon } from "../components/Icon";
import { formatarData } from "../lib/format";
import { notificacoesApi } from "../services/qualityApi";
import type { Notificacao } from "../types/api";

export function AppLayout() {
  const { usuario, imagemPerfilUrl, sair } = useAuth();
  const localizacao = useLocation();
  const areaNotificacoes = useRef<HTMLDivElement>(null);
  const [notificacoes, setNotificacoes] = useState<Notificacao[]>([]);
  const [notificacoesAbertas, setNotificacoesAbertas] = useState(false);
  const [carregandoNotificacoes, setCarregandoNotificacoes] = useState(true);
  const [erroNotificacoes, setErroNotificacoes] = useState(false);
  const iniciais = usuario?.nome
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0]?.toUpperCase())
    .join("") || "Q";
  const quantidadeNaoLidas = notificacoes.filter(
    (notificacao) => notificacao.status === "NAO_LIDA",
  ).length;

  const carregarNotificacoes = useCallback(async () => {
    try {
      setErroNotificacoes(false);
      const pagina = await notificacoesApi.listar(0);
      setNotificacoes(pagina.conteudo);
    } catch {
      setErroNotificacoes(true);
    } finally {
      setCarregandoNotificacoes(false);
    }
  }, []);

  useEffect(() => {
    const intervalo = window.setInterval(() => void carregarNotificacoes(), 60_000);
    const atualizar = () => void carregarNotificacoes();
    window.addEventListener("quality:notifications-updated", atualizar);
    return () => {
      window.clearInterval(intervalo);
      window.removeEventListener("quality:notifications-updated", atualizar);
    };
  }, [carregarNotificacoes]);

  useEffect(() => {
    setNotificacoesAbertas(false);
    void carregarNotificacoes();
  }, [carregarNotificacoes, localizacao.pathname]);

  useEffect(() => {
    if (!notificacoesAbertas) return;
    const fecharAoClicarFora = (evento: PointerEvent) => {
      if (!areaNotificacoes.current?.contains(evento.target as Node)) {
        setNotificacoesAbertas(false);
      }
    };
    const fecharComEscape = (evento: KeyboardEvent) => {
      if (evento.key === "Escape") setNotificacoesAbertas(false);
    };
    document.addEventListener("pointerdown", fecharAoClicarFora);
    document.addEventListener("keydown", fecharComEscape);
    return () => {
      document.removeEventListener("pointerdown", fecharAoClicarFora);
      document.removeEventListener("keydown", fecharComEscape);
    };
  }, [notificacoesAbertas]);

  async function marcarComoLida(notificacao: Notificacao) {
    if (notificacao.status === "LIDA") return;
    setNotificacoes((atuais) => atuais.map((item) =>
      item.id === notificacao.id ? { ...item, status: "LIDA" } : item
    ));
    try {
      await notificacoesApi.marcarLida(notificacao.id);
    } catch {
      void carregarNotificacoes();
    }
  }

  return (
    <div className="app-shell">
      <a className="skip-link" href="#conteudo-principal">Ir para o conteúdo</a>
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink className="brand" to="/dashboard" aria-label="Quality, dashboard">
            <span className="brand-mark" aria-hidden="true"><i /><i /><i /></span>
            <span>Quality</span>
          </NavLink>
          <div className="user-summary">
            <div className="topbar-notifications" ref={areaNotificacoes}>
              <button
                className="topbar-notification-button"
                type="button"
                aria-label={quantidadeNaoLidas > 0
                  ? `Notificações, ${quantidadeNaoLidas} não lidas`
                  : "Notificações"}
                aria-expanded={notificacoesAbertas}
                aria-controls="painel-notificacoes"
                onClick={() => setNotificacoesAbertas((abertas) => !abertas)}
              >
                <Icon name="bell" size={20} />
                {quantidadeNaoLidas > 0 && (
                  <span className="topbar-notification-badge">
                    {quantidadeNaoLidas > 99 ? "99+" : quantidadeNaoLidas}
                  </span>
                )}
              </button>

              {notificacoesAbertas && (
                <section className="topbar-notification-popover" id="painel-notificacoes" aria-label="Notificações recentes">
                  <header>
                    <h2>Notificações</h2>
                    <span>{quantidadeNaoLidas} não {quantidadeNaoLidas === 1 ? "lida" : "lidas"}</span>
                  </header>
                  {carregandoNotificacoes ? (
                    <p className="topbar-notification-state">Carregando notificações...</p>
                  ) : erroNotificacoes ? (
                    <p className="topbar-notification-state">Não foi possível atualizar as notificações.</p>
                  ) : notificacoes.length === 0 ? (
                    <p className="topbar-notification-state">Nenhuma notificação recebida.</p>
                  ) : (
                    <ul>
                      {notificacoes.slice(0, 5).map((notificacao) => (
                        <li className={notificacao.status === "NAO_LIDA" ? "is-unread" : ""} key={notificacao.id}>
                          <Link
                            to={`/planos/${notificacao.planoId}/nao-conformidades/${notificacao.naoConformidadeId}`}
                            onClick={() => void marcarComoLida(notificacao)}
                          >
                            <span className="topbar-notification-dot" aria-hidden="true" />
                            <span>
                              <small>{notificacao.planoNome} · {notificacao.artefatoNome}</small>
                              <strong>{notificacao.titulo}</strong>
                              <p>{notificacao.mensagem}</p>
                              <time dateTime={notificacao.criadaEm}>{formatarData(notificacao.criadaEm)}</time>
                            </span>
                          </Link>
                        </li>
                      ))}
                    </ul>
                  )}
                  <footer><Link to="/notificacoes">Ver todas as notificações</Link></footer>
                </section>
              )}
            </div>
            <NavLink
              className={({ isActive }) => `user-profile-link${isActive ? " active" : ""}`}
              to="/perfil"
              aria-label={`Editar perfil de ${usuario?.nome ?? "usuário"}`}
            >
              <span className="user-avatar" aria-hidden="true">
                {imagemPerfilUrl
                  ? <img src={imagemPerfilUrl} alt="" />
                  : iniciais}
              </span>
              <span className="user-identity">
                <strong>{usuario?.nome}</strong>
                <small>{usuario?.email}</small>
              </span>
            </NavLink>
            <button type="button" className="text-button logout-button" onClick={sair}>Sair</button>
          </div>
        </div>
      </header>
      <main className="app-content" id="conteudo-principal">
        <Outlet />
      </main>
    </div>
  );
}
