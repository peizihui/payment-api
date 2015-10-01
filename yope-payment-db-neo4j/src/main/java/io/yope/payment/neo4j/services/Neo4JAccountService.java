/**
 *
 */
package io.yope.payment.neo4j.services;


import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.yope.payment.domain.Account;
import io.yope.payment.domain.Account.Status;
import io.yope.payment.exceptions.ObjectNotFoundException;
import io.yope.payment.neo4j.domain.Neo4JAccount;
import io.yope.payment.neo4j.repositories.AccountRepository;
import io.yope.payment.services.AccountService;

/**
 * @author massi
 *
 */
@Service
@Transactional
public class Neo4JAccountService implements AccountService, InitializingBean {

    @Autowired
    private AccountRepository repository;

    @Autowired
    private Neo4jTemplate template;

    /*
     * (non-Javadoc)
     * @see io.yope.payment.services.AccountService#create(io.yope.payment.domain.Account)
     */
    @Override
    public Account create(final Account account) {
        return repository.save(Neo4JAccount.from(account).registrationDate(System.currentTimeMillis()).build());
    }

    /*
     * (non-Javadoc)
     * @see io.yope.payment.services.AccountService#getById(java.lang.Long)
     */
    @Override
    public Account getById(final Long id) {
        return repository.findOne(id);
    }

    /*
     * (non-Javadoc)
     * @see io.yope.payment.services.AccountService#update(java.lang.Long, io.yope.payment.domain.Account)
     */
    @Override
    public Account update(final Long id, final Account account) throws ObjectNotFoundException {
        if (getById(id) == null) {
            throw new ObjectNotFoundException(String.valueOf(id), Account.class);
        }
        return repository.save(Neo4JAccount.from(account).modificationDate(System.currentTimeMillis()).id(id).build());
    }

    /*
     * (non-Javadoc)
     * @see io.yope.payment.services.AccountService#delete(java.lang.Long)
     */
    @Override
    public Account delete(final Long id) throws ObjectNotFoundException{
        final Account account = getById(id);
        if (account == null) {
            throw new ObjectNotFoundException(String.valueOf(id), Account.class);
        }
        final Neo4JAccount toDelete = Neo4JAccount.from(account).modificationDate(System.currentTimeMillis()).status(Status.DEACTIVATED).build();
        return repository.save(toDelete);
    }

    /*
     * (non-Javadoc)
     * @see io.yope.payment.services.AccountService#getAccounts()
     */
    @Override
    public List<Account> getAccounts() {
        return repository.findAll().as(List.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        template.query("MATCH (n:Account) SET n:`_Account`", null).finish();
        template.query("MATCH (n:Wallet) SET n:`_Wallet`", null).finish();
    }
}