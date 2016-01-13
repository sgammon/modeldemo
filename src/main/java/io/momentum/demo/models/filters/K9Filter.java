package io.momentum.demo.models.filters;

import javax.servlet.*;

import java.io.IOException;


/**
 * Created by sam on 1/12/16.
 */
public final class K9Filter implements Filter {
  private FilterConfig config;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.config = filterConfig;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    chain.doFilter(request, response);  // no-op
  }

  @Override
  public void destroy() {
    this.config = null;
  }
}
