import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { AuthWelcomePanel } from "../components/AuthWelcomePanel";
import { ErrorMessage, SuccessMessage } from "../components/Feedback";
import { mensagemErro } from "../lib/api";

export function LoginPage() {
  const { entrar } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [senhaVisivel, setSenhaVisivel] = useState(false);
  const [erro, setErro] = useState("");
  const [enviando, setEnviando] = useState(false);

  async function enviar(event: FormEvent) {
    event.preventDefault();
    try {
      setEnviando(true);
      setErro("");
      await entrar(email, senha);
      const destino = (location.state as { de?: string } | null)?.de || "/dashboard";
      navigate(destino, { replace: true });
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="auth-page login-page">
      <section className="auth-panel login-panel" aria-label="Acesso ao Quality">
        <AuthWelcomePanel
          tituloId="login-slogan"
          titulo="Qualidade, conduzida com clareza."
          descricao="Planeje, audite e acompanhe cada resolução em um fluxo preciso e rastreável."
        />

        <div className="login-access">
          <form
            className="form-stack auth-form login-form"
            aria-labelledby="titulo-login"
            onSubmit={enviar}
          >
            <div className="auth-form-heading login-form-heading">
              <h2 id="titulo-login">Entre na sua conta</h2>
              <p>Acesse seus planos e continue de onde parou.</p>
            </div>

            {(location.state as { cadastroConcluido?: boolean } | null)?.cadastroConcluido && (
              <SuccessMessage mensagem="Conta criada. Entre com suas credenciais." />
            )}
            {erro && <ErrorMessage mensagem={erro} />}

            <label>
              E-mail
              <input
                type="email"
                autoComplete="email"
                inputMode="email"
                required
                autoFocus
                disabled={enviando}
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="nome@empresa.com"
              />
            </label>

            <div className="login-field">
              <label htmlFor="senha-login">Senha</label>
              <span className="login-password-field">
                <input
                  id="senha-login"
                  type={senhaVisivel ? "text" : "password"}
                  autoComplete="current-password"
                  required
                  disabled={enviando}
                  value={senha}
                  onChange={(event) => setSenha(event.target.value)}
                  placeholder="Digite sua senha"
                />
                <button
                  type="button"
                  aria-label={senhaVisivel ? "Ocultar senha" : "Mostrar senha"}
                  aria-pressed={senhaVisivel}
                  onClick={() => setSenhaVisivel((visivel) => !visivel)}
                >
                  {senhaVisivel ? "Ocultar" : "Mostrar"}
                </button>
              </span>
            </div>

            <button className="button button-primary login-submit" type="submit" disabled={enviando}>
              {enviando ? "Entrando…" : "Entrar"}
            </button>

            <div className="login-register-cta">
              <div>
                <strong>Ainda não possui uma conta?</strong>
                <span>Crie seu acesso para começar um plano de qualidade.</span>
              </div>
              <Link className="button login-register-link" to="/cadastro">Criar conta</Link>
            </div>
          </form>
        </div>
      </section>
    </main>
  );
}
