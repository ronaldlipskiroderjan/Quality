import type { ItemAuditoria, NaoConformidade } from "../types/api";

export interface ResumoAuditoria {
  totalItens: number;
  totalRespondidos: number;
  conformes: number;
  naoConformes: number;
  naoAplicaveis: number;
  aderenciaPercentual: number | null;
}

export interface PendenciasAuditoria {
  itensSemResposta: number;
  naoConformidadesSemEnvio: number;
}

export function calcularResumoAuditoria(itens: ItemAuditoria[]): ResumoAuditoria {
  const conformes = itens.filter((item) => item.resultado === "CONFORME").length;
  const naoConformes = itens.filter((item) => item.resultado === "NAO_CONFORME").length;
  const naoAplicaveis = itens.filter((item) => item.resultado === "NAO_APLICAVEL").length;
  const totalRespondidos = conformes + naoConformes + naoAplicaveis;
  const itensAvaliados = conformes + naoConformes;

  return {
    totalItens: itens.length,
    totalRespondidos,
    conformes,
    naoConformes,
    naoAplicaveis,
    aderenciaPercentual: itensAvaliados === 0 ? null : Number(((conformes / itensAvaliados) * 100).toFixed(2)),
  };
}

export function obterPendenciasAuditoria(
  itens: ItemAuditoria[],
  naoConformidades: NaoConformidade[],
): PendenciasAuditoria {
  const respostasComNaoConformidade = new Set(
    naoConformidades.map((naoConformidade) => naoConformidade.respostaId),
  );

  return {
    itensSemResposta: itens.filter((item) => item.resultado === null).length,
    naoConformidadesSemEnvio: itens.filter(
      (item) =>
        item.resultado === "NAO_CONFORME" &&
        item.respostaId !== null &&
        !respostasComNaoConformidade.has(item.respostaId),
    ).length,
  };
}
