/*
 * UploadIT.java
 *
 * Created on 2019-11-25, 13:08
 */
package com.marcnuri.demo.kubernetesclient;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.marcnuri.demo.kubernetesclient.Utils.execCommand;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-11-25.
 */
class UploadITCase {

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
  void uploadFile_shouldUploadFile() throws Exception {
    // When
    oc.pods().withName("busybox")
        .file("/tmp/uploaded-example.txt")
        .upload(Path.of(UploadITCase.class.getResource("/sample-upload-file.txt").toURI()));
    // Then
    final String result = execCommand(oc.pods().withName("busybox"),
        "[ -f /tmp/uploaded-example.txt ] && echo \"true\" || echo \"false\"");
    assertThat(result, is("true\n"));
  }

  @Test
  void uploadDir_shouldUploadDir() throws Exception {
    // When
    oc.pods().withName("busybox")
        .dir("/tmp/uploaded-dir")
        .upload(Path.of(UploadITCase.class.getResource("/sample-directory").toURI()));
    // Then
    final String directoryExists = execCommand(oc.pods().withName("busybox"),
        "[ -d /tmp/uploaded-dir ] && echo \"true\" || echo \"false\"");
    assertThat(directoryExists, is("true\n"));
    final String result = execCommand(oc.pods().withName("busybox"),
        "du -ha /tmp/uploaded-dir | sort -k 2");
    assertThat(result, is("3.4M\t/tmp/uploaded-dir\n" +
        "8.0K\t/tmp/uploaded-dir/nested-dir-1\n" +
        "4.0K\t/tmp/uploaded-dir/nested-dir-1/file1.txt\n" +
        "3.4M\t/tmp/uploaded-dir/nested-dir-2\n" +
        "3.4M\t/tmp/uploaded-dir/nested-dir-2/big-file.txt\n" +
        "0\t/tmp/uploaded-dir/nested-dir-2/empty-file.txt\n"));
  }
}
