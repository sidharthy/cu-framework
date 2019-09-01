// Compilation Units Framework: a very generic & powerful data driven programming framework.
// Copyright (c) 2019 Sidharth Yadav, sidharth_08@yahoo.com
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.

package org.cuframework.util.logging;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.SocketHandler;

/**
 * @author Sidharth Yadav
 */
public class LogManager {
    private static final String DEFAULT_CONSOLE_LOGGER = "-default-console-logger-";
    private static final String DEFAULT_FILE_LOGGER = "-default-file-logger-";
    private static final String DEFAULT_SOCKET_LOGGER = "-default-socket-logger-";
    private static final String DEFAULT_NONE_LOGGER = "-default-none-logger-";  //if this is set as the defaultLoggerId then it basically would disable all logging
                                                                                //except for those log statements that have explicitly set their log target.

    private String defaultLoggerId = DEFAULT_NONE_LOGGER;  //by default logging is disabled.
    private Map<String, Logger> loggers = new HashMap<>();
    private static final SimpleFormatter defaultLogFormatter = newSimpleFormatter("[%1$tF %1$tT] [%2$-7s] %3$s %n");

    private static SimpleFormatter newSimpleFormatter(final String format) {
        return new SimpleFormatter() {
            private SimpleDateFormat dateFormat = null;
            private final String lineSep = System.getProperty("line.separator");

            @Override
            public String format(LogRecord record) {
                //The method body is essentially a copy of the java.util.logging.SimpleFormatter#format with the
                //only difference of changing the date format in the first line of log.
                StringBuffer buf = new StringBuffer(180);

                if (dateFormat == null)
                    dateFormat = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]");  //.getDateTimeInstance();  //<--- this is changed

                buf.append(dateFormat.format(new Date(record.getMillis())));
                buf.append(" [");
                buf.append(record.getSourceClassName());
                buf.append("] [");
                buf.append(record.getSourceMethodName());
                buf.append("]");
                buf.append(lineSep);

                buf.append(record.getLevel());
                buf.append(": ");
                buf.append(formatMessage(record));

                buf.append(lineSep);

                Throwable throwable = record.getThrown();
                if (throwable != null)
                {
                    StringWriter sink = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sink, true));
                    buf.append(sink.toString());
                }

                return buf.toString();

