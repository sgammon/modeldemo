package io.momentum.demo.models;


import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.AuthLevel;


/**
  * Add your first API methods in this class, or you may create another class. In that case, please
  * update your web.xml accordingly.
 **/
@Api(name = "unified",
     version = "v1",
     title = "Unified API",
     canonicalName = "Unified API",
     description = "Sample combined API.",
     defaultVersion = AnnotationBoolean.TRUE,
     useDatastoreForAdditionalConfig = AnnotationBoolean.FALSE,
     authLevel = AuthLevel.NONE,
     clientIds = {"292824132082.apps.googleusercontent.com"},
     namespace = @ApiNamespace(ownerName = "momentum ideas",
                               ownerDomain = "momentum.io",
                               packagePath = "platform/sample"))
public class UnifiedAPI {

}
