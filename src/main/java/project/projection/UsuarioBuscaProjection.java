package project.projection;

public interface UsuarioBuscaProjection {
    Long getId();
    String getUsername();
    byte[] getAvatarData();
    String getNacionalidade();
    Boolean getIsVerified();
}
