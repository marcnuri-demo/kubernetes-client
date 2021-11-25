package com.marcnuri.demo.kubernetesclient;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateRequestExample {

  private static final Logger log = LoggerFactory.getLogger(CertificateRequestExample.class);

  private final KubernetesClient kubernetesClient;

  public CertificateRequestExample(KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }

  public void delete(String certNamespace, String certRequestName) {
    try {
      MixedOperation<
              CertificateRequest,
              KubernetesResourceList<CertificateRequest>,
              Resource<CertificateRequest>>
        certClient = kubernetesClient.resources(CertificateRequest.class);

      CertificateRequest certificateRequest =
        certClient.inNamespace(certNamespace).withName(certRequestName).get();
      if (certificateRequest == null) {
        log.info(
          "CertificateRequest {} in certNamespce {} is a new one.",
          certRequestName, certNamespace);
      } else {
        certClient.inNamespace(certNamespace).withName(certRequestName).delete();
        log.info(
          "Delete existing CertificateRequest {} in certNamespce {}, then create a new one.",
          certRequestName, certNamespace);
      }
    } catch (Exception exception) {
      log.error("Hit an error when create a CertificateRequest: ", exception);
    }
  }

  @Group("our-certificates.custom.example.com")
  @Version("v1")
  @Plural("certificaterequests")
  public static final class CertificateRequest
    extends CustomResource<CertificateRequestSpec, CertificateRequestStatus> {

  }
  public static final class CertificateRequestSpec {}
  public static final class CertificateRequestStatus {}
}
