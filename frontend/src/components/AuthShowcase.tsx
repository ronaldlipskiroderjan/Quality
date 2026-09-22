interface AuthShowcaseProps {
  titulo: string;
  descricao: string;
}

export function AuthShowcase({ titulo, descricao }: AuthShowcaseProps) {
  return (
    <div className="auth-intro">
      <div>
        <span className="brand auth-brand">
          <span className="brand-mark" aria-hidden="true"><i /><i /><i /></span>
          <span>Quality</span>
        </span>
        <h1>{titulo}</h1>
        <p>{descricao}</p>
      </div>
      <div className="auth-product-preview" aria-hidden="true">
        <div className="preview-toolbar">
          <span /><span /><span />
          <small>Plano de Garantia da Qualidade</small>
        </div>
        <div className="preview-body">
          <div className="preview-heading"><span /><span /></div>
          <div className="preview-grid">
            <div className="preview-card preview-card-wide"><span /><span /><span /></div>
            <div className="preview-card"><span /><span /></div>
            <div className="preview-card"><span /><span /></div>
          </div>
        </div>
      </div>
    </div>
  );
}
