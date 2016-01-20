package io.momentum.demo.models.logic.service.models;


import com.google.api.client.util.Joiner;
import com.google.api.server.spi.config.ApiTransformer;

import io.momentum.demo.models.logic.service.transformers.QueryOptionsTransformer;

import java.util.*;
import java.util.logging.Logger;


/**
 * Created by sam on 1/12/16.
 */
@ApiTransformer(QueryOptionsTransformer.class)
public final class QueryOptions {
  private static final Logger logging = Logger.getLogger(QueryOptions.class.getSimpleName());

  /** -- properties -- **/
  public Integer limit;
  public Integer offset;
  public String cursor;
  public Boolean keysOnly;
  public List<String> project;

  /** -- constructors -- **/
  public QueryOptions() {}

  public QueryOptions(Integer limit,
                      Integer offset,
                      String cursor,
                      Boolean keysOnly,
                      List<String> project) {
    this.limit = limit;
    this.offset = offset;
    this.cursor = cursor;
    this.keysOnly = keysOnly;
    this.project = project;
  }

  /** -- getters / setters -- **/
  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  public String getCursor() {
    return cursor;
  }

  public void setCursor(String cursor) {
    this.cursor = cursor;
  }

  public Boolean getKeysOnly() {
    return keysOnly != null ? keysOnly : false;
  }

  public void setKeysOnly(Boolean keysOnly) {
    this.keysOnly = keysOnly;
  }

  public List<String> getProject() {
    return project;
  }

  public void setProject(List<String> project) {
    this.project = project;
  }

  /** -- options string -- **/
  public String toString() {
    HashMap<String, String> options = new HashMap<>();
    if (cursor != null) options.put("c", cursor);
    if (limit != null) options.put("l", String.valueOf(limit));
    if (offset != null) options.put("o", String.valueOf(offset));
    if (keysOnly != null) options.put("k", String.valueOf(keysOnly));
    if (project != null) options.put("p", Joiner.on("|".charAt(0)).join(project));

    ArrayList<String> optionsSet = new ArrayList<>();
    for (Map.Entry<String, String> entry : options.entrySet()) {
      optionsSet.add(entry.getKey() + ":" + entry.getValue());
    }
    return Joiner.on(",".charAt(0)).join(optionsSet);
  }

  public static QueryOptions fromString(String subject) {
    // first, split by "," to get options list
    String[] optionsSet = subject.split(",");
    if (optionsSet.length > 0) {
      String cursor = null;
      Integer limit = null;
      Integer offset = null;
      Boolean keysOnly = null;
      List<String> project = new ArrayList<>();

      for (String option : optionsSet) {
        // extract key and value
        String[] splitItem = option.split(":");

        if (splitItem.length != 2) {
          // something is wrong: skip it
          logging.warning("Encountered invalid query option parameter: '" + option + "'. Skipping.");
          continue;
        }
        String itemKey = splitItem[0];
        String itemValue = splitItem[1];

        switch (itemKey) {
          case "c":
            cursor = itemValue;
            break;

          case "l":
            limit = Integer.valueOf(itemValue);
            break;

          case "o":
            offset = Integer.valueOf(itemValue);
            break;

          case "k":
            keysOnly = Boolean.valueOf(itemValue);
            break;

          case "p":
            // split projected keys
            String[] keys = itemValue.split("|");
            if (keys.length > 0) {
              project.addAll(Arrays.asList(keys));
            } else {
              logging.warning("Received projection-enabled request, but it specified no keys. Skipping.");
              continue;
            }
            break;
        }
      }

      // we have query options
      return new QueryOptions(limit,
                              offset,
                              cursor,
                              keysOnly,
                              project);
    } else {
      // empty
      return new QueryOptions();
    }
  }
}
