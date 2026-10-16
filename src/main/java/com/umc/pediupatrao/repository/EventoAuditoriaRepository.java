package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.EventoAuditoria;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface EventoAuditoriaRepository extends Repository<EventoAuditoria, String> {

    <S extends EventoAuditoria> S save(S evento);

    List<EventoAuditoria> findAllByOrderByDataHoraDesc();
}
