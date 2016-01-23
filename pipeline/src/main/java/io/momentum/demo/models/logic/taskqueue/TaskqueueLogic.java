package io.momentum.demo.models.logic.taskqueue;


import com.google.appengine.api.taskqueue.*;
import javax.servlet.http.HttpServletRequest;

import io.momentum.demo.models.logic.PlatformLogic;
import io.momentum.demo.models.logic.runtime.state.AppEnginePlatformState;
import io.momentum.demo.models.pipeline.PlatformCodec;
import io.momentum.demo.models.schema.AppModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withDefaults;


/**
 * Created by sam on 1/21/16.
 */
public final class TaskqueueLogic extends PlatformLogic {
  /* -- configuration structures -- */
  private final static String DEFAULT_METHOD = "POST";
  private final static String URI_PREFIX = "/_internal/queue/";

  public interface AppQueue {
    String name();
  }

  public static class GenericAppQueue implements AppQueue {
    private final String _name;

    public GenericAppQueue(String name) {
      this._name = name.toLowerCase();
    }

    public String getName() {
      return _name;
    }

    public String toString() {
      return getName();
    }

    public String name() {
      return getName();
    }
  }

  public final static class GoogleHeaderNames {
    public final static String QUEUE_NAME = "X-AppEngine-QueueName";
    public final static String TASK_NAME = "X-AppEngine-TaskName";
    public final static String TASK_RETRIES = "X-AppEngine-TaskRetryCount";
    public final static String TASK_EXECUTIONS = "X-AppEngine-TaskExecutionCount";
    public final static String TASK_ETA = "X-AppEngine-TaskETA";
    public final static String TASK_FAILFAST = "X-AppEngine-FailFast";
  }


  public final static class K9HeaderNames {
    public final static String QUEUE = "K9-Queue-ID";
    public final static String TARGET = "K9-Queue-Target";
    public final static String FORMAT = "K9-Task-PayloadEncoding";
  }


  public static class TaskMetadata {
    /* -- properties -- */
    public String uri;  // original task URI
    public String target;  // app version that should receive the task
    public String queue;  // raw string `name` of the subject queue
    public String taskName;  // name/id of the task, whether assigned manually or by Google
    public Long taskRetries;  // integer count of retries for this task (including non-executions)
    public Long taskExecutions;  // integer count of executions attempted for this task
    public Long taskETA;  // ETA, in milliseconds since epoch, for when this task was/is supposed to execute
    public Boolean failFast;  // flag indicating a preference to fail as fast as possible if a failure is expected
    public Task.TaskDataFormat format;  // indicates data format for encoded task payload
    public TaskOptions.Method taskMethod;  // HTTP method to execute when submitting task

    /* -- constructors -- */
    public TaskMetadata() {}

    public TaskMetadata(HttpServletRequest request, String method) {
      taskMethod = TaskOptions.Method.valueOf(method.toUpperCase());
      inflateFromServletRequest(request);
    }

    /* -- internals -- */
    private void inflateFromServletRequest(HttpServletRequest request) {
      uri = request.getRequestURI();
      queue = request.getHeader(GoogleHeaderNames.QUEUE_NAME);
      taskName = request.getHeader(GoogleHeaderNames.TASK_NAME);
      format = Task.TaskDataFormat.valueOf(request.getHeader(K9HeaderNames.FORMAT));
      taskRetries = safeParseLong(GoogleHeaderNames.TASK_RETRIES, request);
      taskExecutions = safeParseLong(GoogleHeaderNames.TASK_EXECUTIONS, request);
      taskETA = Long.valueOf((long)safeParseDouble(GoogleHeaderNames.TASK_ETA, request));
      failFast = (request.getHeader(GoogleHeaderNames.TASK_FAILFAST) != null);
    }

    private long safeParseLong(String headerName, HttpServletRequest request) {
      String headerValue = request.getHeader(headerName);
      if (headerValue != null) {
        try {
          return Long.parseLong(headerValue);
        } catch (NumberFormatException exc) {
          logging.severe("Failed to parse integer from header '"
                             + headerName + "'.\n" +
                             "Invalid value was: '" + headerValue + "'.");
          return 0;
        }
      } else {
        logging.warning("Received empty or null header value for '"
                            + headerName + "'.");
      }
      return 0;
    }

    private double safeParseDouble(String headerName, HttpServletRequest request) {
      String headerValue = request.getHeader(headerName);
      if (headerValue != null) {
        try {
          return Float.parseFloat(headerValue);
        } catch (NumberFormatException exc) {
          logging.severe("Failed to parse float from header '"
                             + headerName + "'.\n" +
                             "Invalid value was: '" + headerValue + "'.");
        }
      }
      return 0.0;
    }

    public TaskMetadata(HttpServletRequest request, TaskOptions.Method method) {
      taskMethod = method;
      inflateFromServletRequest(request);
    }

    public TaskMetadata(Task taskRecord) {
      inflateFromTaskRecord(taskRecord);
    }

    private void inflateFromTaskRecord(Task target) {
      taskName = target.name;
      taskETA = target.eta;
      queue = target.queue.name();
      this.target = target.target;

      if (target.method != null)
        taskMethod = target.method;
    }

