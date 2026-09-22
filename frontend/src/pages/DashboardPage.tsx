import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { EmptyState, ErrorMessage, LoadingState } from "../components/Feedback";
import { Icon } from "../components/Icon";
import { Modal } from "../components/Modal";
import { PlanForm } from "../components/PlanForm";
import { PlanImage } from "../components/PlanImage";
import {
  formatarData,
  rotuloEnum,
  rotuloStatusNaoConformidade,
} from "../lib/format";
import { minhasAuditoriasApi, minhasNaoConformidadesApi, meusEscalonamentosApi, planosApi } from "../services/qualityApi";
import type { AuditoriaAgenda, EscalonamentoPainel, NaoConformidade, Pagina, PlanoResumo } from "../types/api";

interface DashboardData {
  planos: Pagina<PlanoResumo> | null;
  resolucoes: Pagina<NaoConformidade> | null;
  escalonamentos: Pagina<EscalonamentoPainel> | null;
  auditorias: Pagina<AuditoriaAgenda> | null;
}

interface DashboardTask {
  id: string;
  tipo: "resolucao" | "escalonamento";
  titulo: string;
  descricao: string;
  data: string;
  status: string;
  destino: string;
}

interface CalendarEvent {
  id: string;
  tipo: "auditoria" | "prazo";
  titulo: string;
  descricao: string;
  data: string;
  destino: string;
  concluido?: boolean;
}

const dadosIniciais: DashboardData = {
  planos: null,
  resolucoes: null,
  escalonamentos: null,
  auditorias: null,
};
const diasSemana = ["Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"];
const formatadorMes = new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric" });
const formatadorDia = new Intl.DateTimeFormat("pt-BR", { weekday: "long", day: "numeric", month: "long" });

function chaveDia(valor: string | Date) {
  if (typeof valor === "string" && /^\d{4}-\d{2}-\d{2}$/.test(valor)) {
    return valor;
  }
  const data = typeof valor === "string" ? new Date(valor) : valor;
  if (Number.isNaN(data.getTime())) return "";
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, "0");
  const dia = String(data.getDate()).padStart(2, "0");
  return `${ano}-${mes}-${dia}`;
}

function instanteEvento(valor: string, fimDoDia = false) {
  const somenteData = /^\d{4}-\d{2}-\d{2}$/.test(valor);
  const data = somenteData
    ? new Date(`${valor}T${fimDoDia ? "23:59:59" : "12:00:00"}`)
    : new Date(valor);
  return data.getTime();
}

function diasVisiveis(mes: Date) {
  const primeiroDia = new Date(mes.getFullYear(), mes.getMonth(), 1);
  const deslocamento = (primeiroDia.getDay() + 6) % 7;
  const inicio = new Date(mes.getFullYear(), mes.getMonth(), 1 - deslocamento);
  return Array.from({ length: 42 }, (_, indice) => {
    const data = new Date(inicio);
    data.setDate(inicio.getDate() + indice);
    return data;
  });
}

