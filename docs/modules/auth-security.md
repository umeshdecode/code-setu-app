# Auth & Security Module

> Standalone module reference. Verified directly against source on 2026-08-09. Where a claim could not
> be directly confirmed from source it is marked **(inferred)**. Cross-reference:
> [`../controllers.md`](controllers.md), [`../entities.md`](entities.md),
> [`../api-urls.md`](api-urls.md), [`../services-and-known-issues.md`](services-and-known-issues.md)
> — those docs already flagged some of the issues repeated here (inert `@PreAuthorize`, non-revoking
> refresh token, `GoogleRoleMapper` backdoor); this doc re-verifies each against the actual file rather
> than trusting the summary.

---

## 1. Overview

This module owns every path by which a request acquires, proves, or is denied an identity in the
CargoSetu backend: username/password login, mobile-OTP login (staff and customer), JWT issuance/parsing,
the (effectively decorative) refresh-token endpoint, the global Spring Security filter chain, and the
constants that define what a "permission" or a "public URL" is. Every other module depends on it —
any endpoint not explicitly public passes through `JwtAuthenticationFilter`, and any controller that
reads `@AuthenticationPrincipal LoggedInUser` depends on `LoggedInUser`'s shape and on `AuthService`/
`OtpService` having populated it correctly at login time. The module also seeds the initial SUPERADMIN
account and role/permission set on application startup.

---

## 2. API Controllers & Endpoints

Base path prefix: `BaseURL.BASE_URL = "/api/v2/cargosetu"` (`src/main/java/com/cargosetu/utils/BaseURL.java`)
applies only to `BaseApiController`. `AuthController` and `OtpAuthController` are **not** under that
prefix — they use their own `@RequestMapping` root.

### `AuthController` — `src/main/java/com/cargosetu/controller/AuthController.java`
`@RequestMapping(UrlConstants.AUTH)` → base path `/auth` (`UrlConstants.AUTH = "/auth"`,
`src/main/java/com/cargosetu/config/utils/UrlConstants.java`).

| Method | Full path | Auth required? | Request body | Response |
|---|---|---|---|---|
| `POST` | `/auth/login` (`UrlConstants.LOGIN_URL = "/login"`) | No (public — see §7) | `LoginRequest` `{email, userName, password}` | `200 OK` with `TokenResponse` on success; `417 EXPECTATION_FAILED` with `ApiResponseDto` if `TokenResponse.accessToken` is null, or `417` with a plain string body `"Something went wrong"` if `AuthService.isValidRequest()` returns false (in practice `isValidRequest` always either returns `true` or throws `CustomValidationException`, so this branch is effectively dead) |
| `POST` | `/auth/user/register` (`UrlConstants.REGISTER_USER = "/user/register"`) | No (public — see §7) | `StaffDTO` | `GenericDataDTO` — `CommonStatusCode.VALIDATION_ERROR` (422) if duplicate username/email, `CommonStatusCode.CREATED` (201) with saved `StaffDTO` on success, `INTERNAL_SERVER_ERROR` (500) if save returns null. Delegates validation/duplicate-check/save to `StaffService` (not part of this module) |

Note: `AuthController` declares constants `OTP`, `LOGOUT_URL` in `UrlConstants` but neither is mapped to
an actual endpoint in this controller — `LOGOUT_URL` has no controller method anywhere in the module
(**stateless JWT means there is nothing server-side to invalidate on logout today**).

### `OtpAuthController` — `src/main/java/com/cargosetu/controller/OtpAuthController.java`
`@RequestMapping("/auth/otp")` (hardcoded string literal, not built from `UrlConstants.AUTH + UrlConstants.OTP`).

| Method | Full path | Auth required? | Request body | Response |
|---|---|---|---|---|
| `POST` | `/auth/otp/send` | No (public — see §7) | `OtpSendRequest` `{mobileNumber}` (static nested class, no bean-validation annotations) | `200 OK` with `Map<String,Object>` `{status, message, mockOtp?}` — see §3 for `mockOtp` gating |
| `POST` | `/auth/otp/verify` | No (public — see §7) | `OtpVerifyRequest` `{mobileNumber, otpCode}` | `200 OK` with `TokenResponse` |

### `BaseApiController` — `src/main/java/com/cargosetu/controller/BaseApiController.java`
`@RequestMapping(BaseURL.BASE_URL)` → base path `/api/v2/cargosetu`.

| Method | Full path | Auth required? | Request | Response |
|---|---|---|---|---|
| `GET` | `/api/v2/cargosetu/refreshtoken` | Requires an `Authorization: Bearer <token>` header (the token may be **expired** — see §11) but the path itself is **not** in any `permitAll()` matcher and is not `/auth/**`, so it is subject to normal JWT-filter processing; however the filter simply fails to authenticate an expired token and moves on, and the controller reads the header directly | None (reads `Authorization` header itself) | `200 OK` with a new `TokenResponse` if `Authorization: Bearer <oldToken>` header present and `AuthService.refreshToken()` succeeds; `401 UNAUTHORIZED` with body `"Invalid Refresh Request"` otherwise (no header, wrong scheme, or any exception) |

