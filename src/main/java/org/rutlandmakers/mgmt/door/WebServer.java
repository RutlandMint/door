package org.rutlandmakers.mgmt.door;

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServer extends Server {
	private static final Logger log = LoggerFactory.getLogger(WebServer.class);

	public WebServer(final DoorHardware dh, final MemberDatabase db, final AccessLog al, final AccessController ac) {
		super(8081);

		// Serves static resources
		final ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setWelcomeFiles(new String[] { "index.html" });
		resourceHandler.setBaseResource(Resource.newClassPathResource("/web"));

		// Serves last 100 log entries at /log.json
		final Handler logHandler = new AbstractHandler() {
			final LinkedList<LoggingEvent> logBuffer = new LinkedList<>();
			final Appender appender = new AppenderSkeleton() {
				@Override
				public boolean requiresLayout() {
					return false;
				}

				@Override
				public void close() {

				}

				@Override
				protected void append(final LoggingEvent le) {
					synchronized (logBuffer) {
						logBuffer.add(le);
						if (logBuffer.size() > 100) {
							logBuffer.pop();
						}
					}
				}

			};

			{
				org.apache.log4j.Logger.getRootLogger().addAppender(appender);
			}

			@Override
			public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
					final HttpServletResponse response) throws IOException, ServletException {
				if (target.equals("/log.json")) {
					response.setStatus(200);
					response.setContentType("application/json");
					final JSONObject ret = new JSONObject();
					final JSONArray a = new JSONArray();
					ret.put("log", a);
					synchronized (logBuffer) {
						logBuffer.forEach(le -> {
							final JSONObject e = new JSONObject();
							e.put("time", le.getTimeStamp());
							e.put("level", le.getLevel().toString());
							e.put("category", le.getLogger().getName());
							e.put("message", le.getRenderedMessage());
							a.put(e);
						});
					}
					response.getWriter().print(ret.toString());
					baseRequest.setHandled(true);
				}
			}
		};

		// Serves status at /status.json
		final Handler statusHandler = new AbstractHandler() {
			@Override
			public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
					final HttpServletResponse response) throws IOException, ServletException {
				if (target.equals("/status.json")) {
					response.setStatus(200);
					response.setContentType("application/json");
					final JSONObject ret = new JSONObject();
					ret.put("database", db.getStatus());
					ret.put("door", dh.getStatus());
					ret.put("accessControl", ac.getStatus());
					response.getWriter().print(ret.toString());
					baseRequest.setHandled(true);
				}
			}
		};

		// Serves access log at /accessLog.json
		final Handler accessHandler = new AbstractHandler() {
			@Override
			public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
					final HttpServletResponse response) throws IOException, ServletException {
				if (target.equals("/accessLog.json")) {
					response.setHeader("Cache-Control", "must-revalidate");
					if (request.getDateHeader("If-Modified-Since") >= al.getLastModified()) {
						response.setStatus(304);
					} else {
						response.setStatus(200);
						response.setContentType("application/json");
						response.setDateHeader("Last-Modified", al.getLastModified());
						response.getWriter().print(al.getLog().toString());
					}
					baseRequest.setHandled(true);
				}
			}
		};

		// Serves actual interaction
		final Handler actionHandler = new AbstractHandler() {
			@Override
			public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
					final HttpServletResponse response) throws IOException, ServletException {
				if (!baseRequest.getMethod().equals("POST")) {
					return;
				}
				switch (target) {
				case "/openBriefly.do":
					al.log("Admin", "Opened Door Remotely");
					dh.unlockBriefly();
					break;
				case "/disable.do":
					if (ac.isMemberAccessEnabled()) {
						al.log("Admin", "Disabled Member Access");
					}
					ac.setMemberAccessEnabled(false);
					break;
				case "/enable.do":
					if (!ac.isMemberAccessEnabled()) {
						al.log("Admin", "Enabled Member Access");
					}
					ac.setMemberAccessEnabled(true);
					break;
				}

				response.setStatus(200);
				baseRequest.setHandled(true);
			}
		};

		// Add the ResourceHandler to the server.
		final HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { resourceHandler, statusHandler, actionHandler, logHandler, accessHandler,
				new DefaultHandler() });
		setHandler(handlers);

	}
}
