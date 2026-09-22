import type { ReactNode } from "react";

interface PageHeaderProps {
  titulo: string;
  descricao?: string;
  acao?: ReactNode;
}

export function PageHeader({ titulo, descricao, acao }: PageHeaderProps) {
  return (
    <header className="page-header">
      <div>
        <h1>{titulo}</h1>
        {descricao && <p>{descricao}</p>}
      </div>
      {acao && <div className="page-actions">{acao}</div>}
    </header>
  );
}
