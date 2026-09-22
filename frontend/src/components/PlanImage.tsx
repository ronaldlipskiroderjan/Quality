import { useEffect, useState, type ReactNode } from "react";
import { planosApi } from "../services/qualityApi";
import type { UUID } from "../types/api";

interface PlanImageProps {
  planoId: UUID;
  alt: string;
  className?: string;
  fallback?: ReactNode;
  revisao?: number;
}

export function PlanImage({ planoId, alt, className, fallback = null, revisao = 0 }: PlanImageProps) {
  const [url, setUrl] = useState("");

  useEffect(() => {
    let ativo = true;
    let objetoUrl = "";

    planosApi.buscarImagem(planoId)
      .then((imagem) => {
        if (!ativo) return;
        objetoUrl = URL.createObjectURL(imagem);
        setUrl(objetoUrl);
      })
      .catch(() => {
        if (ativo) setUrl("");
      });

    return () => {
      ativo = false;
      if (objetoUrl) URL.revokeObjectURL(objetoUrl);
    };
  }, [planoId, revisao]);

  return url ? <img className={className} src={url} alt={alt} /> : fallback;
}
