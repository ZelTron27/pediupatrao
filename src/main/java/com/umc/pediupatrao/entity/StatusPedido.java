package com.umc.pediupatrao.entity;

import java.util.List;

public enum StatusPedido {
    RECEBIDO,
    EM_PREPARACAO,
    PRONTO,
    SAIU_PARA_ENTREGA,
    RETIRADO,
    FINALIZADO,
    CANCELADO;

    public List<StatusPedido> proximos() {
        return switch (this) {
            case RECEBIDO -> List.of(EM_PREPARACAO, CANCELADO);
            case EM_PREPARACAO -> List.of(PRONTO, CANCELADO);
            case PRONTO -> List.of(SAIU_PARA_ENTREGA, RETIRADO, CANCELADO);
            case SAIU_PARA_ENTREGA, RETIRADO -> List.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> List.of();
        };
    }
}
