package com.marcnuri.demo.kubernetesclient;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Example for https://stackoverflow.com/questions/69810796/mock-customresouce-with-fabric8
@EnableKubernetesMockClient
class CertificateRequestTest {

  KubernetesClient kubernetesClient;
  KubernetesMockServer mockServer;

  @Test
  void testDeleteWhenNotExistsShouldDoNothing() throws Exception {
    new CertificateRequestExample(kubernetesClient).delete("my-namespace", "my-cert");
    assertThat(mockServer.getLastRequest())
      .hasFieldOrPropertyWithValue("path", "/apis/our-certificates.custom.example.com/v1/certificaterequests/my-cert")
      .hasFieldOrPropertyWithValue("method", "GET");
  }

  @Test
  void testDeleteWhenExistsShouldDelete() throws Exception {
    mockServer.expect().get().withPath("/apis/our-certificates.custom.example.com/v1/certificaterequests/my-cert")
      .andReturn(200, "{}").once();
    new CertificateRequestExample(kubernetesClient).delete("my-namespace", "my-cert");
    assertThat(mockServer.getLastRequest())
      .hasFieldOrPropertyWithValue("path", "/apis/our-certificates.custom.example.com/v1/certificaterequests/my-cert")
      .hasFieldOrPropertyWithValue("method", "DELETE");
  }
}
