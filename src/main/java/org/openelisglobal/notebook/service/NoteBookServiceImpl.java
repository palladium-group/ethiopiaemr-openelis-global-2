package org.openelisglobal.notebook.service;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.dao.NoteBookDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NoteBookServiceImpl extends AuditableBaseObjectServiceImpl<NoteBook, Integer> implements NoteBookService {

    @Autowired
    protected NoteBookDAO baseObjectDAO;

    public NoteBookServiceImpl() {
        super(NoteBook.class);
        this.auditTrailLog = true;
    }

    @Override
    protected BaseDAO<NoteBook, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }

}
