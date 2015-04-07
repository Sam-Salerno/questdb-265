/*
 * Copyright (c) 2014-2015. Vlad Ilyushchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nfsdb.lang.cst.impl.rsrc;

import com.nfsdb.exceptions.JournalException;
import com.nfsdb.exceptions.JournalRuntimeException;
import com.nfsdb.lang.cst.*;
import com.nfsdb.lang.cst.impl.ref.StringRef;
import com.nfsdb.storage.KVIndex;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * Streams rowids on assumption that {@link #keySource} produces only one key.
 * This is used in nested-loop join where "slave" source is scanned for one key at a time.
 */
@SuppressFBWarnings({"EXS_EXCEPTION_SOFTENING_NO_CHECKED"})
public class KvIndexTopRowSource implements RowSource, RowCursor {

    private final StringRef column;
    private final RowFilter filter;
    private final KeySource keySource;

    private KVIndex index;
    private KeyCursor keyCursor;
    private long lo;
    private long hi;
    private long localRowID;
    private RowAcceptor rowAcceptor;

    public KvIndexTopRowSource(StringRef column, KeySource keySource, RowFilter filter) {
        this.column = column;
        this.keySource = keySource;
        this.filter = filter;
    }

    @Override
    public RowCursor cursor(PartitionSlice slice) {
        rowAcceptor = filter != null ? filter.acceptor(slice) : null;
        try {
            this.index = slice.partition.getIndexForColumn(column.value);
            this.lo = slice.lo;
            this.hi = slice.calcHi ? slice.partition.open().size() - 1 : slice.hi;
            this.keyCursor = keySource.cursor(slice);
            return this;
        } catch (JournalException e) {
            throw new JournalRuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {

        if (!keyCursor.hasNext()) {
            return false;
        }

        KVIndex.IndexCursor indexCursor = index.cachedCursor(keyCursor.next());
        while (indexCursor.hasNext()) {
            localRowID = indexCursor.next();
            if (localRowID >= lo && localRowID <= hi && (rowAcceptor == null || rowAcceptor.accept(localRowID) == Choice.PICK)) {
                return true;
            }

            if (localRowID < lo) {
                break;
            }
        }

        return false;
    }

    @Override
    public long next() {
        return localRowID;
    }

    @Override
    public void reset() {
        keySource.reset();
    }
}
