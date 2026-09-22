import { Component, type ErrorInfo, type ReactNode } from "react";

interface AppErrorBoundaryProps {
  children: ReactNode;
}

interface AppErrorBoundaryState {
  falhou: boolean;
}

export class AppErrorBoundary extends Component<
  AppErrorBoundaryProps,
  AppErrorBoundaryState
> {
  state: AppErrorBoundaryState = { falhou: false };

  static getDerivedStateFromError(): AppErrorBoundaryState {
    return { falhou: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("Falha ao renderizar a aplicação.", error, info);
  }

  render() {
    if (this.state.falhou) {
      return (
        <main className="fatal-error" role="alert">
          <h1>Não foi possível exibir esta tela</h1>
          <p>Recarregue pelo navegador para tentar novamente.</p>
        </main>
      );
    }

    return this.props.children;
  }
}
