package io.momentum.demo.models.pipeline;


import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.options.PipelineOptions;
import com.google.cloud.dataflow.sdk.transforms.windowing.FixedWindows;
import com.google.cloud.dataflow.sdk.transforms.windowing.Window;
import com.google.cloud.dataflow.sdk.values.PCollection;
import com.googlecode.objectify.Objectify;
import org.joda.time.Duration;

import io.momentum.demo.models.logic.PlatformBridge;
import io.momentum.demo.models.logic.runtime.datastore.DatastoreService;
import io.momentum.demo.models.pipeline.options.FixedWindowOptions;
import io.momentum.demo.models.schema.UserMessage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;


/**
 * Created by sam on 1/12/16.
 */
public abstract class BasePipeline {
  public static final SimpleDateFormat daystampFormat = new SimpleDateFormat("yyyyMMdd");
  protected static final Logger logging = Logger.getAnonymousLogger();

  /* -- serialization -- */
  public static PlatformCodec codec = new PlatformCodec();

  /* -- datastore & logic -- */
  protected static PlatformBridge platform = PlatformBridge.acquire();
  protected static Objectify datastore() {
    return DatastoreService.ofy();
  }

  protected abstract Pipeline collapse(Pipeline pipeline);
  protected abstract PipelineOptions prepare(String[] args, Class<? extends PipelineOptions> optionsType) throws IOException;

  protected static <T> PCollection<T> window(FixedWindowOptions options,
                                             PCollection<T> stream) {
    return stream.apply(Window.<T>into(FixedWindows.of(Duration.standardMinutes(options.getFixedWindowDuration())))
                              .named("Fixed " + String.valueOf(options.getFixedWindowDuration()) + "-minute windows"));
  }
}
