import { useCallback, useEffect, useState } from "react";
import { Navigate, NavLink, Outlet, useLocation, useParams } from "react-router-dom";
import { ErrorMessage, LoadingState } from "../components/Feedback";
import { Icon } from "../components/Icon";
import { planosApi } from "../services/qualityApi";
import type { PermissaoPlano, Plano } from "../types/api";

export interface PlanOutletContext {
  plano: Plano;
  pode: (permissao: PermissaoPlano) => boolean;
  recarregarPlano: () => Promise<void>;
}

const classeAba = ({ isActive }: { isActive: boolean }) => (isActive ? "tab-link active" : "tab-link");

export function PlanLayout() {
  const { planoId } = useParams();
  const { pathname } = useLocation();
  const [plano, setPlano] = useState<Plano | null>(null);
  const [erro, setErro] = useState("");

  const carregar = useCallback(async () => {
    if (!planoId) return;
    try {
      setErro("");
      setPlano(await planosApi.buscar(planoId));
    } catch (error) {
      setErro(error instanceof Error ? error.message : "Não foi possível carregar o plano.");
    }
  }, [planoId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  if (erro) return <ErrorMessage mensagem={erro} />;
  if (!plano) return <LoadingState mensagem="Carregando plano..." />;

  const pode = (permissao: PermissaoPlano) => plano.minhasPermissoes.includes(permissao);

  const equipeResolucao = plano.meusPapeis.includes("MEMBRO_EQUIPE_RESOLUCAO");
  const superior = plano.meusPapeis.some((papel) => papel === "SUPERIOR_N1" || papel === "SUPERIOR_N2");
  const caminhoBase = `/planos/${plano.id}`;
  const caminhoAtual = pathname.replace(/\/$/, "");
  const rotaNc = `${caminhoBase}/nao-conformidades/`;
  if (equipeResolucao && caminhoAtual !== caminhoBase && !caminhoAtual.startsWith(rotaNc)) {
    return <Navigate to={caminhoBase} replace />;
  }
  const rotasSuperior = new Set([caminhoBase, `${caminhoBase}/equipe`, `${caminhoBase}/escalonamentos`]);
  if (superior && !rotasSuperior.has(caminhoAtual) && !caminhoAtual.startsWith(rotaNc)) {
    return <Navigate to={caminhoBase} replace />;
  }

  return (
    <section className={`plan-workspace${equipeResolucao ? " plan-workspace-resolution" : ""}`}>
      <header className="plan-context">
        <NavLink to="/dashboard" className="back-link">
          Voltar à dashboard
        </NavLink>
      </header>
      <nav className={`tabs${superior ? " tabs-superior" : ""}${equipeResolucao ? " tabs-resolution" : ""}`} aria-label="Seções do plano">
        <NavLink end className={classeAba} to={`/planos/${plano.id}`}>
          <Icon name={equipeResolucao ? "resolution" : "overview"} /><span><strong>{equipeResolucao ? "Solicitações" : "Visão geral"}</strong><small>{equipeResolucao ? plano.nomeProjeto : "Estrutura do plano"}</small></span>
        </NavLink>
        {!equipeResolucao && <>
        <NavLink className={classeAba} to={`/planos/${plano.id}/equipe`}>
          <Icon name="users" /><span><strong>Auditores</strong><small>Acesso e histórico</small></span>
        </NavLink>
        {superior ? (
          <NavLink className={classeAba} to={`/planos/${plano.id}/escalonamentos`}>
            <Icon name="warning" /><span><strong>Escalonamentos</strong><small>Prazos sob sua decisão</small></span>
          </NavLink>
        ) : <>
          <NavLink className={classeAba} to={`/planos/${plano.id}/documentos`}>
            <Icon name="document" /><span><strong>Documentos</strong><small>Fontes e auditados</small></span>
          </NavLink>
          <NavLink className={classeAba} to={`/planos/${plano.id}/artefatos`}>
            <Icon name="clipboard" /><span><strong>Auditorias</strong><small>Artefatos e checklists</small></span>
          </NavLink>
          <NavLink className={classeAba} to={`/planos/${plano.id}/nao-conformidades`}>
            <Icon name="resolution" /><span><strong>Resolução</strong><small>Equipe e NCs</small></span>
          </NavLink>
        </>}
        </>}
      </nav>
      <Outlet context={{ plano, pode, recarregarPlano: carregar } satisfies PlanOutletContext} />
    </section>
  );
}
