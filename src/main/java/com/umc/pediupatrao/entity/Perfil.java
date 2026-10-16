package com.umc.pediupatrao.entity;

public enum Perfil {
    ADMIN(0),
    GERENTE(20),
    ATENDENTE(0);

    private final int limiteDesconto;

    Perfil(int limiteDesconto) {
        this.limiteDesconto = limiteDesconto;
    }

    public int getLimiteDesconto() {
        return limiteDesconto;
    }
}
