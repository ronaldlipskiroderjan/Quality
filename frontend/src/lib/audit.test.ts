import { describe, expect, it } from "vitest";
import { calcularResumoAuditoria, obterPendenciasAuditoria } from "./audit";
import type { ItemAuditoria, NaoConformidade, ResultadoItem } from "../types/api";

function item(itemId: string, respostaId: string | null, resultado: ResultadoItem | null): ItemAuditoria {
  return {
    itemId,
    respostaId,
    ordem: Number(itemId),
    pergunta: "Pergunta " + itemId,
    resultado,
    observacao: null,
    respondidoEm: null,
    atualizadoEm: null,
    versaoRegistro: null,
  };
}

describe("regras da auditoria", () => {
  it("calcula progresso e aderência desconsiderando itens não aplicáveis", () => {
    const resumo = calcularResumoAuditoria([
      item("1", "r1", "CONFORME"),
      item("2", "r2", "NAO_CONFORME"),
      item("3", "r3", "NAO_APLICAVEL"),
      item("4", null, null),
    ]);

    expect(resumo).toEqual({
      totalItens: 4,
      totalRespondidos: 3,
      conformes: 1,
      naoConformes: 1,
      naoAplicaveis: 1,
      aderenciaPercentual: 50,
    });
  });

  it("informa respostas pendentes e não conformidades sem registro", () => {
    const itens = [
      item("1", "r1", "NAO_CONFORME"),
      item("2", "r2", "NAO_CONFORME"),
      item("3", null, null),
    ];
    const registros = [{ respostaId: "r1" } as NaoConformidade];

    expect(obterPendenciasAuditoria(itens, registros)).toEqual({
      itensSemResposta: 1,
      naoConformidadesSemEnvio: 1,
    });
  });
});
