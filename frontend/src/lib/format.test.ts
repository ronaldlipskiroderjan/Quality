import { describe, expect, it } from "vitest";
import {
  formatarPrazo,
  formatarTamanho,
  rotuloEnum,
  rotuloStatusNaoConformidade,
} from "./format";

describe("formatadores", () => {
  it("converte enums em rótulos legíveis", () => {
    expect(rotuloEnum("AUDITOR_RESPONSAVEL_QUALIDADE"))
      .toBe("Auditor e responsável de qualidade");
    expect(rotuloEnum(undefined)).toBe("Não informado");
  });

  it("formata tamanhos de arquivo", () => {
    expect(formatarTamanho(512)).toBe("512 B");
    expect(formatarTamanho(2048)).toBe("2.0 KB");
    expect(formatarTamanho(2 * 1024 * 1024)).toBe("2.0 MB");
  });

  it("formata prazos combinando dias e horas", () => {
    expect(formatarPrazo(2, 3)).toBe("2 dias e 3 horas");
    expect(formatarPrazo(1, 0)).toBe("1 dia");
    expect(formatarPrazo(0, 1)).toBe("1 hora");
    expect(formatarPrazo(0, 0)).toBe("0 horas");
  });

  it("apresenta os estados operacionais da não conformidade", () => {
    expect(rotuloStatusNaoConformidade("RASCUNHO")).toBe("");
    expect(rotuloStatusNaoConformidade("EM_TRATAMENTO")).toBe("Em correção");
    expect(rotuloStatusNaoConformidade("ESCALONADA_N1"))
      .toBe("Escalonado · N1");
    expect(rotuloStatusNaoConformidade("ESCALONADA_N2"))
      .toBe("Escalonado · N2");
  });
});
