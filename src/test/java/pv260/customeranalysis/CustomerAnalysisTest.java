package pv260.customeranalysis;

import static com.googlecode.catchexception.CatchException.catchException;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import pv260.customeranalysis.entities.Customer;
import pv260.customeranalysis.entities.Offer;
import pv260.customeranalysis.entities.Product;
import pv260.customeranalysis.exceptions.CantUnderstandException;
import pv260.customeranalysis.exceptions.GeneralException;
import pv260.customeranalysis.interfaces.AnalyticalEngine;
import pv260.customeranalysis.interfaces.ErrorHandler;
import pv260.customeranalysis.interfaces.NewsList;
import pv260.customeranalysis.interfaces.Storage;

import java.util.List;

import static org.mockito.Matchers.isA;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CustomerAnalysisTest {



    /**
     * Verify the ErrorHandler is invoked when one of the AnalyticalEngine methods
     * throws exception and the exception is not re-thrown from the CustomerAnalysis.
     * The exception is passed to the ErrorHandler directly, not wrapped.
     */
    @Test
    public void testErrorHandlerInvokedWhenEngineThrows() throws GeneralException {

        AnalyticalEngine engine = mock(AnalyticalEngine.class);
        ErrorHandler errorHandler = mock(ErrorHandler.class);
        Product product = mock(Product.class);
        Storage storage = mock(Storage.class);
        NewsList nl = mock(NewsList.class);
        when(engine.interesetingCustomers(product)).thenThrow(new CantUnderstandException());

        CustomerAnalysis analyzer = new CustomerAnalysis(asList(engine), storage, nl, errorHandler);

        catchException(() -> analyzer.findInterestingCustomers(product));

        verify(engine).interesetingCustomers(product);

    }

    /**
     * Verify that if first AnalyticalEngine fails by throwing an exception,
     * subsequent engines are tried with the same input.
     * Ordering of engines is given by their order in the List passed to
     * constructor of AnalyticalEngine
     */
    @Test
    public void testSubsequentEnginesTriedIfOneFails() throws GeneralException {

        AnalyticalEngine failingEngine = mock(AnalyticalEngine.class);
        AnalyticalEngine goodEngine = mock(AnalyticalEngine.class);
        ErrorHandler errorHandler = mock(ErrorHandler.class);
        Product product = mock(Product.class);
        Storage storage = mock(Storage.class);
        NewsList nl = mock(NewsList.class);
        when(failingEngine.interesetingCustomers(product)).thenThrow(new CantUnderstandException());

        CustomerAnalysis analyzer = new CustomerAnalysis(asList(failingEngine, goodEngine), storage, nl, errorHandler);

        catchException(() -> analyzer.findInterestingCustomers(product));

        verify(failingEngine).interesetingCustomers(product);
        verify(goodEngine).interesetingCustomers(product);
    }

    /**
     * Verify that as soon as the first AnalyticalEngine succeeds,
     * this result is returned as result and no subsequent
     * AnalyticalEngine is invoked for this input
     */
    @Test
    public void testNoMoreEnginesTriedAfterOneSucceeds() throws GeneralException {

        AnalyticalEngine firstEngine = mock(AnalyticalEngine.class);
        AnalyticalEngine secondEngine = mock(AnalyticalEngine.class);
        ErrorHandler errorHandler = mock(ErrorHandler.class);
        Product product = mock(Product.class);
        Storage storage = mock(Storage.class);
        NewsList nl = mock(NewsList.class);

        Customer customer = mock(Customer.class);

        when(firstEngine.interesetingCustomers(product)).thenReturn(asList(customer));

        CustomerAnalysis analyzer = new CustomerAnalysis(asList(firstEngine, secondEngine), storage, nl, errorHandler);

        List<Customer> customers = analyzer.findInterestingCustomers(product);

        verify(firstEngine).interesetingCustomers(product);
        verify(secondEngine, never()).interesetingCustomers(product);

        assertThat(customers.equals(asList(customer)));
    }

    /**
     * Verify that once Offer is created for the Customer,
     * this order is persisted in the Storage before being
     * added to the NewsList
     * HINT: you might use mockito InOrder
     */
    @Test
    public void testOfferIsPersistedBefreAddedToNewsList() throws GeneralException {

        AnalyticalEngine engine = mock(AnalyticalEngine.class);
        ErrorHandler errorHandler = mock(ErrorHandler.class);
        Product product = mock(Product.class);
        Storage storage = mock(Storage.class);
        NewsList nl = mock(NewsList.class);
        Customer customer = mock(Customer.class);

        CustomerAnalysis analyzer = new CustomerAnalysis(asList(engine), storage, nl, errorHandler);

        when(storage.find(Product.class, 0L)).thenReturn(product);
        when(engine.interesetingCustomers(product)).thenReturn(asList(customer));

        InOrder inOrder = inOrder(storage, nl);
        analyzer.prepareOfferForProduct(0L);

        inOrder.verify(storage).persist(Mockito.any(Offer.class));
        inOrder.verify(nl).sendPeriodically(Mockito.any(Offer.class));

    }

    /**
     * Verify that Offer is created for every selected Customer for the given Product
     * test with at least two Customers selected by the AnalyticalEngine
     * HINT: you might use mockito ArgumentCaptor 
     */
    @Test
    public void testOfferContainsProductAndCustomer() throws GeneralException {

        AnalyticalEngine engine = mock(AnalyticalEngine.class);
        ErrorHandler errorHandler = mock(ErrorHandler.class);
        Product product = mock(Product.class);
        Storage storage = mock(Storage.class);
        NewsList nl = mock(NewsList.class);
        Customer customerOne = mock(Customer.class);
        Customer customerTwo = mock(Customer.class);

        CustomerAnalysis analyzer = new CustomerAnalysis(asList(engine), storage, nl, errorHandler);
        when(storage.find(Product.class, 0L)).thenReturn(product);
        when(engine.interesetingCustomers(product)).thenReturn(asList(customerOne, customerTwo));

        ArgumentCaptor<Offer> varArgs = ArgumentCaptor.forClass(Offer.class);

        analyzer.prepareOfferForProduct(0L);

        verify(storage, Mockito.times(2)).persist(varArgs.capture());

        assertThat(varArgs.getAllValues().size() != 2);

        assertThat(varArgs.getAllValues().get(0).getCustomer() != customerOne);
        assertThat(varArgs.getAllValues().get(1).getCustomer() != customerTwo);
      
    }

}
