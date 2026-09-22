import type { Pagina } from "../types/api";

interface PaginationProps {
  pagina: Pick<Pagina<unknown>, "pagina" | "totalPaginas" | "primeira" | "ultima">;
  aoMudar: (pagina: number) => void;
}

export function Pagination({ pagina, aoMudar }: PaginationProps) {
  if (pagina.totalPaginas <= 1) return null;

  return (
    <nav className="pagination" aria-label="Paginação">
      <button type="button" className="button button-secondary" disabled={pagina.primeira} onClick={() => aoMudar(pagina.pagina - 1)}>
        Anterior
      </button>
      <span>
        Página {pagina.pagina + 1} de {pagina.totalPaginas}
      </span>
      <button type="button" className="button button-secondary" disabled={pagina.ultima} onClick={() => aoMudar(pagina.pagina + 1)}>
        Próxima
      </button>
    </nav>
  );
}
