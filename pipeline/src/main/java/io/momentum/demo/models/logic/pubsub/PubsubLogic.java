package io.momentum.demo.models.logic.pubsub;


import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.model.*;
import com.google.cloud.dataflow.sdk.coders.AvroCoder;
import com.google.cloud.dataflow.sdk.coders.Coder;
import com.google.cloud.dataflow.sdk.coders.DefaultCoder;

import io.momentum.demo.models.logic.PlatformLogic;
import io.momentum.demo.models.logic.http.RetryHttpInitializerWrapper;
import io.momentum.demo.models.logic.runtime.state.AppEnginePlatformState;
import io.momentum.demo.models.logic.runtime.state.AppEngineRuntimeState;
import io.momentum.demo.models.logic.service.models.SerializedModel;
import io.momentum.demo.models.pipeline.coder.ModelCoder;
import io.momentum.demo.models.schema.AppModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by sam on 1/13/16.
 */
public final class PubsubLogic extends PlatformLogic {
  public static final boolean allowLocal = false;  // allow pubsub from dev
  private static final String APPLICATION_NAME = "mm/api-sample";
  private static final String SERVICE_PATH = "v1beta2/";
  private static final String ROOT_URL = "https://pubsub.googleapis.com/";

  /* -- API: topics -- */
  public Topic createTopic(String name)
      throws IOException {
    return createTopic(name, true);
  }

  public Topic createTopic(String name, boolean persist)
      throws IOException {
    Topic topic = new Topic();
    topic.setName(name);

    if (persist) {
      // provision via pubsub API
      publisher().projects()
                 .topics()
                 .create(topic.getName(), topic)
                 .execute();
    }
    return topic;
  }

  public Pubsub publisher()
      throws IOException {
    logging.finer("Allocating pub/sub publisher client.");

    logging.info("Using pubsub endpoint: '" + ROOT_URL + "'.");
    logging.finer("Using service path: '" + SERVICE_PATH + "'.");
    logging.finer("Using applciation name: '" + APPLICATION_NAME + "'.");

    HttpRequestInitializer initializer = new RetryHttpInitializerWrapper(resolveCredentials());

    return new Pubsub.Builder(GAE_TRANSPORT, JSON_FACTORY, initializer)
               .setRootUrl(ROOT_URL)
               .setApplicationName(APPLICATION_NAME)
               .build();
  }

  public List<Topic> listTopics()
      throws IOException {
    // prepare query
    Pubsub.Projects.Topics.List listQuery = publisher()
                                                .projects()
                                                .topics()
                                                .list("projects/" + new AppEnginePlatformState().getProject());
    String nextPageToken = null;

    // prepare response and report
    ListTopicsResponse response;
    List<Topic> topics = new ArrayList<>();
    String topicReport = "Topics:";

    // retrieve topics until there are no remaining pages
    do {
      if (nextPageToken != null)
        listQuery.setPageToken(nextPageToken);

      // execute query
      response = listQuery.execute();

      // add to report and list of results
      for (Topic topic : response.getTopics()) {
        topicReport += "\n  - " + topic;
        topics.add(topic);
      }

      nextPageToken = response.getNextPageToken();
    } while (nextPageToken != null);

    logging.info(topicReport);
    return topics;
  }

  /* -- API: publish -- */
  public PublishResponse publish(String topic, String data)
      throws IOException {
    return publish(resolveTopic(buildTopicKey(topic)), data);
  }

  public <M extends AppModel> PublishResponse publish(String topic, M model) throws IOException {
    ModelCoder coder = ModelCoder.of(model.getClass());

    // encode object
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    coder.encode(model, outputStream, Coder.Context.OUTER);
    byte[] bytestream = outputStream.toByteArray();

    // publish normally
    return publish(topic, bytestream);
  }

  public PublishResponse publish(String topic, byte[] data) throws IOException {
    return publish(resolveTopic(buildTopicKey(topic)), data);
  }

  public PublishResponse publish(Topic topic, String data)
      throws IOException {
    // allocate one-item list and publish
    List<String> oneShotBatch = new ArrayList<>(1);
    oneShotBatch.add(data);

    return publish(topic, oneShotBatch);
  }

  public PublishResponse publish(Topic topic, byte[] data) throws IOException {
    List<PubsubMessage> messages = new ArrayList<>();

    // allocate message and encode
    PubsubMessage message = new PubsubMessage();
    message.encodeData(data);
    messages.add(message);

    // pack message
    PublishRequest publishRequest = new PublishRequest().setMessages(messages);

    // execute request
    return publisher().projects().topics()
                      .publish(topic.getName(), publishRequest)
                      .execute();

  }

  public Topic resolveTopic(String name)
      throws IOException {
    return resolveTopic(name, true);
  }

  /* -- API: utils -- */
  public static String buildTopicKey(String topicName) {
    return "projects/" + new AppEnginePlatformState().getProject() + "/topics/" + topicName;
  }

  public PublishResponse publish(Topic topic, List<String> data)
      throws IOException {
    // loop state
    List<PubsubMessage> messages = new ArrayList<>();

    // pack each message
    for (String payload : data) {

      // allocate message and encode
      PubsubMessage message = new PubsubMessage();
      message.encodeData(payload.getBytes("UTF-8"));

      // pack message
      messages.add(message);
    }

    PublishRequest publishRequest = new PublishRequest().setMessages(messages);

    // execute request
    return publisher().projects().topics()
                      .publish(topic.getName(), publishRequest)
                      .execute();
  }

  public Topic resolveTopic(String name, boolean persist)
      throws IOException {

    Topic topic = null;
    Object cachedTopicMarker = this.bridge.memcache.sync.get("pubsub::topic::" + name);
    if (cachedTopicMarker != null) {
      logging.fine("Remembering resolved status for topic '" + name + "'.");
      topic = new Topic().setName(name);
    } else {
      logging.fine("Resolving pubsub topic: '" + name + "'.");

      try {
        topic = publisher().projects()
                           .topics()
                           .get(name)
                           .execute();
      } catch (GoogleJsonResponseException e) {
        if (e.getDetails() == null || e.getDetails()
                                       .getCode() == 404) {
          logging.info("Topic not found. Creating.");
          topic = createTopic(name, persist);
        } else {
          logging.severe("Encountered severe error fetching pubsub topic: " + e.getLocalizedMessage());
        }
      }

      if (topic == null)
        topic = createTopic(name, persist);

      logging.fine("Topic existence cached at key: '" + "pubsub::topic::" + name + "'");
      this.bridge.memcache.async.put("pubsub::topic::" + name, name);
    }

    return topic;
  }
}
