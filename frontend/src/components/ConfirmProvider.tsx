import { createContext, useContext, useState, type ReactNode } from "react";
import { Icon } from "./Icon";
import { Modal } from "./Modal";

interface ConfirmOptions {
  titulo: string;
  mensagem: string;
  textoConfirmar: string;
  perigo?: boolean;
}

interface PendingConfirmation extends ConfirmOptions {
  concluir: (valor: boolean) => void;
}

const ConfirmContext = createContext<((opcoes: ConfirmOptions) => Promise<boolean>) | null>(null);

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [pendente, setPendente] = useState<PendingConfirmation | null>(null);

  function confirmar(opcoes: ConfirmOptions) {
    return new Promise<boolean>((concluir) => setPendente({ ...opcoes, concluir }));
  }

  function responder(valor: boolean) {
    pendente?.concluir(valor);
    setPendente(null);
  }

  return (
    <ConfirmContext.Provider value={confirmar}>
      {children}
      <Modal
        aberto={Boolean(pendente)}
        titulo={pendente?.titulo ?? ""}
        descricao={pendente?.perigo ? undefined : pendente?.mensagem}
        aoFechar={() => responder(false)}
      >
        {pendente?.perigo && (
          <div className="confirm-warning" role="alert">
            <span aria-hidden="true"><Icon name="warning" size={22} /></span>
            <p>{pendente.mensagem}</p>
          </div>
        )}
        <div className="confirm-actions">
          <button className="button button-secondary" type="button" onClick={() => responder(false)}>
            Cancelar
          </button>
          <button
            className={pendente?.perigo ? "button button-danger" : "button button-primary"}
            type="button"
            onClick={() => responder(true)}
          >
            {pendente?.textoConfirmar}
          </button>
        </div>
      </Modal>
    </ConfirmContext.Provider>
  );
}

export function useConfirm() {
  const confirmar = useContext(ConfirmContext);
  if (!confirmar) throw new Error("useConfirm deve ser usado dentro de ConfirmProvider.");
  return confirmar;
}
