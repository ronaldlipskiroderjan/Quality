import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "../auth/AuthContext";
import { useConfirm } from "../components/ConfirmProvider";
import { ErrorMessage, SuccessMessage } from "../components/Feedback";
import { Icon } from "../components/Icon";
import { PageHeader } from "../components/PageHeader";
import { ProfileImageEditor } from "../components/ProfileImageEditor";
import { mensagemErro } from "../lib/api";

function iniciais(nome: string) {
  return nome
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0]?.toUpperCase())
    .join("");
}

export function ProfilePage() {
  const {
    usuario,
    imagemPerfilUrl,
    atualizarPerfil,
    salvarImagemPerfil,
    removerImagemPerfil,
    alterarSenha,
  } = useAuth();
  const confirmar = useConfirm();
  const [nome, setNome] = useState(usuario?.nome ?? "");
  const [email, setEmail] = useState(usuario?.email ?? "");
  const [editorImagemAberto, setEditorImagemAberto] = useState(false);
  const [salvandoPerfil, setSalvandoPerfil] = useState(false);
  const [salvandoImagem, setSalvandoImagem] = useState(false);
  const [salvandoSenha, setSalvandoSenha] = useState(false);
  const [erroPerfil, setErroPerfil] = useState("");
  const [erroImagem, setErroImagem] = useState("");
  const [erroSenha, setErroSenha] = useState("");
  const [sucessoPerfil, setSucessoPerfil] = useState("");
  const [sucessoImagem, setSucessoImagem] = useState("");
  const [sucessoSenha, setSucessoSenha] = useState("");
  const [senhaAtual, setSenhaAtual] = useState("");
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmacaoSenha, setConfirmacaoSenha] = useState("");

  useEffect(() => {
    if (!usuario) return;
    setNome(usuario.nome);
    setEmail(usuario.email);
  }, [usuario]);

  if (!usuario) return null;

  const perfilAlterado = nome.trim() !== usuario.nome || email.trim().toLowerCase() !== usuario.email;

  async function salvarDados(event: FormEvent) {
    event.preventDefault();
    setSalvandoPerfil(true);
    setErroPerfil("");
    setSucessoPerfil("");
    try {
      await atualizarPerfil(nome.trim(), email.trim().toLowerCase());
      setSucessoPerfil("Seus dados pessoais foram atualizados.");
    } catch (error) {
      setErroPerfil(mensagemErro(error));
    } finally {
      setSalvandoPerfil(false);
    }
  }

  async function enviarImagem(imagem: File) {
    setSalvandoImagem(true);
    setErroImagem("");
    setSucessoImagem("");
    try {
      await salvarImagemPerfil(imagem);
      setEditorImagemAberto(false);
      setSucessoImagem("Sua imagem de perfil foi atualizada.");
    } catch (error) {
      setErroImagem(mensagemErro(error));
    } finally {
      setSalvandoImagem(false);
    }
  }

  async function excluirImagem() {
    if (!await confirmar({
      titulo: "Excluir imagem de perfil?",
      mensagem: "A imagem será removida da sua conta e suas iniciais voltarão a ser exibidas no sistema.",
      textoConfirmar: "Excluir imagem",
      perigo: true,
    })) return;
    setSalvandoImagem(true);
    setErroImagem("");
    setSucessoImagem("");
    try {
      await removerImagemPerfil();
      setEditorImagemAberto(false);
      setSucessoImagem("Sua imagem de perfil foi excluída.");
    } catch (error) {
      setErroImagem(mensagemErro(error));
    } finally {
      setSalvandoImagem(false);
    }
  }

  async function salvarNovaSenha(event: FormEvent) {
    event.preventDefault();
    setErroSenha("");
    setSucessoSenha("");
    if (novaSenha !== confirmacaoSenha) {
      setErroSenha("A confirmação deve ser igual à nova senha.");
      return;
    }
    setSalvandoSenha(true);
    try {
      await alterarSenha(senhaAtual, novaSenha, confirmacaoSenha);
      setSenhaAtual("");
      setNovaSenha("");
      setConfirmacaoSenha("");
      setSucessoSenha("Sua senha foi alterada.");
    } catch (error) {
      setErroSenha(mensagemErro(error));
    } finally {
      setSalvandoSenha(false);
    }
  }

  return (
    <div className="profile-page">
      <PageHeader titulo="Sua conta" />

      <div className="profile-workspace">
        <aside className="profile-photo-panel" aria-labelledby="foto-perfil-titulo">
          <div className="profile-photo-frame">
            <div className="profile-photo-preview">
              {imagemPerfilUrl
                ? <img src={imagemPerfilUrl} alt={`Foto de perfil de ${usuario.nome}`} />
                : <span aria-hidden="true">{iniciais(usuario.nome)}</span>}
            </div>
            <button
              className="profile-photo-edit"
              type="button"
              aria-label="Editar imagem de perfil"
              onClick={() => {
                setErroImagem("");
                setSucessoImagem("");
                setEditorImagemAberto(true);
              }}
            >
              <Icon name="edit" size={18} />
            </button>
          </div>
          <div>
            <h2 id="foto-perfil-titulo">Imagem de perfil</h2>
            <p>Visível no cabeçalho e nas áreas em que você participa.</p>
          </div>
          {erroImagem && !editorImagemAberto && <ErrorMessage mensagem={erroImagem} />}
          {sucessoImagem && <SuccessMessage mensagem={sucessoImagem} aoFechar={() => setSucessoImagem("")} />}
        </aside>

        <div className="profile-settings">
          <section className="profile-section" aria-labelledby="dados-pessoais-titulo">
            <header>
              <h2 id="dados-pessoais-titulo">Dados pessoais</h2>
              <p>Essas informações identificam você nos planos e históricos.</p>
            </header>
            <form className="form-stack" onSubmit={salvarDados}>
              <label>
                Nome
                <input
                  autoComplete="name"
                  required
                  maxLength={100}
                  value={nome}
                  onChange={(event) => setNome(event.target.value)}
                />
              </label>
              <label>
                E-mail de acesso
                <input
                  autoComplete="email"
                  type="email"
                  required
                  maxLength={254}
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              </label>
              {erroPerfil && <ErrorMessage mensagem={erroPerfil} />}
              {sucessoPerfil && <SuccessMessage mensagem={sucessoPerfil} aoFechar={() => setSucessoPerfil("")} />}
              <div className="form-actions">
                <button
                  className="button button-primary"
                  disabled={salvandoPerfil || !perfilAlterado || !nome.trim() || !email.trim()}
                >
                  {salvandoPerfil ? "Salvando…" : "Salvar dados"}
                </button>
              </div>
            </form>
          </section>

          <section className="profile-section" aria-labelledby="seguranca-titulo">
            <header>
              <h2 id="seguranca-titulo">Senha</h2>
              <p>Use pelo menos oito caracteres para proteger sua conta.</p>
            </header>
            <form className="form-stack" onSubmit={salvarNovaSenha}>
              <label>
                Senha atual
                <input
                  autoComplete="current-password"
                  type="password"
                  required
                  value={senhaAtual}
                  onChange={(event) => setSenhaAtual(event.target.value)}
                />
              </label>
              <div className="form-row">
                <label>
                  Nova senha
                  <input
                    autoComplete="new-password"
                    type="password"
                    required
                    minLength={8}
                    maxLength={72}
                    value={novaSenha}
                    onChange={(event) => setNovaSenha(event.target.value)}
                  />
                </label>
                <label>
                  Confirmar nova senha
                  <input
                    autoComplete="new-password"
                    type="password"
                    required
                    minLength={8}
                    maxLength={72}
                    value={confirmacaoSenha}
                    onChange={(event) => setConfirmacaoSenha(event.target.value)}
                  />
                </label>
              </div>
              {erroSenha && <ErrorMessage mensagem={erroSenha} />}
              {sucessoSenha && <SuccessMessage mensagem={sucessoSenha} aoFechar={() => setSucessoSenha("")} />}
              <div className="form-actions">
                <button className="button button-primary" disabled={salvandoSenha}>
                  {salvandoSenha ? "Alterando…" : "Alterar senha"}
                </button>
              </div>
            </form>
          </section>
        </div>
      </div>

      {editorImagemAberto && (
        <ProfileImageEditor
          imagemAtualUrl={imagemPerfilUrl}
          possuiImagem={usuario.temImagem}
          nomeUsuario={usuario.nome}
          salvando={salvandoImagem}
          erro={erroImagem}
          aoFechar={() => {
            if (!salvandoImagem) setEditorImagemAberto(false);
          }}
          aoErro={setErroImagem}
          aoSalvar={enviarImagem}
          aoExcluir={excluirImagem}
        />
      )}
    </div>
  );
}
