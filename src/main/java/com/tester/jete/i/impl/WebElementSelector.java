package com.tester.jete.i.impl;

import com.tester.jete.i.Driver;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.By.ByCssSelector;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.tester.jete.e.SelectorMode.CSS;
import static java.lang.Thread.currentThread;

/**
 * @ClassName WebElementSelector
 * @Description TODO
 * @Author David.Jackson.Lyd@gmail.com
 * @Date: 2020/05/16 19:01
 */
public class WebElementSelector {
    public static WebElementSelector instance = new WebElementSelector();

    protected String sizzleSource;

    public WebElement findElement(Driver driver, SearchContext context, By selector) {
        checkThatXPathNotStartingFromSlash(context, selector);

        if (driver.config().selectorMode() == CSS || !(selector instanceof ByCssSelector)) {
            return context.findElement(selector);
        }

        List<WebElement> webElements = evaluateSizzleSelector(driver, context, (ByCssSelector) selector);
        return webElements.isEmpty() ? null : webElements.get(0);
    }

    public List<WebElement> findElements(Driver driver, SearchContext context, By selector) {
        checkThatXPathNotStartingFromSlash(context, selector);

        if (driver.config().selectorMode() == CSS || !(selector instanceof ByCssSelector)) {
            return context.findElements(selector);
        }

        return evaluateSizzleSelector(driver, context, (ByCssSelector) selector);
    }

    protected void checkThatXPathNotStartingFromSlash(SearchContext context, By selector) {
        if (context instanceof WebElement) {
            if (selector instanceof By.ByXPath) {
                if (selector.toString().startsWith("By.xpath: /")) {
                    throw new IllegalArgumentException("XPath starting from / searches from root");
                }
            }
        }
    }

    protected List<WebElement> evaluateSizzleSelector(Driver driver, SearchContext context, ByCssSelector sizzleCssSelector) {
        injectSizzleIfNeeded(driver);

        String sizzleSelector = sizzleCssSelector.toString()
                .replace("By.selector: ", "")
                .replace("By.cssSelector: ", "");

        if (context instanceof WebElement)
            return driver.executeJavaScript("return Sizzle(arguments[0], arguments[1])", sizzleSelector, context);
        else
            return driver.executeJavaScript("return Sizzle(arguments[0])", sizzleSelector);
    }

    protected void injectSizzleIfNeeded(Driver driver) {
        if (!sizzleLoaded(driver)) {
            injectSizzle(driver);
        }
    }

    protected Boolean sizzleLoaded(Driver driver) {
        try {
            return driver.executeJavaScript("return typeof Sizzle != 'undefined'");
        } catch (WebDriverException e) {
            return false;
        }
    }

    protected synchronized void injectSizzle(Driver driver) {
        if (sizzleSource == null) {
            try {
                sizzleSource = IOUtils.toString(currentThread().getContextClassLoader().getResource("sizzle.js"), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Cannot load sizzle.js from classpath", e);
            }
        }
        driver.executeJavaScript(sizzleSource);
    }
}
