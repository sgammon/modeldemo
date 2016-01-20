package io.momentum.demo.models.logic.service.exceptions;


import com.google.api.server.spi.ServiceException;


/**
 * Created by sam on 1/12/16.
 */
public interface ServiceError {
  Class<? extends ServiceException> exception();
}
