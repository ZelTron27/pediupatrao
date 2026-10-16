package com.umc.pediupatrao.seguranca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.EventoAuditoria;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.EventoAuditoriaRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class AutorizacaoIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private PedidoRepository pedidoRepository;

    @MockitoBean
    private ClienteRepository clienteRepository;

    @MockitoBean
    private ProdutoRepository produtoRepository;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    private static RequestPostProcessor como(String nome, String perfil) {
        return user(nome).roles(perfil);
    }

    private static final RequestPostProcessor ADMIN = como("admin", "ADMIN");
    private static final RequestPostProcessor GERENTE = como("gerente", "GERENTE");
    private static final RequestPostProcessor ATENDENTE = como("atendente01", "ATENDENTE");

    @BeforeEach
    void prepararPedido() {
        Pedido pedido = new Pedido();
        pedido.setId("p1");
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setSubtotal(200.0);
        pedido.setPercentualDesconto(0.0);
        pedido.setValorDesconto(0.0);
        pedido.setValorTotal(200.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private EventoAuditoria eventoGravado() {
        ArgumentCaptor<EventoAuditoria> captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(eventoAuditoriaRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void visitanteSemLoginEhRedirecionadoParaOLogin() throws Exception {
        mvc.perform(get("/api/pedidos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/auditoria"))
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void requisicaoDeEscritaSemTokenCsrfEhRecusada() throws Exception {
        mvc.perform(post("/api/pedidos").with(ATENDENTE)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminNaoConsegueAplicarDescontoNemCancelar() throws Exception {
        mvc.perform(patch("/api/pedidos/p1/desconto").with(ADMIN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"percentual\":10}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/pedidos/p1/desconto").with(ADMIN).with(csrf()).param("percentual", "10"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/pedidos/p1/status").with(ADMIN).with(csrf())
                        .param("status", "CANCELADO").param("justificativa", "x"))
                .andExpect(status().isForbidden());

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void atendenteNaoConsegueAplicarDescontoNemCancelarPelaApi() throws Exception {
        mvc.perform(patch("/api/pedidos/p1/desconto").with(ATENDENTE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"percentual\":10}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/pedidos/p1/status").with(ATENDENTE).with(csrf())
                        .param("status", "CANCELADO").param("justificativa", "x"))
                .andExpect(status().isForbidden());

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void atendenteNaoConsegueAplicarDescontoNemCancelarPeloFormulario() throws Exception {
        mvc.perform(post("/pedidos/p1/desconto").with(ATENDENTE).with(csrf()).param("percentual", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erro", "Somente o GERENTE pode conceder desconto."));
        mvc.perform(post("/pedidos/p1/status").with(ATENDENTE).with(csrf())
                        .param("status", "CANCELADO").param("justificativa", "x"))
                .andExpect(flash().attribute("erro", "Somente o GERENTE pode cancelar pedidos."));

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void atendentePodeAvancarStatusDoPedido() throws Exception {
        mvc.perform(put("/api/pedidos/p1/status").with(ATENDENTE).with(csrf())
                        .param("status", "EM_PREPARACAO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_PREPARACAO"));
    }

    @Test
    void gerenteCancelaComJustificativaEAuditoriaRegistraOperacao() throws Exception {
        mvc.perform(put("/api/pedidos/p1/status").with(GERENTE).with(csrf())
                        .param("status", "CANCELADO").param("justificativa", "cliente desistiu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.justificativaCancelamento").value("cliente desistiu"));

        EventoAuditoria evento = eventoGravado();
        assertEquals(TipoEvento.CANCELAMENTO, evento.getTipo());
        assertEquals("gerente", evento.getUsuario());
        assertEquals(Perfil.GERENTE, evento.getPerfil());
        assertEquals("p1", evento.getEntidadeId());
        assertEquals("cliente desistiu", evento.getJustificativa());
        assertEquals("RECEBIDO", evento.getCampos().get(0).anterior());
        assertEquals("CANCELADO", evento.getCampos().get(0).novo());
    }

    @Test
    void gerenteNaoCancelaSemJustificativa() throws Exception {
        mvc.perform(post("/pedidos/p1/status").with(GERENTE).with(csrf()).param("status", "CANCELADO"))
                .andExpect(flash().attribute("erro", "Informe a justificativa do cancelamento."));

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void gerenteAplicaDescontoDentroDoLimiteEAuditoriaGuardaValoresAnterioresENovos() throws Exception {
        mvc.perform(patch("/api/pedidos/p1/desconto").with(GERENTE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"percentual\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorDesconto").value(30.0))
                .andExpect(jsonPath("$.valorTotal").value(170.0));

        EventoAuditoria evento = eventoGravado();
        assertEquals(TipoEvento.DESCONTO, evento.getTipo());
        assertEquals("gerente", evento.getUsuario());
        assertEquals("200.0", evento.getCampos().stream()
                .filter(c -> c.campo().equals("valorTotal")).findFirst().orElseThrow().anterior());
        assertEquals("170.0", evento.getCampos().stream()
                .filter(c -> c.campo().equals("valorTotal")).findFirst().orElseThrow().novo());
    }

    @Test
    void gerenteNaoUltrapassaLimiteDeVintePorCentoDeDesconto() throws Exception {
        mvc.perform(post("/pedidos/p1/desconto").with(GERENTE).with(csrf()).param("percentual", "21"))
                .andExpect(flash().attribute("erro", "O desconto deve ficar entre 0% e 20%."));

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void pedidoCriadoIgnoraTotalEnviadoNaRequisicao() throws Exception {
        when(clienteRepository.existsById("c1")).thenReturn(true);
        Produto produto = new Produto();
        produto.setId("pr1");
        produto.setNome("Calabresa");
        produto.setPreco(40.0);
        produto.setAtivo(true);
        when(produtoRepository.findById("pr1")).thenReturn(Optional.of(produto));

        mvc.perform(post("/api/pedidos").with(ATENDENTE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":\"c1\",\"valorTotal\":1.0,\"valorDesconto\":39.0,"
                                + "\"status\":\"FINALIZADO\","
                                + "\"itens\":[{\"produtoId\":\"pr1\",\"quantidade\":2,\"precoUnitario\":0.01}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(80.0))
                .andExpect(jsonPath("$.valorDesconto").value(0.0))
                .andExpect(jsonPath("$.status").value("RECEBIDO"))
                .andExpect(jsonPath("$.responsavel").value("atendente01"));

        assertEquals(TipoEvento.INCLUSAO_PEDIDO, eventoGravado().getTipo());
    }

    @Test
    void apenasGerenteEAtendenteMexemEmPedidosEAdminSoConsulta() throws Exception {
        mvc.perform(get("/api/pedidos").with(ADMIN)).andExpect(status().isOk());
        mvc.perform(post("/api/pedidos").with(ADMIN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiDeUsuariosEhExclusivaDoAdmin() throws Exception {
        mvc.perform(get("/api/usuarios").with(GERENTE)).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").with(ATENDENTE)).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").with(ADMIN)).andExpect(status().isOk());
        mvc.perform(post("/usuarios/salvar").with(GERENTE).with(csrf())
                        .param("username", "novo").param("password", "1").param("perfil", "ADMIN"))
                .andExpect(status().isForbidden());

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void adminNaoConsegueAlterarOProprioPerfil() throws Exception {
        Usuario existente = new Usuario();
        existente.setId("u1");
        existente.setUsername("admin");
        existente.setPerfil(Perfil.ADMIN);
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(existente));
        when(usuarioRepository.countByPerfil(Perfil.ADMIN)).thenReturn(2L);

        mvc.perform(post("/usuarios/editar/u1").with(ADMIN).with(csrf())
                        .param("username", "admin").param("perfil", "ATENDENTE"))
                .andExpect(flash().attribute("erro", "Você não pode alterar o seu próprio perfil."));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void senhaDoUsuarioNovoEhGravadaCriptografada() throws Exception {
        when(usuarioRepository.existsByUsername("novo")).thenReturn(false);

        mvc.perform(post("/usuarios/salvar").with(ADMIN).with(csrf())
                        .param("username", "novo").param("password", "segredo").param("perfil", "ATENDENTE"))
                .andExpect(flash().attributeExists("sucesso"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertNotEquals("segredo", captor.getValue().getPassword());
        assertEquals(true, captor.getValue().getPassword().startsWith("$2"));
    }

    @Test
    void consultaDeClientesNaoEstaDisponivelParaAdmin() throws Exception {
        mvc.perform(get("/api/clientes").with(ADMIN)).andExpect(status().isForbidden());
        mvc.perform(get("/api/clientes").with(GERENTE)).andExpect(status().isOk());
        mvc.perform(get("/api/clientes").with(ATENDENTE)).andExpect(status().isOk());
    }

    @Test
    void somenteAtendenteCadastraEAlteraClientes() throws Exception {
        mvc.perform(post("/api/clientes").with(GERENTE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Ana\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/clientes").with(ATENDENTE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Ana\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void alteracaoDeEnderecoDoClienteGeraAuditoriaComValorAnteriorENovo() throws Exception {
        Cliente anterior = new Cliente();
        anterior.setId("c1");
        anterior.setNome("Ana");
        anterior.setLogradouro("Rua das Flores");
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(anterior));

        mvc.perform(put("/api/clientes/c1").with(ATENDENTE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"logradouro\":\"Av. Brasil\"}"))
                .andExpect(status().isOk());

        EventoAuditoria evento = eventoGravado();
        assertEquals(TipoEvento.ALTERACAO_CLIENTE, evento.getTipo());
        assertEquals("atendente01", evento.getUsuario());
        assertEquals(Perfil.ATENDENTE, evento.getPerfil());
        assertEquals("c1", evento.getEntidadeId());
        assertEquals(1, evento.getCampos().size());
        assertEquals("logradouro", evento.getCampos().get(0).campo());
        assertEquals("Rua das Flores", evento.getCampos().get(0).anterior());
        assertEquals("Av. Brasil", evento.getCampos().get(0).novo());
    }

    @Test
    void consultaDaAuditoriaEhRestritaAAdminEGerente() throws Exception {
        when(eventoAuditoriaRepository.findAllByOrderByDataHoraDesc()).thenReturn(List.of());

        mvc.perform(get("/auditoria").with(ATENDENTE)).andExpect(status().isForbidden());
        mvc.perform(get("/auditoria").with(ADMIN)).andExpect(status().isOk());
        mvc.perform(get("/auditoria").with(GERENTE)).andExpect(status().isOk());
    }

    @Test
    void registrosDeAuditoriaNaoPodemSerAlteradosNemExcluidos() throws Exception {
        for (RequestPostProcessor perfil : List.of(ADMIN, GERENTE, ATENDENTE)) {
            mvc.perform(post("/auditoria/e1").with(perfil).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(put("/auditoria/e1").with(perfil).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(patch("/auditoria/e1").with(perfil).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(delete("/auditoria/e1").with(perfil).with(csrf())).andExpect(status().isForbidden());
        }

        verify(eventoAuditoriaRepository, never()).save(any());
    }
}
