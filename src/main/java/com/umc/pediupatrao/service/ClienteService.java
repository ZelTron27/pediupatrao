package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.EventoAuditoria.CampoAlterado;
import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Service
public class ClienteService {

    private static final Map<String, Function<Cliente, String>> CAMPOS_AUDITADOS = new LinkedHashMap<>();

    static {
        CAMPOS_AUDITADOS.put("nome", Cliente::getNome);
        CAMPOS_AUDITADOS.put("telefone", Cliente::getTelefone);
        CAMPOS_AUDITADOS.put("cep", Cliente::getCep);
        CAMPOS_AUDITADOS.put("logradouro", Cliente::getLogradouro);
        CAMPOS_AUDITADOS.put("numero", Cliente::getNumero);
        CAMPOS_AUDITADOS.put("complemento", Cliente::getComplemento);
        CAMPOS_AUDITADOS.put("bairro", Cliente::getBairro);
        CAMPOS_AUDITADOS.put("cidade", Cliente::getCidade);
        CAMPOS_AUDITADOS.put("estado", Cliente::getEstado);
        CAMPOS_AUDITADOS.put("endereco", Cliente::getEndereco);
    }

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TrilhaAuditoria trilhaAuditoria;

    public Cliente novoCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Método para excluir cliente
    public void excluir(String id) {
        clienteRepository.deleteById(id);
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    public Cliente salvar(Cliente cliente) {
        // Se o cliente não tem ID (novo cliente), salva como novo
        if (cliente.getId() == null) {
            Cliente salvo = clienteRepository.save(cliente);  // Cria um novo cliente
            trilhaAuditoria.registrar(TipoEvento.INCLUSAO_CLIENTE, "Cliente", salvo.getId(), null,
                    diferencas(new Cliente(), salvo));
            return salvo;
        } // Se já tem ID (cliente existente), atualiza
        else {
            // Verifica se o cliente existe antes de atualizar
            Cliente anterior = clienteRepository.findById(cliente.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para atualização."));
            Cliente salvo = clienteRepository.save(cliente);  // Atualiza o cliente existente
            List<CampoAlterado> campos = diferencas(anterior, salvo);
            if (!campos.isEmpty()) {
                trilhaAuditoria.registrar(TipoEvento.ALTERACAO_CLIENTE, "Cliente", salvo.getId(), null, campos);
            }
            return salvo;
        }
    }

    private List<CampoAlterado> diferencas(Cliente antes, Cliente depois) {
        List<CampoAlterado> campos = new ArrayList<>();
        CAMPOS_AUDITADOS.forEach((nome, leitor) -> {
            String anterior = semVazio(leitor.apply(antes));
            String novo = semVazio(leitor.apply(depois));
            if (!Objects.equals(anterior, novo)) {
                campos.add(new CampoAlterado(nome, anterior, novo));
            }
        });
        return campos;
    }

    private String semVazio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

}
