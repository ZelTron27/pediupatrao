package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "auditoria")
public class EventoAuditoria {

    @Id
    private String id;
    private String usuario;
    private Perfil perfil;
    private LocalDateTime dataHora;
    private TipoEvento tipo;
    private String entidade;
    private String entidadeId;
    private List<CampoAlterado> campos;
    private String justificativa;

    public record CampoAlterado(String campo, String anterior, String novo) {

        public static CampoAlterado de(String campo, Object anterior, Object novo) {
            return new CampoAlterado(campo, Objects.toString(anterior, null), Objects.toString(novo, null));
        }
    }
}