`BaseApiController.apiResponse(Integer, HashMap)` is a shared helper used by *other* modules' controllers
(not auth-specific) to build a `{..., timestamp, status}` envelope keyed off `APIConstants.SUCCESS/FAIL/
INTERNAL_SERVER_ERROR/NO_CONTENT_FOUND` — included here only because it lives in this file.

---

## 3. Business / Service Logic

### `AuthService` — `src/main/java/com/cargosetu/service/AuthService.java`

- **`isValidRequest(LoginRequest)`**: throws `CustomValidationException(400, ...)` if the request is null,
  email is blank, or password is blank/null. Note the email-format check (`isValidEmail`/`EMAIL_REGEX`) is
  present but **commented out** (lines 58–63) — any non-blank string in the `email` field is accepted at
  this stage, including a plain username, because the actual DB lookup is by-email-OR-username (below).
- **`loginUser(LoginRequest)`**: only proceeds if `loginDto.getEmail() != null` (the `userName` field on
  `LoginRequest` is never read here). Calls `loginWithEmailAndPassword(email, password)`, which queries
  `staffRepository.findByEmailOrUserName(email, email)` — so despite the field being named `email`, a
  plain username also works because it's passed as both arguments. Throws
  `CustomValidationException(401, "Password is incorrect!!")` on a bcrypt mismatch, or
  `CustomValidationException(401, "Account not found for email "+email+...)` if no `Staff` row matches.
  On success: builds a `LoggedInUser` from the `Staff` row (roles = `staff.getRoles()` mapped to
  `Role.getCode()`; permissions = flattened `Role.getPermissions()` mapped to `Permission.getCode()`),
  calls `jwtUtils.generateToken(loggedInUser)`, and returns `new TokenResponse(jwtToken,
  jwtUtils.getTokenExpirationTime())`.
- **`loginWithUserNameAndPassword(String, String)`**: exists (package-private) but is **never called** —
  dead code; `loginUser()` only ever goes through `loginWithEmailAndPassword`.
