package com.example;

import com.lowagie.text.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LoanRestructureApplication {
    private static final Logger LOG = LoggerFactory.getLogger(LoanRestructureApplication.class);
    private static final String Excel_FILE = "/home/ankit/Downloads/loanRestructre/src/main/resources";

    public static void main(String[] args) throws IOException, DocumentException {

        LoanRestructureApplication thymeleaf2Pdf = new LoanRestructureApplication();
        thymeleaf2Pdf.parseThymeleaf();
    }

    private void parseThymeleaf() throws IOException, DocumentException {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();

        try {
            Class.forName("org.relique.jdbc.csv.CsvDriver");
            Connection conn = DriverManager.getConnection("jdbc:relique:csv:" + Excel_FILE);

            Statement stmt = conn.createStatement();
            Statement insideStmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT * FROM loan_detail");

            while (resultSet.next()) {
                String newLoanId = resultSet.getString("new_loan_id");
                context.setVariable("name", resultSet.getString("name"));
                context.setVariable("time", resultSet.getString("restructured_date"));
                context.setVariable("phone", resultSet.getString("phone"));
                context.setVariable("oldLoanId", resultSet.getString("old_loan_id"));
                context.setVariable("refNo", resultSet.getString("ref_id"));
                context.setVariable("newLoanId", resultSet.getString("new_loan_id"));
                context.setVariable("amount", resultSet.getString("amount_requested"));
                context.setVariable("interest", resultSet.getString("rate_of_interest"));
                context.setVariable("partnerName", resultSet.getString("nbfc_partner"));
                context.setVariable("tenure", resultSet.getString("tenure_in_months"));
                context.setVariable("fee", resultSet.getString("restructure_fee"));
                context.setVariable("gstFee", resultSet.getString("gst_restructure_fee"));
            List<LoanList> loanLists = new ArrayList<>();
            ResultSet amortRes = insideStmt.executeQuery("SELECT * FROM amort_detail WHERE new_loan_id ='" + newLoanId + "'");
            while (amortRes.next()) {
                loanLists.add(new LoanList(amortRes.getString("installment"), amortRes.getString("principal"), amortRes.getString("interest"), amortRes.getString("payment_due_date").replace("-", "/")));
            }
            context.setVariable("loanLists", loanLists);
            String html = templateEngine.process("templates/loan_restructure", context);

            generatePdfFromHtml(html, newLoanId);
            }
        } catch (SQLException | ClassNotFoundException e) {
            LOG.error("Unable to parse");
            e.printStackTrace();
        }
    }

    public void generatePdfFromHtml(String html, String newLoanId) throws IOException, DocumentException {
        String file = "src/main/resources/incred/loanRestructure_" + newLoanId + ".pdf";
        OutputStream outputStream = new FileOutputStream(file);

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(outputStream);

        GoogleDriveService.uploadToDrive(file);
        outputStream.close();
    }


}