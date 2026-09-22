import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute, PublicOnlyRoute } from "./auth/ProtectedRoute";
import { AppLayout } from "./layouts/AppLayout";
import { PlanLayout } from "./layouts/PlanLayout";
import { ArtifactsPage } from "./pages/ArtifactsPage";
import { AuditExecutionPage } from "./pages/AuditExecutionPage";
import { AuditsPage } from "./pages/AuditsPage";
import { DocumentsPage } from "./pages/DocumentsPage";
import { DashboardPage } from "./pages/DashboardPage";
import { LoginPage } from "./pages/LoginPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { NonConformitiesPage } from "./pages/NonConformitiesPage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { ParticipantsPage } from "./pages/ParticipantsPage";
import { PlanIndexPage } from "./pages/PlanIndexPage";
import { PlanNonConformityPage } from "./pages/PlanNonConformityPage";
import { RegisterPage } from "./pages/RegisterPage";
import { EscalationsPage } from "./pages/EscalationsPage";
import { ProfilePage } from "./pages/ProfilePage";
import { HomePage } from "./pages/HomePage";

export function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />

      <Route element={<PublicOnlyRoute />}>
        <Route path="/entrar" element={<LoginPage />} />
        <Route path="/cadastro" element={<RegisterPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/perfil" element={<ProfilePage />} />
          <Route path="/planos" element={<Navigate to="/dashboard" replace />} />
          <Route
            path="/planos/novo"
            element={<Navigate to="/dashboard" replace state={{ abrirNovoPlano: true }} />}
          />
          <Route path="/planos/:planoId" element={<PlanLayout />}>
            <Route index element={<PlanIndexPage />} />
            <Route path="equipe" element={<ParticipantsPage />} />
            <Route path="escalonamentos" element={<EscalationsPage />} />
            <Route path="documentos" element={<DocumentsPage />} />
            <Route path="artefatos" element={<ArtifactsPage />} />
            <Route path="artefatos/:artefatoId/auditorias" element={<AuditsPage />} />
            <Route path="artefatos/:artefatoId/auditorias/:auditoriaId" element={<AuditExecutionPage />} />
            <Route path="nao-conformidades" element={<NonConformitiesPage />} />
            <Route path="nao-conformidades/:naoConformidadeId" element={<PlanNonConformityPage />} />
          </Route>
          <Route path="/notificacoes" element={<NotificationsPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/entrar" replace />} />
    </Routes>
  );
}
