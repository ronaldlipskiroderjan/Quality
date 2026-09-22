import { createPortal } from "react-dom";
import { useEffect, useId, useRef, type ReactNode } from "react";

interface ModalProps {
  aberto: boolean;
  titulo: string;
  descricao?: string;
  aoFechar: () => void;
  children: ReactNode;
  amplo?: boolean;
}

export function Modal({ aberto, titulo, descricao, aoFechar, children, amplo = false }: ModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const tituloId = useId();
  const descricaoId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog || !aberto) return;
    const focoAnterior = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    dialog.showModal();
    requestAnimationFrame(() => {
      dialog.querySelector<HTMLElement>(
        "[autofocus], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), .modal-body button:not([disabled]), .modal-close",
      )?.focus();
    });
    return () => {
      if (dialog.open) dialog.close();
      focoAnterior?.focus();
    };
  }, [aberto]);

  if (!aberto) return null;

  return createPortal(
    <dialog
      ref={dialogRef}
      className={"modal" + (amplo ? " modal-wide" : "")}
      aria-labelledby={tituloId}
      aria-describedby={descricao ? descricaoId : undefined}
      onCancel={(event) => {
        event.preventDefault();
        aoFechar();
      }}
      onClick={(event) => {
        if (event.target === dialogRef.current) aoFechar();
      }}
    >
      <div className="modal-content">
        <header className="modal-header">
          <div>
            <h2 id={tituloId}>{titulo}</h2>
            {descricao && <p id={descricaoId}>{descricao}</p>}
          </div>
          <button className="modal-close" type="button" aria-label="Fechar janela" onClick={aoFechar}>
            ×
          </button>
        </header>
        <div className="modal-body">{children}</div>
      </div>
    </dialog>,
    document.body,
  );
}
