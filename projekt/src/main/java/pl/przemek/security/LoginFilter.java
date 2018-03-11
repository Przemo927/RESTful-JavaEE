package pl.przemek.security;


import pl.przemek.model.User;
import pl.przemek.repository.JpaUserRepository;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Key;
import java.util.List;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LOCATION;

@Provider
public class LoginFilter implements ContainerRequestFilter {

    @Inject
    private HttpServletRequest request;
    @Context
    private HttpServletResponse response;
    @Inject
    private JpaUserRepository userrep;
    @Inject
    private TokenService tokenService;
    @Inject
    private TokenStore tokenStore;
    @Inject
    private AuthenticationDataStore userDataStore;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
    	if(request.getSession()!=null){
    	if(request.getSession(false).getAttribute("user")!=null){
        	System.out.println(request.getSession().getCreationTime()+" 2");
        	String token=requestContext.getHeaderString(AUTHORIZATION);
            checkToken(token,userDataStore.getEncryptedPassword());
			saveToken(userDataStore.getUsername(),userDataStore.getEncryptedPassword());
        }

    	//if(request.getSession()!=null){
        if(requestContext.getSecurityContext().getUserPrincipal() != null && request.getSession(false).getAttribute("user") == null) {
        	System.out.println(request.getSession().getCreationTime()+" 1");
            String username = requestContext.getSecurityContext().getUserPrincipal().getName();
            List<User> listUserByUsername = userrep.getUserByUsername(username);
            User userByUsername=listUserByUsername.get(0);
            saveToken(userByUsername.getUsername(),userByUsername.getPassword());
            saveUserData(userByUsername);
            try {
                LogoutIfInActiveStatus(userByUsername,request);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            saveUserInSession(request,userByUsername);

        }
    	}

    }
    void LogoutIfInActiveStatus(User user, HttpServletRequest request) throws IOException, URISyntaxException {
        if(!user.isActive()){
            request.getSession().invalidate();
			Response.seeOther(new URI("http://localhost:8080/projekt/index.html#/"));
        }
    }
    void saveUserInSession(HttpServletRequest request,User user) {

        request.getSession(false).setAttribute("user", user);
    }
    void saveToken(String username,String password){
    	String token=tokenService.generateToken(username, password);
        tokenStore.setToken(token);
        System.out.println("Generated new token "+token);
    }
    void saveUserData(User user){
    	userDataStore.setUsername(user.getUsername());
        userDataStore.setEncryptedPassword(user.getPassword());
    }
    void checkToken(String token,String encryptedPassword){

        try {
            Key key=tokenService.generateKey(encryptedPassword);
            Jwts.parser().setSigningKey(key).parseClaimsJws(token);
        } catch (Exception e) {
            System.out.println("Exception");
            try {
                request.logout();
            } catch (ServletException e1) {
                e1.printStackTrace();
            }

            request.getSession().invalidate();
        }

    }
}
