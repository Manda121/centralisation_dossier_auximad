package com.auximad.service;

import com.auximad.dto.PageResponse;
import com.auximad.entity.Client;
import com.auximad.entity.Pays;
import com.auximad.entity.TypeClient;
import com.auximad.repository.ClientRepository;
import com.auximad.repository.PaysRepository;
import com.auximad.repository.TypeClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final PaysRepository paysRepository;
    private final TypeClientRepository typeClientRepository;

    public ClientService(ClientRepository clientRepository,
                         PaysRepository paysRepository,
                         TypeClientRepository typeClientRepository) {
        this.clientRepository = clientRepository;
        this.paysRepository = paysRepository;
        this.typeClientRepository = typeClientRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<Client> searchClients(String query, String nif, String stat, String fax,
                                              Integer codeClient, String nom, String prenom,
                                              String email, String adresse, String ville,
                                              String codePostal, Integer paysId, Integer typeClientId,
                                              int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.unsorted();
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        }
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Client> clientPage = clientRepository.searchClients(
                query, nif, stat, fax, codeClient, nom, prenom,
                email, adresse, ville, codePostal, paysId, typeClientId, pageable
        );
        return PageResponse.of(clientPage);
    }

    @Transactional(readOnly = true)
    public Optional<Client> getClientById(Integer id) {
        return clientRepository.findById(id);
    }

    @Transactional
    public Client createClient(Client client) {
        return clientRepository.save(client);
    }

    @Transactional
    public Client updateClient(Integer id, Client clientDetails) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable avec l'id : " + id));

        client.setNom(clientDetails.getNom());
        client.setPrenom(clientDetails.getPrenom());
        client.setEmail(clientDetails.getEmail());
        client.setTelephone(clientDetails.getTelephone());
        client.setAdresse(clientDetails.getAdresse());
        client.setVille(clientDetails.getVille());
        client.setCodePostal(clientDetails.getCodePostal());
        client.setPays(clientDetails.getPays());
        client.setTypeClient(clientDetails.getTypeClient());
        client.setNif(clientDetails.getNif());
        client.setStat(clientDetails.getStat());
        client.setFax(clientDetails.getFax());

        return clientRepository.save(client);
    }

    @Transactional
    public void deleteClient(Integer id) {
        if (!clientRepository.existsById(id)) {
            throw new IllegalArgumentException("Client introuvable avec l'id : " + id);
        }
        clientRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Pays> getAllPays() {
        return paysRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TypeClient> getAllTypeClients() {
        return typeClientRepository.findAll();
    }
}