                /*return String.format(format,
                          new Date(record.getMillis()),
                          record.getLevel().getLocalizedName(),
                          record.getMessage()
                );*/
            }
        };
    }

    private static LogManager logManager = new LogManager();

    private LogManager() {
    }

    public static LogManager instance() {
        return logManager;
    }

    private static Formatter getFormatter(String logMsgFormat) {
        return logMsgFormat == null? defaultLogFormatter: newSimpleFormatter(logMsgFormat);
    }

    private Logger getDefaultConsoleLogger() {
        boolean newlyCreated = getOrCreateLogger(DEFAULT_CONSOLE_LOGGER);
        Logger logger = loggers.get(DEFAULT_CONSOLE_LOGGER);
        if (newlyCreated && logger != null) {
            Handler handler = new ConsoleHandler();
            Formatter formatter = defaultLogFormatter;
            Level level = null;
            try {
                Properties loggingConfig = getLoggingConfig(new Properties());
                String logMsgFormat = loggingConfig.getProperty("logging.console.format", loggingConfig.getProperty("logging.format"));
                formatter = getFormatter(logMsgFormat);
                String logLevel = loggingConfig.getProperty("logging.console.level", loggingConfig.getProperty("logging.level"));
                if (logLevel != null) {
                    level = LogManager.getLogLevel(logLevel);
                }
            } catch(IOException ioe) {
                //nothing to do
            }
            handler.setFormatter(formatter);
            if (level != null) {
                handler.setLevel(level);
                logger.setLevel(level);  //sync the logger level as well
            }
            logger.addHandler(handler);
        }
        return logger;
    }

    private Logger getDefaultFileLogger() {
        boolean newlyCreated = getOrCreateLogger(DEFAULT_FILE_LOGGER);
        Logger logger = loggers.get(DEFAULT_FILE_LOGGER);
        if (newlyCreated && logger != null) {
            try {
                Properties loggingConfig = getLoggingConfig(new Properties());
                String logFileDestination = loggingConfig.getProperty("logging.file.pattern");
                if (logFileDestination != null) {
                    String logFileSizeLimit = loggingConfig.getProperty("logging.file.limit");
                    int fileLimit = 0;
                    try {
                        fileLimit = Integer.parseInt(logFileSizeLimit);
                    } catch(Exception e) {
                        //ignore
                    }
                    String logFileCount = loggingConfig.getProperty("logging.file.count");
                    int fileCount = 1;
                    try {
                        fileCount = Integer.parseInt(logFileCount);
                        if (fileCount < 1)
                            fileCount = 1;
                    } catch(Exception e) {
                        //ignore
                    }
                    String logFileAppend = loggingConfig.getProperty("logging.file.append");  //whether append to file or clear. Expected values 'true' or 'false'
                    Handler handler = new FileHandler(logFileDestination, fileLimit, fileCount, Boolean.valueOf(logFileAppend));
                    handler.setFormatter(getFormatter(loggingConfig.getProperty("logging.file.format", loggingConfig.getProperty("logging.format"))));
                    String logLevel = loggingConfig.getProperty("logging.file.level", loggingConfig.getProperty("logging.level"));
                    if (logLevel != null) {
                        Level level = LogManager.getLogLevel(logLevel);
                        handler.setLevel(level);
                        logger.setLevel(level);  //sync the logger level as well
                    }
                    logger.addHandler(handler);
                }
            } catch(IOException ioe) {
                //nothing to do. the file logger wouldn't have been set and next call would reattempt its initialization.
            }
        }
        return logger;
    }

    private Logger getDefaultSocketLogger() {
        boolean newlyCreated = getOrCreateLogger(DEFAULT_SOCKET_LOGGER);
        Logger logger = loggers.get(DEFAULT_SOCKET_LOGGER);
        if (newlyCreated && logger != null) {
            try {
                Properties loggingConfig = getLoggingConfig(new Properties());
                String socketHost = loggingConfig.getProperty("logging.socket.host");
                String socketPort = loggingConfig.getProperty("logging.socket.port");
                if (socketHost != null && socketPort != null) {
                    Handler handler = new SocketHandler(socketHost, Integer.parseInt(socketPort));
                    handler.setFormatter(getFormatter(loggingConfig.getProperty("logging.socket.format", loggingConfig.getProperty("logging.format"))));
                    String logLevel = loggingConfig.getProperty("logging.socket.level", loggingConfig.getProperty("logging.level"));
                    if (logLevel != null) {
                        Level level = LogManager.getLogLevel(logLevel);
                        handler.setLevel(level);
                        logger.setLevel(level);  //sync the logger level as well
                    }
                    logger.addHandler(handler);
                }
            } catch(Exception e) {
                //nothing to do. the socket logger wouldn't have been set and next call would reattempt its initialization.
            }
        }
        return logger;
    }

    private boolean getOrCreateLogger(String loggerId) {
        boolean newlyCreated = false;
        Logger logger = loggers.get(loggerId);
        if (logger == null) {
            logger = Logger.getLogger(loggerId);
            logger.setUseParentHandlers(false);
            loggers.put(loggerId, logger);
            newlyCreated = true;
        }
        return newlyCreated;
    }

    private static Properties getLoggingConfig(Properties props) throws IOException {
        FileInputStream fin = null;
        try {
            if (System.getProperty("cu.logging.config") != null) {
                fin = new FileInputStream(System.getProperty("cu.logging.config"));
                props.load(fin);
            }
        } finally {
            if (fin != null)
                fin.close();
        }
        return props;
    }

    public Logger getLoggerByType(String type) {
        if (type == null) {
            return null;
        }
        Logger logger = null;
        switch(type) {
            case("file"): logger = getDefaultFileLogger(); break;
            case("console"): logger = getDefaultConsoleLogger(); break;
            case("socket"): logger = getDefaultSocketLogger(); break;
            case("none"): break;  //there is no logger to return
        }
        return logger;
    }

    public Logger getLogger() {
        return getLogger(defaultLoggerId, true);
    }

    public Logger getLogger(String loggerId) {
        return getLogger(loggerId, false);
    }

    /**
        Returns the logger against the loggerId. If no logger found then the default logger is returned depending on the value of the second parameter.
     */
    public Logger getLogger(String loggerId, boolean useDefaultLoggerIfNull) {
        if (loggerId == null) {
            if (!useDefaultLoggerIfNull)
                return null;
            loggerId = defaultLoggerId;
        }
        Logger logger = loggers.get(loggerId);
        return logger == null && useDefaultLoggerIfNull? getLoggerByType(getDefaultLoggerType()): logger;
    }

    public void setDefaultLoggerType(String type) {
        if (type == null) {
            return;
        }
        switch(type) {
            case("file"): defaultLoggerId = DEFAULT_FILE_LOGGER; getDefaultFileLogger(); break;  //calling getter to make sure the logger also initializes as needed.
            case("console"): defaultLoggerId = DEFAULT_CONSOLE_LOGGER; getDefaultConsoleLogger(); break;  //calling getter to make sure the logger also initializes as needed.
            case("socket"): defaultLoggerId = DEFAULT_SOCKET_LOGGER; getDefaultSocketLogger(); break;  //calling getter to make sure the logger also initializes as needed.
            case("none"): defaultLoggerId = DEFAULT_NONE_LOGGER; break;  //there is no logger to initialize in this case.
        }
    }

    public String getDefaultLoggerType() {
        String type = null;
        switch(defaultLoggerId) {
            case(DEFAULT_CONSOLE_LOGGER): type = "console"; break;
            case(DEFAULT_FILE_LOGGER): type = "file"; break;
            case(DEFAULT_SOCKET_LOGGER): type = "socket"; break;
            case(DEFAULT_NONE_LOGGER): type = "none"; break;
        }
        return type;
    }

    /* Commenting out setter method of log level because it may result in invalidation of handlers which are having lower log level set as their threshold. If setting of
       log level is to be provided then make sure the appropriate handlers are again added to the corresponding loggers.

    public boolean setLogLevel(Level thresholdLogLevel) {
        return setLogLevel(defaultLoggerId, thresholdLogLevel);
    }

    public boolean setLogLevel(String loggerId, Level thresholdLogLevel) {
        Logger logger = getLogger(loggerId, false);
        if (logger == null) {
            return false;
        }
        logger.setLevel(thresholdLogLevel);
        return true;
    }
    */

    /* Commenting out getter as well as there is no practical utility of having it. 
    public Level getLevel(String loggerId) {
        Logger logger = getLogger(loggerId, false);
        if (logger == null) {
            return null;
        }
        return logger.getLevel();
    }
    */

    /*
    public void addHandler(String loggerId, Handler handler) {
        Logger logger = getLogger(loggerId, false);
        if (logger != null && handler != null) {
            logger.addHandler(handler);
        }
    }

    public Handler[] getHandlers(String loggerId, Class<Handler> handlerType) {
        LinkedList<Handler> ll = new LinkedList<>();
        Logger logger = getLogger(loggerId, false);
        if (logger != null && handlerType != null) {
            Handler[] handlers = logger.getHandlers();
            if (handlers != null) {
                for (Handler handler : handlers) {
                    if (handlerType.isInstance(handler)) {
                        ll.add(handler);
                    }
                }
            }
        }
        return ll.toArray(new Handler[0]);
    }
    */

    public static Level getLogLevel(Object logLevel) {
        if (logLevel instanceof Level)
            return (Level) logLevel;

        Level level = Level.INFO;
        if (logLevel != null) {
            switch(logLevel.toString().toLowerCase().trim()) {
                case("warning"): level = Level.WARNING; break;
                case("error"):
                case("severe"): level = Level.SEVERE; break;
                case("fine"): level = Level.FINE; break;
                case("finer"): level = Level.FINER; break;
                case("debug"):
                case("finest"): level = Level.FINEST; break;
                default: level = Level.INFO; break;
            }
        }
        return level;
    }
}
