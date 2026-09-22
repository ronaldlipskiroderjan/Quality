import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { LoadingState } from "../components/Feedback";

export function ProtectedRoute() {
  const { autenticado, carregando } = useAuth();
  const location = useLocation();

  if (carregando) return <LoadingState mensagem="Validando sessão..." />;
  if (!autenticado) return <Navigate to="/entrar" replace state={{ de: location.pathname }} />;
  return <Outlet />;
}

export function PublicOnlyRoute() {
  const { autenticado, carregando } = useAuth();
  if (carregando) return <LoadingState mensagem="Validando sessão..." />;
  if (autenticado) return <Navigate to="/dashboard" replace />;
  return <Outlet />;
}
