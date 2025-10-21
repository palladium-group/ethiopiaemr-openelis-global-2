package org.openelisglobal.notebook.dao;

import java.util.Date;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;

public interface NoteBookDAO extends BaseDAO<NoteBook, Integer> {

    List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags, Date fromDate,
            Date toDate);

}
