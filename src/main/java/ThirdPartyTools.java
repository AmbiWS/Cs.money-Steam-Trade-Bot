import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;

class ThirdPartyTools {

    synchronized void refreshCsmINFO() {
        SForm.getSeleniumForSharktools().getSharktoolsChromeDriver().get("https://cs.money/ru");
        SForm.getSeleniumForCsm().loadCsrfTokenHeader(SForm.getSeleniumForSharktools().getSharktoolsChromeDriver());
        SForm.getSeleniumForCsm().loadTokens(SForm.getSeleniumForSharktools().getSharktoolsChromeDriver());
    }

    void setupIcon(String pathName, JFrame jf) {
        try {
            jf.setIconImage(ImageIO.read(new File(pathName)));
        } catch (Exception exc) {

            exc.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ошибка на стадии загрузки графических файлов, выключение программы...");
            System.exit(0);

        }
    }

    synchronized boolean checkSharkSub() {
        boolean sharktoolsSubscription = false;

        try {
            String link = "https://shark.tools/profile/getServiceToken";
            String dataToPost = "service_id=" + SeleniumForSharktools.getServiceId();
            byte[] dataToPostBytes = dataToPost.getBytes();

            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
            connection.setRequestProperty("Content-Length", String.valueOf(dataToPostBytes.length));
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("Cookie", SeleniumForSharktools.getSessionToken() + "; " + SeleniumForSharktools.getXSRFToken());
            connection.setDoOutput(true);

            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                dos.write(dataToPostBytes);
                dos.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {

                if (SForm.lmf != null) {

                    return false;

                } else {

                    JOptionPane.showMessageDialog(null, "Ошибка при подключении к серверам shark.tools, выключение программы...");
                    System.exit(0);

                }

            }

            StringBuilder responseBuilder = new StringBuilder();
            String newLine;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()))) {
                while ((newLine = in.readLine()) != null)
                    responseBuilder.append(newLine);
            }

            String successValue = responseBuilder.toString().substring(
                    responseBuilder.toString().indexOf("\"success\":") + 10,
                    responseBuilder.toString().indexOf(","));

            sharktoolsSubscription = Boolean.parseBoolean(successValue);

        } catch (Exception exc) {

            if (SForm.lmf != null) {

                return false;

            } else {

                exc.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка при подключении к серверам shark.tools, выключение программы...");
                System.exit(0);

            }
        }

        return sharktoolsSubscription;
    }

    void startApp(JButton b1, JButton b2) {
        System.out.println("Enabling application...");
        b1.setEnabled(false);
        b2.setEnabled(false);
    }

    void stopApp(Timer t1, Timer t2, Timer t3, CsmSocketFullParser csfp, JButton b1, JButton b2) {
        csfp.isStopped = true;
        t1.cancel();
        t2.cancel();
        t3.cancel();
        b1.setEnabled(true);
        b2.setEnabled(false);
    }
}
