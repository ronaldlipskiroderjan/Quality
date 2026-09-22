import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type PointerEvent as ReactPointerEvent,
} from "react";
import { ErrorMessage } from "./Feedback";
import { Modal } from "./Modal";

const TAMANHO_MAXIMO_IMAGEM = 5 * 1024 * 1024;
const TIPOS_IMAGEM = ["image/jpeg", "image/png", "image/webp"];
const TAMANHO_SAIDA = 512;

interface ProfileImageEditorProps {
  imagemAtualUrl: string | null;
  possuiImagem: boolean;
  nomeUsuario: string;
  salvando: boolean;
  erro: string;
  aoFechar: () => void;
  aoErro: (mensagem: string) => void;
  aoSalvar: (imagem: File) => Promise<void>;
  aoExcluir: () => Promise<void>;
}

interface MetricasImagem {
  escala: number;
  largura: number;
  altura: number;
  limiteX: number;
  limiteY: number;
}

function limitar(valor: number, minimo: number, maximo: number) {
  return Math.min(maximo, Math.max(minimo, valor));
}

function iniciais(nome: string) {
  return nome
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0]?.toUpperCase())
    .join("");
}

function calcularMetricas(imagem: HTMLImageElement | null, zoom: number): MetricasImagem {
  if (!imagem) return { escala: 1, largura: 0, altura: 0, limiteX: 0, limiteY: 0 };
  const escalaBase = Math.max(
    TAMANHO_SAIDA / imagem.naturalWidth,
    TAMANHO_SAIDA / imagem.naturalHeight,
  );
  const escala = escalaBase * zoom;
  const largura = imagem.naturalWidth * escala;
  const altura = imagem.naturalHeight * escala;
  return {
    escala,
    largura,
    altura,
    limiteX: Math.max(0, (largura - TAMANHO_SAIDA) / 2),
    limiteY: Math.max(0, (altura - TAMANHO_SAIDA) / 2),
  };
}

function gerarArquivo(canvas: HTMLCanvasElement) {
  return new Promise<File>((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error("Não foi possível preparar a imagem."));
        return;
      }
      resolve(new File([blob], "imagem-perfil.jpg", { type: "image/jpeg" }));
    }, "image/jpeg", 0.92);
  });
}

