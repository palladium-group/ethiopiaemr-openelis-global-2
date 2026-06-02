package org.openelisglobal.menu.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuController {

    private static final String RECEPTION_MENU_ELEMENT_ID = "menu_sample_reception";

    @Autowired
    private MenuService menuService;
    @Autowired
    private UserRoleService userRoleService;

    @GetMapping(value = "/rest/menu", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MenuItem> getMenuTree(HttpServletRequest request) {
        return filterReceptionMenuIfUnauthorized(MenuUtil.getMenuTree(), request);
    }

    @GetMapping(value = "/rest/menu/{elementId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Optional<MenuItem> getMenuTree(@PathVariable String elementId, HttpServletRequest request) {
        return findMenuItem(elementId, filterReceptionMenuIfUnauthorized(MenuUtil.getMenuTree(), request));
    }

    @PostMapping("/rest/menu")
    public List<MenuItem> postMenuTree(@RequestBody List<MenuItem> menuItems) {
        return menuService.save(menuItems);
    }

    @PostMapping("/rest/menu/{elementId}")
    public MenuItem postMenuTree(@PathVariable String elementId, @RequestBody MenuItem menuItem) {
        return menuService.save(menuItem);
    }

    private Optional<MenuItem> findMenuItem(String elementId, List<MenuItem> menuItems) {
        Queue<MenuItem> queue = new ArrayDeque<>();
        queue.addAll(menuItems);
        while (!queue.isEmpty()) {
            MenuItem menuItem = queue.remove();
            if (elementId.equals(menuItem.getMenu().getElementId())) {
                return Optional.of(menuItem);
            } else {
                for (MenuItem childMenuItem : menuItem.getChildMenus()) {
                    if (menuItem.getMenu().getElementId() != childMenuItem.getMenu().getElementId()) {
                        queue.add(childMenuItem); // prevent infinite loops if a menu option points to itself
                    }
                }
            }
        }
        return Optional.empty();
    }

    private List<MenuItem> filterReceptionMenuIfUnauthorized(List<MenuItem> menuItems, HttpServletRequest request) {
        if (userHasSampleReceptionApprovalRole(request)) {
            return menuItems;
        }
        return filterMenuItems(menuItems);
    }

    private List<MenuItem> filterMenuItems(List<MenuItem> menuItems) {
        List<MenuItem> filtered = new ArrayList<>();
        for (MenuItem menuItem : menuItems) {
            if (RECEPTION_MENU_ELEMENT_ID.equals(menuItem.getMenu().getElementId())) {
                continue;
            }
            MenuItem copied = new MenuItem();
            copied.setMenu(menuItem.getMenu());
            copied.setChildMenus(filterMenuItems(menuItem.getChildMenus()));
            filtered.add(copied);
        }
        return filtered;
    }

    private boolean userHasSampleReceptionApprovalRole(HttpServletRequest request) {
        UserSessionData userSessionData = (UserSessionData) request.getSession()
                .getAttribute(IActionConstants.USER_SESSION_DATA);
        if (userSessionData == null) {
            return false;
        }
        return userRoleService.userInRole(Integer.toString(userSessionData.getSystemUserId()),
                Constants.ROLE_SAMPLE_RECEPTION_APPROVAL);
    }
}
