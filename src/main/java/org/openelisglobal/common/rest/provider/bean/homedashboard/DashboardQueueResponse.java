package org.openelisglobal.common.rest.provider.bean.homedashboard;

import java.util.ArrayList;
import java.util.List;

public class DashboardQueueResponse {

    private int page;

    private int pageSize;

    private int totalItems;

    private int totalPages;

    private List<DashboardQueueItemDTO> items = new ArrayList<>();

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

    public List<DashboardQueueItemDTO> getItems() {
        return items;
    }

    public void setItems(List<DashboardQueueItemDTO> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
