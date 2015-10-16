package io.yope.payment.rest.resources;

import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.yope.payment.blockchain.BlockchainException;
import io.yope.payment.domain.Account;
import io.yope.payment.domain.Account.Type;
import io.yope.payment.domain.Transaction;
import io.yope.payment.domain.Transaction.Direction;
import io.yope.payment.domain.Transaction.Status;
import io.yope.payment.domain.transferobjects.TransactionTO;
import io.yope.payment.exceptions.IllegalTransactionStateException;
import io.yope.payment.exceptions.InsufficientFundsException;
import io.yope.payment.exceptions.ObjectNotFoundException;
import io.yope.payment.rest.BadRequestException;
import io.yope.payment.rest.helpers.AccountHelper;
import io.yope.payment.rest.helpers.TransactionHelper;

/**
 * Wallet Resource.
 */
@Controller
@EnableAutoConfiguration
@RequestMapping("/transactions")
public class TransactionResource extends BaseResource {

    @Autowired
    private AccountHelper accountHelper;

    @Autowired
    private TransactionHelper transactionHelper;

    /**
     * Create Transaction.
     * @param transaction
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public @ResponseBody PaymentResponse<Transaction> create(
            final HttpServletResponse response,
            @RequestBody(required=true) final TransactionTO transaction) {
        final ResponseHeader header = new ResponseHeader(true, "", Response.Status.CREATED.getStatusCode());
        final Account loggedAccount = getLoggedAccount();
        try {
            final Transaction saved = transactionHelper.create(transaction, loggedAccount.getId());
            response.setStatus(Response.Status.CREATED.getStatusCode());
            return new PaymentResponse<Transaction>(header, saved);
        } catch (final ObjectNotFoundException e) {
            response.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
            return this.badRequest(transaction, e.getMessage());
        } catch (final BlockchainException e) {
            response.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return this.badRequest(transaction, e.getMessage());
        } catch (final BadRequestException e) {
            response.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
            return this.badRequest(transaction, e.getMessage());
        }
    }

    /**
     * set a new status  for a transaction.
     * it is open only to administrator.
     * @param response
     * @param transactionId
     * @param status
     * @return
     */
    @RequestMapping(value="/{transactionId}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public @ResponseBody PaymentResponse<Transaction> modify(
            final HttpServletResponse response,
            @PathVariable final Long transactionId,
            @RequestParam(value="status", required=true) final Status status) {
        final ResponseHeader header = new ResponseHeader(true, "", Response.Status.ACCEPTED.getStatusCode());
        final Account loggedAccount = getLoggedAccount();
        if (!Type.ADMIN.equals(loggedAccount.getType())) {
            return this.unauthorized(null);
        }
        try {
            final Transaction saved = transactionHelper.transition(transactionId, status);
            response.setStatus(Response.Status.ACCEPTED.getStatusCode());
            return new PaymentResponse<Transaction>(header, TransactionTO.from(saved).build());
        } catch (final ObjectNotFoundException e) {
            response.setStatus(Response.Status.NOT_FOUND.getStatusCode());
            return this.notFound(null, e.getMessage());
        } catch (final InsufficientFundsException e) {
            response.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
            return this.badRequest(null, e.getMessage());
        } catch (final IllegalTransactionStateException e) {
            response.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
            return this.badRequest(null, e.getMessage());
        }
    }

    @RequestMapping(value="/{transactionId}", method = RequestMethod.GET, consumes = "application/json", produces = "application/json")
    public @ResponseBody PaymentResponse<Transaction> get(@PathVariable final long transactionId) {
        final Account loggedAccount = getLoggedAccount();
        final Transaction transaction = transactionHelper.get(transactionId);
        final ResponseHeader header = new ResponseHeader(true, "", Response.Status.OK.getStatusCode());
        if (accountHelper.owns(loggedAccount, transaction.getSource().getId())
         || accountHelper.owns(loggedAccount, transaction.getDestination().getId())) {
            return new PaymentResponse<Transaction>(header, transaction);
        }
        return this.unauthorized(null);
    }

    /**
     * retrieves an account's transactions.
     * @param accountId
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody PaymentResponse<List<TransactionTO>> getTransactions(final HttpServletResponse response,
           @RequestParam(value="reference", required=false) final String reference,
           @RequestParam(value="dir", required=false) final Direction direction) {

        final Account loggedAccount = getLoggedAccount();
        final Long accountId = loggedAccount.getId();
        final ResponseHeader header = new ResponseHeader(true, "", Response.Status.OK.getStatusCode());
        List<TransactionTO> transactions = null;
        try {
            transactions = transactionHelper.getTransactionsForAccount(accountId, reference, direction);
        } catch (final ObjectNotFoundException e) {
            response.setStatus(Response.Status.NOT_FOUND.getStatusCode());
            return this.notFound(null, e.getMessage());
        }
        return new PaymentResponse<List<TransactionTO>>(header, transactions);
    }

}
