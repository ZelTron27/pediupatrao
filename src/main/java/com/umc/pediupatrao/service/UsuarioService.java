package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Busca um usuário por ID
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    @PreAuthorize("hasRole('ADMIN')")
    public void salvarUsuario(Usuario usuario) {
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()
                || usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new RegraDaCasaException("Informe usuário e senha.");
        }
        if (usuario.getPerfil() == null) {
            throw new RegraDaCasaException("Selecione o perfil do usuário.");
        }
        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            throw new RegraDaCasaException("Já existe um usuário com esse nome.");
        }
        usuario.setId(null);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Criptografa a senha
        usuarioRepository.save(usuario);
    }

    // Deleta um usuário pelo ID
    @PreAuthorize("hasRole('ADMIN')")
    public void deletarUsuario(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraDaCasaException("Usuário não encontrado."));
        if (usuario.getUsername().equals(usuarioLogado())) {
            throw new RegraDaCasaException("Você não pode excluir o seu próprio usuário.");
        }
        impedirRemocaoDoUltimoAdmin(usuario);
        usuarioRepository.deleteById(id);
    }

    // Lista todos os usuários
    @PreAuthorize("hasRole('ADMIN')")
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário usuários
    @PreAuthorize("hasRole('ADMIN')")
    public void atualizarUsuario(String id, Usuario usuarioAtualizado) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraDaCasaException("Usuário não encontrado."));

        if (usuarioAtualizado.getUsername() == null || usuarioAtualizado.getUsername().isBlank()) {
            throw new RegraDaCasaException("Informe o usuário.");
        }
        if (!usuarioExistente.getUsername().equals(usuarioAtualizado.getUsername())
                && usuarioRepository.existsByUsername(usuarioAtualizado.getUsername())) {
            throw new RegraDaCasaException("Já existe um usuário com esse nome.");
        }

        Perfil novoPerfil = usuarioAtualizado.getPerfil() != null
                ? usuarioAtualizado.getPerfil()
                : usuarioExistente.getPerfil();
        if (novoPerfil != usuarioExistente.getPerfil()) {
            if (usuarioExistente.getUsername().equals(usuarioLogado())) {
                throw new RegraDaCasaException("Você não pode alterar o seu próprio perfil.");
            }
            impedirRemocaoDoUltimoAdmin(usuarioExistente);
        }

        usuarioExistente.setUsername(usuarioAtualizado.getUsername());
        usuarioExistente.setPerfil(novoPerfil);

        // Só atualiza a senha se uma nova foi informada
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
            usuarioExistente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }

        usuarioRepository.save(usuarioExistente);
    }

    private void impedirRemocaoDoUltimoAdmin(Usuario usuario) {
        if (usuario.getPerfil() == Perfil.ADMIN && usuarioRepository.countByPerfil(Perfil.ADMIN) <= 1) {
            throw new RegraDaCasaException("O sistema precisa ter pelo menos um ADMIN.");
        }
    }

    private String usuarioLogado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
