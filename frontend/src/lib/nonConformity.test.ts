import { describe, expect, it } from "vitest";
import {
  encontrarResolucaoPendente,
  permiteInformarResolucao,
} from "./nonConformity";
import type { Resolucao } from "../types/api";

describe("regras da não conformidade", () => {
  it("reconhece estados em que a equipe pode informar a resolução", () => {
    expect(permiteInformarResolucao("ENVIADA")).toBe(true);
    expect(permiteInformarResolucao("EM_TRATAMENTO")).toBe(true);
    expect(permiteInformarResolucao("ESCALONADA_N1")).toBe(true);
    expect(permiteInformarResolucao("ESCALONADA_N2")).toBe(true);
    expect(permiteInformarResolucao("RESOLUCAO_INFORMADA")).toBe(false);
  });

  it("localiza a resolução que aguarda validação", () => {
    const resolucoes = [
      { id: "r1", status: "AJUSTES_SOLICITADOS" },
      { id: "r2", status: "INFORMADA" },
    ] as Resolucao[];

    expect(encontrarResolucaoPendente(resolucoes)?.id).toBe("r2");
  });
});
