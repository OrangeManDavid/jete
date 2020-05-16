package com.tester.jete.i.impl;

import com.tester.jete.i.SelenideElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.concurrent.locks.Condition;

import static java.util.Collections.singletonList;

/**
 * @ClassName WebElementSource
 * @Description TODO
 * @Author David.Jackson.Lyd@gmail.com
 * @Date: 2020/05/16 18:01
 */
public class WebElementSource {
    public abstract Driver driver();
    public abstract WebElement getWebElement();

    public abstract String getSearchCriteria();

    public SelenideElement find(SelenideElement proxy, Object arg, int index) {
        return ElementFinder.wrap(driver(), proxy, getSelector(arg), index);
    }

    public List<WebElement> findAll() throws IndexOutOfBoundsException {
        return singletonList(getWebElement());
    }

    public ElementNotFound createElementNotFoundError(Condition condition, Throwable lastError) {
        return new ElementNotFound(driver(), getSearchCriteria(), condition, lastError);
    }

    public static By getSelector(Object arg) {
        return arg instanceof By ? (By) arg : By.cssSelector((String) arg);
    }

    public WebElement checkCondition(String prefix, Condition condition, boolean invert) {
        Condition check = invert ? not(condition) : condition;

        Throwable lastError = null;
        WebElement element = null;
        try {
            element = getWebElement();
            if (element != null && check.apply(driver(), element)) {
                return element;
            }
        }
        catch (WebDriverException | IndexOutOfBoundsException | AssertionError e) {
            lastError = e;
        }

        if (Cleanup.of.isInvalidSelectorError(lastError)) {
            throw Cleanup.of.wrap(lastError);
        }

        if (element == null) {
            if (!check.applyNull()) {
                throw createElementNotFoundError(check, lastError);
            }
        }
        else if (invert) {
            throw new ElementShouldNot(driver(), getSearchCriteria(), prefix, condition, element, lastError);
        }
        else {
            throw new ElementShould(driver(), getSearchCriteria(), prefix, condition, element, lastError);
        }
        return null;
    }

    /**
     * Asserts that returned element can be interacted with.
     *
     * Elements which are transparent (opacity:0) are considered to be invisible, but interactable.
     * User (as of 05.12.2018) can click, doubleClick etc., and enter text etc. to transparent elements
     * for all major browsers
     *
     * @return element or throws ElementShould/ElementShouldNot exceptions
     */
    public WebElement findAndAssertElementIsInteractable() {
        return checkCondition("be ",
                or("visible or transparent", visible, have(cssValue("opacity", "0"))),
                false);
    }
}