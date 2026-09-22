import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AuthWelcomePanel } from "../components/AuthWelcomePanel";
import { ErrorMessage } from "../components/Feedback";
import { mensagemErro } from "../lib/api";
import { authApi } from "../services/qualityApi";

export function RegisterPage() {
  const navigate = useNavigate();
  const [nome, setNome] = useState("");
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
      await authApi.cadastrar(nome, email, senha);
      navigate("/entrar", { replace: true, state: { cadastroConcluido: true } });
    } catch (error) {
      setErro(mensagemErro(error));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="auth-page login-page register-page">
      <section className="auth-panel login-panel" aria-label="Cadastro no Quality">
        <AuthWelcomePanel
          tituloId="cadastro-slogan"
          titulo="Comece com uma base confiável."
          descricao="Crie sua conta para estruturar planos, auditorias e resoluções em um só ambiente."
        />

        <div className="login-access">
          <form
            className="form-stack auth-form login-form register-form"
            aria-labelledby="titulo-cadastro"
            onSubmit={enviar}
          >
            <div className="auth-form-heading login-form-heading">
              <h2 id="titulo-cadastro">Crie sua conta</h2>
              <p>Configure seu acesso para começar.</p>
            </div>

            {erro && <ErrorMessage mensagem={erro} />}

            <label>
              Nome
              <input
                autoComplete="name"
                required
                autoFocus
                maxLength={100}
                disabled={enviando}
                value={nome}
                onChange={(event) => setNome(event.target.value)}
                placeholder="Seu nome completo"
              />
            </label>

            <label>
              E-mail
              <input
                type="email"
                autoComplete="email"
                inputMode="email"
                required
                maxLength={254}
                disabled={enviando}
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="nome@empresa.com"
              />
            </label>

            <div className="login-field">
              <label htmlFor="senha-cadastro">Senha</label>
              <span className="login-password-field">
                <input
                  id="senha-cadastro"
                  type={senhaVisivel ? "text" : "password"}
                  autoComplete="new-password"
                  required
                  minLength={8}
                  maxLength={72}
                  disabled={enviando}
                  value={senha}
                  onChange={(event) => setSenha(event.target.value)}
                  placeholder="Crie uma senha"
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
              <small className="auth-field-help">Use entre 8 e 72 caracteres.</small>
            </div>

            <button className="button button-primary login-submit" type="submit" disabled={enviando}>
              {enviando ? "Criando conta…" : "Criar conta"}
            </button>

            <div className="login-register-cta">
              <div>
                <strong>Já possui uma conta?</strong>
                <span>Entre para acessar seus planos e atividades.</span>
              </div>
              <Link className="button login-register-link" to="/entrar">Entrar</Link>
            </div>
          </form>
        </div>
      </section>
    </main>
  );
}
