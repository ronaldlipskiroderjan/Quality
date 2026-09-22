import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <main className="not-found">
      <h1>Página não encontrada</h1>
      <p>O endereço informado não existe ou não está mais disponível.</p>
      <Link className="button button-primary" to="/dashboard">Voltar à dashboard</Link>
    </main>
  );
}
