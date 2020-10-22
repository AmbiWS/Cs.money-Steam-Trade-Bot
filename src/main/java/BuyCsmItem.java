import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

class BuyCsmItem {

    static Float overallBuyValue = 0.00f;
    static boolean isReadyToVirtualOffer = false;
    static int countOfTries;

    synchronized void buy(String itemName, String itemPrice, String itemId, String itemHoldTime,
                          String itemBotValue, String itemFloat, JTextArea logTextArea,
                          boolean isFromMarket, String customPrice, JTextField statusTF,
                          String itemJsID, String gameId, String gemsCount) {
        try {
            countOfTries = 0;
            isReadyToVirtualOffer = false;

            String reality;

            if (!isFromMarket) reality = "\"reality\":\"physical\",";
            else reality = "\"reality\":\"virtual\",";

            String finalPrice;
            if (!isFromMarket) {
                 finalPrice = itemPrice;
            } else {
                finalPrice = customPrice;
            }

            if (itemFloat == null) itemFloat = "0";
            else itemFloat = "\"" + itemFloat + "\"";

            String urlParameters = "";
            if (gameId.equals("730")) {
                if (isFromMarket) {

                    urlParameters = "{\"peopleItems\":[],\"botItems\":[{" +
                            "\"assetid\":\"" + itemId + "\"," +
                            "\"local_price\":" + itemPrice + "," +
                            "\"price\":" + finalPrice + "," +
                            "\"hold_time\":" + itemHoldTime + "," +
                            "\"market_hash_name\":\"" + itemName + "\"," +
                            "\"bot\":\"" + itemBotValue + "\"," +
                            reality +
                            "\"currency\":\"USD\"," +
                            "\"username\":\"" + SeleniumForCsm.getUserNameToken() + "\"," +
                            "\"appid\":730," +
                            "\"name_id\":" + itemJsID + "," +
                            "\"float\":" + itemFloat + "," +
                            "\"stickers_count\":0," +
                            "\"custom_price\":" + finalPrice + "}]," +
                            "\"onWallet\":-" + finalPrice + "," +
                            "\"forceVirtual\":0}";
                } else {

                    urlParameters = "{\"peopleItems\":[],\"botItems\":[{" +
                            "\"assetid\":\"" + itemId + "\"," +
                            "\"local_price\":" + itemPrice + "," +
                            "\"price\":" + finalPrice + "," +
                            "\"hold_time\":" + itemHoldTime + "," +
                            "\"market_hash_name\":\"" + itemName + "\"," +
                            "\"bot\":\"" + itemBotValue + "\"," +
                            reality +
                            "\"currency\":\"USD\"," +
                            "\"username\":\"" + SeleniumForCsm.getUserNameToken() + "\"," +
                            "\"appid\":730," +
                            "\"name_id\":" + itemJsID + "," +
                            "\"float\":" + itemFloat + "," +
                            "\"stickers_count\":0}]," +
                            "\"onWallet\":-" + finalPrice + "," +
                            "\"forceVirtual\":0}";
                }
            } else if (gameId.equals("570")) {

                if (isFromMarket) {
                    urlParameters = "{\"peopleItems\":[],\"botItems\":[{" +
                            "\"assetid\":\"" + itemId + "\"," +
                            "\"local_price\":" + itemPrice + "," +
                            "\"price\":" + finalPrice + "," +
                            "\"hold_time\":null," +
                            "\"market_hash_name\":\"" + itemName + "\"," +
                            "\"bot\":\"" + itemBotValue + "\"," +
                            reality +
                            "\"currency\":\"USD\"," +
                            "\"username\":\"" + SeleniumForCsm.getUserNameToken() + "\"," +
                            "\"appid\":570," +
                            "\"name_id\":" + itemJsID + "," +
                            "\"gems_count\":" + gemsCount + "," +
                            "\"custom_price\":" + finalPrice +"}]," +
                            "\"onWallet\":-" + finalPrice +"," +
                            "\"forceVirtual\":0}";
                } else {

                    urlParameters = "{\"peopleItems\":[],\"botItems\":[{" +
                            "\"assetid\":\"" + itemId +"\"," +
                            "\"local_price\":" + itemPrice +"," +
                            "\"price\":" + finalPrice + "," +
                            "\"hold_time\":null," +
                            "\"market_hash_name\":\"" + itemName +"\"," +
                            "\"bot\":\"" + itemBotValue + "\"," +
                            reality +
                            "\"currency\":\"USD\"," +
                            "\"username\":\"" + SeleniumForCsm.getUserNameToken() + "\"," +
                            "\"appid\":570," +
                            "\"name_id\":" + itemJsID + "," +
                            "\"gems_count\":" + gemsCount + "}]," +
                            "\"onWallet\":-" + finalPrice + "," +
                            "\"forceVirtual\":0}";
                }
            }

            System.out.println("Send data to buy item ~> " + urlParameters);

            byte[] postData = urlParameters.getBytes();
            int contentLength = postData.length;

            String requestTo = "https://cs.money/send_offer";

            URL url = new URL(requestTo);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36");

            connection.setRequestProperty("Content-type", "application/json");
            connection.setRequestProperty("Content-Length", String.valueOf(contentLength));
            connection.setRequestProperty("CSRF-Token", SeleniumForCsm.getCsrfTokenHeader());

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

            String str;
            try (DataOutputStream dos = new DataOutputStream(
                    connection.getOutputStream()
            )) {
                dos.write(postData);
                dos.flush();

                int responseCode = connection.getResponseCode();
                System.out.println("Response code -> " + responseCode);

                str = IOUtils.toString(new InputStreamReader(connection.getInputStream()));
                System.out.println("Response: " + str);
                logTextArea.append("\r\nResponse: ");
                logTextArea.append(str);

                connection.disconnect();
            }

            if (str.contains(":false")) {
                SForm.lmf.logTextArea.append("\r\n\r\n" +
                        "Бот словил невалидный запрос, перезапуск программы..." +
                        "\r\n" +
                        "Текущее состояние: " +
                        SForm.lmf.statusTF.getText());

                ListModeForm.csfp.isStopped = true;
                ListModeForm.coids.isStopped = true;

                SForm.lmf.rt();

                return;
            }

            /*
                   Accept virtual TRADE
             */

            if (!SForm.lmf.virtOffersCB.isSelected()) {
                if (itemHoldTime != null) {

                    do {
                        System.out.println("Trying to accept virtual offer...");
                        TimeUnit.SECONDS.sleep(5);
                        countOfTries++;

                        if (countOfTries >= 6) {
                            System.out.println("Socket info about virtual offer was missed...");
                            return;
                        }
                    }
                    while (!isReadyToVirtualOffer);
                } else {
                    String temp = str;
                    temp = temp.substring(temp.indexOf("\"uniqid\":") + 9);
                    String uniId = temp.substring(0, temp.indexOf(","));
                    ListModeForm.uniqueIdsForUnlockedItems.add(uniId);
                }
            }

            overallBuyValue += Float.parseFloat(itemPrice);
            String tempStr = statusTF.getText();
            String p1 = tempStr.substring(0, tempStr.indexOf("$") + 1);
            String p2 = tempStr.substring(tempStr.indexOf("<"), tempStr.length());
            statusTF.setText(p1 + overallBuyValue + " " + p2);

            System.out.println("Waiting 15 sec. after success purchase...");
            TimeUnit.SECONDS.sleep(15);
        } catch (InterruptedException exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Прерывание сна (повторная остановка программы)...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии покупки предмета, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }
    }
}
