import org.openqa.selenium.Cookie;
import org.openqa.selenium.chrome.ChromeDriver;
import javax.swing.*;
import java.util.concurrent.TimeUnit;

class SeleniumForSharktools {
    private ChromeDriver sharktoolsChromeDriver;
    private final static String serviceId = "18"; // CSM List Withdraw
    private static String sessionToken, XSRFToken;
    private static String userSteamId64 = "shark.tools";

    static String getUserSteamId64() {
        return userSteamId64;
    }

    static String getXSRFToken() {
        return XSRFToken;
    }

    static String getSessionToken() {
        return sessionToken;
    }

    static String getServiceId() {
        return serviceId;
    }

    SeleniumForSharktools() {
        sharktoolsChromeDriver = new ChromeDriver();
        connectToSharktools();
    }

    ChromeDriver getSharktoolsChromeDriver() {
        return sharktoolsChromeDriver;
    }

    private void connectToSharktools() {
        try {
            TimeUnit.SECONDS.sleep(3);
            sharktoolsChromeDriver.get("https://shark.tools/");
        } catch (Exception exc) {

            exc.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ошибка на стадии подключения к сайту shark.tools, выключение программы...");
            System.exit(0);

        }
    }

    void loadSteamId64() {
        String ps = sharktoolsChromeDriver.getPageSource();
        ps = ps.substring(ps.indexOf("<div class=\"info_value\" style=\"text-align: center;line-height: 34px;\">") + 70);
        userSteamId64 = ps.substring(0, ps.indexOf("</div>"));
        System.out.println("Your steamId64 is: " + userSteamId64);
    }

    void loadTokens() {
        try {
            Cookie sessionCookie = sharktoolsChromeDriver.manage().getCookieNamed("sharktools_session");
            sessionToken = sessionCookie.getName() + "=" + sessionCookie.getValue();

            Cookie XSRFCookie = sharktoolsChromeDriver.manage().getCookieNamed("XSRF-TOKEN");
            XSRFToken = XSRFCookie.getName() + "=" + XSRFCookie.getValue();

            System.out.print("\n---\n" +
                    "Shark.Tools tokens was successfully loaded!" +
                    "\n---\n");
        } catch (Exception exc) {

            exc.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ошибка на стадии сохранения сессии shark.tools, выключение программы...");
            System.exit(0);

        }
    }
}
