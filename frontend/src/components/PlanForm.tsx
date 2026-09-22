import { useEffect, useRef, useState, type FormEvent } from "react";
import { ErrorMessage } from "./Feedback";
import { Icon } from "./Icon";
import { PlanImage } from "./PlanImage";
import { mensagemErro } from "../lib/api";
import type { PlanoEntrada, UUID } from "../types/api";

interface PlanFormProps {
  valorInicial?: PlanoEntrada;
  planoId?: UUID;
  temImagem?: boolean;
  revisaoImagem?: number;
  textoBotao: string;
  aoSalvar: (entrada: PlanoEntrada, imagem?: File, removerImagem?: boolean) => Promise<void>;
  aoCancelar?: () => void;
  focoInicial?: boolean;
}

const vazio: PlanoEntrada = { nomeProjeto: "", versao: "", objetivo: "", visaoGeral: "" };

export function PlanForm({ valorInicial = vazio, planoId, temImagem = false, revisaoImagem = 0, textoBotao, aoSalvar, aoCancelar, focoInicial = false }: PlanFormProps) {
  const [entrada, setEntrada] = useState<PlanoEntrada>(valorInicial);
  const [imagem, setImagem] = useState<File>();
  const [preview, setPreview] = useState("");
  const [removerImagem, setRemoverImagem] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const inputImagem = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!imagem) {
      setPreview("");
      return;
    }
    const url = URL.createObjectURL(imagem);
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [imagem]);

  async function enviar(event: FormEvent) {
    event.preventDefault();
    try {
      setSalvando(true);
      setErro("");
      await aoSalvar(entrada, imagem, removerImagem);
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setSalvando(false);
    }
  }

  function selecionarImagem(arquivo?: File) {
    setErro("");
    if (!arquivo) {
      setImagem(undefined);
      return;
    }
    if (!["image/jpeg", "image/png"].includes(arquivo.type)) {
      setErro("Selecione uma imagem PNG ou JPG.");
      if (inputImagem.current) inputImagem.current.value = "";
      return;
    }
    if (arquivo.size > 5 * 1024 * 1024) {
      setErro("A imagem deve ter no máximo 5 MB.");
      if (inputImagem.current) inputImagem.current.value = "";
      return;
    }
    setImagem(arquivo);
    setRemoverImagem(false);
  }

  function removerSelecao() {
    setImagem(undefined);
    if (inputImagem.current) inputImagem.current.value = "";
    setRemoverImagem(temImagem);
  }

  return (
    <form className="form-stack content-width" onSubmit={enviar}>
      {erro && <ErrorMessage mensagem={erro} />}
      <section className="plan-image-field" aria-labelledby="imagem-plano-label">
        <div className="plan-image-preview">
          {preview ? <img src={preview} alt="Pré-visualização da imagem selecionada" /> : planoId && temImagem && !removerImagem ? (
            <PlanImage planoId={planoId} revisao={revisaoImagem} alt="Imagem atual do plano" fallback={<span className="plan-image-placeholder"><Icon name="layers" size={26} /></span>} />
          ) : <span className="plan-image-placeholder"><Icon name="layers" size={26} /></span>}
        </div>
        <div className="plan-image-control">
          <label id="imagem-plano-label" htmlFor="imagem-plano">Imagem do plano</label>
          <input ref={inputImagem} id="imagem-plano" type="file" accept="image/png,image/jpeg" onChange={(event) => selecionarImagem(event.target.files?.[0])} />
          <small>PNG ou JPG, até 5 MB.</small>
          <div className="plan-image-actions">
            {(imagem || (temImagem && !removerImagem)) && <button className="text-button danger-text" type="button" onClick={removerSelecao}>{imagem ? "Remover seleção" : "Remover imagem"}</button>}
            {removerImagem && <button className="text-button" type="button" onClick={() => setRemoverImagem(false)}>Manter imagem atual</button>}
          </div>
        </div>
      </section>
      <div className="form-row">
        <label>
          Nome do projeto
          <input
            autoFocus={focoInicial}
            required
            maxLength={150}
            value={entrada.nomeProjeto}
            onChange={(event) => setEntrada({ ...entrada, nomeProjeto: event.target.value })}
          />
        </label>
        <label>
          Versão
          <input
            required
            maxLength={30}
            value={entrada.versao}
            onChange={(event) => setEntrada({ ...entrada, versao: event.target.value })}
          />
        </label>
      </div>
      <label>
        Objetivo
        <textarea
          required
          maxLength={2000}
          rows={4}
          value={entrada.objetivo}
          onChange={(event) => setEntrada({ ...entrada, objetivo: event.target.value })}
        />
      </label>
      <label>
        Visão geral
        <textarea
          required
          maxLength={5000}
          rows={7}
          value={entrada.visaoGeral}
          onChange={(event) => setEntrada({ ...entrada, visaoGeral: event.target.value })}
        />
      </label>
      <div className="form-actions">
        <button className="button button-primary" type="submit" disabled={salvando}>
          {salvando ? "Salvando..." : textoBotao}
        </button>
        {aoCancelar && (
          <button className="button button-secondary" type="button" onClick={aoCancelar} disabled={salvando}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  );
}
