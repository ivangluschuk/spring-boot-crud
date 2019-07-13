package crud.security.oauth;

import crud.model.User;
import crud.repository.UserRepository;
import crud.security.UserPrincipal;
import crud.security.oauth.user.OAuth2UserInfo;
import crud.security.oauth.user.OAuth2UserInfoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        var oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) throws AuthenticationException {
        var oAuth2UserInfo = OAuth2UserInfoFactory
                .getOAuth2UserInfo(oAuth2UserRequest.getClientRegistration().getRegistrationId(),
                        oAuth2User.getAttributes());

        Optional<User> userOptional = userRepository.findById(Long.parseLong(oAuth2UserInfo.getId()));
        User user;

        if (userOptional.isEmpty()) {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        } else {
            user = userRepository.findById(Long.parseLong(oAuth2UserInfo.getId())).get();
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
       var user = new User();

       user.setId(Long.parseLong(oAuth2UserInfo.getId()));
       user.setUsername(oAuth2UserInfo.getName());
       user.setActive(true);

       return  userRepository.save(user);
    }
}
