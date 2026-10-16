package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.EventoAuditoria;
import com.umc.pediupatrao.entity.EventoAuditoria.CampoAlterado;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.repository.EventoAuditoriaRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TrilhaAuditoria {

    @Autowired
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    public void registrar(TipoEvento tipo, String entidade, String entidadeId,
            String justificativa, List<CampoAlterado> campos) {
        Authentication usuario = SecurityContextHolder.getContext().getAuthentication();

        EventoAuditoria evento = new EventoAuditoria();
        evento.setUsuario(usuario.getName());
        evento.setPerfil(usuario.getAuthorities().stream()
                .map(a -> Perfil.valueOf(a.getAuthority().replace("ROLE_", "")))
                .findFirst()
                .orElse(null));
        evento.setDataHora(LocalDateTime.now());
        evento.setTipo(tipo);
        evento.setEntidade(entidade);
        evento.setEntidadeId(entidadeId);
        evento.setJustificativa(justificativa);
        evento.setCampos(campos);
        eventoAuditoriaRepository.save(evento);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public List<EventoAuditoria> consultar() {
        return eventoAuditoriaRepository.findAllByOrderByDataHoraDesc();
    }
}