export function ProfileImageEditor({
  imagemAtualUrl,
  possuiImagem,
  nomeUsuario,
  salvando,
  erro,
  aoFechar,
  aoErro,
  aoSalvar,
  aoExcluir,
}: ProfileImageEditorProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const arrasteRef = useRef<{
    pointerId: number;
    inicioX: number;
    inicioY: number;
    deslocamentoX: number;
    deslocamentoY: number;
  } | null>(null);
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [arquivoUrl, setArquivoUrl] = useState<string | null>(null);
  const [imagem, setImagem] = useState<HTMLImageElement | null>(null);
  const [zoom, setZoom] = useState(1);
  const [deslocamentoX, setDeslocamentoX] = useState(0);
  const [deslocamentoY, setDeslocamentoY] = useState(0);

  useEffect(() => {
    if (!arquivo) {
      setArquivoUrl(null);
      return;
    }
    const url = URL.createObjectURL(arquivo);
    setArquivoUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [arquivo]);

  const fonte = arquivoUrl ?? imagemAtualUrl;

  useEffect(() => {
    if (!fonte) {
      setImagem(null);
      return;
    }
    let ativo = true;
    const novaImagem = new Image();
    novaImagem.onload = () => {
      if (!ativo) return;
      setImagem(novaImagem);
      setZoom(1);
      setDeslocamentoX(0);
      setDeslocamentoY(0);
    };
    novaImagem.onerror = () => {
      if (ativo) aoErro("Não foi possível abrir essa imagem. Escolha outro arquivo.");
    };
    novaImagem.src = fonte;
    return () => {
      ativo = false;
    };
  }, [aoErro, fonte]);

  const metricas = useMemo(() => calcularMetricas(imagem, zoom), [imagem, zoom]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !imagem) return;
    const contexto = canvas.getContext("2d");
    if (!contexto) return;
    contexto.clearRect(0, 0, TAMANHO_SAIDA, TAMANHO_SAIDA);
    contexto.drawImage(
      imagem,
      (TAMANHO_SAIDA - metricas.largura) / 2 + deslocamentoX,
      (TAMANHO_SAIDA - metricas.altura) / 2 + deslocamentoY,
      metricas.largura,
      metricas.altura,
    );
  }, [deslocamentoX, deslocamentoY, imagem, metricas]);

  function selecionarImagem(event: ChangeEvent<HTMLInputElement>) {
    const selecionado = event.target.files?.[0] ?? null;
    aoErro("");
    if (!selecionado) return;
    if (!TIPOS_IMAGEM.includes(selecionado.type)) {
      event.target.value = "";
      aoErro("Escolha uma imagem JPG, PNG ou WebP.");
      return;
    }
    if (selecionado.size > TAMANHO_MAXIMO_IMAGEM) {
      event.target.value = "";
      aoErro("A imagem deve possuir no máximo 5 MB.");
      return;
    }
    setArquivo(selecionado);
  }

  function alterarZoom(valor: number) {
    const novasMetricas = calcularMetricas(imagem, valor);
    setZoom(valor);
    setDeslocamentoX((atual) => limitar(atual, -novasMetricas.limiteX, novasMetricas.limiteX));
    setDeslocamentoY((atual) => limitar(atual, -novasMetricas.limiteY, novasMetricas.limiteY));
  }

  function iniciarArraste(event: ReactPointerEvent<HTMLCanvasElement>) {
    if (!imagem) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    arrasteRef.current = {
      pointerId: event.pointerId,
      inicioX: event.clientX,
      inicioY: event.clientY,
      deslocamentoX,
      deslocamentoY,
    };
  }

  function moverImagem(event: ReactPointerEvent<HTMLCanvasElement>) {
    const arraste = arrasteRef.current;
    if (!arraste || arraste.pointerId !== event.pointerId) return;
    const proporcao = TAMANHO_SAIDA / event.currentTarget.getBoundingClientRect().width;
    setDeslocamentoX(limitar(
      arraste.deslocamentoX + (event.clientX - arraste.inicioX) * proporcao,
      -metricas.limiteX,
      metricas.limiteX,
    ));
    setDeslocamentoY(limitar(
      arraste.deslocamentoY + (event.clientY - arraste.inicioY) * proporcao,
      -metricas.limiteY,
      metricas.limiteY,
    ));
  }

  function encerrarArraste(event: ReactPointerEvent<HTMLCanvasElement>) {
    if (arrasteRef.current?.pointerId === event.pointerId) arrasteRef.current = null;
  }

  async function salvar() {
    const canvas = canvasRef.current;
    if (!canvas || !imagem) return;
    aoErro("");
    try {
      await aoSalvar(await gerarArquivo(canvas));
    } catch (error) {
      aoErro(error instanceof Error ? error.message : "Não foi possível preparar a imagem.");
    }
  }

  return (
    <Modal
      aberto
      amplo
      titulo="Editar imagem de perfil"
      descricao="Ajuste o enquadramento antes de salvar. A prévia circular mostra como sua imagem aparecerá no sistema."
      aoFechar={aoFechar}
    >
      <div className="profile-image-editor">
        <div className="profile-editor-stage">
          {fonte ? (
            <canvas
              ref={canvasRef}
              className={`profile-crop-canvas${imagem ? " is-draggable" : ""}`}
              width={TAMANHO_SAIDA}
              height={TAMANHO_SAIDA}
              role="img"
              aria-label="Pré-visualização do enquadramento da imagem de perfil"
              onPointerDown={iniciarArraste}
              onPointerMove={moverImagem}
              onPointerUp={encerrarArraste}
              onPointerCancel={encerrarArraste}
            />
          ) : (
            <div className="profile-editor-empty" aria-label="Perfil sem imagem">
              <span aria-hidden="true">{iniciais(nomeUsuario)}</span>
              <p>Escolha uma imagem para começar.</p>
            </div>
          )}
          {imagem && <p className="profile-drag-hint">Arraste a imagem para reposicionar.</p>}
        </div>

        <div className="profile-editor-controls">
          <div className="profile-editor-file-info">
            <strong>Arquivo da imagem</strong>
            <span>JPG, PNG ou WebP · até 5 MB</span>
            {arquivo && <small title={arquivo.name}>{arquivo.name}</small>}
          </div>

          <input
            ref={inputRef}
            className="visually-hidden"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={selecionarImagem}
          />
          <button
            className="button button-secondary profile-editor-choose"
            type="button"
            autoFocus
            onClick={() => inputRef.current?.click()}
          >
            Escolher imagem no dispositivo
          </button>

          {imagem && (
            <div className="profile-crop-controls">
              <label>
                Zoom
                <input
                  type="range"
                  min={1}
                  max={3}
                  step={0.01}
                  value={zoom}
                  onChange={(event) => alterarZoom(Number(event.target.value))}
                />
              </label>
              <label>
                Posição horizontal
                <input
                  type="range"
                  min={-metricas.limiteX}
                  max={metricas.limiteX}
                  step={1}
                  value={deslocamentoX}
                  disabled={metricas.limiteX === 0}
                  onChange={(event) => setDeslocamentoX(Number(event.target.value))}
                />
              </label>
              <label>
                Posição vertical
                <input
                  type="range"
                  min={-metricas.limiteY}
                  max={metricas.limiteY}
                  step={1}
                  value={deslocamentoY}
                  disabled={metricas.limiteY === 0}
                  onChange={(event) => setDeslocamentoY(Number(event.target.value))}
                />
              </label>
            </div>
          )}

          {erro && <ErrorMessage mensagem={erro} />}

          {possuiImagem && (
            <button
              className="text-button danger-text profile-editor-delete"
              type="button"
              disabled={salvando}
              onClick={() => void aoExcluir()}
            >
              Excluir imagem atual
            </button>
          )}
        </div>
      </div>

      <div className="profile-editor-actions">
        <button className="button button-secondary" type="button" disabled={salvando} onClick={aoFechar}>
          Cancelar
        </button>
        <button className="button button-primary" type="button" disabled={salvando || !imagem} onClick={() => void salvar()}>
          {salvando ? "Salvando…" : "Salvar imagem"}
        </button>
      </div>
    </Modal>
  );
}
