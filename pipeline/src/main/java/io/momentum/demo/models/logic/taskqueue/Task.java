package io.momentum.demo.models.logic.taskqueue;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.taskqueue.TaskOptions;

import io.momentum.demo.models.pipeline.PlatformCodec;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Created by sam on 1/21/16.
 */
public final class Task {
  public enum TaskDataFormat {
    JSON,  // JSON-formatted task body
    MODEL,  // serialized model, usually also using JSON
    RAW  // raw data of some sort
  }

  // -- queue state
  public TaskqueueLogic.AppQueue queue;

  // -- task data
  public String name;
  public String data;
  public TaskDataFormat format;

  // -- task execution
  public Long eta;
  public String uri;
  public String target;
  public TaskOptions.Method method;

  // -- error state
  public String exceptionClass;
  public String exceptionMessage;

  // -- constructors
  public Task() {}

  public Task(TaskqueueLogic.AppQueue taskQueue) {
    queue = taskQueue;
  }

  public void setState(Exception exception) {
    exceptionClass = exception.getClass()
                              .getName();
    exceptionMessage = exception.getMessage();
  }

  public Task(TaskqueueLogic.AppQueue taskQueue,
              TaskqueueLogic.TaskMetadata metadata) {
    this.queue = taskQueue;
    this.name = metadata.taskName;
    this.format = metadata.format;
    this.eta = metadata.taskETA;
    this.uri = metadata.uri;
    this.target = metadata.target;
    this.method = metadata.taskMethod;
  }

  // -- metadata management
  public Task setETA(long taskETA) {
    eta = taskETA;
    return this;
  }

  public Task setURI(String taskUri) {
    uri = taskUri;
    return this;
  }

  public Task setName(String taskName) {
    name = taskName;
    return this;
  }

  public Task setMethod(TaskOptions.Method taskMethod) {
    method = taskMethod;
    return this;
  }

  public Task setTarget(String newTarget) {
    target = newTarget;
    return this;
  }

  // -- data management
  public void setData(TaskDataFormat dataFormat, String taskData) {
    data = taskData;
    format = dataFormat;
  }

  public void setData(Map<String, Object> jsonData) throws IOException {
    ObjectMapper mapper = new PlatformCodec().getWriter();
    setData(TaskDataFormat.JSON, mapper.writeValueAsString(jsonData));
  }
}