export function DashboardPage() {
  const { usuario } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const hoje = useMemo(() => new Date(), []);
  const [dados, setDados] = useState<DashboardData>(dadosIniciais);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [mesCalendario, setMesCalendario] = useState(() => new Date(hoje.getFullYear(), hoje.getMonth(), 1));
  const [diaSelecionado, setDiaSelecionado] = useState(() => chaveDia(hoje));
  const [modalNovoPlano, setModalNovoPlano] = useState(false);

  useEffect(() => {
    const estado = location.state as { abrirNovoPlano?: boolean } | null;
    if (!estado?.abrirNovoPlano) return;
    setModalNovoPlano(true);
    navigate("/dashboard", { replace: true, state: null });
  }, [location.state, navigate]);

  useEffect(() => {
    let ignorar = false;

    async function buscarDados() {
      setCarregando(true);
      setErro("");
      const resultados = await Promise.allSettled([
        planosApi.listarTodos(),
        minhasNaoConformidadesApi.listarAtribuidas(),
        meusEscalonamentosApi.listar(),
        minhasAuditoriasApi.listarTodas(),
      ]);
      if (ignorar) return;

      const [planos, resolucoes, escalonamentos, auditorias] = resultados;
      setDados({
        planos: planos.status === "fulfilled" ? planos.value : null,
        resolucoes: resolucoes.status === "fulfilled" ? resolucoes.value : null,
        escalonamentos: escalonamentos.status === "fulfilled" ? escalonamentos.value : null,
        auditorias: auditorias.status === "fulfilled" ? auditorias.value : null,
      });

      const falhas = resultados.filter((resultado) => resultado.status === "rejected").length;
      if (falhas === resultados.length) setErro("Não foi possível carregar a dashboard. Tente atualizar a página.");
      else if (falhas > 0) setErro("Parte das informações não pôde ser atualizada. Os dados disponíveis continuam visíveis.");
      setCarregando(false);
    }

    void buscarDados();
    return () => { ignorar = true; };
  }, []);

  const resolucoesPendentes = useMemo(
    () => dados.resolucoes?.conteudo.filter((item) => !["CONCLUIDA", "CANCELADA"].includes(item.status)) ?? [],
    [dados.resolucoes],
  );
  const auditoriasPlanejadas = useMemo(
    () => dados.auditorias?.conteudo ?? [],
    [dados.auditorias],
  );

  const tarefas = useMemo<DashboardTask[]>(() => {
    const resolucoes = resolucoesPendentes.map((item) => ({
      id: `resolucao-${item.id}`,
      tipo: "resolucao" as const,
      titulo: `${item.artefatoNome} · item ${item.itemOrdem}`,
      descricao: item.pergunta,
      data: item.atualizadoEm,
      status: rotuloStatusNaoConformidade(item.status),
      destino: `/planos/${item.planoId}/nao-conformidades/${item.id}`,
    }));
    const escalonamentos = (dados.escalonamentos?.conteudo ?? []).map((item) => ({
      id: `escalonamento-${item.id}`,
      tipo: "escalonamento" as const,
      titulo: `${item.planoNome} · ${item.artefatoNome}`,
      descricao: `Item ${item.itemOrdem} · ${item.pergunta}`,
      data: item.escalonadoEm,
      status: `Escalonamento ${item.nivel}`,
      destino: `/planos/${item.planoId}/escalonamentos`,
    }));
    return [...resolucoes, ...escalonamentos]
      .sort((a, b) => new Date(b.data).getTime() - new Date(a.data).getTime());
  }, [dados.escalonamentos, resolucoesPendentes]);

  const eventosCalendario = useMemo<CalendarEvent[]>(() => {
    const prazos = resolucoesPendentes
      .filter((item) => item.prazoEm)
      .map((item) => ({
        id: `prazo-${item.id}`,
        tipo: "prazo" as const,
        titulo: `${item.artefatoNome} · item ${item.itemOrdem}`,
        descricao: rotuloStatusNaoConformidade(item.status),
        data: item.prazoEm as string,
        destino: `/planos/${item.planoId}/nao-conformidades/${item.id}`,
      }));
    const auditorias = auditoriasPlanejadas
      .filter((item) => item.status !== "CANCELADO")
      .map((item) => ({
        id: `auditoria-${item.auditoriaId}`,
        tipo: "auditoria" as const,
        titulo: item.artefatoNome,
        descricao: `Auditoria · ${item.planoNome} · ${rotuloEnum(item.status)}`,
        data: item.dataPlanejada,
        destino: `/planos/${item.planoId}/artefatos/${item.artefatoId}/auditorias/${item.auditoriaId}`,
        concluido: item.status === "CONCLUIDO",
      }));
    return [...auditorias, ...prazos]
      .sort((a, b) => instanteEvento(a.data) - instanteEvento(b.data));
  }, [auditoriasPlanejadas, resolucoesPendentes]);

  const eventosPorDia = useMemo(() => {
    const grupos = new Map<string, CalendarEvent[]>();
    eventosCalendario.forEach((evento) => {
      const chave = chaveDia(evento.data);
      if (!chave) return;
      grupos.set(chave, [...(grupos.get(chave) ?? []), evento]);
    });
    return grupos;
  }, [eventosCalendario]);

  const dias = useMemo(() => diasVisiveis(mesCalendario), [mesCalendario]);
  const eventosDoDia = eventosPorDia.get(diaSelecionado) ?? [];
  const agora = hoje.getTime();
  const limiteProximo = agora + 7 * 24 * 60 * 60 * 1000;
  const hojeChave = chaveDia(hoje);
  const limiteCalendario = new Date(hoje);
  limiteCalendario.setDate(limiteCalendario.getDate() + 7);
  const limiteCalendarioChave = chaveDia(limiteCalendario);
  const prazosEmAberto = resolucoesPendentes.filter((item) => item.prazoEm);
  const prazosProximos = prazosEmAberto.filter((item) => {
    const prazo = new Date(item.prazoEm as string).getTime();
    return prazo >= agora && prazo <= limiteProximo;
  }).length;
  const prazosVencidos = prazosEmAberto.filter((item) => new Date(item.prazoEm as string).getTime() < agora).length;
  const auditoriasEmAberto = auditoriasPlanejadas.filter(
    (item) => !["CONCLUIDO", "CANCELADO"].includes(item.status),
  );
  const auditoriasProximas = auditoriasEmAberto.filter((item) => {
    const data = chaveDia(item.dataPlanejada);
    return data >= hojeChave && data <= limiteCalendarioChave;
  }).length;
  const auditoriasAtrasadas = auditoriasEmAberto.filter(
    (item) => chaveDia(item.dataPlanejada) < hojeChave,
  ).length;
  const compromissosProximos = prazosProximos + auditoriasProximas;
  const compromissosAtrasados = prazosVencidos + auditoriasAtrasadas;
  const proximoEvento = eventosCalendario.find(
    (evento) => !evento.concluido && instanteEvento(evento.data, true) >= agora,
  );

  function mudarMes(deslocamento: number) {
    const novoMes = new Date(mesCalendario.getFullYear(), mesCalendario.getMonth() + deslocamento, 1);
    setMesCalendario(novoMes);
    setDiaSelecionado(chaveDia(novoMes));
  }

  function selecionarDia(data: Date) {
    setDiaSelecionado(chaveDia(data));
    if (data.getMonth() !== mesCalendario.getMonth() || data.getFullYear() !== mesCalendario.getFullYear()) {
      setMesCalendario(new Date(data.getFullYear(), data.getMonth(), 1));
    }
  }

  return (
    <section className="quality-dashboard">
      <div className="dashboard-overview-layout">
        <aside className="dashboard-plan-rail" aria-labelledby="titulo-planos">
          <header className="dashboard-plan-rail-header">
            <div>
              <h2 id="titulo-planos">Planos</h2>
              <span>{dados.planos?.totalElementos ?? 0}</span>
            </div>
            <button
              className="button button-primary dashboard-new-plan"
              type="button"
              onClick={() => setModalNovoPlano(true)}
            >
              <Icon name="plus" size={17} />
              Novo plano
            </button>
          </header>

          <div className="dashboard-plan-rail-body">
            {carregando ? (
              <LoadingState mensagem="Carregando planos..." />
            ) : !dados.planos || dados.planos.conteudo.length === 0 ? (
              <div className="dashboard-plan-rail-empty">
                <Icon name="layers" size={26} />
                <strong>Nenhum plano</strong>
                <p>Crie o primeiro plano para começar a organizar as auditorias.</p>
              </div>
            ) : (
              <nav className="dashboard-plan-list" aria-label="Planos disponíveis">
                {dados.planos.conteudo.map((plano) => (
                  <Link className="dashboard-plan-item" to={`/planos/${plano.id}`} key={plano.id}>
                    <span className={`dashboard-plan-item-visual${plano.temImagem ? " has-image" : ""}`} aria-hidden="true">
                      {plano.temImagem
                        ? <PlanImage planoId={plano.id} className="dashboard-plan-item-image" alt="" fallback={<Icon name="layers" size={19} />} />
                        : <Icon name="layers" size={19} />}
                    </span>
                    <span className="dashboard-plan-item-content">
                      <strong>{plano.nomeProjeto}</strong>
                      <small>Versão {plano.versao}</small>
                    </span>
                    <span className={`dashboard-plan-status is-${plano.status.toLowerCase()}`} title={rotuloEnum(plano.status)} role="img" aria-label={rotuloEnum(plano.status)} />
                  </Link>
                ))}
              </nav>
            )}
          </div>
        </aside>

        <div className="dashboard-command-grid">
          <header className="dashboard-command-hero">
          <div className="dashboard-command-copy">
            <p>{formatadorDia.format(hoje)}</p>
            <h1>Olá, {usuario?.nome?.split(" ")[0] ?? "usuário"}.</h1>
            <span>Acompanhe os planos, prazos e ações de qualidade em um único lugar.</span>
          </div>

          <div className="dashboard-next-event" aria-label="Próximo compromisso">
            <span className="dashboard-next-icon">
              <Icon name={proximoEvento?.tipo === "auditoria" ? "clipboard" : "clock"} size={20} />
            </span>
            <div>
              <small>Próximo compromisso</small>
              {proximoEvento ? (
                <>
                  <strong>{proximoEvento.titulo}</strong>
                  <time dateTime={proximoEvento.data}>{formatarData(proximoEvento.data)}</time>
                </>
              ) : (
                <strong>Nenhum compromisso próximo</strong>
              )}
            </div>
          </div>
          </header>

          <aside className="dashboard-calendar" aria-labelledby="titulo-calendario">
          <header className="dashboard-calendar-header">
            <div>
              <h2 id="titulo-calendario">Calendário</h2>
              <p>Auditorias e prazos do seu trabalho</p>
            </div>
            <div className="dashboard-calendar-navigation" aria-label="Navegação do calendário">
              <button type="button" onClick={() => mudarMes(-1)} aria-label="Mês anterior"><Icon name="chevronLeft" size={17} /></button>
              <button type="button" onClick={() => mudarMes(1)} aria-label="Próximo mês"><Icon name="chevronRight" size={17} /></button>
            </div>
          </header>

          <div className="dashboard-calendar-month">
            <strong>{formatadorMes.format(mesCalendario)}</strong>
            <button type="button" onClick={() => { setMesCalendario(new Date(hoje.getFullYear(), hoje.getMonth(), 1)); setDiaSelecionado(chaveDia(hoje)); }}>Hoje</button>
          </div>

          <div className="dashboard-calendar-legend" aria-label="Tipos de compromisso">
            <span><i className="is-auditoria" aria-hidden="true" />Auditoria</span>
            <span><i className="is-prazo" aria-hidden="true" />Prazo de NC</span>
          </div>

          <div className="dashboard-calendar-weekdays" aria-hidden="true">
            {diasSemana.map((dia) => <span key={dia}>{dia}</span>)}
          </div>
          <div className="dashboard-calendar-grid">
            {dias.map((data) => {
              const chave = chaveDia(data);
              const eventos = eventosPorDia.get(chave) ?? [];
              const mesAtual = data.getMonth() === mesCalendario.getMonth();
              const selecionado = chave === diaSelecionado;
              const atual = chave === chaveDia(hoje);
              return (
                <button
                  className={[
                    "dashboard-calendar-day",
                    mesAtual ? "" : "is-outside",
                    selecionado ? "is-selected" : "",
                    atual ? "is-today" : "",
                  ].filter(Boolean).join(" ")}
                  type="button"
                  aria-label={`${formatadorDia.format(data)}${eventos.length ? `, ${eventos.length} eventos` : ""}`}
                  aria-pressed={selecionado}
                  onClick={() => selecionarDia(data)}
                  key={chave}
                >
                  <span>{data.getDate()}</span>
                  {eventos.length > 0 && (
                    <span className="dashboard-calendar-events" aria-hidden="true">
                      {Array.from(new Set(eventos.map((evento) => evento.tipo))).map((tipo) => (
                        <i className={`is-${tipo}`} key={tipo} />
                      ))}
                    </span>
                  )}
                </button>
              );
            })}
          </div>

          <section className="dashboard-day-agenda" aria-live="polite">
            <header>
              <h3>{formatadorDia.format(new Date(`${diaSelecionado}T12:00:00`))}</h3>
              <span>{eventosDoDia.length}</span>
            </header>
            {eventosDoDia.length === 0 ? (
              <p>Nenhuma auditoria ou prazo para este dia.</p>
            ) : (
              <ul>
                {eventosDoDia.map((evento) => (
                  <li key={evento.id}>
                    <Link to={evento.destino}>
                      <span className={`dashboard-agenda-marker is-${evento.tipo}`} aria-hidden="true" />
                      <span><strong>{evento.titulo}</strong><small>{evento.descricao} · {formatarData(evento.data)}</small></span>
                      <Icon name="arrowRight" size={15} />
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </section>
          </aside>

          <dl className="dashboard-metrics">
          <div><dt>Auditorias previstas</dt><dd>{auditoriasPlanejadas.length}</dd><span>Entregas acompanhadas no calendário</span></div>
          <div><dt>Ações aguardando</dt><dd>{tarefas.length}</dd><span>NCs e escalonamentos atribuídos</span></div>
          <div><dt>Próximos 7 dias</dt><dd>{compromissosProximos}</dd><span>Auditorias e prazos no período</span></div>
          <div className={compromissosAtrasados > 0 ? "has-risk" : ""}><dt>Fora do prazo</dt><dd>{compromissosAtrasados}</dd><span>Auditorias e NCs que exigem atenção</span></div>
          </dl>

          <div className="dashboard-priority-stack">
            {erro && <div className="dashboard-error"><ErrorMessage mensagem={erro} /></div>}

            <section className="dashboard-priority" aria-labelledby="titulo-trabalho">
              <header className="dashboard-panel-heading dashboard-priority-heading">
                <h2 id="titulo-trabalho">Prioridades</h2>
                <span aria-label={`${tarefas.length} ${tarefas.length === 1 ? "prioridade" : "prioridades"}`}>
                  {tarefas.length}
                </span>
              </header>
              {carregando ? <LoadingState mensagem="Organizando seu espaço de trabalho..." /> : tarefas.length === 0 ? (
                <EmptyState titulo="Nenhuma prioridade atribuída" mensagem="NCs e escalonamentos enviados para você aparecerão aqui." />
              ) : (
                <div className="dashboard-priority-list" tabIndex={0} aria-label="Lista de prioridades; role para consultar as demais">
                  {tarefas.map((tarefa) => (
                    <Link className="dashboard-priority-row" to={tarefa.destino} key={tarefa.id}>
                      <span className={`dashboard-priority-icon is-${tarefa.tipo}`}><Icon name={tarefa.tipo === "escalonamento" ? "warning" : "resolution"} size={18} /></span>
                      <span className="dashboard-priority-content">
                        <span><strong>{tarefa.titulo}</strong><small>{tarefa.status}</small></span>
                        <p>{tarefa.descricao}</p>
                      </span>
                      <time dateTime={tarefa.data}>{formatarData(tarefa.data)}</time>
                      <Icon name="arrowRight" size={17} />
                    </Link>
                  ))}
                </div>
              )}
            </section>
          </div>
        </div>
      </div>

      <Modal
        aberto={modalNovoPlano}
        titulo="Criar plano de qualidade"
        descricao="Defina a identidade e o propósito do plano. Você poderá adicionar auditores, documentos e classificações em seguida."
        aoFechar={() => setModalNovoPlano(false)}
        amplo
      >
        <div className="plan-create-dialog">
          <PlanForm
            textoBotao="Criar plano"
            focoInicial
            aoSalvar={async (entrada, imagem) => {
              const plano = await planosApi.criar(entrada, imagem);
              setModalNovoPlano(false);
              navigate(`/planos/${plano.id}`);
            }}
            aoCancelar={() => setModalNovoPlano(false)}
          />
        </div>
      </Modal>
    </section>
  );
}
