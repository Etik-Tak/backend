package dk.etiktak.backend.repository.user;

import dk.etiktak.backend.model.user.Client;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends PagingAndSortingRepository<Client, Long> {
    Client findByUuid(String uuid);
    Client findByMobileNumberHashPasswordHashHashed(String mobileNumberHashedPaswordHashedHashed);
}
