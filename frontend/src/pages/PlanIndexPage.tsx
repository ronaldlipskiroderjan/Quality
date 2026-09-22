import { useOutletContext } from "react-router-dom";
import type { PlanOutletContext } from "../layouts/PlanLayout";
import { PlanOverviewPage } from "./PlanOverviewPage";
import { PlanResolutionInboxPage } from "./PlanResolutionInboxPage";

export function PlanIndexPage() {
  const { plano } = useOutletContext<PlanOutletContext>();

  if (plano.meusPapeis.includes("MEMBRO_EQUIPE_RESOLUCAO")) {
    return <PlanResolutionInboxPage plano={plano} />;
  }

  return <PlanOverviewPage />;
}
