package com.marcnuri.demo.kubernetesclient.karaf;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarafApplication {

  private static final Logger log = LoggerFactory.getLogger(KarafApplication.class);

  public void init() {
    log.info("Karaf Application has started");
    final KubernetesClient kubernetesClient = new DefaultKubernetesClient();
    log.info("Available nodes:");
    kubernetesClient.nodes().list().getItems().stream()
      .map(HasMetadata::getMetadata)
      .map(ObjectMeta::getName)
      .forEach(log::info);
    log.info("Pods:");
    kubernetesClient.pods().list().getItems().stream()
      .forEach(pod -> log.info("{} - {}", pod.getMetadata().getNamespace(), pod.getMetadata().getName()));
    kubernetesClient.load(KarafApplication.class.getResourceAsStream("/pod.yml"));
    kubernetesClient.load(KarafApplication.class.getResourceAsStream("/deployment.yml"));
  }

  public void destroy() {
    log.error("Stopping Karaf Application");
  }
}
