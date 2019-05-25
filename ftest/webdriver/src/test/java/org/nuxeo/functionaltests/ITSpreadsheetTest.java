/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.functionaltests.contentView.ContentViewElement.ResultLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.contentView.ContentViewElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Nuxeo Spreadsheet Tests
 *
 * @since 7.1
 */
public class ITSpreadsheetTest extends AbstractTest {

    private final static String SPREADSHEET_ACTION_TITLE = "Spreadsheet";

    private final static String[] STANDALONE_COLUMNS = new String[] { "Title", "Modified", "Last Contributor", "State" };

    private static final String IFRAME_XPATH = "//iframe[starts-with(@src, '/nuxeo/spreadsheet')]";

    private final static String WORKSPACE_TITLE = "WorkspaceSpreadsheet_" + new Date().getTime();

    @Before
    public void setUp() throws DocumentBasePage.UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();

        // Create test File
        DocumentBasePage workspacePage = documentBasePage.createWorkspace(WORKSPACE_TITLE, null);

        workspacePage.createFile("Test file", "Test File description", false, null, null, null);

        logout();
    }

    @Test
    public void itShouldBeAvailableInWorkspace() throws DocumentBasePage.UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();
        DocumentBasePage workspacesPage = documentBasePage.getNavigationSubPage().goToDocument("Workspaces");
        workspacesPage.getNavigationSubPage().goToDocument(WORKSPACE_TITLE);

        ContentViewElement contentView = getWebFragment(By.id("cv_document_content_0_panel"), ContentViewElement.class);

        contentView = contentView.switchToResultLayout(ResultLayout.LISTING);
        assertNotNull(contentView.getActionByTitle(SPREADSHEET_ACTION_TITLE));

        logout();
    }

    @Test
    public void itShouldBeAvailableInSearch() throws DocumentBasePage.UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();
        documentBasePage.goToSearchPage();

        ContentViewElement contentView = getWebFragment(By.id("nxw_searchContentView"), ContentViewElement.class);

        contentView.switchToResultLayout(ResultLayout.LISTING);
        assertNotNull(contentView.getActionByTitle(SPREADSHEET_ACTION_TITLE));

        logout();
    }

    @Test
    public void itShouldBeAvailableStandalone() throws DocumentBasePage.UserNotConnectedException, IOException {
        login();

        navToUrl(NUXEO_URL + "/spreadsheet");
        SpreadsheetPage spreadsheet = asPage(SpreadsheetPage.class);

        // Run a query
        spreadsheet.executeQuery("SELECT * FROM Document");

        // Should display the default columns
        List<String> headers = spreadsheet.getHeaders();
        headers.remove(0); // Remove first column
        assertEquals(Arrays.asList(STANDALONE_COLUMNS), headers);

        // Should have the same rows
        List<WebElement> rows = spreadsheet.getRows();
        assertTrue(rows.size() > 0);

        logout();
    }

    @Test
    public void itShouldDisplayTheSameData() throws DocumentBasePage.UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();

        DocumentBasePage workspacesPage = documentBasePage.getNavigationSubPage().goToDocument("Workspaces");
        workspacesPage.getNavigationSubPage().goToDocument(WORKSPACE_TITLE);

        ContentViewElement contentView = getWebFragment(By.id("cv_document_content_0_panel"), ContentViewElement.class);

        contentView.switchToResultLayout(ResultLayout.LISTING);
        contentView = getWebFragment(By.id("cv_document_content_0_panel"), ContentViewElement.class);
        List<String> resultColumns = getContentViewColumns(contentView);

        SpreadsheetPage spreadsheet = openSpreadsheet(contentView);

        String query = spreadsheet.queryInput.getAttribute("value");
        assertNotNull(query);

        // Should display the same columns
        List<String> headers = spreadsheet.getHeaders();
        headers.remove(0); // Remove first column
        assertEquals(resultColumns, headers);

        // Should have the same rows
        List<WebElement> rows = spreadsheet.getRows();
        assertEquals(1, rows.size());
    }

    @Test
    public void itShouldAllowUpdatingData() throws DocumentBasePage.UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();

        DocumentBasePage workspacesPage = documentBasePage.getNavigationSubPage().goToDocument("Workspaces");
        workspacesPage.getNavigationSubPage().goToDocument(WORKSPACE_TITLE);

        ContentViewElement contentView = getWebFragment(By.id("cv_document_content_0_panel"), ContentViewElement.class);

        SpreadsheetPage spreadsheet = openSpreadsheet(contentView);

        // First column should be the title
        List<String> headers = spreadsheet.getHeaders();
        assertEquals("Title", headers.get(1));

        // Change the title, save and close the spreadsheet
        spreadsheet.setData(0, 0, "'New Title'");
        assertEquals("New Title", spreadsheet.getData(0, 0));

        spreadsheet.save();

        assertEquals("1 rows saved", spreadsheet.getMessages());

        closeSpreadsheet(spreadsheet);

        // Check that the title was updated
        contentView = getWebFragment(By.id("cv_document_content_0_panel"), ContentViewElement.class);

        WebElement row = getContentViewRows(contentView).get(0);
        String title = row.findElement(By.className("documentTitle")).getText();

        assertEquals("New Title", title);
    }

    @After
    public void tearDown() throws DocumentBasePage.UserNotConnectedException {
        DocumentBasePage documentBasePage = login();

        documentBasePage.deleteWorkspace(WORKSPACE_TITLE);

        logout();
    }

    /**
     * opens the spreadsheet popup
     */
    private SpreadsheetPage openSpreadsheet(ContentViewElement cv) {
        WebElement spreadsheetAction = cv.switchToResultLayout(ResultLayout.LISTING).getActionByTitle(
                SPREADSHEET_ACTION_TITLE);
        assertNotNull(spreadsheetAction);

        spreadsheetAction.click();

        WebElement iFrame = Locator.findElementWithTimeout(By.xpath(IFRAME_XPATH));
        assertNotNull(iFrame);
        driver.switchTo().frame(iFrame);

        SpreadsheetPage spreadsheet = new SpreadsheetPage(driver);

        // wait for ajax requests and render to complete
        spreadsheet.waitReady();

        // fill the page object
        fillElement(SpreadsheetPage.class, spreadsheet);

        return spreadsheet;
    }

    private void closeSpreadsheet(SpreadsheetPage spreadsheet) {
        spreadsheet.close();
        driver.switchTo().defaultContent();
        AjaxRequestManager ajax = new AjaxRequestManager(driver);
        ajax.watchAjaxRequests();
        ajax.waitForAjaxRequests();
    }

    /**
     * returns the names of the content view's result columns
     */
    private List<String> getContentViewColumns(ContentViewElement cv) {
        List<String> headers = new ArrayList<>();
        WebElement dataOutput = cv.findElement(By.className("dataOutput"));
        List<WebElement> cvHeaders = dataOutput.findElements(By.className("colHeader"));
        for (WebElement e : cvHeaders) {
            headers.add(e.getText());
        }
        return headers;
    }

    /**
     * returns the content view rows
     */
    private List<WebElement> getContentViewRows(ContentViewElement cv) {
        return cv.findElements(By.cssSelector(".dataOutput > tbody > tr"));
    }
}
