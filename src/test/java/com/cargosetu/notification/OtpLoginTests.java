package com.cargosetu.notification;

import com.cargosetu.config.dto.TokenResponse;
import com.cargosetu.config.JwtUtils;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.customer.repository.CustomerRepository;
import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.model.Staff;
import com.cargosetu.model.OtpVerification;
import com.cargosetu.repo.StaffRepository;
import com.cargosetu.repo.OtpVerificationRepository;
import com.cargosetu.service.OtpService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OtpLoginTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OtpService otpService;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OtpVerificationRepository otpRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void testOtpLogin_CustomerFlow_Success() {
        // 1. Create and save a mock Customer
        Customer customer = new Customer();
        customer.setFirstName("Test");
        customer.setLastName("Customer");
        customer.setMobileNo("9999999999");
        customer.setEmail("customer@test.com");
        customer.setUuid(UUID.randomUUID().toString());
        customer.setIsDelete(false);
        customerRepository.save(customer);

        // 2. Trigger send OTP
        Map<String, Object> sendResult = otpService.sendOtp("9999999999");
        assertThat(sendResult.get("status")).isEqualTo("SUCCESS");
        
        String mockOtp = (String) sendResult.get("mockOtp");
        assertThat(mockOtp).isNotNull();

        // 3. Verify OTP record in DB
        Optional<OtpVerification> dbOtp = otpRepository.findTopByMobileNumberOrderByCreatedAtDesc("9999999999");
        assertThat(dbOtp).isPresent();
        assertThat(dbOtp.get().getOtpCode()).isEqualTo(mockOtp);

        // 4. Verify OTP and login to receive JWT
        TokenResponse tokenResponse = otpService.verifyOtpAndLogin("9999999999", mockOtp);
        assertThat(tokenResponse.getAccessToken()).isNotNull();

        // 5. Decode JWT and assert claims
        Claims claims = jwtUtils.validateToken(tokenResponse.getAccessToken());
        assertThat(claims.getSubject()).isEqualTo(customer.getCustomerId().toString());
        assertThat(claims.get("email")).isEqualTo("customer@test.com");
        
        List<String> roles = (List<String>) claims.get("roles");
        assertThat(roles).contains("CUSTOMER");

        // 6. Verify OTP record is deleted after successful validation
        Optional<OtpVerification> deletedOtp = otpRepository.findTopByMobileNumberOrderByCreatedAtDesc("9999999999");
        assertThat(deletedOtp).isEmpty();
    }

    @Test
    void testOtpLogin_StaffFlow_Success() {
        // 1. Create and save a mock Staff
        Staff staff = new Staff();
        staff.setFirstName("Test");
        staff.setLastName("Staff");
        staff.setMobileNumber("8888888888");
        staff.setEmail("staff@test.com");
        staff.setUuid(UUID.randomUUID().toString());
        staff.setUserName("teststaff");
        staff.setIsDeleted(false);
        staffRepository.save(staff);

        // 2. Trigger send OTP
        Map<String, Object> sendResult = otpService.sendOtp("8888888888");
        assertThat(sendResult.get("status")).isEqualTo("SUCCESS");
        
        String mockOtp = (String) sendResult.get("mockOtp");
        assertThat(mockOtp).isNotNull();

        // 3. Verify OTP and login
        TokenResponse tokenResponse = otpService.verifyOtpAndLogin("8888888888", mockOtp);
        assertThat(tokenResponse.getAccessToken()).isNotNull();

        // 4. Decode JWT
        Claims claims = jwtUtils.validateToken(tokenResponse.getAccessToken());
        assertThat(claims.getSubject()).isEqualTo(staff.getStaffId().toString());
    }

    @Test
    void testOtpLogin_UnregisteredNumber_ThrowsException() {
        assertThatThrownBy(() -> otpService.sendOtp("1111111111"))
                .isInstanceOf(CustomValidationException.class)
                .hasMessageContaining("Mobile number is not registered");
    }

    @Test
    void testOtpLogin_IncorrectOtp_ThrowsException() {
        // Create customer
        Customer customer = new Customer();
        customer.setFirstName("Test2");
        customer.setMobileNo("7777777777");
        customer.setUuid(UUID.randomUUID().toString());
        customer.setIsDelete(false);
        customerRepository.save(customer);

        // Send OTP
        otpService.sendOtp("7777777777");

        // Verify with invalid OTP code
        assertThatThrownBy(() -> otpService.verifyOtpAndLogin("7777777777", "999999"))
                .isInstanceOf(CustomValidationException.class)
                .hasMessageContaining("Incorrect OTP code");
    }

    @Test
    void testOtpLogin_ExpiredOtp_ThrowsException() {
        // Create customer
        Customer customer = new Customer();
        customer.setFirstName("Test3");
        customer.setMobileNo("6666666666");
        customer.setUuid(UUID.randomUUID().toString());
        customer.setIsDelete(false);
        customerRepository.save(customer);

        // Send OTP
        otpService.sendOtp("6666666666");

        // Manually set expiry time to the past
        OtpVerification otp = otpRepository.findTopByMobileNumberOrderByCreatedAtDesc("6666666666").get();
        otp.setExpiryTime(LocalDateTime.now().minusMinutes(1));
        otpRepository.save(otp);

        // Verify expired OTP
        assertThatThrownBy(() -> otpService.verifyOtpAndLogin("6666666666", otp.getOtpCode()))
                .isInstanceOf(CustomValidationException.class)
                .hasMessageContaining("OTP has expired");
    }

    @Test
    void testOtpLogin_RestController_SendAndVerifyQueryParam_Success() throws Exception {
        // Create customer
        Customer customer = new Customer();
        customer.setFirstName("WebTest1");
        customer.setMobileNo("5555555555");
        customer.setUuid(UUID.randomUUID().toString());
        customer.setIsDelete(false);
        customerRepository.save(customer);

        // 1. Send OTP (Query Param)
        mockMvc.perform(post("/auth/otp/send")
                        .param("mobileNumber", "5555555555")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.mockOtp").exists());

        OtpVerification otp = otpRepository.findTopByMobileNumberOrderByCreatedAtDesc("5555555555").get();
        String otpCode = otp.getOtpCode();

        // 2. Verify OTP (Query Param)
        mockMvc.perform(post("/auth/otp/verify")
                        .param("mobileNumber", "5555555555")
                        .param("otpCode", otpCode)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void testOtpLogin_RestController_SendAndVerifyJson_Success() throws Exception {
        // Create customer
        Customer customer = new Customer();
        customer.setFirstName("WebTest2");
        customer.setMobileNo("4444444444");
        customer.setUuid(UUID.randomUUID().toString());
        customer.setIsDelete(false);
        customerRepository.save(customer);

        // 1. Send OTP (JSON)
        mockMvc.perform(post("/auth/otp/send-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobileNumber\":\"4444444444\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.mockOtp").exists());

        OtpVerification otp = otpRepository.findTopByMobileNumberOrderByCreatedAtDesc("4444444444").get();
        String otpCode = otp.getOtpCode();

        // 2. Verify OTP (JSON)
        mockMvc.perform(post("/auth/otp/verify-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobileNumber\":\"4444444444\",\"otpCode\":\"" + otpCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
