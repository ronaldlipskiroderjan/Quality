import { useOutletContext } from "react-router-dom";
import type { PlanOutletContext } from "../layouts/PlanLayout";
import { NonConformityPage } from "./NonConformityPage";
import { ResolutionTaskPage } from "./ResolutionTaskPage";

export function PlanNonConformityPage() {
  const { plano } = useOutletContext<PlanOutletContext>();

  return plano.meusPapeis.includes("MEMBRO_EQUIPE_RESOLUCAO")
    ? <ResolutionTaskPage />
    : <NonConformityPage />;
}