    public TaskMetadata(HttpServletRequest request) {
      taskMethod = TaskOptions.Method.valueOf(DEFAULT_METHOD);  // default to POST-ed tasks
      inflateFromServletRequest(request);
    }

    /* -- setters -- */
    public String report() {
      return "  Name: " + taskName + "\n" +
                 "  Queue: " + queue + "\n" +
                 "  Method: " + taskMethod.toString() + "\n" +
                 "  Retries: " + String.valueOf(taskRetries) + "\n" +
                 "  Executions: " + String.valueOf(taskExecutions) + "\n" +
                 "  ETA: " + String.valueOf(taskETA) + "\n" +
                 "  FailFast: " + String.valueOf(failFast) + "\n";
    }
  }

  /* -- API: queues -- */
  public Queue resolveQueue(AppQueue queue) {
    return resolveQueue(queue.name());
  }

  public Queue resolveQueue(String name) {
    return QueueFactory.getQueue(name);
  }

  public AppQueue resolveAppQueue(String name) {
    return new GenericAppQueue(name);
  }

  /* -- API: tasks -- */
  public TaskOptions build(TaskMetadata task, Map<String, Object> data) throws IOException {
    String jsonData = new PlatformCodec().getWriter().writeValueAsString(data);
    return build(task, Task.TaskDataFormat.JSON, jsonData.getBytes("UTF-8"));
  }

  public TaskOptions build(TaskMetadata task, Task.TaskDataFormat dataFormat, byte[] dataPayload) {
    TaskOptions target = buildTaskFromMetadata(task, dataFormat);
    target.payload(dataPayload);
    return target;
  }

  private TaskOptions buildTaskFromMetadata(TaskMetadata metadata, Task.TaskDataFormat dataFormat) {
    TaskOptions target = buildTaskFromMetadata(metadata);
    target.header(K9HeaderNames.FORMAT, dataFormat.toString());

    if (metadata.target != null)
      target.header("Host", metadata.target + "-dot-" + (new AppEnginePlatformState().getProject()) + ".appspot.com");

    return target;
  }

  /* -- internals -- */
  private TaskOptions buildTaskFromMetadata(TaskMetadata metadata) {
    TaskOptions target = withDefaults().method(metadata.taskMethod);
    target.header(K9HeaderNames.QUEUE, metadata.queue);

    if (metadata.target != null) {
      target.header(K9HeaderNames.TARGET, metadata.target);
      target.header("Host", metadata.target + "-dot-mm-corp.appspot.com");
    }

    // fill target properties
    if (metadata.taskETA != null) target.etaMillis(metadata.taskETA);
    if (metadata.taskName != null) target.taskName(metadata.taskName);
    target.url(buildTaskURI(metadata.queue));
    return target;
  }

  public String buildTaskURI(String queue) {
    return URI_PREFIX + queue.toLowerCase();
  }

  public String buildTaskURI(AppQueue queue) {
    return URI_PREFIX + queue.name()
                             .toLowerCase();
  }

  public <M extends AppModel> TaskOptions build(TaskMetadata task, M data) throws IOException {
    // encode key and build
    String encoded = new PlatformCodec().getWriter().writeValueAsString(data);
    return build(task, Task.TaskDataFormat.MODEL, encoded.getBytes("UTF-8"));
  }

  public TaskOptions build(TaskMetadata task, String data) throws IOException {
    return build(task, Task.TaskDataFormat.RAW, data.getBytes("UTF-8"));
  }

  public TaskOptions build(TaskMetadata task, byte[] data) throws IOException {
    return build(task, Task.TaskDataFormat.RAW, data);
  }

  public TaskOptions build(TaskMetadata task, Task taskRecord) {
    TaskMetadata metadata = new TaskMetadata(taskRecord);  // @TODO: this looks like a serious bug

    if (taskRecord.data != null)
      return build(metadata, taskRecord.format, taskRecord.data.getBytes(StandardCharsets.UTF_8));
    return buildTaskFromMetadata(task);
  }

  public TaskMetadata load(HttpServletRequest request) throws IOException {
    return new TaskMetadata(request);
  }

  /* -- API: submit -- */
  public TaskOptions submit(Task targetOperation) throws IOException {
    // handle URI
    if (targetOperation.uri == null)
      targetOperation.uri = buildTaskURI(targetOperation.queue);

    // grab metadata and queue
    TaskMetadata meta = new TaskMetadata(targetOperation);
    Queue queue = QueueFactory.getQueue(meta.queue);
    return enqueue(queue, build(meta, targetOperation));
  }

  /* -- API: enqueue -- */
  public TaskOptions enqueue(String queue, TaskOptions task) {
    return enqueue(resolveQueue(queue), task);
  }

  public TaskOptions enqueue(Queue queue, TaskOptions task) {
    // add to queue
    TaskHandle handle = queue.add(task);
    task.taskName(handle.getName());  // copy over the name
    return task;
  }

  /* -- API: stats -- */
  public QueueStatistics stats(AppQueue queue) {
    return stats(QueueFactory.getQueue(queue.name()));
  }

  public QueueStatistics stats(Queue queue) {
    return queue.fetchStatistics();
  }

  public QueueStatistics stats(String queue) {
    return stats(QueueFactory.getQueue(queue));
  }

  public QueueStatistics stats() {
    return stats(QueueFactory.getDefaultQueue());
  }
}
