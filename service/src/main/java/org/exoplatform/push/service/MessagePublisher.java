package org.exoplatform.push.service;

import org.exoplatform.push.domain.Message;

public interface MessagePublisher {

  void send(Message message) throws Exception;

}
