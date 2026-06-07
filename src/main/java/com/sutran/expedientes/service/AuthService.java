package com.sutran.expedientes.service;

import com.sutran.expedientes.dto.LoginRequest;
import com.sutran.expedientes.dto.LoginResponse;
import com.sutran.expedientes.entity.Usuario;
import com.sutran.expedientes.repository.UsuarioRepository;
import com.sutran.expedientes.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        // Si las credenciales son inválidas, lanza BadCredentialsException
        // (capturada por el GlobalExceptionHandler -> 401)
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usuario(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generarToken(userDetails);

        Usuario usuario = usuarioRepository.findByUsuarioAndEstado(request.usuario(), "A").orElseThrow();

        return new LoginResponse(token, usuario.getUsuario(), usuario.getNombres(),
                usuario.getApellidos(), usuario.getRol(),
                usuario.getArea() != null ? usuario.getArea().getIdArea() : null,
                usuario.getArea() != null ? usuario.getArea().getNombreArea() : null);
    }
}
