import { useEffect, useRef, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { Icon } from "./Icon";

interface FeedbackProps {
  mensagem: string;
}

interface ErrorMessageProps extends FeedbackProps {
  titulo?: string;
  acao?: ReactNode;
}

interface SuccessMessageProps extends FeedbackProps {
  aoFechar?: () => void;
}

export function LoadingState({ mensagem }: FeedbackProps) {
  return (
    <div className="state-message" role="status" aria-live="polite">
      <div className="state-skeleton" aria-hidden="true">
        <span /><span /><span />
      </div>
      <strong>{mensagem}</strong>
      <span>Aguarde enquanto organizamos as informações.</span>
    </div>
  );
}

export function EmptyState({ mensagem, titulo = "Nada por aqui ainda", acao }: FeedbackProps & { titulo?: string; acao?: ReactNode }) {
  return (
    <div className="empty-state">
      <span className="state-icon"><Icon name="archive" size={22} /></span>
      <strong>{titulo}</strong>
      <p>{mensagem}</p>
      {acao}
    </div>
  );
}

export function ErrorMessage({ mensagem, titulo = "Não foi possível concluir", acao }: ErrorMessageProps) {
  return (
    <div className="alert alert-error" role="alert">
      <span className="feedback-symbol" aria-hidden="true"><Icon name="warning" size={18} /></span>
      <div className="feedback-copy"><strong>{titulo}</strong><span>{mensagem}</span>{acao}</div>
    </div>
  );
}

export function SuccessMessage({ mensagem, aoFechar }: SuccessMessageProps) {
  const [visivel, setVisivel] = useState(true);
  const [saindo, setSaindo] = useState(false);
  const aoFecharRef = useRef(aoFechar);

  useEffect(() => {
    aoFecharRef.current = aoFechar;
  }, [aoFechar]);

  useEffect(() => {
    setVisivel(true);
    setSaindo(false);

    const iniciarSaida = window.setTimeout(() => setSaindo(true), 1700);
    const ocultar = window.setTimeout(() => {
      setVisivel(false);
      aoFecharRef.current?.();
    }, 2000);

    return () => {
      window.clearTimeout(iniciarSaida);
      window.clearTimeout(ocultar);
    };
  }, [mensagem]);

  if (!visivel) return null;

  return createPortal(
    <div
      className={`alert alert-success feedback-temporary${saindo ? " is-leaving" : ""}`}
      role="status"
      aria-live="polite"
      aria-atomic="true"
    >
      <span className="feedback-symbol" aria-hidden="true"><Icon name="check" size={18} /></span>
      <div className="feedback-copy"><strong>Alteração concluída</strong><span>{mensagem}</span></div>
    </div>,
    document.body,
  );
}
