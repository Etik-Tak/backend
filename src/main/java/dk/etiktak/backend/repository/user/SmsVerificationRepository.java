package dk.etiktak.backend.repository.user;

import dk.etiktak.backend.model.user.SmsVerification;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface SmsVerificationRepository extends PagingAndSortingRepository<SmsVerification, Long> {
    SmsVerification findByMobileNumberHash(String mobileNumberHash);
}
