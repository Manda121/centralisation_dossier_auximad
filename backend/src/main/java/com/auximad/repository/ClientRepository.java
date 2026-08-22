package com.auximad.repository;

import com.auximad.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {

    @Query(value = "SELECT c.* FROM clients c WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "to_tsvector('french', coalesce(c.nom, '') || ' ' || coalesce(c.prenom, '') || ' ' || coalesce(c.email, '') || ' ' || coalesce(c.nif, '') || ' ' || coalesce(c.stat, '') || ' ' || coalesce(c.telephone, '')) @@ plainto_tsquery('french', :query)) " +
            "AND (:nif IS NULL OR :nif = '' OR c.nif = :nif) " +
            "AND (:stat IS NULL OR :stat = '' OR c.stat = :stat) " +
            "AND (:fax IS NULL OR :fax = '' OR c.fax = :fax) " +
            "AND (:codeClient IS NULL OR c.id = :codeClient) " +
            "AND (:nom IS NULL OR :nom = '' OR c.nom ILIKE CONCAT('%', :nom, '%')) " +
            "AND (:prenom IS NULL OR :prenom = '' OR c.prenom ILIKE CONCAT('%', :prenom, '%')) " +
            "AND (:email IS NULL OR :email = '' OR c.email ILIKE CONCAT('%', :email, '%')) " +
            "AND (:adresse IS NULL OR :adresse = '' OR c.adresse ILIKE CONCAT('%', :adresse, '%')) " +
            "AND (:ville IS NULL OR :ville = '' OR c.ville ILIKE CONCAT('%', :ville, '%')) " +
            "AND (:codePostal IS NULL OR :codePostal = '' OR c.code_postal = :codePostal) " +
            "AND (:paysId IS NULL OR c.id_pays = :paysId) " +
            "AND (:typeClientId IS NULL OR c.id_type_client = :typeClientId)",
            countQuery = "SELECT count(*) FROM clients c WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "to_tsvector('french', coalesce(c.nom, '') || ' ' || coalesce(c.prenom, '') || ' ' || coalesce(c.email, '') || ' ' || coalesce(c.nif, '') || ' ' || coalesce(c.stat, '') || ' ' || coalesce(c.telephone, '')) @@ plainto_tsquery('french', :query)) " +
            "AND (:nif IS NULL OR :nif = '' OR c.nif = :nif) " +
            "AND (:stat IS NULL OR :stat = '' OR c.stat = :stat) " +
            "AND (:fax IS NULL OR :fax = '' OR c.fax = :fax) " +
            "AND (:codeClient IS NULL OR c.id = :codeClient) " +
            "AND (:nom IS NULL OR :nom = '' OR c.nom ILIKE CONCAT('%', :nom, '%')) " +
            "AND (:prenom IS NULL OR :prenom = '' OR c.prenom ILIKE CONCAT('%', :prenom, '%')) " +
            "AND (:email IS NULL OR :email = '' OR c.email ILIKE CONCAT('%', :email, '%')) " +
            "AND (:adresse IS NULL OR :adresse = '' OR c.adresse ILIKE CONCAT('%', :adresse, '%')) " +
            "AND (:ville IS NULL OR :ville = '' OR c.ville ILIKE CONCAT('%', :ville, '%')) " +
            "AND (:codePostal IS NULL OR :codePostal = '' OR c.code_postal = :codePostal) " +
            "AND (:paysId IS NULL OR c.id_pays = :paysId) " +
            "AND (:typeClientId IS NULL OR c.id_type_client = :typeClientId)",
            nativeQuery = true)
    Page<Client> searchClients(
            @Param("query") String query,
            @Param("nif") String nif,
            @Param("stat") String stat,
            @Param("fax") String fax,
            @Param("codeClient") Integer codeClient,
            @Param("nom") String nom,
            @Param("prenom") String prenom,
            @Param("email") String email,
            @Param("adresse") String adresse,
            @Param("ville") String ville,
            @Param("codePostal") String codePostal,
            @Param("paysId") Integer paysId,
            @Param("typeClientId") Integer typeClientId,
            Pageable pageable
    );
}
