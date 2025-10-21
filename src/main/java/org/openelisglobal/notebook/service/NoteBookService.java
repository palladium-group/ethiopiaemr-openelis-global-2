package org.openelisglobal.notebook.service;

import java.util.Date;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.bean.NoteBookDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean;
import org.openelisglobal.notebook.form.NoteBookForm;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;

public interface NoteBookService extends BaseObjectService<NoteBook, Integer> {

    List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags, Date fromDate,
            Date toDate);

    void updateWithStatus(Integer noteBookId, NoteBookStatus status);

    void createWithFormValues(NoteBookForm form, String currentUserId);

    void updateWithFormValues(Integer noteBookId, NoteBookForm form, String currentUserId);

    NoteBookDisplayBean convertToDisplayBean(Integer noteBookId);

    NoteBookFullDisplayBean convertToFullDisplayBean(Integer noteBookId);

}
