package com.cargosetu.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.dto.LoginRequest;
import com.cargosetu.config.dto.TokenResponse;
import com.cargosetu.config.JwtUtils;
import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.model.Permission;
import com.cargosetu.model.Staff;
import com.cargosetu.repo.StaffRepository;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private static final String EMAIL_REGEX =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

    private static final Pattern PATTERN = Pattern.compile(EMAIL_REGEX);
    private final StaffRepository staffRepository;
    @Autowired
    private JwtUtils jwtUtils;

    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(StaffRepository staffRepository, BCryptPasswordEncoder bCryptPasswordEncoder, JwtUtils jwtUtils) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = bCryptPasswordEncoder;
    }

    public static boolean isValidEmail(String email) {
        Matcher matcher = PATTERN.matcher(email);
        return matcher.matches();
    }

    public boolean isValidRequest(LoginRequest loginDto){
        if(loginDto == null){
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(),"Login Request cannot be null");
        }
        if(!StringUtils.hasText(loginDto.getEmail())){
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(),"please enter valid email!!");
        }
        if(loginDto.getPassword() == null || loginDto.getPassword().trim().isEmpty()){
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(),"please enter valid password!!");
        }
//        if(  loginDto.getEmail() !=null && !loginDto.getEmail().isEmpty()  ){
//            boolean validEmail = isValidEmail(loginDto.getEmail());
//            if(!validEmail){
//                throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(),"email address is invalid");
//            }
//        }
        return true;
    }

    public TokenResponse loginUser(LoginRequest loginDto) {
        Staff staff = null;

         if(loginDto.getEmail() != null){
            staff = loginWithEmailAndPassword(loginDto.getEmail(),loginDto.getPassword());
        }
        if(staff == null){
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(),"User not found,try with correct credentials!!");
        }

        // extracting staff roles
        Set<String> staffRoles = staff.getRoles().stream().map(role -> role.getCode()).collect(Collectors.toSet());

        Set<String> staffPermissions =
                staff.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .map(Permission::getCode)
                        .collect(Collectors.toSet());

        LoggedInUser loggedInUser = new LoggedInUser();
        loggedInUser.setUserName(staff.getUserName());
        loggedInUser.setEmail(staff.getEmail());
        loggedInUser.setRoles(staffRoles);
        loggedInUser.setPermissions(staffPermissions);
        loggedInUser.setUuid(staff.getUuid());
        loggedInUser.setStaffId(staff.getStaffId());
        loggedInUser.setFirstName(staff.getFirstName());
        loggedInUser.setLastName(staff.getLastName());
        loggedInUser.setCompanyId(staff.getCompanyId());

        // generating jwt token
        String jwtToken = jwtUtils.generateToken(loggedInUser);

         return new TokenResponse(jwtToken, jwtUtils.getTokenExpirationTime());
    }

    Staff loginWithUserNameAndPassword(String userName,String password){
        Optional<Staff> byUserName = staffRepository.findByUserName(userName);
        if(byUserName.isPresent()){
            if(!passwordEncoder.matches(password,byUserName.get().getPassword())){
                throw new CustomValidationException(HttpStatus.UNAUTHORIZED.value(),"Password is incorrect!!");
            }
            return byUserName.get();
        }else throw new CustomValidationException(HttpStatus.UNAUTHORIZED.value(),"Account not found for username "+userName+",try with correct credentials!!");

    }

    private Staff loginWithEmailAndPassword(String email, String password) {
        Optional<Staff> byEmail = staffRepository.findByEmailOrUserName(email,email);
        if(byEmail.isPresent()){
            if(!passwordEncoder.matches(password,byEmail.get().getPassword())){
                throw new CustomValidationException(HttpStatus.UNAUTHORIZED.value(),"Password is incorrect!!");
            }
            return byEmail.get();
        }else throw new CustomValidationException(HttpStatus.UNAUTHORIZED.value(),"Account not found for email "+email+",try with correct credentials!!");
    }

    public TokenResponse refreshToken(String oldToken) {
        try {
            // We parse the claims even if the token is expired to get the user details
            Claims claims = jwtUtils.getClaimsFromExpiredToken(oldToken);

            // Reconstruct the LoggedInUser object from claims
            LoggedInUser user = new LoggedInUser();
            user.setStaffId(Long.parseLong(claims.getSubject()));
            user.setUuid((String) claims.get("uuid"));
            user.setUserName((String) claims.get("userName"));
            user.setFirstName((String) claims.get("firstName"));
            user.setLastName((String) claims.get("lastName"));
            user.setEmail((String) claims.get("email"));
            user.setRoles(new HashSet<>((List<String>) claims.get("roles")));
            user.setPermissions(new HashSet<>((List<String>) claims.get("permissions")));

            if(user.getStaffId() != null){
                Optional<Staff> byId = staffRepository.findById(user.getStaffId());
                byId.ifPresent(staff -> user.setCompanyId(staff.getCompanyId()));
            }
            // Generate a fresh token using your existing method
            String newToken = jwtUtils.generateToken(user);

            return new TokenResponse(newToken, jwtUtils.getTokenExpirationTime());

        } catch (Exception e) {
            throw new CustomValidationException(HttpStatus.UNAUTHORIZED.value(), "Session expired, please login again.");
        }
    }
}
