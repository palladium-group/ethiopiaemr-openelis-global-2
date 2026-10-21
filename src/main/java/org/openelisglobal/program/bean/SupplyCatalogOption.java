package org.openelisglobal.program.bean;

/**
 * An OpenMRS concept offered in the supply search or the quantity-unit list.
 */
public class SupplyCatalogOption {

    private String uuid;
    private String display;

    public SupplyCatalogOption() {
    }

    public SupplyCatalogOption(String uuid, String display) {
        this.uuid = uuid;
        this.display = display;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }
}
