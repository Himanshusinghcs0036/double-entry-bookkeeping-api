package com.yanimetaxas.doubleentry;

import static com.yanimetaxas.realitycheck.Reality.checkThat;

import com.yanimetaxas.doubleentry.tools.CoverageTool;
import com.yanimetaxas.doubleentry.util.BankContextUtil;
import com.yanimetaxas.doubleentry.validation.TransferValidationException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class BankFunctionalTest {

  private static final String FACTORY_CLASS_NAME = "com.yanimetaxas.doubleentry.BankFactoryImpl";

  private static final String CASH_ACCOUNT_1 = "cash_1_EUR";
  private static final String REVENUE_ACCOUNT_1 = "revenue_1_EUR";

  private AccountService accountService;
  private TransferService transferService;

  @Before
  public void setupSystemStateBeforeEachTest() throws Exception {
    BankFactory bankFactory = (BankFactory) Class.forName(FACTORY_CLASS_NAME).newInstance();

    accountService = bankFactory.getAccountService();
    transferService = bankFactory.getTransferService();

    bankFactory.setupInitialData();
  }

  @Test
  public void findTransactionsByAccountRefWhenTransactionForAccountNotExists() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("10.00", "EUR"));
    List<Transaction> transactions = transferService.findTransactionsByAccountRef(CASH_ACCOUNT_1);

    checkThat(transactions.size()).isEqualTo(0);
  }

  @Test
  public void getTransactionByRefWhenRefNotExists() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T2")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-10.50", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("10.50", "EUR"))
        .build());

    Transaction transaction = transferService.getTransactionByRef("");

    checkThat(transaction).isNull();
  }

  @Test(expected = IllegalStateException.class)
  public void transferFundsWhenTransferHasOneLeg() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void transferFundsWhenTransferReferenceIsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference(null)
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void transferFundsWhenTransferTypeIsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type(null)
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = TransferValidationException.class)
  public void transferFundsWhenTransferLegAccountRefIsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(null).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = TransferValidationException.class)
  public void transferFundsWhenTransferLegAmountIsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(null)
        .build());
  }

  @Test(expected = UnbalancedLegsException.class)
  public void transferFundsWhenTransactionLegsAreUnbalanced() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-10.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = TransferValidationException.class)
  public void transferFundsWhenAccountCurrencyNotMatchTransferCurrency() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "SEK"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = AccountNotFoundException.class)
  public void transferFundsWhenAccountNotFound() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "SEK"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account("wrong_account").amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = InsufficientFundsException.class)
  public void transferFundsWhenAccountIsOverdrawn() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("10.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1").type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-20.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("20.00", "EUR"))
        .build());
  }

  @Test(expected = InfrastructureException.class)
  public void accountAlreadyExists() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void createAccountWhenMoneyAmountIsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, new Money(null, Currency.getInstance("EUR")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void createAccountWhenMoneyCurrencyIsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, new Money(new BigDecimal("1000.00"), null));
  }

  @Test(expected = AccountNotFoundException.class)
  public void accountBalanceNotExists() {
    accountService.getAccountBalance(CASH_ACCOUNT_1);
  }

  @Test(expected = AccountNotFoundException.class)
  public void accountBalanceIsNull() {
    accountService.getAccountBalance(null);
  }

  @Test(expected = AccountNotFoundException.class)
  public void findTransactionsByAccountRef_WhenAccountNotExists() {
    transferService.findTransactionsByAccountRef("");
  }

  @After
  public void tearDown() throws Exception {
    CoverageTool.testPrivateConstructor(BankContextUtil.class);
  }
}
