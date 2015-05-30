package dk.etiktak.backend.service;

import dk.etiktak.backend.model.Client;
import dk.etiktak.backend.repository.ClientRepository;
import dk.etiktak.backend.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Override
    public List<Client> findClients() {
        return Lists.asList(clientRepository.findAll());
    }
}
