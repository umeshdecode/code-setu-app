package com.cargosetu.notification.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Builds the recurring "success card" EMAIL body fragment (checkmark badge, heading,
 * intro text, an optional key/value detail card, and an optional CTA button) from
 * structured inputs, so individual event templates don't hand-write/duplicate the
 * underlying HTML markup. The returned fragment is dropped into the shared
 * `templates/email/layout.html` content slot by TemplateRenderingServiceImpl.
 */
public final class EmailTemplateFragments {

    private EmailTemplateFragments() {
    }

    /**
     * One row of the detail card, e.g. ("Full Name", "{{name}}").
     *
     * @param label        the left-hand label text
     * @param valueMarkup  the right-hand value — a {{var}} placeholder (or literal text)
     * @param emphasized   true to render the value in the brand color + monospace font
     *                     (use for identifiers like usernames/IDs), false for plain text
     */
    public record DetailRow(String label, String valueMarkup, boolean emphasized) {
        public static DetailRow plain(String label, String valueMarkup) {
            return new DetailRow(label, valueMarkup, false);
        }

        public static DetailRow emphasized(String label, String valueMarkup) {
            return new DetailRow(label, valueMarkup, true);
        }
    }

    /**
     * Builds a success-card EMAIL body: checkmark badge + heading + intro text,
     * an optional detail card (omitted when {@code rows} is null/empty), and an
     * optional CTA button that only renders when the {@code loginUrl} variable is
     * present on the event (via {@code th:if="${loginUrl}"}).
     *
     * @param heading    the success heading, e.g. "Account created successfully"
     * @param introHtml  intro paragraph content (may include inline HTML like <strong>)
     * @param rows       detail card rows, in display order; pass null/empty to skip the card
     * @param ctaLabel   CTA button label, e.g. "Sign in to your account"; pass null to skip the button
     * @return the assembled HTML fragment
     */
    public static String successCard(String heading, String introHtml, List<DetailRow> rows, String ctaLabel) {
        StringBuilder html = new StringBuilder();
        html.append("<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">");

        html.append("<tr><td align=\"center\" style=\"padding-bottom:15px;\">")
                .append("<div style=\"display:inline-block; background-color:#dcfce7; color:#16a34a; width:36px; height:36px; border-radius:50%; text-align:center; line-height:36px; font-size:18px; font-weight:bold;\">&#10003;</div>")
                .append("</td></tr>");

        html.append("<tr><td align=\"center\"><h1 style=\"margin:0; font-size:22px; font-weight:700; color:#0f172a;\">")
                .append(heading)
                .append("</h1></td></tr>");

        html.append("<tr><td align=\"center\" style=\"padding:15px 0 25px 0;\">")
                .append("<p style=\"margin:0; font-size:15px; line-height:1.6; color:#475569;\">")
                .append(introHtml)
                .append("</p></td></tr>");

        if (rows != null && !rows.isEmpty()) {
            html.append("<tr><td>").append(detailCard(rows)).append("</td></tr>");
        }

        if (ctaLabel != null && !ctaLabel.isBlank()) {
            html.append("<tr th:if=\"${loginUrl}\"><td align=\"center\" style=\"padding-top:25px;\">")
                    .append("<a th:href=\"${loginUrl}\" style=\"background-color:#2563eb; border-radius:8px; color:#ffffff; display:inline-block; font-size:15px; font-weight:600; line-height:50px; text-align:center; text-decoration:none; width:100%;\">")
                    .append(ctaLabel)
                    .append("</a></td></tr>");
        }

        html.append("</table>");
        return html.toString();
    }

    private static String detailCard(List<DetailRow> rows) {
        int lastIndex = rows.size() - 1;
        String rowsHtml = IntStream.range(0, rows.size())
                .mapToObj(i -> detailRow(rows.get(i), i < lastIndex))
                .collect(Collectors.joining());

        return "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color:#f8fafc; border:1px solid #e2e8f0; border-radius:10px; padding:20px;\">"
                + rowsHtml
                + "</table>";
    }

    private static String detailRow(DetailRow row, boolean withBottomBorder) {
        String valueStyle = row.emphasized()
                ? "font-size:14px; font-weight:600; color:#2563eb; font-family:monospace;"
                : "font-size:14px; font-weight:600; color:#0f172a;";
        String cellStyle = withBottomBorder ? "padding:12px 0; border-bottom:1px solid #e2e8f0;" : "padding:12px 0 0 0;";

        return "<tr><td style=\"" + cellStyle + "\">"
                + "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\"><tr>"
                + "<td style=\"font-size:13px; font-weight:500; color:#64748b;\">" + row.label() + "</td>"
                + "<td align=\"right\" style=\"" + valueStyle + "\">" + row.valueMarkup() + "</td>"
                + "</tr></table></td></tr>";
    }
}
