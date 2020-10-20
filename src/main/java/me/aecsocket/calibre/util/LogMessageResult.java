package me.aecsocket.calibre.util;

import me.aecsocket.unifiedframework.resource.result.DataResult;
import me.aecsocket.unifiedframework.resource.result.Result;
import me.aecsocket.unifiedframework.resource.result.entry.DataResultEntry;
import me.aecsocket.unifiedframework.util.log.LogLevel;

public class LogMessageResult extends DataResult<LogMessageResult.Message, LogMessageResult.Message> {
    public static class Message {
        private final LogLevel level;
        private final String message;
        private final String detail;

        public Message(LogLevel level, String message, String detail) {
            this.level = level;
            this.message = message;
            this.detail = detail;
        }

        public LogLevel getLevel() { return level; }
        public String getMessage() { return message; }
        public String getDetail() { return detail; }
    }

    public LogMessageResult addSuccessData(LogLevel level, String message, String detail) { addSuccessData(new Message(level, message, detail)); return this; }
    public LogMessageResult addSuccessData(LogLevel level, String message) { return addSuccessData(level, message, null); }
    public LogMessageResult addFailureData(LogLevel level, String message, String detail) { addFailureData(new Message(level, message, detail)); return this; }
    public LogMessageResult addFailureData(LogLevel level, String message) { return addFailureData(level, message, null); }

    @Override public LogMessageResult combine(Result<DataResultEntry<?>, DataResultEntry<Message>, DataResultEntry<Message>> other) { super.combine(other); return this; }
}
