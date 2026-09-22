interface AuthWelcomePanelProps {
  tituloId: string;
  titulo: string;
  descricao: string;
}

export function AuthWelcomePanel({ tituloId, titulo, descricao }: AuthWelcomePanelProps) {
  return (
    <aside className="login-welcome" aria-labelledby={tituloId}>
      <div className="login-welcome-lines" aria-hidden="true">
        <span />
        <span />
        <span />
      </div>

      <span className="brand login-brand">
        <span className="brand-mark" aria-hidden="true">
          <i />
          <i />
          <i />
        </span>
        <span>Quality</span>
      </span>

      <div className="login-welcome-copy">
        <h1 id={tituloId}>{titulo}</h1>
        <p>{descricao}</p>
      </div>
    </aside>
  );
}
