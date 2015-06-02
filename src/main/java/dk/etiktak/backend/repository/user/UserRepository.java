package dk.etiktak.backend.repository.user;

import dk.etiktak.backend.model.user.User;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends PagingAndSortingRepository<User, Long> {
    User findByUuid(String uuid);
    User findByUsernameHash(String usernameHash);
}
