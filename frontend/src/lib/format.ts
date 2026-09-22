export function formatarData(data?: string | null): string {
  if (!data) return "—";
  const somenteData = !data.includes("T");
  const valor = somenteData ? new Date(`${data}T00:00:00`) : new Date(data);
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: somenteData ? undefined : "short",
  }).format(valor);
}

export function formatarTamanho(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 ** 2).toFixed(1)} MB`;
}

export function formatarPrazo(dias: number, horas: number): string {
  const partes: string[] = [];
  if (dias > 0) partes.push(`${dias} ${dias === 1 ? "dia" : "dias"}`);
  if (horas > 0) partes.push(`${horas} ${horas === 1 ? "hora" : "horas"}`);
  return partes.join(" e ") || "0 horas";
}

export function rotuloEnum(valor?: string | null): string {
  if (!valor) return "Não informado";
  if (valor === "AUDITOR_RESPONSAVEL_QUALIDADE") {
    return "Auditor e responsável de qualidade";
  }
  return valor
    .toLocaleLowerCase("pt-BR")
    .split("_")
    .map((parte) => parte.charAt(0).toLocaleUpperCase("pt-BR") + parte.slice(1))
    .join(" ");
}

export function rotuloStatusNaoConformidade(
  status?: string | null,
): string {
  if (!status || status === "RASCUNHO") return "";
  if (status === "EM_TRATAMENTO" || status === "ENVIADA") {
    return "Em correção";
  }
  if (status === "ESCALONADA_N1") return "Escalonado · N1";
  if (status === "ESCALONADA_N2") return "Escalonado · N2";
  if (status === "VENCIDA_N2") return "Prazo N2 excedido";
  return rotuloEnum(status);
}
