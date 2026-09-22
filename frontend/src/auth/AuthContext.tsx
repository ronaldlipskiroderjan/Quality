import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { limparSessao, obterToken, salvarSessao, sessaoValida } from "../lib/api";
import { authApi } from "../services/qualityApi";
import type { Usuario } from "../types/api";

interface AuthContextValue {
  usuario: Usuario | null;
  imagemPerfilUrl: string | null;
  autenticado: boolean;
  carregando: boolean;
  entrar: (email: string, senha: string) => Promise<void>;
  atualizarPerfil: (nome: string, email: string) => Promise<void>;
  salvarImagemPerfil: (imagem: File) => Promise<void>;
  removerImagemPerfil: () => Promise<void>;
  alterarSenha: (senhaAntiga: string, novaSenha: string, confirmacaoSenha: string) => Promise<void>;
  sair: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [imagemPerfilUrl, setImagemPerfilUrl] = useState<string | null>(null);
  const [versaoImagem, setVersaoImagem] = useState(0);
  const [carregando, setCarregando] = useState(true);

  const sair = useCallback(() => {
    limparSessao();
    setUsuario(null);
  }, []);

  const carregarPerfil = useCallback(async () => {
    if (!sessaoValida()) {
      sair();
      setCarregando(false);
      return;
    }

    try {
      setUsuario(await authApi.perfil());
    } catch {
      sair();
    } finally {
      setCarregando(false);
    }
  }, [sair]);

  useEffect(() => {
    void carregarPerfil();
  }, [carregarPerfil]);

  useEffect(() => {
    if (!usuario?.temImagem) {
      setImagemPerfilUrl(null);
      return;
    }

    let ativo = true;
    let url: string | null = null;
    void authApi.buscarImagemPerfil()
      .then((imagem) => {
        const novaUrl = URL.createObjectURL(imagem);
        if (!ativo) {
          URL.revokeObjectURL(novaUrl);
          return;
        }
        url = novaUrl;
        setImagemPerfilUrl(novaUrl);
      })
      .catch(() => {
        if (ativo) setImagemPerfilUrl(null);
      });

    return () => {
      ativo = false;
      if (url) URL.revokeObjectURL(url);
    };
  }, [usuario?.id, usuario?.temImagem, versaoImagem]);

  useEffect(() => {
    const aoExpirar = () => sair();
    window.addEventListener("quality:unauthorized", aoExpirar);
    return () => window.removeEventListener("quality:unauthorized", aoExpirar);
  }, [sair]);

  const entrar = useCallback(async (email: string, senha: string) => {
    const token = await authApi.entrar(email, senha);
    salvarSessao(token.accessToken, token.expiresInMs);
    try {
      setUsuario(await authApi.perfil());
    } catch (error) {
      limparSessao();
      throw error;
    }
  }, []);

  const atualizarPerfil = useCallback(async (nome: string, email: string) => {
    setUsuario(await authApi.atualizarPerfil(nome, email));
  }, []);

  const salvarImagemPerfil = useCallback(async (imagem: File) => {
    await authApi.salvarImagemPerfil(imagem);
    setUsuario((atual) => atual ? { ...atual, temImagem: true } : atual);
    setVersaoImagem((versao) => versao + 1);
  }, []);

  const removerImagemPerfil = useCallback(async () => {
    await authApi.removerImagemPerfil();
    setUsuario((atual) => atual ? { ...atual, temImagem: false } : atual);
    setImagemPerfilUrl(null);
    setVersaoImagem((versao) => versao + 1);
  }, []);

  const alterarSenha = useCallback(async (
    senhaAntiga: string,
    novaSenha: string,
    confirmacaoSenha: string,
  ) => {
    await authApi.alterarSenha(senhaAntiga, novaSenha, confirmacaoSenha);
  }, []);

  useEffect(() => {
    if (!obterToken()) return;
    const intervalo = window.setInterval(async () => {
      if (!sessaoValida()) {
        sair();
        return;
      }
      try {
        const token = await authApi.renovar();
        salvarSessao(token.accessToken, token.expiresInMs);
      } catch {
        sair();
      }
    }, 15 * 60 * 1000);
    return () => window.clearInterval(intervalo);
  }, [sair, usuario]);

  const valor = useMemo(
    () => ({
      usuario,
      imagemPerfilUrl,
      autenticado: Boolean(usuario),
      carregando,
      entrar,
      atualizarPerfil,
      salvarImagemPerfil,
      removerImagemPerfil,
      alterarSenha,
      sair,
    }),
    [
      usuario,
      imagemPerfilUrl,
      carregando,
      entrar,
      atualizarPerfil,
      salvarImagemPerfil,
      removerImagemPerfil,
      alterarSenha,
      sair,
    ],
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext);
  if (!contexto) throw new Error("useAuth deve ser usado dentro de AuthProvider.");
  return contexto;
}
