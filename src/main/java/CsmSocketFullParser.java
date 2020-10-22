import com.neovisionaries.ws.client.*;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class CsmSocketFullParser {
    private WebSocket csmWS;
    private Integer mode;
    boolean isStopped = false;
    boolean isSecondStage = false;

    private ListModeForm lmf;

    private Float priceFrom = 0f;
    private Float priceTo = 0f;

    CsmSocketFullParser(Integer mode, ListModeForm lmf, Float priceFrom, Float priceTo) {
        this.mode = mode;
        this.lmf = lmf;
        this.priceFrom = priceFrom;
        this.priceTo = priceTo;
    }

    void setupAndRunWSSConnector(String sessionToken, String userIdToken) {
        try {
            isStopped = false;
            final String csmWsLink = "wss://cs.money/ws";
            csmWS = new WebSocketFactory()
                    .createSocket(csmWsLink);

            csmWS.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String text) {
                    if (!isStopped) {
                        if (isSecondStage) {
                            if (text.contains("add_items_730")) {
                                parseCsmSocket(text, "730");
                            } else if (text.contains("add_items_570")) {
                                parseCsmSocket(text, "570");
                            }
                        }
                    }
                }

                @Override
                public void onFrame(WebSocket websocket, WebSocketFrame frame) {
                    if (isStopped) {
                        csmWS.disconnect();
                    }
                }

                @Override
                public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                    try {
                        if (!isStopped) {
                            System.out.println("Reconnecting to WSS[1]...");
                            csmWS = csmWS.recreate().connect();
                        }
                    } catch (Exception excp) {

                        SForm.lmf.logTextArea.append("\r\n\r\n" +
                                "Ошибка на стадии реконнекта к WSS CSM[1], остановка программы...");
                        excp.printStackTrace();
                        SForm.lmf.stopButton.doClick();

                    }
                }

                @Override
                public void onStateChanged(WebSocket websocket, WebSocketState newState) {
                    System.out.println("WSSMAIN State ~> " + newState.name());
                }
            });

            csmWS.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");

            if (SeleniumForCsm.getCfclearanceToken() == null) {
                csmWS.addHeader("Cookie", sessionToken + "; " +
                        userIdToken + "; " +
                        "type_device=desktop");
            } else {
                csmWS.addHeader("Cookie", sessionToken + "; " +
                        userIdToken + "; " +
                        "type_device=desktop; " +
                        SeleniumForCsm.getCfclearanceToken());
            }

            csmWS.connect();

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии подключения к WSS CSM[1], остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }
    }

    private String getAllGamesDataFromNamesJS() {
        String data = "";

        try {

            data = (getAllDataFromNamesJS("730") + getAllDataFromNamesJS("570"));

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии подключения к CSM JS базе имен предметов (для cs:go + dota 2), остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }

        return data;
    }

    private String getAllDataFromNamesJS(String appId) {
        String data = "";

        try {

            String link;

            switch (appId) {
                case "730":
                    link = "https://cs.money/js/database-skins/library-en-730.js";
                    break;

                case "570":
                    link = "https://cs.money/js/database-skins/library-en-570.js";
                    break;

                default:
                    data = getAllGamesDataFromNamesJS();
                    return data;
            }

            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");

            if (SeleniumForCsm.getCfclearanceToken() == null) {
                connection.setRequestProperty("Cookie", SeleniumForCsm.getSessionToken() + "; " +
                        SeleniumForCsm.getUserIdToken() + "; " +
                        "type_device=desktop");
            } else {
                connection.setRequestProperty("Cookie", SeleniumForCsm.getSessionToken() + "; " +
                        SeleniumForCsm.getUserIdToken() + "; " +
                        "type_device=desktop; " +
                        SeleniumForCsm.getCfclearanceToken());
            }

            StringBuilder builder = new StringBuilder();
            String newLine;

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

            while ((newLine = br.readLine()) != null)
                builder.append(newLine);

            data = builder.toString();

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии подключения к CSM JS базе имен предметов, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }

        return data;
    }

    private String parseItemNameFromJS(String dataFromJS, String itemXHRId) {
        String itemName = "";

        try {

            String temp = dataFromJS;
            String itemIdFromXHR = "\"" + itemXHRId + "\":";

            if (temp.contains(itemIdFromXHR)) {

                temp = temp.substring(temp.indexOf(itemIdFromXHR) + itemIdFromXHR.length());
                temp = temp.substring(temp.indexOf("\"m\":\"") + 5);
                itemName = temp.substring(0, temp.indexOf("\""));

            } else {
                System.out.println("Can't find item by current ID!");
                return "Null";
            }

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии обработки CSM JS базы имен предметов, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }

        return itemName;
    }

    private void parseCsmSocket(String socketInfo, String gameId) {

        try {

            if (SForm.lmf.csgoGame.isSelected() && gameId.equals("570"))
                return;

            if (SForm.lmf.dotaGame.isSelected() && gameId.equals("730"))
                return;

            System.out.println("\r\nLoading NamesJS data...");

            String jsData = "";
            if (gameId.equals("730")) {
                jsData = getAllDataFromNamesJS("730");
            } else if (gameId.equals("570")) {
                jsData = getAllDataFromNamesJS("570");
            }

            System.out.println("NamesJS data was successfully loaded.\r\n");

            String temp = socketInfo.substring(socketInfo.indexOf("\"data\":[") + 7);
            loopByItems(temp, jsData, false, gameId);

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии вызова функций по обработке данных wss, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }
    }

    private String loadCsmItemsDB(String appId) {
        StringBuilder sb = new StringBuilder();
        try {
            String link = "";

            switch (appId) {
                case "730":
                    link = "https://cs.money/730/load_bots_inventory";
                    break;

                case "570":
                    link = "https://cs.money/570/load_bots_inventory";
                    break;
            }

            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");

            if (SeleniumForCsm.getCfclearanceToken() == null) {
                connection.setRequestProperty("Cookie", SeleniumForCsm.getSessionToken() + "; " +
                        SeleniumForCsm.getUserIdToken() + "; " +
                        "type_device=desktop");
            } else {
                connection.setRequestProperty("Cookie", SeleniumForCsm.getSessionToken() + "; " +
                        SeleniumForCsm.getUserIdToken() + "; " +
                        "type_device=desktop; " +
                        SeleniumForCsm.getCfclearanceToken());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String newLine;

            while ((newLine = br.readLine()) != null)
                sb.append(newLine);

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии загрузки CSM базы предметов, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();
        }

        return sb.toString();
    }

    private String subAssetId(String data) {
        return data.substring(0, data.indexOf("]"));
    }

    private String subItemName(String data, String jsData) {
        return parseItemNameFromJS(jsData, data.substring(0,
                data.indexOf(",")));
    }

    private String subHoldTime(String data) {

        if (!data.contains("\"b\":")) {
            if (data.substring(0, data.indexOf("\"pd\":")).contains("\"t\":")) {
                String tempForHoldTime = data.substring(data.indexOf("\"t\":") + 5);
                return tempForHoldTime.substring(0, tempForHoldTime.indexOf("]"));
            }
        } else {
            if (data.substring(0, data.indexOf("\"b\":")).contains("\"t\":")) {
                String tempForHoldTime = data.substring(data.indexOf("\"t\":") + 5);
                return tempForHoldTime.substring(0, tempForHoldTime.indexOf("]"));
            }
        }
        return null;
    }

    private String subItemFullPrice(String data) {
        return data.substring(0, data.indexOf(","));
    }

    private String subItemUserFullPrice(String data) {
        if (!data.contains("\"b\":")) {
            if (data.substring(0, data.indexOf("\"pd\":")).contains("\"cp\":")) {
                String tempForUserFullPrice = data.substring(data.indexOf("\"cp\":") + 5);
                return tempForUserFullPrice.substring(0, tempForUserFullPrice.indexOf(","));
            }
        } else {
            if (data.substring(0, data.indexOf("\"b\":")).contains("\"cp\":")) {
                String tempForUserFullPrice = data.substring(data.indexOf("\"cp\":") + 5);
                return tempForUserFullPrice.substring(0, tempForUserFullPrice.indexOf(","));
            }
        }

        return null;
    }

    private String subItemFloat(String data) {

        if (!data.contains("\"b\":")) {
            if (data.substring(0, data.indexOf("\"pd\":")).contains("\"f\":")) {
                String tempForItemFloat = data.substring(data.indexOf(
                        "\"f\":") + 5);
                return tempForItemFloat.substring(0, tempForItemFloat.indexOf("]"));
            }
        } else {
            if (data.substring(0, data.indexOf("\"b\":")).contains("\"f\":")) {
                String tempForItemFloat = data.substring(data.indexOf(
                        "\"f\":") + 5);
                return tempForItemFloat.substring(0, tempForItemFloat.indexOf("]"));
            }
        }

        return null;
    }

    private String subItemBotValue(String data) {
        String tempForBot;
        if (!data.contains("\"b\":")) {
            tempForBot = data.substring(data.indexOf("\"bi\":") + 6);
        } else {
            tempForBot = data.substring(data.indexOf("\"b\":") + 5);
        }
        return tempForBot.substring(0, tempForBot.indexOf("]"));
    }

    private void fillStickersListOfNames(String data, List<String> stickersList, String jsData) {
        if (data.contains("\"n\":")) {
            data = data.substring(data.indexOf("\"n\":") + 5);
            stickersList.add(data.substring(0, data.indexOf("\"")));
        } else if (data.contains("\"o\":")) {
            data = data.substring(data.indexOf("\"o\":") + 4);
            String idOfSticker = data.substring(0, data.indexOf(","));
            stickersList.add(parseItemNameFromJS(jsData, idOfSticker));
        }
    }

    private void fillStickersListOfWear(String data, List<String> stickersListWear) {
        String currentStickerWear = data.substring(0, data.indexOf("}"));
        if (currentStickerWear.contains("\"i\":"))
            currentStickerWear = currentStickerWear.substring(0,
                    currentStickerWear.indexOf(","));

        stickersListWear.add(currentStickerWear);
    }

    void parseCsmDB(JButton b1, String gameId) {
        try {

            if (gameId.equals("730")) {
                System.out.println("\r\nLoading NamesJS data...");
                String jsData = getAllDataFromNamesJS("730");
                System.out.println("NamesJS data was successfully loaded.\r\n");

                TimeUnit.SECONDS.sleep(7);

                System.out.println("Loading CsmItemsDB...");
                String temp = loadCsmItemsDB("730");
                System.out.println("CsmItemsDB was successfully loaded.\r\n");

                b1.setEnabled(true);

                loopByItems(temp, jsData, true, "730");
            } else {
                System.out.println("\r\nLoading NamesJS data...");
                String jsData = getAllDataFromNamesJS("570");
                System.out.println("NamesJS data was successfully loaded.\r\n");

                TimeUnit.SECONDS.sleep(7);

                System.out.println("Loading CsmItemsDB...");
                String temp = loadCsmItemsDB("570");
                System.out.println("CsmItemsDB was successfully loaded.\r\n");

                b1.setEnabled(true);

                loopByItems(temp, jsData, true, "570");
            }

        } catch (InterruptedException exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Прерывание сна (повторная остановка программы)...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии обработки CSM базы предметов, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }
    }

    private String getMinAndMaxIdx(String[] array, Float p1, Float p2) {

        float min = 1, max = array.length - 1;

        try {

            for (int i = 1; i < array.length; i++) {
                if (array[i].contains("\"cp\":")) continue;

                String tmp = array[i];
                tmp = tmp.substring(tmp.indexOf("\"p\":") + 4);
                String itemPrice = subItemFullPrice(tmp);

                if (Float.parseFloat(itemPrice) <= p2) {
                    max = i;
                    break;
                }
            }

            for (int i = array.length - 1; i > 1; i--) {
                if (array[i].contains("\"cp\":")) continue;

                String tmp = array[i];
                tmp = tmp.substring(tmp.indexOf("\"p\":") + 4);
                String itemPrice = subItemFullPrice(tmp);

                if (Float.parseFloat(itemPrice) >= p1) {
                    min = i;
                    break;
                }
            }

            if (Math.round(min) <= Math.round(max)) {
                min = 1;
                max = array.length - 1;
            }

        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии чистки списка от ненужных предметов, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }

        return Math.round(min) + " " + Math.round(max);

    }

    private void loopByItems(String temp, String jsData, boolean isJsonData, String gameId) {
        try {

            if (temp != null) {
                int idx1, idx2;
                String itemsList[] = temp.split("\"id\":\\[");

                if (isJsonData) {
                    System.out.println("Removing unnecessary skins...");
                    idx1 = Integer.parseInt(getMinAndMaxIdx(itemsList, priceFrom, priceTo).split(" ")[1]);
                    idx2 = Integer.parseInt(getMinAndMaxIdx(itemsList, priceFrom, priceTo).split(" ")[0]);
                } else {
                    idx1 = 1;
                    idx2 = itemsList.length;
                }

                if (gameId.equals("730")) {
                    for (int i = idx1; i < idx2; i++) {

                        if (isStopped) break;

                        if (itemsList[i].length() < 10) continue;

                        String itemSteamHashName, itemId, itemFullPrice,
                                itemUserFullPrice, itemHoldTime, itemBotValue,
                                itemFloat, itemExtraPrice = null, itemJsID;

                        List<String> stickersList, stickersListWear;
                        stickersList = new ArrayList<>();
                        stickersListWear = new ArrayList<>();

                    /*
                            Parsing
                    */

                        Integer countOfStackedItems;
                        String countOfStackedItemsStr = itemsList[i].substring(0, itemsList[i].indexOf("]"));
                        if (countOfStackedItemsStr.contains(",")) {
                            String[] itemIdArray = countOfStackedItemsStr.split(",");
                            for (int j = 0; j < itemIdArray.length; j++)
                                itemIdArray[j] = itemIdArray[j].replaceAll("\"", "");

                            countOfStackedItems = itemIdArray.length;

                            // marketHashName
                            itemsList[i] = itemsList[i].substring(itemsList[i].indexOf("\"o\":") + 4);

                            // itemJsID
                            itemJsID = itemsList[i].substring(0, itemsList[i].indexOf(","));
                            itemSteamHashName = subItemName(itemsList[i], jsData);

                            // holdTime
                            itemHoldTime = subHoldTime(itemsList[i]);
                            String[] itemHoldTimeArray = new String[countOfStackedItems];
                            if (itemHoldTime != null)
                                itemHoldTimeArray = itemHoldTime.split(",");
                            else {
                                for (int j = 0; j < countOfStackedItems; j++) {
                                    itemHoldTimeArray[j] = null;
                                }
                            }

                            // itemFullPrice
                            String tempForItemFullPrice = itemsList[i].substring(itemsList[i].indexOf("\"p\":") + 4);
                            itemFullPrice = subItemFullPrice(tempForItemFullPrice);

                            // itemUserFullPrice
                            itemUserFullPrice = subItemUserFullPrice(itemsList[i]);

                            // itemExtraPrice
                            // itemExtraPriceReason
                            // itemDefaultPrice

                            boolean isArContains = false;

                            if (!tempForItemFullPrice.contains("\"b\":")) {
                                if (tempForItemFullPrice.substring(0, tempForItemFullPrice.indexOf("\"pd\":")).contains("\"ar\":"))
                                    isArContains = true;
                            } else {
                                if (tempForItemFullPrice.substring(0, tempForItemFullPrice.indexOf("\"b\":")).contains("\"ar\":"))
                                    isArContains = true;
                            }

                            if (isArContains) {
                                String tempForExtraPrice = tempForItemFullPrice.substring(
                                        tempForItemFullPrice.indexOf("\"ar\":") + 5);

                                if (tempForExtraPrice.contains("\"reason\":")) {
                                    tempForExtraPrice = tempForExtraPrice.substring(0,
                                            tempForExtraPrice.indexOf("}]") + 2);
                                } else
                                    tempForExtraPrice = tempForExtraPrice.substring(0, tempForExtraPrice.indexOf("]") + 1);

                                // ExtraPrice
                                if (tempForExtraPrice.contains("\"add_price\":")) {
                                    tempForExtraPrice = tempForExtraPrice.substring(
                                            tempForExtraPrice.indexOf("\"add_price\":") + 12);
                                    itemExtraPrice = tempForExtraPrice.substring(0,
                                            tempForExtraPrice.indexOf(","));
                                }
                            }

                            // itemFloat
                            itemFloat = subItemFloat(itemsList[i]);
                            String[] itemFloatArray = new String[countOfStackedItems];
                            if (itemFloat != null) {
                                itemFloatArray = itemFloat.split(",");
                                for (int j = 0; j < countOfStackedItems; j++) {
                                    itemFloatArray[j] = itemFloatArray[j].replaceAll("\"", "");
                                }
                            } else {
                                for (int j = 0; j < countOfStackedItems; j++) {
                                    itemFloatArray[j] = null;
                                }
                            }

                            // itemBotValue
                            itemBotValue = subItemBotValue(itemsList[i]);
                            String[] itemBotValueArray = itemBotValue.split(",");
                            for (int j = 0; j < countOfStackedItems; j++) {
                                itemBotValueArray[j] = itemBotValueArray[j].replaceAll("\"", "");
                            }

                            String itemFinalPrice = itemFullPrice;

                            boolean isOverpaid = (itemExtraPrice != null);
                            boolean isFromMarket = (itemUserFullPrice != null);
                            boolean isLocked = (itemHoldTime != null);

                            for (int j = 0; j < countOfStackedItems; j++) {
                                switch (mode) {

                                    case 3:
                                    /*System.out.println("Just New Item (stacked): " + itemSteamHashName + " price:" + itemFinalPrice + " id:" + itemIdArray[j] + "\r\n" +
                                            " holdTime:" + itemHoldTimeArray[j] + " botValue:" + itemBotValueArray[j] + " float:" + itemFloatArray[j] + " overpaid:" + isOverpaid + "\r\n" +
                                            " fromMarket:" + isFromMarket + " userFinalPrice:" + itemUserFullPrice + " locked:" + isLocked);*/

                                        lmf.check(itemSteamHashName, itemFinalPrice, itemIdArray[j],
                                                itemHoldTimeArray[j], itemBotValueArray[j], itemFloatArray[j], isLocked, isOverpaid,
                                                isFromMarket, itemUserFullPrice, itemJsID);

                                        break;
                                }
                            }

                        } else {

                            // assetId
                            itemId = subAssetId(itemsList[i]);
                            itemId = itemId.replaceAll("\"", "");

                            // marketHashName
                            itemsList[i] = itemsList[i].substring(itemsList[i].indexOf("\"o\":") + 4);

                            // itemJsID
                            itemJsID = itemsList[i].substring(0, itemsList[i].indexOf(","));
                            itemSteamHashName = subItemName(itemsList[i], jsData);

                            // holdTime
                            itemHoldTime = subHoldTime(itemsList[i]);

                            // itemFullPrice
                            String tempForItemFullPrice = itemsList[i].substring(itemsList[i].indexOf("\"p\":") + 4);
                            itemFullPrice = subItemFullPrice(tempForItemFullPrice);

                            // itemUserFullPrice
                            itemUserFullPrice = subItemUserFullPrice(itemsList[i]);

                            // itemExtraPrice
                            // itemExtraPriceReason
                            // itemDefaultPrice

                            boolean isArContains = false;

                            if (!tempForItemFullPrice.contains("\"b\":")) {
                                if (tempForItemFullPrice.substring(0, tempForItemFullPrice.indexOf("\"pd\":")).contains("\"ar\":"))
                                    isArContains = true;
                            } else {
                                if (tempForItemFullPrice.substring(0, tempForItemFullPrice.indexOf("\"b\":")).contains("\"ar\":"))
                                    isArContains = true;
                            }

                            if (isArContains) {
                                String tempForExtraPrice = tempForItemFullPrice.substring(
                                        tempForItemFullPrice.indexOf("\"ar\":") + 5);

                                if (tempForExtraPrice.contains("\"reason\":")) {
                                    tempForExtraPrice = tempForExtraPrice.substring(0,
                                            tempForExtraPrice.indexOf("}]") + 2);
                                } else
                                    tempForExtraPrice = tempForExtraPrice.substring(0, tempForExtraPrice.indexOf("]") + 1);

                                // ExtraPrice
                                if (tempForExtraPrice.contains("\"add_price\":")) {
                                    tempForExtraPrice = tempForExtraPrice.substring(
                                            tempForExtraPrice.indexOf("\"add_price\":") + 12);
                                    itemExtraPrice = tempForExtraPrice.substring(0,
                                            tempForExtraPrice.indexOf(","));
                                }
                            }

                            // itemFloat
                            itemFloat = subItemFloat(itemsList[i]);
                            if (itemFloat != null)
                                itemFloat = itemFloat.replaceAll("\"", "");

                            // itemBotValue
                            itemBotValue = subItemBotValue(itemsList[i]);
                            itemBotValue = itemBotValue.replaceAll("\"", "");

                            boolean isStickersContains = false;

                            if (!tempForItemFullPrice.contains("\"b\":")) {
                                if (itemsList[i].substring(0, itemsList[i].indexOf("\"pd\":")).contains("\"s\":"))
                                    isStickersContains = true;
                            } else {
                                if (itemsList[i].substring(0, itemsList[i].indexOf("\"b\":")).contains("\"s\":"))
                                    isStickersContains = true;
                            }

                            if (isStickersContains) {
                                String tempForStickers = itemsList[i].substring(itemsList[i].indexOf("\"s\":") + 4);
                                tempForStickers = tempForStickers.substring(0,
                                        tempForStickers.indexOf("}]") + 2);

                                // List, Wear, SVar
                                while (tempForStickers.contains("\"n\":") || tempForStickers.contains("\"o\":")) {
                                    // Names
                                    fillStickersListOfNames(tempForStickers, stickersList, jsData);

                                    // Wear
                                    tempForStickers = tempForStickers.substring(tempForStickers.indexOf("\"w\":") + 4);
                                    fillStickersListOfWear(tempForStickers, stickersListWear);
                                }
                            }

            /*
                    Print and check
             */

                            String itemFinalPrice = itemFullPrice;

                            boolean isOverpaid = (itemExtraPrice != null);
                            boolean isFromMarket = (itemUserFullPrice != null);
                            boolean isLocked = (itemHoldTime != null);

                            switch (mode) {
                                case 3:
                                /*System.out.println("Just New Item (solo): " + itemSteamHashName + " price:" + itemFinalPrice + " id:" + itemId + "\r\n" +
                                        " holdTime:" + itemHoldTime + " botValue:" + itemBotValue + " float:" + itemFloat + " overpaid:" + isOverpaid + "\r\n" +
                                        " fromMarket:" + isFromMarket + " userFinalPrice:" + itemUserFullPrice + " locked:" + isLocked);*/

                                    lmf.check(itemSteamHashName, itemFinalPrice, itemId,
                                            itemHoldTime, itemBotValue, itemFloat, isLocked, isOverpaid,
                                            isFromMarket, itemUserFullPrice, itemJsID);

                                    break;
                            }
                        }
                    }
                }
                else {

                    /*
                            START OF DOTA CYCLE
                     */

                    for (int i = idx1; i < idx2; i++) {

                        if (isStopped) break;

                        if (itemsList[i].length() < 10) continue;

                        String itemSteamHashName, itemId, itemFullPrice,
                                itemUserFullPrice, itemBotValue, itemJsID;

                        String gemsCount;

                    /*
                            Parsing
                    */

                        Integer countOfStackedItems;
                        String countOfStackedItemsStr = itemsList[i].substring(0, itemsList[i].indexOf("]"));
                        if (countOfStackedItemsStr.contains(",")) {
                            String[] itemIdArray = countOfStackedItemsStr.split(",");
                            for (int j = 0; j < itemIdArray.length; j++)
                                itemIdArray[j] = itemIdArray[j].replaceAll("\"", "");

                            countOfStackedItems = itemIdArray.length;

                            // marketHashName
                            itemsList[i] = itemsList[i].substring(itemsList[i].indexOf("\"o\":") + 4);

                            // itemJsID
                            itemJsID = itemsList[i].substring(0, itemsList[i].indexOf(","));
                            itemSteamHashName = subItemName(itemsList[i], jsData);

                            // itemFullPrice
                            String tempForItemFullPrice = itemsList[i].substring(itemsList[i].indexOf("\"p\":") + 4);
                            itemFullPrice = subItemFullPrice(tempForItemFullPrice);

                            // itemGemsCount
                            if (itemsList[i].contains("\"g\":")) {
                                String tempForGemsCount = itemsList[i].substring(itemsList[i].indexOf("\"g\":") + 4);
                                tempForGemsCount = tempForGemsCount.substring(0, tempForGemsCount.indexOf("}],") + 2);
                                gemsCount = String.valueOf(tempForGemsCount.split("\"i\":").length - 1);
                            } else gemsCount = "0";

                            // itemUserFullPrice
                            itemUserFullPrice = subItemUserFullPrice(itemsList[i]);

                            // itemBotValue
                            itemBotValue = subItemBotValue(itemsList[i]);
                            String[] itemBotValueArray = itemBotValue.split(",");
                            for (int j = 0; j < countOfStackedItems; j++) {
                                itemBotValueArray[j] = itemBotValueArray[j].replaceAll("\"", "");
                            }

                            boolean isFromMarket = (itemUserFullPrice != null);
                            for (int j = 0; j < countOfStackedItems; j++) {
                                switch (mode) {

                                    case 3:
                                    /*System.out.println("Just New Item (stacked)(dota2): " + itemSteamHashName + " price:" + itemFinalPrice + " id:" + itemIdArray[j] + "\r\n" +
                                            " holdTime:" + itemHoldTimeArray[j] + " botValue:" + itemBotValueArray[j] + " float:" + itemFloatArray[j] + " overpaid:" + isOverpaid + "\r\n" +
                                            " fromMarket:" + isFromMarket + " userFinalPrice:" + itemUserFullPrice + " locked:" + isLocked);*/

                                        lmf.checkDota(itemSteamHashName, itemFullPrice, itemIdArray[j], itemBotValueArray[j], isFromMarket, itemUserFullPrice,
                                                itemJsID, gemsCount);

                                        break;
                                }
                            }

                        } else {

                            // assetId
                            itemId = subAssetId(itemsList[i]);
                            itemId = itemId.replaceAll("\"", "");

                            // marketHashName
                            itemsList[i] = itemsList[i].substring(itemsList[i].indexOf("\"o\":") + 4);

                            // itemJsID
                            itemJsID = itemsList[i].substring(0, itemsList[i].indexOf(","));
                            itemSteamHashName = subItemName(itemsList[i], jsData);

                            // itemFullPrice
                            String tempForItemFullPrice = itemsList[i].substring(itemsList[i].indexOf("\"p\":") + 4);
                            itemFullPrice = subItemFullPrice(tempForItemFullPrice);

                            // itemGemsCount
                            if (itemsList[i].contains("\"g\":")) {
                                String tempForGemsCount = itemsList[i].substring(itemsList[i].indexOf("\"g\":") + 4);
                                tempForGemsCount = tempForGemsCount.substring(0, tempForGemsCount.indexOf("}],") + 2);
                                gemsCount = String.valueOf(tempForGemsCount.split("\"i\":").length - 1);
                            } else gemsCount = "0";

                            // itemUserFullPrice
                            itemUserFullPrice = subItemUserFullPrice(itemsList[i]);

                            // itemBotValue
                            itemBotValue = subItemBotValue(itemsList[i]);
                            itemBotValue = itemBotValue.replaceAll("\"", "");



            /*
                    Print and check
             */

                            boolean isFromMarket = (itemUserFullPrice != null);
                            switch (mode) {
                                case 3:
                                /*System.out.println("Just New Item (solo)(dota 2): " + itemSteamHashName + " price:" + itemFinalPrice + " id:" + itemId + "\r\n" +
                                        " holdTime:" + itemHoldTime + " botValue:" + itemBotValue + " float:" + itemFloat + " overpaid:" + isOverpaid + "\r\n" +
                                        " fromMarket:" + isFromMarket + " userFinalPrice:" + itemUserFullPrice + " locked:" + isLocked);*/

                                    lmf.checkDota(itemSteamHashName, itemFullPrice, itemId, itemBotValue,
                                            isFromMarket, itemUserFullPrice, itemJsID, gemsCount);

                                    break;
                            }
                        }
                    }
                }
            }
        } catch (Exception exc) {

            SForm.lmf.logTextArea.append("\r\n\r\n" +
                    "Ошибка на стадии обработки одного из предметов CSM, остановка программы...");
            exc.printStackTrace();
            SForm.lmf.stopButton.doClick();

        }
    }
}
