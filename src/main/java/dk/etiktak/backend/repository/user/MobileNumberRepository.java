package dk.etiktak.backend.repository.user;

import dk.etiktak.backend.model.user.MobileNumber;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MobileNumberRepository extends PagingAndSortingRepository<MobileNumber, Long> {
    MobileNumber findByMobileNumberHash(String mobileNumberHash);
}
