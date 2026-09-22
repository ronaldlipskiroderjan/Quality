import { describe, expect, it } from "vitest";
import { normalizarParticipante } from "./participants";
import type { ParticipanteApi } from "./participants";

const participanteBase = {
  id: "participacao-1",
  usuarioId: "usuario-1",
  nome: "Maria",
  email: "maria@quality.test",
  temImagem: false,
  permissoes: [],
  criadoEm: "2026-09-16T08:00:00",
};

describe("normalização de participantes", () => {
  it("mantém o papel do contrato atual", () => {
    const participante = normalizarParticipante({
      ...participanteBase,
      papel: "AUDITOR_RESPONSAVEL_QUALIDADE",
    });

    expect(participante.papel).toBe("AUDITOR_RESPONSAVEL_QUALIDADE");
  });

  it("aceita o array de papéis do contrato anterior", () => {
    const entrada: ParticipanteApi = {
      ...participanteBase,
      papeis: ["RESPONSAVEL_QUALIDADE"],
    };

    expect(normalizarParticipante(entrada).papel)
      .toBe("AUDITOR_RESPONSAVEL_QUALIDADE");
  });

  it("preserva a tela quando o papel não foi informado", () => {
    expect(normalizarParticipante(participanteBase).papel).toBeNull();
  });
});
