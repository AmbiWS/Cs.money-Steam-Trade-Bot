import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

class SharkListGrabber {

    private final static String serviceName = "CSM List Withdraw"; // CSM List Withdraw
    static ArrayList<String> userSets = new ArrayList<>();

    static void fillUserListFromST(String listName, String tokenSession, String tokenXSRF,
                                           ArrayList<String> itemNameList, ArrayList<Float> itemNameListMaxPrice) {
        try {
            String linkWhereCheck = "https://shark.tools/profile/getSetByServiceName";
            String dataToPost = "title=" + serviceName + "&game=730";
            byte[] dataToPostBytes = dataToPost.getBytes();

            URL url = new URL(linkWhereCheck);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Length", String.valueOf(dataToPostBytes.length));
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
            connection.setRequestProperty("Cookie", tokenSession + "; " + tokenXSRF);
            connection.setDoOutput(true);

            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                dos.write(dataToPostBytes);
                dos.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 302) {
                JOptionPane.showMessageDialog(null, "Ошибка при попытке спарсить список пользователя из shark.tools!");
                System.exit(0);
            }

            String newLine;
            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"
            ))) {
                while ((newLine = in.readLine()) != null)
                    responseBuilder.append(newLine);
            }

            String responseData = responseBuilder.toString();
            String itemListNameByResponse = "\"set_name\":" + "\"" + listName + "\",";
            if (responseData.contains(itemListNameByResponse)) {

                responseData = responseData.substring(responseData.indexOf(itemListNameByResponse) + itemListNameByResponse.length());
                responseData = responseData.substring(0, responseData.indexOf("]}") + 1);

                while (responseData.contains("\"max_price\":")) {

                    responseData = responseData.substring(responseData.indexOf("\"max_price\":") + 12);

                    String tmp = responseData.substring(0, responseData.indexOf(","));
                    if (tmp.equals("null")) {
                        itemNameListMaxPrice.add(0f);
                    } else {
                        itemNameListMaxPrice.add(Float.parseFloat(tmp.replace(",", ".")));
                    }

                    responseData = responseData.substring(responseData.indexOf("\"name\":\"") + 8);
                    itemNameList.add(responseData.substring(0, responseData.indexOf("\",")));
                }
            } else {
                JOptionPane.showMessageDialog(null, "Ошибка при попытке получить выбранный список!");
                System.exit(0);
            }

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии получения списка пользователя на shark.tools, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }
    }

    static void loadUserListsSet(String tokenSession, String tokenXSRF) {
        try {
            userSets.clear();
            String linkWhereCheck = "https://shark.tools/profile/getSetByServiceName";
            String dataToPost = "title=" + serviceName + "&game=730";
            byte[] dataToPostBytes = dataToPost.getBytes();

            URL url = new URL(linkWhereCheck);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Length", String.valueOf(dataToPostBytes.length));
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
            connection.setRequestProperty("Cookie", tokenSession + "; " + tokenXSRF);
            connection.setDoOutput(true);

            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                dos.write(dataToPostBytes);
                dos.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 302) {
                JOptionPane.showMessageDialog(null, "Ошибка при попытке загрузить списки пользователя из shark.tools!");
                System.exit(0);
            }

            String newLine;
            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"
            ))) {
                while ((newLine = in.readLine()) != null)
                    responseBuilder.append(newLine);
            }

            ArrayList<String> setList = new ArrayList<>();
            String temp = responseBuilder.toString();
            if (!temp.contains("\"set_name\":")) {
                JOptionPane.showMessageDialog(null, "Создайте хотя-бы один список с предметами для 'CSM List Withdraw', а только потом запускайте бота!");
                System.exit(0);
            }

            while (temp.contains("\"set_name\":")) {
                temp = temp.substring(temp.indexOf("\"set_name\":") + 12);
                setList.add(temp.substring(0, temp.indexOf("\",\"item\":")));
            }

            for (String s: setList) {
                userSets.add(s);
            }

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии загрузки списков пользователя на shark.tools, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }
    }
}
