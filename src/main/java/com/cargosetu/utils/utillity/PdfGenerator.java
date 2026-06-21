package com.cargosetu.utils.utillity;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PdfGenerator {
    public byte[] generatePdf(String html) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setArgs(List.of("--no-sandbox"))
            );
            Page page = browser.newPage();
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            int height = ((Number) page.evaluate(
                    "() => Math.ceil(document.documentElement.scrollHeight)"
            )).intValue();
            byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setWidth("210mm")
                    .setHeight(height + "px")
                    .setMargin(new Margin().setTop("0mm").setBottom("0mm").setLeft("0mm").setRight("0mm")));

            browser.close();
            return pdf;
        }
    }
}
