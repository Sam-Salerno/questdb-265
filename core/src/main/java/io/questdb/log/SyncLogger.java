/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2023 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.log;

import io.questdb.mp.Sequence;
import io.questdb.network.Net;
import io.questdb.std.ThreadLocal;
import io.questdb.std.*;
import io.questdb.std.datetime.microtime.MicrosecondClock;
import io.questdb.std.datetime.microtime.TimestampFormatUtils;
import io.questdb.std.str.StringSink;

import java.io.File;

public final class SyncLogger implements LogRecord, Log {
    private final static ThreadLocal<StringSink> line = new ThreadLocal<>(StringSink::new);
    private final Sequence advisorySeq;
    private final MicrosecondClock clock;
    private final Sequence criticalSeq;
    private final Sequence debugSeq;
    private final Sequence errorSeq;
    private final Sequence infoSeq;
    private final CharSequence name;

    SyncLogger(
            MicrosecondClock clock,
            CharSequence name,
            Sequence debugSeq,
            Sequence infoSeq,
            Sequence errorSeq,
            Sequence criticalSeq,
            Sequence advisorySeq
    ) {
        this.clock = clock;
        this.name = name;
        this.debugSeq = debugSeq;
        this.infoSeq = infoSeq;
        this.errorSeq = errorSeq;
        this.criticalSeq = criticalSeq;
        this.advisorySeq = advisorySeq;
    }

    @Override
    public void $() {
        StringSink sink = line.get();
        System.out.println(sink);
        sink.clear();
    }

    @Override
    public LogRecord $(CharSequence sequence) {
        if (sequence == null) {
            sink().put("null");
        } else {
            sink().put(sequence);
        }
        return this;
    }

    @Override
    public LogRecord $(CharSequence sequence, int lo, int hi) {
        sink().put(sequence, lo, hi);
        return this;
    }

    @Override
    public LogRecord $(int x) {
        sink().put(x);
        return this;
    }

    @Override
    public LogRecord $(double x) {
        sink().put(x);
        return this;
    }

    @Override
    public LogRecord $(long l) {
        sink().put(l);
        return this;
    }

    @Override
    public LogRecord $(boolean x) {
        sink().put(x);
        return this;
    }

    @Override
    public LogRecord $(char c) {
        sink().put(c);
        return this;
    }

    @Override
    public LogRecord $(Throwable e) {
        if (e != null) {
            sink().put(Misc.EOL).put(e);
        }
        return this;
    }

    @Override
    public LogRecord $(File x) {
        sink().put(x == null ? "null" : x.getAbsolutePath());
        return this;
    }

    @Override
    public LogRecord $(Object x) {
        sink().put(x == null ? "null" : x.toString());
        return this;
    }

    @Override
    public LogRecord $(Sinkable x) {
        if (x == null) {
            sink().put("null");
        } else {
            x.toSink(sink());
        }
        return this;
    }

    @Override
    public LogRecord $256(long a, long b, long c, long d) {
        Numbers.appendLong256(a, b, c, d, sink());
        return this;
    }

    @Override
    public LogRecord $hex(long value) {
        Numbers.appendHex(sink(), value, false);
        return this;
    }

    @Override
    public LogRecord $hexPadded(long value) {
        Numbers.appendHex(sink(), value, true);
        return this;
    }

    @Override
    public LogRecord $ip(long ip) {
        Net.appendIP4(sink(), ip);
        return this;
    }

    @Override
    public LogRecord $ts(long x) {
        sink().putISODate(x);
        return this;
    }

    @Override
    public LogRecord $utf8(long lo, long hi) {
        Chars.utf8toUtf16(lo, hi, this);
        return this;
    }

    @Override
    public LogRecord advisory() {
        return addTimestamp(xadvisory(), LogLevel.ADVISORY_HEADER);
    }

    @Override
    public LogRecord advisoryW() {
        return addTimestamp(xAdvisoryW(), LogLevel.ADVISORY_HEADER);
    }

    @Override
    public LogRecord critical() {
        return addTimestamp(xcritical(), LogLevel.CRITICAL_HEADER);
    }

    @Override
    public LogRecord debug() {
        return addTimestamp(xdebug(), LogLevel.DEBUG_HEADER);
    }

    @Override
    public LogRecord debugW() {
        return addTimestamp(xDebugW(), LogLevel.DEBUG_HEADER);
    }

    @Override
    public LogRecord error() {
        return addTimestamp(xerror(), LogLevel.ERROR_HEADER);
    }

    @Override
    public LogRecord errorW() {
        return addTimestamp(xErrorW(), LogLevel.ERROR_HEADER);
    }

    @Override
    public LogRecord info() {
        return addTimestamp(xinfo(), LogLevel.INFO_HEADER);
    }

    @Override
    public LogRecord infoW() {
        return addTimestamp(xInfoW(), LogLevel.INFO_HEADER);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public LogRecord microTime(long x) {
        TimestampFormatUtils.appendDateTimeUSec(sink(), x);
        return this;
    }

    @Override
    public LogRecord put(char c) {
        sink().put(c);
        return this;
    }

    @Override
    public LogRecord ts() {
        sink().putISODate(clock.getTicks());
        return this;
    }

    @Override
    public LogRecord utf8(CharSequence sequence) {
        if (sequence == null) {
            sink().put("null");
        } else {
            sink().encodeUtf8(sequence);
        }
        return this;
    }

    public LogRecord xAdvisoryW() {
        return next(advisorySeq);
    }

    public LogRecord xDebugW() {
        return next(debugSeq);
    }

    public LogRecord xErrorW() {
        return next(errorSeq);
    }

    /**
     * Guaranteed log delivery at INFO level. The calling thread will wait for async logger
     * to become available instead of discarding log message.
     *
     * @return log record API
     */
    public LogRecord xInfoW() {
        return next(infoSeq);
    }

    @Override
    public LogRecord xadvisory() {
        return next(advisorySeq);
    }

    public LogRecord xcritical() {
        return next(criticalSeq);
    }

    public LogRecord xdebug() {
        return next(debugSeq);
    }

    public LogRecord xerror() {
        return next(errorSeq);
    }

    public LogRecord xinfo() {
        return next(infoSeq);
    }

    private LogRecord addTimestamp(LogRecord rec, String level) {
        return rec.ts().$(level).$(name);
    }

    private LogRecord next(Sequence seq) {
        if (seq == null) {
            return NullLogRecord.INSTANCE;
        }
        return this;
    }

    private StringSink sink() {
        return line.get();
    }
}
