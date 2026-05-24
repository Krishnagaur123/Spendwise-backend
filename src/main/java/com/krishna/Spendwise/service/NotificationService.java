package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.ExpenseDto;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.repository.ProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled email notifications sent to all users daily.
 *
 * <ul>
 *   <li><b>22:00 IST</b> — reminder to log income and expenses for the day.</li>
 *   <li><b>23:00 IST</b> — HTML table summary of today's recorded expenses (skipped if none).</li>
 * </ul>
 *
 * Both jobs iterate over all profiles. For large user bases, this should be replaced
 * with a paginated or queue-based approach.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final ExpenseService expenseService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Scheduled(cron = "0 0 22 * * *", zone = "IST")
    public void sendDailyIncomeExpenseReminder() {
        log.info("Job started: sendDailyIncomeExpenseReminder()");
        List<ProfileEntity> profiles = profileRepository.findAll();
        for (ProfileEntity profile : profiles) {
            String body = "Hi " + profile.getFullName() + ",<br><br>"
                    + "This is a friendly reminder to add your income and expenses for today in Money Manager."
                    + "<br><br><a href=" + frontendUrl
                    + " style='display:inline-block;padding:10px 20px;background:#4f46e5;color:#fff;text-decoration:none;border-radius:6px'>Open App</a>"
                    + "<br><br>Best regards,<br>Money Manager Team";
            emailService.sendVerificationEmail(profile.getEmail(),
                    "Daily reminder: Add your income and expenses", body);
        }
        log.info("Job finished: sendDailyIncomeExpenseReminder()");
    }

    @Transactional
    @Scheduled(cron = "0 0 23 * * *", zone = "IST")
    public void sendDailyExpenseSummary() {
        log.info("Job started: sendDailyExpenseSummary()");
        List<ProfileEntity> profiles = profileRepository.findAll();
        for (ProfileEntity profile : profiles) {
            List<ExpenseDto> todaysExpenses = expenseService.getExpenseForUserOnDate(profile.getId(), LocalDate.now());
            if (todaysExpenses.isEmpty()) continue; // no email if nothing was logged

            StringBuilder table = new StringBuilder();
            table.append("<table style='border-collapse:collapse;width:100%;'>")
                 .append("<tr style='background-color:#f2f2f2;'>")
                 .append("<th style='border:1px solid #ddd;padding:8px;text-align:left;'>#</th>")
                 .append("<th style='border:1px solid #ddd;padding:8px;text-align:left;'>Name</th>")
                 .append("<th style='border:1px solid #ddd;padding:8px;text-align:right;'>Amount</th>")
                 .append("<th style='border:1px solid #ddd;padding:8px;text-align:left;'>Category ID</th>")
                 .append("</tr>");

            int i = 1;
            for (ExpenseDto expense : todaysExpenses) {
                table.append("<tr>")
                     .append("<td style='border:1px solid #ddd;padding:8px;'>").append(i++).append("</td>")
                     .append("<td style='border:1px solid #ddd;padding:8px;'>").append(escape(expense.getName())).append("</td>")
                     .append("<td style='border:1px solid #ddd;padding:8px;text-align:right;'>").append(expense.getAmount()).append("</td>")
                     .append("<td style='border:1px solid #ddd;padding:8px;'>").append(expense.getCategoryId()).append("</td>")
                     .append("</tr>");
            }
            table.append("</table>");

            emailService.sendVerificationEmail(profile.getEmail(), "Today's Expense Summary",
                    "<p>Hi " + profile.getFullName() + ",</p><p>Here is your expense summary for today.</p>" + table);
        }
    }

    /** Escapes HTML special characters to prevent XSS in user-provided expense names inside emails. */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

}
