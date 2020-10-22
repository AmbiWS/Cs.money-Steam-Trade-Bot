import org.apache.commons.io.IOUtils;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.chrome.ChromeDriver;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class SeleniumForCsm {
    private static String sessionToken;
    private static String userIdToken;
    private static String userId;
    private static String userNameToken;
    private static String csrfToken;
    private static String csrfTokenHeader;
    private static String cfclearanceToken = null;

    static String getUserId() {
        return userId;
    }

    static String getCfclearanceToken() {
        return cfclearanceToken;
    }

    private static void setCsrfTokenHeader(String csrfTokenHeader) {
        SeleniumForCsm.csrfTokenHeader = csrfTokenHeader;
        System.out.println("New CSRF-Token is ~> " + SeleniumForCsm.csrfTokenHeader + ".\r\n");
    }

    private static String offer_id = "shark.tools";

    private static String getOffer_id() {
        return offer_id;
    }

    static void setOffer_id(String offer_id) {
        if (SeleniumForCsm.offer_id.equals(offer_id)) return;

        SeleniumForCsm.offer_id = offer_id;
        System.out.println("New offer ID ~> " + SeleniumForCsm.offer_id + ".");

        try {

            String requestTo = "https://cs.money/confirm_virtual_offer";

            String steamId64 = SeleniumForCsm.getUserIdToken().substring(SeleniumForCsm.getUserIdToken()
                    .indexOf("=") + 1, SeleniumForCsm.getUserIdToken().length());

            String offerId = SeleniumForCsm.getOffer_id();

            String urlParameters = "{\"action\":\"confirm\",\"steamid64\":\"" +
                    steamId64
                    + "\",\"offer_id\":\"" +
                    offerId
                    + "\"}";

            System.out.println("Send data to confirm virtual offer ~> " + urlParameters);

            byte[] postData = urlParameters.getBytes();
            int contentLength = postData.length;

            URL url = new URL(requestTo);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36");

            connection.setRequestProperty("Content-type", "application/json");
            connection.setRequestProperty("Content-Length", String.valueOf(contentLength));

            if (SeleniumForCsm.getCfclearanceToken() == null) {
                connection.setRequestProperty("Cookie", "type_device=desktop; " +
                        SeleniumForCsm.getUserIdToken() + "; " +
                        SeleniumForCsm.getSessionToken() + "; " +
                        SeleniumForCsm.getCsrfToken());
            } else {
                connection.setRequestProperty("Cookie", "type_device=desktop; " +
                        SeleniumForCsm.getUserIdToken() + "; " +
                        SeleniumForCsm.getSessionToken() + "; " +
                        SeleniumForCsm.getCsrfToken() + "; " +
                        SeleniumForCsm.getCfclearanceToken());
            }

            try (DataOutputStream dos = new DataOutputStream(
                    connection.getOutputStream()
            )) {
                dos.write(postData);
                dos.flush();

                int responseCode = connection.getResponseCode();
                System.out.println("Response code -> " + responseCode);

                String str = IOUtils.toString(new InputStreamReader(connection.getInputStream()));
                System.out.println("Response: " + str);

                connection.disconnect();
            }

            BuyCsmItem.isReadyToVirtualOffer = true;

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии подтверждения виртуального обмена, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }

    }

    static String getCsrfToken() {
        return csrfToken;
    }

    static String getCsrfTokenHeader() {
        return csrfTokenHeader;
    }

    static String getUserIdToken() {
        return userIdToken;
    }

    static String getSessionToken() {
        return sessionToken;
    }

    static String getUserNameToken() {
        return userNameToken;
    }

    void loadCsrfTokenHeader(ChromeDriver driver) {
        try {

            String ps = driver.getPageSource();
            ps = ps.substring(ps.indexOf("<meta id=\"csrf_token\"") + 42);
            ps = ps.substring(0, ps.indexOf("\""));
            setCsrfTokenHeader(ps);

        } catch (Exception exc) {

            if (SForm.lmf != null) {

                SForm.lmf.logTextArea.append("\r\n\r\n" +
                        "Ошибка на стадии получения CSRF-Token CSM, остановка программы...");
                exc.printStackTrace();
                SForm.lmf.stopButton.doClick();

            } else {

                exc.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка на стадии получения CSRF-Token CSM, выключение программы...");
                System.exit(0);

            }
        }
    }

    void loadTokens(ChromeDriver driver) {
        try {
            Cookie sessionCookie = driver.manage().getCookieNamed("csgo_ses");
            sessionToken = sessionCookie.getName() + "=" + sessionCookie.getValue();

            Cookie userIdCookie = driver.manage().getCookieNamed("steamid");
            userIdToken = userIdCookie.getName() + "=" + userIdCookie.getValue();
            userId = userIdCookie.getValue();

            Cookie userNameCookie = driver.manage().getCookieNamed("username");
            userNameToken = userNameCookie.getValue();

            Cookie csrfCookie = driver.manage().getCookieNamed("_csrf");
            csrfToken = csrfCookie.getName() + "=" + csrfCookie.getValue();

            if (driver.manage().getCookieNamed("cf_clearance") != null) {
                Cookie cflcCookie = driver.manage().getCookieNamed("cf_clearance");
                cfclearanceToken = cflcCookie.getName() + "=" + cflcCookie.getValue();
            } else cfclearanceToken = null;

            System.out.print("---\n" +
                    "CSM Session was successfully saved!" +
                    "\n---\n\n");
        } catch (Exception exc) {

            if (SForm.lmf != null) {

                SForm.lmf.logTextArea.append("\r\n\r\n" +
                        "Ошибка на стадии сохранения сессии CSM, остановка программы...");
                exc.printStackTrace();
                SForm.lmf.stopButton.doClick();

            } else {

                exc.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка на стадии сохранения сессии CSM, выключение программы...");
                System.exit(0);

            }
        }
    }
}
