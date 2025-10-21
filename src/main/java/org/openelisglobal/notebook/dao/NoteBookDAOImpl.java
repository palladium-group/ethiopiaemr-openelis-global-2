package org.openelisglobal.notebook.dao;

import java.util.Date;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NoteBookDAOImpl extends BaseDAOImpl<NoteBook, Integer> implements NoteBookDAO {
    public NoteBookDAOImpl() {
        super(NoteBook.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate) {

        StringBuilder hql = new StringBuilder("select distinct nb from NoteBook nb ");
        hql.append("left join nb.tags t where 1=1 ");

        if (statuses != null && !statuses.isEmpty()) {
            hql.append("and nb.status in (:statuses) ");
        }

        if (types != null && !types.isEmpty()) {
            hql.append("and nb.type in (:types) ");
        }

        if (tags != null && !tags.isEmpty()) {
            hql.append("and t in (:tags) ");
        }

        if (fromDate != null) {
            hql.append("and nb.dateCreated >= :fromDate ");
        }

        if (toDate != null) {
            hql.append("and nb.dateCreated <= :toDate ");
        }

        Query<NoteBook> query = entityManager.unwrap(Session.class).createQuery(hql.toString(), NoteBook.class);

        if (statuses != null && !statuses.isEmpty()) {
            query.setParameterList("statuses", statuses);
        }

        if (types != null && !types.isEmpty()) {
            query.setParameterList("types", types);
        }

        if (tags != null && !tags.isEmpty()) {
            query.setParameterList("tags", tags);
        }

        if (fromDate != null) {
            query.setParameter("fromDate", fromDate);
        }

        if (toDate != null) {
            query.setParameter("toDate", toDate);
        }

        return query.list();
    }

}
