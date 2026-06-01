package project.modal.request;

public record CadastroRequest(String username,
                              String email,
                              String password,
                              String nationality,
                              String language) {}
