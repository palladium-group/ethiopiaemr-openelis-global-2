package org.openelisglobal.notebook.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NoteBookDAOImpl extends BaseDAOImpl<NoteBook, Integer> implements NoteBookDAO {
    public NoteBookDAOImpl() {
        super(NoteBook.class);
    }

}
