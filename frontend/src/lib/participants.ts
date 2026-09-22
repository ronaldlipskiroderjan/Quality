import type { Pagina, PapelPlano, Participante } from "../types/api";

export type ParticipanteApi = Omit<Participante, "papel"> & {
  papel?: PapelPlanoApi | null;
  papeis?: PapelPlanoApi[];
};

type PapelPlanoApi =
  | PapelPlano
  | "AUDITOR"
  | "RESPONSAVEL_QUALIDADE";

function normalizarPapel(papel?: PapelPlanoApi | null): PapelPlano | null {
  if (papel === "AUDITOR" || papel === "RESPONSAVEL_QUALIDADE") {
    return "AUDITOR_RESPONSAVEL_QUALIDADE";
  }
  return papel ?? null;
}

export function normalizarParticipante(entrada: ParticipanteApi): Participante {
  return {
    ...entrada,
    papel: normalizarPapel(entrada.papel ?? entrada.papeis?.[0]),
  };
}

export function normalizarPaginaParticipantes(
  pagina: Pagina<ParticipanteApi>,
): Pagina<Participante> {
  return {
    ...pagina,
    conteudo: pagina.conteudo.map(normalizarParticipante),
  };
}
