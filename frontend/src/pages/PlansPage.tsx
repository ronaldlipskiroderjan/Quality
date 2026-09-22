import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { EmptyState, ErrorMessage, LoadingState } from "../components/Feedback";
import { PageHeader } from "../components/PageHeader";
import { Pagination } from "../components/Pagination";
import { PlanImage } from "../components/PlanImage";
import { formatarData, rotuloEnum } from "../lib/format";
import { mensagemErro } from "../lib/api";
import { planosApi } from "../services/qualityApi";
import type { Pagina, PlanoResumo } from "../types/api";

export function PlansPage() {
  const [dados, setDados] = useState<Pagina<PlanoResumo> | null>(null);
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(true);

  const carregar = useCallback(async (pagina = 0) => {
    try {
      setCarregando(true);
      setErro("");
      setDados(await planosApi.listar(pagina));
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  return (
    <section>
      <PageHeader
        titulo="Planos de qualidade"
        acao={<Link className="button button-primary" to="/planos/novo">Criar plano</Link>}
      />
      {erro && <ErrorMessage mensagem={erro} />}
      {carregando && <LoadingState mensagem="Carregando planos..." />}
      {!carregando && dados?.conteudo.length === 0 && (
        <EmptyState
          titulo="Crie seu primeiro plano de qualidade"
          mensagem="Defina o objetivo, reúna os documentos e prepare a primeira auditoria."
          acao={<Link className="button button-primary" to="/planos/novo">Criar primeiro plano</Link>}
        />
      )}
      {!carregando && dados && dados.conteudo.length > 0 && (
        <>
          <div className="resource-list plans-grid">
            {dados.conteudo.map((plano) => (
              <article className="resource-row plan-card" key={plano.id}>
                {plano.temImagem && <PlanImage planoId={plano.id} className="plan-card-image" alt="" />}
                <div className="resource-main">
                  <h2><Link to={`/planos/${plano.id}`}>{plano.nomeProjeto}</Link></h2>
                  <p>Versão {plano.versao} · Criado em {formatarData(plano.criadoEm)}</p>
                </div>
                <div className="plan-card-footer">
                  <div className="tag-list">
                    <span className={`status status-${plano.status.toLowerCase()}`}>{rotuloEnum(plano.status)}</span>
                    {plano.meusPapeis.map((papel) => <span className="tag" key={papel}>{rotuloEnum(papel)}</span>)}
                  </div>
                  <Link className="button button-secondary" to={`/planos/${plano.id}`}>Abrir plano</Link>
                </div>
              </article>
            ))}
          </div>
          <Pagination pagina={dados} aoMudar={(pagina) => void carregar(pagina)} />
        </>
      )}
    </section>
  );
}
