/**
 *
 */
package io.yope.payment.neo4j.repositories;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

import io.yope.payment.neo4j.domain.Neo4JWallet;

/**
 * @author massi
 *
 */
public interface WalletRepository extends GraphRepository<Neo4JWallet> {

    /**
     * finds all the wollet for a given account.
     * @param accountId the account's id
     * @return a list of wallet
     */
    @Query("MATCH (a)-[:OWN]->(w:Neo4JWallet) where id(a) = {accountId} RETURN w")
    List<Neo4JWallet> findAllByOwner(@Param("accountId") Long accountId);

    @Query("MATCH (a)-[:OWN]->(w:Neo4JWallet {name: {name}}) where id(a) = {accountId} RETURN w")
    Neo4JWallet findByName(@Param("accountId") Long accountId, @Param("name") String name);

}
