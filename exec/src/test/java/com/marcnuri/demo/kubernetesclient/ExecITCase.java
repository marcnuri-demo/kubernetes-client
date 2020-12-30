package com.marcnuri.demo.kubernetesclient;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.openshift.client.OpenShiftClient;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class ExecITCase {

  private static OpenShiftClient oc;

  @BeforeAll
  static void initEnvironment() {
    oc = new DefaultKubernetesClient().adapt(OpenShiftClient.class);
    deletePod();
    oc.pods().create(
      new PodBuilder()
          .withNewSpec()
            .addNewContainer()
              .withImage("busybox")
              .withName("busybox")
              .withCommand(Collections.singletonList("top"))
            .endContainer()
          .endSpec()
          .withNewMetadata()
            .withName("busybox")
          .endMetadata()
      .build()
    );
    await().atMost(10, TimeUnit.SECONDS).until(() ->
        oc.pods().withName("busybox").get().getStatus().getPhase().equals("Running"));
  }

  @AfterAll
  static void tearDown() {
    deletePod();
    oc = null;
  }

  private static void deletePod() {
    oc.pods().withName("busybox").withGracePeriod(1L).delete();
    await().atMost(10, TimeUnit.SECONDS).until(() -> oc.pods().withName("busybox").get() == null);
  }

  @Test
  void execShouldWaitToCompleteAndCaptureOutput() throws Exception {
    // Given
    final CountDownLatch cdl = new CountDownLatch(1);
    final ExecListener waitToComplete = new ExecListener() {
      @Override
      public void onOpen(Response response) {
      }

      @Override
      public void onFailure(Throwable t, Response response) {
        cdl.countDown();
      }

      @Override
      public void onClose(int code, String reason) {
        cdl.countDown();
      }
    };
    try (
      final ByteArrayOutputStream result = new ByteArrayOutputStream()
    ) {
      // When
      oc.pods().withName("busybox")
        .redirectingInput().writingOutput(result)
        .usingListener(waitToComplete)
        .exec("sh", "-c", "sleep 8 && echo hello");
      cdl.await(10, TimeUnit.SECONDS);
      // Then
      assertThat(result.toString(StandardCharsets.UTF_8.name()), is("hello\n"));
    }
  }
}
