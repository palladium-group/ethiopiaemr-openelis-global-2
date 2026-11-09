package org.openelisglobal.program.valueholder;

/**
 * Service categories that share the single pathology reception queue. The code
 * matches the {@code Program.code} seeded from {@code programs/*.json}, so a
 * category maps 1:1 onto a program dashboard's underlying case table.
 */
public enum ServiceCategory {

    HISTOPATHOLOGY("PATH", "Histopathology"), CYTOPATHOLOGY("CYTO", "Cytopathology");

    private final String code;
    private final String display;

    ServiceCategory(String code, String display) {
        this.code = code;
        this.display = display;
    }

    public String getCode() {
        return code;
    }

    public String getDisplay() {
        return display;
    }

    /**
     * @return the category whose code equals the program code (PATH, CYTO), or null
     *         for programs that are not a pathology service category (e.g. IHC)
     */
    public static ServiceCategory fromProgramCode(String programCode) {
        if (programCode == null) {
            return null;
        }
        for (ServiceCategory category : values()) {
            if (category.code.equals(programCode.trim())) {
                return category;
            }
        }
        return null;
    }
}
