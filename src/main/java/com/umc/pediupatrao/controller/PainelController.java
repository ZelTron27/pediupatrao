package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.FiltroPainel;
import com.umc.pediupatrao.dto.IndicadoresVenda;
import com.umc.pediupatrao.dto.ProdutoNoRanking;
import com.umc.pediupatrao.dto.VendaDoDia;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.PainelService;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel")
public class PainelController {

    @Autowired
    private PainelService painelService;

    @GetMapping("/resumo")
    public IndicadoresVenda resumo(FiltroPainel filtro) {
        return painelService.indicadores(filtro);
    }

    @GetMapping("/vendas")
    public List<VendaDoDia> vendas(FiltroPainel filtro) {
        return painelService.vendasPorDia(filtro);
    }

    @GetMapping("/status")
    public Map<StatusPedido, Long> status(FiltroPainel filtro) {
        return painelService.pedidosPorStatus(filtro);
    }

    @GetMapping("/produtos")
    public List<ProdutoNoRanking> produtos(FiltroPainel filtro, @RequestParam(defaultValue = "5") int limite) {
        return painelService.maisVendidos(filtro, limite);
    }

    @GetMapping("/movimento")
    public Map<DayOfWeek, long[]> movimento(FiltroPainel filtro) {
        return painelService.movimentoPorHorario(filtro);
    }
}
