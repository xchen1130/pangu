/**
 *
 */
package snowpine.pangu;

import snowpine.pangu.rest.TransferReq;
import snowpine.pangu.rest.TransferRes;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import snowpine.pangu.dao.Transaction;
import snowpine.pangu.dao.User;

/**
 * @author xuesong
 *
 */
public class MainIT {

    private static final Client client = ClientBuilder.newClient();
    private static Process server;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.out.println("starting REST API server...\n");
        ProcessBuilder pb = new ProcessBuilder("java", "-jar",
                "./target/pangu-1.0.0.jar");
        pb.redirectError(new File("./error.log"));
        pb.redirectOutput(new File("./output.log"));
        server = pb.start();

        Thread.sleep(10 * 1000);
        System.out.println("done\n");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        System.out.println("\nshutting down REST API server...\n");
        Writer w = new OutputStreamWriter(server.getOutputStream());
        w.write("\n");
        w.flush();
        server.waitFor();
        System.out.println("done\n");
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws Exception {

        testGetBalance(1, 200, 100);
        testGetBalance(2, 200, 200);
        testGetBalance(3, 200, 300);
        testGetBalance(4, 404, 0);

        testTransfer(new TransferReq(3, 1, 100), 200, 1);
        testTransfer(new TransferReq(3, 4, 100), 400, 2);
        testTransfer(new TransferReq(4, 1, 100), 400, 3);
        testTransfer(new TransferReq(3, 1, 1000), 400, 4);
        testTransfer(new TransferReq(3, 1, -1000), 400, 5);
        testTransfer(new TransferReq(1, 1, 100), 400, 6);

        testGetBalance(1, 200, 200);
        testGetBalance(2, 200, 200);
        testGetBalance(3, 200, 200);

        testGetTransaction(1, 200, new TransferReq(3, 1, 100));
        testGetTransaction(2, 404, null);

        int n = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(n);
        class Task implements Runnable {

            private final TransferReq req;

            Task(TransferReq req) {
                this.req = req;
            }

            @Override
            public void run() {
                transfer(req);
            }
        }

        for (int i = 0; i < 15; i++) {
            int j = i % 3;
            executor.submit(new Task(new TransferReq(j + 1, (j + 1) % 3 + 1, i + 1)));            
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        testGetBalance(1, 200, 210);
        testGetBalance(2, 200, 195);
        testGetBalance(3, 200, 195);

    }

    private void testGetBalance(long userId, int httpStatus, long balance) {
        System.out.println("testing user " + userId + " balance");

        WebTarget target = client.target("http://localhost:9997/api/balance/"
                + userId);
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(httpStatus, response.getStatus());
        if (response.getStatus() == 200) {
            User user = response.readEntity(User.class);
            assertEquals(balance, user.getBalance());
        } else {
            System.out.println(response.readEntity(String.class));
        }

    }

    private void transfer(TransferReq req) {
        System.out.println("making transfer from " + req.getFrom()
                + " to " + req.getTo() + ": amount " + req.getAmount());

        WebTarget target = client.target("http://localhost:9997/api/transfer");
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(req, MediaType.APPLICATION_JSON_TYPE));

        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }

    private void testTransfer(TransferReq req, int httpStatus,
            long transactionId) {
        System.out.println("testing transfer " + transactionId);

        WebTarget target = client.target("http://localhost:9997/api/transfer");
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(req, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(httpStatus, response.getStatus());
        if (response.getStatus() == 200) {
            TransferRes ret = response.readEntity(TransferRes.class);
            assertEquals(transactionId, ret.getTransactionId());
        } else {
            System.out.println(response.readEntity(String.class));
        }
    }

    private void testGetTransaction(long transactionId, int httpStatus, TransferReq req) {
        System.out.println("testing transaction " + transactionId);

        WebTarget target = client.target("http://localhost:9997/api/transaction/"
                + transactionId);
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(httpStatus, response.getStatus());
        if (response.getStatus() == 200) {
            Transaction ret = response.readEntity(Transaction.class);
            assertEquals(req.getFrom(), ret.getFromUser());
            assertEquals(req.getTo(), ret.getToUser());
            assertEquals(req.getAmount(), ret.getAmount());
        } else {
            System.out.println(response.readEntity(String.class));
        }
    }

}
