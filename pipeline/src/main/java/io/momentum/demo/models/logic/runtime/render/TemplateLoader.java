package io.momentum.demo.models.logic.runtime.render;


import com.mitchellbosecke.pebble.error.LoaderException;
import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;

import io.momentum.demo.models.logic.runtime.state.AppEngineRuntimeState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.logging.Logger;


/**
 * Created by sam on 1/13/16.
 */
public final class TemplateLoader extends FileLoader implements Loader<String> {
  private static final String cacheVersion = "c1";
  private static final String separator = "::";
  private static final Logger logging = Logger.getLogger(TemplateLoader.class.getName());
  private final String version;
  private final String namespace;

  private String suffix = ".html";
  private String prefix = "WEB-INF/resources/templates/";
  private String charset = "UTF-8";

  public TemplateLoader() {
    super();
    AppEngineRuntimeState runtimeState = new AppEngineRuntimeState();
    version = runtimeState.getRuntimeVersion();
    namespace = runtimeState.getNamespace();
  }

  @Override
  public Reader getReader(String templateCacheKey) throws LoaderException {
    String[] split = templateCacheKey.split("::");
    String templateName = split[split.length - 1];  // last item is path
    String filepath = prefix + templateName;

    if (!templateName.endsWith(".html") &&
            !templateName.endsWith(".js") &&
            !templateName.endsWith(".css")) {
      filepath = prefix + templateName + suffix;
    }
    File f = new File(filepath);
    try {
      return new FileReader(f);
    } catch (FileNotFoundException e) {
      logging.severe("Template file not found: " + templateName);
      throw new RuntimeException(e);
    }
  }

  @Override
  public String createCacheKey(String path) {
    return cacheVersion + separator +
               namespace + separator +
               version + separator +
               path;
  }

  @Override
  public String resolveRelativePath(String s, String s1) {
    return null;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }
}
