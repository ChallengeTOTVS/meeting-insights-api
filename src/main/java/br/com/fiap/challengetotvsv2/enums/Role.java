package br.com.fiap.challengetotvsv2.enums;

/**
 * Perfis de acesso.
 * ADMIN: gestor/administrador; vê a equipe inteira e gerencia usuários.
 * FUNCIONARIO: vendedor; cadastra clientes e reuniões e gera análises.
 * CONSULTA: pessoas de outras áreas (marketing, produto, diretoria...);
 *           só usam o assistente (chatbot), sem acesso às telas do sistema.
 */
public enum Role {
    ADMIN,
    FUNCIONARIO,
    CONSULTA
}
