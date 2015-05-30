package dk.etiktak.backend.service;

import dk.etiktak.backend.model.Client;

import java.util.List;

public interface ClientService {
    List<Client> findClients();
}