- **`refreshToken(String oldToken)`**: see §11 — parses claims from the (possibly expired) old token via
  `jwtUtils.getClaimsFromExpiredToken`, rebuilds a `LoggedInUser` from the claims (re-fetching only
  `companyId` from `StaffRepository.findById`, everything else — including roles/permissions — is trusted
  as-is from the old token's claims, not re-derived from the DB), and issues a brand-new JWT. Wraps any
  failure in `CustomValidationException(401, "Session expired, please login again.")`.

### `OtpService` — `src/main/java/com/cargosetu/service/OtpService.java`

**Phone-number normalization/matching logic** (the reusable piece flagged by the task — duplicated
identically in both `sendOtp` and `verifyOtpAndLogin`/lookup):
1. `cleanNumber = mobileNumber.replaceAll("[^0-9+]", "")` — strip everything except digits and `+`.
2. `searchNumber = cleanNumber.startsWith("+") ? cleanNumber.substring(1) : cleanNumber` — the "no leading
   plus" form.
3. Look up by `searchNumber` first.
4. If empty **and** `cleanNumber` started with `+`, retry with the original `+`-prefixed `cleanNumber`.
5. If still empty and `searchNumber.length() > 10`, retry with just the **last 10 digits** of
   `searchNumber` (handles country-code-prefixed numbers stored without it, or vice versa).
6. This 3-step fallback chain is run independently against `StaffRepository.findByMobileNumberAndIsDeleted`
   and `CustomerRepository.findByMobileNoAndIsDelete` (both with `isDeleted/isDelete = false`).

- **`sendOtp(String mobileNumber)`** (`@Transactional`):
  1. Rejects blank input (`CustomValidationException(400, "Mobile number is required!")`).
  2. Rejects if the cleaned number has `< 10` chars (`CustomValidationException(400, "Invalid mobile
     number format!")`).
  3. Runs the phone-matching chain above against both Staff and Customer; if neither matches, throws
     `CustomValidationException(400, "Mobile number is not registered in the system!")` — **OTP send is
     gated on the number already existing in the system**, this is not an open signup-by-OTP flow.
  4. Generates a 6-digit numeric OTP: `String.valueOf(100000 + random.nextInt(900000))` using
     `java.security.SecureRandom`.
  5. Deletes any existing `OtpVerification` rows for both `cleanNumber` and `searchNumber`
     (`otpRepository.deleteByMobileNumber`, called twice) before inserting the new one — i.e. only one
     live OTP per number at a time, keyed under `cleanNumber`.
  6. Saves a new `OtpVerification` with `expiryTime = now + 5 minutes`, `verified = false`.
  7. Publishes a `GenericNotificationEvent("OTP_SMS", List.of(cleanNumber), NotificationChannel.SMS,
     {"otpCode": otpCode})` via Spring's `ApplicationEventPublisher` — actual delivery is handled
     asynchronously by the notification module's `SmsChannelSender` (Twilio-backed, see §10).
  8. Returns `{status: "SUCCESS", message: "OTP sent successfully.", mockOtp: <otp>}` — the `mockOtp` key
     is only included **if `app.auth.otp.mock=true`** (`@Value("${app.auth.otp.mock:true}")`, defaulting
     to `true` when the property is absent). It is currently `true` in the checked-in
     `application.properties`.
- **`verifyOtpAndLogin(String mobileNumber, String otpCode)`** (`@Transactional`):
  1. Rejects blank mobile/otp input.
  2. Re-derives `cleanNumber`/`searchNumber` the same way, then looks up the most recent OTP row via
     `otpRepository.findTopByMobileNumberOrderByCreatedAtDesc(cleanNumber)`, falling back to
     `searchNumber` if empty.
  3. Throws `CustomValidationException(400, "OTP not sent or invalid!")` if no row found.
  4. Throws `CustomValidationException(400, "OTP already verified and used!")` if `verification.isVerified()`
     is already true (defensive — in practice the row is deleted after successful verification, so this
     only fires on a race/replay within the same transaction window).
  5. Throws `CustomValidationException(400, "OTP has expired! Please request a new one.")` if
     `expiryTime.isBefore(now)`.
  6. Throws `CustomValidationException(400, "Incorrect OTP code. Please try again!")` on code mismatch
     (plain `String.equals`, not constant-time comparison).
  7. On match: sets `verified = true` then immediately **deletes** the row (`otpRepository.delete`) — so
     the `verified` flag is set but never actually persisted/read back; the delete is what prevents reuse.
  8. Re-runs the Staff lookup chain first; if a `Staff` matches, builds `LoggedInUser` with that staff's
     real roles/permissions and issues a JWT keyed on `staff.getStaffId()`.
  9. If no Staff matches, re-runs the Customer lookup chain; if a `Customer` matches, builds a
     `LoggedInUser` with `roles = Set.of("CUSTOMER")` and empty permissions, `staffId` field repurposed to
     hold `customer.getCustomerId()` (JWT subject), and issues a JWT.
  10. If neither matches (should be unreachable given step 2 of `sendOtp` already required a match, but
      the record could have been deleted in between), throws
      `CustomValidationException(500, "Failed to log in: user record disappeared!")`.

### `UserDetailsServiceImpl` — `src/main/java/com/cargosetu/service/UserDetailsServiceImpl.java`

Implements Spring Security's `UserDetailsService`. `loadUserByUsername(username)` tries
`staffRepository.findByEmail(username)` first, then falls back to `staffRepository.findByUserName(username)`;
throws `UsernameNotFoundException` if neither matches. Converts the found `Staff` into a `LoggedInUser`
(roles only — no permissions populated here, unlike `AuthService`/`OtpService`). **Not currently wired
into the active auth flow**: `SecurityConfig` has no `DaoAuthenticationProvider`/`AuthenticationManager`
bean configured (both are commented out, see §7), and no `@Bean UserDetailsService` is exposed — so this
class exists but nothing appears to invoke it today **(inferred from absence of any autowiring/usage
elsewhere in the searched codebase)**.

### `SeedSuperAdmin` — `src/main/java/com/cargosetu/service/SeedSuperAdmin.java`

`@Configuration` implementing `ApplicationRunner` — runs once on every application startup.
1. Looks up a `Staff` with username `"SUPERADMIN"`; if one already exists, does nothing.
2. Otherwise looks up (or creates via `seedDefaultRole()`) a `Role` with code `"SUPERADMIN"`.
3. `seedDefaultRole()` builds a fixed list of ~19 `Permission` rows (role/staff/department/plan/company/
   customer CRUD codes, e.g. `ROLE_CREATE`, `STAFF_READ`, `COMPANY_UPDATE`, `CUSTOMER_DELETE` — plain
   codes, no `PERM_` prefix at the DB layer; see §7/`PermissionConstants`), saves them via
   `permissionRepo.saveAllAndFlush`, attaches them to the `Role`, and saves the `Role`.
4. Creates the `Staff` row with a **hardcoded bcrypt password hash literal** in source
   (`"$2a$10$c6yl2E/iNR99hiHKlg6zkOTJJAD48lhDxQzG5/3ByTMrdg/7XGrbm"`) and hardcoded UUID
   `"31201SUPERADMIN31201"`, email `"superadmin@cargosetu"`, mobile country code `"+91"`, mobile number
   literal `"+++"`.

---

## 4. DTOs / Request & Response Models

| DTO | File | Fields | Notes |
|---|---|---|---|
| `LoginRequest` | `src/main/java/com/cargosetu/config/dto/LoginRequest.java` | `email`, `userName`, `password` (all `String`) | `@JsonIgnoreProperties(ignoreUnknown = true)`. `userName` is accepted on the wire but never read by `AuthService.loginUser()` (see §3) |
| `TokenResponse` | `src/main/java/com/cargosetu/config/dto/TokenResponse.java` | `accessToken` (String), `tokenType` (String, defaults to `"Bearer "` — note trailing space), `expiresIn` (Long, epoch-millis absolute expiry time, **not** a duration) | Returned by `/auth/login`, `/auth/otp/verify`, and `/api/v2/cargosetu/refreshtoken` |
| `OtpAuthController.OtpSendRequest` | `src/main/java/com/cargosetu/controller/OtpAuthController.java` (static nested class) | `mobileNumber` (String) | No validation annotations; `@Data` (Lombok) |
| `OtpAuthController.OtpVerifyRequest` | same file | `mobileNumber`, `otpCode` (both String) | No validation annotations; `@Data` |
| `LoggedInUser` | `src/main/java/com/cargosetu/config/dto/LoggedInUser.java` | `staffId` (Long), `uuid`, `userName`, `firstName`, `lastName`, `mobileNumber`, `mobileCode`, `email`, `roles` (Set\<String\>), `permissions` (Set\<String\>), `password`, `companyId` (Integer) | Implements Spring Security `UserDetails`. `getAuthorities()` builds a `Set<SimpleGrantedAuthority>` from `roles` prefixed `"ROLE_"` plus raw `permissions` strings (no `PERM_` prefix added here — see the `PERM_` mismatch note in §7). `getUsername()` returns `email` (not `userName`). This is the exact object injected wherever a controller declares `@AuthenticationPrincipal LoggedInUser` |

---

## 5. Entities & Database Relationships

### `RefreshToken` — `src/main/java/com/cargosetu/model/RefreshToken.java`
Table: `tblm_refresh_tokens`.

| Column | Type | Constraints |
|---|---|---|
| `id` | `Long` | `@Id`, `IDENTITY` |
| `staffId` | `Long` | plain column — **no `@ManyToOne`/`@JoinColumn` to `Staff`**, it's a bare FK-by-convention only, not a JPA relationship |
| `token` | `String` | `unique = true, nullable = false, length = 500` |
| `expiresAt` | `Instant` | — |
| `revoked` | `boolean` | default `false` |

**No `@ManyToOne` relationship to `Staff`/`Customer` exists on this entity** — it is a flat table.
`@Data` (Lombok) generates getters/setters/equals/hashCode.

### `OtpVerification` — `src/main/java/com/cargosetu/model/OtpVerification.java`
Table: `tblm_otp_verification`.

| Column | Type | Constraints |
|---|---|---|
| `id` | `Long` | `@Id`, `IDENTITY` |
| `mobile_number` | `String` | `nullable = false, length = 20` |
| `otp_code` | `String` | `nullable = false, length = 10` |
| `expiry_time` | `LocalDateTime` | `nullable = false` |
| `verified` | `boolean` | `nullable = false`, default `false` |
| `created_at` | `LocalDateTime` | `nullable = false`, set in `@PrePersist onCreate()` |

**No FK/relationship to `Staff` or `Customer`** — matching happens purely by string comparison of
`mobile_number` at query time (see §3's normalization logic), not via a foreign key.

---

## 6. Repositories

### `RefreshTokenRepository` — `src/main/java/com/cargosetu/repo/RefreshTokenRepository.java`
```java
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> { }
```
**No custom query methods at all** — plain `JpaRepository` CRUD only. `JwtUtils.createRefreshToken(Long
staffId)` calls `.save()` on it, but nothing in the codebase searched calls any find/delete method on this
repository — see §11 for the consequence (rows are written but apparently never read back for validation).

### `OtpVerificationRepository` — `src/main/java/com/cargosetu/repo/OtpVerificationRepository.java`
```java
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByMobileNumberOrderByCreatedAtDesc(String mobileNumber);
    void deleteByMobileNumber(String mobileNumber);
}
```
Both methods are exactly as declared — no others exist on this repository.

(`StaffRepository.findByMobileNumberAndIsDeleted`, `findByEmail`, `findByUserName`,
`findByEmailOrUserName`, and `CustomerRepository.findByMobileNoAndIsDelete` are used heavily by this
module's services but belong to the Staff and Customer modules respectively — listed here only for
completeness of what auth code calls.)

---

## 7. Security & Authentication Logic

### JWT generation/validation/expiry — `JwtUtils` — `src/main/java/com/cargosetu/config/JwtUtils.java`

- **Signing key is hardcoded in source**: `private final String secretKey =
  "Sa419Xr9r2R9gZPWS1gJNPfzs16rQzqX7MGk4z7E5GY=";`, turned into an HMAC `Key` via
  `Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8))`. Not read from configuration/
  environment — this is the exact class/line the task asked to locate and cite.
- **Algorithm**: whatever `Jwts.builder().signWith(key)` infers from the key (HMAC-SHA, via `jjwt`'s
  `io.jsonwebtoken.security.Keys`/`SignatureAlgorithm` — the specific HS256/384/512 variant is chosen
  automatically by `signWith(Key)` based on key length; not pinned explicitly in this code).
- **Expiry**: `EXPIRATION = 1000 * 60 * 60 * 24` ms = **24 hours**, hardcoded constant (not
  `@Value`-configurable). `getTokenExpirationTime()` returns `System.currentTimeMillis() + EXPIRATION`
  (absolute epoch-millis, used as `TokenResponse.expiresIn`).
- **`generateToken(LoggedInUser user)`**: builds claims `sub=staffId`, `staffId`, `uuid`, `roles`,
  `userName`, `firstName`, `lastName`, `email`, `permissions`, `companyId`; `iat=now`;
  `exp=now+24h`; signs with the hardcoded key.
- **`validateToken(String token)`**: `Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)
  .getBody()` — throws (e.g. `ExpiredJwtException`, `SignatureException`) on any invalid/expired token;
  callers are expected to catch.
- **`getClaimsFromExpiredToken(String token)`**: same parse, but catches `ExpiredJwtException` and returns
  `e.getClaims()` anyway — i.e. **deliberately allows extracting claims from an expired token**, which is
  what powers the "refresh" flow in §11.
- **`createRefreshToken(Long staffId)`**: builds a `RefreshToken` with a random `UUID` token string and
  `expiresAt = now + 30 days`, saves it via `RefreshTokenRepository`. **Not called from
  `AuthService.loginUser()`, `OtpService.verifyOtpAndLogin()`, or `AuthController`/`OtpAuthController`
  in the files read for this doc** — no code path was found that invokes `createRefreshToken` during
  login, so the `tblm_refresh_tokens` table appears to be write-path-only-if-called and currently unused
  by the actual login flows **(inferred from absence of a call site in the classes read; a caller could
  exist elsewhere in the codebase not covered by this review's file list)**.

### `JwtAuthenticationFilter` — `src/main/java/com/cargosetu/config/JwtAuthenticationFilter.java`

`OncePerRequestFilter`, registered via `.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`
in `SecurityConfig`.

- **`shouldNotFilter(HttpServletRequest request)`** — bypasses the filter entirely (no JWT parsing
  attempted, request proceeds with whatever authentication — usually none — already exists) for:
  - `path.equals("/pages/login")`
  - `path.startsWith("/css")`
  - `path.startsWith("/js")`
  - `path.startsWith("/images")`
  - `path.startsWith("/oauth2/")`
  - `path.startsWith("/login/oauth2/")`
  - `path.startsWith("/auth/")` — this is why `/auth/login`, `/auth/user/register`, `/auth/otp/send`,
    `/auth/otp/verify` all work with no `Authorization` header
  - `path.startsWith("/public/")`
  - `path.equals("/")`
- **`doFilterInternal`**: if `SecurityContextHolder` already has an `Authentication` (e.g. from OAuth2 —
  though OAuth2 login is disabled, see below), skips JWT processing entirely. Otherwise, if an
  `Authorization: Bearer <token>` header is present, calls `jwtUtil.validateToken(token)` (this is
  `validateToken`, **not** `getClaimsFromExpiredToken` — an expired token here throws and is caught,
  clearing the context, so **normal protected requests with an expired token are correctly rejected**;
  it's only the dedicated `/refreshtoken` endpoint that deliberately tolerates expiry). On success,
  extracts `roles` (each becomes `ROLE_<r>` authority) and `permissions` (each becomes `PERM_<p>`
  authority — note the filter adds the `PERM_` prefix itself; contrast with `LoggedInUser.getAuthorities()`
  in §4, which does **not** add any prefix to permissions — these two authority-building code paths are
  inconsistent, `LoggedInUser.getAuthorities()` is Spring's `UserDetails` contract method but the filter
  is what actually populates the live `SecurityContext` on every JWT-authenticated request, so **the
  filter's `PERM_`-prefixed version is what actually reaches `@PreAuthorize`/`hasAuthority()` checks**).
  Rebuilds `LoggedInUser` from claims and sets it as the `Authentication` principal via
  `UsernamePasswordAuthenticationToken(user, null, authorities)`. On any exception, clears the
  `SecurityContext` and logs to stdout (`System.out.println`, not the class's SLF4J logger) — the request
  then proceeds unauthenticated rather than being rejected by the filter itself (rejection happens later,
  at `.anyRequest().authenticated()` in `SecurityConfig`).
- A large block (lines ~47–109) of an earlier version of `doFilterInternal` is left in as a comment —
  functionally dead but worth noting for anyone diffing this file.

### `SecurityConfig` — `src/main/java/com/cargosetu/config/SecurityConfig.java`

- `@EnableWebSecurity`. Registers `BCryptPasswordEncoder` as a bean. CSRF disabled, session policy
  `STATELESS`, `formLogin`/`httpBasic`/`oauth2Login` all explicitly **disabled**
  (`AbstractHttpConfigurer::disable`) — despite `GoogleRoleMapper` (an OAuth2
  `GrantedAuthoritiesMapper`) being injected as a field, **OAuth2 login is not actually active** in this
  filter chain as configured; the field is unused beyond being a constructor dependency
  (`@RequiredArgsConstructor`) — confirmed by direct read, no `.oauth2Login(oauth -> ...)` configuration
  block references it.
- `permitAll()` matchers (`authorizeHttpRequests`):
  `/auth/**`, `/login.html`, `/css/**`, `/resources/static/assests**` *(sic — typo for "assets")*,
  `/js/**`, `/images/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html`,
  `/api/demo-notifications/**`, `/public/plan/public/**`, `/public/**`, plus separately
  `/ws/**` and `/topic/**`.
  `.anyRequest().authenticated()` covers everything else, including `/api/v2/cargosetu/refreshtoken`.
- `addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)` wires in `JwtAuthenticationFilter`.
- CORS (`corsConfigurationSource` bean): allowed origins are an explicit list —
  `http://127.0.0.1:31201`, `http://localhost:4200`, `https://code-setu-app.onrender.com`,
  `https://code-setu-app-ui.umeshsharma-code.workers.dev`, `https://code-setu-app-ui.onrender.com`;
  methods `GET, POST, PUT, DELETE` (no `PATCH`/`OPTIONS` explicitly listed); headers `Authorization,
  Content-Type`; `allowCredentials = true`.
- A `DaoAuthenticationProvider` bean and its own `AuthenticationManager` bean are present but **commented
  out** (lines 48–53, 107–110) — consistent with `UserDetailsServiceImpl` appearing unused (see §3).

### `MethodSecurityConfig` — `src/main/java/com/cargosetu/config/MethodSecurityConfig.java`

Full file:
```java
@Configuration
@EnableMethodSecurity(prePostEnabled = false)
public class MethodSecurityConfig {
}
```
Confirmed by direct read: **`prePostEnabled = false`**. Per Spring Security's own semantics, this
explicitly turns **off** processing of `@PreAuthorize`/`@PostAuthorize`/`@PreFilter`/`@PostFilter`
annotations. Practical effect: **any `@PreAuthorize(...)` annotation anywhere in the codebase (this
module or any other) is silently ignored** — the method executes regardless of the annotation's
condition. The only real gate on any given endpoint is therefore `SecurityConfig`'s URL-level
`authorizeHttpRequests` (i.e., "is this URL `permitAll()` or does it merely require *any* authenticated
JWT" — there is no role/permission-level authorization actually enforced at runtime today via
`@PreAuthorize`).

### `GoogleRoleMapper` — `src/main/java/com/cargosetu/utils/GoogleRoleMapper.java`

Full logic:
```java
@Override
public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
    List<GrantedAuthority> mapped = new ArrayList<>();
    // Give Google users SUPERADMIN (for now)
    mapped.add(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
    return mapped;
}
```
Confirmed by direct read: it **ignores its input entirely** (the `authorities` Google actually returned
are discarded) and **unconditionally** grants exactly one authority, `ROLE_SUPERADMIN`, to **any** caller
whose authorities get mapped through it — there is no email-domain check, no allow-list, no condition of
any kind. As-implemented, this is a full-privilege-escalation grant for whoever it's applied to.
**However**, `SecurityConfig` disables `oauth2Login` outright (see above) and no other place in the files
read wires this bean into an active `OAuth2UserService`/`GrantedAuthoritiesMapper` chain — so this class
is a registered Spring bean that currently has **no live invocation path** given the current
`SecurityConfig`. It would become an active superadmin backdoor the moment OAuth2 login is re-enabled and
this mapper is attached to it, so it should not be treated as inert-forever, only inert-as-currently-wired
**(inferred: no explicit `.oauth2Login(oauth -> oauth.userInfoEndpoint(u -> u.userAuthoritiesMapper(...)))`
call exists in the files reviewed; a wiring elsewhere in the codebase outside this review's file list
would change this conclusion)**.

### `PermissionConstants` — `src/main/java/com/cargosetu/utils/PermissionConstants.java`

Defines `PERM_PREFIX = "PERM_"` and ~30 `PERM_<CODE>` string constants (role/staff/company/plan/customer/
shift/shift-inventory/trip/trip-status-history/trip-expense/trip-document groups) intended for use in
`@PreAuthorize("hasAuthority(PermissionConstants.X)")` across controllers. The class's own doc comment
states the DB must store the **unprefixed** code (e.g. `STAFF_READ`) and the `PERM_` prefix is added only
at the constant-definition layer for use in annotations. This matches `JwtAuthenticationFilter` adding the
`PERM_` prefix when building live authorities from JWT claims (§ above) — but note `LoggedInUser.
getAuthorities()` (§4) does **not** add the prefix, so any code path that authenticates via
`UserDetailsServiceImpl`/`LoggedInUser.getAuthorities()` directly (rather than via the JWT filter) would
produce mismatched, unprefixed permission authorities. Moot in practice today only because (a) that path
appears unused (§3) and (b) `@PreAuthorize` checks are globally inert anyway (§ above).

---

## 8. Exception Handling

`CustomValidationException` — `src/main/java/com/cargosetu/handler/CustomValidationException.java`:
a `RuntimeException` subclass carrying an `Integer errorCode` alongside the message (two constructors,
with/without a `Throwable cause`).

Thrown directly by this module in:
- `AuthService.isValidRequest`: `400` for null request / blank email / blank password.
- `AuthService.loginWithUserNameAndPassword` (dead code, never called): `401` for bad password, `401` for
  account not found.
- `AuthService.loginWithEmailAndPassword`: `401` "Password is incorrect!!", `401` "Account not found for
  email ...".
- `AuthService.loginUser`: `400` "User not found,try with correct credentials!!" (when
  `loginWithEmailAndPassword` returns null — in practice unreachable since that method always throws
  rather than returning null, but kept as a defensive check).
- `AuthService.refreshToken`: `401` "Session expired, please login again." (wraps any exception during
  claim-parsing/token-regeneration).
- `OtpService.sendOtp`: `400` for blank number, `400` for too-short number, `400` "Mobile number is not
  registered in the system!".
- `OtpService.verifyOtpAndLogin`: `400` for blank input, `400` "OTP not sent or invalid!", `400` "OTP
  already verified and used!", `400` "OTP has expired! Please request a new one.", `400` "Incorrect OTP
  code. Please try again!", `500` "Failed to log in: user record disappeared!" (last-resort/unreachable
  branch).

`GlobalExceptionHandler` — `src/main/java/com/cargosetu/handler/GlobalExceptionHandler.java`
(`@RestControllerAdvice`, applies globally, not module-specific):
- `@ExceptionHandler(CustomValidationException.class)` → builds `ApiResponseDto(ex.getErrorCode(),
  ex.getMessage(), null)` and returns it via `ResponseEntity.badRequest()`. **The HTTP status line is
  always 400 regardless of `errorCode`** — a `401`- or `500`-tagged `CustomValidationException` (as
  thrown throughout this module) still comes back over the wire as HTTP 400; the "real" status only shows
  up inside the JSON body's `responseCode` field. Callers that branch on HTTP status rather than the body
  will not see 401/500 distinctions for any of the auth errors listed above.
- `@ExceptionHandler(Exception.class)` (catch-all) → builds `ApiResponseDto(CommonStatusCode.
  INTERNAL_SERVER_ERROR, ex.getMessage(), null)`, also returned via `ResponseEntity.badRequest()` — so an
  unexpected exception anywhere also surfaces as HTTP 400 with `responseCode=500` in the body.
- Note: `AuthController.login()` does **not** rely on `GlobalExceptionHandler` for its own explicit
  `417 EXPECTATION_FAILED` branches — those are returned directly by the controller before any exception
  would be thrown for that particular code path.

---

## 9. Configuration

| Property | Default / value in `application.properties` | Used by | Purpose |
|---|---|---|---|
| `app.auth.otp.mock` | `true` (also defaults to `true` via `@Value("${app.auth.otp.mock:true}")` if absent) | `OtpService.mockOtpEnabled` | Controls whether the generated OTP code is echoed back in the `/auth/otp/send` response body as `mockOtp` (for testing without real SMS) |
| `app.notification.twilio.account-sid` | real Twilio SID committed in plaintext | `TwilioConfig` (notification module) | Twilio SDK init (used for SMS/WhatsApp delivery, not auth-specific but is the transport for OTP SMS) |
| `app.notification.twilio.auth-token` | real Twilio token committed in plaintext | `TwilioConfig` | Twilio SDK init |
| `app.notification.twilio.from-number` | `+14012796973` | `SmsChannelSender.fromNumber` | Sender number for outbound SMS, including OTP messages |
| `app.notification.twilio.mock` | `true` | `TwilioConfig`, `SmsChannelSender.mockEnabled` | When `true`, SMS (and thus OTP) sending is mocked/logged rather than dispatched via the real Twilio API |
| `spring.security.oauth2.client.registration.google.client-id` | real Google OAuth2 client ID committed in plaintext | Spring Security OAuth2 auto-configuration | Google OAuth2 client registration (see §10) — currently inert since `oauth2Login` is disabled in `SecurityConfig` |
| `spring.security.oauth2.client.registration.google.client-secret` | real Google OAuth2 client secret committed in plaintext | same | same |

No `@Value`/`@ConfigurationProperties` was found for JWT signing key or JWT expiry — both are hardcoded
constants in `JwtUtils` (§7), not externally configurable. No `@ConfigurationProperties` class was found
for OTP (length is a hardcoded `100000 + random.nextInt(900000)` 6-digit range, expiry is a hardcoded
`plusMinutes(5)` in `OtpService`) — only the mock-mode flag is externalized.

All three property files live at `src/main/resources/application.properties`,
`application-local.properties`, `application-prod.properties`. **These files contain live plaintext
secrets (Twilio SID/token, Gmail SMTP password, Google OAuth2 client secret, production MySQL password)
— not reproduced verbatim here beyond what's already flagged; treat as a rotation candidate, matching the
existing note in `../services-and-known-issues.md`.**

---

## 10. External Integrations

- **SMS (OTP delivery)**: Twilio, via `notification/infrastructure/channel/SmsChannelSender.java` and
  `notification/config/TwilioConfig.java` (both outside this module's own package but are the concrete
  delivery mechanism for `OtpService.sendOtp`'s `GenericNotificationEvent(..., NotificationChannel.SMS,
  ...)`). Gated by `app.notification.twilio.mock` — currently `true`, so OTP SMS is **not actually sent**
  in the current checked-in config; the OTP code is only visible via the `mockOtp` field in the send
  response (itself gated by the separate `app.auth.otp.mock` flag, also `true`).
- **Google OAuth2**: client registration present in `application.properties`
  (`spring.security.oauth2.client.registration.google.client-id`/`client-secret`), and a
  `GrantedAuthoritiesMapper` bean (`GoogleRoleMapper`) exists to post-process any Google-authenticated
  user's authorities. **No scopes are explicitly configured** beyond whatever Spring Boot's OAuth2 client
  auto-configuration defaults to for a `google` registration (`openid`, `profile`, `email` by Spring
  Boot's own default when no `scope` property is set — **(inferred, not present as an explicit property
  in the files read)**). As covered in §7, `SecurityConfig` disables `oauth2Login`, so this integration is
  configured but not currently reachable through any live endpoint in the files reviewed.

---

## 11. Important Business Rules & Validations

- **Password hashing**: `BCryptPasswordEncoder` (Spring Security default strength), bean defined in
  `SecurityConfig`. Used by `AuthService.loginWithEmailAndPassword`/`loginWithUserNameAndPassword` via
  `passwordEncoder.matches(rawPassword, storedHash)`.
- **JWT expiry**: 24 hours, hardcoded (`JwtUtils.EXPIRATION`), not configurable — see §7.
- **OTP expiry window**: 5 minutes from generation (`OtpService.sendOtp`:
  `verification.setExpiryTime(LocalDateTime.now().plusMinutes(5))`), checked in `verifyOtpAndLogin` via
  `expiryTime.isBefore(LocalDateTime.now())`.
- **OTP format**: 6 numeric digits, `100000`–`999999` inclusive, generated with `SecureRandom`.
- **OTP one-shot / anti-replay**: the matched `OtpVerification` row is deleted immediately upon successful
  verification (`otpRepository.delete(verification)`), so the same code cannot be verified twice; a second
  `/auth/otp/send` call for the same number deletes any prior pending OTP row first
  (`otpRepository.deleteByMobileNumber`, called for both address forms).
- **OTP send requires a pre-existing account**: `sendOtp` throws if the mobile number matches neither a
  non-deleted `Staff` nor a non-deleted `Customer` — OTP cannot be used to self-register a brand-new
  identity.
- **Refresh token behavior — explicitly verified by reading `AuthService.refreshToken()` directly**:
  **DB-backed revocation is NOT checked.** The `refreshToken(String oldToken)` method never queries
  `RefreshTokenRepository` at all — it works purely by re-parsing claims out of the *JWT itself* (even if
  expired, via `jwtUtils.getClaimsFromExpiredToken`) and minting a brand-new JWT from those claims (only
  re-fetching `companyId` fresh from `StaffRepository`; roles/permissions are carried over unchanged from
  the old token's claims, not re-derived). The `RefreshToken` entity/table/`RefreshTokenRepository` exist
  in the codebase and `JwtUtils.createRefreshToken()` can write a row, but **no code path found in the
  files reviewed calls `createRefreshToken`, and no code path checks `RefreshToken.revoked` or
  `RefreshToken.expiresAt`.** Practical consequence: as long as a caller can produce *any* previously
  valid JWT for a given `staffId` (even long-expired, even one belonging to a since-deleted or
  since-demoted staff member, since roles/permissions aren't re-checked against current DB state either —
  only `companyId` is refreshed), the `/api/v2/cargosetu/refreshtoken` endpoint will happily mint a fresh
  24-hour token carrying the **stale** roles/permissions from the original login. There is no mechanism
  in this module to actually revoke a still-cryptographically-valid JWT before its 24h natural expiry.
- **No true logout**: consistent with the above, there is no endpoint or logic anywhere in this module
  that invalidates a live JWT (`UrlConstants.LOGOUT_URL` is declared but unmapped) — logout can only be a
  client-side token-discard.
- **OTP code comparison is not constant-time** (`verification.getOtpCode().equals(otpCode)`) — a timing
  side-channel exists in principle, though a 6-digit code and network jitter make this a low-severity,
  largely theoretical concern.

---

## 12. Dependencies Between Modules

**This module depends on:**
- **Staff / Role / Permission (staff-rbac module)** — `Staff`, `Role`, `Permission` entities and
  `StaffRepository`, `RolesRepo`, `PermissionRepo` repositories (all outside this module's own
  package/doc scope) are read directly by `AuthService`, `OtpService`, `UserDetailsServiceImpl`, and
  `SeedSuperAdmin` to authenticate users and build role/permission sets.
- **Customer module** — `Customer` entity and `CustomerRepository` (`com.cargosetu.customer.*`) are read
  by `OtpService` to support customer-side OTP login.
- **Notification module** — `OtpService.sendOtp` publishes a `GenericNotificationEvent` consumed by the
  notification engine's `SmsChannelSender`/`TwilioConfig` to actually (or mock-)deliver the OTP SMS.
- **`StaffService`** (used by `AuthController.registerUser`, not itself part of this module) for
  registration validation/duplicate-check/save.

**What depends on this module:** effectively every other module in the application. Any controller
protected by the default `.anyRequest().authenticated()` rule in `SecurityConfig` relies on
`JwtAuthenticationFilter` having successfully populated `SecurityContextHolder` from a valid bearer token,
and any controller method that declares `@AuthenticationPrincipal LoggedInUser` depends directly on the
`LoggedInUser` shape defined here and on `AuthService`/`OtpService` having set its `staffId`/`companyId`/
`roles`/`permissions` correctly at login time. Every `@PreAuthorize("hasAuthority(PermissionConstants.X)")`
annotation across the codebase depends on `PermissionConstants` (this module) for its string constants —
though, per §7, those checks are currently inert application-wide because of
`MethodSecurityConfig.prePostEnabled = false`.
