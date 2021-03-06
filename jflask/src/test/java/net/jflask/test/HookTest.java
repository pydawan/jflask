package net.jflask.test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import net.jflask.ErrorHandler;
import net.jflask.Request;
import net.jflask.Route;
import net.jflask.SuccessHandler;

public class HookTest extends AbstractAppTest {

  /** This one conflicts with initial declaration for ErrorHandler. */
  @Route("/")
  public String root() {
    return "root";
  }

  @Route("/barf")
  public String barf() {
    throw new RuntimeException("barf");
  }

  @Route("/hello/:name")
  public String getOk(String name) {
    return "Hello " + name;
  }

  /**
   * Check that 404 and other errors can be handled by ErrorHandlers.
   */
  @Test
  public void testErrorHook() throws Exception {

    final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    app.addErrorHandler(new ErrorHandler() {
      @Override
      public void onError(int status, Request request, Throwable t) {
        queue.offer(status + " " + request.getMethod() + " " + request.getRequestURI() + " " + t);
      }
    });

    try {
      client.get("/unknown");
    }
    catch (IOException e) {
    }

    Assert.assertEquals("404 GET /unknown null", queue.poll(1, TimeUnit.SECONDS));

    try {
      client.get("/barf");
    }
    catch (IOException e) {
    }

    Assert.assertEquals("500 GET /barf java.lang.RuntimeException: barf",
                        queue.poll(1, TimeUnit.SECONDS));

    Assert.assertEquals("root", client.get("/"));
    Assert.assertNull(queue.poll(500, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testSuccessHook() throws Exception {
    final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    app.addSuccessHandler(new SuccessHandler() {
      @Override
      public void onSuccess(Request r,
                            Method method,
                            Object[] args,
                            Object result) {
        queue.offer(r.getMethod() + " " + r.getRequestURI() + " " 
                    + method.getName() + Arrays.toString(args) + " " + result);
      }
    });

    client.get("/hello/world");

    Assert.assertEquals("GET /hello/world getOk[world] Hello world",
                        queue.poll(1, TimeUnit.SECONDS));

    Assert.assertEquals("root", client.get("/"));
    Assert.assertEquals("GET / root[] root", queue.poll(1, TimeUnit.SECONDS));
  }
}
