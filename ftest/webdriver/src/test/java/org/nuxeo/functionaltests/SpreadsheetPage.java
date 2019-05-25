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

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Spreadsheet page object
 *
 * @since 7.1
 */
public class SpreadsheetPage {

    private static final String HEADERS_XPATH = "thead/tr/th/div/span";

    private static final String ROWS_XPATH = "tbody/tr/*[1]";

    private final WebDriver driver;

    @Required
    @FindBy(xpath = "//table[@class=\"htCore\"]")
    WebElement table;

    @Required
    @FindBy(id = "query")
    WebElement queryInput;

    @FindBy(id = "execute")
    WebElement executeButton;

    @FindBy(id = "save")
    WebElement saveButton;

    @FindBy(id = "close")
    WebElement closeButton;

    @Required
    @FindBy(id = "console")
    WebElement console;

    public SpreadsheetPage(WebDriver driver) {
        this.driver = driver;
    }

    public List<WebElement> getHeaderElements() {
        return table.findElements(By.xpath(HEADERS_XPATH));
    }

    public List<String> getHeaders() {
        List<String> headers = new ArrayList<>();
        for (WebElement e : getHeaderElements()) {
            headers.add(e.getText());
        }
        return headers;
    }

    public List<WebElement> getRows() {
        return table.findElements(By.xpath(ROWS_XPATH));
    }

    public String getMessages() {
        return console.getText();
    }

    /**
     * Set data at given row and cell
     */
    public void setData(int row, int cell, Object data) {
        callHandsontable("setDataAtCell", row, cell, data);
    }

    /**
     * Get data at given row and cell
     */
    public Object getData(int row, int cell) {
        return callHandsontable("getDataAtCell", row, cell);
    }

    /**
     * Run a query
     */
    public void executeQuery(String query) {
        queryInput.sendKeys("SELECT * FROM Document");
        executeButton.click();
        waitForRequests();
    }

    /**
     * Save the data
     */
    public void save() {
        saveButton.click();
        waitForRequests();
    }

    /**
     * Close the spreadsheet
     */
    public void close() {
        closeButton.click();
    }

    /**
     * Wait for ajax requests to complete
     */
    protected void waitForRequests() {
        new AjaxRequestManager(driver).waitForJQueryRequests();
    }

    /**
     * Call a Handsontable method
     */
    protected Object callHandsontable(String method, Object... params) {
        return ((JavascriptExecutor) driver).executeScript(
            "return $('#grid').handsontable('" + method + "', " + StringUtils.join(params, ",") + ");");
    }

    /**
     * @since 8.4
     */
    public void waitReady() {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                AbstractTest.POLLING_FREQUENCY_MILLISECONDS, TimeUnit.MILLISECONDS);
        wait.until(driver -> ((JavascriptExecutor) driver).executeScript("return window.nuxeoSpreadsheetReady;"));
    }
}
