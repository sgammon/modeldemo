package io.momentum.demo.models.logic.servlets.internal.queue.pubsub;


import com.google.api.services.pubsub.model.PublishResponse;
import javax.servlet.http.HttpServletRequest;

import io.momentum.demo.models.logic.servlets.internal.queue.HookFailedException;
import io.momentum.demo.models.logic.servlets.internal.queue.TaskServlet;
import io.momentum.demo.models.logic.taskqueue.Task;
import io.momentum.demo.models.logic.taskqueue.TaskqueueLogic;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by sam on 1/22/16.
 */
public final class PubsubEventServlet extends TaskServlet {
  private HookFailedException decodeFail() {
    return new HookFailedException("Unable to decode pubsub payload.");
  }

  @Override
  @SuppressWarnings("unchecked")
  public int execute(HttpServletRequest request, Task task, Object data, TaskqueueLogic.TaskMetadata metadata)
      throws IOException, HookFailedException {
    // try to extract deferred pubsub payload
    if (data.getClass().isAssignableFrom(HashMap.class)) {
      try {
        Map<String, Object> payload = (Map<String, Object>)data;
        String topic = (String)payload.get("topic");
        List<String> messages = (List<String>)payload.get("messages");

        PublishResponse response = this.platform.pubsub.publish(topic, messages);

        logging.info("Published " + response.getMessageIds().size() + " messages to topic '" + topic + "'.");
        return 202;

      } catch (ClassCastException e) {
        throw decodeFail();
      }
    } else {
      throw decodeFail();
    }
  }
}
