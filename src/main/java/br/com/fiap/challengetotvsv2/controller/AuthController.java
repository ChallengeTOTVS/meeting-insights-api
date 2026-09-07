package br.com.fiap.challengetotvsv2.controller;

import br.com.fiap.challengetotvsv2.dto.auth.LoginRequestDto;
import br.com.fiap.challengetotvsv2.dto.auth.LoginResponseDto;
import br.com.fiap.challengetotvsv2.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {

        return authService.autenticar(request);
    }
}
