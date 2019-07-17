package org.rutlandmint.mgmt.door.log;

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

// Serves last 100 log entries at /log.json
@Controller
public class LogController {
	private @Autowired FileEventLog fl;
	
	final LinkedList<ILoggingEvent> logBuffer = new LinkedList<>();

	{
		Logger rootLogger = (Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		AppenderBase<ILoggingEvent> appender = new AppenderBase<ILoggingEvent>() {
			@Override
			protected void append(ILoggingEvent le) {
				synchronized (logBuffer) {
					logBuffer.add(le);
					if (logBuffer.size() > 100) {
						logBuffer.pop();
					}
				}
			}
		};
		appender.start();
		appender.setContext(rootLogger.getLoggerContext());
		rootLogger.addAppender(appender);
	}

	@RequestMapping("log.json")
	@ResponseBody
	JSONArray getLog() {
		final JSONArray a = new JSONArray();
		synchronized (logBuffer) {
			logBuffer.forEach(le -> {
				final JSONObject e = new JSONObject();
				e.put("time", le.getTimeStamp());
				e.put("level", le.getLevel().toString());
				e.put("category", le.getLoggerName());
				e.put("message", le.getFormattedMessage());
				a.put(e);
			});
		}
		return a;
	}

	@RequestMapping("accessLog.json")
	@ResponseBody
	JSONObject getAccessLog(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		if (request.getDateHeader("If-Modified-Since") >= fl.getLastModified()) {
			response.setStatus(304);
			return null;
		} else {
			return fl.getLog();
		}
	}

}
