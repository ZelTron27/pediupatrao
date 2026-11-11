package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.StatusPedido;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record FiltroPainel(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
        StatusPedido status) {

    public static final FiltroPainel NENHUM = new FiltroPainel(null, null, null);

    public boolean temPeriodo() {
        return inicio != null || fim != null;
    }
}
