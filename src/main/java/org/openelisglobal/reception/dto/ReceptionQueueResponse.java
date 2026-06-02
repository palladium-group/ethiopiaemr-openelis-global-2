package org.openelisglobal.reception.dto;

import java.util.ArrayList;
import java.util.List;

public class ReceptionQueueResponse {

    private int page;
    private int pageSize;
    private int totalItems;
    private int totalPages;
    private List<ReceptionQueueItemDTO> items = new ArrayList<>();

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<ReceptionQueueItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ReceptionQueueItemDTO> items) {
        this.items = items;
    }
}
