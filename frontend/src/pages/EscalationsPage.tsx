import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useOutletContext } from "react-router-dom";
import { EmptyState, ErrorMessage, LoadingState, SuccessMessage } from "../components/Feedback";
import { Icon } from "../components/Icon";
import { Modal } from "../components/Modal";
import { PageHeader } from "../components/PageHeader";
import type { PlanOutletContext } from "../layouts/PlanLayout";
import { mensagemErro } from "../lib/api";
import { formatarData, rotuloEnum, rotuloStatusNaoConformidade } from "../lib/format";
import { escalonamentosPlanoApi } from "../services/qualityApi";
import type { EscalonamentoPainel } from "../types/api";

function paraDataLocal(valor: string) {
  const data = new Date(valor);
  const local = new Date(data.getTime() - data.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}

function escalonamentoAtivo(item: EscalonamentoPainel) {
  return item.nivel === "N1"
    ? item.statusNaoConformidade === "ESCALONADA_N1"
    : item.statusNaoConformidade === "ESCALONADA_N2";
}

export function EscalationsPage() {
  const { plano } = useOutletContext<PlanOutletContext>();
  const [itens, setItens] = useState<EscalonamentoPainel[]>([]);
  const [selecionado, setSelecionado] = useState<EscalonamentoPainel | null>(null);
  const [novaData, setNovaData] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const [erroModal, setErroModal] = useState("");
  const [sucesso, setSucesso] = useState("");

  const carregar = useCallback(async () => {
    try {
      setCarregando(true);
      setErro("");
      setItens((await escalonamentosPlanoApi.listar(plano.id)).conteudo);
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setCarregando(false);
    }
  }, [plano.id]);

  useEffect(() => { void carregar(); }, [carregar]);

  function abrir(item: EscalonamentoPainel) {
    setSelecionado(item);
    setNovaData(paraDataLocal(item.prazoEm));
    setErroModal("");
  }

  async function revisar(prazoEm: string) {
    if (!selecionado) return;
    try {
      setSalvando(true);
      setErroModal("");
      await escalonamentosPlanoApi.revisarPrazo(plano.id, selecionado.id, prazoEm);
      setSelecionado(null);
      setSucesso(prazoEm === selecionado.prazoEm ? "Prazo atual mantido." : "Novo prazo de resolução definido.");
      await carregar();
    } catch (error) {
      setErroModal(mensagemErro(error));
    } finally {
      setSalvando(false);
    }
  }

  function salvarNovaData(event: FormEvent) {
    event.preventDefault();
    if (!novaData) return;
    void revisar(new Date(novaData).toISOString());
  }

  return (
    <div className="section-content escalations-page">
      <PageHeader titulo="Escalonamentos" />
      {erro && <ErrorMessage mensagem={erro} />}
      {sucesso && <SuccessMessage mensagem={sucesso} aoFechar={() => setSucesso("")} />}
      {carregando ? <LoadingState mensagem="Carregando escalonamentos..." /> : itens.length === 0 ? (
        <EmptyState titulo="Nenhum escalonamento atribuído" mensagem="As não conformidades que ultrapassarem o prazo aparecerão aqui." />
      ) : (
        <section className="escalation-board" aria-label="Escalonamentos atribuídos">
          <div className="escalation-board-header" aria-hidden="true"><span>Origem</span><span>Responsável</span><span>Prazo</span><span>Status</span><span>Ação</span></div>
          <ul>{itens.map((item) => {
            const ativo = escalonamentoAtivo(item);
            return <li key={item.id} className={ativo ? "is-active" : ""}>
              <div className="escalation-origin"><span><Icon name="clipboard" size={18} /></span><div><strong>{item.artefatoNome}</strong><small>Item {item.itemOrdem} · {item.pergunta}</small><em>Escalonamento {item.nivel} em {formatarData(item.escalonadoEm)}</em></div></div>
              <span>{item.responsavelResolucaoNome ?? "Equipe de resolução"}</span>
              <div className="escalation-deadline"><strong>{formatarData(item.prazoEm)}</strong>{item.revisadoEm && <small>Revisado em {formatarData(item.revisadoEm)}</small>}</div>
              <span className={`status status-${item.statusNaoConformidade.toLowerCase()}`}>{rotuloStatusNaoConformidade(item.statusNaoConformidade)}</span>
              {ativo ? <button className="button button-secondary" type="button" onClick={() => abrir(item)}>Definir prazo</button> : <span className="escalation-closed">Encerrado</span>}
            </li>;
          })}</ul>
        </section>
      )}

      <Modal aberto={Boolean(selecionado)} titulo={`Prazo do escalonamento ${selecionado?.nivel ?? ""}`} descricao={selecionado ? `${selecionado.artefatoNome} · item ${selecionado.itemOrdem}` : undefined} aoFechar={() => setSelecionado(null)}>
        {selecionado && <form className="form-stack" onSubmit={salvarNovaData}>
          <dl className="escalation-modal-details"><div><dt>Prazo original</dt><dd>{formatarData(selecionado.prazoOriginalEm)}</dd></div><div><dt>Prazo atual</dt><dd>{formatarData(selecionado.prazoEm)}</dd></div><div><dt>Status</dt><dd>{rotuloEnum(selecionado.statusNaoConformidade)}</dd></div></dl>
          <label>Nova data de resolução<input type="datetime-local" required min={paraDataLocal(new Date().toISOString())} value={novaData} onChange={(event) => setNovaData(event.target.value)} /></label>
          {erroModal && <ErrorMessage mensagem={erroModal} />}
          <div className="form-actions"><button className="button button-primary" disabled={salvando}>Salvar nova data</button><button className="button button-secondary" type="button" disabled={salvando} onClick={() => void revisar(selecionado.prazoEm)}>Manter data atual</button><button className="text-button" type="button" onClick={() => setSelecionado(null)}>Cancelar</button></div>
        </form>}
      </Modal>
    </div>
  );
}
