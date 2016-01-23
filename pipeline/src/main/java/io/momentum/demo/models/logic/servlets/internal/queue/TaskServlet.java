package io.momentum.demo.models.logic.servlets.internal.queue;


import com.fasterxml.jackson.core.type.TypeReference;
import javax.servlet.http.HttpServletRequest;

import io.momentum.demo.models.logic.taskqueue.Task;
import io.momentum.demo.models.logic.taskqueue.TaskqueueLogic;
import io.momentum.demo.models.pipeline.PlatformCodec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by sam on 1/22/16.
 */
public abstract class TaskServlet extends HookEventServlet {
  /* -- overrides -- */
  @Override
  public int onHook(HttpServletRequest request) throws IOException, HookFailedException {
    // load task metadata & data
    TaskqueueLogic.TaskMetadata metadata = this.platform.taskqueue.load(request);
    Object data;

    switch (metadata.format) {
      case RAW:
        data = rawPayload(request);
        break;
      case JSON:
        data = loadDataFromJson(request);
        break;
      case MODEL:
        data = loadDataFromModel(request);
        break;
      default:
        data = null;
        break;
    }

    // generate task
    TaskqueueLogic.AppQueue queue = this.platform.taskqueue.resolveAppQueue(metadata.queue);
    Task task = new Task(queue, metadata);

    if (data == null)
      logging.warning("Failed to load stapled task data.");
    return execute(request, task, data, metadata);
  }

  /* -- data methods -- */
  private Map<String, Object> loadDataFromJson(HttpServletRequest request) throws IOException, HookFailedException {
    return decodeJSONPayload(request, getBufferedBodyReader(request));
  }


  private Map<String, Object> loadDataFromModel(HttpServletRequest request) throws IOException, HookFailedException {
    String source = payloadAsString(getBufferedBodyReader(request));

    return new PlatformCodec()
               .getReader()
               .readValue(source, new TypeReference<HashMap<String, Object>>() { });
  }

  /* -- task execution -- */
  public int execute(HttpServletRequest request,
                     Task task,
                     Object data,
                     TaskqueueLogic.TaskMetadata metadata)
      throws IOException, HookFailedException {
    logging.info("Default task handler executed for task '" +
                     metadata.taskName + "'.");
    return 200;
  }
}
