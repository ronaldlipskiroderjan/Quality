import type { ProblemaApi } from "../types/api";

const API_URL = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/$/, "");
const TOKEN_KEY = "quality.accessToken";
const EXPIRES_KEY = "quality.expiresAt";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly problema?: ProblemaApi,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function obterToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function salvarSessao(token: string, expiresInMs: number): void {
  sessionStorage.setItem(TOKEN_KEY, token);
  sessionStorage.setItem(EXPIRES_KEY, String(Date.now() + expiresInMs));
}

export function limparSessao(): void {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(EXPIRES_KEY);
}

export function sessaoValida(): boolean {
  const token = obterToken();
  const expiresAt = Number(sessionStorage.getItem(EXPIRES_KEY) || 0);
  return Boolean(token) && expiresAt > Date.now();
}

async function lerErro(response: Response): Promise<ProblemaApi | undefined> {
  const contentType = response.headers.get("content-type") || "";
  if (!contentType.includes("json")) return undefined;

  try {
    return (await response.json()) as ProblemaApi;
  } catch {
    return undefined;
  }
}

export async function requisitar<T>(caminho: string, opcoes: RequestInit = {}): Promise<T> {
  const headers = new Headers(opcoes.headers);
  const token = obterToken();

  headers.set("Accept", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (opcoes.body && !(opcoes.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_URL}${caminho}`, { ...opcoes, headers });

  if (!response.ok) {
    const problema = await lerErro(response);
    if (response.status === 401 && token) {
      window.dispatchEvent(new Event("quality:unauthorized"));
    }
    throw new ApiError(
      problema?.detail || problema?.title || "Não foi possível concluir a solicitação.",
      response.status,
      problema,
    );
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export async function baixarArquivo(caminho: string, nomeArquivo: string): Promise<void> {
  const token = obterToken();
  const response = await fetch(`${API_URL}${caminho}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });

  if (!response.ok) {
    const problema = await lerErro(response);
    throw new ApiError(problema?.detail || "Não foi possível baixar o arquivo.", response.status, problema);
  }

  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement("a");
  link.href = url;
  link.download = nomeArquivo;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export async function obterArquivo(caminho: string): Promise<Blob> {
  const token = obterToken();
  const response = await fetch(`${API_URL}${caminho}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!response.ok) {
    const problema = await lerErro(response);
    throw new ApiError(problema?.detail || "Não foi possível abrir o arquivo.", response.status, problema);
  }
  return response.blob();
}

export function mensagemErro(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  return "Ocorreu um erro inesperado. Tente novamente.";
}
