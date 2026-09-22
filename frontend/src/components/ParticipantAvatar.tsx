import { useEffect, useMemo, useState } from "react";
import { participantesApi } from "../services/qualityApi";
import type { Participante } from "../types/api";

interface ParticipantAvatarProps {
  planoId: string;
  participante: Participante;
}

function obterIniciais(nome: string) {
  return nome
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0]?.toUpperCase())
    .join("");
}

export function ParticipantAvatar({ planoId, participante }: ParticipantAvatarProps) {
  const [imagemUrl, setImagemUrl] = useState("");
  const iniciais = useMemo(() => obterIniciais(participante.nome), [participante.nome]);

  useEffect(() => {
    let ativo = true;
    let url = "";
    setImagemUrl("");

    if (!participante.temImagem) {
      return () => undefined;
    }

    void participantesApi.buscarImagem(planoId, participante.id)
      .then((imagem) => {
        url = URL.createObjectURL(imagem);
        if (ativo) {
          setImagemUrl(url);
        } else {
          URL.revokeObjectURL(url);
        }
      })
      .catch(() => {
        if (ativo) setImagemUrl("");
      });

    return () => {
      ativo = false;
      if (url) URL.revokeObjectURL(url);
    };
  }, [participante.id, participante.temImagem, planoId]);

  return (
    <span className="participant-avatar" aria-hidden="true">
      {imagemUrl ? <img src={imagemUrl} alt="" /> : iniciais}
    </span>
  );
}
