import type { Resolucao, StatusNaoConformidade } from "../types/api";

const statusTrataveis = new Set<StatusNaoConformidade>([
  "ENVIADA",
  "EM_TRATAMENTO",
  "VENCIDA",
  "ESCALONADA_N1",
  "VENCIDA_N1",
  "ESCALONADA_N2",
  "VENCIDA_N2",
]);

export function permiteInformarResolucao(status: StatusNaoConformidade): boolean {
  return statusTrataveis.has(status);
}

export function encontrarResolucaoPendente(resolucoes: Resolucao[]): Resolucao | undefined {
  return resolucoes.find((resolucao) => resolucao.status === "INFORMADA");
}
