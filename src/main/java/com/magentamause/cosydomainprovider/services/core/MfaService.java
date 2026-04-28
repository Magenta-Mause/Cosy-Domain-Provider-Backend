package com.magentamause.cosydomainprovider.services.core;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.core.MfaSetupResponseDto;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private static final String ISSUER = "CosyDomainProvider";

    private final UserRepository userRepository;

    public MfaSetupResponseDto setupMfa(UserEntity user) {
        if (!user.isVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not verified");
        }
        if (user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MFA already enabled");
        }
        String secret = new DefaultSecretGenerator(32).generate();
        user.setMfaSecret(secret);
        userRepository.save(user);
        String uri =
                new QrData.Builder()
                        .label(user.getEmail())
                        .secret(secret)
                        .issuer(ISSUER)
                        .algorithm(HashingAlgorithm.SHA1)
                        .digits(6)
                        .period(30)
                        .build()
                        .getUri();
        return MfaSetupResponseDto.builder().totpUri(uri).secret(secret).build();
    }

    public void confirmMfa(UserEntity user, String totpCode) {
        if (user.getMfaSecret() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA setup not initiated");
        }
        if (!verifyCode(user.getMfaSecret(), totpCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid TOTP code");
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    public boolean verifyCode(String secret, String code) {
        DefaultCodeVerifier verifier =
                new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
        verifier.setAllowedTimePeriodDiscrepancy(1);
        return verifier.isValidCode(secret, code);
    }
}
