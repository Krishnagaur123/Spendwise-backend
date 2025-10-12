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
                    "Daily reminder: Add your income and expenses",
                    body);
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
            if (!todaysExpenses.isEmpty()) {
                StringBuilder table = new StringBuilder();
                table.append("<table style='border-collapse:collapse;width:100%;'>");
                table.append("<tr style='background-color:#f2f2f2;'>"
                        + "<th style='border:1px solid #ddd;padding:8px;text-align:left;'>#</th>"
                        + "<th style='border:1px solid #ddd;padding:8px;text-align:left;'>Name</th>"
                        + "<th style='border:1px solid #ddd;padding:8px;text-align:right;'>Amount</th>"
                        + "<th style='border:1px solid #ddd;padding:8px;text-align:left;'>Category ID</th>"
                        + "</tr>");
                int i = 1;
                for (ExpenseDto expense : todaysExpenses) {
                    table.append("<tr>");
                    table.append("<td style='border:1px solid #ddd;padding:8px;'>").append(i++).append("</td>");
                    table.append("<td style='border:1px solid #ddd;padding:8px;'>").append(escape(expense.getName())).append("</td>");
                    table.append("<td style='border:1px solid #ddd;padding:8px;text-align:right;'>").append(expense.getAmount()).append("</td>");
                    table.append("<td style='border:1px solid #ddd;padding:8px;'>").append(expense.getCategoryId()).append("</td>");
                    table.append("</tr>");
                }
                table.append("</table>");

                String subject = "Today's Expense Summary";
                String body = "<p>Hi " + profile.getFullName() + ",</p>"
                        + "<p>Here is the summary of today's expenses.</p>"
                        + table;
                emailService.sendVerificationEmail(profile.getEmail(), subject, body);
            }
        }
    }

    // Utility to escape HTML entities to avoid breaking markup in names
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

}
